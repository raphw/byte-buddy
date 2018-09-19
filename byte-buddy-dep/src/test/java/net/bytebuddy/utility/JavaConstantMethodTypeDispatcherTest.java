package net.bytebuddy.utility;

import org.junit.Test;

import static org.mockito.Mockito.mock;

public class JavaConstantMethodTypeDispatcherTest {

    @Test(expected = UnsupportedOperationException.class)
    public void testLegacyVmReturnType() throws Exception {
        JavaConstant.MethodType.Dispatcher.ForLegacyVm.INSTANCE.returnType(mock(Object.class));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testLegacyVmParameterArray() throws Exception {
        JavaConstant.MethodType.Dispatcher.ForLegacyVm.INSTANCE.parameterArray(mock(Object.class));
    }
}
