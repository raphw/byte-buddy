package com.blogspot.mydailyjava.bytebuddy.method.bytecode;

import com.blogspot.mydailyjava.bytebuddy.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.type.TypeDescription;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.asm.Type;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class SuperClassDelegationTest {

    private static final String FOO = "foo";
    private static final int NON_DEFAULT_VALUE = 42;

    private TypeDescription typeDescription;

    @Before
    public void setUp() throws Exception {
        typeDescription = mock(TypeDescription.class);
    }

    @SuppressWarnings("unused")
    public static class ReferenceDelegation extends AbstractCallHistoryTraceable {

        private boolean called;

        public Object foo() {
            markCalled();
            return FOO;
        }
    }

    @Test
    public void testReferenceDelegation() throws Exception {
        testDelegation(ReferenceDelegation.class, is(FOO));
    }

    @SuppressWarnings("unused")
    public static class IntDelegation extends AbstractCallHistoryTraceable {

        public int foo() {
            markCalled();
            return NON_DEFAULT_VALUE;
        }
    }

    @Test
    public void testStubMethodInt() throws Exception {
        testDelegation(IntDelegation.class, is(NON_DEFAULT_VALUE));
    }

    @SuppressWarnings("unused")
    public static class DelegationDelegation extends AbstractCallHistoryTraceable {

        public long foo() {
            markCalled();
            return (long) NON_DEFAULT_VALUE;
        }
    }

    @Test
    public void testStubMethodLong() throws Exception {
        testDelegation(DelegationDelegation.class, is((long) NON_DEFAULT_VALUE));
    }

    @SuppressWarnings("unused")
    public static class ShortDelegation extends AbstractCallHistoryTraceable {

        public short foo() {
            markCalled();
            return (short) NON_DEFAULT_VALUE;
        }
    }

    @Test
    public void testStubMethodShort() throws Exception {
        testDelegation(ShortDelegation.class, is((short) NON_DEFAULT_VALUE));
    }

    @SuppressWarnings("unused")
    public static class ByteDelegation extends AbstractCallHistoryTraceable {

        public byte foo() {
            markCalled();
            return (byte) NON_DEFAULT_VALUE;
        }
    }

    @Test
    public void testStubMethodByte() throws Exception {
        testDelegation(ByteDelegation.class, is((byte) NON_DEFAULT_VALUE));
    }

    @SuppressWarnings("unused")
    public static class FloatDelegation extends AbstractCallHistoryTraceable {

        public float foo() {
            markCalled();
            return (float) NON_DEFAULT_VALUE;
        }
    }

    @Test
    public void testStubMethodFloat() throws Exception {
        testDelegation(FloatDelegation.class, is((float) NON_DEFAULT_VALUE));
    }

    @SuppressWarnings("unused")
    public static class DoubleDelegation extends AbstractCallHistoryTraceable {

        public double foo() {
            markCalled();
            return (double) NON_DEFAULT_VALUE;
        }
    }

    @Test
    public void testStubMethodDouble() throws Exception {
        testDelegation(DoubleDelegation.class, is((double) NON_DEFAULT_VALUE));
    }

    @SuppressWarnings("unused")
    public static class CharDelegation extends AbstractCallHistoryTraceable {

        public char foo() {
            markCalled();
            return (char) NON_DEFAULT_VALUE;
        }
    }

    @Test
    public void testStubMethodChar() throws Exception {
        testDelegation(CharDelegation.class, is((char) NON_DEFAULT_VALUE));
    }

    @SuppressWarnings("unused")
    public static class BooleanDelegation extends AbstractCallHistoryTraceable {

        public boolean foo() {
            markCalled();
            return true;
        }
    }

    @Test
    public void testStubMethodBoolean() throws Exception {
        testDelegation(BooleanDelegation.class, is(true));
    }

    @SuppressWarnings("unused")
    public static class VoidDelegation extends AbstractCallHistoryTraceable {

        public void foo() {
            markCalled();
        }
    }

    @Test
    public void testStubMethodVoid() throws Exception {
        testDelegation(VoidDelegation.class, nullValue(Void.class));
    }

    @SuppressWarnings("unused")
    public static class ReferenceDelegationWithArguments extends AbstractCallHistoryTraceable {

        public Object[] foo(boolean a, byte b, short c, int d, char e, float f, double g, long h, Object i) {
            markCalled();
            return new Object[]{a, b, c, d, e, f, g, h, i};
        }
    }

    @Test
    public void testReferenceDelegationWithArguments() throws Exception {
        Object[] argument = new Object[]{true,
                (byte) NON_DEFAULT_VALUE,
                (short) NON_DEFAULT_VALUE,
                NON_DEFAULT_VALUE,
                (char) NON_DEFAULT_VALUE,
                (float) NON_DEFAULT_VALUE,
                (double) NON_DEFAULT_VALUE,
                (long) NON_DEFAULT_VALUE,
                FOO};
        testDelegation(ReferenceDelegationWithArguments.class,
                is(argument),
                new Class<?>[]{boolean.class,
                        byte.class,
                        short.class,
                        int.class,
                        char.class,
                        float.class,
                        double.class,
                        long.class,
                        Object.class},
                argument);
    }

    private void testDelegation(Class<?> type, Matcher<?> matcher) throws Exception {
        testDelegation(type, matcher, new Class<?>[0], new Object[0]);
    }

    @SuppressWarnings("unchecked")
    private void testDelegation(Class<?> type, Matcher<?> matcher, Class<?>[] parameterType, Object[] parameter) throws Exception {
        assertThat("Arguments cannot produce valid result", parameterType.length, is(parameter.length));
        when(typeDescription.getSuperClassInternalName()).thenReturn(Type.getInternalName(type));
        ByteCodeAppenderFactoryTester tester = new ByteCodeAppenderFactoryTester(SuperClassDelegation.INSTANCE, typeDescription, type);
        MethodDescription methodDescription = new MethodDescription.ForMethod(type.getDeclaredMethod(FOO, parameterType));
        MethodDescription spied = spy(methodDescription);
        Class<?> instrumented = tester.applyTo(spied, methodDescription);
        assertEquals(type, instrumented.getSuperclass());
        assertThat(instrumented.getDeclaredMethods().length, is(1));
        Object instance = instrumented.getDeclaredConstructor().newInstance();
        assertThat(instrumented.getDeclaredMethod(FOO, parameterType).invoke(instance, parameter), (Matcher) matcher);
        assertThat((Integer) instrumented.getMethod(AbstractCallHistoryTraceable.METHOD_NAME).invoke(instance), is(1));
        verify(typeDescription, atLeast(1)).getSuperClassInternalName();
        verifyNoMoreInteractions(typeDescription);
    }
}
