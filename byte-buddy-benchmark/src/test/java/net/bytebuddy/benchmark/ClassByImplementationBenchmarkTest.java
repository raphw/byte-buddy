package net.bytebuddy.benchmark;

import net.bytebuddy.benchmark.specimen.ExampleInterface;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class ClassByImplementationBenchmarkTest {

    private static final boolean BOOLEAN_VALUE = true;

    private static final byte BYTE_VALUE = 42;

    private static final short SHORT_VALUE = 42;

    private static final char CHAR_VALUE = '@';

    private static final int INT_VALUE = 42;

    private static final long LONG_VALUE = 42L;

    private static final float FLOAT_VALUE = 42f;

    private static final double DOUBLE_VALUE = 42d;

    private static final Object REFERENCE_VALUE = "foo";

    private ClassByImplementationBenchmark classByImplementationBenchmark;

    private static void assertReturnValues(ExampleInterface exampleInterface) {
        assertThat(exampleInterface.method(BOOLEAN_VALUE), is(ClassByImplementationBenchmark.DEFAULT_BOOLEAN_VALUE));
        assertThat(exampleInterface.method(BYTE_VALUE), is(ClassByImplementationBenchmark.DEFAULT_BYTE_VALUE));
        assertThat(exampleInterface.method(SHORT_VALUE), is(ClassByImplementationBenchmark.DEFAULT_SHORT_VALUE));
        assertThat(exampleInterface.method(CHAR_VALUE), is(ClassByImplementationBenchmark.DEFAULT_CHAR_VALUE));
        assertThat(exampleInterface.method(INT_VALUE), is(ClassByImplementationBenchmark.DEFAULT_INT_VALUE));
        assertThat(exampleInterface.method(LONG_VALUE), is(ClassByImplementationBenchmark.DEFAULT_LONG_VALUE));
        assertThat(exampleInterface.method(FLOAT_VALUE), is(ClassByImplementationBenchmark.DEFAULT_FLOAT_VALUE));
        assertThat(exampleInterface.method(DOUBLE_VALUE), is(ClassByImplementationBenchmark.DEFAULT_DOUBLE_VALUE));
        assertThat(exampleInterface.method(REFERENCE_VALUE), is((Object) ClassByImplementationBenchmark.DEFAULT_REFERENCE_VALUE));
        assertThat(exampleInterface.method(BOOLEAN_VALUE, BOOLEAN_VALUE, BOOLEAN_VALUE),
                is((boolean[]) (Object) ClassByImplementationBenchmark.DEFAULT_REFERENCE_VALUE));
        assertThat(exampleInterface.method(BYTE_VALUE, BYTE_VALUE, BYTE_VALUE),
                is((byte[]) (Object) ClassByImplementationBenchmark.DEFAULT_REFERENCE_VALUE));
        assertThat(exampleInterface.method(SHORT_VALUE, SHORT_VALUE, SHORT_VALUE),
                is((short[]) (Object) ClassByImplementationBenchmark.DEFAULT_REFERENCE_VALUE));
        assertThat(exampleInterface.method(CHAR_VALUE, CHAR_VALUE, CHAR_VALUE),
                is((char[]) (Object) ClassByImplementationBenchmark.DEFAULT_REFERENCE_VALUE));
        assertThat(exampleInterface.method(INT_VALUE, INT_VALUE, INT_VALUE),
                is((int[]) (Object) ClassByImplementationBenchmark.DEFAULT_REFERENCE_VALUE));
        assertThat(exampleInterface.method(LONG_VALUE, LONG_VALUE, LONG_VALUE),
                is((long[]) (Object) ClassByImplementationBenchmark.DEFAULT_REFERENCE_VALUE));
        assertThat(exampleInterface.method(FLOAT_VALUE, FLOAT_VALUE, FLOAT_VALUE),
                is((float[]) (Object) ClassByImplementationBenchmark.DEFAULT_REFERENCE_VALUE));
        assertThat(exampleInterface.method(DOUBLE_VALUE, DOUBLE_VALUE, DOUBLE_VALUE),
                is((double[]) (Object) ClassByImplementationBenchmark.DEFAULT_REFERENCE_VALUE));
        assertThat(exampleInterface.method(REFERENCE_VALUE, REFERENCE_VALUE, REFERENCE_VALUE),
                is((Object[]) (Object) ClassByImplementationBenchmark.DEFAULT_REFERENCE_VALUE));
    }

    @Before
    public void setUp() throws Exception {
        classByImplementationBenchmark = new ClassByImplementationBenchmark();
    }

    @Test
    public void testBaseline() throws Exception {
        ExampleInterface instance = classByImplementationBenchmark.baseline();
        assertThat(Arrays.asList(instance.getClass().getInterfaces()), hasItem(ClassByImplementationBenchmark.BASE_CLASS));
        assertThat(instance.getClass().getSuperclass(), CoreMatchers.<Class<?>>is(Object.class));
        assertThat(classByImplementationBenchmark.benchmarkByteBuddy().getClass(), not(CoreMatchers.<Class<?>>is(instance.getClass())));
        assertReturnValues(instance);
    }

    @Test
    public void testByteBuddyClassCreation() throws Exception {
        ExampleInterface instance = classByImplementationBenchmark.benchmarkByteBuddy();
        assertThat(Arrays.asList(instance.getClass().getInterfaces()), hasItem(ClassByImplementationBenchmark.BASE_CLASS));
        assertThat(instance.getClass().getSuperclass(), CoreMatchers.<Class<?>>is(Object.class));
        assertThat(classByImplementationBenchmark.benchmarkByteBuddy().getClass(), not(CoreMatchers.<Class<?>>is(instance.getClass())));
        assertReturnValues(instance);
    }

    @Test
    public void testCglibClassCreation() throws Exception {
        ExampleInterface instance = classByImplementationBenchmark.benchmarkCglib();
        assertThat(Arrays.asList(instance.getClass().getInterfaces()), hasItem(ClassByImplementationBenchmark.BASE_CLASS));
        assertThat(instance.getClass().getSuperclass(), CoreMatchers.<Class<?>>is(Object.class));
        assertThat(classByImplementationBenchmark.benchmarkCglib().getClass(), not(CoreMatchers.<Class<?>>is(instance.getClass())));
        assertReturnValues(instance);
    }

    @Test
    public void testJavassistClassCreation() throws Exception {
        ExampleInterface instance = classByImplementationBenchmark.benchmarkJavassist();
        assertThat(Arrays.asList(instance.getClass().getInterfaces()), hasItem(ClassByImplementationBenchmark.BASE_CLASS));
        assertThat(instance.getClass().getSuperclass(), CoreMatchers.<Class<?>>is(Object.class));
        assertThat(classByImplementationBenchmark.benchmarkJavassist().getClass(), not(CoreMatchers.<Class<?>>is(instance.getClass())));
        assertReturnValues(instance);
    }

    @Test
    public void testJdkProxyClassCreation() throws Exception {
        ExampleInterface instance = classByImplementationBenchmark.benchmarkJdkProxy();
        assertThat(Arrays.asList(instance.getClass().getInterfaces()), hasItem(ClassByImplementationBenchmark.BASE_CLASS));
        assertThat(instance.getClass().getSuperclass(), CoreMatchers.<Class<?>>is(Proxy.class));
        assertThat(classByImplementationBenchmark.benchmarkByteBuddy().getClass(), not(CoreMatchers.<Class<?>>is(instance.getClass())));
        assertReturnValues(instance);
    }
}
