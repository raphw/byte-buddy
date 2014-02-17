package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.annotation.IgnoreForBinding;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.InstrumentedType;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class MethodDelegationSimpleTest {

    private static final String FOO = "foo", BAR = "bar";

    private static final int ARGUMENT_VALUE = 21, MULTIPLICATOR = 2, RESULT = ARGUMENT_VALUE * MULTIPLICATOR;

    private InstrumentedType instrumentedType;

    @Before
    public void setUp() throws Exception {
        instrumentedType = mock(InstrumentedType.class);
        SimpleDelegationTarget.clearStackTraceRecord();
    }

    @SuppressWarnings("unused")
    public static class SimpleDelegationTarget {

        private static class ParameterizedStackTraceElement {

            private final StackTraceElement stackTraceElement;
            private final Class<?>[] parameterTypes;

            private ParameterizedStackTraceElement(StackTraceElement stackTraceElement,
                                                   Class<?>[] parameterTypes) {
                this.stackTraceElement = stackTraceElement;
                this.parameterTypes = parameterTypes;
            }

            public StackTraceElement getStackTraceElement() {
                return stackTraceElement;
            }

            public Class<?>[] getParameterTypes() {
                return parameterTypes;
            }
        }

        private static final List<ParameterizedStackTraceElement> STACK_TRACE_RECORD = new LinkedList<ParameterizedStackTraceElement>();

        @IgnoreForBinding
        private static void recordStackTrace(Class<?>... parameterType) {
            STACK_TRACE_RECORD.add(new ParameterizedStackTraceElement(Thread.currentThread().getStackTrace()[2], parameterType));
        }

        @IgnoreForBinding
        public static void clearStackTraceRecord() {
            STACK_TRACE_RECORD.clear();
        }

        @IgnoreForBinding
        public static void assertCallRecord(String name, Class<?>[] parameterType) {
            assertThat(STACK_TRACE_RECORD.size(), is(1));
            assertThat(STACK_TRACE_RECORD.get(0).getStackTraceElement().getMethodName(), is(name));
            assertThat(STACK_TRACE_RECORD.get(0).getParameterTypes(), equalTo(parameterType));
        }

        public static boolean bar(boolean a) {
            recordStackTrace(boolean.class);
            return true;
        }

        public static byte bar(byte a) {
            recordStackTrace(byte.class);
            return (byte) (a * MULTIPLICATOR);
        }

        public static short bar(short a) {
            recordStackTrace(short.class);
            return (short) (a * MULTIPLICATOR);
        }

        public static int bar(int a) {
            recordStackTrace(int.class);
            return a * MULTIPLICATOR;
        }

        public static char bar(char a) {
            recordStackTrace(char.class);
            return (char) (a * MULTIPLICATOR);
        }

        public static long bar(long a) {
            recordStackTrace(long.class);
            return a * MULTIPLICATOR;
        }

        public static float bar(float a) {
            recordStackTrace(float.class);
            return a * MULTIPLICATOR;
        }

        public static double bar(double a) {
            recordStackTrace(double.class);
            return a * MULTIPLICATOR;
        }

        public static Object bar(Object a) {
            recordStackTrace(Object.class);
            return FOO + a;
        }

        public static void bar() {
            recordStackTrace();
        }
    }

    @SuppressWarnings("unused")
    public static class BooleanDelegationSource {

        public boolean foo(boolean a) {
            return false;
        }
    }

    @Test
    public void testBooleanDelegation() throws Exception {
        testDirectDelegation(BooleanDelegationSource.class,
                SimpleDelegationTarget.class,
                is(true),
                new Class<?>[]{boolean.class},
                new Object[]{true});
    }

    @SuppressWarnings("unused")
    public static class ByteDelegationSource {

        public byte foo(byte a) {
            return (byte) ARGUMENT_VALUE;
        }
    }

    @Test
    public void testByteDelegation() throws Exception {
        testDirectDelegation(ByteDelegationSource.class,
                SimpleDelegationTarget.class,
                is((byte) RESULT),
                new Class<?>[]{byte.class},
                new Object[]{(byte) ARGUMENT_VALUE});
    }

    @SuppressWarnings("unused")
    public static class ShortDelegationSource {

        public short foo(short a) {
            return (short) ARGUMENT_VALUE;
        }
    }

    @Test
    public void testShortDelegation() throws Exception {
        testDirectDelegation(ShortDelegationSource.class,
                SimpleDelegationTarget.class,
                is((short) RESULT),
                new Class<?>[]{short.class},
                new Object[]{(short) ARGUMENT_VALUE});
    }

    @SuppressWarnings("unused")
    public static class IntDelegationSource {

        public int foo(int a) {
            return ARGUMENT_VALUE;
        }
    }

    @Test
    public void testIntDelegation() throws Exception {
        testDirectDelegation(IntDelegationSource.class,
                SimpleDelegationTarget.class,
                is(RESULT),
                new Class<?>[]{int.class},
                new Object[]{ARGUMENT_VALUE});
    }

    @SuppressWarnings("unused")
    public static class LongDelegationSource {

        public long foo(long a) {
            return (long) ARGUMENT_VALUE;
        }
    }

    @Test
    public void testLongDelegation() throws Exception {
        testDirectDelegation(LongDelegationSource.class,
                SimpleDelegationTarget.class,
                is((long) RESULT),
                new Class<?>[]{long.class},
                new Object[]{(long) ARGUMENT_VALUE});
    }

    @SuppressWarnings("unused")
    public static class FloatDelegationSource {

        public float foo(float a) {
            return (short) ARGUMENT_VALUE;
        }
    }

    @Test
    public void testFloatDelegation() throws Exception {
        testDirectDelegation(FloatDelegationSource.class,
                SimpleDelegationTarget.class,
                is((float) RESULT),
                new Class<?>[]{float.class},
                new Object[]{(float) ARGUMENT_VALUE});
    }

    @SuppressWarnings("unused")
    public static class DoubleDelegationSource {

        public double foo(double a) {
            return (double) ARGUMENT_VALUE;
        }
    }

    @Test
    public void testDoubleDelegation() throws Exception {
        testDirectDelegation(DoubleDelegationSource.class,
                SimpleDelegationTarget.class,
                is((double) RESULT),
                new Class<?>[]{double.class},
                new Object[]{(double) ARGUMENT_VALUE});
    }

    @SuppressWarnings("unused")
    public static class VoidDelegationSource {

        public void foo() {
            /* empty */
        }
    }

    @Test
    public void testVoidDelegation() throws Exception {
        testDirectDelegation(VoidDelegationSource.class,
                SimpleDelegationTarget.class,
                nullValue(Void.class),
                new Class<?>[0],
                new Object[0]);
    }

    @SuppressWarnings("unused")
    public static class ReferenceDelegationSource {

        public Object foo(Object a) {
            return a;
        }
    }

    @Test
    public void testReferenceDelegation() throws Exception {
        testDirectDelegation(ReferenceDelegationSource.class,
                SimpleDelegationTarget.class,
                is(FOO + BAR),
                new Class<?>[]{Object.class},
                new Object[]{BAR});
    }

    @SuppressWarnings("unchecked")
    private void testDirectDelegation(Class<?> sourceType,
                                      Class<?> targetType,
                                      Matcher<?> matcher,
                                      Class<?>[] parameterType,
                                      Object[] parameter) throws Exception {
        ByteCodeAppenderFactoryTester tester = new ByteCodeAppenderFactoryTester(MethodDelegation.to(targetType),
                instrumentedType,
                sourceType);
        MethodDescription methodDescription = new MethodDescription.ForMethod(sourceType.getDeclaredMethod(FOO, parameterType));
        MethodDescription spied = spy(methodDescription);
        Class<?> instrumented = tester.applyTo(spied, methodDescription);
        assertEquals(sourceType, instrumented.getSuperclass());
        assertThat(instrumented.getDeclaredMethods().length, is(1));
        Object instance = instrumented.getDeclaredConstructor().newInstance();
        assertThat(instrumented.getDeclaredMethod(FOO, parameterType).invoke(instance, parameter), (Matcher<Object>) matcher);
        SimpleDelegationTarget.assertCallRecord(BAR, parameterType);
        verifyZeroInteractions(instrumentedType);
    }
}
