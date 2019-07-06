package net.bytebuddy.agent;

import net.bytebuddy.test.utility.UnixRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;

import static org.mockito.Mockito.*;

public class VirtualMachineDefaultTest {

    @Rule
    public MethodRule unixRule = new UnixRule();

    @Test
    @UnixRule.Enforce
    public void testAttachment() throws Exception {
        VirtualMachine.Default.Connection connection = mock(VirtualMachine.Default.Connection.class);
        when(connection.read(any(byte[].class))).then(new ByteAnswer("0".getBytes("UTF-8"))).then(new ByteAnswer((byte) 10));
        VirtualMachine virtualMachine = new VirtualMachine.Default(connection);
        virtualMachine.loadAgent("foo", "bar");
        InOrder order = inOrder(connection);
        order.verify(connection).write("1".getBytes("UTF-8"));
        order.verify(connection).write(new byte[1]);
        order.verify(connection).write("load".getBytes("UTF-8"));
        order.verify(connection).write(new byte[1]);
        order.verify(connection).write("instrument".getBytes("UTF-8"));
        order.verify(connection).write(new byte[1]);
        order.verify(connection).write(Boolean.FALSE.toString().getBytes("UTF-8"));
        order.verify(connection).write(new byte[1]);
        order.verify(connection).write("foo=bar".getBytes("UTF-8"));
        order.verify(connection).write(new byte[1]);
        virtualMachine.detach();
        verify(connection).close();
    }

    @Test
    @UnixRule.Enforce
    public void testAttachmentWithoutArgument() throws Exception {
        VirtualMachine.Default.Connection connection = mock(VirtualMachine.Default.Connection.class);
        when(connection.read(any(byte[].class))).then(new ByteAnswer("0".getBytes("UTF-8"))).then(new ByteAnswer((byte) 10));
        VirtualMachine virtualMachine = new VirtualMachine.Default(connection);
        virtualMachine.loadAgent("foo", null);
        InOrder order = inOrder(connection);
        order.verify(connection).write("1".getBytes("UTF-8"));
        order.verify(connection).write(new byte[1]);
        order.verify(connection).write("load".getBytes("UTF-8"));
        order.verify(connection).write(new byte[1]);
        order.verify(connection).write("instrument".getBytes("UTF-8"));
        order.verify(connection).write(new byte[1]);
        order.verify(connection).write(Boolean.FALSE.toString().getBytes("UTF-8"));
        order.verify(connection).write(new byte[1]);
        order.verify(connection).write("foo".getBytes("UTF-8"));
        order.verify(connection).write(new byte[1]);
        virtualMachine.detach();
        verify(connection).close();
    }


    @Test(expected = IOException.class)
    @UnixRule.Enforce
    public void testAttachmentIncompatibleProtocol() throws Exception {
        VirtualMachine.Default.Connection connection = mock(VirtualMachine.Default.Connection.class);
        when(connection.read(any(byte[].class)))
                .then(new ByteAnswer("1".getBytes("UTF-8")))
                .then(new ByteAnswer("0".getBytes("UTF-8")))
                .then(new ByteAnswer("1".getBytes("UTF-8")))
                .then(new ByteAnswer((byte) 10));
        new VirtualMachine.Default(connection).loadAgent("foo", null);
    }

    @Test(expected = IllegalStateException.class)
    @UnixRule.Enforce
    public void testAttachmentUnknownError() throws Exception {
        VirtualMachine.Default.Connection connection = mock(VirtualMachine.Default.Connection.class);
        when(connection.read(any(byte[].class)))
                .then(new ByteAnswer("1".getBytes("UTF-8")))
                .then(new ByteAnswer((byte) 10))
                .then(new ByteAnswer("foo".getBytes("UTF-8")))
                .thenReturn(-1);
        new VirtualMachine.Default(connection).loadAgent("foo", null);
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