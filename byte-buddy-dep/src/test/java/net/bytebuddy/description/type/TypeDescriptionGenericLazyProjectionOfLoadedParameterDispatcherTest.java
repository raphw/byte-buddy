package net.bytebuddy.description.type;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;

import static org.mockito.Mockito.mock;

public class TypeDescriptionGenericLazyProjectionOfLoadedParameterDispatcherTest {

    @Test(expected = IllegalStateException.class)
    public void testLegacyVmGetType() throws Exception {
        TypeDescription.Generic.LazyProjection.ForLoadedParameter.Dispatcher.ForLegacyVm.INSTANCE.getType(mock(Object.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testLegacyVmGetParameterizedType() throws Exception {
        TypeDescription.Generic.LazyProjection.ForLoadedParameter.Dispatcher.ForLegacyVm.INSTANCE.getParameterizedType(mock(Object.class));
    }

    @Test
    public void testObjectProperties() throws Exception {
        final Iterator<Method> methods = Arrays.asList(Object.class.getDeclaredMethods()).iterator();
        ObjectPropertyAssertion.of(TypeDescription.Generic.LazyProjection.ForLoadedParameter.Dispatcher.ForModernVm.class)
                .create(new ObjectPropertyAssertion.Creator<Method>() {
                    @Override
                    public Method create() {
                        return methods.next();
                    }
                }).apply();
        ObjectPropertyAssertion.of(TypeDescription.Generic.LazyProjection.ForLoadedParameter.Dispatcher.ForLegacyVm.class).apply();
    }
}