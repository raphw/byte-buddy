package net.bytebuddy.agent;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class VirtualMachineForHotSpotTest {

    private static final String FOO = "foo", BAR = "bar";

    @Test
    public void testSystemProperties() throws Exception {
        VirtualMachine.ForHotSpot.Connection connection = mock(VirtualMachine.ForHotSpot.Connection.class);
        VirtualMachine.ForHotSpot.Connection.Response response = mock(VirtualMachine.ForHotSpot.Connection.Response.class);
        when(connection.execute("1", "properties", null, null, null)).thenReturn(response);
        when(response.read(any(byte[].class)))
                .then(new ByteAnswer("0".getBytes("UTF-8")))
                .then(new ByteAnswer("\n".getBytes("UTF-8")))
                .then(new ByteAnswer((FOO + "=" + BAR).getBytes("ISO_8859_1")))
                .thenReturn(-1);
        VirtualMachine virtualMachine = new VirtualMachine.ForHotSpot(connection);
        Properties properties = virtualMachine.getSystemProperties();
        assertThat(properties.size(), is(1));
        assertThat(properties.getProperty(FOO), is(BAR));
        verify(connection).execute("1", "properties", null, null, null);
        verify(response).close();
        verifyNoMoreInteractions(connection);
        virtualMachine.detach();
        verify(connection).close();
    }

    @Test
    public void testAgentProperties() throws Exception {
        VirtualMachine.ForHotSpot.Connection connection = mock(VirtualMachine.ForHotSpot.Connection.class);
        VirtualMachine.ForHotSpot.Connection.Response response = mock(VirtualMachine.ForHotSpot.Connection.Response.class);
        when(connection.execute("1", "agentProperties", null, null, null)).thenReturn(response);
        when(response.read(any(byte[].class)))
                .then(new ByteAnswer("0".getBytes("UTF-8")))
                .then(new ByteAnswer("\n".getBytes("UTF-8")))
                .then(new ByteAnswer((FOO + "=" + BAR).getBytes("ISO_8859_1")))
                .thenReturn(-1);
        VirtualMachine virtualMachine = new VirtualMachine.ForHotSpot(connection);
        Properties properties = virtualMachine.getAgentProperties();
        assertThat(properties.size(), is(1));
        assertThat(properties.getProperty(FOO), is(BAR));
        verify(connection).execute("1", "agentProperties", null, null, null);
        verify(response).close();
        verifyNoMoreInteractions(connection);
        virtualMachine.detach();
        verify(connection).close();
    }

    @Test
    public void testAttachment() throws Exception {
        VirtualMachine.ForHotSpot.Connection connection = mock(VirtualMachine.ForHotSpot.Connection.class);
        VirtualMachine.ForHotSpot.Connection.Response response = mock(VirtualMachine.ForHotSpot.Connection.Response.class);
        when(connection.execute("1", "load", "instrument", "false", FOO + "=" + BAR)).thenReturn(response);
        when(response.read(any(byte[].class))).then(new ByteAnswer("0".getBytes("UTF-8"))).then(new ByteAnswer((byte) 10));
        VirtualMachine virtualMachine = new VirtualMachine.ForHotSpot(connection);
        virtualMachine.loadAgent(FOO, BAR);
        verify(connection).execute("1", "load", "instrument", "false", FOO + "=" + BAR);
        verify(response).close();
        verifyNoMoreInteractions(connection);
        virtualMachine.detach();
        verify(connection).close();
    }

    @Test
    public void testAttachmentWithoutArgument() throws Exception {
        VirtualMachine.ForHotSpot.Connection connection = mock(VirtualMachine.ForHotSpot.Connection.class);
        VirtualMachine.ForHotSpot.Connection.Response response = mock(VirtualMachine.ForHotSpot.Connection.Response.class);
        when(connection.execute("1", "load", "instrument", "false", FOO)).thenReturn(response);
        when(response.read(any(byte[].class))).then(new ByteAnswer("0".getBytes("UTF-8"))).then(new ByteAnswer((byte) 10));
        VirtualMachine virtualMachine = new VirtualMachine.ForHotSpot(connection);
        virtualMachine.loadAgent(FOO);
        verify(connection).execute("1", "load", "instrument", "false", FOO);
        verify(response).close();
        verifyNoMoreInteractions(connection);
        virtualMachine.detach();
        verify(connection).close();
    }

    @Test
    public void testNativeAttachment() throws Exception {
        VirtualMachine.ForHotSpot.Connection connection = mock(VirtualMachine.ForHotSpot.Connection.class);
        VirtualMachine.ForHotSpot.Connection.Response response = mock(VirtualMachine.ForHotSpot.Connection.Response.class);
        when(connection.execute("1", "load", "instrument", "true", FOO + "=" + BAR)).thenReturn(response);
        when(response.read(any(byte[].class))).then(new ByteAnswer("0".getBytes("UTF-8"))).then(new ByteAnswer((byte) 10));
        VirtualMachine virtualMachine = new VirtualMachine.ForHotSpot(connection);
        virtualMachine.loadAgentPath(FOO, BAR);
        verify(connection).execute("1", "load", "instrument", "true", FOO + "=" + BAR);
        verify(response).close();
        verifyNoMoreInteractions(connection);
        virtualMachine.detach();
        verify(connection).close();
    }

    @Test
    public void testNativeAttachmentWithoutArgument() throws Exception {
        VirtualMachine.ForHotSpot.Connection connection = mock(VirtualMachine.ForHotSpot.Connection.class);
        VirtualMachine.ForHotSpot.Connection.Response response = mock(VirtualMachine.ForHotSpot.Connection.Response.class);
        when(connection.execute("1", "load", "instrument", "true", FOO)).thenReturn(response);
        when(response.read(any(byte[].class))).then(new ByteAnswer("0".getBytes("UTF-8"))).then(new ByteAnswer((byte) 10));
        VirtualMachine virtualMachine = new VirtualMachine.ForHotSpot(connection);
        virtualMachine.loadAgentPath(FOO);
        verify(connection).execute("1", "load", "instrument", "true", FOO);
        verify(response).close();
        verifyNoMoreInteractions(connection);
        virtualMachine.detach();
        verify(connection).close();
    }

    @Test
    public void testNativeLibraryAttachment() throws Exception {
        VirtualMachine.ForHotSpot.Connection connection = mock(VirtualMachine.ForHotSpot.Connection.class);
        VirtualMachine.ForHotSpot.Connection.Response response = mock(VirtualMachine.ForHotSpot.Connection.Response.class);
        when(connection.execute("1", "load", "instrument", "false", FOO + "=" + BAR)).thenReturn(response);
        when(response.read(any(byte[].class))).then(new ByteAnswer("0".getBytes("UTF-8"))).then(new ByteAnswer((byte) 10));
        VirtualMachine virtualMachine = new VirtualMachine.ForHotSpot(connection);
        virtualMachine.loadAgentLibrary(FOO, BAR);
        verify(connection).execute("1", "load", "instrument", "false", FOO + "=" + BAR);
        verify(response).close();
        verifyNoMoreInteractions(connection);
        virtualMachine.detach();
        verify(connection).close();
    }

    @Test
    public void testNativeLibraryAttachmentWithoutArgument() throws Exception {
        VirtualMachine.ForHotSpot.Connection connection = mock(VirtualMachine.ForHotSpot.Connection.class);
        VirtualMachine.ForHotSpot.Connection.Response response = mock(VirtualMachine.ForHotSpot.Connection.Response.class);
        when(connection.execute("1", "load", "instrument", "false", FOO)).thenReturn(response);
        when(response.read(any(byte[].class))).then(new ByteAnswer("0".getBytes("UTF-8"))).then(new ByteAnswer((byte) 10));
        VirtualMachine virtualMachine = new VirtualMachine.ForHotSpot(connection);
        virtualMachine.loadAgentLibrary(FOO);
        verify(connection).execute("1", "load", "instrument", "false", FOO);
        verify(response).close();
        verifyNoMoreInteractions(connection);
        virtualMachine.detach();
        verify(connection).close();
    }

    @Test
    public void testStartManagementAgent() throws Exception {
        VirtualMachine.ForHotSpot.Connection connection = mock(VirtualMachine.ForHotSpot.Connection.class);
        VirtualMachine.ForHotSpot.Connection.Response response = mock(VirtualMachine.ForHotSpot.Connection.Response.class);
        when(connection.execute("1", "jcmd", "ManagementAgent.start foo=bar", null, null)).thenReturn(response);
        when(response.read(any(byte[].class))).then(new ByteAnswer("0".getBytes("UTF-8"))).then(new ByteAnswer((byte) 10));
        VirtualMachine virtualMachine = new VirtualMachine.ForHotSpot(connection);
        Properties properties = new Properties();
        properties.setProperty("com.sun.management.foo", BAR);
        virtualMachine.startManagementAgent(properties);
        verify(connection).execute("1", "jcmd", "ManagementAgent.start foo=bar", null, null);
        verify(response).close();
        verifyNoMoreInteractions(connection);
        virtualMachine.detach();
        verify(connection).close();
    }

    @Test
    public void testStartLocalManagementAgent() throws Exception {
        VirtualMachine.ForHotSpot.Connection connection = mock(VirtualMachine.ForHotSpot.Connection.class);
        VirtualMachine.ForHotSpot.Connection.Response response = mock(VirtualMachine.ForHotSpot.Connection.Response.class);
        when(connection.execute("1", "jcmd", "ManagementAgent.start_local", null, null)).thenReturn(response);
        when(response.read(any(byte[].class)))
                .then(new ByteAnswer("0".getBytes("UTF-8")))
                .then(new ByteAnswer((byte) 10));
        VirtualMachine virtualMachine = spy(new VirtualMachine.ForHotSpot(connection));
        Properties properties = new Properties();
        properties.setProperty("com.sun.management.jmxremote.localConnectorAddress", BAR);
        doReturn(properties).when(virtualMachine).getAgentProperties();
        assertThat(virtualMachine.startLocalManagementAgent(), is(BAR));
        verify(connection).execute("1", "jcmd", "ManagementAgent.start_local", null, null);
        verify(response).close();
        verifyNoMoreInteractions(connection);
        virtualMachine.detach();
        verify(connection).close();
    }

    @Test(expected = IOException.class)
    public void testAttachmentIncompatibleProtocol() throws Exception {
        VirtualMachine.ForHotSpot.Connection connection = mock(VirtualMachine.ForHotSpot.Connection.class);
        VirtualMachine.ForHotSpot.Connection.Response response = mock(VirtualMachine.ForHotSpot.Connection.Response.class);
        when(connection.execute(anyString(), Mockito.<String[]>any())).thenReturn(response);
        when(response.read(any(byte[].class)))
                .then(new ByteAnswer("1".getBytes("UTF-8")))
                .then(new ByteAnswer("0".getBytes("UTF-8")))
                .then(new ByteAnswer("1".getBytes("UTF-8")))
                .then(new ByteAnswer((byte) 10));
        new VirtualMachine.ForHotSpot(connection).loadAgent(FOO, null);
    }

    @Test(expected = IllegalStateException.class)
    public void testAttachmentUnknownError() throws Exception {
        VirtualMachine.ForHotSpot.Connection connection = mock(VirtualMachine.ForHotSpot.Connection.class);
        VirtualMachine.ForHotSpot.Connection.Response response = mock(VirtualMachine.ForHotSpot.Connection.Response.class);
        when(connection.execute(anyString(), Mockito.<String[]>any())).thenReturn(response);
        when(response.read(any(byte[].class)))
                .then(new ByteAnswer("1".getBytes("UTF-8")))
                .then(new ByteAnswer((byte) 10))
                .then(new ByteAnswer(FOO.getBytes("UTF-8")))
                .thenReturn(-1);
        new VirtualMachine.ForHotSpot(connection).loadAgent(FOO, null);
    }

    private static class ByteAnswer implements Answer<Integer> {

        private final byte[] value;

        private ByteAnswer(byte... value) {
            this.value = value;
        }

        public Integer answer(InvocationOnMock invocationOnMock) throws Throwable {
            byte[] buffer = invocationOnMock.getArgument(0);
            System.arraycopy(value, 0, buffer, 0, value.length);
            return value.length;
        }
    }
}
