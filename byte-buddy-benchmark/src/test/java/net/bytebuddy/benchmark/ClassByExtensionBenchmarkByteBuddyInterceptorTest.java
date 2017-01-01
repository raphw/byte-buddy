package net.bytebuddy.benchmark;

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.Assert.fail;

public class ClassByExtensionBenchmarkByteBuddyInterceptorTest {

    @Test(expected = UnsupportedOperationException.class)
    public void testProxyInterceptor() throws Exception {
        Constructor<?> constructor = ClassByExtensionBenchmark.ByteBuddyProxyInterceptor.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        try {
            constructor.newInstance();
            fail();
        } catch (InvocationTargetException exception) {
            throw (UnsupportedOperationException) exception.getCause();
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAccessorInterceptor() throws Exception {
        Constructor<?> constructor = ClassByExtensionBenchmark.ByteBuddyAccessInterceptor.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        try {
            constructor.newInstance();
            fail();
        } catch (InvocationTargetException exception) {
            throw (UnsupportedOperationException) exception.getCause();
        }
    }
}
