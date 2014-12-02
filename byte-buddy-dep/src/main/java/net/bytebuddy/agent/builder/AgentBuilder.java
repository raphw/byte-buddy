package net.bytebuddy.agent.builder;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoaderByteArrayInjector;
import net.bytebuddy.dynamic.scaffold.inline.MethodRebaseResolver;
import net.bytebuddy.instrumentation.LoadedTypeInitializer;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import net.bytebuddy.instrumentation.method.bytecode.stack.Removal;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.collection.ArrayFactory;
import net.bytebuddy.instrumentation.method.bytecode.stack.constant.ClassConstant;
import net.bytebuddy.instrumentation.method.bytecode.stack.constant.NullConstant;
import net.bytebuddy.instrumentation.method.bytecode.stack.constant.TextConstant;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodInvocation;
import net.bytebuddy.instrumentation.type.InstrumentedType;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.StreamDrainer;
import org.objectweb.asm.MethodVisitor;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static net.bytebuddy.matcher.ElementMatchers.*;
import static net.bytebuddy.utility.ByteBuddyCommons.join;
import static net.bytebuddy.utility.ByteBuddyCommons.nonNull;

/**
 * <p>
 * An agent builder provides a convenience API for defining a
 * <a href="http://docs.oracle.com/javase/6/docs/api/java/lang/instrument/package-summary.html">Java agent</a> using
 * Byte Buddy's
 * {@link net.bytebuddy.ByteBuddy#rebase(net.bytebuddy.instrumentation.type.TypeDescription, net.bytebuddy.dynamic.ClassFileLocator)}.
 * </p>
 * <p>
 * When defining several {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer}s, the agent builder always
 * applies the transformers that were supplied with the last applicable matcher. Therefore, more general transfromers
 * should be defined first.
 * </p>
 */
public interface AgentBuilder {

    /**
     * Matches a type being loaded in order to apply the supplied
     * {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer}s before loading this type.
     *
     * @param matcher A matcher that decides if the entailed
     *                {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer}s should be applied for a type that
     *                is being loaded.
     * @return A definable that represents this agent builder which allows for the definition of one or several
     * {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer}s to be applied when the given {@code matcher}
     * indicates a match.
     */
    Identified rebase(RawMatcher matcher);

    /**
     * Matches a type being loaded in order to apply the supplied
     * {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer}s before loading this type.
     *
     * @param typeMatcher An {@link net.bytebuddy.matcher.ElementMatcher} that is applied on the type being loaded that
     *                    decides if the entailed
     *                    {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer}s should be applied for that
     *                    type.
     * @return A definable that represents this agent builder which allows for the definition of one or several
     * {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer}s to be applied when the given {@code typeMatcher}
     * indicates a match.
     */
    Identified rebase(ElementMatcher<? super TypeDescription> typeMatcher);

    /**
     * Matches a type being loaded in order to apply the supplied
     * {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer}s before loading this type.
     *
     * @param typeMatcher        An {@link net.bytebuddy.matcher.ElementMatcher} that is applied on the type being
     *                           loaded that decides if the entailed
     *                           {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer}s should be applied for
     *                           that type.
     * @param classLoaderMatcher An {@link net.bytebuddy.matcher.ElementMatcher} that is applied to the
     *                           {@link java.lang.ClassLoader} that is loading the type being loaded. This matcher
     *                           is always applied first where the type matcher is not applied in case that this
     *                           matcher does not indicate a match.
     * @return A definable that represents this agent builder which allows for the definition of one or several
     * {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer}s to be applied when both the given
     * {@code typeMatcher} and {@code classLoaderMatcher} indicate a match.
     */
    Identified rebase(ElementMatcher<? super TypeDescription> typeMatcher,
                      ElementMatcher<? super ClassLoader> classLoaderMatcher);

    /**
     * Defines the given {@link net.bytebuddy.ByteBuddy} instance to be used by the created agent.
     *
     * @param byteBuddy The Byte Buddy instance to be used.
     * @return A new instance of this agent builder which makes use of the given {@code byteBuddy} instance.
     */
    AgentBuilder withByteBuddy(ByteBuddy byteBuddy);

    /**
     * Defines the given {@link net.bytebuddy.agent.builder.AgentBuilder.Listener} to be notified by the created agent.
     * The given listener is notified after any other listener that is already registered. If a listener is registered
     * twice, it is also notified twice.
     *
     * @param listener The listener to be notified.
     * @return A new instance of this agent builder which creates an agent that informs the given listener about
     * events.
     */
    AgentBuilder withListener(Listener listener);

    /**
     * Defines the use of the given binary locator for locating binary data to given class names.
     *
     * @param binaryLocator The binary locator to use.
     * @return A new instance of this agent builder which uses the given binary locator for looking up class files.
     */
    AgentBuilder withBinaryLocator(BinaryLocator binaryLocator);

    /**
     * Enables the use of the given native method prefix for instrumented methods. Note that this prefix is also
     * applied when preserving non-native methods. The use of this prefix is also registered when installing the
     * final agent with an {@link java.lang.instrument.Instrumentation}.
     *
     * @param prefix The prefix to be used.
     * @return A new instance of this agent builder which uses the given native method prefix.
     */
    AgentBuilder withNativeMethodPrefix(String prefix);

    /**
     * <p>
     * Disables the execution of any {@link net.bytebuddy.instrumentation.LoadedTypeInitializer}s that are registered
     * with a {@link net.bytebuddy.dynamic.DynamicType}. This might cause the dynamic type to malfunction if the
     * {@link net.bytebuddy.instrumentation.LoadedTypeInitializer} are not executed elsewhere before an instrumented
     * type is put in use for the first time.
     * </p>
     * <p>
     * In order to execute a self initialization, Byte Buddy adds a call back into any dynamic type's type initializer.
     * This call back requires the injection of a call back dispatcher into the system class loader what might not
     * be a feasible solution on distributed applications where classes are shared among different JVMs where a
     * different strategy for executing {@link net.bytebuddy.instrumentation.LoadedTypeInitializer}s might be
     * more appropriate.
     * </p>
     *
     * @return A new instance of this agent builder which does not apply self initialization.
     */
    AgentBuilder disableSelfInitialization();

    /**
     * Enables retransformation when this agent is installed. Note that retransformation on does not currently allow
     * for adding or removing fields or methods on the Hot Spot Virtual machine.
     *
     * @return A new instance of this agent builder which does not apply self initialization.
     */
    AgentBuilder allowRetransformation();

    /**
     * Creates a {@link java.lang.instrument.ClassFileTransformer} that implements the configuration of this
     * agent builder.
     *
     * @return A class file transformer that implements the configuration of this agent builder.
     */
    ClassFileTransformer makeRaw();

    /**
     * Creates and installs a {@link java.lang.instrument.ClassFileTransformer} that implements the configuration of
     * this agent builder with a given {@link java.lang.instrument.Instrumentation}.
     *
     * @param instrumentation The instrumentation on which this agent builder's configuration is to be installed.
     * @return The installed class file transformer.
     */
    ClassFileTransformer installOn(Instrumentation instrumentation);

    /**
     * Creates and installs a {@link java.lang.instrument.ClassFileTransformer} that implements the configuration of
     * this agent builder with the Byte Buddy-agent which must be installed prior to calling this method.
     *
     * @return The installed class file transformer.
     */
    ClassFileTransformer installOnByteBuddyAgent();

    /**
     * Describes an {@link net.bytebuddy.agent.builder.AgentBuilder} which was handed a matcher for identifying
     * types to instrumented in order to supply one or several
     * {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer}s.
     */
    static interface Identified {

        /**
         * This interface is used to allow for optionally providing several
         * {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer} to applied when a matcher identifies a type
         * to be instrumented. Any subsequent transformers are applied in the order they are registered.
         */
        static interface Extendable extends AgentBuilder, Identified {
            /* this is merely a unionizing interface that does not declare methods */
        }

        /**
         * Applies the given transformer for the already supplied matcher.
         *
         * @param transformer The transformer to apply.
         * @return This agent builder with the transformer being applied when the previously supplied matcher
         * identified a type for instrumentation which also allows for the registration of subsequent transformers.
         */
        Extendable transform(Transformer transformer);
    }

    static class Default implements AgentBuilder {

        /**
         * The name of the Byte Buddy agent class.
         */
        private static final String BYTE_BUDDY_AGENT_TYPE = "net.bytebuddy.agent.ByteBuddyAgent";

        /**
         * The name of the {@code ByteBuddyAgent} class's method for obtaining an instrumentation.
         */
        private static final String GET_INSTRUMENTATION_METHOD = "getInstrumentation";

        /**
         * Base for access to a reflective member to make the code more readable.
         */
        private static final Object STATIC_METHOD = null;

        protected static final String NO_NATIVE_PREFIX = "";

        private static final byte[] NO_TRANSFORMATION = null;

        private final ByteBuddy byteBuddy;

        private final BinaryLocator binaryLocator;

        private final Listener listener;

        private final String nativeMethodPrefix;

        private final boolean disableSelfInitialization;

        private final boolean retransformation;

        private final List<Entry> entries;

        public Default() {
            this(new ByteBuddy());
        }

        public Default(ByteBuddy byteBuddy) {
            this(nonNull(byteBuddy),
                    BinaryLocator.Default.INSTANCE,
                    Listener.NoOp.INSTANCE,
                    NO_NATIVE_PREFIX,
                    false,
                    false,
                    Collections.<Entry>emptyList());
        }

        protected Default(ByteBuddy byteBuddy,
                          BinaryLocator binaryLocator,
                          Listener listener,
                          String nativeMethodPrefix,
                          boolean disableSelfInitialization,
                          boolean retransformation,
                          List<Entry> entries) {
            this.byteBuddy = byteBuddy;
            this.binaryLocator = binaryLocator;
            this.listener = listener;
            this.nativeMethodPrefix = nativeMethodPrefix;
            this.disableSelfInitialization = disableSelfInitialization;
            this.retransformation = retransformation;
            this.entries = entries;
        }

        @Override
        public Identified rebase(RawMatcher matcher) {
            return new Matched(nonNull(matcher), Transformer.NoOp.INSTANCE);
        }

        @Override
        public Identified rebase(ElementMatcher<? super TypeDescription> typeMatcher) {
            return rebase(typeMatcher, any());
        }

        @Override
        public Identified rebase(ElementMatcher<? super TypeDescription> typeMatcher,
                                 ElementMatcher<? super ClassLoader> classLoaderMatcher) {
            return rebase(new RawMatcher.ForElementMatcherPair(nonNull(typeMatcher), nonNull(classLoaderMatcher)));
        }

        @Override
        public AgentBuilder withByteBuddy(ByteBuddy byteBuddy) {
            return new Default(nonNull(byteBuddy),
                    binaryLocator,
                    listener,
                    nativeMethodPrefix,
                    disableSelfInitialization,
                    retransformation,
                    entries);
        }

        @Override
        public AgentBuilder withListener(Listener listener) {
            return new Default(byteBuddy,
                    binaryLocator,
                    new Listener.Compound(this.listener, nonNull(listener)),
                    nativeMethodPrefix,
                    disableSelfInitialization,
                    retransformation,
                    entries);
        }

        @Override
        public AgentBuilder withBinaryLocator(BinaryLocator binaryLocator) {
            return new Default(byteBuddy,
                    nonNull(binaryLocator),
                    listener,
                    nativeMethodPrefix,
                    disableSelfInitialization,
                    retransformation,
                    entries);
        }

        @Override
        public AgentBuilder withNativeMethodPrefix(String prefix) {
            if (nonNull(prefix).length() == 0) {
                throw new IllegalArgumentException("The empty string is not a legal value for a native method prefix");
            }
            return new Default(byteBuddy,
                    binaryLocator,
                    listener,
                    prefix,
                    disableSelfInitialization,
                    retransformation,
                    entries);
        }

        @Override
        public AgentBuilder allowRetransformation() {
            return new Default(byteBuddy,
                    binaryLocator,
                    listener,
                    nativeMethodPrefix,
                    disableSelfInitialization,
                    true,
                    entries);
        }

        @Override
        public AgentBuilder disableSelfInitialization() {
            return new Default(byteBuddy,
                    binaryLocator,
                    listener,
                    nativeMethodPrefix,
                    true,
                    retransformation,
                    entries);
        }

        @Override
        public ClassFileTransformer makeRaw() {
            return new ExecutingTransformer();
        }

        @Override
        public ClassFileTransformer installOn(Instrumentation instrumentation) {
            ClassFileTransformer classFileTransformer = makeRaw();
            instrumentation.addTransformer(classFileTransformer, retransformation);
            if (!NO_NATIVE_PREFIX.equals(nonNull(nativeMethodPrefix))) {
                instrumentation.setNativeMethodPrefix(classFileTransformer, nativeMethodPrefix);
            }
            return classFileTransformer;
        }

        @Override
        public ClassFileTransformer installOnByteBuddyAgent() {
            try {
                return installOn((Instrumentation) ClassLoader.getSystemClassLoader()
                        .loadClass(BYTE_BUDDY_AGENT_TYPE)
                        .getDeclaredMethod(GET_INSTRUMENTATION_METHOD)
                        .invoke(STATIC_METHOD));
            } catch (Exception e) {
                throw new IllegalStateException("The Byte Buddy agent is not installed or not accessible", e);
            }
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other == null || getClass() != other.getClass()) return false;
            Default aDefault = (Default) other;
            return binaryLocator.equals(aDefault.binaryLocator)
                    && byteBuddy.equals(aDefault.byteBuddy)
                    && listener.equals(aDefault.listener)
                    && nativeMethodPrefix.equals(aDefault.nativeMethodPrefix)
                    && disableSelfInitialization == aDefault.disableSelfInitialization
                    && retransformation == aDefault.retransformation
                    && entries.equals(aDefault.entries);

        }

        @Override
        public int hashCode() {
            int result = byteBuddy.hashCode();
            result = 31 * result + binaryLocator.hashCode();
            result = 31 * result + listener.hashCode();
            result = 31 * result + nativeMethodPrefix.hashCode();
            result = 31 * result + (disableSelfInitialization ? 1 : 0);
            result = 31 * result + (retransformation ? 1 : 0);
            result = 31 * result + entries.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "AgentBuilder.Default{" +
                    "byteBuddy=" + byteBuddy +
                    ", binaryLocator=" + binaryLocator +
                    ", listener=" + listener +
                    ", nativeMethodPrefix=" + nativeMethodPrefix +
                    ", disableSelfInitialization=" + disableSelfInitialization +
                    ", retransformation=" + retransformation +
                    ", entries=" + entries +
                    '}';
        }

        protected class ExecutingTransformer implements ClassFileTransformer {

            private final MethodRebaseResolver.MethodNameTransformer methodNameTransformer;

            private final InitializationStrategy initializationStrategy;

            public ExecutingTransformer() {
                methodNameTransformer = NO_NATIVE_PREFIX.equals(nativeMethodPrefix)
                        ? new MethodRebaseResolver.MethodNameTransformer.Suffixing()
                        : new MethodRebaseResolver.MethodNameTransformer.Prefixing(nativeMethodPrefix);
                initializationStrategy = disableSelfInitialization
                        ? InitializationStrategy.NoOp.INSTANCE
                        : new InitializationStrategy.SelfInjection();
            }

            @Override
            public byte[] transform(ClassLoader classLoader,
                                    String internalTypeName,
                                    Class<?> classBeingRedefined,
                                    ProtectionDomain protectionDomain,
                                    byte[] binaryRepresentation) {
                String binaryTypeName = internalTypeName.replace('/', '.');
                try {
                    BinaryLocator.Initialized initialized = binaryLocator.initialize(binaryTypeName, binaryRepresentation, classLoader);
                    TypeDescription typeDescription = initialized.getTypePool().describe(binaryTypeName).resolve();
                    for (Entry entry : entries) {
                        if (entry.matches(typeDescription, classLoader, classBeingRedefined, protectionDomain)) {
                            DynamicType.Unloaded<?> dynamicType = initializationStrategy.apply(
                                    entry.transform(byteBuddy.rebase(typeDescription,
                                            initialized.getClassFileLocator(),
                                            methodNameTransformer))).make();
                            Map<TypeDescription, LoadedTypeInitializer> loadedTypeInitializers = dynamicType.getLoadedTypeInitializers();
                            if (loadedTypeInitializers.size() > 1) {
                                ClassLoaderByteArrayInjector injector = new ClassLoaderByteArrayInjector(classLoader, protectionDomain);
                                for (Map.Entry<TypeDescription, byte[]> auxiliary : dynamicType.getRawAuxiliaryTypes().entrySet()) {
                                    Class<?> type = injector.inject(auxiliary.getKey().getName(), auxiliary.getValue());
                                    initializationStrategy.initialize(type, loadedTypeInitializers.get(auxiliary.getKey()));
                                }
                            }
                            initializationStrategy.register(binaryTypeName,
                                    classLoader,
                                    loadedTypeInitializers.get(dynamicType.getTypeDescription()));
                            listener.onTransformation(dynamicType);
                            return dynamicType.getBytes();
                        }
                    }
                    listener.onIgnored(binaryTypeName);
                    return NO_TRANSFORMATION;
                } catch (Throwable throwable) {
                    listener.onError(binaryTypeName, throwable);
                    return NO_TRANSFORMATION;
                } finally {
                    listener.onComplete(binaryTypeName);
                }
            }

            @Override
            public String toString() {
                return "AgentBuilder.Default.ExecutingTransformer{" +
                        "agentBuilder=" + Default.this +
                        ", methodNameTransformer=" + methodNameTransformer +
                        ", initializationStrategy=" + initializationStrategy +
                        '}';
            }
        }

        protected static class Entry implements RawMatcher, Transformer {

            private final RawMatcher rawMatcher;

            private final Transformer transformer;

            public Entry(RawMatcher rawMatcher, Transformer transformer) {
                this.rawMatcher = rawMatcher;
                this.transformer = transformer;
            }

            @Override
            public boolean matches(TypeDescription typeDescription,
                                   ClassLoader classLoader,
                                   Class<?> classBeingRedefined,
                                   ProtectionDomain protectionDomain) {
                return rawMatcher.matches(typeDescription, classLoader, classBeingRedefined, protectionDomain);
            }

            @Override
            public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder) {
                return transformer.transform(builder);
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && rawMatcher.equals(((Entry) other).rawMatcher)
                        && transformer.equals(((Entry) other).transformer);
            }

            @Override
            public int hashCode() {
                int result = rawMatcher.hashCode();
                result = 31 * result + transformer.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "AgentBuilder.Default.Entry{" +
                        "rawMatcher=" + rawMatcher +
                        ", transformer=" + transformer +
                        '}';
            }
        }

        protected class Matched implements Identified.Extendable {

            private final RawMatcher rawMatcher;

            private final Transformer transformer;

            public Matched(RawMatcher rawMatcher,
                           Transformer transformer) {
                this.rawMatcher = rawMatcher;
                this.transformer = transformer;
            }

            @Override
            public Identified.Extendable transform(Transformer transformer) {
                return new Matched(rawMatcher, new Transformer.Compound(this.transformer, nonNull(transformer)));
            }

            @Override
            public Identified rebase(RawMatcher matcher) {
                return materialize().rebase(matcher);
            }

            @Override
            public Identified rebase(ElementMatcher<? super TypeDescription> typeMatcher) {
                return materialize().rebase(typeMatcher);
            }

            @Override
            public Identified rebase(ElementMatcher<? super TypeDescription> typeMatcher,
                                     ElementMatcher<? super ClassLoader> classLoaderMatcher) {
                return materialize().rebase(typeMatcher, classLoaderMatcher);
            }

            @Override
            public AgentBuilder withByteBuddy(ByteBuddy byteBuddy) {
                return materialize().withByteBuddy(byteBuddy);
            }

            @Override
            public AgentBuilder withListener(Listener listener) {
                return materialize().withListener(listener);
            }

            @Override
            public AgentBuilder withBinaryLocator(BinaryLocator binaryLocator) {
                return materialize().withBinaryLocator(binaryLocator);
            }

            @Override
            public AgentBuilder withNativeMethodPrefix(String prefix) {
                return materialize().withNativeMethodPrefix(prefix);
            }

            @Override
            public AgentBuilder disableSelfInitialization() {
                return materialize().disableSelfInitialization();
            }

            @Override
            public AgentBuilder allowRetransformation() {
                return materialize().allowRetransformation();
            }

            @Override
            public ClassFileTransformer makeRaw() {
                return materialize().makeRaw();
            }

            @Override
            public ClassFileTransformer installOn(Instrumentation instrumentation) {
                return materialize().installOn(instrumentation);
            }

            @Override
            public ClassFileTransformer installOnByteBuddyAgent() {
                return materialize().installOnByteBuddyAgent();
            }

            protected AgentBuilder materialize() {
                return new Default(byteBuddy,
                        binaryLocator,
                        listener,
                        nativeMethodPrefix,
                        disableSelfInitialization,
                        retransformation,
                        join(new Entry(rawMatcher, transformer), entries));
            }

            private Default getOuter() {
                return Default.this;
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && rawMatcher.equals(((Matched) other).rawMatcher)
                        && transformer.equals(((Matched) other).transformer)
                        && Default.this.equals(((Matched) other).getOuter());
            }

            @Override
            public int hashCode() {
                int result = rawMatcher.hashCode();
                result = 31 * result + transformer.hashCode();
                result = 31 * result + Default.this.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "AgentBuilder.Default.Matched{" +
                        "rawMatcher=" + rawMatcher +
                        ", transformer=" + transformer +
                        ", agentBuilder=" + Default.this +
                        '}';
            }
        }

        public static interface InitializationStrategy {

            void initialize(Class<?> type, LoadedTypeInitializer loadedTypeInitializer);

            static class SelfInjection implements InitializationStrategy, net.bytebuddy.instrumentation.Instrumentation, ByteCodeAppender {

                private final Nexus.Accessor accessor;

                public SelfInjection() {
                    accessor = Nexus.Accessor.INSTANCE;
                }

                @Override
                public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder) {
                    return builder.invokable(none()).intercept(this);
                }

                @Override
                public void initialize(Class<?> type, LoadedTypeInitializer loadedTypeInitializer) {
                    loadedTypeInitializer.onLoad(type);
                }

                @Override
                public InstrumentedType prepare(InstrumentedType instrumentedType) {
                    return instrumentedType.withInitializer(accessor.initializerFor(instrumentedType));
                }

                @Override
                public ByteCodeAppender appender(Target instrumentationTarget) {
                    return this;
                }

                @Override
                public boolean appendsCode() {
                    return true;
                }

                @Override
                public Size apply(MethodVisitor methodVisitor, Context instrumentationContext, MethodDescription instrumentedMethod) {
                    throw new IllegalStateException("Initialization strategy illegally applied to " + instrumentedMethod);
                }

                @Override
                public void register(String name, ClassLoader classLoader, LoadedTypeInitializer loadedTypeInitializer) {
                    if (loadedTypeInitializer.isAlive()) {
                        accessor.register(name, classLoader, loadedTypeInitializer);
                    }
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && accessor == ((SelfInjection) other).accessor;
                }

                @Override
                public int hashCode() {
                    return accessor.hashCode();
                }

                @Override
                public String toString() {
                    return "AgentBuilder.Default.InitializationStrategy.SelfInjection{" +
                            "accessor=" + accessor +
                            '}';
                }

                public static class Nexus {

                    private final String name;

                    private final ClassLoader classLoader;

                    public Nexus(Class<?> type) {
                        name = type.getName();
                        classLoader = type.getClassLoader();
                    }

                    public Nexus(String name, ClassLoader classLoader) {
                        this.name = name;
                        this.classLoader = classLoader;
                    }

                    @Override
                    public boolean equals(Object other) {
                        if (this == other) return true;
                        if (other == null || getClass() != other.getClass()) return false;
                        Nexus nexus = (Nexus) other;
                        return !(classLoader != null ? !classLoader.equals(nexus.classLoader) : nexus.classLoader != null)
                                && name.equals(nexus.name);
                    }

                    @Override
                    public int hashCode() {
                        int result = name.hashCode();
                        result = 31 * result + (classLoader != null ? classLoader.hashCode() : 0);
                        return result;
                    }

                    @Override
                    public String toString() {
                        return "AgentBuilder.Default.InitializationStrategy.SelfInjection.Nexus{" +
                                "name='" + name + '\'' +
                                ", classLoader=" + classLoader +
                                '}';
                    }

                    private static final ConcurrentMap<Nexus, Object> TYPE_INITIALIZERS = new ConcurrentHashMap<Nexus, Object>();

                    @SuppressWarnings("unused")
                    public static void initialize(Class<?> type)
                            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
                        Object typeInitializer = TYPE_INITIALIZERS.remove(new Nexus(type));
                        if (typeInitializer != null) {
                            typeInitializer.getClass().getMethod("onLoad", Class.class).invoke(typeInitializer, type);
                        }
                    }

                    @SuppressWarnings("unused")
                    public static void register(String name, ClassLoader classLoader, Object typeInitializer) {
                        TYPE_INITIALIZERS.put(new Nexus(name, classLoader), typeInitializer);
                    }

                    protected static enum Accessor {

                        INSTANCE;

                        private final Method registration;

                        private final MethodDescription systemClassLoader;

                        private final MethodDescription loadClass;

                        private final MethodDescription findMethod;

                        private final MethodDescription invokeMethod;

                        private Accessor() {
                            try {
                                ClassLoader classLoader = ClassLoader.getSystemClassLoader();
                                ClassLoaderByteArrayInjector injector = new ClassLoaderByteArrayInjector(classLoader);
                                Class<?> nexus = injector.inject(Nexus.class.getName(), new StreamDrainer().drain(classLoader
                                        .getResourceAsStream(Nexus.class.getName().replace('.', '/') + ".class")));
                                registration = nexus.getDeclaredMethod("register", String.class, ClassLoader.class, Object.class);
                                systemClassLoader = new TypeDescription.ForLoadedType(ClassLoader.class).getDeclaredMethods()
                                        .filter(named("getSystemClassLoader")).getOnly();
                                loadClass = new TypeDescription.ForLoadedType(ClassLoader.class).getDeclaredMethods()
                                        .filter(named("loadClass").and(takesArguments(String.class))).getOnly();
                                findMethod = new TypeDescription.ForLoadedType(Class.class).getDeclaredMethods()
                                        .filter(named("getDeclaredMethod").and(takesArguments(String.class, Class[].class))).getOnly();
                                invokeMethod = new TypeDescription.ForLoadedType(Method.class).getDeclaredMethods()
                                        .filter(named("invoke").and(takesArguments(Object.class, Object[].class))).getOnly();
                            } catch (Exception e) {
                                throw new IllegalStateException("Cannot create type initialization accessor", e);
                            }
                        }

                        public void register(String name, ClassLoader classLoader, Object typeInitializer) {
                            try {
                                registration.invoke(null, name, classLoader, typeInitializer);
                            } catch (IllegalAccessException e) {
                                throw new IllegalStateException("Cannot register type initializer for " + name, e);
                            } catch (InvocationTargetException e) {
                                throw new IllegalStateException("Cannot register type initializer for " + name, e.getCause());
                            }
                        }

                        public StackManipulation initializerFor(TypeDescription instrumentedType) {
                            return new StackManipulation.Compound(
                                    MethodInvocation.invoke(systemClassLoader),
                                    new TextConstant(Nexus.class.getName()),
                                    MethodInvocation.invoke(loadClass),
                                    new TextConstant("initialize"),
                                    ArrayFactory.targeting(new TypeDescription.ForLoadedType(Class.class))
                                            .withValues(Collections.singletonList(ClassConstant.of(new TypeDescription.ForLoadedType(Class.class)))),
                                    MethodInvocation.invoke(findMethod),
                                    NullConstant.INSTANCE,
                                    ArrayFactory.targeting(new TypeDescription.ForLoadedType(Object.class))
                                            .withValues(Collections.singletonList(ClassConstant.of(instrumentedType))),
                                    MethodInvocation.invoke(invokeMethod),
                                    Removal.SINGLE
                            );
                        }
                    }
                }
            }

            static enum NoOp implements InitializationStrategy {

                INSTANCE;

                @Override
                public void initialize(Class<?> type, LoadedTypeInitializer loadedTypeInitializer) {
                    /* do nothing */
                }

                @Override
                public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder) {
                    return builder;
                }


                @Override
                public void register(String name, ClassLoader classLoader, LoadedTypeInitializer loadedTypeInitializer) {
                    /* do nothing */
                }
            }

            DynamicType.Builder<?> apply(DynamicType.Builder<?> builder);

            void register(String name, ClassLoader classLoader, LoadedTypeInitializer loadedTypeInitializer);
        }
    }

    /**
     * A matcher that allows to determine if a {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer}
     * should be applied during the execution of a {@link java.lang.instrument.ClassFileTransformer} that was
     * generated by an {@link net.bytebuddy.agent.builder.AgentBuilder}.
     */
    static interface RawMatcher {

        /**
         * Decides if the given {@code typeDescription} should be instrumented with the entailed
         * {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer}s.
         *
         * @param typeDescription     A description of the type to be instrumented.
         * @param classLoader         The class loader of the instrumented type. Might be {@code null} if this class
         *                            loader represents the bootstrap class loader.
         * @param classBeingRedefined The class being redefined which is only not {@code null} if a retransofmration
         *                            is applied.
         * @param protectionDomain    The protection domain of the type being transformed.
         * @return {@code true} if the entailed {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer}s should
         * be applied for the given {@code typeDescription}.
         */
        boolean matches(TypeDescription typeDescription,
                        ClassLoader classLoader,
                        Class<?> classBeingRedefined,
                        ProtectionDomain protectionDomain);

        /**
         * A raw matcher implementation that checks a {@link net.bytebuddy.instrumentation.type.TypeDescription}
         * and its {@link java.lang.ClassLoader} against two suitable matchers in order to determine if the matched
         * type should be instrumented.
         */
        static class ForElementMatcherPair implements RawMatcher {

            /**
             * The type matcher to apply to a {@link net.bytebuddy.instrumentation.type.TypeDescription}.
             */
            private final ElementMatcher<? super TypeDescription> typeMatcher;

            /**
             * The class loader to apply to a {@link java.lang.ClassLoader}.
             */
            private final ElementMatcher<? super ClassLoader> classLoaderMatcher;

            /**
             * Creates a new {@link net.bytebuddy.agent.builder.AgentBuilder.RawMatcher} that only matches the
             * supplied {@link net.bytebuddy.instrumentation.type.TypeDescription} and its
             * {@link java.lang.ClassLoader} against two matcher in order to decied if an instrumentation should
             * be conducted.
             *
             * @param typeMatcher        The type matcher to apply to a
             *                           {@link net.bytebuddy.instrumentation.type.TypeDescription}.
             * @param classLoaderMatcher The class loader to apply to a {@link java.lang.ClassLoader}.
             */
            public ForElementMatcherPair(ElementMatcher<? super TypeDescription> typeMatcher,
                                         ElementMatcher<? super ClassLoader> classLoaderMatcher) {
                this.typeMatcher = typeMatcher;
                this.classLoaderMatcher = classLoaderMatcher;
            }

            @Override
            public boolean matches(TypeDescription typeDescription,
                                   ClassLoader classLoader,
                                   Class<?> classBeingRedefined,
                                   ProtectionDomain protectionDomain) {
                return classLoaderMatcher.matches(classLoader) && typeMatcher.matches(typeDescription);
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && classLoaderMatcher.equals(((ForElementMatcherPair) other).classLoaderMatcher)
                        && typeMatcher.equals(((ForElementMatcherPair) other).typeMatcher);
            }

            @Override
            public int hashCode() {
                int result = typeMatcher.hashCode();
                result = 31 * result + classLoaderMatcher.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "AgentBuilder.RawMatcher.ForElementMatcherPair{" +
                        "typeMatcher=" + typeMatcher +
                        ", classLoaderMatcher=" + classLoaderMatcher +
                        '}';
            }
        }
    }

    /**
     * A transformer allows to apply modifications to a {@link net.bytebuddy.dynamic.DynamicType}. Such a modification
     * is then applied to any instrumented type that was matched by the preceding matcher.
     */
    static interface Transformer {

        /**
         * A no-op implementation of a {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer} that does
         * not modify the supplied dynamic type.
         */
        static enum NoOp implements Transformer {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder) {
                return builder;
            }
        }

        /**
         * A compound transformer that allows to group several
         * {@link net.bytebuddy.agent.builder.AgentBuilder.Transformer}s as a single transformer.
         */
        static class Compound implements Transformer {

            /**
             * The transformers to apply in their application order.
             */
            private final Transformer[] transformer;

            /**
             * Creates a new compound transformer.
             *
             * @param transformer The transformers to apply in their application order.
             */
            public Compound(Transformer... transformer) {
                this.transformer = transformer;
            }

            @Override
            public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder) {
                for (Transformer transformer : this.transformer) {
                    builder = transformer.transform(builder);
                }
                return builder;
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && Arrays.equals(transformer, ((Compound) other).transformer);
            }

            @Override
            public int hashCode() {
                return Arrays.hashCode(transformer);
            }

            @Override
            public String toString() {
                return "AgentBuilder.Transformer.Compound{" +
                        "transformer=" + Arrays.toString(transformer) +
                        '}';
            }
        }

        /**
         * Allows for a transformation of a {@link net.bytebuddy.dynamic.DynamicType.Builder}.
         *
         * @param builder The dynamic builder to transform.
         * @return A transformed version of the supplied {@code builder}.
         */
        DynamicType.Builder<?> transform(DynamicType.Builder<?> builder);
    }

    /**
     * A binary locator allows to specify how binary data is located by an
     * {@link net.bytebuddy.agent.builder.AgentBuilder}.
     */
    static interface BinaryLocator {

        /**
         * Initializes this binary locator.
         *
         * @param typeName             The binary name of the type that is being instrumented.
         * @param binaryRepresentation The binary representation of the instrumented type.
         * @param classLoader          The class loader of the instrumented type. Might be {@code null} if this class
         *                             loader represents the bootstrap class loader.
         * @return This binary locator in its initialized form.
         */
        Initialized initialize(String typeName, byte[] binaryRepresentation, ClassLoader classLoader);

        /**
         * A default implementation of a {@link net.bytebuddy.agent.builder.AgentBuilder.BinaryLocator} that
         * is using a {@link net.bytebuddy.pool.TypePool.Default} with a
         * {@link net.bytebuddy.pool.TypePool.CacheProvider.Simple} and a
         * {@link net.bytebuddy.dynamic.ClassFileLocator.ForClassLoader}.
         */
        static enum Default implements BinaryLocator {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public BinaryLocator.Initialized initialize(String typeName,
                                                        byte[] binaryRepresentation,
                                                        ClassLoader classLoader) {
                return new Initialized(typeName,
                        binaryRepresentation,
                        new TypePool.CacheProvider.Simple(),
                        ClassFileLocator.ForClassLoader.of(classLoader));
            }

            /**
             * The {@link net.bytebuddy.agent.builder.AgentBuilder.BinaryLocator.Default} in its initialized form.
             */
            protected static class Initialized implements BinaryLocator.Initialized, ClassFileLocator {

                /**
                 * The binary name of the instrumented type.
                 */
                private final String typeName;

                /**
                 * The binary representation of the instrumented type.
                 */
                private final byte[] binaryRepresentation;

                /**
                 * The class file locator to use.
                 */
                private final ClassFileLocator classFileLocator;

                /**
                 * The type pool to use.
                 */
                private final TypePool typePool;

                /**
                 * Creates a new initialized form of a default binary locator.
                 *
                 * @param typeName             The binary name of the type that is being instrumented.
                 * @param binaryRepresentation The binary representation of the instrumented type.
                 * @param cacheProvider        The cache provider to use.
                 * @param classFileLocator     The class file locator to use.
                 */
                public Initialized(String typeName,
                                   byte[] binaryRepresentation,
                                   TypePool.CacheProvider cacheProvider,
                                   ClassFileLocator classFileLocator) {
                    this.typeName = typeName;
                    this.binaryRepresentation = binaryRepresentation;
                    typePool = new TypePool.Default(cacheProvider, classFileLocator);
                    this.classFileLocator = classFileLocator;
                }

                @Override
                public TypePool getTypePool() {
                    return typePool;
                }

                @Override
                public ClassFileLocator getClassFileLocator() {
                    return this;
                }

                @Override
                public ClassFileLocator.Resolution locate(String typeName) throws IOException {
                    return typeName.equals(typeName)
                            ? new ClassFileLocator.Resolution.Explicit(binaryRepresentation)
                            : classFileLocator.locate(typeName);
                }

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (other == null || getClass() != other.getClass()) return false;
                    Initialized that = (Initialized) other;
                    return Arrays.equals(binaryRepresentation, that.binaryRepresentation)
                            && classFileLocator.equals(that.classFileLocator)
                            && typeName.equals(that.typeName)
                            && typePool.equals(that.typePool);
                }

                @Override
                public int hashCode() {
                    int result = typeName.hashCode();
                    result = 31 * result + Arrays.hashCode(binaryRepresentation);
                    result = 31 * result + classFileLocator.hashCode();
                    result = 31 * result + typePool.hashCode();
                    return result;
                }

                @Override
                public String toString() {
                    return "AgentBuilder.BinaryLocator.Default.Initialized{" +
                            "typeName='" + typeName + '\'' +
                            ", binaryRepresentation=" + Arrays.toString(binaryRepresentation) +
                            ", classFileLocator=" + classFileLocator +
                            ", typePool=" + typePool +
                            '}';
                }
            }
        }

        /**
         * A {@link net.bytebuddy.agent.builder.AgentBuilder.BinaryLocator} in initialized state.
         */
        static interface Initialized {

            /**
             * Returns the type pool to be used of an {@link net.bytebuddy.agent.builder.AgentBuilder}.
             *
             * @return The type pool to use.
             */
            TypePool getTypePool();

            /**
             * Returns the class file locator to be used of an {@link net.bytebuddy.agent.builder.AgentBuilder}.
             *
             * @return The class file locator to use.
             */
            ClassFileLocator getClassFileLocator();
        }
    }

    /**
     * A listener that is informed about events that occur during an instrumentation process.
     */
    static interface Listener {

        /**
         * Invoked right before a successful transformation is applied.
         *
         * @param dynamicType The dynamic type that was created.
         */
        void onTransformation(DynamicType dynamicType);

        /**
         * Invoked when an error has occurred.
         *
         * @param typeName  The binary name of the instrumented type.
         * @param throwable The occurred error.
         */
        void onError(String typeName, Throwable throwable);

        /**
         * Invokes when a type is not transformed.
         *
         * @param typeName The binary name of the type.
         */
        void onIgnored(String typeName);

        /**
         * Invoked after a class was attempted to be loaded, independently of its treatment.
         *
         * @param typeName The binary name of the instrumented type.
         */
        void onComplete(String typeName);

        /**
         * A no-op implementation of a {@link net.bytebuddy.agent.builder.AgentBuilder.Listener}.
         */
        static enum NoOp implements Listener {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public void onTransformation(DynamicType dynamicType) {
                /* do nothing */
            }

            @Override
            public void onError(String typeName, Throwable throwable) {
                /* do nothing */
            }

            @Override
            public void onIgnored(String typeName) {
                /* do nothing */
            }

            @Override
            public void onComplete(String typeName) {
                /* do nothing */
            }
        }

        /**
         * A compound listener that allows to group several listeners in one instance.
         */
        static class Compound implements Listener {

            /**
             * The listeners that are represented by this compound listener in their application order.
             */
            private final Listener[] listener;

            /**
             * Creates a new compound listener.
             *
             * @param listener The listeners to apply in their application order.
             */
            public Compound(Listener... listener) {
                this.listener = listener;
            }

            @Override
            public void onTransformation(DynamicType dynamicType) {
                for (Listener listener : this.listener) {
                    listener.onTransformation(dynamicType);
                }
            }

            @Override
            public void onError(String typeName, Throwable throwable) {
                for (Listener listener : this.listener) {
                    listener.onError(typeName, throwable);
                }
            }

            @Override
            public void onIgnored(String typeName) {
                for (Listener listener : this.listener) {
                    listener.onIgnored(typeName);
                }
            }

            @Override
            public void onComplete(String typeName) {
                for (Listener listener : this.listener) {
                    listener.onComplete(typeName);
                }
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && Arrays.equals(listener, ((Compound) other).listener);
            }

            @Override
            public int hashCode() {
                return Arrays.hashCode(listener);
            }

            @Override
            public String toString() {
                return "AgentBuilder.Listener.Compound{" +
                        "listener=" + Arrays.toString(listener) +
                        '}';
            }
        }
    }
}
