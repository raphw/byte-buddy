package net.bytebuddy.pool;

import net.bytebuddy.instrumentation.method.AbstractMethodDescriptionTest;
import net.bytebuddy.instrumentation.method.MethodDescription;
import org.junit.After;
import org.junit.Before;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static net.bytebuddy.matcher.ElementMatchers.is;

public class TypePoolDefaultMethodDescriptionTest extends AbstractMethodDescriptionTest {

    private TypePool typePool;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        typePool = TypePool.Default.ofClassPath();
    }

    @After
    public void tearDown() throws Exception {
        typePool.clear();
    }

    @Override
    protected MethodDescription describe(Method method) {
        return typePool.describe(method.getDeclaringClass().getName()).getDeclaredMethods().filter(is(method)).getOnly();
    }

    @Override
    protected MethodDescription describe(Constructor<?> constructor) {
        return typePool.describe(constructor.getDeclaringClass().getName()).getDeclaredMethods().filter(is(constructor)).getOnly();
    }
}
