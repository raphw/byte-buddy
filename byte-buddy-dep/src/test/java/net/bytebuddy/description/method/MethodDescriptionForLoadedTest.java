package net.bytebuddy.description.method;

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodDescriptionForLoadedTest extends AbstractMethodDescriptionTest {

    @Override
    protected MethodDescription.InDefinedShape describe(Method method) {
        return new MethodDescription.ForLoadedMethod(method);
    }

    @Override
    protected MethodDescription.InDefinedShape describe(Constructor<?> constructor) {
        return new MethodDescription.ForLoadedConstructor(constructor);
    }

    @Test
    public void testGetLoadedMethod() throws Exception {
        Method method = Object.class.getDeclaredMethod("toString");
        assertThat(new MethodDescription.ForLoadedMethod(method).getLoadedMethod(), sameInstance(method));
    }

    @Override
    protected boolean canReadDebugInformation() {
        return false;
    }
}
