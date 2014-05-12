package net.bytebuddy.benchmark;

import net.bytebuddy.benchmark.specimen.ExampleInterface;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.logic.BlackHole;

import java.util.concurrent.TimeUnit;

/**
 * This benchmark measures the invocation speed of stub method invocations. All classes implement
 * {@link net.bytebuddy.benchmark.specimen.ExampleInterface} and implement all methods to return the return type's
 * default value, independently of the arguments.
 */
@State(Scope.Benchmark)
public class StubInvocationBenchmark {

    private static final String STRING_VALUE = "foo";
    private static final boolean BOOLEAN_VALUE = true;
    private static final byte BYTE_VALUE = 42;
    private static final short SHORT_VALUE = 42;
    private static final char CHAR_VALUE = '@';
    private static final int INT_VALUE = 42;
    private static final long LONG_VALUE = 42L;
    private static final float FLOAT_VALUE = 42f;
    private static final double DOUBLE_VALUE = 42d;

    private ExampleInterface byteBuddyInstance;
    private ExampleInterface cglibInstance;
    private ExampleInterface javassistInstance;
    private ExampleInterface jdkProxyInstance;

    /**
     * Creates an instance for each code generation library. The classes are extracted from the
     * {@link net.bytebuddy.benchmark.ClassByImplementationBenchmark}.
     *
     * @throws Exception Covers the exception declarations of the setup methods.
     */
    @Setup
    public void setUp() throws Exception {
        ClassByImplementationBenchmark classByImplementationBenchmark = new ClassByImplementationBenchmark();
        byteBuddyInstance = classByImplementationBenchmark.benchmarkByteBuddy();
        cglibInstance = classByImplementationBenchmark.benchmarkCglib();
        javassistInstance = classByImplementationBenchmark.benchmarkJavassist();
        jdkProxyInstance = classByImplementationBenchmark.benchmarkJdkProxy();
    }

    /**
     * Performs a benchmark for a trivial class creation using Byte Buddy.
     *
     * @param blackHole A black hole for avoiding JIT erasure.
     */
    @GenerateMicroBenchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @OperationsPerInvocation(20)
    public void benchmarkByteBuddy(BlackHole blackHole) {
        blackHole.consume(byteBuddyInstance.method(BOOLEAN_VALUE));
        blackHole.consume(byteBuddyInstance.method(BYTE_VALUE));
        blackHole.consume(byteBuddyInstance.method(SHORT_VALUE));
        blackHole.consume(byteBuddyInstance.method(INT_VALUE));
        blackHole.consume(byteBuddyInstance.method(CHAR_VALUE));
        blackHole.consume(byteBuddyInstance.method(INT_VALUE));
        blackHole.consume(byteBuddyInstance.method(LONG_VALUE));
        blackHole.consume(byteBuddyInstance.method(FLOAT_VALUE));
        blackHole.consume(byteBuddyInstance.method(DOUBLE_VALUE));
        blackHole.consume(byteBuddyInstance.method(STRING_VALUE));
        blackHole.consume(byteBuddyInstance.method(BOOLEAN_VALUE, BOOLEAN_VALUE, BOOLEAN_VALUE));
        blackHole.consume(byteBuddyInstance.method(BYTE_VALUE, BYTE_VALUE, BYTE_VALUE));
        blackHole.consume(byteBuddyInstance.method(SHORT_VALUE, SHORT_VALUE, SHORT_VALUE));
        blackHole.consume(byteBuddyInstance.method(INT_VALUE, INT_VALUE, INT_VALUE));
        blackHole.consume(byteBuddyInstance.method(CHAR_VALUE, CHAR_VALUE, CHAR_VALUE));
        blackHole.consume(byteBuddyInstance.method(INT_VALUE, INT_VALUE, INT_VALUE));
        blackHole.consume(byteBuddyInstance.method(LONG_VALUE, LONG_VALUE, LONG_VALUE));
        blackHole.consume(byteBuddyInstance.method(FLOAT_VALUE, FLOAT_VALUE, FLOAT_VALUE));
        blackHole.consume(byteBuddyInstance.method(DOUBLE_VALUE, DOUBLE_VALUE, DOUBLE_VALUE));
        blackHole.consume(byteBuddyInstance.method(STRING_VALUE, STRING_VALUE, STRING_VALUE));
    }

    /**
     * Performs a benchmark for a trivial class creation using cglib.
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
     * Performs a benchmark for a trivial class creation using javassist.
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

    /**
     * Performs a benchmark for a trivial class creation using the Java Class Library's utilities.
     *
     * @param blackHole A black hole for avoiding JIT erasure.
     */
    @GenerateMicroBenchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @OperationsPerInvocation(20)
    public void benchmarkJdkProxy(BlackHole blackHole) {
        blackHole.consume(jdkProxyInstance.method(BOOLEAN_VALUE));
        blackHole.consume(jdkProxyInstance.method(BYTE_VALUE));
        blackHole.consume(jdkProxyInstance.method(SHORT_VALUE));
        blackHole.consume(jdkProxyInstance.method(INT_VALUE));
        blackHole.consume(jdkProxyInstance.method(CHAR_VALUE));
        blackHole.consume(jdkProxyInstance.method(INT_VALUE));
        blackHole.consume(jdkProxyInstance.method(LONG_VALUE));
        blackHole.consume(jdkProxyInstance.method(FLOAT_VALUE));
        blackHole.consume(jdkProxyInstance.method(DOUBLE_VALUE));
        blackHole.consume(jdkProxyInstance.method(STRING_VALUE));
        blackHole.consume(jdkProxyInstance.method(BOOLEAN_VALUE, BOOLEAN_VALUE, BOOLEAN_VALUE));
        blackHole.consume(jdkProxyInstance.method(BYTE_VALUE, BYTE_VALUE, BYTE_VALUE));
        blackHole.consume(jdkProxyInstance.method(SHORT_VALUE, SHORT_VALUE, SHORT_VALUE));
        blackHole.consume(jdkProxyInstance.method(INT_VALUE, INT_VALUE, INT_VALUE));
        blackHole.consume(jdkProxyInstance.method(CHAR_VALUE, CHAR_VALUE, CHAR_VALUE));
        blackHole.consume(jdkProxyInstance.method(INT_VALUE, INT_VALUE, INT_VALUE));
        blackHole.consume(jdkProxyInstance.method(LONG_VALUE, LONG_VALUE, LONG_VALUE));
        blackHole.consume(jdkProxyInstance.method(FLOAT_VALUE, FLOAT_VALUE, FLOAT_VALUE));
        blackHole.consume(jdkProxyInstance.method(DOUBLE_VALUE, DOUBLE_VALUE, DOUBLE_VALUE));
        blackHole.consume(jdkProxyInstance.method(STRING_VALUE, STRING_VALUE, STRING_VALUE));
    }
}
