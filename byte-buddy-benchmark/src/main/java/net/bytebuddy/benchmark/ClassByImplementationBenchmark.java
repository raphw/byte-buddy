package net.bytebuddy.benchmark;

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.benchmark.specimen.ExampleInterface;
import net.bytebuddy.dynamic.ClassLoadingStrategy;
import net.bytebuddy.instrumentation.StubMethod;
import net.sf.cglib.proxy.CallbackHelper;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.FixedValue;
import net.sf.cglib.proxy.NoOp;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.GenerateMicroBenchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.TimeUnit;

import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.isDeclaredBy;
import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.none;

/**
 * This benchmark dynamically creates a class which implements {@link net.bytebuddy.benchmark.specimen.ExampleInterface}
 * which overrides all methods to invoke the direct super class's implementation. The benchmark furthermore creates an
 * instance of this class since some code generation frameworks rely on this property.
 */
public class ClassByImplementationBenchmark {

    /**
     * The base class to be subclassed in all benchmarks.
     */
    public static final Class<? extends ExampleInterface> BASE_CLASS = ExampleInterface.class;

    /**
     * The default reference value. By defining the default reference value as a string type instead of as an object
     * type, the field is inlined by the compiler, similar to the primitive values.
     */
    public static final String DEFAULT_REFERENCE_VALUE = null;

    /**
     * The default {@code boolean} value.
     */
    public static final boolean DEFAULT_BOOLEAN_VALUE = false;

    /**
     * The default {@code byte} value.
     */
    public static final byte DEFAULT_BYTE_VALUE = 0;

    /**
     * The default {@code short} value.
     */
    public static final short DEFAULT_SHORT_VALUE = 0;

    /**
     * The default {@code char} value.
     */
    public static final char DEFAULT_CHAR_VALUE = 0;

    /**
     * The default {@code int} value.
     */
    public static final int DEFAULT_INT_VALUE = 0;

    /**
     * The default {@code long} value.
     */
    public static final long DEFAULT_LONG_VALUE = 0L;

    /**
     * The default {@code float} value.
     */
    public static final float DEFAULT_FLOAT_VALUE = 0f;

    /**
     * The default {@code double} value.
     */
    public static final double DEFAULT_DOUBLE_VALUE = 0d;

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
     * Performs a benchmark of an interface implementation using Byte Buddy.
     *
     * @return The created instance, in order to avoid JIT removal.
     * @throws java.lang.Exception If the reflective invocation causes an exception.
     */
    @GenerateMicroBenchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public ExampleInterface benchmarkByteBuddy() throws Exception {
        return new ByteBuddy()
                .withIgnoredMethods(none())
                .subclass(BASE_CLASS)
                .method(isDeclaredBy(BASE_CLASS)).intercept(StubMethod.INSTANCE)
                .make()
                .load(newClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                .getLoaded()
                .newInstance();
    }

    /**
     * Performs a benchmark of an interface implementation using cglib.
     *
     * @return The created instance, in order to avoid JIT removal.
     */
    @GenerateMicroBenchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public ExampleInterface benchmarkCglib() {
        Enhancer enhancer = new Enhancer();
        enhancer.setUseCache(false);
        enhancer.setClassLoader(newClassLoader());
        enhancer.setSuperclass(BASE_CLASS);
        CallbackHelper callbackHelper = new CallbackHelper(Object.class, new Class[]{BASE_CLASS}) {
            @Override
            protected Object getCallback(Method method) {
                if (method.getDeclaringClass() == BASE_CLASS) {
                    return new FixedValue() {
                        @Override
                        public Object loadObject() throws Exception {
                            return null;
                        }
                    };
                } else {
                    return NoOp.INSTANCE;
                }
            }
        };
        enhancer.setCallbackFilter(callbackHelper);
        enhancer.setCallbacks(callbackHelper.getCallbacks());
        return (ExampleInterface) enhancer.create();
    }

    /**
     * Performs a benchmark of an interface implementation using javassist proxies.
     *
     * @return The created instance, in order to avoid JIT removal.
     * @throws java.lang.Exception If the reflective invocation causes an exception.
     */
    @GenerateMicroBenchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public ExampleInterface benchmarkJavassist() throws Exception {
        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.setUseCache(false);
        ProxyFactory.classLoaderProvider = new ProxyFactory.ClassLoaderProvider() {
            @Override
            public ClassLoader get(ProxyFactory proxyFactory) {
                return newClassLoader();
            }
        };
        proxyFactory.setSuperclass(Object.class);
        proxyFactory.setInterfaces(new Class<?>[]{BASE_CLASS});
        proxyFactory.setFilter(new MethodFilter() {
            public boolean isHandled(Method method) {
                return true;
            }
        });
        Object instance = proxyFactory.createClass().newInstance();
        ((javassist.util.proxy.Proxy) instance).setHandler(new MethodHandler() {
            public Object invoke(Object self,
                                 Method thisMethod,
                                 Method proceed,
                                 Object[] args) throws Throwable {
                Class<?> returnType = thisMethod.getReturnType();
                if (returnType.isPrimitive()) {
                    if (returnType == boolean.class) {
                        return DEFAULT_BOOLEAN_VALUE;
                    } else if (returnType == byte.class) {
                        return DEFAULT_BYTE_VALUE;
                    } else if (returnType == short.class) {
                        return DEFAULT_SHORT_VALUE;
                    } else if (returnType == char.class) {
                        return DEFAULT_CHAR_VALUE;
                    } else if (returnType == int.class) {
                        return DEFAULT_INT_VALUE;
                    } else if (returnType == long.class) {
                        return DEFAULT_LONG_VALUE;
                    } else if (returnType == float.class) {
                        return DEFAULT_FLOAT_VALUE;
                    } else {
                        return DEFAULT_DOUBLE_VALUE;
                    }
                } else {
                    return DEFAULT_REFERENCE_VALUE;
                }
            }
        });
        return (ExampleInterface) instance;
    }

    /**
     * Performs a benchmark of an interface implementation using the Java Class Library's utilities.
     *
     * @return The created instance, in order to avoid JIT removal.
     * @throws java.lang.Exception If the reflective invocation causes an exception.
     */
    @GenerateMicroBenchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public ExampleInterface benchmarkJdkProxy() throws Exception {
        return (ExampleInterface) Proxy.newProxyInstance(newClassLoader(),
                new Class<?>[]{BASE_CLASS},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        Class<?> returnType = method.getReturnType();
                        if (returnType.isPrimitive()) {
                            if (returnType == boolean.class) {
                                return DEFAULT_BOOLEAN_VALUE;
                            } else if (returnType == byte.class) {
                                return DEFAULT_BYTE_VALUE;
                            } else if (returnType == short.class) {
                                return DEFAULT_SHORT_VALUE;
                            } else if (returnType == char.class) {
                                return DEFAULT_CHAR_VALUE;
                            } else if (returnType == int.class) {
                                return DEFAULT_INT_VALUE;
                            } else if (returnType == long.class) {
                                return DEFAULT_LONG_VALUE;
                            } else if (returnType == float.class) {
                                return DEFAULT_FLOAT_VALUE;
                            } else {
                                return DEFAULT_DOUBLE_VALUE;
                            }
                        } else {
                            return DEFAULT_REFERENCE_VALUE;
                        }
                    }
                }
        );
    }
}
