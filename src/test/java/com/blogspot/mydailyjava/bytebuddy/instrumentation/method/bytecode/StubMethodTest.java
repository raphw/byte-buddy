package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.InstrumentedType0;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class StubMethodTest {

    private static final String FOO = "foo";
    private static final int NON_DEFAULT_VALUE = 42;
    private static final int DEFAULT_VALUE = 0;

    private InstrumentedType0 instrumentedType;

    @Before
    public void setUp() throws Exception {
        instrumentedType = mock(InstrumentedType0.class);
    }

    @SuppressWarnings("unused")
    public static class StubbedReference extends AbstractCallHistoryTraceable {

        public Object foo() {
            markCalled();
            return FOO;
        }
    }

    @Test
    public void testStubMethodReference() throws Exception {
        testStubbing(StubbedReference.class, nullValue(Object.class));
    }

    @SuppressWarnings("unused")
    public static class StubbedInt extends AbstractCallHistoryTraceable {

        public int foo() {
            markCalled();
            return NON_DEFAULT_VALUE;
        }
    }

    @Test
    public void testStubMethodInt() throws Exception {
        testStubbing(StubbedInt.class, is(DEFAULT_VALUE));
    }

    @SuppressWarnings("unused")
    public static class StubbedLong extends AbstractCallHistoryTraceable {

        public long foo() {
            markCalled();
            return (long) NON_DEFAULT_VALUE;
        }
    }

    @Test
    public void testStubMethodLong() throws Exception {
        testStubbing(StubbedLong.class, is((long) DEFAULT_VALUE));
    }

    @SuppressWarnings("unused")
    public static class StubbedShort extends AbstractCallHistoryTraceable {

        public short foo() {
            markCalled();
            return (short) NON_DEFAULT_VALUE;
        }
    }

    @Test
    public void testStubMethodShort() throws Exception {
        testStubbing(StubbedShort.class, is((short) DEFAULT_VALUE));
    }

    @SuppressWarnings("unused")
    public static class StubbedByte extends AbstractCallHistoryTraceable {

        public byte foo() {
            markCalled();
            return (byte) NON_DEFAULT_VALUE;
        }
    }

    @Test
    public void testStubMethodByte() throws Exception {
        testStubbing(StubbedByte.class, is((byte) DEFAULT_VALUE));
    }

    @SuppressWarnings("unused")
    public static class StubbedFloat extends AbstractCallHistoryTraceable {

        public float foo() {
            markCalled();
            return (float) NON_DEFAULT_VALUE;
        }
    }

    @Test
    public void testStubMethodFloat() throws Exception {
        testStubbing(StubbedFloat.class, is((float) DEFAULT_VALUE));
    }

    @SuppressWarnings("unused")
    public static class StubbedDouble extends AbstractCallHistoryTraceable {

        public double foo() {
            markCalled();
            return (double) NON_DEFAULT_VALUE;
        }
    }

    @Test
    public void testStubMethodDouble() throws Exception {
        testStubbing(StubbedDouble.class, is((double) DEFAULT_VALUE));
    }

    @SuppressWarnings("unused")
    public static class StubbedChar extends AbstractCallHistoryTraceable {

        public char foo() {
            markCalled();
            return (char) NON_DEFAULT_VALUE;
        }
    }

    @Test
    public void testStubMethodChar() throws Exception {
        testStubbing(StubbedChar.class, is((char) DEFAULT_VALUE));
    }

    @SuppressWarnings("unused")
    public static class StubbedBoolean extends AbstractCallHistoryTraceable {

        public boolean foo() {
            markCalled();
            return true;
        }
    }

    @Test
    public void testStubMethodBoolean() throws Exception {
        testStubbing(StubbedBoolean.class, is(false));
    }

    @SuppressWarnings("unused")
    public static class StubbedVoid extends AbstractCallHistoryTraceable {

        public void foo() {
            markCalled();
        }
    }

    @Test
    public void testStubMethodVoid() throws Exception {
        testStubbing(StubbedVoid.class, nullValue(Void.class));
    }

    @SuppressWarnings("unused")
    public static class StubWithArguments extends AbstractCallHistoryTraceable {

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
                (long) NON_DEFAULT_VALUE, FOO};
        testStubbing(StubWithArguments.class,
                nullValue(Object[].class),
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

    private void testStubbing(Class<?> type, Matcher<?> matcher) throws Exception {
        testStubbing(type, matcher, new Class<?>[0], new Object[0]);
    }

    @SuppressWarnings("unchecked")
    private void testStubbing(Class<?> type, Matcher<?> matcher, Class<?>[] parameterType, Object[] parameter) throws Exception {
        assertThat("Arguments cannot produce valid result", parameterType.length, is(parameter.length));
        ByteCodeAppenderFactoryTester tester = new ByteCodeAppenderFactoryTester(StubMethod.INSTANCE, instrumentedType, type);
        MethodDescription methodDescription = new MethodDescription.ForMethod(type.getDeclaredMethod(FOO, parameterType));
        MethodDescription spied = spy(methodDescription);
        Class<?> instrumented = tester.applyTo(spied, methodDescription);
        assertEquals(type, instrumented.getSuperclass());
        assertThat(instrumented.getDeclaredMethods().length, is(1));
        Object instance = instrumented.getDeclaredConstructor().newInstance();
        assertThat(instrumented.getDeclaredMethod(FOO, parameterType).invoke(instance, parameter), (Matcher) matcher);
        assertThat((Integer) instrumented.getMethod(AbstractCallHistoryTraceable.METHOD_NAME).invoke(instance), is(0));
        verify(spied, atLeast(1)).getReturnType();
        verify(spied, atLeast(1)).getStackSize();
        verifyZeroInteractions(instrumentedType);
    }
}
