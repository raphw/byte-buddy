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

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.benchmark.specimen.ExampleClass;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.implementation.bind.annotation.*;
import net.bytebuddy.pool.TypePool;
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
     * An implementation to be used by {@link ClassByExtensionBenchmark#benchmarkByteBuddyWithProxyAndReusedDelegator()}.
     */
    private Implementation proxyInterceptor;

    /**
     * An implementation to be used by {@link ClassByExtensionBenchmark#benchmarkByteBuddyWithAccessorAndReusedDelegator()}.
     */
    private Implementation accessInterceptor;

    /**
     * An implementation to be used by {@link ClassByExtensionBenchmark#benchmarkByteBuddyWithPrefixAndReusedDelegator()}.
     */
    private Implementation.Composable prefixInterceptor;

    /**
     * A description of {@link ClassByExtensionBenchmark#baseClass}.
     */
    private TypeDescription baseClassDescription;

    /**
     * A description of {@link ByteBuddyProxyInterceptor}.
     */
    private TypeDescription proxyClassDescription;

    /**
     * A description of {@link ByteBuddyAccessInterceptor}.
     */
    private TypeDescription accessClassDescription;

    /**
     * A description of {@link ByteBuddyPrefixInterceptor}.
     */
    private TypeDescription prefixClassDescription;

    /**
     * A method delegation to {@link ByteBuddyProxyInterceptor}.
     */
    private Implementation proxyInterceptorDescription;

    /**
     * A method delegation to {@link ByteBuddyAccessInterceptor}.
     */
    private Implementation accessInterceptorDescription;

    /**
     * A method delegation to {@link ByteBuddyPrefixInterceptor}.
     */
    private Implementation.Composable prefixInterceptorDescription;

    /**
     * A setup method to create precomputed delegator.
     */
    @Setup
    public void setup() {
        proxyInterceptor = MethodDelegation.to(ByteBuddyProxyInterceptor.class);
        accessInterceptor = MethodDelegation.to(ByteBuddyAccessInterceptor.class);
        prefixInterceptor = MethodDelegation.to(ByteBuddyPrefixInterceptor.class);
        baseClassDescription = TypePool.Default.ofSystemLoader().describe(baseClass.getName()).resolve();
        proxyClassDescription = TypePool.Default.ofSystemLoader().describe(ByteBuddyProxyInterceptor.class.getName()).resolve();
        accessClassDescription = TypePool.Default.ofSystemLoader().describe(ByteBuddyAccessInterceptor.class.getName()).resolve();
        prefixClassDescription = TypePool.Default.ofSystemLoader().describe(ByteBuddyPrefixInterceptor.class.getName()).resolve();
        proxyInterceptorDescription = MethodDelegation.to(proxyClassDescription);
        accessInterceptorDescription = MethodDelegation.to(accessClassDescription);
        prefixInterceptorDescription = MethodDelegation.to(prefixClassDescription);
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
     * Performs a benchmark of a class extension using Byte Buddy. This benchmark creates proxy classes for the invocation
     * of super methods which requires the creation of auxiliary classes.
     *
     * @return The created instance, in order to avoid JIT removal.
     * @throws java.lang.Exception If the invocation causes an exception.
     */
    @Benchmark
    public ExampleClass benchmarkByteBuddyWithProxy() throws Exception {
        return new ByteBuddy()
                .with(TypeValidation.DISABLED)
                .ignore(none())
                .subclass(baseClass)
                .method(isDeclaredBy(baseClass)).intercept(MethodDelegation.to(ByteBuddyProxyInterceptor.class))
                .make()
                .load(newClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                .getLoaded()
                .getDeclaredConstructor()
                .newInstance();
    }

    /**
     * Performs a benchmark of a class extension using Byte Buddy. This benchmark also uses the annotation-based approach
     * but creates delegation methods which do not require the creation of additional classes. This benchmark reuses a
     * precomputed delegator.
     *
     * @return The created instance, in order to avoid JIT removal.
     * @throws Exception If the invocation causes an exception.
     */
    @Benchmark
    public ExampleClass benchmarkByteBuddyWithProxyAndReusedDelegator() throws Exception {
        return new ByteBuddy()
                .with(TypeValidation.DISABLED)
                .ignore(none())
                .subclass(baseClass)
                .method(isDeclaredBy(baseClass)).intercept(proxyInterceptor)
                .make()
                .load(newClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                .getLoaded()
                .getDeclaredConstructor()
                .newInstance();
    }

    /**
     * Performs a benchmark of a class extension using Byte Buddy. This benchmark creates proxy classes for the invocation
     * of super methods which requires the creation of auxiliary classes. This benchmark uses a type pool to compare against
     * usage of the reflection API.
     *
     * @return The created instance, in order to avoid JIT removal.
     * @throws java.lang.Exception If the invocation causes an exception.
     */
    @Benchmark
    public ExampleClass benchmarkByteBuddyWithProxyWithTypePool() throws Exception {
        return (ExampleClass) new ByteBuddy()
                .with(TypeValidation.DISABLED)
                .ignore(none())
                .subclass(baseClassDescription)
                .method(isDeclaredBy(baseClassDescription)).intercept(MethodDelegation.to(proxyClassDescription))
                .make()
                .load(newClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                .getLoaded()
                .getDeclaredConstructor()
                .newInstance();
    }

    /**
     * Performs a benchmark of a class extension using Byte Buddy. This benchmark also uses the annotation-based approach
     * but creates delegation methods which do not require the creation of additional classes. This benchmark reuses a
     * precomputed delegator. This benchmark uses a type pool to compare against usage of the reflection API.
     *
     * @return The created instance, in order to avoid JIT removal.
     * @throws Exception If the invocation causes an exception.
     */
    @Benchmark
    public ExampleClass benchmarkByteBuddyWithProxyAndReusedDelegatorWithTypePool() throws Exception {
        return (ExampleClass) new ByteBuddy()
                .with(TypeValidation.DISABLED)
                .ignore(none())
                .subclass(baseClassDescription)
                .method(isDeclaredBy(baseClassDescription)).intercept(proxyInterceptorDescription)
                .make()
                .load(newClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                .getLoaded()
                .getDeclaredConstructor()
                .newInstance();
    }

    /**
     * Performs a benchmark of a class extension using Byte Buddy. This benchmark also uses the annotation-based approach
     * but creates delegation methods which do not require the creation of additional classes.
     *
     * @return The created instance, in order to avoid JIT removal.
     * @throws Exception If the invocation causes an exception.
     */
    @Benchmark
    public ExampleClass benchmarkByteBuddyWithAccessor() throws Exception {
        return new ByteBuddy()
                .with(TypeValidation.DISABLED)
                .ignore(none())
                .subclass(baseClass)
                .method(isDeclaredBy(baseClass)).intercept(MethodDelegation.to(ByteBuddyAccessInterceptor.class))
                .make()
                .load(newClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                .getLoaded()
                .getDeclaredConstructor()
                .newInstance();
    }

    /**
     * Performs a benchmark of a class extension using Byte Buddy. This benchmark also uses the annotation-based approach
     * but creates delegation methods which do not require the creation of additional classes. This benchmark reuses a
     * precomputed delegator.
     *
     * @return The created instance, in order to avoid JIT removal.
     * @throws Exception If the invocation causes an exception.
     */
    @Benchmark
    public ExampleClass benchmarkByteBuddyWithAccessorAndReusedDelegator() throws Exception {
        return new ByteBuddy()
                .with(TypeValidation.DISABLED)
                .ignore(none())
                .subclass(baseClass)
                .method(isDeclaredBy(baseClass)).intercept(accessInterceptor)
                .make()
                .load(newClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                .getLoaded()
                .getDeclaredConstructor()
                .newInstance();
    }

    /**
     * Performs a benchmark of a class extension using Byte Buddy. This benchmark also uses the annotation-based approach
     * but creates delegation methods which do not require the creation of additional classes. This benchmark uses a type
     * pool to compare against usage of the reflection API.
     *
     * @return The created instance, in order to avoid JIT removal.
     * @throws Exception If the invocation causes an exception.
     */
    @Benchmark
    public ExampleClass benchmarkByteBuddyWithAccessorWithTypePool() throws Exception {
        return (ExampleClass) new ByteBuddy()
                .with(TypeValidation.DISABLED)
                .ignore(none())
                .subclass(baseClassDescription)
                .method(isDeclaredBy(baseClassDescription)).intercept(MethodDelegation.to(accessClassDescription))
                .make()
                .load(newClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                .getLoaded()
                .getDeclaredConstructor()
                .newInstance();
    }

    /**
     * Performs a benchmark of a class extension using Byte Buddy. This benchmark also uses the annotation-based approach
     * but creates delegation methods which do not require the creation of additional classes. This benchmark reuses a
     * precomputed delegator. This benchmark uses a type pool to compare against usage of the reflection API.
     *
     * @return The created instance, in order to avoid JIT removal.
     * @throws Exception If the invocation causes an exception.
     */
    @Benchmark
    public ExampleClass benchmarkByteBuddyWithAccessorAndReusedDelegatorWithTypePool() throws Exception {
        return (ExampleClass) new ByteBuddy()
                .with(TypeValidation.DISABLED)
                .ignore(none())
                .subclass(baseClassDescription)
                .method(isDeclaredBy(baseClassDescription)).intercept(accessInterceptorDescription)
                .make()
                .load(newClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                .getLoaded()
                .getDeclaredConstructor()
                .newInstance();
    }

    /**
     * Performs a benchmark of a class extension using Byte Buddy. This benchmark uses delegation but completes with a
     * hard-coded super method call.
     *
     * @return The created instance, in order to avoid JIT removal.
     * @throws Exception If the invocation causes an exception.
     */
    @Benchmark
    public ExampleClass benchmarkByteBuddyWithPrefix() throws Exception {
        return new ByteBuddy()
                .with(TypeValidation.DISABLED)
                .ignore(none())
                .subclass(baseClass)
                .method(isDeclaredBy(baseClass)).intercept(MethodDelegation.to(ByteBuddyPrefixInterceptor.class).andThen(SuperMethodCall.INSTANCE))
                .make()
                .load(newClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                .getLoaded()
                .getDeclaredConstructor()
                .newInstance();
    }

    /**
     * Performs a benchmark of a class extension using Byte Buddy. This benchmark uses delegation but completes with a
     * hard-coded super method call. This benchmark reuses a precomputed delegator.
     *
     * @return The created instance, in order to avoid JIT removal.
     * @throws Exception If the invocation causes an exception.
     */
    @Benchmark
    public ExampleClass benchmarkByteBuddyWithPrefixAndReusedDelegator() throws Exception {
        return new ByteBuddy()
                .with(TypeValidation.DISABLED)
                .ignore(none())
                .subclass(baseClass)
                .method(isDeclaredBy(baseClass)).intercept(prefixInterceptor.andThen(SuperMethodCall.INSTANCE))
                .make()
                .load(newClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                .getLoaded()
                .getDeclaredConstructor()
                .newInstance();
    }

    /**
     * Performs a benchmark of a class extension using Byte Buddy. This benchmark uses delegation but completes with a
     * hard-coded super method call. This benchmark uses a type pool to compare against usage of the reflection API.
     *
     * @return The created instance, in order to avoid JIT removal.
     * @throws Exception If the invocation causes an exception.
     */
    @Benchmark
    public ExampleClass benchmarkByteBuddyWithPrefixWithTypePool() throws Exception {
        return (ExampleClass) new ByteBuddy()
                .with(TypeValidation.DISABLED)
                .ignore(none())
                .subclass(baseClassDescription)
                .method(isDeclaredBy(baseClassDescription)).intercept(MethodDelegation.to(prefixClassDescription).andThen(SuperMethodCall.INSTANCE))
                .make()
                .load(newClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                .getLoaded()
                .getDeclaredConstructor()
                .newInstance();
    }

    /**
     * Performs a benchmark of a class extension using Byte Buddy. This benchmark uses delegation but completes with a
     * hard-coded super method call. This benchmark reuses a precomputed delegator. This benchmark uses a type pool to
     * compare against usage of the reflection API.
     *
     * @return The created instance, in order to avoid JIT removal.
     * @throws Exception If the invocation causes an exception.
     */
    @Benchmark
    public ExampleClass benchmarkByteBuddyWithPrefixAndReusedDelegatorWithTypePool() throws Exception {
        return (ExampleClass) new ByteBuddy()
                .with(TypeValidation.DISABLED)
                .ignore(none())
                .subclass(baseClassDescription)
                .method(isDeclaredBy(baseClassDescription)).intercept(prefixInterceptorDescription.andThen(SuperMethodCall.INSTANCE))
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
     * @throws java.lang.Exception If the invocation causes an exception.
     */
    @Benchmark
    public ExampleClass benchmarkByteBuddySpecialized() throws Exception {
        return new ByteBuddy()
                .with(TypeValidation.DISABLED)
                .ignore(none())
                .subclass(baseClass)
                .method(isDeclaredBy(baseClass)).intercept(SuperMethodCall.INSTANCE)
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
        enhancer.setUseFactory(false);
        enhancer.setInterceptDuringConstruction(true);
        enhancer.setClassLoader(newClassLoader());
        enhancer.setSuperclass(baseClass);
        CallbackHelper callbackHelper = new CallbackHelper(baseClass, new Class[0]) {
            protected Object getCallback(Method method) {
                if (method.getDeclaringClass() == baseClass) {
                    return new MethodInterceptor() {
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
     * @throws java.lang.Exception If the invocation causes an exception.
     */
    @Benchmark
    public ExampleClass benchmarkJavassist() throws Exception {
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
    public static class ByteBuddyProxyInterceptor {

        /**
         * The interceptor's constructor is not supposed to be invoked.
         */
        private ByteBuddyProxyInterceptor() {
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

    /**
     * Instead of using the {@link net.bytebuddy.implementation.SuperMethodCall} implementation, we are creating
     * delegate methods that allow the invocation of the original code.
     */
    public static class ByteBuddyAccessInterceptor {

        /**
         * The interceptor's constructor is not supposed to be invoked.
         */
        private ByteBuddyAccessInterceptor() {
            throw new UnsupportedOperationException();
        }

        /**
         * Calls the super method.
         *
         * @param target    The target instance.
         * @param arguments The arguments to the method.
         * @param method    A method for invoking the original code.
         * @return The return value of the method.
         * @throws Exception If the super method call yields an exception.
         */
        @RuntimeType
        public static Object intercept(@This Object target, @AllArguments Object[] arguments, @SuperMethod(privileged = false) Method method) throws Exception {
            return method.invoke(target, arguments);
        }
    }

    /**
     * An interceptor that is invoked prior to a super method call.
     */
    public static class ByteBuddyPrefixInterceptor {

        /**
         * The interceptor's constructor is not supposed to be invoked.
         */
        private ByteBuddyPrefixInterceptor() {
            throw new UnsupportedOperationException();
        }

        /**
         * Invoked prior to a method call.
         */
        public static void intercept() {
            /* do nothing */
        }
    }
}
