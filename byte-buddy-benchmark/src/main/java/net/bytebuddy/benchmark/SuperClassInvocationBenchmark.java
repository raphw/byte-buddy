/*
 * Copyright 2014 - Present Rafael Winterhalter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
    private ExampleClass byteBuddyWithProxyInstance;

    /**
     * An instance created by Byte Buddy for performing benchmarks on. This instance is created by adding
     * super invocation methods which are exposed via the reflection API.
     */
    private ExampleClass byteBuddyWithAccessorInstance;

    /**
     * An instance created by Byte Buddy for performing benchmarks on. This instance is created by a delegation
     * followed by a hard-coded super method call.
     */
    private ExampleClass byteBuddyWithPrefixInstance;

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
        byteBuddyWithProxyInstance = classByExtensionBenchmark.benchmarkByteBuddyWithProxy();
        byteBuddyWithAccessorInstance = classByExtensionBenchmark.benchmarkByteBuddyWithAccessor();
        byteBuddyWithPrefixInstance = classByExtensionBenchmark.benchmarkByteBuddyWithPrefix();
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
    public void benchmarkByteBuddyWithProxy(Blackhole blackHole) {
        blackHole.consume(byteBuddyWithProxyInstance.method(booleanValue));
        blackHole.consume(byteBuddyWithProxyInstance.method(byteValue));
        blackHole.consume(byteBuddyWithProxyInstance.method(shortValue));
        blackHole.consume(byteBuddyWithProxyInstance.method(intValue));
        blackHole.consume(byteBuddyWithProxyInstance.method(charValue));
        blackHole.consume(byteBuddyWithProxyInstance.method(intValue));
        blackHole.consume(byteBuddyWithProxyInstance.method(longValue));
        blackHole.consume(byteBuddyWithProxyInstance.method(floatValue));
        blackHole.consume(byteBuddyWithProxyInstance.method(doubleValue));
        blackHole.consume(byteBuddyWithProxyInstance.method(stringValue));
        blackHole.consume(byteBuddyWithProxyInstance.method(booleanValue, booleanValue, booleanValue));
        blackHole.consume(byteBuddyWithProxyInstance.method(byteValue, byteValue, byteValue));
        blackHole.consume(byteBuddyWithProxyInstance.method(shortValue, shortValue, shortValue));
        blackHole.consume(byteBuddyWithProxyInstance.method(intValue, intValue, intValue));
        blackHole.consume(byteBuddyWithProxyInstance.method(charValue, charValue, charValue));
        blackHole.consume(byteBuddyWithProxyInstance.method(intValue, intValue, intValue));
        blackHole.consume(byteBuddyWithProxyInstance.method(longValue, longValue, longValue));
        blackHole.consume(byteBuddyWithProxyInstance.method(floatValue, floatValue, floatValue));
        blackHole.consume(byteBuddyWithProxyInstance.method(doubleValue, doubleValue, doubleValue));
        blackHole.consume(byteBuddyWithProxyInstance.method(stringValue, stringValue, stringValue));
    }

    /**
     * Performs a benchmark of a super method invocation using Byte Buddy. This benchmark also uses the annotation-based approach
     * but creates delegation methods which do not require the creation of additional classes.
     *
     * @param blackHole A black hole for avoiding JIT erasure.
     */
    @Benchmark
    @OperationsPerInvocation(20)
    public void benchmarkByteBuddyWithAccessor(Blackhole blackHole) {
        blackHole.consume(byteBuddyWithAccessorInstance.method(booleanValue));
        blackHole.consume(byteBuddyWithAccessorInstance.method(byteValue));
        blackHole.consume(byteBuddyWithAccessorInstance.method(shortValue));
        blackHole.consume(byteBuddyWithAccessorInstance.method(intValue));
        blackHole.consume(byteBuddyWithAccessorInstance.method(charValue));
        blackHole.consume(byteBuddyWithAccessorInstance.method(intValue));
        blackHole.consume(byteBuddyWithAccessorInstance.method(longValue));
        blackHole.consume(byteBuddyWithAccessorInstance.method(floatValue));
        blackHole.consume(byteBuddyWithAccessorInstance.method(doubleValue));
        blackHole.consume(byteBuddyWithAccessorInstance.method(stringValue));
        blackHole.consume(byteBuddyWithAccessorInstance.method(booleanValue, booleanValue, booleanValue));
        blackHole.consume(byteBuddyWithAccessorInstance.method(byteValue, byteValue, byteValue));
        blackHole.consume(byteBuddyWithAccessorInstance.method(shortValue, shortValue, shortValue));
        blackHole.consume(byteBuddyWithAccessorInstance.method(intValue, intValue, intValue));
        blackHole.consume(byteBuddyWithAccessorInstance.method(charValue, charValue, charValue));
        blackHole.consume(byteBuddyWithAccessorInstance.method(intValue, intValue, intValue));
        blackHole.consume(byteBuddyWithAccessorInstance.method(longValue, longValue, longValue));
        blackHole.consume(byteBuddyWithAccessorInstance.method(floatValue, floatValue, floatValue));
        blackHole.consume(byteBuddyWithAccessorInstance.method(doubleValue, doubleValue, doubleValue));
        blackHole.consume(byteBuddyWithAccessorInstance.method(stringValue, stringValue, stringValue));
    }

    /**
     * Performs a benchmark of a super method invocation using Byte Buddy. This benchmark also uses the annotation-based approach
     * but hard-codes the super method call subsequently to the method.
     *
     * @param blackHole A black hole for avoiding JIT erasure.
     */
    @Benchmark
    @OperationsPerInvocation(20)
    public void benchmarkByteBuddyWithPrefix(Blackhole blackHole) {
        blackHole.consume(byteBuddyWithPrefixInstance.method(booleanValue));
        blackHole.consume(byteBuddyWithPrefixInstance.method(byteValue));
        blackHole.consume(byteBuddyWithPrefixInstance.method(shortValue));
        blackHole.consume(byteBuddyWithPrefixInstance.method(intValue));
        blackHole.consume(byteBuddyWithPrefixInstance.method(charValue));
        blackHole.consume(byteBuddyWithPrefixInstance.method(intValue));
        blackHole.consume(byteBuddyWithPrefixInstance.method(longValue));
        blackHole.consume(byteBuddyWithPrefixInstance.method(floatValue));
        blackHole.consume(byteBuddyWithPrefixInstance.method(doubleValue));
        blackHole.consume(byteBuddyWithPrefixInstance.method(stringValue));
        blackHole.consume(byteBuddyWithPrefixInstance.method(booleanValue, booleanValue, booleanValue));
        blackHole.consume(byteBuddyWithPrefixInstance.method(byteValue, byteValue, byteValue));
        blackHole.consume(byteBuddyWithPrefixInstance.method(shortValue, shortValue, shortValue));
        blackHole.consume(byteBuddyWithPrefixInstance.method(intValue, intValue, intValue));
        blackHole.consume(byteBuddyWithPrefixInstance.method(charValue, charValue, charValue));
        blackHole.consume(byteBuddyWithPrefixInstance.method(intValue, intValue, intValue));
        blackHole.consume(byteBuddyWithPrefixInstance.method(longValue, longValue, longValue));
        blackHole.consume(byteBuddyWithPrefixInstance.method(floatValue, floatValue, floatValue));
        blackHole.consume(byteBuddyWithPrefixInstance.method(doubleValue, doubleValue, doubleValue));
        blackHole.consume(byteBuddyWithPrefixInstance.method(stringValue, stringValue, stringValue));
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
