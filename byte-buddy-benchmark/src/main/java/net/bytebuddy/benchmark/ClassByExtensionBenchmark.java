package net.bytebuddy.benchmark;

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.benchmark.specimen.ExampleClass;
import net.bytebuddy.dynamic.ClassLoadingStrategy;
import net.bytebuddy.instrumentation.MethodDelegation;
import net.bytebuddy.instrumentation.SuperMethodCall;
import net.bytebuddy.instrumentation.method.bytecode.bind.annotation.RuntimeType;
import net.bytebuddy.instrumentation.method.bytecode.bind.annotation.SuperCall;
import net.sf.cglib.proxy.*;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.isDeclaredBy;
import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.none;

/**
 * This benchmark dynamically creates a subclass of {@link ExampleClass} which overrides all methods to invoke the
 * direct super class's implementation. The benchmark furthermore creates an instance of this class since some
 * code generation frameworks rely on this property. Because this benchmark requires the creation of a subclass,
 * the JDK proxy is not included in this benchmark.
 */
public class ClassByExtensionBenchmark {

    /**
     * The base class to be subclassed in all benchmarks.
     */
    public static final Class<? extends ExampleClass> BASE_CLASS = ExampleClass.class;

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
     * Performs a benchmark of a class extension using Byte Buddy. This benchmark uses an annotation-based approach
     * which is by its reflective nature more difficult to optimize by the JIT compiler.
     *
     * @return The created instance, in order to avoid JIT removal.
     * @throws java.lang.Exception If the reflective invocation causes an exception.
     */
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public ExampleClass benchmarkByteBuddyWithAnnotations() throws Exception {
        return new ByteBuddy()
                .withIgnoredMethods(none())
                .subclass(BASE_CLASS)
                .method(isDeclaredBy(ExampleClass.class)).intercept(MethodDelegation.to(ByteBuddyInterceptor.class))
                .make()
                .load(newClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                .getLoaded()
                .newInstance();
    }

    /**
     * Performs a benchmark of a class extension using Byte Buddy. This benchmark uses a specialized interception
     * strategy which is easier to inline by the compiler.
     *
     * @return The created instance, in order to avoid JIT removal.
     * @throws java.lang.Exception If the reflective invocation causes an exception.
     */
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public ExampleClass benchmarkByteBuddySpecialized() throws Exception {
        return new ByteBuddy()
                .withIgnoredMethods(none())
                .subclass(BASE_CLASS)
                .method(isDeclaredBy(ExampleClass.class)).intercept(SuperMethodCall.INSTANCE)
                .make()
                .load(newClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                .getLoaded()
                .newInstance();
    }

    /**
     * Performs a benchmark of a class extension using cglib.
     *
     * @return The created instance, in order to avoid JIT removal.
     */
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public ExampleClass benchmarkCglib() {
        Enhancer enhancer = new Enhancer();
        enhancer.setUseCache(false);
        enhancer.setClassLoader(newClassLoader());
        enhancer.setSuperclass(BASE_CLASS);
        CallbackHelper callbackHelper = new CallbackHelper(BASE_CLASS, new Class[0]) {
            @Override
            protected Object getCallback(Method method) {
                if (method.getDeclaringClass() == BASE_CLASS) {
                    return new MethodInterceptor() {
                        @Override
                        public Object intercept(Object object,
                                                Method method,
                                                Object[] arguments,
                                                MethodProxy methodProxy) throws Throwable {
                            return methodProxy.invokeSuper(object, arguments);
                        }
                    };
                } else {
                    return NoOp.INSTANCE;
                }
            }
        };
        enhancer.setCallbackFilter(callbackHelper);
        enhancer.setCallbacks(callbackHelper.getCallbacks());
        return (ExampleClass) enhancer.create();
    }

    /**
     * Performs a benchmark of a class extension using javassist proxies.
     *
     * @return The created instance, in order to avoid JIT removal.
     * @throws java.lang.Exception If the reflective invocation causes an exception.
     */
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public ExampleClass benchmarkJavassist() throws Exception {
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
                return method.getDeclaringClass() == BASE_CLASS;
            }
        });
        Object instance = proxyFactory.createClass().newInstance();
        ((javassist.util.proxy.Proxy) instance).setHandler(new MethodHandler() {
            public Object invoke(Object self,
                                 Method thisMethod,
                                 Method proceed,
                                 Object[] args) throws Throwable {
                return proceed.invoke(self, args);
            }
        });
        return (ExampleClass) instance;
    }

    /**
     * Instead of using the {@link net.bytebuddy.instrumentation.SuperMethodCall} instrumentation, we are using
     * a delegate in order to emulate the interception approach of other instrumentation libraries. Otherwise,
     * this benchmark would be biased in favor of Byte Buddy.
     */
    public static class ByteBuddyInterceptor {

        /**
         * Call the super method.
         *
         * @param zuper A proxy for invoking the super method.
         * @return The return value of the super method invocation.
         * @throws Exception As declared by {@link java.util.concurrent.Callable}'s contract.
         */
        @RuntimeType
        public static Object intercept(@SuperCall Callable<?> zuper) throws Exception {
            return zuper.call();
        }
    }
}
