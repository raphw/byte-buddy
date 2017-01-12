package net.bytebuddy.benchmark;

import javassist.util.proxy.MethodFilter;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.benchmark.specimen.ExampleInterface;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.StubMethod;
import net.bytebuddy.pool.TypePool;
import net.sf.cglib.proxy.CallbackHelper;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.FixedValue;
import net.sf.cglib.proxy.NoOp;
import org.openjdk.jmh.annotations.*;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.TimeUnit;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static net.bytebuddy.matcher.ElementMatchers.none;

/**
 * <p>
 * This benchmark dynamically creates a class which implements {@link net.bytebuddy.benchmark.specimen.ExampleInterface}
 * which overrides all methods to invoke the direct super class's implementation. The benchmark furthermore creates an
 * instance of this class since some code generation frameworks rely on this property.
 * </p>
 * <p>
 * Note that this class defines all values that are accessed by benchmark methods as instance fields. This way, the JIT
 * compiler's capability of constant folding is limited in order to produce more comparable test results.
 * </p>
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
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
     * The base class to be subclassed in all benchmarks.
     */
    private Class<? extends ExampleInterface> baseClass = BASE_CLASS;

    /**
     * The default reference value. By defining the default reference value as a string type instead of as an object
     * type, the field is inlined by the compiler, similar to the primitive values.
     */
    private String defaultReferenceValue = DEFAULT_REFERENCE_VALUE;

    /**
     * The default {@code boolean} value.
     */
    private boolean defaultBooleanValue = DEFAULT_BOOLEAN_VALUE;

    /**
     * The default {@code byte} value.
     */
    private byte defaultByteValue = DEFAULT_BYTE_VALUE;

    /**
     * The default {@code short} value.
     */
    private short defaultShortValue = DEFAULT_SHORT_VALUE;

    /**
     * The default {@code char} value.
     */
    private char defaultCharValue = DEFAULT_CHAR_VALUE;

    /**
     * The default {@code int} value.
     */
    private int defaultIntValue = DEFAULT_INT_VALUE;

    /**
     * The default {@code long} value.
     */
    private long defaultLongValue = DEFAULT_LONG_VALUE;

    /**
     * The default {@code float} value.
     */
    private float defaultFloatValue = DEFAULT_FLOAT_VALUE;

    /**
     * The default {@code double} value.
     */
    private double defaultDoubleValue = DEFAULT_DOUBLE_VALUE;

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
     * A description of {@link ClassByExtensionBenchmark#baseClass}.
     */
    private TypeDescription baseClassDescription;

    /**
     * Sets up this benchmark.
     */
    @Setup
    public void setup() {
        baseClassDescription = TypePool.Default.ofClassPath().describe(baseClass.getName()).resolve();
    }

    /**
     * Creates a baseline for the benchmark.
     *
     * @return A simple object that is not transformed.
     */
    @Benchmark
    public ExampleInterface baseline() {
        return new ExampleInterface() {
            @Override
            public boolean method(boolean arg) {
                return false;
            }

            @Override
            public byte method(byte arg) {
                return 0;
            }

            @Override
            public short method(short arg) {
                return 0;
            }

            @Override
            public int method(int arg) {
                return 0;
            }

            @Override
            public char method(char arg) {
                return 0;
            }

            @Override
            public long method(long arg) {
                return 0;
            }

            @Override
            public float method(float arg) {
                return 0;
            }

            @Override
            public double method(double arg) {
                return 0;
            }

            @Override
            public Object method(Object arg) {
                return null;
            }

            @Override
            public boolean[] method(boolean arg1, boolean arg2, boolean arg3) {
                return null;
            }

            @Override
            public byte[] method(byte arg1, byte arg2, byte arg3) {
                return null;
            }

            @Override
            public short[] method(short arg1, short arg2, short arg3) {
                return null;
            }

            @Override
            public int[] method(int arg1, int arg2, int arg3) {
                return null;
            }

            @Override
            public char[] method(char arg1, char arg2, char arg3) {
                return null;
            }

            @Override
            public long[] method(long arg1, long arg2, long arg3) {
                return null;
            }

            @Override
            public float[] method(float arg1, float arg2, float arg3) {
                return null;
            }

            @Override
            public double[] method(double arg1, double arg2, double arg3) {
                return null;
            }

            @Override
            public Object[] method(Object arg1, Object arg2, Object arg3) {
                return null;
            }
        };
    }

    /**
     * Performs a benchmark of an interface implementation using Byte Buddy.
     *
     * @return The created instance, in order to avoid JIT removal.
     * @throws java.lang.Exception If the reflective invocation causes an exception.
     */
    @Benchmark
    public ExampleInterface benchmarkByteBuddy() throws Exception {
        return new ByteBuddy()
                .with(TypeValidation.DISABLED)
                .ignore(none())
                .subclass(baseClass)
                .method(isDeclaredBy(baseClass)).intercept(StubMethod.INSTANCE)
                .make()
                .load(newClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                .getLoaded()
                .getDeclaredConstructor()
                .newInstance();
    }

    /**
     * Performs a benchmark of an interface implementation using Byte Buddy. This benchmark uses a type pool to compare against
     * usage of the reflection API.
     *
     * @return The created instance, in order to avoid JIT removal.
     * @throws java.lang.Exception If the reflective invocation causes an exception.
     */
    @Benchmark
    public ExampleInterface benchmarkByteBuddyWithTypePool() throws Exception {
        return (ExampleInterface) new ByteBuddy()
                .with(TypeValidation.DISABLED)
                .ignore(none())
                .subclass(baseClassDescription)
                .method(isDeclaredBy(baseClassDescription)).intercept(StubMethod.INSTANCE)
                .make()
                .load(newClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                .getLoaded()
                .getDeclaredConstructor()
                .newInstance();
    }

    /**
     * Performs a benchmark of an interface implementation using cglib.
     *
     * @return The created instance, in order to avoid JIT removal.
     */
    @Benchmark
    public ExampleInterface benchmarkCglib() {
        Enhancer enhancer = new Enhancer();
        enhancer.setUseCache(false);
        enhancer.setClassLoader(newClassLoader());
        enhancer.setSuperclass(baseClass);
        CallbackHelper callbackHelper = new CallbackHelper(Object.class, new Class[]{baseClass}) {
            @Override
            protected Object getCallback(Method method) {
                if (method.getDeclaringClass() == baseClass) {
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
    @Benchmark
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
        proxyFactory.setInterfaces(new Class<?>[]{baseClass});
        proxyFactory.setFilter(new MethodFilter() {
            public boolean isHandled(Method method) {
                return true;
            }
        });
        @SuppressWarnings("unchecked")
        Object instance = proxyFactory.createClass().getDeclaredConstructor().newInstance();
        ((javassist.util.proxy.Proxy) instance).setHandler(new MethodHandler() {
            public Object invoke(Object self,
                                 Method thisMethod,
                                 Method proceed,
                                 Object[] args) throws Throwable {
                Class<?> returnType = thisMethod.getReturnType();
                if (returnType.isPrimitive()) {
                    if (returnType == boolean.class) {
                        return defaultBooleanValue;
                    } else if (returnType == byte.class) {
                        return defaultByteValue;
                    } else if (returnType == short.class) {
                        return defaultShortValue;
                    } else if (returnType == char.class) {
                        return defaultCharValue;
                    } else if (returnType == int.class) {
                        return defaultIntValue;
                    } else if (returnType == long.class) {
                        return defaultLongValue;
                    } else if (returnType == float.class) {
                        return defaultFloatValue;
                    } else {
                        return defaultDoubleValue;
                    }
                } else {
                    return defaultReferenceValue;
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
    @Benchmark
    public ExampleInterface benchmarkJdkProxy() throws Exception {
        return (ExampleInterface) Proxy.newProxyInstance(newClassLoader(),
                new Class<?>[]{baseClass},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        Class<?> returnType = method.getReturnType();
                        if (returnType.isPrimitive()) {
                            if (returnType == boolean.class) {
                                return defaultBooleanValue;
                            } else if (returnType == byte.class) {
                                return defaultByteValue;
                            } else if (returnType == short.class) {
                                return defaultShortValue;
                            } else if (returnType == char.class) {
                                return defaultCharValue;
                            } else if (returnType == int.class) {
                                return defaultIntValue;
                            } else if (returnType == long.class) {
                                return defaultLongValue;
                            } else if (returnType == float.class) {
                                return defaultFloatValue;
                            } else {
                                return defaultDoubleValue;
                            }
                        } else {
                            return defaultReferenceValue;
                        }
                    }
                }
        );
    }
}
