package net.bytebuddy.agent;

import net.bytebuddy.test.utility.JavaVersionRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class VirtualMachineForOpenJ9Test {

    private static final String FOO = "foo", BAR = "bar";

    private static final int PROCESS_ID = 42, USER_ID = 84, VM_ID = 168;

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    private File temporaryFolder, attachFolder;

    private InetAddress loopback;

    @Before
    public void setUp() throws Exception {
        temporaryFolder = File.createTempFile("ibm", "temporary");
        assertThat(temporaryFolder.delete(), is(true));
        assertThat(temporaryFolder.mkdir(), is(true));
        attachFolder = new File(temporaryFolder, ".com_ibm_tools_attach");
        assertThat(attachFolder.mkdir(), is(true));
        try {
            loopback = (InetAddress) InetAddress.class.getMethod("getLoopbackAddress").invoke(null);
        } catch (Exception ignored) {
            /* do nothing */
        }
    }

    @After
    public void tearDown() throws Exception {
        assertThat(attachFolder.delete(), is(true));
        assertThat(temporaryFolder.delete(), is(true));
    }

    @Test(timeout = 10000L)
    @JavaVersionRule.Enforce(7)
    // Fails sometimes on Java 6 due to timeout issues that are difficult to reproduce and solve.
    public void testAttachment() throws Throwable {
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();
        VirtualMachine.ForOpenJ9.Dispatcher dispatcher = mock(VirtualMachine.ForOpenJ9.Dispatcher.class);
        when(dispatcher.getTemporaryFolder(Integer.toString(PROCESS_ID))).thenReturn(temporaryFolder.getAbsolutePath());
        File targetFolder = new File(attachFolder, Integer.toString(PROCESS_ID));
        assertThat(targetFolder.mkdir(), is(true));
        try {
            File attachInfo = new File(targetFolder, "attachInfo");
            Properties properties = new Properties();
            properties.setProperty("processId", Integer.toString(PROCESS_ID));
            properties.setProperty("userUid", Integer.toString(USER_ID));
            properties.setProperty("vmId", Integer.toString(VM_ID));
            OutputStream outputStream = new FileOutputStream(attachInfo);
            try {
                try {
                    properties.store(outputStream, null);
                } finally {
                    outputStream.close();
                }
                final File sourceFolder = new File(attachFolder, Integer.toString(VM_ID)), replyInfo = new File(sourceFolder, "replyInfo");
                assertThat(sourceFolder.mkdir(), is(true));
                try {
                    when(dispatcher.userId()).thenReturn(USER_ID);
                    when(dispatcher.getOwnerIdOf(targetFolder)).thenReturn(USER_ID);
                    when(dispatcher.isExistingProcess(PROCESS_ID)).thenReturn(true);
                    Thread attachmentThread = new Thread() {
                        @Override
                        public void run() {
                            while (!Thread.interrupted()) {
                                try {
                                    if (replyInfo.exists()) {
                                        String key;
                                        int port;
                                        BufferedReader reader = new BufferedReader(new FileReader(replyInfo));
                                        try {
                                            key = reader.readLine();
                                            port = Integer.parseInt(reader.readLine());
                                            assertThat(reader.read(), is(-1));
                                        } catch (Exception exception) { // Reattempt to avoid races with attachment.
                                            Logger.getLogger("net.bytebuddy").info("Unexpected reply file content: " + exception.getMessage());
                                            continue;
                                        } finally {
                                            reader.close();
                                        }
                                        Socket socket = new Socket();
                                        try {
                                            socket.connect(new InetSocketAddress(loopback, port), 5000);
                                            socket.getOutputStream().write((' ' + key + ' ').getBytes("UTF-8"));
                                            socket.getOutputStream().write(0);
                                            socket.getOutputStream().flush();
                                            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                                            byte[] buffer = new byte[1024];
                                            int length;
                                            while ((length = socket.getInputStream().read(buffer)) != -1) {
                                                if (length > 0 && buffer[length - 1] == 0) {
                                                    outputStream.write(buffer, 0, length - 1);
                                                    break;
                                                } else {
                                                    outputStream.write(buffer, 0, length);
                                                }
                                            }
                                            assertThat(outputStream.toString("UTF-8"), is("ATTACH_DETACH"));
                                            socket.getOutputStream().write(0);
                                            socket.getOutputStream().flush();
                                        } finally {
                                            socket.close();
                                        }
                                        return;
                                    }
                                    Thread.sleep(100);
                                } catch (InterruptedException ignored) {
                                    break;
                                } catch (Throwable throwable) {
                                    error.set(throwable);
                                }
                            }
                        }
                    };
                    attachmentThread.setDaemon(true);
                    attachmentThread.setName("attachment-thread-emulation");
                    attachmentThread.start();
                    try {
                        VirtualMachine.ForOpenJ9.attach(Integer.toString(PROCESS_ID), 10000, dispatcher).detach();
                        attachmentThread.join(10000);
                    } catch (RuntimeException exception) {
                        Throwable throwable = error.get();
                        if (throwable == null) {
                            throw exception;
                        } else {
                            try {
                                Throwable.class.getMethod("addSuppressed", Throwable.class).invoke(throwable, exception);
                                throw throwable;
                            } catch (NoSuchMethodException ignored) {
                                throw throwable;
                            }
                        }
                    } finally {
                        attachmentThread.interrupt();
                    }
                } finally {
                    assertThat(sourceFolder.delete(), is(true));
                }
            } finally {
                assertThat(attachInfo.delete(), is(true));
            }
        } finally {
            assertThat(targetFolder.delete(), is(true));
        }
        Throwable throwable = error.get();
        if (throwable != null) {
            throw throwable;
        }
        for (String infrastructure : Arrays.asList("attachNotificationSync", "_master", "_attachlock")) {
            File file = new File(attachFolder, infrastructure);
            assertThat(file.isFile(), is(true));
            assertThat(file.delete(), is(true));
        }
    }

    @Test
    public void testGetSystemProperties() throws Exception {
        Socket socket = mock(Socket.class);
        OutputStream outputStream = mock(OutputStream.class);
        when(socket.getOutputStream()).thenReturn(outputStream);
        InputStream inputStream = mock(InputStream.class);
        when(socket.getInputStream()).thenReturn(inputStream);
        when(inputStream.read(any(byte[].class))).then(new Answer<Integer>() {
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                byte[] result = (FOO + "=" + BAR).getBytes("ISO_8859_1");
                byte[] buffer = invocation.getArgument(0);
                System.arraycopy(result, 0, buffer, 0, result.length);
                buffer[result.length] = 0;
                return result.length + 1;
            }
        });
        Properties properties = new VirtualMachine.ForOpenJ9(socket).getSystemProperties();
        assertThat(properties.size(), is(1));
        assertThat(properties.getProperty(FOO), is(BAR));
        verify(outputStream).write("ATTACH_GETSYSTEMPROPERTIES".getBytes("UTF-8"));
    }

    @Test
    public void testGetAgentProperties() throws Exception {
        Socket socket = mock(Socket.class);
        OutputStream outputStream = mock(OutputStream.class);
        when(socket.getOutputStream()).thenReturn(outputStream);
        InputStream inputStream = mock(InputStream.class);
        when(socket.getInputStream()).thenReturn(inputStream);
        when(inputStream.read(any(byte[].class))).then(new Answer<Integer>() {
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                byte[] result = (FOO + "=" + BAR).getBytes("ISO_8859_1");
                byte[] buffer = invocation.getArgument(0);
                System.arraycopy(result, 0, buffer, 0, result.length);
                buffer[result.length] = 0;
                return result.length + 1;
            }
        });
        Properties properties = new VirtualMachine.ForOpenJ9(socket).getAgentProperties();
        assertThat(properties.size(), is(1));
        assertThat(properties.getProperty(FOO), is(BAR));
        verify(outputStream).write("ATTACH_GETAGENTPROPERTIES".getBytes("UTF-8"));
    }

    @Test
    public void testLoadAgent() throws Exception {
        Socket socket = mock(Socket.class);
        OutputStream outputStream = mock(OutputStream.class);
        when(socket.getOutputStream()).thenReturn(outputStream);
        InputStream inputStream = mock(InputStream.class);
        when(socket.getInputStream()).thenReturn(inputStream);
        when(inputStream.read(any(byte[].class))).then(new Answer<Integer>() {
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                byte[] result = "ATTACH_ACK".getBytes("UTF-8");
                byte[] buffer = invocation.getArgument(0);
                System.arraycopy(result, 0, buffer, 0, result.length);
                buffer[result.length] = 0;
                return result.length + 1;
            }
        });
        new VirtualMachine.ForOpenJ9(socket).loadAgent(FOO, BAR);
        verify(outputStream).write(("ATTACH_LOADAGENT(instrument," + FOO + '=' + BAR + ')').getBytes("UTF-8"));
    }

    @Test
    public void testLoadAgentWithoutArgument() throws Exception {
        Socket socket = mock(Socket.class);
        OutputStream outputStream = mock(OutputStream.class);
        when(socket.getOutputStream()).thenReturn(outputStream);
        InputStream inputStream = mock(InputStream.class);
        when(socket.getInputStream()).thenReturn(inputStream);
        when(inputStream.read(any(byte[].class))).then(new Answer<Integer>() {
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                byte[] result = "ATTACH_ACK".getBytes("UTF-8");
                byte[] buffer = invocation.getArgument(0);
                System.arraycopy(result, 0, buffer, 0, result.length);
                buffer[result.length] = 0;
                return result.length + 1;
            }
        });
        new VirtualMachine.ForOpenJ9(socket).loadAgent(FOO);
        verify(outputStream).write(("ATTACH_LOADAGENT(instrument," + FOO + "=)").getBytes("UTF-8"));
    }

    @Test
    public void testLoadAgentPath() throws Exception {
        Socket socket = mock(Socket.class);
        OutputStream outputStream = mock(OutputStream.class);
        when(socket.getOutputStream()).thenReturn(outputStream);
        InputStream inputStream = mock(InputStream.class);
        when(socket.getInputStream()).thenReturn(inputStream);
        when(inputStream.read(any(byte[].class))).then(new Answer<Integer>() {
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                byte[] result = "ATTACH_ACK".getBytes("UTF-8");
                byte[] buffer = invocation.getArgument(0);
                System.arraycopy(result, 0, buffer, 0, result.length);
                buffer[result.length] = 0;
                return result.length + 1;
            }
        });
        new VirtualMachine.ForOpenJ9(socket).loadAgentPath(FOO, BAR);
        verify(outputStream).write(("ATTACH_LOADAGENTPATH(" + FOO + ',' + BAR + ')').getBytes("UTF-8"));
    }

    @Test
    public void testLoadAgentPathWithoutArgument() throws Exception {
        Socket socket = mock(Socket.class);
        OutputStream outputStream = mock(OutputStream.class);
        when(socket.getOutputStream()).thenReturn(outputStream);
        InputStream inputStream = mock(InputStream.class);
        when(socket.getInputStream()).thenReturn(inputStream);
        when(inputStream.read(any(byte[].class))).then(new Answer<Integer>() {
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                byte[] result = "ATTACH_ACK".getBytes("UTF-8");
                byte[] buffer = invocation.getArgument(0);
                System.arraycopy(result, 0, buffer, 0, result.length);
                buffer[result.length] = 0;
                return result.length + 1;
            }
        });
        new VirtualMachine.ForOpenJ9(socket).loadAgentPath(FOO);
        verify(outputStream).write(("ATTACH_LOADAGENTPATH(" + FOO + ')').getBytes("UTF-8"));
    }

    @Test
    public void testLoadAgentLibrary() throws Exception {
        Socket socket = mock(Socket.class);
        OutputStream outputStream = mock(OutputStream.class);
        when(socket.getOutputStream()).thenReturn(outputStream);
        InputStream inputStream = mock(InputStream.class);
        when(socket.getInputStream()).thenReturn(inputStream);
        when(inputStream.read(any(byte[].class))).then(new Answer<Integer>() {
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                byte[] result = "ATTACH_ACK".getBytes("UTF-8");
                byte[] buffer = invocation.getArgument(0);
                System.arraycopy(result, 0, buffer, 0, result.length);
                buffer[result.length] = 0;
                return result.length + 1;
            }
        });
        new VirtualMachine.ForOpenJ9(socket).loadAgentLibrary(FOO, BAR);
        verify(outputStream).write(("ATTACH_LOADAGENTLIBRARY(" + FOO + ',' + BAR + ')').getBytes("UTF-8"));
    }

    @Test
    public void testLoadAgentLibraryWithoutArgument() throws Exception {
        Socket socket = mock(Socket.class);
        OutputStream outputStream = mock(OutputStream.class);
        when(socket.getOutputStream()).thenReturn(outputStream);
        InputStream inputStream = mock(InputStream.class);
        when(socket.getInputStream()).thenReturn(inputStream);
        when(inputStream.read(any(byte[].class))).then(new Answer<Integer>() {
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                byte[] result = "ATTACH_ACK".getBytes("UTF-8");
                byte[] buffer = invocation.getArgument(0);
                System.arraycopy(result, 0, buffer, 0, result.length);
                buffer[result.length] = 0;
                return result.length + 1;
            }
        });
        new VirtualMachine.ForOpenJ9(socket).loadAgentLibrary(FOO);
        verify(outputStream).write(("ATTACH_LOADAGENTLIBRARY(" + FOO + ')').getBytes("UTF-8"));
    }

    @Test
    public void testStartManagementAgent() throws Exception {
        Socket socket = mock(Socket.class);
        OutputStream outputStream = mock(OutputStream.class);
        when(socket.getOutputStream()).thenReturn(outputStream);
        InputStream inputStream = mock(InputStream.class);
        when(socket.getInputStream()).thenReturn(inputStream);
        when(inputStream.read(any(byte[].class))).then(new Answer<Integer>() {
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                byte[] result = "ATTACH_ACK".getBytes("UTF-8");
                byte[] buffer = invocation.getArgument(0);
                System.arraycopy(result, 0, buffer, 0, result.length);
                buffer[result.length] = 0;
                return result.length + 1;
            }
        });
        final Properties properties = new Properties();
        properties.setProperty(FOO, BAR);
        new VirtualMachine.ForOpenJ9(socket).startManagementAgent(properties);
        verify(outputStream).write("ATTACH_START_MANAGEMENT_AGENT".getBytes("UTF-8"));
        verify(outputStream).write(argThat(new ArgumentMatcher<byte[]>() {

            public boolean matches(byte[] buffer) {
                Properties matched = new Properties();
                try {
                    matched.load(new ByteArrayInputStream(buffer));
                } catch (IOException e) {
                    throw new AssertionError(e);
                }
                return properties.equals(matched);
            }
        }));
    }

    @Test
    public void testStartLocalManagementAgent() throws Exception {
        Socket socket = mock(Socket.class);
        OutputStream outputStream = mock(OutputStream.class);
        when(socket.getOutputStream()).thenReturn(outputStream);
        InputStream inputStream = mock(InputStream.class);
        when(socket.getInputStream()).thenReturn(inputStream);
        when(inputStream.read(any(byte[].class))).then(new Answer<Integer>() {
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                byte[] result = ("ATTACH_ACK" + FOO).getBytes("UTF-8");
                byte[] buffer = invocation.getArgument(0);
                System.arraycopy(result, 0, buffer, 0, result.length);
                buffer[result.length] = 0;
                return result.length + 1;
            }
        });
        assertThat(new VirtualMachine.ForOpenJ9(socket).startLocalManagementAgent(), is(FOO));
        verify(outputStream).write("ATTACH_START_LOCAL_MANAGEMENT_AGENT".getBytes("UTF-8"));
    }

    @Test
    public void testDetach() throws Exception {
        Socket socket = mock(Socket.class);
        OutputStream outputStream = mock(OutputStream.class);
        when(socket.getOutputStream()).thenReturn(outputStream);
        InputStream inputStream = mock(InputStream.class);
        when(socket.getInputStream()).thenReturn(inputStream);
        when(inputStream.read(any(byte[].class))).then(new Answer<Integer>() {
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                byte[] buffer = invocation.getArgument(0);
                buffer[0] = 0;
                return 1;
            }
        });
        new VirtualMachine.ForOpenJ9(socket).detach();
        verify(outputStream).write("ATTACH_DETACH".getBytes("UTF-8"));
    }
}
