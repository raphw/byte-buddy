package net.bytebuddy.benchmark;

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.ProxyFactory;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.ClassLoadingStrategy;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.NoOp;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.TimeUnit;

import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.any;

/**
 * A benchmark for creating plain subclasses of {@link Object} that do not override any methods. This benchmark
 * intends to measure the general overhead of each library.
 */
public class TrivialClassCreationBenchmark {

    /**
     * The base class to be subclassed in all benchmarks.
     */
    public static final Class<?> BASE_CLASS = Object.class;

    /**
     * Creates a new class loader. By using a fresh class loader for each creation, we avoid name space issues.
     * A class loader's creation is part of the benchmark but since any test creates a class loader exactly once,
     * the benchmark remains valid.
     *
     * @return A new class loader.
     */
    private static ClassLoader newClassLoader() {
        return new URLClassLoader(new URL[0]);
    }

    /**
     * Performs a benchmark for a trivial class creation using Byte Buddy.
     *
     * @return The created instance, in order to avoid JIT removal.
     */
    @GenerateMicroBenchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public Class<?> benchmarkByteBuddy() {
        return new ByteBuddy()
                .withIgnoredMethods(any())
                .subclass(BASE_CLASS)
                .make()
                .load(newClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                .getLoaded();
    }

    /**
     * Performs a benchmark for a trivial class creation using cglib.
     *
     * @return The created instance, in order to avoid JIT removal.
     */
    @GenerateMicroBenchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public Class<?> benchmarkCglib() {
        Enhancer enhancer = new Enhancer();
        enhancer.setUseCache(false);
        enhancer.setClassLoader(newClassLoader());
        enhancer.setSuperclass(BASE_CLASS);
        enhancer.setCallbackType(NoOp.class);
        return enhancer.createClass();
    }

    /**
     * Performs a benchmark for a trivial class creation using javassist proxies.
     *
     * @return The created instance, in order to avoid JIT removal.
     */
    @GenerateMicroBenchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public Class<?> benchmarkJavassist() {
        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.setUseCache(false);
        ProxyFactory.classLoaderProvider = new ProxyFactory.ClassLoaderProvider() {
            @Override
            public ClassLoader get(ProxyFactory proxyFactory) {
                return newClassLoader();
            }
        };
        proxyFactory.setSuperclass(BASE_CLASS);
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
    @GenerateMicroBenchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public Class<?> benchmarkJdkProxy() {
        return Proxy.getProxyClass(newClassLoader(), new Class[0]);
    }
}
