package net.bytebuddy.benchmark;

import net.bytebuddy.benchmark.specimen.ExampleClass;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * This benchmark measures the invocation speed of stub method invocations. All classes implement
 * {@link net.bytebuddy.benchmark.specimen.ExampleClass} and call this class's super method invocation. Since it
 * is not possible to create a subclass with the JDK proxy utilities, the latter is omitted from the benchmark.
 */
@State(Scope.Benchmark)
public class SuperClassInvocationBenchmark {

    /**
     * A generic {@link String} value.
     */
    private static final String STRING_VALUE = "foo";

    /**
     * A generic {@code boolean} value.
     */
    private static final boolean BOOLEAN_VALUE = true;

    /**
     * A generic {@code byte} value.
     */
    private static final byte BYTE_VALUE = 42;

    /**
     * A generic {@code short} value.
     */
    private static final short SHORT_VALUE = 42;

    /**
     * A generic {@code char} value.
     */
    private static final char CHAR_VALUE = '@';

    /**
     * A generic {@code int} value.
     */
    private static final int INT_VALUE = 42;

    /**
     * A generic {@code long} value.
     */
    private static final long LONG_VALUE = 42L;

    /**
     * A generic {@code float} value.
     */
    private static final float FLOAT_VALUE = 42f;

    /**
     * A generic {@code double} value.
     */
    private static final double DOUBLE_VALUE = 42d;

    /**
     * An instance created by Byte Buddy for performing benchmarks on. This instance is created by adding
     * auxiliary classes that allow for an invocation of a method from a delegation target.
     */
    private ExampleClass byteBuddyWithAnnotationsInstance;

    /**
     * An instance created by Byte Buddy for performing benchmarks on. This instance is created by hard-coding
     * a super method invocation into the intercepted method.
     */
    private ExampleClass byteBuddySpecializedInstance;

    /**
     * An instance created by cglib for performing benchmarks on.
     */
    private ExampleClass cglibInstance;

    /**
     * An instance created by javassist for performing benchmarks on.
     */
    private ExampleClass javassistInstance;

    /**
     * Creates an instance for each code generation library. The classes are extracted from the
     * {@link net.bytebuddy.benchmark.ClassByExtensionBenchmark}.
     *
     * @throws Exception Covers the exception declarations of the setup methods.
     */
    @Setup
    public void setUp() throws Exception {
        ClassByExtensionBenchmark classByExtensionBenchmark = new ClassByExtensionBenchmark();
        byteBuddyWithAnnotationsInstance = classByExtensionBenchmark.benchmarkByteBuddyWithAnnotations();
        byteBuddySpecializedInstance = classByExtensionBenchmark.benchmarkByteBuddySpecialized();
        cglibInstance = classByExtensionBenchmark.benchmarkCglib();
        javassistInstance = classByExtensionBenchmark.benchmarkJavassist();
    }

    /**
     * Performs a benchmark of a super method invocation using Byte Buddy. This benchmark uses an annotation-based
     * approach which is by its reflective nature more difficult to optimize by the JIT compiler.
     *
     * @param blackHole A black hole for avoiding JIT erasure.
     */
    @GenerateMicroBenchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @OperationsPerInvocation(20)
    public void benchmarkByteBuddyWithAnnotations(Blackhole blackHole) {
        blackHole.consume(byteBuddyWithAnnotationsInstance.method(BOOLEAN_VALUE));
        blackHole.consume(byteBuddyWithAnnotationsInstance.method(BYTE_VALUE));
        blackHole.consume(byteBuddyWithAnnotationsInstance.method(SHORT_VALUE));
        blackHole.consume(byteBuddyWithAnnotationsInstance.method(INT_VALUE));
        blackHole.consume(byteBuddyWithAnnotationsInstance.method(CHAR_VALUE));
        blackHole.consume(byteBuddyWithAnnotationsInstance.method(INT_VALUE));
        blackHole.consume(byteBuddyWithAnnotationsInstance.method(LONG_VALUE));
        blackHole.consume(byteBuddyWithAnnotationsInstance.method(FLOAT_VALUE));
        blackHole.consume(byteBuddyWithAnnotationsInstance.method(DOUBLE_VALUE));
        blackHole.consume(byteBuddyWithAnnotationsInstance.method(STRING_VALUE));
        blackHole.consume(byteBuddyWithAnnotationsInstance.method(BOOLEAN_VALUE, BOOLEAN_VALUE, BOOLEAN_VALUE));
        blackHole.consume(byteBuddyWithAnnotationsInstance.method(BYTE_VALUE, BYTE_VALUE, BYTE_VALUE));
        blackHole.consume(byteBuddyWithAnnotationsInstance.method(SHORT_VALUE, SHORT_VALUE, SHORT_VALUE));
        blackHole.consume(byteBuddyWithAnnotationsInstance.method(INT_VALUE, INT_VALUE, INT_VALUE));
        blackHole.consume(byteBuddyWithAnnotationsInstance.method(CHAR_VALUE, CHAR_VALUE, CHAR_VALUE));
        blackHole.consume(byteBuddyWithAnnotationsInstance.method(INT_VALUE, INT_VALUE, INT_VALUE));
        blackHole.consume(byteBuddyWithAnnotationsInstance.method(LONG_VALUE, LONG_VALUE, LONG_VALUE));
        blackHole.consume(byteBuddyWithAnnotationsInstance.method(FLOAT_VALUE, FLOAT_VALUE, FLOAT_VALUE));
        blackHole.consume(byteBuddyWithAnnotationsInstance.method(DOUBLE_VALUE, DOUBLE_VALUE, DOUBLE_VALUE));
        blackHole.consume(byteBuddyWithAnnotationsInstance.method(STRING_VALUE, STRING_VALUE, STRING_VALUE));
    }

    /**
     * Performs a benchmark of a super method invocation using Byte Buddy. This benchmark uses a specialized
     * interception strategy which is easier to inline by the compiler.
     *
     * @param blackHole A black hole for avoiding JIT erasure.
     */
    @GenerateMicroBenchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @OperationsPerInvocation(20)
    public void benchmarkByteBuddySpecialized(Blackhole blackHole) {
        blackHole.consume(byteBuddySpecializedInstance.method(BOOLEAN_VALUE));
        blackHole.consume(byteBuddySpecializedInstance.method(BYTE_VALUE));
        blackHole.consume(byteBuddySpecializedInstance.method(SHORT_VALUE));
        blackHole.consume(byteBuddySpecializedInstance.method(INT_VALUE));
        blackHole.consume(byteBuddySpecializedInstance.method(CHAR_VALUE));
        blackHole.consume(byteBuddySpecializedInstance.method(INT_VALUE));
        blackHole.consume(byteBuddySpecializedInstance.method(LONG_VALUE));
        blackHole.consume(byteBuddySpecializedInstance.method(FLOAT_VALUE));
        blackHole.consume(byteBuddySpecializedInstance.method(DOUBLE_VALUE));
        blackHole.consume(byteBuddySpecializedInstance.method(STRING_VALUE));
        blackHole.consume(byteBuddySpecializedInstance.method(BOOLEAN_VALUE, BOOLEAN_VALUE, BOOLEAN_VALUE));
        blackHole.consume(byteBuddySpecializedInstance.method(BYTE_VALUE, BYTE_VALUE, BYTE_VALUE));
        blackHole.consume(byteBuddySpecializedInstance.method(SHORT_VALUE, SHORT_VALUE, SHORT_VALUE));
        blackHole.consume(byteBuddySpecializedInstance.method(INT_VALUE, INT_VALUE, INT_VALUE));
        blackHole.consume(byteBuddySpecializedInstance.method(CHAR_VALUE, CHAR_VALUE, CHAR_VALUE));
        blackHole.consume(byteBuddySpecializedInstance.method(INT_VALUE, INT_VALUE, INT_VALUE));
        blackHole.consume(byteBuddySpecializedInstance.method(LONG_VALUE, LONG_VALUE, LONG_VALUE));
        blackHole.consume(byteBuddySpecializedInstance.method(FLOAT_VALUE, FLOAT_VALUE, FLOAT_VALUE));
        blackHole.consume(byteBuddySpecializedInstance.method(DOUBLE_VALUE, DOUBLE_VALUE, DOUBLE_VALUE));
        blackHole.consume(byteBuddySpecializedInstance.method(STRING_VALUE, STRING_VALUE, STRING_VALUE));
    }

    /**
     * Performs a benchmark of a super method invocation using cglib.
     *
     * @param blackHole A black hole for avoiding JIT erasure.
     */
    @GenerateMicroBenchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @OperationsPerInvocation(20)
    public void benchmarkCglib(Blackhole blackHole) {
        blackHole.consume(cglibInstance.method(BOOLEAN_VALUE));
        blackHole.consume(cglibInstance.method(BYTE_VALUE));
        blackHole.consume(cglibInstance.method(SHORT_VALUE));
        blackHole.consume(cglibInstance.method(INT_VALUE));
        blackHole.consume(cglibInstance.method(CHAR_VALUE));
        blackHole.consume(cglibInstance.method(INT_VALUE));
        blackHole.consume(cglibInstance.method(LONG_VALUE));
        blackHole.consume(cglibInstance.method(FLOAT_VALUE));
        blackHole.consume(cglibInstance.method(DOUBLE_VALUE));
        blackHole.consume(cglibInstance.method(STRING_VALUE));
        blackHole.consume(cglibInstance.method(BOOLEAN_VALUE, BOOLEAN_VALUE, BOOLEAN_VALUE));
        blackHole.consume(cglibInstance.method(BYTE_VALUE, BYTE_VALUE, BYTE_VALUE));
        blackHole.consume(cglibInstance.method(SHORT_VALUE, SHORT_VALUE, SHORT_VALUE));
        blackHole.consume(cglibInstance.method(INT_VALUE, INT_VALUE, INT_VALUE));
        blackHole.consume(cglibInstance.method(CHAR_VALUE, CHAR_VALUE, CHAR_VALUE));
        blackHole.consume(cglibInstance.method(INT_VALUE, INT_VALUE, INT_VALUE));
        blackHole.consume(cglibInstance.method(LONG_VALUE, LONG_VALUE, LONG_VALUE));
        blackHole.consume(cglibInstance.method(FLOAT_VALUE, FLOAT_VALUE, FLOAT_VALUE));
        blackHole.consume(cglibInstance.method(DOUBLE_VALUE, DOUBLE_VALUE, DOUBLE_VALUE));
        blackHole.consume(cglibInstance.method(STRING_VALUE, STRING_VALUE, STRING_VALUE));
    }

    /**
     * Performs a benchmark of a super method invocation using javassist.
     *
     * @param blackHole A black hole for avoiding JIT erasure.
     */
    @GenerateMicroBenchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @OperationsPerInvocation(20)
    public void benchmarkJavassist(Blackhole blackHole) {
        blackHole.consume(javassistInstance.method(BOOLEAN_VALUE));
        blackHole.consume(javassistInstance.method(BYTE_VALUE));
        blackHole.consume(javassistInstance.method(SHORT_VALUE));
        blackHole.consume(javassistInstance.method(INT_VALUE));
        blackHole.consume(javassistInstance.method(CHAR_VALUE));
        blackHole.consume(javassistInstance.method(INT_VALUE));
        blackHole.consume(javassistInstance.method(LONG_VALUE));
        blackHole.consume(javassistInstance.method(FLOAT_VALUE));
        blackHole.consume(javassistInstance.method(DOUBLE_VALUE));
        blackHole.consume(javassistInstance.method(STRING_VALUE));
        blackHole.consume(javassistInstance.method(BOOLEAN_VALUE, BOOLEAN_VALUE, BOOLEAN_VALUE));
        blackHole.consume(javassistInstance.method(BYTE_VALUE, BYTE_VALUE, BYTE_VALUE));
        blackHole.consume(javassistInstance.method(SHORT_VALUE, SHORT_VALUE, SHORT_VALUE));
        blackHole.consume(javassistInstance.method(INT_VALUE, INT_VALUE, INT_VALUE));
        blackHole.consume(javassistInstance.method(CHAR_VALUE, CHAR_VALUE, CHAR_VALUE));
        blackHole.consume(javassistInstance.method(INT_VALUE, INT_VALUE, INT_VALUE));
        blackHole.consume(javassistInstance.method(LONG_VALUE, LONG_VALUE, LONG_VALUE));
        blackHole.consume(javassistInstance.method(FLOAT_VALUE, FLOAT_VALUE, FLOAT_VALUE));
        blackHole.consume(javassistInstance.method(DOUBLE_VALUE, DOUBLE_VALUE, DOUBLE_VALUE));
        blackHole.consume(javassistInstance.method(STRING_VALUE, STRING_VALUE, STRING_VALUE));
    }
}
