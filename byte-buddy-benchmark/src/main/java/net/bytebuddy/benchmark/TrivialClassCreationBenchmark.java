package net.bytebuddy.benchmark;

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.ProxyFactory;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
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
     * Performs a benchmark for a trivial class creation using Byte Buddy.
     *
     * @return The created instance, in order to avoid JIT removal.
     */
    @Benchmark
    public Class<?> benchmarkByteBuddy() {
        return new ByteBuddy()
                .withIgnoredMethods(any())
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
        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.setUseCache(false);
        ProxyFactory.classLoaderProvider = new ProxyFactory.ClassLoaderProvider() {
            @Override
            public ClassLoader get(ProxyFactory proxyFactory) {
                return newClassLoader();
            }
        };
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
    public Class<?> benchmarkJdkProxy() {
        return Proxy.getProxyClass(newClassLoader(), new Class<?>[urlLength]);
    }
}
