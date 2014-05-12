package net.bytebuddy.benchmark;

import net.bytebuddy.benchmark.specimen.ExampleClass;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.logic.BlackHole;

import java.util.concurrent.TimeUnit;

/**
 * This benchmark measures the invocation speed of stub method invocations. All classes implement
 * {@link net.bytebuddy.benchmark.specimen.ExampleClass} and call this class's super method invocation. Since it
 * is not possible to create a subclass with the JDK proxy utilities, the latter is omitted from the benchmark.
 */
@State(Scope.Benchmark)
public class SuperClassInvocationBenchmark {

    private static final String STRING_VALUE = "foo";
    private static final boolean BOOLEAN_VALUE = true;
    private static final byte BYTE_VALUE = 42;
    private static final short SHORT_VALUE = 42;
    private static final char CHAR_VALUE = '@';
    private static final int INT_VALUE = 42;
    private static final long LONG_VALUE = 42L;
    private static final float FLOAT_VALUE = 42f;
    private static final double DOUBLE_VALUE = 42d;

    private ExampleClass byteBuddyWithAnnotationsInstance;
    private ExampleClass byteBuddySpecializedInstance;
    private ExampleClass cglibInstance;
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
    public void benchmarkByteBuddyWithAnnotations(BlackHole blackHole) {
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
    public void benchmarkByteBuddySpecialized(BlackHole blackHole) {
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
    public void benchmarkCglib(BlackHole blackHole) {
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
    public void benchmarkJavassist(BlackHole blackHole) {
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
