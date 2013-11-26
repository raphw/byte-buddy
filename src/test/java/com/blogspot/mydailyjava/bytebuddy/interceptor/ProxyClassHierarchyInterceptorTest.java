package com.blogspot.mydailyjava.bytebuddy.interceptor;

import com.blogspot.mydailyjava.bytebuddy.sample.constructor.*;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.Assert.*;

public class ProxyClassHierarchyInterceptorTest extends AbstractInterceptorTest {

    @Test
    public void test_Public0() throws Exception {
        Class<?> clazz = makeSimpleProxy(Public0.class);
        assertEquals(1, clazz.getDeclaredConstructors().length);
        assertTrue(Modifier.isPublic(clazz.getDeclaredConstructors()[0].getModifiers()));
        clazz.getDeclaredConstructors()[0].newInstance();
    }

    @Test
    public void test_Protected0() throws Exception {
        Class<?> clazz = makeSimpleProxy(Protected0.class);
        assertEquals(1, clazz.getDeclaredConstructors().length);
        assertTrue(Modifier.isProtected(clazz.getDeclaredConstructors()[0].getModifiers()));
        Constructor<?> constructor = clazz.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        constructor.newInstance();
    }

    @Test
    public void test_PackagePrivate0() throws Exception {
        Class<?> clazz = makeSimpleProxy(PackagePrivate0.class);
        assertEquals(1, clazz.getDeclaredConstructors().length);
        assertFalse(Modifier.isPublic(clazz.getDeclaredConstructors()[0].getModifiers()));
        assertFalse(Modifier.isProtected(clazz.getDeclaredConstructors()[0].getModifiers()));
        assertFalse(Modifier.isPrivate(clazz.getDeclaredConstructors()[0].getModifiers()));
        OBJENESIS.getInstantiatorOf(clazz).newInstance();
    }

    @Test
    public void test_Private0() throws Exception {
        Class<?> clazz = makeSimpleProxy(Private0.class);
        assertEquals(0, clazz.getDeclaredConstructors().length);
        OBJENESIS.getInstantiatorOf(clazz).newInstance();
    }

    @Test
    public void test_Exception0() throws Exception {
        Class<?> clazz = makeSimpleProxy(Exception0.class);
        assertEquals(1, clazz.getDeclaredConstructors().length);
        Class<?>[] exceptionClass = clazz.getDeclaredConstructors()[0].getExceptionTypes();
        assertEquals(2, exceptionClass.length);
        assertEquals(Exception.class, exceptionClass[0]);
        assertEquals(RuntimeException.class, exceptionClass[1]);
        clazz.getDeclaredConstructors()[0].newInstance();
    }

    @Test
    public void test_P1_Array() throws Exception {
        Class<?> clazz = makeSimpleProxy(P1_Array.class);
        assertEquals(1, clazz.getDeclaredConstructors().length);
        clazz.getDeclaredConstructors()[0].newInstance(new Object[]{ARRAY});
    }

    @Test
    public void test_P1_double() throws Exception {
        Class<?> clazz = makeSimpleProxy(P1_double.class);
        assertEquals(1, clazz.getDeclaredConstructors().length);
        clazz.getDeclaredConstructors()[0].newInstance(DOUBLE);
    }

    @Test
    public void test_P1_float() throws Exception {
        Class<?> clazz = makeSimpleProxy(P1_float.class);
        assertEquals(1, clazz.getDeclaredConstructors().length);
        clazz.getDeclaredConstructors()[0].newInstance(FLOAT);
    }

    @Test
    public void test_P1_int() throws Exception {
        Class<?> clazz = makeSimpleProxy(P1_int.class);
        assertEquals(1, clazz.getDeclaredConstructors().length);
        clazz.getDeclaredConstructors()[0].newInstance(INT);
    }

    @Test
    public void test_P1_Object() throws Exception {
        Class<?> clazz = makeSimpleProxy(P1_Object.class);
        assertEquals(1, clazz.getDeclaredConstructors().length);
        clazz.getDeclaredConstructors()[0].newInstance(OBJECT);
    }

    @Test
    public void test_P2_Array_Array() throws Exception {
        Class<?> clazz = makeSimpleProxy(P2_Array_Array.class);
        assertEquals(1, clazz.getDeclaredConstructors().length);
        clazz.getDeclaredConstructors()[0].newInstance(ARRAY, ARRAY);
    }

    @Test
    public void test_P2_Array_double() throws Exception {
        Class<?> clazz = makeSimpleProxy(P2_Array_double.class);
        assertEquals(1, clazz.getDeclaredConstructors().length);
        clazz.getDeclaredConstructors()[0].newInstance(ARRAY, DOUBLE);
    }

    @Test
    public void test_P2_Array_float() throws Exception {
        Class<?> clazz = makeSimpleProxy(P2_Array_float.class);
        assertEquals(1, clazz.getDeclaredConstructors().length);
        clazz.getDeclaredConstructors()[0].newInstance(ARRAY, FLOAT);
    }

    @Test
    public void test_P2_Array_int() throws Exception {
        Class<?> clazz = makeSimpleProxy(P2_Array_int.class);
        assertEquals(1, clazz.getDeclaredConstructors().length);
        clazz.getDeclaredConstructors()[0].newInstance(ARRAY, INT);
    }

    @Test
    public void test_P2_Array_Object() throws Exception {
        Class<?> clazz = makeSimpleProxy(P2_Array_Object.class);
        assertEquals(1, clazz.getDeclaredConstructors().length);
        clazz.getDeclaredConstructors()[0].newInstance(ARRAY, OBJECT);
    }

    @Test
    public void test_P2_double_Array() throws Exception {
        Class<?> clazz = makeSimpleProxy(P2_double_Array.class);
        assertEquals(1, clazz.getDeclaredConstructors().length);
        clazz.getDeclaredConstructors()[0].newInstance(DOUBLE, ARRAY);
    }

    @Test
    public void test_P2_double_double() throws Exception {
        Class<?> clazz = makeSimpleProxy(P2_double_double.class);
        assertEquals(1, clazz.getDeclaredConstructors().length);
        clazz.getDeclaredConstructors()[0].newInstance(DOUBLE, DOUBLE);
    }

    @Test
    public void test_P2_double_float() throws Exception {
        Class<?> clazz = makeSimpleProxy(P2_double_float.class);
        assertEquals(1, clazz.getDeclaredConstructors().length);
        clazz.getDeclaredConstructors()[0].newInstance(DOUBLE, FLOAT);
    }

    @Test
    public void test_P2_double_int() throws Exception {
        Class<?> clazz = makeSimpleProxy(P2_double_int.class);
        assertEquals(1, clazz.getDeclaredConstructors().length);
        clazz.getDeclaredConstructors()[0].newInstance(DOUBLE, INT);
    }

    @Test
    public void test_P2_double_Object() throws Exception {
        Class<?> clazz = makeSimpleProxy(P2_double_Object.class);
        assertEquals(1, clazz.getDeclaredConstructors().length);
        clazz.getDeclaredConstructors()[0].newInstance(DOUBLE, OBJECT);
    }

    @Test
    public void test_P2_float_Array() throws Exception {
        Class<?> clazz = makeSimpleProxy(P2_float_Array.class);
        assertEquals(1, clazz.getDeclaredConstructors().length);
        clazz.getDeclaredConstructors()[0].newInstance(FLOAT, ARRAY);
    }

    @Test
    public void test_P2_float_double() throws Exception {
        Class<?> clazz = makeSimpleProxy(P2_float_double.class);
        assertEquals(1, clazz.getDeclaredConstructors().length);
        clazz.getDeclaredConstructors()[0].newInstance(FLOAT, DOUBLE);
    }

    @Test
    public void test_P2_float_float() throws Exception {
        Class<?> clazz = makeSimpleProxy(P2_float_float.class);
        assertEquals(1, clazz.getDeclaredConstructors().length);
        clazz.getDeclaredConstructors()[0].newInstance(FLOAT, FLOAT);
    }

    @Test
    public void test_P2_float_int() throws Exception {
        Class<?> clazz = makeSimpleProxy(P2_float_int.class);
        assertEquals(1, clazz.getDeclaredConstructors().length);
        clazz.getDeclaredConstructors()[0].newInstance(FLOAT, INT);
    }

    @Test
    public void test_P2_float_Object() throws Exception {
        Class<?> clazz = makeSimpleProxy(P2_float_Object.class);
        assertEquals(1, clazz.getDeclaredConstructors().length);
        clazz.getDeclaredConstructors()[0].newInstance(FLOAT, OBJECT);
    }

    @Test
    public void test_P2_int_Array() throws Exception {
        Class<?> clazz = makeSimpleProxy(P2_int_Array.class);
        assertEquals(1, clazz.getDeclaredConstructors().length);
        clazz.getDeclaredConstructors()[0].newInstance(INT, ARRAY);
    }

    @Test
    public void test_P2_int_double() throws Exception {
        Class<?> clazz = makeSimpleProxy(P2_int_double.class);
        assertEquals(1, clazz.getDeclaredConstructors().length);
        clazz.getDeclaredConstructors()[0].newInstance(INT, DOUBLE);
    }

    @Test
    public void test_P2_int_float() throws Exception {
        Class<?> clazz = makeSimpleProxy(P2_int_float.class);
        assertEquals(1, clazz.getDeclaredConstructors().length);
        clazz.getDeclaredConstructors()[0].newInstance(INT, FLOAT);
    }

    @Test
    public void test_P2_int_int() throws Exception {
        Class<?> clazz = makeSimpleProxy(P2_int_int.class);
        assertEquals(1, clazz.getDeclaredConstructors().length);
        clazz.getDeclaredConstructors()[0].newInstance(INT, INT);
    }

    @Test
    public void test_P2_int_Object() throws Exception {
        Class<?> clazz = makeSimpleProxy(P2_int_Object.class);
        assertEquals(1, clazz.getDeclaredConstructors().length);
        clazz.getDeclaredConstructors()[0].newInstance(INT, OBJECT);
    }

    @Test
    public void test_P2_Object_Array() throws Exception {
        Class<?> clazz = makeSimpleProxy(P2_Object_Array.class);
        assertEquals(1, clazz.getDeclaredConstructors().length);
        clazz.getDeclaredConstructors()[0].newInstance(OBJECT, ARRAY);
    }

    @Test
    public void test_P2_Object_double() throws Exception {
        Class<?> clazz = makeSimpleProxy(P2_Object_double.class);
        assertEquals(1, clazz.getDeclaredConstructors().length);
        clazz.getDeclaredConstructors()[0].newInstance(OBJECT, DOUBLE);
    }

    @Test
    public void test_P2_Object_float() throws Exception {
        Class<?> clazz = makeSimpleProxy(P2_Object_float.class);
        assertEquals(1, clazz.getDeclaredConstructors().length);
        clazz.getDeclaredConstructors()[0].newInstance(OBJECT, FLOAT);
    }

    @Test
    public void test_P2_Object_int() throws Exception {
        Class<?> clazz = makeSimpleProxy(P2_Object_int.class);
        assertEquals(1, clazz.getDeclaredConstructors().length);
        clazz.getDeclaredConstructors()[0].newInstance(OBJECT, INT);
    }

    @Test
    public void test_P2_Object_Object() throws Exception {
        Class<?> clazz = makeSimpleProxy(P2_Object_Object.class);
        assertEquals(1, clazz.getDeclaredConstructors().length);
        clazz.getDeclaredConstructors()[0].newInstance(OBJECT, OBJECT);
    }

    private Class<?> makeSimpleProxy(Class<?> clazz) throws IOException {
        ClassReader classReader = new ClassReader(clazz.getName());
        ClassWriter classWriter = new ClassWriter(FLAGS);
        classReader.accept(new ProxyClassHierarchyInterceptor(classWriter), FLAGS);
        Class<?> proxyClass = classLoader.loadFrom(classWriter);
        assertTrue(clazz.isAssignableFrom(proxyClass));
        return proxyClass;
    }
}
