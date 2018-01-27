package net.bytebuddy.dynamic;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import com.google.auto.value.AutoValue;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.TypeInitializer;
import net.bytebuddy.implementation.LoadedTypeInitializer;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * A type resolution strategy is responsible for loading a class and for initializing its {@link LoadedTypeInitializer}s.
 */
public interface TypeResolutionStrategy {

    /**
     * Resolves a type resolution strategy for actual application.
     *
     * @return A resolved version of this type resolution strategy.
     */
    Resolved resolve();

    /**
     * A resolved {@link TypeResolutionStrategy}.
     */
    interface Resolved {

        /**
         * Injects a type initializer into the supplied type initializer, if applicable. This way, a type resolution
         * strategy is capable of injecting code into the generated class's initializer to inline the initialization.
         *
         * @param typeInitializer The type initializer to potentially expend.
         * @return A type initializer to apply for performing the represented type resolution.
         */
        TypeInitializer injectedInto(TypeInitializer typeInitializer);

        /**
         * Loads and initializes a dynamic type.
         *
         * @param dynamicType          The dynamic type to initialize.
         * @param classLoader          The class loader to use.
         * @param classLoadingStrategy The class loading strategy to use.
         * @param <S>                  The least specific type of class loader this strategy can apply to.
         * @return A map of all type descriptions mapped to their representation as a loaded class.
         */
        <S extends ClassLoader> Map<TypeDescription, Class<?>> initialize(DynamicType dynamicType,
                                                                          S classLoader,
                                                                          ClassLoadingStrategy<? super S> classLoadingStrategy);
    }

    /**
     * A type resolution strategy that applies all {@link LoadedTypeInitializer} after class loading using reflection. This implies that the initializers
     * are executed <b>after</b> a type initializer is executed.
     */
    enum Passive implements TypeResolutionStrategy, Resolved {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public Resolved resolve() {
            return this;
        }

        @Override
        public TypeInitializer injectedInto(TypeInitializer typeInitializer) {
            return typeInitializer;
        }

        @Override
        public <S extends ClassLoader> Map<TypeDescription, Class<?>> initialize(DynamicType dynamicType,
                                                                                 S classLoader,
                                                                                 ClassLoadingStrategy<? super S> classLoadingStrategy) {
            Map<TypeDescription, Class<?>> types = classLoadingStrategy.load(classLoader, dynamicType.getAllTypes());
            for (Map.Entry<TypeDescription, LoadedTypeInitializer> entry : dynamicType.getLoadedTypeInitializers().entrySet()) {
                entry.getValue().onLoad(types.get(entry.getKey()));
            }
            return new HashMap<TypeDescription, Class<?>>(types);
        }
    }

    /**
     * A type resolution strategy that applies all {@link LoadedTypeInitializer} as a part of class loading using reflection. This implies that the initializers
     * are executed <b>before</b> (as a first action of) a type initializer is executed.
     */
    @AutoValue
    class Active implements TypeResolutionStrategy {

        /**
         * The nexus accessor to use.
         */
        private final NexusAccessor nexusAccessor;

        /**
         * Creates a new active type resolution strategy that uses a default nexus accessor.
         */
        public Active() {
            this(new NexusAccessor());
        }

        /**
         * Creates a new active type resolution strategy that uses the supplied nexus accessor.
         *
         * @param nexusAccessor The nexus accessor to use.
         */
        public Active(NexusAccessor nexusAccessor) {
            this.nexusAccessor = nexusAccessor;
        }

        @Override
        @SuppressFBWarnings(value = "DMI_RANDOM_USED_ONLY_ONCE", justification = "Avoid thread-contention")
        public TypeResolutionStrategy.Resolved resolve() {
            return new Resolved(nexusAccessor, new Random().nextInt());
        }

        /**
         * A resolved version of an active type resolution strategy.
         */
        @AutoValue
        protected static class Resolved implements TypeResolutionStrategy.Resolved {

            /**
             * The nexus accessor to use.
             */
            private final NexusAccessor nexusAccessor;

            /**
             * The id used for identifying the loaded type initializer that was added to the {@link Nexus}.
             */
            private final int identification;

            /**
             * Creates a new resolved active type resolution strategy.
             *
             * @param nexusAccessor  The nexus accessor to use.
             * @param identification The id used for identifying the loaded type initializer that was added to the {@link Nexus}.
             */
            protected Resolved(NexusAccessor nexusAccessor, int identification) {
                this.nexusAccessor = nexusAccessor;
                this.identification = identification;
            }

            @Override
            public TypeInitializer injectedInto(TypeInitializer typeInitializer) {
                return typeInitializer.expandWith(new NexusAccessor.InitializationAppender(identification));
            }

            @Override
            public <S extends ClassLoader> Map<TypeDescription, Class<?>> initialize(DynamicType dynamicType,
                                                                                     S classLoader,
                                                                                     ClassLoadingStrategy<? super S> classLoadingStrategy) {
                Map<TypeDescription, LoadedTypeInitializer> loadedTypeInitializers = new HashMap<TypeDescription, LoadedTypeInitializer>(dynamicType.getLoadedTypeInitializers());
                TypeDescription instrumentedType = dynamicType.getTypeDescription();
                Map<TypeDescription, Class<?>> types = classLoadingStrategy.load(classLoader, dynamicType.getAllTypes());
                nexusAccessor.register(instrumentedType.getName(),
                        types.get(instrumentedType).getClassLoader(),
                        identification,
                        loadedTypeInitializers.remove(instrumentedType));
                for (Map.Entry<TypeDescription, LoadedTypeInitializer> entry : loadedTypeInitializers.entrySet()) {
                    entry.getValue().onLoad(types.get(entry.getKey()));
                }
                return types;
            }
        }
    }

    /**
     * A type resolution strategy that does not apply any {@link LoadedTypeInitializer}s but only loads all types.
     */
    enum Lazy implements TypeResolutionStrategy, Resolved {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public Resolved resolve() {
            return this;
        }

        @Override
        public TypeInitializer injectedInto(TypeInitializer typeInitializer) {
            return typeInitializer;
        }

        @Override
        public <S extends ClassLoader> Map<TypeDescription, Class<?>> initialize(DynamicType dynamicType,
                                                                                 S classLoader,
                                                                                 ClassLoadingStrategy<? super S> classLoadingStrategy) {
            return classLoadingStrategy.load(classLoader, dynamicType.getAllTypes());
        }
    }

    /**
     * A type resolution strategy that does not allow for explicit loading of a class and that does not inject any code into the type initializer.
     */
    enum Disabled implements TypeResolutionStrategy, Resolved {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public Resolved resolve() {
            return this;
        }

        @Override
        public TypeInitializer injectedInto(TypeInitializer typeInitializer) {
            return typeInitializer;
        }

        @Override
        public <S extends ClassLoader> Map<TypeDescription, Class<?>> initialize(DynamicType dynamicType,
                                                                                 S classLoader,
                                                                                 ClassLoadingStrategy<? super S> classLoadingStrategy) {
            throw new IllegalStateException("Cannot initialize a dynamic type for a disabled type resolution strategy");
        }
    }
}
