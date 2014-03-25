package com.blogspot.mydailyjava.bytebuddy.instrumentation.method;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import com.blogspot.mydailyjava.bytebuddy.utility.PackagePrivateMethod;
import org.junit.Before;
import org.junit.Test;
import org.mockito.asm.Type;

import java.lang.reflect.Method;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;

public class MethodDescriptionForLoadedMethodTest {

    private static final String HASH_CODE = "hashCode";
    private static final String INT_VALUE = "intValue";
    private static final String LONG_BITS_TO_DOUBLE = "longBitsToDouble";
    private static final String WAIT = "wait";
    private static final String CLONE = "clone";

    private MethodDescription objectHashCode;
    private MethodDescription integerIntValue;
    private MethodDescription doubleDoubleValue;
    private MethodDescription objectWait;
    private MethodDescription objectClone;
    private MethodDescription protectedMethod;
    private MethodDescription packagePrivateMethod;
    private MethodDescription privateMethod;

    @Before
    public void setUp() throws Exception {
        objectHashCode = new MethodDescription.ForLoadedMethod(Object.class.getDeclaredMethod(HASH_CODE));
        integerIntValue = new MethodDescription.ForLoadedMethod(Integer.class.getDeclaredMethod(INT_VALUE));
        doubleDoubleValue = new MethodDescription.ForLoadedMethod(Double.class.getDeclaredMethod(LONG_BITS_TO_DOUBLE, long.class));
        objectWait = new MethodDescription.ForLoadedMethod(Object.class.getDeclaredMethod(WAIT, long.class, int.class));
        objectClone = new MethodDescription.ForLoadedMethod(Object.class.getDeclaredMethod(CLONE));
        protectedMethod = new MethodDescription.ForLoadedMethod(PackagePrivateMethod.class.getDeclaredMethod(PackagePrivateMethod.PROTECTED_METHOD_NAME));
        packagePrivateMethod = new MethodDescription.ForLoadedMethod(PackagePrivateMethod.class.getDeclaredMethod(PackagePrivateMethod.PACKAGE_PRIVATE_METHOD_NAME));
        privateMethod = new MethodDescription.ForLoadedMethod(PackagePrivateMethod.class.getDeclaredMethod(PackagePrivateMethod.PRIVATE_METHOD_NAME));
    }

    @Test
    public void testRepresents() throws Exception {
        assertThat(objectHashCode.represents(Object.class.getDeclaredMethod(HASH_CODE)), is(true));
        assertThat(objectHashCode.represents(Integer.class.getDeclaredMethod(INT_VALUE)), is(false));
        assertThat(objectHashCode.represents(Double.class.getDeclaredMethod(LONG_BITS_TO_DOUBLE, long.class)), is(false));
        assertThat(objectHashCode.represents(Object.class.getDeclaredConstructor()), is(false));
        assertThat(integerIntValue.represents(Object.class.getDeclaredMethod(HASH_CODE)), is(false));
        assertThat(integerIntValue.represents(Integer.class.getDeclaredMethod(INT_VALUE)), is(true));
        assertThat(integerIntValue.represents(Double.class.getDeclaredMethod(LONG_BITS_TO_DOUBLE, long.class)), is(false));
        assertThat(integerIntValue.represents(Object.class.getDeclaredConstructor()), is(false));
        assertThat(doubleDoubleValue.represents(Object.class.getDeclaredMethod(HASH_CODE)), is(false));
        assertThat(doubleDoubleValue.represents(Integer.class.getDeclaredMethod(INT_VALUE)), is(false));
        assertThat(doubleDoubleValue.represents(Double.class.getDeclaredMethod(LONG_BITS_TO_DOUBLE, long.class)), is(true));
        assertThat(doubleDoubleValue.represents(Object.class.getDeclaredConstructor()), is(false));
    }

    @Test
    public void testGetInternalName() throws Exception {
        assertThat(objectHashCode.getInternalName(), is(HASH_CODE));
        assertThat(integerIntValue.getInternalName(), is(INT_VALUE));
        assertThat(doubleDoubleValue.getInternalName(), is(LONG_BITS_TO_DOUBLE));
    }

    @Test
    public void testGetDescriptor() throws Exception {
        assertThat(objectHashCode.getDescriptor(), is(Type.getMethodDescriptor(Object.class.getDeclaredMethod(HASH_CODE))));
        assertThat(integerIntValue.getDescriptor(), is(Type.getMethodDescriptor(Integer.class.getDeclaredMethod(INT_VALUE))));
        assertThat(doubleDoubleValue.getDescriptor(), is(Type.getMethodDescriptor(Double.class.getDeclaredMethod(LONG_BITS_TO_DOUBLE, long.class))));
    }

    @Test
    public void testIsOverridable() throws Exception {
        assertThat(objectHashCode.isOverridable(), is(true));
        assertThat(integerIntValue.isOverridable(), is(false));
        assertThat(doubleDoubleValue.isOverridable(), is(false));
    }

    @Test
    public void testStackSize() throws Exception {
        assertThat(objectHashCode.getStackSize(), is(1));
        assertThat(integerIntValue.getStackSize(), is(1));
        assertThat(doubleDoubleValue.getStackSize(), is(2));
    }

    @Test
    public void testGetParameterOffset() throws Exception {
        assertThat(doubleDoubleValue.getParameterOffset(0), is(0));
        assertThat(objectWait.getParameterOffset(0), is(1));
        assertThat(objectWait.getParameterOffset(1), is(3));
    }

    @Test
    public void testIsVisibleTo() throws Exception {
        assertThat(objectClone.isVisibleTo(new TypeDescription.ForLoadedType(Object.class)), is(true));
        assertThat(integerIntValue.isVisibleTo(new TypeDescription.ForLoadedType(Object.class)), is(true));
        assertThat(privateMethod.isVisibleTo(new TypeDescription.ForLoadedType(Object.class)), is(false));
        assertThat(packagePrivateMethod.isVisibleTo(new TypeDescription.ForLoadedType(Object.class)), is(false));
        assertThat(protectedMethod.isVisibleTo(new TypeDescription.ForLoadedType(Object.class)), is(false));
    }

    @Test
    public void testHashCode() throws Exception {
        assertThat(objectHashCode.hashCode(), is(hashCode(Object.class.getMethod(HASH_CODE))));
        assertThat(integerIntValue.hashCode(), is(hashCode(Integer.class.getMethod(INT_VALUE))));
        assertThat(doubleDoubleValue.hashCode(), is(hashCode(Double.class.getMethod(LONG_BITS_TO_DOUBLE, long.class))));
    }

    private static int hashCode(Method method) {
        return (Type.getInternalName(method.getDeclaringClass()) + "." + method.getName() + Type.getMethodDescriptor(method)).hashCode();
    }

    @Test
    public void testEquals() throws Exception {
        assertMethodEquality(objectHashCode, Object.class.getMethod(HASH_CODE));
        assertMethodEquality(integerIntValue, Integer.class.getMethod(INT_VALUE));
        assertMethodEquality(doubleDoubleValue, Double.class.getMethod(LONG_BITS_TO_DOUBLE, long.class));
    }

    private static void assertMethodEquality(MethodDescription methodDescription, Method method) {
        MethodDescription otherMethod = mock(MethodDescription.class);
        TypeDescription otherType = mock(TypeDescription.class);
        when(otherMethod.getUniqueSignature()).thenReturn(method.getName() + Type.getMethodDescriptor(method));
        when(otherMethod.getDeclaringType()).thenReturn(otherType);
        when(otherType.getName()).thenReturn(method.getDeclaringClass().getName());
        assertThat(methodDescription.equals(otherMethod), is(true));
        verify(otherType).getName();
        verifyNoMoreInteractions(otherType);
    }
}
