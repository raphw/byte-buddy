package net.bytebuddy.agent;

public class VirtualMachineForHotSpotTest {

    // TODO: Adjust tests after API correction

    /*@Test
    public void testAttachment() throws Exception {
        VirtualMachine.ForHotSpot.Connection connection = mock(VirtualMachine.ForHotSpot.Connection.class);
        when(connection.read(any(byte[].class))).then(new ByteAnswer("0".getBytes("UTF-8"))).then(new ByteAnswer((byte) 10));
        VirtualMachine virtualMachine = new VirtualMachine.ForHotSpot(connection);
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
    public void testAttachmentWithoutArgument() throws Exception {
        VirtualMachine.ForHotSpot.Connection connection = mock(VirtualMachine.ForHotSpot.Connection.class);
        when(connection.read(any(byte[].class))).then(new ByteAnswer("0".getBytes("UTF-8"))).then(new ByteAnswer((byte) 10));
        VirtualMachine virtualMachine = new VirtualMachine.ForHotSpot(connection);
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
    public void testAttachmentIncompatibleProtocol() throws Exception {
        VirtualMachine.ForHotSpot.Connection connection = mock(VirtualMachine.ForHotSpot.Connection.class);
        when(connection.read(any(byte[].class)))
                .then(new ByteAnswer("1".getBytes("UTF-8")))
                .then(new ByteAnswer("0".getBytes("UTF-8")))
                .then(new ByteAnswer("1".getBytes("UTF-8")))
                .then(new ByteAnswer((byte) 10));
        new VirtualMachine.ForHotSpot(connection).loadAgent("foo", null);
    }

    @Test(expected = IllegalStateException.class)
    public void testAttachmentUnknownError() throws Exception {
        VirtualMachine.ForHotSpot.Connection connection = mock(VirtualMachine.ForHotSpot.Connection.class);
        when(connection.read(any(byte[].class)))
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
    }*/
}