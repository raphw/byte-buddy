package net.bytebuddy.benchmark;

import net.bytebuddy.benchmark.specimen.ExampleInterface;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * This benchmark measures the invocation speed of stub method invocations. All classes implement
 * {@link net.bytebuddy.benchmark.specimen.ExampleInterface} and implement all methods to return the return type's
 * default value, independently of the arguments.
 */
@State(Scope.Benchmark)
public class StubInvocationBenchmark {

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
     * An instance created by Byte Buddy for performing benchmarks on.
     */
    private ExampleInterface byteBuddyInstance;

    /**
     * An instance created by cglib for performing benchmarks on.
     */
    private ExampleInterface cglibInstance;

    /**
     * An instance created by javassist for performing benchmarks on.
     */
    private ExampleInterface javassistInstance;

    /**
     * An instance created by the JDK proxy for performing benchmarks on.
     */
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
    public void benchmarkByteBuddy(Blackhole blackHole) {
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
     * Performs a benchmark for a trivial class creation using javassist.
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

    /**
     * Performs a benchmark for a trivial class creation using the Java Class Library's utilities.
     *
     * @param blackHole A black hole for avoiding JIT erasure.
     */
    @GenerateMicroBenchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    @OperationsPerInvocation(20)
    public void benchmarkJdkProxy(Blackhole blackHole) {
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
