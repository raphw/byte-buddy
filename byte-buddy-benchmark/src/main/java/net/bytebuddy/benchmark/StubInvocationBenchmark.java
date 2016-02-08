package net.bytebuddy.benchmark;

import net.bytebuddy.benchmark.specimen.ExampleInterface;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * <p>
 * This benchmark measures the invocation speed of stub method invocations. All classes implement
 * {@link net.bytebuddy.benchmark.specimen.ExampleInterface} and implement all methods to return the return type's
 * default value, independently of the arguments.
 * </p>
 * <p>
 * Note that this class defines all values that are accessed by benchmark methods as instance fields. This way, the JIT
 * compiler's capability of constant folding is limited in order to produce more comparable test results.
 * </p>
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class StubInvocationBenchmark {

    /**
     * A generic {@link String} value.
     */
    private String stringValue = "foo";

    /**
     * A generic {@code boolean} value.
     */
    private boolean booleanValue = true;

    /**
     * A generic {@code byte} value.
     */
    private byte byteValue = 42;

    /**
     * A generic {@code short} value.
     */
    private short shortValue = 42;

    /**
     * A generic {@code char} value.
     */
    private char charValue = '@';

    /**
     * A generic {@code int} value.
     */
    private int intValue = 42;

    /**
     * A generic {@code long} value.
     */
    private long longValue = 42L;

    /**
     * A generic {@code float} value.
     */
    private float floatValue = 42f;

    /**
     * A generic {@code double} value.
     */
    private double doubleValue = 42d;

    /**
     * A casual instance that serves as a baseline.
     */
    private ExampleInterface baselineInstance;

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
        baselineInstance = classByImplementationBenchmark.baseline();
        byteBuddyInstance = classByImplementationBenchmark.benchmarkByteBuddy();
        cglibInstance = classByImplementationBenchmark.benchmarkCglib();
        javassistInstance = classByImplementationBenchmark.benchmarkJavassist();
        jdkProxyInstance = classByImplementationBenchmark.benchmarkJdkProxy();
    }

    /**
     * Performs a benchmark for a casual class as a baseline.
     *
     * @param blackHole A black hole for avoiding JIT erasure.
     */
    @Benchmark
    @OperationsPerInvocation(20)
    public void baseline(Blackhole blackHole) {
        blackHole.consume(baselineInstance.method(booleanValue));
        blackHole.consume(baselineInstance.method(byteValue));
        blackHole.consume(baselineInstance.method(shortValue));
        blackHole.consume(baselineInstance.method(intValue));
        blackHole.consume(baselineInstance.method(charValue));
        blackHole.consume(baselineInstance.method(intValue));
        blackHole.consume(baselineInstance.method(longValue));
        blackHole.consume(baselineInstance.method(floatValue));
        blackHole.consume(baselineInstance.method(doubleValue));
        blackHole.consume(baselineInstance.method(stringValue));
        blackHole.consume(baselineInstance.method(booleanValue, booleanValue, booleanValue));
        blackHole.consume(baselineInstance.method(byteValue, byteValue, byteValue));
        blackHole.consume(baselineInstance.method(shortValue, shortValue, shortValue));
        blackHole.consume(baselineInstance.method(intValue, intValue, intValue));
        blackHole.consume(baselineInstance.method(charValue, charValue, charValue));
        blackHole.consume(baselineInstance.method(intValue, intValue, intValue));
        blackHole.consume(baselineInstance.method(longValue, longValue, longValue));
        blackHole.consume(baselineInstance.method(floatValue, floatValue, floatValue));
        blackHole.consume(baselineInstance.method(doubleValue, doubleValue, doubleValue));
        blackHole.consume(baselineInstance.method(stringValue, stringValue, stringValue));
    }

    /**
     * Performs a benchmark for a trivial class creation using Byte Buddy.
     *
     * @param blackHole A black hole for avoiding JIT erasure.
     */
    @Benchmark
    @OperationsPerInvocation(20)
    public void benchmarkByteBuddy(Blackhole blackHole) {
        blackHole.consume(byteBuddyInstance.method(booleanValue));
        blackHole.consume(byteBuddyInstance.method(byteValue));
        blackHole.consume(byteBuddyInstance.method(shortValue));
        blackHole.consume(byteBuddyInstance.method(intValue));
        blackHole.consume(byteBuddyInstance.method(charValue));
        blackHole.consume(byteBuddyInstance.method(intValue));
        blackHole.consume(byteBuddyInstance.method(longValue));
        blackHole.consume(byteBuddyInstance.method(floatValue));
        blackHole.consume(byteBuddyInstance.method(doubleValue));
        blackHole.consume(byteBuddyInstance.method(stringValue));
        blackHole.consume(byteBuddyInstance.method(booleanValue, booleanValue, booleanValue));
        blackHole.consume(byteBuddyInstance.method(byteValue, byteValue, byteValue));
        blackHole.consume(byteBuddyInstance.method(shortValue, shortValue, shortValue));
        blackHole.consume(byteBuddyInstance.method(intValue, intValue, intValue));
        blackHole.consume(byteBuddyInstance.method(charValue, charValue, charValue));
        blackHole.consume(byteBuddyInstance.method(intValue, intValue, intValue));
        blackHole.consume(byteBuddyInstance.method(longValue, longValue, longValue));
        blackHole.consume(byteBuddyInstance.method(floatValue, floatValue, floatValue));
        blackHole.consume(byteBuddyInstance.method(doubleValue, doubleValue, doubleValue));
        blackHole.consume(byteBuddyInstance.method(stringValue, stringValue, stringValue));
    }

    /**
     * Performs a benchmark for a trivial class creation using cglib.
     *
     * @param blackHole A black hole for avoiding JIT erasure.
     */
    @Benchmark
    @OperationsPerInvocation(20)
    public void benchmarkCglib(Blackhole blackHole) {
        blackHole.consume(cglibInstance.method(booleanValue));
        blackHole.consume(cglibInstance.method(byteValue));
        blackHole.consume(cglibInstance.method(shortValue));
        blackHole.consume(cglibInstance.method(intValue));
        blackHole.consume(cglibInstance.method(charValue));
        blackHole.consume(cglibInstance.method(intValue));
        blackHole.consume(cglibInstance.method(longValue));
        blackHole.consume(cglibInstance.method(floatValue));
        blackHole.consume(cglibInstance.method(doubleValue));
        blackHole.consume(cglibInstance.method(stringValue));
        blackHole.consume(cglibInstance.method(booleanValue, booleanValue, booleanValue));
        blackHole.consume(cglibInstance.method(byteValue, byteValue, byteValue));
        blackHole.consume(cglibInstance.method(shortValue, shortValue, shortValue));
        blackHole.consume(cglibInstance.method(intValue, intValue, intValue));
        blackHole.consume(cglibInstance.method(charValue, charValue, charValue));
        blackHole.consume(cglibInstance.method(intValue, intValue, intValue));
        blackHole.consume(cglibInstance.method(longValue, longValue, longValue));
        blackHole.consume(cglibInstance.method(floatValue, floatValue, floatValue));
        blackHole.consume(cglibInstance.method(doubleValue, doubleValue, doubleValue));
        blackHole.consume(cglibInstance.method(stringValue, stringValue, stringValue));
    }

    /**
     * Performs a benchmark for a trivial class creation using javassist.
     *
     * @param blackHole A black hole for avoiding JIT erasure.
     */
    @Benchmark
    @OperationsPerInvocation(20)
    public void benchmarkJavassist(Blackhole blackHole) {
        blackHole.consume(javassistInstance.method(booleanValue));
        blackHole.consume(javassistInstance.method(byteValue));
        blackHole.consume(javassistInstance.method(shortValue));
        blackHole.consume(javassistInstance.method(intValue));
        blackHole.consume(javassistInstance.method(charValue));
        blackHole.consume(javassistInstance.method(intValue));
        blackHole.consume(javassistInstance.method(longValue));
        blackHole.consume(javassistInstance.method(floatValue));
        blackHole.consume(javassistInstance.method(doubleValue));
        blackHole.consume(javassistInstance.method(stringValue));
        blackHole.consume(javassistInstance.method(booleanValue, booleanValue, booleanValue));
        blackHole.consume(javassistInstance.method(byteValue, byteValue, byteValue));
        blackHole.consume(javassistInstance.method(shortValue, shortValue, shortValue));
        blackHole.consume(javassistInstance.method(intValue, intValue, intValue));
        blackHole.consume(javassistInstance.method(charValue, charValue, charValue));
        blackHole.consume(javassistInstance.method(intValue, intValue, intValue));
        blackHole.consume(javassistInstance.method(longValue, longValue, longValue));
        blackHole.consume(javassistInstance.method(floatValue, floatValue, floatValue));
        blackHole.consume(javassistInstance.method(doubleValue, doubleValue, doubleValue));
        blackHole.consume(javassistInstance.method(stringValue, stringValue, stringValue));
    }

    /**
     * Performs a benchmark for a trivial class creation using the Java Class Library's utilities.
     *
     * @param blackHole A black hole for avoiding JIT erasure.
     */
    @Benchmark
    @OperationsPerInvocation(20)
    public void benchmarkJdkProxy(Blackhole blackHole) {
        blackHole.consume(jdkProxyInstance.method(booleanValue));
        blackHole.consume(jdkProxyInstance.method(byteValue));
        blackHole.consume(jdkProxyInstance.method(shortValue));
        blackHole.consume(jdkProxyInstance.method(intValue));
        blackHole.consume(jdkProxyInstance.method(charValue));
        blackHole.consume(jdkProxyInstance.method(intValue));
        blackHole.consume(jdkProxyInstance.method(longValue));
        blackHole.consume(jdkProxyInstance.method(floatValue));
        blackHole.consume(jdkProxyInstance.method(doubleValue));
        blackHole.consume(jdkProxyInstance.method(stringValue));
        blackHole.consume(jdkProxyInstance.method(booleanValue, booleanValue, booleanValue));
        blackHole.consume(jdkProxyInstance.method(byteValue, byteValue, byteValue));
        blackHole.consume(jdkProxyInstance.method(shortValue, shortValue, shortValue));
        blackHole.consume(jdkProxyInstance.method(intValue, intValue, intValue));
        blackHole.consume(jdkProxyInstance.method(charValue, charValue, charValue));
        blackHole.consume(jdkProxyInstance.method(intValue, intValue, intValue));
        blackHole.consume(jdkProxyInstance.method(longValue, longValue, longValue));
        blackHole.consume(jdkProxyInstance.method(floatValue, floatValue, floatValue));
        blackHole.consume(jdkProxyInstance.method(doubleValue, doubleValue, doubleValue));
        blackHole.consume(jdkProxyInstance.method(stringValue, stringValue, stringValue));
    }
}
