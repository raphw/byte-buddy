package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.annotation.Argument;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.annotation.This;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.InstrumentedType;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.spy;

public class MethodDelegationComplexTest {

    private static final String FOO = "foo", BAR = "bar";

    @SuppressWarnings("unused")
    public static class Source {

        public final String foo = FOO;

        public String foo(Void a, String b, int c) {
            return BAR;
        }
    }

    private InstrumentedType instrumentedType;

    @Before
    public void setUp() throws Exception {
//        instrumentedType = new SubclassLoadedTypeInstrumentation(new ClassVersion(Opcodes.V1_6),
//                Source.class,
//                Collections.<Class<?>>emptySet(),
//                Visibility.PUBLIC,
//                TypeManifestation.PLAIN,
//                SyntheticState.NON_SYNTHETIC,
//                new NamingStrategy.PrefixingRandom("utility"));
    }

    @SuppressWarnings("unused")
    public static class AmbiguousTarget {

        public static String bar(@Argument(1) String b) {
            return FOO;
        }

        public static String bar(@Argument(2) int c) {
            return FOO;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParameterTypeAmbiguousBinding() throws Exception {
        makeDelegation(Source.class,
                AmbiguousTarget.class,
                new Class<?>[]{Void.class, String.class, int.class});
    }

    @SuppressWarnings("unused")
    public static class NoMatchingTarget {

        public static String bar(@Argument(10) String e) {
            return FOO;
        }

        public static String bar(@Argument(10) int e) {
            return FOO;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoMatchingTarget() throws Exception {
        makeDelegation(Source.class,
                NoMatchingTarget.class,
                new Class<?>[]{Void.class, String.class, int.class});
    }

    @SuppressWarnings("unused")
    public static class TypeDominantTarget {

        public static String bar(@Argument(1) Object b, @This Source o) {
            return BAR;
        }

        public static String bar(@Argument(1) String b, @This Source o) {
            return FOO + b;
        }
    }

    @Test
    public void testParameterTypeDominantBinding() throws Exception {
        assertThat(makeDelegation(Source.class,
                TypeDominantTarget.class,
                new Class<?>[]{Void.class, String.class, int.class}).foo(null, BAR, 0), is(FOO + BAR));
    }

    @SuppressWarnings("unused")
    public static class ReturnTypeTarget {

        public static Number bar(@Argument(1) String b, @This Object o) {
            return null;
        }

        public static String bar(@Argument(1) String b, @This Source o) {
            return o.foo + b;
        }
    }

    @Test
    public void testReturnTypeConflictBinding() throws Exception {
        assertThat(makeDelegation(Source.class,
                TypeDominantTarget.class,
                new Class<?>[]{Void.class, String.class, int.class}).foo(null, BAR, 0), is(FOO + BAR));
    }

    @SuppressWarnings("unchecked")
    private <T> T makeDelegation(Class<T> sourceType, Class<?> targetType, Class<?>[] parameterType) throws Exception {
        ByteCodeAppenderFactoryTester tester = new ByteCodeAppenderFactoryTester(MethodDelegation.to(targetType), instrumentedType, sourceType);
        MethodDescription methodDescription = new MethodDescription.ForMethod(sourceType.getDeclaredMethod(FOO, parameterType));
        MethodDescription spied = spy(methodDescription);
        Class<?> instrumented = tester.applyTo(spied, methodDescription);
        assertEquals(sourceType, instrumented.getSuperclass());
        assertThat(instrumented.getDeclaredMethods().length, is(1));
        return (T) instrumented.getDeclaredConstructor().newInstance();
    }
}
