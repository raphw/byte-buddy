package net.bytebuddy.benchmark;

import net.bytebuddy.benchmark.specimen.ExampleClass;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * <p>
 * This benchmark measures the invocation speed of stub method invocations. All classes implement
 * {@link net.bytebuddy.benchmark.specimen.ExampleClass} and call this class's super method invocation. Since it
 * is not possible to create a subclass with the JDK proxy utilities, the latter is omitted from the benchmark.
 * </p>
 * <p>
 * Note that this class defines all values that are accessed by benchmark methods as instance fields. This way, the JIT
 * compiler's capability of constant folding is limited in order to produce more comparable test results.
 * </p>
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class SuperClassInvocationBenchmark {

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
    private ExampleClass baselineInstance;

    /**
     * An instance created by Byte Buddy for performing benchmarks on. This instance is created by adding
     * auxiliary classes that allow for an invocation of a method from a delegation target.
     */
    private ExampleClass byteBuddyWithProxiesInstance;

    /**
     * An instance created by Byte Buddy for performing benchmarks on. This instance is created by adding
     * super invocation methods which are exposed via the reflection API.
     */
    private ExampleClass byteBuddyWithAccessorsInstance;

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
        baselineInstance = classByExtensionBenchmark.baseline();
        byteBuddyWithProxiesInstance = classByExtensionBenchmark.benchmarkByteBuddyWithProxies();
        byteBuddyWithAccessorsInstance = classByExtensionBenchmark.benchmarkByteBuddyWithAccessors();
        byteBuddySpecializedInstance = classByExtensionBenchmark.benchmarkByteBuddySpecialized();
        cglibInstance = classByExtensionBenchmark.benchmarkCglib();
        javassistInstance = classByExtensionBenchmark.benchmarkJavassist();
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
     * Performs a benchmark of a super method invocation using Byte Buddy. This benchmark uses an annotation-based
     * approach which is more difficult to optimize by the JIT compiler.
     *
     * @param blackHole A black hole for avoiding JIT erasure.
     */
    @Benchmark
    @OperationsPerInvocation(20)
    public void benchmarkByteBuddyWithProxies(Blackhole blackHole) {
        blackHole.consume(byteBuddyWithProxiesInstance.method(booleanValue));
        blackHole.consume(byteBuddyWithProxiesInstance.method(byteValue));
        blackHole.consume(byteBuddyWithProxiesInstance.method(shortValue));
        blackHole.consume(byteBuddyWithProxiesInstance.method(intValue));
        blackHole.consume(byteBuddyWithProxiesInstance.method(charValue));
        blackHole.consume(byteBuddyWithProxiesInstance.method(intValue));
        blackHole.consume(byteBuddyWithProxiesInstance.method(longValue));
        blackHole.consume(byteBuddyWithProxiesInstance.method(floatValue));
        blackHole.consume(byteBuddyWithProxiesInstance.method(doubleValue));
        blackHole.consume(byteBuddyWithProxiesInstance.method(stringValue));
        blackHole.consume(byteBuddyWithProxiesInstance.method(booleanValue, booleanValue, booleanValue));
        blackHole.consume(byteBuddyWithProxiesInstance.method(byteValue, byteValue, byteValue));
        blackHole.consume(byteBuddyWithProxiesInstance.method(shortValue, shortValue, shortValue));
        blackHole.consume(byteBuddyWithProxiesInstance.method(intValue, intValue, intValue));
        blackHole.consume(byteBuddyWithProxiesInstance.method(charValue, charValue, charValue));
        blackHole.consume(byteBuddyWithProxiesInstance.method(intValue, intValue, intValue));
        blackHole.consume(byteBuddyWithProxiesInstance.method(longValue, longValue, longValue));
        blackHole.consume(byteBuddyWithProxiesInstance.method(floatValue, floatValue, floatValue));
        blackHole.consume(byteBuddyWithProxiesInstance.method(doubleValue, doubleValue, doubleValue));
        blackHole.consume(byteBuddyWithProxiesInstance.method(stringValue, stringValue, stringValue));
    }

    /**
     * Performs a benchmark of a super method invocation using Byte Buddy.  This benchmark also uses the annotation-based approach
     * but creates delegation methods which do not require the creation of additional classes.
     *
     * @param blackHole A black hole for avoiding JIT erasure.
     */
    @Benchmark
    @OperationsPerInvocation(20)
    public void benchmarkByteBuddyWithAccessors(Blackhole blackHole) {
        blackHole.consume(byteBuddyWithAccessorsInstance.method(booleanValue));
        blackHole.consume(byteBuddyWithAccessorsInstance.method(byteValue));
        blackHole.consume(byteBuddyWithAccessorsInstance.method(shortValue));
        blackHole.consume(byteBuddyWithAccessorsInstance.method(intValue));
        blackHole.consume(byteBuddyWithAccessorsInstance.method(charValue));
        blackHole.consume(byteBuddyWithAccessorsInstance.method(intValue));
        blackHole.consume(byteBuddyWithAccessorsInstance.method(longValue));
        blackHole.consume(byteBuddyWithAccessorsInstance.method(floatValue));
        blackHole.consume(byteBuddyWithAccessorsInstance.method(doubleValue));
        blackHole.consume(byteBuddyWithAccessorsInstance.method(stringValue));
        blackHole.consume(byteBuddyWithAccessorsInstance.method(booleanValue, booleanValue, booleanValue));
        blackHole.consume(byteBuddyWithAccessorsInstance.method(byteValue, byteValue, byteValue));
        blackHole.consume(byteBuddyWithAccessorsInstance.method(shortValue, shortValue, shortValue));
        blackHole.consume(byteBuddyWithAccessorsInstance.method(intValue, intValue, intValue));
        blackHole.consume(byteBuddyWithAccessorsInstance.method(charValue, charValue, charValue));
        blackHole.consume(byteBuddyWithAccessorsInstance.method(intValue, intValue, intValue));
        blackHole.consume(byteBuddyWithAccessorsInstance.method(longValue, longValue, longValue));
        blackHole.consume(byteBuddyWithAccessorsInstance.method(floatValue, floatValue, floatValue));
        blackHole.consume(byteBuddyWithAccessorsInstance.method(doubleValue, doubleValue, doubleValue));
        blackHole.consume(byteBuddyWithAccessorsInstance.method(stringValue, stringValue, stringValue));
    }

    /**
     * Performs a benchmark of a super method invocation using Byte Buddy. This benchmark uses a specialized
     * interception strategy which is easier to inline by the compiler.
     *
     * @param blackHole A black hole for avoiding JIT erasure.
     */
    @Benchmark
    @OperationsPerInvocation(20)
    public void benchmarkByteBuddySpecialized(Blackhole blackHole) {
        blackHole.consume(byteBuddySpecializedInstance.method(booleanValue));
        blackHole.consume(byteBuddySpecializedInstance.method(byteValue));
        blackHole.consume(byteBuddySpecializedInstance.method(shortValue));
        blackHole.consume(byteBuddySpecializedInstance.method(intValue));
        blackHole.consume(byteBuddySpecializedInstance.method(charValue));
        blackHole.consume(byteBuddySpecializedInstance.method(intValue));
        blackHole.consume(byteBuddySpecializedInstance.method(longValue));
        blackHole.consume(byteBuddySpecializedInstance.method(floatValue));
        blackHole.consume(byteBuddySpecializedInstance.method(doubleValue));
        blackHole.consume(byteBuddySpecializedInstance.method(stringValue));
        blackHole.consume(byteBuddySpecializedInstance.method(booleanValue, booleanValue, booleanValue));
        blackHole.consume(byteBuddySpecializedInstance.method(byteValue, byteValue, byteValue));
        blackHole.consume(byteBuddySpecializedInstance.method(shortValue, shortValue, shortValue));
        blackHole.consume(byteBuddySpecializedInstance.method(intValue, intValue, intValue));
        blackHole.consume(byteBuddySpecializedInstance.method(charValue, charValue, charValue));
        blackHole.consume(byteBuddySpecializedInstance.method(intValue, intValue, intValue));
        blackHole.consume(byteBuddySpecializedInstance.method(longValue, longValue, longValue));
        blackHole.consume(byteBuddySpecializedInstance.method(floatValue, floatValue, floatValue));
        blackHole.consume(byteBuddySpecializedInstance.method(doubleValue, doubleValue, doubleValue));
        blackHole.consume(byteBuddySpecializedInstance.method(stringValue, stringValue, stringValue));
    }

    /**
     * Performs a benchmark of a super method invocation using cglib.
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
     * Performs a benchmark of a super method invocation using javassist.
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
}
