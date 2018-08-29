package net.bytebuddy.description.method;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class ParameterListForLoadedExectutableDispatcherTest {

    @Test
    public void testLegacyMethod() throws Exception {
        assertThat(ParameterList.ForLoadedExecutable.Dispatcher.ForLegacyVm.INSTANCE.describe(Object.class.getDeclaredMethod("toString"),
                new ParameterDescription.ForLoadedParameter.ParameterAnnotationSource.ForLoadedMethod(Object.class.getDeclaredMethod("toString"))),
                CoreMatchers.<ParameterList<ParameterDescription.InDefinedShape>>is(new ParameterList.ForLoadedExecutable.OfLegacyVmMethod(Object.class.getDeclaredMethod("toString"),
                        new ParameterDescription.ForLoadedParameter.ParameterAnnotationSource.ForLoadedMethod(Object.class.getDeclaredMethod("toString")))));
    }

    @Test
    public void testLegacyConstructor() throws Exception {
        assertThat(ParameterList.ForLoadedExecutable.Dispatcher.ForLegacyVm.INSTANCE.describe(Object.class.getDeclaredConstructor(),
                new ParameterDescription.ForLoadedParameter.ParameterAnnotationSource.ForLoadedConstructor(Object.class.getDeclaredConstructor())),
                CoreMatchers.<ParameterList<ParameterDescription.InDefinedShape>>is(new ParameterList.ForLoadedExecutable.OfLegacyVmConstructor(Object.class.getDeclaredConstructor(),
                        new ParameterDescription.ForLoadedParameter.ParameterAnnotationSource.ForLoadedConstructor(Object.class.getDeclaredConstructor()))));
    }

    @Test(expected = IllegalStateException.class)
    public void testLegacyGetParameterCount() throws Exception {
        ParameterList.ForLoadedExecutable.Dispatcher.ForLegacyVm.INSTANCE.getParameterCount(mock(Object.class));
    }
}
