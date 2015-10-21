package net.bytebuddy.pool;

import net.bytebuddy.description.method.AbstractMethodDescriptionTest;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static net.bytebuddy.matcher.ElementMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TypePoolDefaultMethodDescriptionTest extends AbstractMethodDescriptionTest {

    private TypePool typePool;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        typePool = new TypePool.Default(TypePool.CacheProvider.NoOp.INSTANCE,
                ClassFileLocator.ForClassLoader.ofClassPath(),
                TypePool.Default.ReaderMode.EXTENDED); // In order to allow debug information parsing.
    }

    @After
    public void tearDown() throws Exception {
        typePool.clear();
    }

    @Override
    protected MethodDescription.InDefinedShape describe(Method method) {
        return typePool.describe(method.getDeclaringClass().getName())
                .resolve()
                .getDeclaredMethods().filter(is(method)).getOnly();
    }

    @Override
    protected MethodDescription.InDefinedShape describe(Constructor<?> constructor) {
        return typePool.describe(constructor.getDeclaringClass().getName())
                .resolve()
                .getDeclaredMethods().filter(is(constructor)).getOnly();
    }

    @Override
    protected boolean canReadDebugInformation() {
        return true;
    }
}
