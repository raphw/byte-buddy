package net.bytebuddy.utility;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;

import static org.mockito.Mockito.mock;

public class JavaInstanceMethodTypeDispatcherTest {

    @Test(expected = IllegalStateException.class)
    public void testLegacyVmReturnType() throws Exception {
        JavaInstance.MethodType.Dispatcher.ForLegacyVm.INSTANCE.returnType(mock(Object.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testLegacyVmParameterArray() throws Exception {
        JavaInstance.MethodType.Dispatcher.ForLegacyVm.INSTANCE.parameterArray(mock(Object.class));
    }

    @Test
    public void testObjectProperties() throws Exception {
        final Iterator<Method> methods = Arrays.asList(Object.class.getDeclaredMethods()).iterator();
        ObjectPropertyAssertion.of(JavaInstance.MethodType.Dispatcher.ForModernVm.class).create(new ObjectPropertyAssertion.Creator<Method>() {
            @Override
            public Method create() {
                return methods.next();
            }
        }).apply();
        ObjectPropertyAssertion.of(JavaInstance.MethodType.Dispatcher.ForLegacyVm.class).apply();
    }
}
