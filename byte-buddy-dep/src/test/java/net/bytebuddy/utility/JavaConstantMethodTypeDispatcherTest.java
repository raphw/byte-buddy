package net.bytebuddy.utility;

import org.junit.Test;

import static org.mockito.Mockito.mock;

public class JavaConstantMethodTypeDispatcherTest {

    @Test(expected = IllegalStateException.class)
    public void testLegacyVmReturnType() throws Exception {
        JavaConstant.MethodType.Dispatcher.ForLegacyVm.INSTANCE.returnType(mock(Object.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testLegacyVmParameterArray() throws Exception {
        JavaConstant.MethodType.Dispatcher.ForLegacyVm.INSTANCE.parameterArray(mock(Object.class));
    }
}
