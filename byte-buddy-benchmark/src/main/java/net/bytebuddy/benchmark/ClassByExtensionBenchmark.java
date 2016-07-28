package net.bytebuddy.benchmark;

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.benchmark.specimen.ExampleClass;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.sf.cglib.proxy.*;
import org.openjdk.jmh.annotations.*;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static net.bytebuddy.matcher.ElementMatchers.none;

/**
 * <p>
 * This benchmark dynamically creates a subclass of {@link ExampleClass} which overrides all methods to invoke the
 * direct super class's implementation. The benchmark furthermore creates an instance of this class since some
 * code generation frameworks rely on this property. Because this benchmark requires the creation of a subclass,
 * the JDK proxy is not included in this benchmark.
 * </p>
 * <p>
 * Note that this class defines all values that are accessed by benchmark methods as instance fields. This way, the JIT
 * compiler's capability of constant folding is limited in order to produce more comparable test results.
 * </p>
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class ClassByExtensionBenchmark {

    /**
     * The base class to be subclassed in all benchmarks.
     */
    public static final Class<? extends ExampleClass> BASE_CLASS = ExampleClass.class;

    /**
     * The base class to be subclassed in all benchmarks.
     */
    private Class<? extends ExampleClass> baseClass = BASE_CLASS;

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
     * Creates a baseline for the benchmark.
     *
     * @return A simple object that is not transformed.
     */
    @Benchmark
    public ExampleClass baseline() {
        return new ExampleClass();
    }

    /**
     * Performs a benchmark of a class extension using Byte Buddy. This benchmark uses an annotation-based approach
     * which is by its reflective nature more difficult to optimize by the JIT compiler.
     *
     * @return The created instance, in order to avoid JIT removal.
     * @throws java.lang.Exception If the reflective invocation causes an exception.
     */
    @Benchmark
    public ExampleClass benchmarkByteBuddyWithAnnotations() throws Exception {
        return new ByteBuddy()
                .with(TypeValidation.DISABLED)
                .ignore(none())
                .subclass(baseClass)
                .method(isDeclaredBy(ExampleClass.class)).intercept(MethodDelegation.to(ByteBuddyInterceptor.class))
                .make()
                .load(newClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                .getLoaded()
                .getDeclaredConstructor()
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
    public ExampleClass benchmarkByteBuddySpecialized() throws Exception {
        return new ByteBuddy()
                .with(TypeValidation.DISABLED)
                .ignore(none())
                .subclass(baseClass)
                .method(isDeclaredBy(ExampleClass.class)).intercept(SuperMethodCall.INSTANCE)
                .make()
                .load(newClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                .getLoaded()
                .getDeclaredConstructor()
                .newInstance();
    }

    /**
     * Performs a benchmark of a class extension using cglib.
     *
     * @return The created instance, in order to avoid JIT removal.
     */
    @Benchmark
    public ExampleClass benchmarkCglib() {
        Enhancer enhancer = new Enhancer();
        enhancer.setUseCache(false);
        enhancer.setClassLoader(newClassLoader());
        enhancer.setSuperclass(baseClass);
        CallbackHelper callbackHelper = new CallbackHelper(baseClass, new Class[0]) {
            @Override
            protected Object getCallback(Method method) {
                if (method.getDeclaringClass() == baseClass) {
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
    public ExampleClass benchmarkJavassist() throws Exception {
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
                return method.getDeclaringClass() == baseClass;
            }
        });
        @SuppressWarnings("unchecked")
        Object instance = proxyFactory.createClass().getDeclaredConstructor().newInstance();
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
     * Instead of using the {@link net.bytebuddy.implementation.SuperMethodCall} implementation, we are using
     * a delegate in order to emulate the interception approach of other instrumentation libraries. Otherwise,
     * this benchmark would be biased in favor of Byte Buddy.
     */
    public static class ByteBuddyInterceptor {

        /**
         * The interceptor's constructor is not supposed to be invoked.
         */
        private ByteBuddyInterceptor() {
            throw new UnsupportedOperationException();
        }

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
