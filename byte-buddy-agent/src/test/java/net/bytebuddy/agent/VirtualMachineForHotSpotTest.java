package net.bytebuddy.agent;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;

import static org.mockito.Mockito.*;

public class VirtualMachineForHotSpotTest {

    @Test
    public void testAttachment() throws Exception {
        VirtualMachine.ForHotSpot.Connection connection = mock(VirtualMachine.ForHotSpot.Connection.class);
        VirtualMachine.ForHotSpot.Connection.Response response = mock(VirtualMachine.ForHotSpot.Connection.Response.class);
        when(connection.execute("1", "load", "instrument", "false", "foo=bar")).thenReturn(response);
        when(response.read(any(byte[].class))).then(new ByteAnswer("0".getBytes("UTF-8"))).then(new ByteAnswer((byte) 10));
        VirtualMachine virtualMachine = new VirtualMachine.ForHotSpot(connection);
        virtualMachine.loadAgent("foo", "bar");
        verify(connection).execute("1", "load", "instrument", "false", "foo=bar");
        verify(response).release();
        verifyNoMoreInteractions(connection);
        virtualMachine.detach();
        verify(connection).close();
    }

    @Test
    public void testAttachmentWithoutArgument() throws Exception {
        VirtualMachine.ForHotSpot.Connection connection = mock(VirtualMachine.ForHotSpot.Connection.class);
        VirtualMachine.ForHotSpot.Connection.Response response = mock(VirtualMachine.ForHotSpot.Connection.Response.class);
        when(connection.execute("1", "load", "instrument", "false", "foo")).thenReturn(response);
        when(response.read(any(byte[].class))).then(new ByteAnswer("0".getBytes("UTF-8"))).then(new ByteAnswer((byte) 10));
        VirtualMachine virtualMachine = new VirtualMachine.ForHotSpot(connection);
        virtualMachine.loadAgent("foo", null);
        verify(connection).execute("1", "load", "instrument", "false", "foo");
        verify(response).release();
        verifyNoMoreInteractions(connection);
        virtualMachine.detach();
        verify(connection).close();
    }

    @Test
    public void testNativeAttachment() throws Exception {
        VirtualMachine.ForHotSpot.Connection connection = mock(VirtualMachine.ForHotSpot.Connection.class);
        VirtualMachine.ForHotSpot.Connection.Response response = mock(VirtualMachine.ForHotSpot.Connection.Response.class);
        when(connection.execute("1", "load", "instrument", "true", "foo=bar")).thenReturn(response);
        when(response.read(any(byte[].class))).then(new ByteAnswer("0".getBytes("UTF-8"))).then(new ByteAnswer((byte) 10));
        VirtualMachine virtualMachine = new VirtualMachine.ForHotSpot(connection);
        virtualMachine.loadAgentPath("foo", "bar");
        verify(connection).execute("1", "load", "instrument", "true", "foo=bar");
        verify(response).release();
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
        new VirtualMachine.ForHotSpot(connection).loadAgent("foo", null);
    }

    @Test(expected = IllegalStateException.class)
    public void testAttachmentUnknownError() throws Exception {
        VirtualMachine.ForHotSpot.Connection connection = mock(VirtualMachine.ForHotSpot.Connection.class);
        VirtualMachine.ForHotSpot.Connection.Response response = mock(VirtualMachine.ForHotSpot.Connection.Response.class);
        when(connection.execute(anyString(), Mockito.<String[]>any())).thenReturn(response);
        when(response.read(any(byte[].class)))
                .then(new ByteAnswer("1".getBytes("UTF-8")))
                .then(new ByteAnswer((byte) 10))
                .then(new ByteAnswer("foo".getBytes("UTF-8")))
                .thenReturn(-1);
        new VirtualMachine.ForHotSpot(connection).loadAgent("foo", null);
    }

    private static class ByteAnswer implements Answer<Integer> {

        private final byte[] value;

        private ByteAnswer(byte... value) {
            this.value = value;
        }

        public Integer answer(InvocationOnMock invocationOnMock) throws Throwable {
            System.arraycopy(value, 0, invocationOnMock.getArgument(0), 0, value.length);
            return value.length;
        }
    }
}
