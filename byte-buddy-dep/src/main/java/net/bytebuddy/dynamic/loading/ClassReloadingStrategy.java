package net.bytebuddy.dynamic.loading;

import net.bytebuddy.dynamic.ClassLoadingStrategy;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.utility.StreamDrainer;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.*;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * The class reloading strategy allows to redefine loaded {@link java.lang.Class}es. Note that this strategy
 * will always attempt to load an existing class prior to its redefinition, even if this class is not yet loaded.
 * </p>
 * <p>
 * <b>Note</b>: In order to redefine any type, neither its name or its modifiers must be changed. Furthermore, no
 * fields or methods must be removed or added. This makes this strategy generally incompatible to applying it to a
 * rebased class definition as by {@link net.bytebuddy.ByteBuddy#rebase(Class)} which copies the original method
 * implementations to additional methods. Furthermore, even the {@link net.bytebuddy.ByteBuddy#redefine(Class)}
 * adds a method if the original class contains an explicit <i>class initializer</i>. For these reasons, it is not
 * recommended to use this {@link net.bytebuddy.dynamic.ClassLoadingStrategy} with arbitrary classes.
 * </p>
 */
public class ClassReloadingStrategy implements ClassLoadingStrategy {

    /**
     * The size of the buffer.
     */
    private static final int BUFFER_SIZE = 1024;

    /**
     * A convenience variable representing the end of a stream to make the code more readable.
     */
    private static final int END_OF_STREAM = -1;

    /**
     * A convenience variable representing the first index of an array to make the code more readable.
     */
    private static final int FIRST_INDEX = 0;

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

    /**
     * The class file extension.
     */
    private static final String CLASS_FILE_EXTENSION = ".class";

    /**
     * This instance's instrumentation.
     */
    private final Instrumentation instrumentation;

    /**
     * An engine which performs the actual redefinition of a {@link java.lang.Class}.
     */
    private final Engine engine;

    /**
     * Creates a class reloading strategy for the given instrumentation. The given instrumentation must either
     * support {@link java.lang.instrument.Instrumentation#isRedefineClassesSupported()} or
     * {@link java.lang.instrument.Instrumentation#isRetransformClassesSupported()}. If both modes are supported,
     * classes will be transformed using a class redefinition.
     *
     * @param instrumentation The instrumentation to be used by this reloading strategy.
     */
    public ClassReloadingStrategy(Instrumentation instrumentation) {
        this.instrumentation = instrumentation;
        if (instrumentation.isRedefineClassesSupported()) {
            engine = Engine.REDEFINITION;
        } else if (instrumentation.isRetransformClassesSupported()) {
            engine = Engine.RETRANSFORMATION;
        } else {
            throw new IllegalArgumentException("Instrumentation does not support class redefinition: " + instrumentation);
        }
    }

    /**
     * Creates a class reloading strategy for the given instrumentation using an explicit transformation strategy which
     * is represented by an {@link net.bytebuddy.dynamic.loading.ClassReloadingStrategy.Engine}.
     *
     * @param instrumentation The instrumentation to be used by this reloading strategy.
     * @param engine          An engine which performs the actual redefinition of a {@link java.lang.Class}.
     */
    public ClassReloadingStrategy(Instrumentation instrumentation, Engine engine) {
        this.instrumentation = instrumentation;
        this.engine = engine;
    }

    /**
     * <p>
     * Obtains a {@link net.bytebuddy.dynamic.loading.ClassReloadingStrategy} from an installed Byte Buddy agent. This
     * agent must be installed either by adding the {@code byte-buddy-agent.jar} when starting up the JVM by
     * </p>
     * <p>
     * <code>
     * java -javaagent:byte-buddy-agent.jar -jar app.jar
     * </code>
     * </p>
     * or after the start up using the Attach API. A convenience installer for the OpenJDK is provided by the
     * {@code ByteBuddyAgent} within the {@code byte-buddy-agent} module.
     *
     * @return A class reloading strategy which uses the Byte Buddy agent's {@link java.lang.instrument.Instrumentation}.
     */
    public static ClassReloadingStrategy fromInstalledAgent() {
        try {
            return new ClassReloadingStrategy((Instrumentation) ClassLoader.getSystemClassLoader()
                    .loadClass(BYTE_BUDDY_AGENT_TYPE)
                    .getDeclaredMethod(GET_INSTRUMENTATION_METHOD)
                    .invoke(STATIC_METHOD));
        } catch (Exception e) {
            throw new IllegalStateException("The Byte Buddy agent is not installed or not accessible", e);
        }
    }

    @Override
    public Map<TypeDescription, Class<?>> load(ClassLoader classLoader, Map<TypeDescription, byte[]> types) {
        Map<TypeDescription, Class<?>> loadedClasses = new HashMap<TypeDescription, Class<?>>(types.size());
        ClassLoaderByteArrayInjector classLoaderByteArrayInjector = new ClassLoaderByteArrayInjector(classLoader);
        Map<Class<?>, ClassDefinition> classDefinitions = new ConcurrentHashMap<Class<?>, ClassDefinition>(types.size());
        for (Map.Entry<TypeDescription, byte[]> entry : types.entrySet()) {
            Class<?> type;
            try {
                type = classLoader.loadClass(entry.getKey().getName());
                classDefinitions.put(type, new ClassDefinition(type, entry.getValue()));
            } catch (ClassNotFoundException ignored) {
                type = classLoaderByteArrayInjector.inject(entry.getKey().getName(), entry.getValue());
            }
            loadedClasses.put(entry.getKey(), type);
        }
        try {
            engine.apply(instrumentation, classDefinitions);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Could not locate classes for redefinition", e);
        } catch (UnmodifiableClassException e) {
            throw new IllegalStateException("Cannot redefine specified class", e);
        }
        return loadedClasses;
    }

    /**
     * Resets all classes to their original definition.
     *
     * @param type The types to reset.
     * @return This class reloading strategy.
     */
    public ClassReloadingStrategy reset(Class<?>... type) {
        Map<Class<?>, ClassDefinition> classDefinitions = new ConcurrentHashMap<Class<?>, ClassDefinition>(type.length);
        try {
            for (Class<?> aType : type) {
                InputStream inputStream = aType.getClassLoader().getResourceAsStream(aType.getName().replace('.', '/') + CLASS_FILE_EXTENSION);
                try {
                    classDefinitions.put(aType, new ClassDefinition(aType, new StreamDrainer().drain(inputStream)));
                } finally {
                    inputStream.close();
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Exception while resetting types " + Arrays.toString(type), e);
        }
        try {
            engine.apply(instrumentation, classDefinitions);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Cannot locate types " + Arrays.toString(type), e);
        } catch (UnmodifiableClassException e) {
            throw new IllegalStateException("Cannot reset types " + Arrays.toString(type), e);
        }
        return this;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && engine == ((ClassReloadingStrategy) other).engine
                && instrumentation.equals(((ClassReloadingStrategy) other).instrumentation);
    }

    @Override
    public int hashCode() {
        return 31 * instrumentation.hashCode() + engine.hashCode();
    }

    @Override
    public String toString() {
        return "ClassReloadingStrategy{" +
                "instrumentation=" + instrumentation +
                ", engine=" + engine +
                '}';
    }

    /**
     * An engine which performs the actual redefinition of a {@link java.lang.Class}.
     */
    public static enum Engine {

        /**
         * Redefines a class using
         * {@link java.lang.instrument.Instrumentation#redefineClasses(java.lang.instrument.ClassDefinition...)}.
         */
        REDEFINITION(true) {
            @Override
            protected void apply(Instrumentation instrumentation,
                                 Map<Class<?>, ClassDefinition> classDefinitions) throws UnmodifiableClassException, ClassNotFoundException {
                instrumentation.redefineClasses(classDefinitions.values().toArray(new ClassDefinition[classDefinitions.size()]));
            }
        },

        /**
         * Redefines a class using
         * {@link java.lang.instrument.Instrumentation#retransformClasses(Class[])}. This requires synchronization on
         * the {@link net.bytebuddy.dynamic.loading.ClassReloadingStrategy#instrumentation} object.
         */
        RETRANSFORMATION(false) {
            @Override
            protected void apply(Instrumentation instrumentation,
                                 Map<Class<?>, ClassDefinition> classDefinitions) throws UnmodifiableClassException {
                ClassRedefinitionTransformer classRedefinitionTransformer = new ClassRedefinitionTransformer(classDefinitions);
                synchronized (instrumentation) {
                    instrumentation.addTransformer(classRedefinitionTransformer, REDEFINE_CLASSES);
                    try {
                        instrumentation.retransformClasses(classDefinitions.keySet().toArray(new Class<?>[classDefinitions.size()]));
                    } finally {
                        instrumentation.removeTransformer(classRedefinitionTransformer);
                    }
                }
                classRedefinitionTransformer.assertTransformation();
            }
        };

        /**
         * Instructs a {@link java.lang.instrument.ClassFileTransformer} to redefine classes.
         */
        private static final boolean REDEFINE_CLASSES = true;

        /**
         * {@code true} if the {@link net.bytebuddy.dynamic.loading.ClassReloadingStrategy.Engine#REDEFINITION} engine
         * is used.
         */
        private final boolean redefinition;

        /**
         * Creates a new engine.
         *
         * @param redefinition {@code true} if the {@link net.bytebuddy.dynamic.loading.ClassReloadingStrategy.Engine#REDEFINITION} engine
         *                     is used.
         */
        private Engine(boolean redefinition) {
            this.redefinition = redefinition;
        }

        /**
         * Applies this engine for the given arguments.
         *
         * @param instrumentation  The instrumentation to be used for applying the redefinition.
         * @param classDefinitions A mapping of the classes to be redefined to their redefinition.
         * @throws UnmodifiableClassException If a class is not modifiable.
         * @throws ClassNotFoundException     If a class was not found.
         */
        protected abstract void apply(Instrumentation instrumentation,
                                      Map<Class<?>, ClassDefinition> classDefinitions) throws UnmodifiableClassException, ClassNotFoundException;

        /**
         * Returns {@code true} if this engine represents {@link net.bytebuddy.dynamic.loading.ClassReloadingStrategy.Engine#REDEFINITION}.
         *
         * @return {@code true} if this engine represents {@link net.bytebuddy.dynamic.loading.ClassReloadingStrategy.Engine#REDEFINITION}.
         */
        public boolean isRedefinition() {
            return redefinition;
        }

        /**
         * A class file transformer that applies a given {@link java.lang.instrument.ClassDefinition}.
         */
        private static class ClassRedefinitionTransformer implements ClassFileTransformer {

            /**
             * A mapping of classes to be redefined to their redefined class definitions.
             */
            private final Map<Class<?>, ClassDefinition> redefinedClasses;

            /**
             * Creates a new class redefinition transformer.
             *
             * @param redefinedClasses A mapping of classes to be redefined to their redefined class definitions.
             */
            private ClassRedefinitionTransformer(Map<Class<?>, ClassDefinition> redefinedClasses) {
                this.redefinedClasses = redefinedClasses;
            }

            @Override
            public byte[] transform(ClassLoader loader,
                                    String className,
                                    Class<?> classBeingRedefined,
                                    ProtectionDomain protectionDomain,
                                    byte[] classfileBuffer) throws IllegalClassFormatException {
                ClassDefinition redefinedClass = redefinedClasses.remove(classBeingRedefined);
                if (redefinedClass == null) {
                    throw new IllegalArgumentException("Encountered class without redefinition information");
                }
                return redefinedClass.getDefinitionClassFile();
            }

            /**
             * Validates that all given classes were redefined.
             */
            public void assertTransformation() {
                if (redefinedClasses.size() > 0) {
                    throw new IllegalStateException("Could not transform: " + redefinedClasses.keySet());
                }
            }

            @Override
            public String toString() {
                return "ClassReloadingStrategy.Engine.ClassRedefinitionTransformer{redefinedClasses=" + redefinedClasses + '}';
            }
        }
    }
}
