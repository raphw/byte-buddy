package com.blogspot.mydailyjava.bytebuddy.asm.interceptor;

import com.blogspot.mydailyjava.bytebuddy.instrument.FixedValue;
import com.blogspot.mydailyjava.bytebuddy.sample.method.P_Object;
import com.blogspot.mydailyjava.bytebuddy.util.RenamingInterceptor;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MethodCallInterceptorTest extends AbstractInterceptorTest {

    private static class FixedObjectValue implements FixedValue<Object> {
        @Override
        public Object getValue() {
            return OBJECT;
        }
    }

    private FixedValue<?> fixedValue;

    @Before
    public void setUp() throws Exception {
        fixedValue = new FixedObjectValue();
    }

    @Test
    public void testFixedValue() throws Exception {
        Class<?> clazz = makeSimpleProxy(P_Object.class);
        assertEquals(1, clazz.getDeclaredFields().length);
        Field field = clazz.getDeclaredFields()[0];
        assertTrue(Modifier.isStatic(field.getModifiers()));
        assertTrue(Modifier.isPrivate(field.getModifiers()));
        assertTrue(Modifier.isFinal(field.getModifiers()));
        field.setAccessible(true);
        P_Object p_object = (P_Object) OBJENESIS.getInstantiatorOf(clazz).newInstance();
        p_object.invoke();
//        assertEquals(fixedValue, field.get(null));
    }

    private Class<?> makeSimpleProxy(Class<?> clazz) throws IOException {
        ClassReader classReader = new ClassReader(clazz.getName());
        ClassWriter classWriter = new ClassWriter(FLAGS);
        classReader.accept(new MethodCallInterceptor(new RenamingInterceptor(classWriter), fixedValue), FLAGS);
        Class<?> proxyClass = classLoader.loadFrom(classWriter);
        assertTrue(clazz.isAssignableFrom(proxyClass));
        return proxyClass;
    }
}
