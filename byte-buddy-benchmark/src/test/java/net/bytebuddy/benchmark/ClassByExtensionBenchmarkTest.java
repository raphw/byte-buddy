package net.bytebuddy.benchmark;

import net.bytebuddy.benchmark.specimen.ExampleClass;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class ClassByExtensionBenchmarkTest {

    private static final boolean BOOLEAN_VALUE = true;

    private static final byte BYTE_VALUE = 42;

    private static final short SHORT_VALUE = 42;

    private static final char CHAR_VALUE = '@';

    private static final int INT_VALUE = 42;

    private static final long LONG_VALUE = 42L;

    private static final float FLOAT_VALUE = 42f;

    private static final double DOUBLE_VALUE = 42d;

    private static final Object REFERENCE_VALUE = "foo";

    private ClassByExtensionBenchmark classByExtensionBenchmark;

    private static void assertReturnValues(ExampleClass exampleClass) {
        assertThat(exampleClass.method(BOOLEAN_VALUE), is(BOOLEAN_VALUE));
        assertThat(exampleClass.method(BYTE_VALUE), is(BYTE_VALUE));
        assertThat(exampleClass.method(SHORT_VALUE), is(SHORT_VALUE));
        assertThat(exampleClass.method(CHAR_VALUE), is(CHAR_VALUE));
        assertThat(exampleClass.method(INT_VALUE), is(INT_VALUE));
        assertThat(exampleClass.method(LONG_VALUE), is(LONG_VALUE));
        assertThat(exampleClass.method(FLOAT_VALUE), is(FLOAT_VALUE));
        assertThat(exampleClass.method(DOUBLE_VALUE), is(DOUBLE_VALUE));
        assertThat(exampleClass.method(REFERENCE_VALUE), is(REFERENCE_VALUE));
        assertThat(exampleClass.method(BOOLEAN_VALUE, BOOLEAN_VALUE, BOOLEAN_VALUE),
                is(new boolean[]{BOOLEAN_VALUE, BOOLEAN_VALUE, BOOLEAN_VALUE}));
        assertThat(exampleClass.method(BYTE_VALUE, BYTE_VALUE, BYTE_VALUE),
                is(new byte[]{BYTE_VALUE, BYTE_VALUE, BYTE_VALUE}));
        assertThat(exampleClass.method(SHORT_VALUE, SHORT_VALUE, SHORT_VALUE),
                is(new short[]{SHORT_VALUE, SHORT_VALUE, SHORT_VALUE}));
        assertThat(exampleClass.method(CHAR_VALUE, CHAR_VALUE, CHAR_VALUE),
                is(new char[]{CHAR_VALUE, CHAR_VALUE, CHAR_VALUE}));
        assertThat(exampleClass.method(INT_VALUE, INT_VALUE, INT_VALUE),
                is(new int[]{INT_VALUE, INT_VALUE, INT_VALUE}));
        assertThat(exampleClass.method(LONG_VALUE, LONG_VALUE, LONG_VALUE),
                is(new long[]{LONG_VALUE, LONG_VALUE, LONG_VALUE}));
        assertThat(exampleClass.method(FLOAT_VALUE, FLOAT_VALUE, FLOAT_VALUE),
                is(new float[]{FLOAT_VALUE, FLOAT_VALUE, FLOAT_VALUE}));
        assertThat(exampleClass.method(DOUBLE_VALUE, DOUBLE_VALUE, DOUBLE_VALUE),
                is(new double[]{DOUBLE_VALUE, DOUBLE_VALUE, DOUBLE_VALUE}));
        assertThat(exampleClass.method(REFERENCE_VALUE, REFERENCE_VALUE, REFERENCE_VALUE),
                is(new Object[]{REFERENCE_VALUE, REFERENCE_VALUE, REFERENCE_VALUE}));
    }

    @Before
    public void setUp() throws Exception {
        classByExtensionBenchmark = new ClassByExtensionBenchmark();
    }

    @Test
    public void testBaseline() throws Exception {
        ExampleClass instance = classByExtensionBenchmark.baseline();
        assertThat(instance.getClass(), CoreMatchers.<Class<?>>is(ClassByExtensionBenchmark.BASE_CLASS));
        assertReturnValues(instance);
    }

    @Test
    public void testByteBuddyWithAnnotationsClassCreation() throws Exception {
        ExampleClass instance = classByExtensionBenchmark.benchmarkByteBuddyWithAnnotations();
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(ClassByExtensionBenchmark.BASE_CLASS)));
        assertThat(instance.getClass().getSuperclass(), CoreMatchers.<Class<?>>is(ClassByExtensionBenchmark.BASE_CLASS));
        assertThat(classByExtensionBenchmark.benchmarkByteBuddyWithAnnotations().getClass(), not(CoreMatchers.<Class<?>>is(instance.getClass())));
        assertReturnValues(instance);
    }

    @Test
    public void testByteBuddySpecializedClassCreation() throws Exception {
        ExampleClass instance = classByExtensionBenchmark.benchmarkByteBuddySpecialized();
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(ClassByExtensionBenchmark.BASE_CLASS)));
        assertThat(instance.getClass().getSuperclass(), CoreMatchers.<Class<?>>is(ClassByExtensionBenchmark.BASE_CLASS));
        assertThat(classByExtensionBenchmark.benchmarkByteBuddySpecialized().getClass(), not(CoreMatchers.<Class<?>>is(instance.getClass())));
        assertReturnValues(instance);
    }

    @Test
    public void testCglibClassCreation() throws Exception {
        ExampleClass instance = classByExtensionBenchmark.benchmarkCglib();
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(ClassByExtensionBenchmark.BASE_CLASS)));
        assertThat(instance.getClass().getSuperclass(), CoreMatchers.<Class<?>>is(ClassByExtensionBenchmark.BASE_CLASS));
        assertThat(classByExtensionBenchmark.benchmarkCglib().getClass(), not(CoreMatchers.<Class<?>>is(instance.getClass())));
        assertReturnValues(instance);
    }

    @Test
    public void testJavassistClassCreation() throws Exception {
        ExampleClass instance = classByExtensionBenchmark.benchmarkJavassist();
        assertThat(instance.getClass(), not(CoreMatchers.<Class<?>>is(ClassByExtensionBenchmark.BASE_CLASS)));
        assertThat(instance.getClass().getSuperclass(), CoreMatchers.<Class<?>>is(ClassByExtensionBenchmark.BASE_CLASS));
        assertThat(classByExtensionBenchmark.benchmarkJavassist().getClass(), not(CoreMatchers.<Class<?>>is(instance.getClass())));
        assertReturnValues(instance);
    }
}
