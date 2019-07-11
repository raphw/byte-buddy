package net.bytebuddy.agent;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VirtualMachineForOpenJ9Test {

    private static final long PROCESS_ID = 42L, USER_ID = 84L, VM_ID = 168L;

    private File temporaryFolder, attachFolder;

    @Before
    public void setUp() throws Exception {
        temporaryFolder = File.createTempFile("ibm", "temporary");
        assertThat(temporaryFolder.delete(), is(true));
        assertThat(temporaryFolder.mkdir(), is(true));
        attachFolder = new File(temporaryFolder, ".com_ibm_tools_attach");
        assertThat(attachFolder.mkdir(), is(true));
    }

    @After
    public void tearDown() throws Exception {
        assertThat(attachFolder.delete(), is(true));
        assertThat(temporaryFolder.delete(), is(true));
    }

    @Test(timeout = 10000L)
    public void testAttachment() throws Exception {
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();
        VirtualMachine.ForOpenJ9.Connector connector = mock(VirtualMachine.ForOpenJ9.Connector.class);
        when(connector.getTemporaryFolder()).thenReturn(temporaryFolder.getAbsolutePath());
        File targetFolder = new File(attachFolder, Long.toString(PROCESS_ID));
        assertThat(targetFolder.mkdir(), is(true));
        try {
            File attachInfo = new File(targetFolder, "attachInfo");
            Properties properties = new Properties();
            properties.setProperty("processId", Long.toString(PROCESS_ID));
            properties.setProperty("userUid", Long.toString(USER_ID));
            properties.setProperty("vmId", Long.toString(VM_ID));
            OutputStream outputStream = new FileOutputStream(attachInfo);
            try {
                try {
                    properties.store(outputStream, null);
                } finally {
                    outputStream.close();
                }
                final File sourceFolder = new File(attachFolder, Long.toString(VM_ID)), replyInfo = new File(sourceFolder, "replyInfo");
                assertThat(sourceFolder.mkdir(), is(true));
                try {
                    when(connector.userId()).thenReturn(USER_ID);
                    when(connector.getOwnerOf(targetFolder)).thenReturn(USER_ID);
                    when(connector.isExistingProcess(PROCESS_ID)).thenReturn(true);
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
                                        } finally {
                                            reader.close();
                                        }
                                        Socket socket = new Socket();
                                        try {
                                            socket.connect(new InetSocketAddress(port), 5000);
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
                        VirtualMachine.ForOpenJ9.attach(Long.toString(PROCESS_ID), 5000, connector).detach();
                        attachmentThread.join(5000);
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
        assertThat(error.get(), nullValue());
        for (String infrastructure : Arrays.asList("attachNotificationSync", "_master", "_attachlock")) {
            File file = new File(attachFolder, infrastructure);
            assertThat(file.isFile(), is(true));
            assertThat(file.delete(), is(true));
        }
    }
}