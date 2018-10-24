/*
 * Copyright 2014 - 2018 Rafael Winterhalter
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

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.ProxyFactory;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.NoOp;
import org.openjdk.jmh.annotations.*;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.TimeUnit;

import static net.bytebuddy.matcher.ElementMatchers.any;

/**
 * <p>
 * A benchmark for creating plain subclasses of {@link Object} that do not override any methods. This benchmark
 * intends to measure the general overhead of each library.
 * </p>
 * <p>
 * Note that this class defines all values that are accessed by benchmark methods as instance fields. This way, the JIT
 * compiler's capability of constant folding is limited in order to produce more comparable test results.
 * </p>
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class TrivialClassCreationBenchmark {

    /**
     * The base class to be subclassed in all benchmarks.
     */
    public static final Class<?> BASE_CLASS = Object.class;

    /**
     * The base class to be subclassed in all benchmarks.
     */
    private Class<?> baseClass = BASE_CLASS;

    /**
     * The zero-length of the class loader's URL.
     */
    private int urlLength = 0;

    /**
     * Creates a new class loader. By using a fresh class loader for each creation, we avoid name space issues.
     * A class loader's creation is part of the benchmark but since any test creates a class loader exactly once,
     * the benchmark remains valid.
     *
     * @return A new class loader.
     */
    private ClassLoader newClassLoader() {
        return new URLClassLoader(new URL[urlLength]);
    }

    /**
     * Returns a non-instrumented class as a baseline.
     *
     * @return A reference to {@link Object}.
     */
    @Benchmark
    public Class<?> baseline() {
        return Object.class;
    }

    /**
     * Performs a benchmark for a trivial class creation using Byte Buddy.
     *
     * @return The created instance, in order to avoid JIT removal.
     */
    @Benchmark
    public Class<?> benchmarkByteBuddy() {
        return new ByteBuddy()
                .with(TypeValidation.DISABLED)
                .ignore(any())
                .subclass(baseClass)
                .make()
                .load(newClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                .getLoaded();
    }

    /**
     * Performs a benchmark for a trivial class creation using cglib.
     *
     * @return The created instance, in order to avoid JIT removal.
     */
    @Benchmark
    public Class<?> benchmarkCglib() {
        Enhancer enhancer = new Enhancer();
        enhancer.setUseCache(false);
        enhancer.setClassLoader(newClassLoader());
        enhancer.setSuperclass(baseClass);
        enhancer.setCallbackType(NoOp.class);
        return enhancer.createClass();
    }

    /**
     * Performs a benchmark for a trivial class creation using javassist proxies.
     *
     * @return The created instance, in order to avoid JIT removal.
     */
    @Benchmark
    public Class<?> benchmarkJavassist() {
        ProxyFactory proxyFactory = new ProxyFactory() {
            protected ClassLoader getClassLoader() {
                return newClassLoader();
            }
        };
        proxyFactory.setUseCache(false);
        proxyFactory.setUseWriteReplace(false);
        proxyFactory.setSuperclass(baseClass);
        proxyFactory.setFilter(new MethodFilter() {
            public boolean isHandled(Method method) {
                return false;
            }
        });
        return proxyFactory.createClass();
    }

    /**
     * Performs a benchmark for a trivial class creation using the Java Class Library's utilities.
     *
     * @return The created instance, in order to avoid JIT removal.
     */
    @Benchmark
    @SuppressWarnings("deprecation")
    public Class<?> benchmarkJdkProxy() {
        return Proxy.getProxyClass(newClassLoader(), new Class<?>[urlLength]);
    }
}
