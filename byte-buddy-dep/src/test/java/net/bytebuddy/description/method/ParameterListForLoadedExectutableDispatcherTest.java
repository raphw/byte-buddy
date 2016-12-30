package net.bytebuddy.description.method;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class ParameterListForLoadedExectutableDispatcherTest {

    @Test
    public void testLegacyMethod() throws Exception {
        assertThat(ParameterList.ForLoadedExecutable.Dispatcher.ForLegacyVm.INSTANCE.describe(Object.class.getDeclaredMethod("toString")),
                CoreMatchers.<ParameterList<ParameterDescription.InDefinedShape>>is(new ParameterList.ForLoadedExecutable.OfLegacyVmMethod(Object.class
                        .getDeclaredMethod("toString"))));
    }

    @Test
    public void testLegacyConstructor() throws Exception {
        assertThat(ParameterList.ForLoadedExecutable.Dispatcher.ForLegacyVm.INSTANCE.describe(Object.class.getDeclaredConstructor()),
                CoreMatchers.<ParameterList<ParameterDescription.InDefinedShape>>is(new ParameterList.ForLoadedExecutable.OfLegacyVmConstructor(Object.class
                        .getDeclaredConstructor())));
    }

    @Test(expected = IllegalStateException.class)
    public void testLegacyGetParameterCount() throws Exception {
        ParameterList.ForLoadedExecutable.Dispatcher.ForLegacyVm.INSTANCE.getParameterCount(mock(Object.class));
    }

    @Test
    public void testObjectProperties() throws Exception {
        final Iterator<Method> methods = Arrays.asList(Object.class.getDeclaredMethods()).iterator();
        ObjectPropertyAssertion.of(ParameterList.ForLoadedExecutable.Dispatcher.ForJava8CapableVm.class).create(new ObjectPropertyAssertion.Creator<Method>() {
            @Override
            public Method create() {
                return methods.next();
            }
        }).apply();
        ObjectPropertyAssertion.of(ParameterList.ForLoadedExecutable.Dispatcher.ForLegacyVm.class).apply();
        ObjectPropertyAssertion.of(ParameterList.ForLoadedExecutable.Dispatcher.CreationAction.class).apply();
    }
}
