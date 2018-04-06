package net.bytebuddy.pool;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.TypeVariableSource;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.annotation.AnnotationValue;
import net.bytebuddy.description.enumeration.EnumerationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.PackageDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.utility.OpenedClassReader;
import org.objectweb.asm.*;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.GenericSignatureFormatError;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * A type pool allows the retrieval of {@link TypeDescription} by its name.
 */
public interface TypePool {

    /**
     * Locates and describes the given type by its name.
     *
     * @param name The name of the type to describe. The name is to be written as when calling {@link Object#toString()}
     *             on a loaded {@link java.lang.Class}.
     * @return A resolution of the type to describe. If the type to be described was found, the returned
     * {@link net.bytebuddy.pool.TypePool.Resolution} represents this type. Otherwise, an illegal resolution is returned.
     */
    Resolution describe(String name);

    /**
     * Clears this type pool's cache.
     */
    void clear();

    /**
     * A resolution of a {@link net.bytebuddy.pool.TypePool} which was queried for a description.
     */
    interface Resolution {

        /**
         * Determines if this resolution represents a fully-resolved {@link TypeDescription}.
         *
         * @return {@code true} if the queried type could be resolved.
         */
        boolean isResolved();

        /**
         * Resolves this resolution to a {@link TypeDescription}. If this resolution is unresolved, this
         * method throws an exception either upon invoking this method or upon invoking at least one method
         * of the returned type description.
         *
         * @return The type description that is represented by this resolution.
         */
        TypeDescription resolve();

        /**
         * A simple resolution that represents a given {@link TypeDescription}.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class Simple implements Resolution {

            /**
             * The represented type description.
             */
            private final TypeDescription typeDescription;

            /**
             * Creates a new successful resolution of a given type description.
             *
             * @param typeDescription The represented type description.
             */
            public Simple(TypeDescription typeDescription) {
                this.typeDescription = typeDescription;
            }

            @Override
            public boolean isResolved() {
                return true;
            }

            @Override
            public TypeDescription resolve() {
                return typeDescription;
            }
        }

        /**
         * A canonical representation of a non-successful resolution of a {@link net.bytebuddy.pool.TypePool}.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class Illegal implements Resolution {

            /**
             * The name of the unresolved type.
             */
            private final String name;

            /**
             * Creates a new illegal resolution.
             *
             * @param name The name of the unresolved type.
             */
            public Illegal(String name) {
                this.name = name;
            }

            @Override
            public boolean isResolved() {
                return false;
            }

            @Override
            public TypeDescription resolve() {
                throw new IllegalStateException("Cannot resolve type description for " + name);
            }
        }
    }

    /**
     * A cache provider for a {@link net.bytebuddy.pool.TypePool}.
     */
    interface CacheProvider {

        /**
         * The value that is returned on a cache-miss.
         */
        Resolution UNRESOLVED = null;

        /**
         * Attempts to find a resolution in this cache.
         *
         * @param name The name of the type to describe.
         * @return A resolution of the type or {@code null} if no such resolution can be found in the cache..
         */
        Resolution find(String name);

        /**
         * Registers a resolution in this cache. If a resolution to the given name already exists in the
         * cache, it should be discarded.
         *
         * @param name       The name of the type that is to be registered.
         * @param resolution The resolution to register.
         * @return The oldest version of a resolution that is currently registered in the cache which might
         * be the given resolution or another resolution that was previously registered.
         */
        Resolution register(String name, Resolution resolution);

        /**
         * Clears this cache.
         */
        void clear();

        /**
         * A non-operational cache that does not store any type descriptions.
         */
        enum NoOp implements CacheProvider {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public Resolution find(String name) {
                return UNRESOLVED;
            }

            @Override
            public Resolution register(String name, Resolution resolution) {
                return resolution;
            }

            @Override
            public void clear() {
                /* do nothing */
            }
        }

        /**
         * A simple, thread-safe type cache based on a {@link java.util.concurrent.ConcurrentHashMap}.
         */
        class Simple implements CacheProvider {

            /**
             * A map containing all cached resolutions by their names.
             */
            private final ConcurrentMap<String, Resolution> cache;

            /**
             * Creates a new simple cache.
             */
            public Simple() {
                cache = new ConcurrentHashMap<String, Resolution>();
            }

            /**
             * Returns a simple cache provider that is prepopulated with the {@link Object} type.
             *
             * @return A simple cache provider that is prepopulated with the {@link Object} type.
             */
            public static CacheProvider withObjectType() {
                CacheProvider cacheProvider = new Simple();
                cacheProvider.register(Object.class.getName(), new Resolution.Simple(TypeDescription.OBJECT));
                return cacheProvider;
            }

            @Override
            public Resolution find(String name) {
                return cache.get(name);
            }

            @Override
            public Resolution register(String name, Resolution resolution) {
                Resolution cached = cache.putIfAbsent(name, resolution);
                return cached == null
                        ? resolution
                        : cached;
            }

            @Override
            public void clear() {
                cache.clear();
            }
        }
    }

    /**
     * An empty type pool that cannot describe any type.
     */
    enum Empty implements TypePool {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public Resolution describe(String name) {
            return new Resolution.Illegal(name);
        }

        @Override
        public void clear() {
            /* do nothing */
        }
    }

    /**
     * A base implementation of a {@link net.bytebuddy.pool.TypePool} that is managing a cache provider and
     * that handles the description of array and primitive types.
     */
    @HashCodeAndEqualsPlugin.Enhance
    abstract class AbstractBase implements TypePool {

        /**
         * A map of primitive types by their name.
         */
        protected static final Map<String, TypeDescription> PRIMITIVE_TYPES;

        /**
         * A map of primitive types by their descriptor.
         */
        protected static final Map<String, String> PRIMITIVE_DESCRIPTORS;

        /**
         * The array symbol as used by Java descriptors.
         */
        private static final String ARRAY_SYMBOL = "[";

        /*
         * Initializes the maps of primitive type names and descriptors.
         */
        static {
            Map<String, TypeDescription> primitiveTypes = new HashMap<String, TypeDescription>();
            Map<String, String> primitiveDescriptors = new HashMap<String, String>();
            for (Class<?> primitiveType : new Class<?>[]{boolean.class,
                    byte.class,
                    short.class,
                    char.class,
                    int.class,
                    long.class,
                    float.class,
                    double.class,
                    void.class}) {
                primitiveTypes.put(primitiveType.getName(), new TypeDescription.ForLoadedType(primitiveType));
                primitiveDescriptors.put(Type.getDescriptor(primitiveType), primitiveType.getName());
            }
            PRIMITIVE_TYPES = Collections.unmodifiableMap(primitiveTypes);
            PRIMITIVE_DESCRIPTORS = Collections.unmodifiableMap(primitiveDescriptors);
        }

        /**
         * The cache provider of this instance.
         */
        protected final CacheProvider cacheProvider;

        /**
         * Creates a new instance.
         *
         * @param cacheProvider The cache provider to be used.
         */
        protected AbstractBase(CacheProvider cacheProvider) {
            this.cacheProvider = cacheProvider;
        }

        @Override
        public Resolution describe(String name) {
            if (name.contains("/")) {
                throw new IllegalArgumentException(name + " contains the illegal character '/'");
            }
            int arity = 0;
            while (name.startsWith(ARRAY_SYMBOL)) {
                arity++;
                name = name.substring(1);
            }
            if (arity > 0) {
                String primitiveName = PRIMITIVE_DESCRIPTORS.get(name);
                name = primitiveName == null
                        ? name.substring(1, name.length() - 1)
                        : primitiveName;
            }
            TypeDescription typeDescription = PRIMITIVE_TYPES.get(name);
            Resolution resolution = typeDescription == null
                    ? cacheProvider.find(name)
                    : new Resolution.Simple(typeDescription);
            if (resolution == null) {
                resolution = doCache(name, doDescribe(name));
            }
            return ArrayTypeResolution.of(resolution, arity);
        }

        /**
         * Writes the resolution to the cache. This method should be overridden if the directly
         * resolved instance should not be added to the cache.
         *
         * @param name       The name of the type.
         * @param resolution The resolution for this type.
         * @return The actual resolution for the type of this name that is stored in the cache.
         */
        protected Resolution doCache(String name, Resolution resolution) {
            return cacheProvider.register(name, resolution);
        }

        @Override
        public void clear() {
            cacheProvider.clear();
        }

        /**
         * Determines a resolution to a non-primitive, non-array type.
         *
         * @param name The name of the type to describe.
         * @return A resolution to the type to describe.
         */
        protected abstract Resolution doDescribe(String name);

        /**
         * Implements a hierarchical view of type pools, similarly to class loader hierarchies. For every lookup, the parent type pool
         * is asked first if it can resolve a type. Only if the parent (and potentially its parents) are unable to resolve a type,
         * this instance is queried for a type description.
         */
        @HashCodeAndEqualsPlugin.Enhance
        public abstract static class Hierarchical extends AbstractBase {

            /**
             * The parent type pool.
             */
            private final TypePool parent;

            /**
             * Creates a hierarchical type pool.
             *
             * @param cacheProvider The cache provider to be used.
             * @param parent        The parent type pool to be used.
             */
            protected Hierarchical(CacheProvider cacheProvider, TypePool parent) {
                super(cacheProvider);
                this.parent = parent;
            }

            @Override
            public Resolution describe(String name) {
                Resolution resolution = parent.describe(name);
                return resolution.isResolved()
                        ? resolution
                        : super.describe(name);
            }

            @Override
            public void clear() {
                try {
                    parent.clear();
                } finally {
                    super.clear();
                }
            }
        }

        /**
         * A resolution for a type that, if resolved, represents an array type.
         */
        @HashCodeAndEqualsPlugin.Enhance
        protected static class ArrayTypeResolution implements Resolution {

            /**
             * The underlying resolution that is represented by this instance.
             */
            private final Resolution resolution;

            /**
             * The arity of the represented array.
             */
            private final int arity;

            /**
             * Creates a wrapper for another resolution that, if resolved, represents an array type.
             *
             * @param resolution The underlying resolution that is represented by this instance.
             * @param arity      The arity of the represented array.
             */
            protected ArrayTypeResolution(Resolution resolution, int arity) {
                this.resolution = resolution;
                this.arity = arity;
            }

            /**
             * Creates a wrapper for another resolution that, if resolved, represents an array type. The wrapper
             * is only created if the arity is not zero. If the arity is zero, the given resolution is simply
             * returned instead.
             *
             * @param resolution The underlying resolution that is represented by this instance.
             * @param arity      The arity of the represented array.
             * @return A wrapper for another resolution that, if resolved, represents an array type or the
             * given resolution if the given arity is zero.
             */
            protected static Resolution of(Resolution resolution, int arity) {
                return arity == 0
                        ? resolution
                        : new ArrayTypeResolution(resolution, arity);
            }

            @Override
            public boolean isResolved() {
                return resolution.isResolved();
            }

            @Override
            public TypeDescription resolve() {
                return TypeDescription.ArrayProjection.of(resolution.resolve(), arity);
            }
        }

        /**
         * Represents a nested annotation value.
         */
        protected static class RawAnnotationValue extends AnnotationValue.AbstractBase<AnnotationDescription, Annotation> {

            /**
             * The type pool to use for looking up types.
             */
            private final TypePool typePool;

            /**
             * The annotation token that represents the nested invocation.
             */
            private final Default.LazyTypeDescription.AnnotationToken annotationToken;

            /**
             * Creates a new annotation value for a nested annotation.
             *
             * @param typePool        The type pool to use for looking up types.
             * @param annotationToken The token that represents the annotation.
             */
            public RawAnnotationValue(TypePool typePool, Default.LazyTypeDescription.AnnotationToken annotationToken) {
                this.typePool = typePool;
                this.annotationToken = annotationToken;
            }

            @Override
            public AnnotationDescription resolve() {
                return annotationToken.toAnnotationDescription(typePool).resolve();
            }

            @Override
            @SuppressWarnings("unchecked")
            public Loaded<Annotation> load(ClassLoader classLoader) throws ClassNotFoundException {
                Class<?> type = Class.forName(annotationToken.getBinaryName(), false, classLoader);
                if (type.isAnnotation()) {
                    return new ForAnnotationDescription.Loaded<Annotation>(AnnotationDescription.AnnotationInvocationHandler.of(classLoader,
                            (Class<? extends Annotation>) type,
                            annotationToken.getValues()));
                } else {
                    return new ForAnnotationDescription.IncompatibleRuntimeType(type);
                }
            }

            @Override
            public boolean equals(Object other) {
                return this == other || other instanceof AnnotationValue<?, ?> && resolve().equals(((AnnotationValue<?, ?>) other).resolve());
            }

            @Override
            public int hashCode() {
                return resolve().hashCode();
            }

            @Override
            public String toString() {
                return resolve().toString();
            }
        }

        /**
         * Represents an enumeration value of an annotation.
         */
        protected static class RawEnumerationValue extends AnnotationValue.AbstractBase<EnumerationDescription, Enum<?>> {

            /**
             * The type pool to use for looking up types.
             */
            private final TypePool typePool;

            /**
             * The descriptor of the enumeration type.
             */
            private final String descriptor;

            /**
             * The name of the enumeration.
             */
            private final String value;

            /**
             * Creates a new enumeration value representation.
             *
             * @param typePool   The type pool to use for looking up types.
             * @param descriptor The descriptor of the enumeration type.
             * @param value      The name of the enumeration.
             */
            public RawEnumerationValue(TypePool typePool, String descriptor, String value) {
                this.typePool = typePool;
                this.descriptor = descriptor;
                this.value = value;
            }

            @Override
            public EnumerationDescription resolve() {
                return new LazyEnumerationDescription();
            }

            @Override
            @SuppressWarnings("unchecked")
            public Loaded<Enum<?>> load(ClassLoader classLoader) throws ClassNotFoundException {
                Class<?> type = Class.forName(descriptor.substring(1, descriptor.length() - 1).replace('/', '.'), false, classLoader);
                try {
                    return type.isEnum()
                            ? new ForEnumerationDescription.Loaded(Enum.valueOf((Class) type, value))
                            : new ForEnumerationDescription.IncompatibleRuntimeType(type);
                } catch (IllegalArgumentException ignored) {
                    return new ForEnumerationDescription.UnknownRuntimeEnumeration((Class) type, value);
                }
            }

            @Override
            public boolean equals(Object other) {
                return this == other || other instanceof AnnotationValue<?, ?> && resolve().equals(((AnnotationValue<?, ?>) other).resolve());
            }

            @Override
            public int hashCode() {
                return resolve().hashCode();
            }

            @Override
            public String toString() {
                return resolve().toString();
            }

            /**
             * An enumeration description where any type references are only resolved on demand.
             */
            protected class LazyEnumerationDescription extends EnumerationDescription.AbstractBase {

                @Override
                public String getValue() {
                    return value;
                }

                @Override
                public TypeDescription getEnumerationType() {
                    return typePool.describe(descriptor.substring(1, descriptor.length() - 1).replace('/', '.')).resolve();
                }

                @Override
                public <T extends Enum<T>> T load(Class<T> type) {
                    return Enum.valueOf(type, value);
                }
            }
        }

        /**
         * Represents a type value of an annotation.
         */
        protected static class RawTypeValue extends AnnotationValue.AbstractBase<TypeDescription, Class<?>> {

            /**
             * A convenience reference indicating that a loaded type should not be initialized.
             */
            private static final boolean NO_INITIALIZATION = false;

            /**
             * The type pool to use for looking up types.
             */
            private final TypePool typePool;

            /**
             * The binary name of the type.
             */
            private final String name;

            /**
             * Represents a type value of an annotation.
             *
             * @param typePool The type pool to use for looking up types.
             * @param type     A type representation of the type that is referenced by the annotation..
             */
            protected RawTypeValue(TypePool typePool, Type type) {
                this.typePool = typePool;
                name = type.getSort() == Type.ARRAY
                        ? type.getInternalName().replace('/', '.')
                        : type.getClassName();
            }

            @Override
            public TypeDescription resolve() {
                return typePool.describe(name).resolve();
            }

            @Override
            public AnnotationValue.Loaded<Class<?>> load(ClassLoader classLoader) throws ClassNotFoundException {
                return new Loaded(Class.forName(name, NO_INITIALIZATION, classLoader));
            }

            @Override
            public boolean equals(Object other) {
                return this == other || other instanceof AnnotationValue<?, ?> && resolve().equals(((AnnotationValue<?, ?>) other).resolve());
            }

            @Override
            public int hashCode() {
                return resolve().hashCode();
            }

            @Override
            public String toString() {
                return RenderingDispatcher.CURRENT.toSourceString(resolve());
            }

            /**
             * Represents a loaded annotation property that represents a type.
             */
            protected static class Loaded extends AnnotationValue.Loaded.AbstractBase<Class<?>> {

                /**
                 * The type that is represented by an annotation property.
                 */
                private final Class<?> type;

                /**
                 * Creates a new representation for an annotation property referencing a type.
                 *
                 * @param type The type that is represented by an annotation property.
                 */
                public Loaded(Class<?> type) {
                    this.type = type;
                }

                @Override
                public State getState() {
                    return State.RESOLVED;
                }

                @Override
                public Class<?> resolve() {
                    return type;
                }

                @Override
                public boolean represents(Object value) {
                    return type.equals(value);
                }

                @Override
                public boolean equals(Object other) {
                    if (this == other) {
                        return true;
                    } else if (!(other instanceof AnnotationValue.Loaded<?>)) {
                        return false;
                    }
                    AnnotationValue.Loaded<?> annotationValue = (AnnotationValue.Loaded<?>) other;
                    return annotationValue.getState().isResolved() && type.equals(annotationValue.resolve());
                }

                @Override
                public int hashCode() {
                    return type.hashCode();
                }

                @Override
                public String toString() {
                    return RenderingDispatcher.CURRENT.toSourceString(new TypeDescription.ForLoadedType(type));
                }
            }
        }

        /**
         * Represents an array that is referenced by an annotation which does not contain primitive values or {@link String}s.
         */
        protected static class RawDescriptionArray extends AnnotationValue.AbstractBase<Object[], Object[]> {

            /**
             * The type pool to use for looking up types.
             */
            private final TypePool typePool;

            /**
             * A reference to the component type.
             */
            private final ComponentTypeReference componentTypeReference;

            /**
             * A list of all values of this array value in their order.
             */
            private List<AnnotationValue<?, ?>> values;

            /**
             * Creates a new array value representation of a complex array.
             *
             * @param typePool               The type pool to use for looking up types.
             * @param componentTypeReference A lazy reference to the component type of this array.
             * @param values                 A list of all values of this annotation.
             */
            public RawDescriptionArray(TypePool typePool,
                                       ComponentTypeReference componentTypeReference,
                                       List<AnnotationValue<?, ?>> values) {
                this.typePool = typePool;
                this.values = values;
                this.componentTypeReference = componentTypeReference;
            }

            @Override
            public Object[] resolve() {
                TypeDescription componentTypeDescription = typePool.describe(componentTypeReference.lookup()).resolve();
                Class<?> componentType;
                if (componentTypeDescription.represents(Class.class)) {
                    componentType = TypeDescription.class;
                } else if (componentTypeDescription.isAssignableTo(Enum.class)) { // Enums can implement annotation interfaces, check this first.
                    componentType = EnumerationDescription.class;
                } else if (componentTypeDescription.isAssignableTo(Annotation.class)) {
                    componentType = AnnotationDescription.class;
                } else if (componentTypeDescription.represents(String.class)) {
                    componentType = String.class;
                } else {
                    throw new IllegalStateException("Unexpected complex array component type " + componentTypeDescription);
                }
                Object[] array = (Object[]) Array.newInstance(componentType, values.size());
                int index = 0;
                for (AnnotationValue<?, ?> annotationValue : values) {
                    Array.set(array, index++, annotationValue.resolve());
                }
                return array;
            }

            @Override
            public AnnotationValue.Loaded<Object[]> load(ClassLoader classLoader) throws ClassNotFoundException {
                List<AnnotationValue.Loaded<?>> loadedValues = new ArrayList<AnnotationValue.Loaded<?>>(values.size());
                for (AnnotationValue<?, ?> value : values) {
                    loadedValues.add(value.load(classLoader));
                }
                return new Loaded(Class.forName(componentTypeReference.lookup(), false, classLoader), loadedValues);
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) {
                    return true;
                } else if (!(other instanceof AnnotationValue<?, ?>)) {
                    return false;
                }
                Object value = ((AnnotationValue<?, ?>) other).resolve();
                return value instanceof Object[] && Arrays.equals(resolve(), (Object[]) value);
            }

            @Override
            public int hashCode() {
                return Arrays.hashCode(resolve());
            }

            @Override
            public String toString() {
                return RenderingDispatcher.CURRENT.toSourceString(values);
            }

            /**
             * A lazy representation of the component type of an array.
             */
            public interface ComponentTypeReference {

                /**
                 * Lazily returns the binary name of the array component type of an annotation value.
                 *
                 * @return The binary name of the component type.
                 */
                String lookup();
            }

            /**
             * Represents a loaded annotation property representing a complex array.
             */
            protected static class Loaded extends AnnotationValue.Loaded.AbstractBase<Object[]> {

                /**
                 * The array's loaded component type.
                 */
                private final Class<?> componentType;

                /**
                 * A list of loaded values of the represented complex array.
                 */
                private final List<AnnotationValue.Loaded<?>> values;

                /**
                 * Creates a new representation of an annotation property representing an array of
                 * non-trivial values.
                 *
                 * @param componentType The array's loaded component type.
                 * @param values        A list of loaded values of the represented complex array.
                 */
                public Loaded(Class<?> componentType, List<AnnotationValue.Loaded<?>> values) {
                    this.componentType = componentType;
                    this.values = values;
                }

                @Override
                public State getState() {
                    for (AnnotationValue.Loaded<?> value : values) {
                        if (!value.getState().isResolved()) {
                            return State.UNRESOLVED;
                        }
                    }
                    return State.RESOLVED;
                }

                @Override
                public Object[] resolve() {
                    Object[] array = (Object[]) Array.newInstance(componentType, values.size());
                    int index = 0;
                    for (AnnotationValue.Loaded<?> annotationValue : values) {
                        Array.set(array, index++, annotationValue.resolve());
                    }
                    return array;
                }

                @Override
                public boolean represents(Object value) {
                    if (!(value instanceof Object[])) return false;
                    if (value.getClass().getComponentType() != componentType) return false;
                    Object[] array = (Object[]) value;
                    if (values.size() != array.length) return false;
                    Iterator<AnnotationValue.Loaded<?>> iterator = values.iterator();
                    for (Object aValue : array) {
                        AnnotationValue.Loaded<?> self = iterator.next();
                        if (!self.getState().isResolved() || !self.represents(aValue)) {
                            return false;
                        }
                    }
                    return true;
                }

                @Override
                public boolean equals(Object other) {
                    if (this == other) {
                        return true;
                    } else if (!(other instanceof AnnotationValue.Loaded<?>)) {
                        return false;
                    }
                    AnnotationValue.Loaded<?> annotationValue = (AnnotationValue.Loaded<?>) other;
                    if (!annotationValue.getState().isResolved()) {
                        return false;
                    }
                    Object value = annotationValue.resolve();
                    if (!(value instanceof Object[])) {
                        return false;
                    }
                    Object[] arrayValue = (Object[]) value;
                    if (values.size() != arrayValue.length) {
                        return false;
                    }
                    Iterator<AnnotationValue.Loaded<?>> iterator = values.iterator();
                    for (Object aValue : arrayValue) {
                        AnnotationValue.Loaded<?> self = iterator.next();
                        if (!self.resolve().equals(aValue)) {
                            return false;
                        }
                    }
                    return true;
                }

                @Override
                public int hashCode() {
                    int result = 1;
                    for (AnnotationValue.Loaded<?> value : values) {
                        result = 31 * result + value.hashCode();
                    }
                    return result;
                }

                @Override
                public String toString() {
                    return RenderingDispatcher.CURRENT.toSourceString(values);
                }
            }
        }
    }

    /**
     * <p>
     * A default implementation of a {@link net.bytebuddy.pool.TypePool} that models binary data in the Java byte code format
     * into a {@link TypeDescription}. The data lookup is delegated to a {@link net.bytebuddy.dynamic.ClassFileLocator}.
     * </p>
     * <p>
     * {@link Resolution}s that are produced by this type pool are either fully resolved or not resolved at all.
     * </p>
     */
    @HashCodeAndEqualsPlugin.Enhance
    class Default extends AbstractBase.Hierarchical {

        /**
         * Indicates that a visited method should be ignored.
         */
        private static final MethodVisitor IGNORE_METHOD = null;

        /**
         * The locator to query for finding binary data of a type.
         */
        protected final ClassFileLocator classFileLocator;

        /**
         * The reader mode to apply by this default type pool.
         */
        protected final ReaderMode readerMode;

        /**
         * Creates a new default type pool without a parent pool.
         *
         * @param cacheProvider    The cache provider to be used.
         * @param classFileLocator The class file locator to be used.
         * @param readerMode       The reader mode to apply by this default type pool.
         */
        public Default(CacheProvider cacheProvider, ClassFileLocator classFileLocator, ReaderMode readerMode) {
            this(cacheProvider, classFileLocator, readerMode, Empty.INSTANCE);
        }

        /**
         * Creates a new default type pool.
         *
         * @param cacheProvider    The cache provider to be used.
         * @param classFileLocator The class file locator to be used.
         * @param readerMode       The reader mode to apply by this default type pool.
         * @param parentPool       The parent type pool.
         */
        public Default(CacheProvider cacheProvider, ClassFileLocator classFileLocator, ReaderMode readerMode, TypePool parentPool) {
            super(cacheProvider, parentPool);
            this.classFileLocator = classFileLocator;
            this.readerMode = readerMode;
        }

        /**
         * Creates a default {@link net.bytebuddy.pool.TypePool} that looks up data by querying the system class
         * loader. The returned instance is configured to use a fast reading mode and a simple cache.
         *
         * @return A type pool that reads its data from the system class path.
         */
        public static TypePool ofClassPath() {
            return of(ClassFileLocator.ForClassLoader.ofClassPath());
        }

        /**
         * Returns a type pool for the provided class loader.
         *
         * @param classLoader The class loader for which this class pool is representing types.
         * @return An appropriate type pool.
         */
        public static TypePool of(ClassLoader classLoader) {
            return of(ClassFileLocator.ForClassLoader.of(classLoader));
        }

        /**
         * Creates a default {@link net.bytebuddy.pool.TypePool} that looks up data by querying the supplied class
         * file locator. The returned instance is configured to use a fast reading mode and a simple cache.
         *
         * @param classFileLocator The class file locator to use.
         * @return A type pool that reads its data from the system class path.
         */
        public static TypePool of(ClassFileLocator classFileLocator) {
            return new Default(new CacheProvider.Simple(), classFileLocator, ReaderMode.FAST);
        }

        @Override
        protected Resolution doDescribe(String name) {
            try {
                ClassFileLocator.Resolution resolution = classFileLocator.locate(name);
                return resolution.isResolved()
                        ? new Resolution.Simple(parse(resolution.resolve()))
                        : new Resolution.Illegal(name);
            } catch (IOException exception) {
                throw new IllegalStateException("Error while reading class file", exception);
            }
        }

        /**
         * Parses a binary representation and transforms it into a type description.
         *
         * @param binaryRepresentation The binary data to be parsed.
         * @return A type description of the binary data.
         */
        private TypeDescription parse(byte[] binaryRepresentation) {
            ClassReader classReader = OpenedClassReader.of(binaryRepresentation);
            TypeExtractor typeExtractor = new TypeExtractor();
            classReader.accept(typeExtractor, readerMode.getFlags());
            return typeExtractor.toTypeDescription();
        }

        /**
         * Determines the granularity of the class file parsing that is conducted by a {@link net.bytebuddy.pool.TypePool.Default}.
         */
        public enum ReaderMode {

            /**
             * The extended reader mode parses the code segment of each method in order to detect parameter names
             * that are only stored in a method's debugging information but are not explicitly included.
             */
            EXTENDED(ClassReader.SKIP_FRAMES),

            /**
             * The fast reader mode skips the code segment of each method and cannot detect parameter names that are
             * only contained within the debugging information. This mode still detects explicitly included method
             * parameter names.
             */
            FAST(ClassReader.SKIP_CODE);

            /**
             * The flags to provide to a {@link ClassReader} for parsing a file.
             */
            private final int flags;

            /**
             * Creates a new reader mode constant.
             *
             * @param flags The flags to provide to a {@link ClassReader} for parsing a file.
             */
            ReaderMode(int flags) {
                this.flags = flags;
            }

            /**
             * Returns the flags to provide to a {@link ClassReader} for parsing a file.
             *
             * @return The flags to provide to a {@link ClassReader} for parsing a file.
             */
            protected int getFlags() {
                return flags;
            }

            /**
             * Determines if this reader mode represents extended reading.
             *
             * @return {@code true} if this reader mode represents extended reading.
             */
            public boolean isExtended() {
                return this == EXTENDED;
            }
        }

        /**
         * <p>
         * A variant of {@link TypePool.Default} that resolves type descriptions lazily. A lazy resolution respects this type
         * pool's {@link CacheProvider} but requeries this cache pool for every access of a property of a {@link TypeDescription}.
         * </p>
         * <p>
         * {@link Resolution}s of this type pool are only fully resolved if a property that is not the type's name is required.
         * </p>
         */
        public static class WithLazyResolution extends Default {

            /**
             * Creates a new default type pool with lazy resolution and without a parent pool.
             *
             * @param cacheProvider    The cache provider to be used.
             * @param classFileLocator The class file locator to be used.
             * @param readerMode       The reader mode to apply by this default type pool.
             */
            public WithLazyResolution(CacheProvider cacheProvider, ClassFileLocator classFileLocator, ReaderMode readerMode) {
                this(cacheProvider, classFileLocator, readerMode, Empty.INSTANCE);
            }

            /**
             * Creates a new default type pool with lazy resolution.
             *
             * @param cacheProvider    The cache provider to be used.
             * @param classFileLocator The class file locator to be used.
             * @param readerMode       The reader mode to apply by this default type pool.
             * @param parentPool       The parent type pool.
             */
            public WithLazyResolution(CacheProvider cacheProvider, ClassFileLocator classFileLocator, ReaderMode readerMode, TypePool parentPool) {
                super(cacheProvider, classFileLocator, readerMode, parentPool);
            }

            /**
             * Creates a default {@link net.bytebuddy.pool.TypePool} with lazy resolution that looks up data by querying the system class
             * loader. The returned instance is configured to use a fast reading mode and a simple cache.
             *
             * @return A type pool that reads its data from the system class path.
             */
            public static TypePool ofClassPath() {
                return of(ClassFileLocator.ForClassLoader.ofClassPath());
            }

            /**
             * Returns a default {@link TypePool} with lazy resolution for the provided class loader.
             *
             * @param classLoader The class loader for which this class pool is representing types.
             * @return An appropriate type pool.
             */
            public static TypePool of(ClassLoader classLoader) {
                return of(ClassFileLocator.ForClassLoader.of(classLoader));
            }

            /**
             * Creates a default {@link net.bytebuddy.pool.TypePool} with lazy resolution that looks up data by querying the supplied class
             * file locator. The returned instance is configured to use a fast reading mode and a simple cache.
             *
             * @param classFileLocator The class file locator to use.
             * @return A type pool that reads its data from the system class path.
             */
            public static TypePool of(ClassFileLocator classFileLocator) {
                return new WithLazyResolution(new CacheProvider.Simple(), classFileLocator, ReaderMode.FAST);
            }

            @Override
            protected Resolution doDescribe(String name) {
                return new LazyResolution(name);
            }

            @Override
            protected Resolution doCache(String name, Resolution resolution) {
                return resolution;
            }

            /**
             * Non-lazily resolves a type name.
             *
             * @param name The name of the type to resolve.
             * @return The resolution for the type of this name.
             */
            protected Resolution doResolve(String name) {
                Resolution resolution = cacheProvider.find(name);
                if (resolution == null) {
                    resolution = cacheProvider.register(name, WithLazyResolution.super.doDescribe(name));
                }
                return resolution;
            }

            /**
             * A lazy resolution of a type that the enclosing type pool attempts to resolve.
             */
            @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
            protected class LazyResolution implements Resolution {

                /**
                 * The type's name.
                 */
                private final String name;

                /**
                 * Creates a new lazy resolution.
                 *
                 * @param name The type's name.
                 */
                protected LazyResolution(String name) {
                    this.name = name;
                }

                @Override
                public boolean isResolved() {
                    return doResolve(name).isResolved();
                }

                @Override
                public TypeDescription resolve() {
                    return new LazyTypeDescription(name);
                }
            }

            /**
             * A lazy type description that resolves any property that is not the name only when requested.
             */
            protected class LazyTypeDescription extends TypeDescription.AbstractBase.OfSimpleType.WithDelegation {

                /**
                 * The type's name.
                 */
                private final String name;

                /**
                 * Creates a new lazy type description.
                 *
                 * @param name The type's name.
                 */
                protected LazyTypeDescription(String name) {
                    this.name = name;
                }

                @Override
                public String getName() {
                    return name;
                }

                @Override
                protected TypeDescription delegate() {
                    return doResolve(name).resolve();
                }
            }
        }

        /**
         * An annotation registrant implements a visitor pattern for reading an unknown amount of values of annotations.
         */
        protected interface AnnotationRegistrant {

            /**
             * Registers an annotation value.
             *
             * @param name            The name of the annotation value.
             * @param annotationValue The value of the annotation.
             */
            void register(String name, AnnotationValue<?, ?> annotationValue);

            /**
             * Called once all annotation values are visited.
             */
            void onComplete();

            /**
             * An abstract base implementation of an annotation registrant.
             */
            abstract class AbstractBase implements AnnotationRegistrant {

                /**
                 * The annotation descriptor.
                 */
                private final String descriptor;

                /**
                 * The values that were collected so far.
                 */
                private final Map<String, AnnotationValue<?, ?>> values;

                /**
                 * Creates a new annotation registrant.
                 *
                 * @param descriptor The annotation descriptor.
                 */
                protected AbstractBase(String descriptor) {
                    this.descriptor = descriptor;
                    values = new HashMap<String, AnnotationValue<?, ?>>();
                }

                @Override
                public void register(String name, AnnotationValue<?, ?> annotationValue) {
                    values.put(name, annotationValue);
                }

                @Override
                public void onComplete() {
                    getTokens().add(new LazyTypeDescription.AnnotationToken(descriptor, values));
                }

                /**
                 * Returns the token list for this collector.
                 *
                 * @return The token list for this collector.
                 */
                protected abstract List<LazyTypeDescription.AnnotationToken> getTokens();

                /**
                 * A base implementation for a collector for a type variable.
                 */
                protected abstract static class ForTypeVariable extends AbstractBase {

                    /**
                     * The type variable's type path.
                     */
                    private final String typePath;

                    /**
                     * Creates a new annotation collector.
                     *
                     * @param descriptor The annotation descriptor.
                     * @param typePath   The type variable's type path.
                     */
                    protected ForTypeVariable(String descriptor, TypePath typePath) {
                        super(descriptor);
                        this.typePath = typePath == null
                                ? LazyTypeDescription.GenericTypeToken.EMPTY_TYPE_PATH
                                : typePath.toString();
                    }

                    @Override
                    protected List<LazyTypeDescription.AnnotationToken> getTokens() {
                        Map<String, List<LazyTypeDescription.AnnotationToken>> pathMap = getPathMap();
                        List<LazyTypeDescription.AnnotationToken> tokens = pathMap.get(typePath);
                        if (tokens == null) {
                            tokens = new ArrayList<LazyTypeDescription.AnnotationToken>();
                            pathMap.put(typePath, tokens);
                        }
                        return tokens;
                    }

                    /**
                     * Returns this collector's path map.
                     *
                     * @return This collector's path map.
                     */
                    protected abstract Map<String, List<LazyTypeDescription.AnnotationToken>> getPathMap();

                    /**
                     * A base implementation for a collector for a type variable with an index.
                     */
                    protected abstract static class WithIndex extends AbstractBase.ForTypeVariable {

                        /**
                         * The type variable's index.
                         */
                        private final int index;

                        /**
                         * Creates a new annotation collector.
                         *
                         * @param descriptor The annotation descriptor.
                         * @param typePath   The type variable's type path.
                         * @param index      The type variable's index.
                         */
                        protected WithIndex(String descriptor, TypePath typePath, int index) {
                            super(descriptor, typePath);
                            this.index = index;
                        }

                        @Override
                        protected Map<String, List<LazyTypeDescription.AnnotationToken>> getPathMap() {
                            Map<Integer, Map<String, List<LazyTypeDescription.AnnotationToken>>> indexedPathMap = getIndexedPathMap();
                            Map<String, List<LazyTypeDescription.AnnotationToken>> pathMap = indexedPathMap.get(index);
                            if (pathMap == null) {
                                pathMap = new HashMap<String, List<LazyTypeDescription.AnnotationToken>>();
                                indexedPathMap.put(index, pathMap);
                            }
                            return pathMap;
                        }

                        /**
                         * Returns this collector's indexed path map.
                         *
                         * @return This collector's indexed path map.
                         */
                        protected abstract Map<Integer, Map<String, List<LazyTypeDescription.AnnotationToken>>> getIndexedPathMap();

                        /**
                         * A base implementation for a collector for a type variable with two indices.
                         */
                        protected abstract static class DoubleIndexed extends WithIndex {

                            /**
                             * The type variable's first index.
                             */
                            private final int preIndex;

                            /**
                             * Creates a new annotation collector.
                             *
                             * @param descriptor The annotation descriptor.
                             * @param typePath   The type variable's type path.
                             * @param index      The type variable's index.
                             * @param preIndex   The type variable's first index.
                             */
                            protected DoubleIndexed(String descriptor, TypePath typePath, int index, int preIndex) {
                                super(descriptor, typePath, index);
                                this.preIndex = preIndex;
                            }

                            @Override
                            protected Map<Integer, Map<String, List<LazyTypeDescription.AnnotationToken>>> getIndexedPathMap() {
                                Map<Integer, Map<Integer, Map<String, List<LazyTypeDescription.AnnotationToken>>>> doubleIndexPathMap = getDoubleIndexedPathMap();
                                Map<Integer, Map<String, List<LazyTypeDescription.AnnotationToken>>> indexedPathMap = doubleIndexPathMap.get(preIndex);
                                if (indexedPathMap == null) {
                                    indexedPathMap = new HashMap<Integer, Map<String, List<LazyTypeDescription.AnnotationToken>>>();
                                    doubleIndexPathMap.put(preIndex, indexedPathMap);
                                }
                                return indexedPathMap;
                            }

                            /**
                             * Returns this collector's double indexed path map.
                             *
                             * @return This collector's double indexed path map.
                             */
                            protected abstract Map<Integer, Map<Integer, Map<String, List<LazyTypeDescription.AnnotationToken>>>> getDoubleIndexedPathMap();
                        }
                    }
                }
            }

            /**
             * An annotation collector for a byte code element.
             */
            class ForByteCodeElement extends AbstractBase {

                /**
                 * The target collection.
                 */
                private final List<LazyTypeDescription.AnnotationToken> annotationTokens;

                /**
                 * Creates a new annotation collector for a byte code element.
                 *
                 * @param descriptor       The annotation descriptor.
                 * @param annotationTokens The target collection.
                 */
                protected ForByteCodeElement(String descriptor, List<LazyTypeDescription.AnnotationToken> annotationTokens) {
                    super(descriptor);
                    this.annotationTokens = annotationTokens;
                }

                @Override
                protected List<LazyTypeDescription.AnnotationToken> getTokens() {
                    return annotationTokens;
                }

                /**
                 * An annotation collector for a byte code element with an index.
                 */
                public static class WithIndex extends AbstractBase {

                    /**
                     * The byte code element's index.
                     */
                    private final int index;

                    /**
                     * The target collection.
                     */
                    private final Map<Integer, List<LazyTypeDescription.AnnotationToken>> annotationTokens;

                    /**
                     * Creates a new annotation collector for a byte code element with an index.
                     *
                     * @param descriptor       The annotation descriptor.
                     * @param index            The byte code element's index.
                     * @param annotationTokens The target collection.
                     */
                    protected WithIndex(String descriptor, int index, Map<Integer, List<LazyTypeDescription.AnnotationToken>> annotationTokens) {
                        super(descriptor);
                        this.index = index;
                        this.annotationTokens = annotationTokens;
                    }

                    @Override
                    protected List<LazyTypeDescription.AnnotationToken> getTokens() {
                        List<LazyTypeDescription.AnnotationToken> annotationTokens = this.annotationTokens.get(index);
                        if (annotationTokens == null) {
                            annotationTokens = new ArrayList<LazyTypeDescription.AnnotationToken>();
                            this.annotationTokens.put(index, annotationTokens);
                        }
                        return annotationTokens;
                    }
                }
            }

            /**
             * An annotation collector for a type variable.
             */
            class ForTypeVariable extends AbstractBase.ForTypeVariable {

                /**
                 * The target collection.
                 */
                private final Map<String, List<LazyTypeDescription.AnnotationToken>> pathMap;

                /**
                 * Creates a new annotation collector.
                 *
                 * @param descriptor The annotation descriptor.
                 * @param typePath   The type variable's type path.
                 * @param pathMap    The target collection.
                 */
                protected ForTypeVariable(String descriptor, TypePath typePath, Map<String, List<LazyTypeDescription.AnnotationToken>> pathMap) {
                    super(descriptor, typePath);
                    this.pathMap = pathMap;
                }

                @Override
                protected Map<String, List<LazyTypeDescription.AnnotationToken>> getPathMap() {
                    return pathMap;
                }

                /**
                 * An annotation collector for a type variable with an index.
                 */
                public static class WithIndex extends AbstractBase.ForTypeVariable.WithIndex {

                    /**
                     * The target collection.
                     */
                    private final Map<Integer, Map<String, List<LazyTypeDescription.AnnotationToken>>> indexedPathMap;

                    /**
                     * Creates a new annotation collector.
                     *
                     * @param descriptor     The annotation descriptor.
                     * @param typePath       The type variable's type path.
                     * @param index          The target index.
                     * @param indexedPathMap The target collection.
                     */
                    protected WithIndex(String descriptor,
                                        TypePath typePath,
                                        int index,
                                        Map<Integer, Map<String, List<LazyTypeDescription.AnnotationToken>>> indexedPathMap) {
                        super(descriptor, typePath, index);
                        this.indexedPathMap = indexedPathMap;
                    }

                    @Override
                    protected Map<Integer, Map<String, List<LazyTypeDescription.AnnotationToken>>> getIndexedPathMap() {
                        return indexedPathMap;
                    }

                    /**
                     * An annotation collector for a type variable with two indices.
                     */
                    public static class DoubleIndexed extends AbstractBase.ForTypeVariable.WithIndex.DoubleIndexed {

                        /**
                         * The target collection.
                         */
                        private final Map<Integer, Map<Integer, Map<String, List<LazyTypeDescription.AnnotationToken>>>> doubleIndexedPathMap;

                        /**
                         * Creates a new annotation collector.
                         *
                         * @param descriptor           The annotation descriptor.
                         * @param typePath             The type variable's type path.
                         * @param index                The target index.
                         * @param preIndex             The initial target index.
                         * @param doubleIndexedPathMap The target collection.
                         */
                        protected DoubleIndexed(String descriptor,
                                                TypePath typePath,
                                                int index,
                                                int preIndex,
                                                Map<Integer, Map<Integer, Map<String, List<LazyTypeDescription.AnnotationToken>>>> doubleIndexedPathMap) {
                            super(descriptor, typePath, index, preIndex);
                            this.doubleIndexedPathMap = doubleIndexedPathMap;
                        }

                        @Override
                        protected Map<Integer, Map<Integer, Map<String, List<LazyTypeDescription.AnnotationToken>>>> getDoubleIndexedPathMap() {
                            return doubleIndexedPathMap;
                        }
                    }
                }
            }
        }

        /**
         * A component type locator allows for the lazy location of an array's component type.
         */
        protected interface ComponentTypeLocator {

            /**
             * Binds this component type to a given property name of an annotation.
             *
             * @param name The name of an annotation property which the returned component type reference should
             *             query for resolving an array's component type.
             * @return A component type reference to an annotation value's component type.
             */
            RawDescriptionArray.ComponentTypeReference bind(String name);

            /**
             * A component type locator which cannot legally resolve an array's component type.
             */
            enum Illegal implements ComponentTypeLocator {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public RawDescriptionArray.ComponentTypeReference bind(String name) {
                    throw new IllegalStateException("Unexpected lookup of component type for " + name);
                }
            }

            /**
             * A component type locator that lazily analyses an annotation for resolving an annotation property's
             * array value's component type.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForAnnotationProperty implements ComponentTypeLocator {

                /**
                 * The type pool to query for type descriptions.
                 */
                private final TypePool typePool;

                /**
                 * The name of the annotation to analyze.
                 */
                private final String annotationName;

                /**
                 * Creates a new component type locator for an array value.
                 *
                 * @param typePool             The type pool to be used for looking up linked types.
                 * @param annotationDescriptor A descriptor of the annotation to analyze.
                 */
                public ForAnnotationProperty(TypePool typePool, String annotationDescriptor) {
                    this.typePool = typePool;
                    annotationName = annotationDescriptor.substring(1, annotationDescriptor.length() - 1).replace('/', '.');
                }

                @Override
                public RawDescriptionArray.ComponentTypeReference bind(String name) {
                    return new Bound(name);
                }

                /**
                 * A bound representation of a
                 * {@link net.bytebuddy.pool.TypePool.Default.ComponentTypeLocator.ForAnnotationProperty}.
                 */
                @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
                protected class Bound implements RawDescriptionArray.ComponentTypeReference {

                    /**
                     * The name of the annotation property.
                     */
                    private final String name;

                    /**
                     * Creates a new bound component type locator for an annotation property.
                     *
                     * @param name The name of the annotation property.
                     */
                    protected Bound(String name) {
                        this.name = name;
                    }

                    @Override
                    public String lookup() {
                        return typePool.describe(annotationName)
                                .resolve()
                                .getDeclaredMethods()
                                .filter(named(name))
                                .getOnly()
                                .getReturnType()
                                .asErasure()
                                .getComponentType()
                                .getName();
                    }
                }
            }

            /**
             * A component type locator that locates an array type by a method's return value from its method descriptor.
             */
            @HashCodeAndEqualsPlugin.Enhance
            class ForArrayType implements ComponentTypeLocator, RawDescriptionArray.ComponentTypeReference {

                /**
                 * The resolved component type's binary name.
                 */
                private final String componentType;

                /**
                 * Creates a new component type locator for an array type.
                 *
                 * @param methodDescriptor The method descriptor to resolve.
                 */
                public ForArrayType(String methodDescriptor) {
                    String arrayType = Type.getMethodType(methodDescriptor).getReturnType().getClassName();
                    componentType = arrayType.substring(0, arrayType.length() - 2);
                }

                @Override
                public RawDescriptionArray.ComponentTypeReference bind(String name) {
                    return this;
                }

                @Override
                public String lookup() {
                    return componentType;
                }
            }
        }

        /**
         * A type registrant allows to register a generic type token.
         */
        protected interface GenericTypeRegistrant {

            /**
             * Registers a discovered generic type token.
             *
             * @param token The token to be registered.
             */
            void register(LazyTypeDescription.GenericTypeToken token);

            /**
             * A signature visitor that rejects any discovered generic type.
             */
            class RejectingSignatureVisitor extends SignatureVisitor {

                /**
                 * The message of the error message.
                 */
                private static final String MESSAGE = "Unexpected token in generic signature";

                /**
                 * Creates a new rejecting signature visitor.
                 */
                public RejectingSignatureVisitor() {
                    super(Opcodes.ASM6);
                }

                @Override
                public void visitFormalTypeParameter(String name) {
                    throw new IllegalStateException(MESSAGE);
                }

                @Override
                public SignatureVisitor visitClassBound() {
                    throw new IllegalStateException(MESSAGE);
                }

                @Override
                public SignatureVisitor visitInterfaceBound() {
                    throw new IllegalStateException(MESSAGE);
                }

                @Override
                public SignatureVisitor visitSuperclass() {
                    throw new IllegalStateException(MESSAGE);
                }

                @Override
                public SignatureVisitor visitInterface() {
                    throw new IllegalStateException(MESSAGE);
                }

                @Override
                public SignatureVisitor visitParameterType() {
                    throw new IllegalStateException(MESSAGE);
                }

                @Override
                public SignatureVisitor visitReturnType() {
                    throw new IllegalStateException(MESSAGE);
                }

                @Override
                public SignatureVisitor visitExceptionType() {
                    throw new IllegalStateException(MESSAGE);
                }

                @Override
                public void visitBaseType(char descriptor) {
                    throw new IllegalStateException(MESSAGE);
                }

                @Override
                public void visitTypeVariable(String name) {
                    throw new IllegalStateException(MESSAGE);
                }

                @Override
                public SignatureVisitor visitArrayType() {
                    throw new IllegalStateException(MESSAGE);
                }

                @Override
                public void visitClassType(String name) {
                    throw new IllegalStateException(MESSAGE);
                }

                @Override
                public void visitInnerClassType(String name) {
                    throw new IllegalStateException(MESSAGE);
                }

                @Override
                public void visitTypeArgument() {
                    throw new IllegalStateException(MESSAGE);
                }

                @Override
                public SignatureVisitor visitTypeArgument(char wildcard) {
                    throw new IllegalStateException(MESSAGE);
                }

                @Override
                public void visitEnd() {
                    throw new IllegalStateException(MESSAGE);
                }
            }
        }

        /**
         * A bag for collecting parameter meta information that is stored as debug information for implemented
         * methods.
         */
        protected static class ParameterBag {

            /**
             * An array of the method's parameter types.
             */
            private final Type[] parameterType;

            /**
             * A map containing the tokens that were collected until now.
             */
            private final Map<Integer, String> parameterRegistry;

            /**
             * Creates a new bag.
             *
             * @param parameterType An array of parameter types for the method on which this parameter bag
             *                      is used.
             */
            protected ParameterBag(Type[] parameterType) {
                this.parameterType = parameterType;
                parameterRegistry = new HashMap<Integer, String>();
            }

            /**
             * Registers a new parameter.
             *
             * @param offset The offset of the registered entry on the local variable array of the method.
             * @param name   The name of the parameter.
             */
            protected void register(int offset, String name) {
                parameterRegistry.put(offset, name);
            }

            /**
             * Resolves the collected parameters as a list of parameter tokens.
             *
             * @param isStatic {@code true} if the analyzed method is static.
             * @return A list of parameter tokens based on the collected information.
             */
            protected List<LazyTypeDescription.MethodToken.ParameterToken> resolve(boolean isStatic) {
                List<LazyTypeDescription.MethodToken.ParameterToken> parameterTokens = new ArrayList<LazyTypeDescription.MethodToken.ParameterToken>(parameterType.length);
                int offset = isStatic
                        ? StackSize.ZERO.getSize()
                        : StackSize.SINGLE.getSize();
                for (Type aParameterType : parameterType) {
                    String name = this.parameterRegistry.get(offset);
                    parameterTokens.add(name == null
                            ? new LazyTypeDescription.MethodToken.ParameterToken()
                            : new LazyTypeDescription.MethodToken.ParameterToken(name));
                    offset += aParameterType.getSize();
                }
                return parameterTokens;
            }
        }

        /**
         * A generic type extractor allows for an iterative extraction of generic type information.
         */
        protected static class GenericTypeExtractor extends GenericTypeRegistrant.RejectingSignatureVisitor implements GenericTypeRegistrant {

            /**
             * A registrant that receives any discovered type.
             */
            private final GenericTypeRegistrant genericTypeRegistrant;

            /**
             * The current token that is in the process of creation.
             */
            private IncompleteToken incompleteToken;

            /**
             * Creates a new generic type extractor.
             *
             * @param genericTypeRegistrant The target to receive the complete type.
             */
            protected GenericTypeExtractor(GenericTypeRegistrant genericTypeRegistrant) {
                this.genericTypeRegistrant = genericTypeRegistrant;
            }

            @Override
            public void visitBaseType(char descriptor) {
                genericTypeRegistrant.register(LazyTypeDescription.GenericTypeToken.ForPrimitiveType.of(descriptor));
            }

            @Override
            public void visitTypeVariable(String name) {
                genericTypeRegistrant.register(new LazyTypeDescription.GenericTypeToken.ForTypeVariable(name));
            }

            @Override
            public SignatureVisitor visitArrayType() {
                return new GenericTypeExtractor(this);
            }

            @Override
            public void register(LazyTypeDescription.GenericTypeToken componentTypeToken) {
                genericTypeRegistrant.register(new LazyTypeDescription.GenericTypeToken.ForGenericArray(componentTypeToken));
            }

            @Override
            public void visitClassType(String name) {
                incompleteToken = new IncompleteToken.ForTopLevelType(name);
            }

            @Override
            public void visitInnerClassType(String name) {
                incompleteToken = new IncompleteToken.ForInnerClass(name, incompleteToken);
            }

            @Override
            public void visitTypeArgument() {
                incompleteToken.appendPlaceholder();
            }

            @Override
            public SignatureVisitor visitTypeArgument(char wildcard) {
                switch (wildcard) {
                    case SignatureVisitor.SUPER:
                        return incompleteToken.appendLowerBound();
                    case SignatureVisitor.EXTENDS:
                        return incompleteToken.appendUpperBound();
                    case SignatureVisitor.INSTANCEOF:
                        return incompleteToken.appendDirectBound();
                    default:
                        throw new IllegalArgumentException("Unknown wildcard: " + wildcard);
                }
            }

            @Override
            public void visitEnd() {
                genericTypeRegistrant.register(incompleteToken.toToken());
            }

            /**
             * An incomplete {@link LazyTypeDescription.GenericTypeToken}.
             */
            protected interface IncompleteToken {

                /**
                 * Appends a lower bound to this token.
                 *
                 * @return A signature visitor for visiting the lower bound's type.
                 */
                SignatureVisitor appendLowerBound();

                /**
                 * Appends an upper bound to this token.
                 *
                 * @return A signature visitor for visiting the upper bound's type.
                 */
                SignatureVisitor appendUpperBound();

                /**
                 * Appends a direct bound to this token.
                 *
                 * @return A signature visitor for visiting the direct bound's type.
                 */
                SignatureVisitor appendDirectBound();

                /**
                 * Appends a placeholder to this token.
                 */
                void appendPlaceholder();

                /**
                 * Returns {@code true} if this token describes a type with parameters.
                 *
                 * @return {@code true} if this token describes a type with parameters.
                 */
                boolean isParameterized();

                /**
                 * Returns the name of this token.
                 *
                 * @return The name of this token.
                 */
                String getName();

                /**
                 * Converts this incomplete token to a completed token.
                 *
                 * @return The finalized token.
                 */
                LazyTypeDescription.GenericTypeToken toToken();

                /**
                 * An abstract base implementation of an incomplete token.
                 */
                abstract class AbstractBase implements IncompleteToken {

                    /**
                     * The parameters of this token.
                     */
                    protected final List<LazyTypeDescription.GenericTypeToken> parameters;

                    /**
                     * Creates a new base implementation of an incomplete token.
                     */
                    public AbstractBase() {
                        parameters = new ArrayList<LazyTypeDescription.GenericTypeToken>();
                    }

                    @Override
                    public SignatureVisitor appendDirectBound() {
                        return new GenericTypeExtractor(new ForDirectBound());
                    }

                    @Override
                    public SignatureVisitor appendUpperBound() {
                        return new GenericTypeExtractor(new ForUpperBound());
                    }

                    @Override
                    public SignatureVisitor appendLowerBound() {
                        return new GenericTypeExtractor(new ForLowerBound());
                    }

                    @Override
                    public void appendPlaceholder() {
                        parameters.add(LazyTypeDescription.GenericTypeToken.ForUnboundWildcard.INSTANCE);
                    }

                    /**
                     * A token for registering a direct bound.
                     */
                    protected class ForDirectBound implements GenericTypeRegistrant {

                        @Override
                        public void register(LazyTypeDescription.GenericTypeToken token) {
                            parameters.add(token);
                        }
                    }

                    /**
                     * A token for registering a wildcard with an upper bound.
                     */
                    protected class ForUpperBound implements GenericTypeRegistrant {

                        @Override
                        public void register(LazyTypeDescription.GenericTypeToken token) {
                            parameters.add(new LazyTypeDescription.GenericTypeToken.ForUpperBoundWildcard(token));
                        }
                    }

                    /**
                     * A token for registering a wildcard with a lower bound.
                     */
                    protected class ForLowerBound implements GenericTypeRegistrant {

                        @Override
                        public void register(LazyTypeDescription.GenericTypeToken token) {
                            parameters.add(new LazyTypeDescription.GenericTypeToken.ForLowerBoundWildcard(token));
                        }
                    }
                }

                /**
                 * An incomplete token representing a generic type without an outer type.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                class ForTopLevelType extends AbstractBase {

                    /**
                     * The internal name of the type.
                     */
                    private final String internalName;

                    /**
                     * Creates a new incomplete token representing a type without an outer type.
                     *
                     * @param internalName The internal name of the type.
                     */
                    public ForTopLevelType(String internalName) {
                        this.internalName = internalName;
                    }

                    @Override
                    public LazyTypeDescription.GenericTypeToken toToken() {
                        return isParameterized()
                                ? new LazyTypeDescription.GenericTypeToken.ForParameterizedType(getName(), parameters)
                                : new LazyTypeDescription.GenericTypeToken.ForRawType(getName());
                    }

                    @Override
                    public boolean isParameterized() {
                        return !parameters.isEmpty();
                    }

                    @Override
                    public String getName() {
                        return internalName.replace('/', '.');
                    }
                }

                /**
                 * An incomplete generic type token representing a type with an outer type.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                class ForInnerClass extends AbstractBase {

                    /**
                     * The separator that indicates an inner type.
                     */
                    private static final char INNER_CLASS_SEPARATOR = '$';

                    /**
                     * The internal name of the type.
                     */
                    private final String internalName;

                    /**
                     * The token representing the outer type.
                     */
                    private final IncompleteToken outerTypeToken;

                    /**
                     * Creates a new incomplete token representing a type without an outer type.
                     *
                     * @param internalName   The internal name of the type.
                     * @param outerTypeToken The incomplete token representing the outer type.
                     */
                    public ForInnerClass(String internalName, IncompleteToken outerTypeToken) {
                        this.internalName = internalName;
                        this.outerTypeToken = outerTypeToken;
                    }

                    @Override
                    public LazyTypeDescription.GenericTypeToken toToken() {
                        return isParameterized() || outerTypeToken.isParameterized()
                                ? new LazyTypeDescription.GenericTypeToken.ForParameterizedType.Nested(getName(), parameters, outerTypeToken.toToken())
                                : new LazyTypeDescription.GenericTypeToken.ForRawType(getName());
                    }

                    @Override
                    public boolean isParameterized() {
                        return !parameters.isEmpty() || !outerTypeToken.isParameterized();
                    }

                    @Override
                    public String getName() {
                        return outerTypeToken.getName() + INNER_CLASS_SEPARATOR + internalName.replace('/', '.');
                    }
                }
            }

            /**
             * A signature visitor for extracting a generic type resolution.
             *
             * @param <T> The type of the resolution this visitor extracts.
             */
            protected abstract static class ForSignature<T extends LazyTypeDescription.GenericTypeToken.Resolution>
                    extends RejectingSignatureVisitor
                    implements GenericTypeRegistrant {

                /**
                 * The resolved type variable tokens.
                 */
                protected final List<LazyTypeDescription.GenericTypeToken.OfFormalTypeVariable> typeVariableTokens;

                /**
                 * The name of the currently constructed type.
                 */
                protected String currentTypeParameter;

                /**
                 * The bounds of the currently constructed type.
                 */
                protected List<LazyTypeDescription.GenericTypeToken> currentBounds;

                /**
                 * Creates a new signature visitor.
                 */
                public ForSignature() {
                    typeVariableTokens = new ArrayList<LazyTypeDescription.GenericTypeToken.OfFormalTypeVariable>();
                }

                /**
                 * Applies an extraction of a generic signature given the supplied visitor.
                 *
                 * @param genericSignature The generic signature to interpret.
                 * @param visitor          The visitor to apply.
                 * @param <S>              The type of the generated resolution.
                 * @return The resolution of the supplied signature.
                 */
                protected static <S extends LazyTypeDescription.GenericTypeToken.Resolution> S extract(String genericSignature, ForSignature<S> visitor) {
                    SignatureReader signatureReader = new SignatureReader(genericSignature);
                    signatureReader.accept(visitor);
                    return visitor.resolve();
                }

                @Override
                public void visitFormalTypeParameter(String name) {
                    collectTypeParameter();
                    currentTypeParameter = name;
                    currentBounds = new ArrayList<LazyTypeDescription.GenericTypeToken>();
                }

                @Override
                public SignatureVisitor visitClassBound() {
                    return new GenericTypeExtractor(this);
                }

                @Override
                public SignatureVisitor visitInterfaceBound() {
                    return new GenericTypeExtractor(this);
                }

                @Override
                public void register(LazyTypeDescription.GenericTypeToken token) {
                    if (currentBounds == null) {
                        throw new IllegalStateException("Did not expect " + token + " before finding formal parameter");
                    }
                    currentBounds.add(token);
                }

                /**
                 * Collects the currently constructed type.
                 */
                protected void collectTypeParameter() {
                    if (currentTypeParameter != null) {
                        typeVariableTokens.add(new LazyTypeDescription.GenericTypeToken.ForTypeVariable.Formal(currentTypeParameter, currentBounds));
                    }
                }

                /**
                 * Completes the current resolution.
                 *
                 * @return The resolved generic signature.
                 */
                public abstract T resolve();

                /**
                 * A parser for a generic type signature.
                 */
                protected static class OfType extends ForSignature<LazyTypeDescription.GenericTypeToken.Resolution.ForType> {

                    /**
                     * The interface type's generic signatures.
                     */
                    private final List<LazyTypeDescription.GenericTypeToken> interfaceTypeTokens;

                    /**
                     * The super type's generic signature.
                     */
                    private LazyTypeDescription.GenericTypeToken superClassToken;

                    /**
                     * Creates a new parser for a type signature.
                     */
                    protected OfType() {
                        interfaceTypeTokens = new ArrayList<LazyTypeDescription.GenericTypeToken>();
                    }

                    /**
                     * Extracts a generic type resolution of a type signature.
                     *
                     * @param genericSignature The signature to interpret.
                     * @return The interpreted type signature.
                     */
                    public static LazyTypeDescription.GenericTypeToken.Resolution.ForType extract(String genericSignature) {
                        try {
                            return genericSignature == null
                                    ? LazyTypeDescription.GenericTypeToken.Resolution.Raw.INSTANCE
                                    : ForSignature.extract(genericSignature, new OfType());
                        } catch (RuntimeException ignored) {
                            return LazyTypeDescription.GenericTypeToken.Resolution.Malformed.INSTANCE;
                        }
                    }

                    @Override
                    public SignatureVisitor visitSuperclass() {
                        collectTypeParameter();
                        return new GenericTypeExtractor(new SuperClassRegistrant());
                    }

                    @Override
                    public SignatureVisitor visitInterface() {
                        return new GenericTypeExtractor(new InterfaceTypeRegistrant());
                    }

                    @Override
                    public LazyTypeDescription.GenericTypeToken.Resolution.ForType resolve() {
                        return new LazyTypeDescription.GenericTypeToken.Resolution.ForType.Tokenized(superClassToken, interfaceTypeTokens, typeVariableTokens);
                    }

                    /**
                     * A registrant for the super type.
                     */
                    @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
                    protected class SuperClassRegistrant implements GenericTypeRegistrant {

                        @Override
                        public void register(LazyTypeDescription.GenericTypeToken token) {
                            superClassToken = token;
                        }
                    }

                    /**
                     * A registrant for the interface types.
                     */
                    @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
                    protected class InterfaceTypeRegistrant implements GenericTypeRegistrant {

                        @Override
                        public void register(LazyTypeDescription.GenericTypeToken token) {
                            interfaceTypeTokens.add(token);
                        }
                    }
                }

                /**
                 * A parser for a generic method signature.
                 */
                protected static class OfMethod extends ForSignature<LazyTypeDescription.GenericTypeToken.Resolution.ForMethod> {

                    /**
                     * The generic parameter types.
                     */
                    private final List<LazyTypeDescription.GenericTypeToken> parameterTypeTokens;

                    /**
                     * The generic exception types.
                     */
                    private final List<LazyTypeDescription.GenericTypeToken> exceptionTypeTokens;

                    /**
                     * The generic return type.
                     */
                    private LazyTypeDescription.GenericTypeToken returnTypeToken;

                    /**
                     * Creates a parser for a generic method signature.
                     */
                    public OfMethod() {
                        parameterTypeTokens = new ArrayList<LazyTypeDescription.GenericTypeToken>();
                        exceptionTypeTokens = new ArrayList<LazyTypeDescription.GenericTypeToken>();
                    }

                    /**
                     * Extracts a generic method resolution of a method signature.
                     *
                     * @param genericSignature The signature to interpret.
                     * @return The interpreted method signature.
                     */
                    public static LazyTypeDescription.GenericTypeToken.Resolution.ForMethod extract(String genericSignature) {
                        try {
                            return genericSignature == null
                                    ? LazyTypeDescription.GenericTypeToken.Resolution.Raw.INSTANCE
                                    : ForSignature.extract(genericSignature, new OfMethod());
                        } catch (RuntimeException ignored) {
                            return LazyTypeDescription.GenericTypeToken.Resolution.Malformed.INSTANCE;
                        }
                    }

                    @Override
                    public SignatureVisitor visitParameterType() {
                        return new GenericTypeExtractor(new ParameterTypeRegistrant());
                    }

                    @Override
                    public SignatureVisitor visitReturnType() {
                        collectTypeParameter();
                        return new GenericTypeExtractor(new ReturnTypeTypeRegistrant());
                    }

                    @Override
                    public SignatureVisitor visitExceptionType() {
                        return new GenericTypeExtractor(new ExceptionTypeRegistrant());
                    }

                    @Override
                    public LazyTypeDescription.GenericTypeToken.Resolution.ForMethod resolve() {
                        return new LazyTypeDescription.GenericTypeToken.Resolution.ForMethod.Tokenized(returnTypeToken,
                                parameterTypeTokens,
                                exceptionTypeTokens,
                                typeVariableTokens);
                    }

                    /**
                     * A registrant for a parameter type.
                     */
                    @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
                    protected class ParameterTypeRegistrant implements GenericTypeRegistrant {

                        @Override
                        public void register(LazyTypeDescription.GenericTypeToken token) {
                            parameterTypeTokens.add(token);
                        }
                    }

                    /**
                     * A registrant for a return type.
                     */
                    @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
                    protected class ReturnTypeTypeRegistrant implements GenericTypeRegistrant {

                        @Override
                        public void register(LazyTypeDescription.GenericTypeToken token) {
                            returnTypeToken = token;
                        }
                    }

                    /**
                     * A registrant for an exception type.
                     */
                    @HashCodeAndEqualsPlugin.Enhance(includeSyntheticFields = true)
                    protected class ExceptionTypeRegistrant implements GenericTypeRegistrant {

                        @Override
                        public void register(LazyTypeDescription.GenericTypeToken token) {
                            exceptionTypeTokens.add(token);
                        }
                    }
                }

                /**
                 * A parser for a generic field signature.
                 */
                protected static class OfField implements GenericTypeRegistrant {

                    /**
                     * The generic field type.
                     */
                    private LazyTypeDescription.GenericTypeToken fieldTypeToken;

                    /**
                     * Extracts a generic field resolution of a field signature.
                     *
                     * @param genericSignature The signature to interpret.
                     * @return The interpreted field signature.
                     */
                    public static LazyTypeDescription.GenericTypeToken.Resolution.ForField extract(String genericSignature) {
                        if (genericSignature == null) {
                            return LazyTypeDescription.GenericTypeToken.Resolution.Raw.INSTANCE;
                        } else {
                            SignatureReader signatureReader = new SignatureReader(genericSignature);
                            OfField visitor = new OfField();
                            try {
                                signatureReader.acceptType(new GenericTypeExtractor(visitor));
                                return visitor.resolve();
                            } catch (RuntimeException ignored) {
                                return LazyTypeDescription.GenericTypeToken.Resolution.Malformed.INSTANCE;
                            }
                        }
                    }

                    @Override
                    public void register(LazyTypeDescription.GenericTypeToken token) {
                        fieldTypeToken = token;
                    }

                    /**
                     * Completes the current resolution.
                     *
                     * @return The resolved generic signature.
                     */
                    protected LazyTypeDescription.GenericTypeToken.Resolution.ForField resolve() {
                        return new LazyTypeDescription.GenericTypeToken.Resolution.ForField.Tokenized(fieldTypeToken);
                    }
                }
            }
        }

        /**
         * A type description that looks up any referenced {@link net.bytebuddy.description.ByteCodeElement} or
         * {@link AnnotationDescription} by querying a type pool at lookup time.
         */
        protected static class LazyTypeDescription extends TypeDescription.AbstractBase.OfSimpleType {

            /**
             * The index of a super class's type annotations.
             */
            private static final int SUPER_CLASS_INDEX = -1;

            /**
             * Indicates that a type does not exist and does therefore not have a name.
             */
            private static final String NO_TYPE = null;

            /**
             * The type pool to be used for looking up linked types.
             */
            private final TypePool typePool;

            /**
             * The actual modifiers of this type.
             */
            private final int actualModifiers;

            /**
             * The modifiers of this type.
             */
            private final int modifiers;

            /**
             * The binary name of this type.
             */
            private final String name;

            /**
             * The type's super type's descriptor or {@code null} if this type does not define a super type.
             */
            private final String superClassDescriptor;

            /**
             * The type's generic signature as found in the class file or {@code null} if the type is not generic.
             */
            private final String genericSignature;

            /**
             * The resolution of this type's generic type.
             */
            private final GenericTypeToken.Resolution.ForType signatureResolution;

            /**
             * The descriptor of this type's interfaces.
             */
            private final List<String> interfaceTypeDescriptors;

            /**
             * A definition of this type's containment within another type or method.
             */
            private final TypeContainment typeContainment;

            /**
             * The binary name of this type's declaring type or {@code null} if no such type exists.
             */
            private final String declaringTypeName;

            /**
             * A list of descriptors representing the types that are declared by this type.
             */
            private final List<String> declaredTypes;

            /**
             * {@code true} if this type is an anonymous type.
             */
            private final boolean anonymousType;

            /**
             * A mapping of type annotations for this type's super type and interface types by their indices.
             */
            private final Map<Integer, Map<String, List<AnnotationToken>>> superTypeAnnotationTokens;

            /**
             * A mapping of type annotations of the type variables' type annotations by their indices.
             */
            private final Map<Integer, Map<String, List<AnnotationToken>>> typeVariableAnnotationTokens;

            /**
             * A mapping of type annotations of the type variables' bounds' type annotations by their indices and each variable's index.
             */
            private final Map<Integer, Map<Integer, Map<String, List<AnnotationToken>>>> typeVariableBoundsAnnotationTokens;

            /**
             * A list of tokens that represent the annotations of this type.
             */
            private final List<AnnotationToken> annotationTokens;

            /**
             * A list of field tokens describing the field's of this type.
             */
            private final List<FieldToken> fieldTokens;

            /**
             * A list of method tokens describing the method's of this type.
             */
            private final List<MethodToken> methodTokens;

            /**
             * Creates a new lazy type description.
             *
             * @param typePool                           The type pool to be used for looking up linked types.
             * @param actualModifiers                    The actual modifiers of this type.
             * @param modifiers                          The modifiers of this type.
             * @param name                               The binary name of this type.
             * @param superClassInternalName             The internal name of this type's super type or {@code null} if no such super type is defined.
             * @param interfaceInternalName              An array of this type's interfaces or {@code null} if this type does not define any interfaces.
             * @param genericSignature                   The type's generic signature as found in the class file or {@code null} if the type is not generic.
             * @param typeContainment                    A definition of this type's containment within another type or method.
             * @param declaringTypeInternalName          The internal name of this type's declaring type or {@code null} if no such type exists.
             * @param declaredTypes                      A list of descriptors representing the types that are declared by this type.
             * @param anonymousType                      {@code true} if this type is an anonymous type.
             * @param superTypeAnnotationTokens          A mapping of type annotations for this type's super type and interface types by their indices.
             * @param typeVariableAnnotationTokens       A mapping of type annotations of the type variables' type annotations by their indices.
             * @param typeVariableBoundsAnnotationTokens A mapping of type annotations of the type variables' bounds' type annotations by their indices
             *                                           and each variable's index.
             * @param annotationTokens                   A list of tokens that represent the annotations of this type.
             * @param fieldTokens                        A list of field tokens describing the field's of this type.
             * @param methodTokens                       A list of method tokens describing the method's of this type.
             */
            protected LazyTypeDescription(TypePool typePool,
                                          int actualModifiers,
                                          int modifiers,
                                          String name,
                                          String superClassInternalName,
                                          String[] interfaceInternalName,
                                          String genericSignature,
                                          TypeContainment typeContainment,
                                          String declaringTypeInternalName,
                                          List<String> declaredTypes,
                                          boolean anonymousType,
                                          Map<Integer, Map<String, List<AnnotationToken>>> superTypeAnnotationTokens,
                                          Map<Integer, Map<String, List<AnnotationToken>>> typeVariableAnnotationTokens,
                                          Map<Integer, Map<Integer, Map<String, List<AnnotationToken>>>> typeVariableBoundsAnnotationTokens,
                                          List<AnnotationToken> annotationTokens,
                                          List<FieldToken> fieldTokens,
                                          List<MethodToken> methodTokens) {
                this.typePool = typePool;
                this.actualModifiers = actualModifiers & ~Opcodes.ACC_SUPER;
                this.modifiers = modifiers & ~(Opcodes.ACC_SUPER | Opcodes.ACC_DEPRECATED);
                this.name = Type.getObjectType(name).getClassName();
                this.superClassDescriptor = superClassInternalName == null
                        ? NO_TYPE
                        : Type.getObjectType(superClassInternalName).getDescriptor();
                this.genericSignature = genericSignature;
                signatureResolution = RAW_TYPES
                        ? GenericTypeToken.Resolution.Raw.INSTANCE
                        : GenericTypeExtractor.ForSignature.OfType.extract(genericSignature);
                if (interfaceInternalName == null) {
                    interfaceTypeDescriptors = Collections.emptyList();
                } else {
                    interfaceTypeDescriptors = new ArrayList<String>(interfaceInternalName.length);
                    for (String internalName : interfaceInternalName) {
                        interfaceTypeDescriptors.add(Type.getObjectType(internalName).getDescriptor());
                    }
                }
                this.typeContainment = typeContainment;
                declaringTypeName = declaringTypeInternalName == null
                        ? NO_TYPE
                        : declaringTypeInternalName.replace('/', '.');
                this.declaredTypes = declaredTypes;
                this.anonymousType = anonymousType;
                this.superTypeAnnotationTokens = superTypeAnnotationTokens;
                this.typeVariableAnnotationTokens = typeVariableAnnotationTokens;
                this.typeVariableBoundsAnnotationTokens = typeVariableBoundsAnnotationTokens;
                this.annotationTokens = annotationTokens;
                this.fieldTokens = fieldTokens;
                this.methodTokens = methodTokens;
            }

            @Override
            public Generic getSuperClass() {
                return superClassDescriptor == null || isInterface()
                        ? Generic.UNDEFINED
                        : signatureResolution.resolveSuperClass(superClassDescriptor, typePool, superTypeAnnotationTokens.get(SUPER_CLASS_INDEX), this);
            }

            @Override
            public TypeList.Generic getInterfaces() {
                return signatureResolution.resolveInterfaceTypes(interfaceTypeDescriptors, typePool, superTypeAnnotationTokens, this);
            }

            @Override
            public MethodDescription getEnclosingMethod() {
                return typeContainment.getEnclosingMethod(typePool);
            }

            @Override
            public TypeDescription getEnclosingType() {
                return typeContainment.getEnclosingType(typePool);
            }

            @Override
            public TypeList getDeclaredTypes() {
                return new LazyTypeList(typePool, declaredTypes);
            }

            @Override
            public boolean isAnonymousClass() {
                return anonymousType;
            }

            @Override
            public boolean isLocalClass() {
                return !anonymousType && typeContainment.isLocalType();
            }

            @Override
            public boolean isMemberClass() {
                return typeContainment.isMemberClass();
            }

            @Override
            public FieldList<FieldDescription.InDefinedShape> getDeclaredFields() {
                return new FieldTokenList();
            }

            @Override
            public MethodList<MethodDescription.InDefinedShape> getDeclaredMethods() {
                return new MethodTokenList();
            }

            @Override
            public PackageDescription getPackage() {
                String name = getName();
                int index = name.lastIndexOf('.');
                return index == -1
                        ? PackageDescription.UNDEFINED
                        : new LazyPackageDescription(typePool, name.substring(0, index));
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public TypeDescription getDeclaringType() {
                return declaringTypeName == null
                        ? TypeDescription.UNDEFINED
                        : typePool.describe(declaringTypeName).resolve();
            }

            @Override
            public int getModifiers() {
                return modifiers;
            }

            @Override
            public int getActualModifiers(boolean superFlag) {
                return superFlag ? (actualModifiers | Opcodes.ACC_SUPER) : actualModifiers;
            }

            @Override
            public AnnotationList getDeclaredAnnotations() {
                return LazyAnnotationDescription.asList(typePool, annotationTokens);
            }

            @Override
            public TypeList.Generic getTypeVariables() {
                return signatureResolution.resolveTypeVariables(typePool, this, typeVariableAnnotationTokens, typeVariableBoundsAnnotationTokens);
            }

            @Override
            public String getGenericSignature() {
                return genericSignature;
            }

            /**
             * A list of field tokens representing each entry as a field description.
             */
            protected class FieldTokenList extends FieldList.AbstractBase<FieldDescription.InDefinedShape> {

                @Override
                public FieldDescription.InDefinedShape get(int index) {
                    return fieldTokens.get(index).toFieldDescription(LazyTypeDescription.this);
                }

                @Override
                public int size() {
                    return fieldTokens.size();
                }
            }

            /**
             * A list of method tokens representing each entry as a method description.
             */
            protected class MethodTokenList extends MethodList.AbstractBase<MethodDescription.InDefinedShape> {

                @Override
                public MethodDescription.InDefinedShape get(int index) {
                    return methodTokens.get(index).toMethodDescription(LazyTypeDescription.this);
                }

                @Override
                public int size() {
                    return methodTokens.size();
                }
            }

            /**
             * A declaration context encapsulates information about whether a type was declared within another type
             * or within a method of another type.
             */
            protected interface TypeContainment {

                /**
                 * Returns the enclosing method or {@code null} if no such method exists.
                 *
                 * @param typePool The type pool to be used for looking up linked types.
                 * @return A method description describing the linked type or {@code null}.
                 */
                MethodDescription getEnclosingMethod(TypePool typePool);

                /**
                 * Returns the enclosing type or {@code null} if no such type exists.
                 *
                 * @param typePool The type pool to be used for looking up linked types.
                 * @return A type description describing the linked type or {@code null}.
                 */
                TypeDescription getEnclosingType(TypePool typePool);

                /**
                 * Returns {@code true} if the type is self-contained.
                 *
                 * @return {@code true} if the type is self-contained.
                 */
                boolean isSelfContained();

                /**
                 * Returns {@code true} if the type is a member type.
                 *
                 * @return {@code true} if the type is a member type.
                 */
                boolean isMemberClass();

                /**
                 * Returns {@code true} if the type is a local type unless it is an anonymous type.
                 *
                 * @return {@code true} if the type is a local type unless it is an anonymous type
                 */
                boolean isLocalType();

                /**
                 * Describes a type that is not contained within another type, a method or a constructor.
                 */
                enum SelfContained implements TypeContainment {

                    /**
                     * The singleton instance.
                     */
                    INSTANCE;

                    @Override
                    public MethodDescription getEnclosingMethod(TypePool typePool) {
                        return MethodDescription.UNDEFINED;
                    }

                    @Override
                    public TypeDescription getEnclosingType(TypePool typePool) {
                        return TypeDescription.UNDEFINED;
                    }

                    @Override
                    public boolean isSelfContained() {
                        return true;
                    }

                    @Override
                    public boolean isMemberClass() {
                        return false;
                    }

                    @Override
                    public boolean isLocalType() {
                        return false;
                    }
                }

                /**
                 * Describes a type that is contained within another type.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                class WithinType implements TypeContainment {

                    /**
                     * The type's binary name.
                     */
                    private final String name;

                    /**
                     * {@code true} if the type is a local type unless it is an anonymous type.
                     */
                    private final boolean localType;

                    /**
                     * Creates a new type containment for a type that is declared within another type.
                     *
                     * @param internalName The type's internal name.
                     * @param localType    {@code true} if the type is a local type unless it is an anonymous type.
                     */
                    public WithinType(String internalName, boolean localType) {
                        name = internalName.replace('/', '.');
                        this.localType = localType;
                    }

                    @Override
                    public MethodDescription getEnclosingMethod(TypePool typePool) {
                        return MethodDescription.UNDEFINED;
                    }

                    @Override
                    public TypeDescription getEnclosingType(TypePool typePool) {
                        return typePool.describe(name).resolve();
                    }

                    @Override
                    public boolean isSelfContained() {
                        return false;
                    }

                    @Override
                    public boolean isMemberClass() {
                        return !localType;
                    }

                    @Override
                    public boolean isLocalType() {
                        return localType;
                    }
                }

                /**
                 * Describes a type that is contained within a method or constructor.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                class WithinMethod implements TypeContainment {

                    /**
                     * The method's declaring type's internal name.
                     */
                    private final String name;

                    /**
                     * The method's internal name.
                     */
                    private final String methodName;

                    /**
                     * The method's descriptor.
                     */
                    private final String methodDescriptor;

                    /**
                     * Creates a new type containment for a type that is declared within a method.
                     *
                     * @param internalName     The method's declaring type's internal name.
                     * @param methodName       The method's internal name.
                     * @param methodDescriptor The method's descriptor.
                     */
                    public WithinMethod(String internalName, String methodName, String methodDescriptor) {
                        name = internalName.replace('/', '.');
                        this.methodName = methodName;
                        this.methodDescriptor = methodDescriptor;
                    }

                    @Override
                    public MethodDescription getEnclosingMethod(TypePool typePool) {
                        return getEnclosingType(typePool).getDeclaredMethods().filter(hasMethodName(methodName).and(hasDescriptor(methodDescriptor))).getOnly();
                    }

                    @Override
                    public TypeDescription getEnclosingType(TypePool typePool) {
                        return typePool.describe(name).resolve();
                    }

                    @Override
                    public boolean isSelfContained() {
                        return false;
                    }

                    @Override
                    public boolean isMemberClass() {
                        return false;
                    }

                    @Override
                    public boolean isLocalType() {
                        return true;
                    }
                }
            }

            /**
             * A token that represents a generic Java type.
             */
            protected interface GenericTypeToken {

                /**
                 * Represents an empty type path.
                 */
                String EMPTY_TYPE_PATH = "";

                /**
                 * Represents a step to a component type within a type path.
                 */
                char COMPONENT_TYPE_PATH = '[';

                /**
                 * Represents a wildcard type step within a type path.
                 */
                char WILDCARD_TYPE_PATH = '*';

                /**
                 * Represents a (reversed) step to an inner class within a type path.
                 */
                char INNER_CLASS_PATH = '.';

                /**
                 * Represents an index type delimiter within a type path.
                 */
                char INDEXED_TYPE_DELIMITER = ';';

                /**
                 * Transforms this token into a generic type representation.
                 *
                 * @param typePool           The type pool to be used for locating non-generic type descriptions.
                 * @param typeVariableSource The type variable source.
                 * @param typePath           The type path of the resolved generic type.
                 * @param annotationTokens   A mapping of the type's annotation tokens by their type path.
                 * @return A description of the represented generic type.
                 */
                Generic toGenericType(TypePool typePool, TypeVariableSource typeVariableSource, String typePath, Map<String, List<AnnotationToken>> annotationTokens);

                /**
                 * Determines if a generic type tokens represents a primary bound of a type variable. This method must only be invoked on types
                 * that represent a {@link Sort#NON_GENERIC},
                 * {@link Sort#PARAMETERIZED} or {@link Sort#VARIABLE}.
                 *
                 * @param typePool The type pool to use.
                 * @return {@code true} if this token represents a primary bound.
                 */
                boolean isPrimaryBound(TypePool typePool);

                /**
                 * Returns the type path prefix that needs to be appended to the existing type path before any further navigation on the parameterized
                 * type. This method must only be called on type tokens that represent parameterized type
                 *
                 * @return A type path segment that needs to be appended to the base type path before any further navigation on the parameterized type.
                 */
                String getTypePathPrefix();

                /**
                 * Represents a generic type token for a formal type variable.
                 */
                interface OfFormalTypeVariable {

                    /**
                     * Transforms this token into a generic type representation.
                     *
                     * @param typePool                 The type pool to be used for locating non-generic type descriptions.
                     * @param typeVariableSource       The type variable source.
                     * @param annotationTokens         A mapping of the type variables' type annotations.
                     * @param boundaryAnnotationTokens A mapping of the type variables' bounds' type annotation by their bound index.
                     * @return A generic type representation of this formal type variable.
                     */
                    Generic toGenericType(TypePool typePool,
                                          TypeVariableSource typeVariableSource,
                                          Map<String, List<AnnotationToken>> annotationTokens,
                                          Map<Integer, Map<String, List<AnnotationToken>>> boundaryAnnotationTokens);
                }

                /**
                 * A generic type token that represents a primitive type.
                 */
                enum ForPrimitiveType implements GenericTypeToken {

                    /**
                     * The generic type token describing the {@code boolean} type.
                     */
                    BOOLEAN(boolean.class),

                    /**
                     * The generic type token describing the {@code byte} type.
                     */
                    BYTE(byte.class),

                    /**
                     * The generic type token describing the {@code short} type.
                     */
                    SHORT(short.class),

                    /**
                     * The generic type token describing the {@code char} type.
                     */
                    CHAR(char.class),

                    /**
                     * The generic type token describing the {@code int} type.
                     */
                    INTEGER(int.class),

                    /**
                     * The generic type token describing the {@code long} type.
                     */
                    LONG(long.class),

                    /**
                     * The generic type token describing the {@code float} type.
                     */
                    FLOAT(float.class),

                    /**
                     * The generic type token describing the {@code double} type.
                     */
                    DOUBLE(double.class),

                    /**
                     * The generic type token describing the {@code void} type.
                     */
                    VOID(void.class);

                    /**
                     * A description of this primitive type token.
                     */
                    private final TypeDescription typeDescription;

                    /**
                     * Creates a new primitive type token.
                     *
                     * @param type The loaded type representing this primitive.
                     */
                    ForPrimitiveType(Class<?> type) {
                        typeDescription = new ForLoadedType(type);
                    }

                    /**
                     * Resolves a generic type token of a primitive type.
                     *
                     * @param descriptor The descriptor of the primitive type.
                     * @return The corresponding generic type token.
                     */
                    public static GenericTypeToken of(char descriptor) {
                        switch (descriptor) {
                            case 'V':
                                return VOID;
                            case 'Z':
                                return BOOLEAN;
                            case 'B':
                                return BYTE;
                            case 'S':
                                return SHORT;
                            case 'C':
                                return CHAR;
                            case 'I':
                                return INTEGER;
                            case 'J':
                                return LONG;
                            case 'F':
                                return FLOAT;
                            case 'D':
                                return DOUBLE;
                            default:
                                throw new IllegalArgumentException("Not a valid primitive type descriptor: " + descriptor);
                        }
                    }

                    @Override
                    public Generic toGenericType(TypePool typePool,
                                                 TypeVariableSource typeVariableSource,
                                                 String typePath,
                                                 Map<String, List<AnnotationToken>> annotationTokens) {
                        return new LazyPrimitiveType(typePool,
                                typePath,
                                annotationTokens == null
                                        ? Collections.<String, List<AnnotationToken>>emptyMap()
                                        : annotationTokens,
                                typeDescription);
                    }

                    @Override
                    public boolean isPrimaryBound(TypePool typePool) {
                        throw new IllegalStateException("A primitive type cannot be a type variable bound: " + this);
                    }

                    @Override
                    public String getTypePathPrefix() {
                        throw new IllegalStateException("A primitive type cannot be the owner of a nested type: " + this);
                    }

                    /**
                     * A representation of a lazy primitive type.
                     */
                    protected static class LazyPrimitiveType extends Generic.OfNonGenericType {

                        /**
                         * The type pool to use.
                         */
                        private final TypePool typePool;

                        /**
                         * This type's type path.
                         */
                        private final String typePath;

                        /**
                         * This type's type annotation tokens.
                         */
                        private final Map<String, List<AnnotationToken>> annotationTokens;

                        /**
                         * The represented type's description.
                         */
                        private final TypeDescription typeDescription;

                        /**
                         * Creates a new lazy primitive type.
                         *
                         * @param typePool         The type pool to use.
                         * @param typePath         This type's type path.
                         * @param annotationTokens This type's type annotation tokens.
                         * @param typeDescription  The represented type's description.
                         */
                        protected LazyPrimitiveType(TypePool typePool,
                                                    String typePath,
                                                    Map<String, List<AnnotationToken>> annotationTokens,
                                                    TypeDescription typeDescription) {
                            this.typePool = typePool;
                            this.typePath = typePath;
                            this.annotationTokens = annotationTokens;
                            this.typeDescription = typeDescription;
                        }

                        @Override
                        public TypeDescription asErasure() {
                            return typeDescription;
                        }

                        @Override
                        public Generic getOwnerType() {
                            return Generic.UNDEFINED;
                        }

                        @Override
                        public Generic getComponentType() {
                            return Generic.UNDEFINED;
                        }

                        @Override
                        public AnnotationList getDeclaredAnnotations() {
                            return LazyAnnotationDescription.asListOfNullable(typePool, annotationTokens.get(typePath));
                        }
                    }
                }

                /**
                 * A generic type token that represents an unbound wildcard.
                 */
                enum ForUnboundWildcard implements GenericTypeToken {

                    /**
                     * The singleton instance.
                     */
                    INSTANCE;

                    @Override
                    public Generic toGenericType(TypePool typePool,
                                                 TypeVariableSource typeVariableSource,
                                                 String typePath,
                                                 Map<String, List<AnnotationToken>> annotationTokens) {
                        return new LazyUnboundWildcard(typePool,
                                typePath,
                                annotationTokens == null
                                        ? Collections.<String, List<AnnotationToken>>emptyMap()
                                        : annotationTokens);
                    }

                    @Override
                    public boolean isPrimaryBound(TypePool typePool) {
                        throw new IllegalStateException("A wildcard type cannot be a type variable bound: " + this);
                    }

                    @Override
                    public String getTypePathPrefix() {
                        throw new IllegalStateException("An unbound wildcard cannot be the owner of a nested type: " + this);
                    }

                    /**
                     * A generic type representation of a generic unbound wildcard.
                     */
                    protected static class LazyUnboundWildcard extends Generic.OfWildcardType {

                        /**
                         * The type pool to use.
                         */
                        private final TypePool typePool;

                        /**
                         * This type's type path.
                         */
                        private final String typePath;

                        /**
                         * The type's type annotations.
                         */
                        private final Map<String, List<AnnotationToken>> annotationTokens;

                        /**
                         * Creates a new lazy unbound wildcard.
                         *
                         * @param typePool         The type pool to use.
                         * @param typePath         This type's type path.
                         * @param annotationTokens The type's type annotations.
                         */
                        protected LazyUnboundWildcard(TypePool typePool, String typePath, Map<String, List<AnnotationToken>> annotationTokens) {
                            this.typePool = typePool;
                            this.typePath = typePath;
                            this.annotationTokens = annotationTokens;
                        }

                        @Override
                        public TypeList.Generic getUpperBounds() {
                            return new TypeList.Generic.Explicit(Generic.OBJECT);
                        }

                        @Override
                        public TypeList.Generic getLowerBounds() {
                            return new TypeList.Generic.Empty();
                        }

                        @Override
                        public AnnotationList getDeclaredAnnotations() {
                            return LazyAnnotationDescription.asListOfNullable(typePool, annotationTokens.get(typePath));
                        }
                    }
                }

                /**
                 * A resolution of a type's, method's or field's generic types.
                 */
                interface Resolution {

                    /**
                     * Resolves the type variables of the represented element.
                     *
                     * @param typePool              The type pool to be used for locating non-generic type descriptions.
                     * @param typeVariableSource    The type variable source to use for resolving type variables.
                     * @param annotationTokens      A mapping of the type variables' type annotation tokens by their indices.
                     * @param boundAnnotationTokens A mapping of the type variables' bounds' type annotation tokens by their indices
                     *                              and each type variable's index.
                     * @return A list describing the resolved generic types.
                     */
                    TypeList.Generic resolveTypeVariables(TypePool typePool,
                                                          TypeVariableSource typeVariableSource,
                                                          Map<Integer, Map<String, List<AnnotationToken>>> annotationTokens,
                                                          Map<Integer, Map<Integer, Map<String, List<AnnotationToken>>>> boundAnnotationTokens);

                    /**
                     * A resolution of a type's, method's or field's generic types if all of the represented element's are raw.
                     */
                    enum Raw implements ForType, ForMethod, ForField {

                        /**
                         * The singleton instance.
                         */
                        INSTANCE;

                        @Override
                        public Generic resolveFieldType(String fieldTypeDescriptor,
                                                        TypePool typePool,
                                                        Map<String, List<AnnotationToken>> annotationTokens,
                                                        FieldDescription.InDefinedShape definingField) {
                            return RawAnnotatedType.of(typePool, annotationTokens, fieldTypeDescriptor);
                        }

                        @Override
                        public Generic resolveReturnType(String returnTypeDescriptor,
                                                         TypePool typePool,
                                                         Map<String, List<AnnotationToken>> annotationTokens,
                                                         MethodDescription.InDefinedShape definingMethod) {
                            return RawAnnotatedType.of(typePool, annotationTokens, returnTypeDescriptor);
                        }

                        @Override
                        public TypeList.Generic resolveParameterTypes(List<String> parameterTypeDescriptors,
                                                                      TypePool typePool,
                                                                      Map<Integer, Map<String, List<AnnotationToken>>> annotationTokens,
                                                                      MethodDescription.InDefinedShape definingMethod) {
                            return RawAnnotatedType.LazyRawAnnotatedTypeList.of(typePool, annotationTokens, parameterTypeDescriptors);
                        }

                        @Override
                        public TypeList.Generic resolveExceptionTypes(List<String> exceptionTypeDescriptors,
                                                                      TypePool typePool,
                                                                      Map<Integer, Map<String, List<AnnotationToken>>> annotationTokens,
                                                                      MethodDescription.InDefinedShape definingMethod) {
                            return RawAnnotatedType.LazyRawAnnotatedTypeList.of(typePool, annotationTokens, exceptionTypeDescriptors);
                        }

                        @Override
                        public Generic resolveSuperClass(String superClassDescriptor,
                                                         TypePool typePool,
                                                         Map<String, List<AnnotationToken>> annotationTokens,
                                                         TypeDescription definingType) {
                            return RawAnnotatedType.of(typePool, annotationTokens, superClassDescriptor);
                        }

                        @Override
                        public TypeList.Generic resolveInterfaceTypes(List<String> interfaceTypeDescriptors,
                                                                      TypePool typePool,
                                                                      Map<Integer, Map<String, List<AnnotationToken>>> annotationTokens,
                                                                      TypeDescription definingType) {
                            return RawAnnotatedType.LazyRawAnnotatedTypeList.of(typePool, annotationTokens, interfaceTypeDescriptors);
                        }

                        @Override
                        public TypeList.Generic resolveTypeVariables(TypePool typePool,
                                                                     TypeVariableSource typeVariableSource,
                                                                     Map<Integer, Map<String, List<AnnotationToken>>> annotationTokens,
                                                                     Map<Integer, Map<Integer, Map<String, List<AnnotationToken>>>> boundAnnotationTokens) {
                            return new TypeList.Generic.Empty();
                        }

                        /**
                         * Represents a non-generic type that defines type annotations.
                         */
                        protected static class RawAnnotatedType extends Generic.OfNonGenericType {

                            /**
                             * The type pool to use.
                             */
                            private final TypePool typePool;

                            /**
                             * The type's type path.
                             */
                            private final String typePath;

                            /**
                             * A mapping of this type's type annotations.
                             */
                            private final Map<String, List<AnnotationToken>> annotationTokens;

                            /**
                             * The represented non-generic type.
                             */
                            private final TypeDescription typeDescription;

                            /**
                             * Creates a new raw annotated type.
                             *
                             * @param typePool         The type pool to use.
                             * @param typePath         The type's type path.
                             * @param annotationTokens A mapping of this type's type annotations.
                             * @param typeDescription  The represented non-generic type.
                             */
                            protected RawAnnotatedType(TypePool typePool,
                                                       String typePath,
                                                       Map<String, List<AnnotationToken>> annotationTokens,
                                                       TypeDescription typeDescription) {
                                this.typePool = typePool;
                                this.typePath = typePath;
                                this.annotationTokens = annotationTokens;
                                this.typeDescription = typeDescription;
                            }

                            /**
                             * Creates a new raw annotated type.
                             *
                             * @param typePool         The type pool to use.
                             * @param annotationTokens A mapping of this type's type annotations.
                             * @param descriptor       The descriptor of the represented non-generic type.
                             * @return An annotated non-generic type.
                             */
                            protected static Generic of(TypePool typePool, Map<String, List<AnnotationToken>> annotationTokens, String descriptor) {
                                return new RawAnnotatedType(typePool,
                                        EMPTY_TYPE_PATH,
                                        annotationTokens == null
                                                ? Collections.<String, List<AnnotationToken>>emptyMap()
                                                : annotationTokens,
                                        TokenizedGenericType.toErasure(typePool, descriptor));
                            }

                            @Override
                            public TypeDescription asErasure() {
                                return typeDescription;
                            }

                            @Override
                            public Generic getOwnerType() {
                                TypeDescription declaringType = typeDescription.getDeclaringType();
                                return declaringType == null
                                        ? Generic.UNDEFINED
                                        : new RawAnnotatedType(typePool, typePath, annotationTokens, declaringType);
                            }

                            @Override
                            public Generic getComponentType() {
                                TypeDescription componentType = typeDescription.getComponentType();
                                return componentType == null
                                        ? Generic.UNDEFINED
                                        : new RawAnnotatedType(typePool, typePath + COMPONENT_TYPE_PATH, annotationTokens, componentType);
                            }

                            @Override
                            public AnnotationList getDeclaredAnnotations() {
                                StringBuilder typePath = new StringBuilder(this.typePath);
                                for (int index = 0; index < typeDescription.getInnerClassCount(); index++) {
                                    typePath = typePath.append(INNER_CLASS_PATH);
                                }
                                return LazyAnnotationDescription.asListOfNullable(typePool, annotationTokens.get(typePath.toString()));
                            }

                            /**
                             * A generic type list representing raw types.
                             */
                            protected static class LazyRawAnnotatedTypeList extends TypeList.Generic.AbstractBase {

                                /**
                                 * The type pool to use for locating types.
                                 */
                                private final TypePool typePool;

                                /**
                                 * A mapping of the represented types' type annotation tokens by their indices.
                                 */
                                private final Map<Integer, Map<String, List<AnnotationToken>>> annotationTokens;

                                /**
                                 * A list of type descriptors that this list represents.
                                 */
                                private final List<String> descriptors;

                                /**
                                 * Creates a generic type list only representing raw types.
                                 *
                                 * @param typePool         The type pool to use for locating types.
                                 * @param annotationTokens A mapping of the represented types' type annotation tokens by their indices.
                                 * @param descriptors      A list of type descriptors that this list represents.
                                 */
                                protected LazyRawAnnotatedTypeList(TypePool typePool,
                                                                   Map<Integer, Map<String, List<AnnotationToken>>> annotationTokens,
                                                                   List<String> descriptors) {
                                    this.typePool = typePool;
                                    this.annotationTokens = annotationTokens;
                                    this.descriptors = descriptors;
                                }

                                /**
                                 * Creates generic type list only representing raw types.
                                 *
                                 * @param typePool         The type pool to use for locating types.
                                 * @param annotationTokens A mapping of the represented types' type annotation tokens by their indices or
                                 *                         {@code null} if no type annotations are defined for any type.
                                 * @param descriptors      A list of type descriptors that this list represents.
                                 * @return A generic type list representing the raw types this list represents.
                                 */
                                protected static TypeList.Generic of(TypePool typePool,
                                                                     Map<Integer, Map<String, List<AnnotationToken>>> annotationTokens,
                                                                     List<String> descriptors) {
                                    return new LazyRawAnnotatedTypeList(typePool,
                                            annotationTokens == null
                                                    ? Collections.<Integer, Map<String, List<AnnotationToken>>>emptyMap()
                                                    : annotationTokens,
                                            descriptors);
                                }

                                @Override
                                public Generic get(int index) {
                                    return RawAnnotatedType.of(typePool, annotationTokens.get(index), descriptors.get(index));
                                }

                                @Override
                                public int size() {
                                    return descriptors.size();
                                }

                                @Override
                                public TypeList asErasures() {
                                    return new LazyTypeList(typePool, descriptors);
                                }

                                @Override
                                public TypeList.Generic asRawTypes() {
                                    return this;
                                }

                                @Override
                                public int getStackSize() {
                                    int stackSize = 0;
                                    for (String descriptor : descriptors) {
                                        stackSize += Type.getType(descriptor).getSize();
                                    }
                                    return stackSize;
                                }
                            }
                        }
                    }

                    /**
                     * A resolution of a type's, method's or field's generic types if its generic signature is malformed.
                     */
                    enum Malformed implements ForType, ForMethod, ForField {

                        /**
                         * The singleton instance.
                         */
                        INSTANCE;

                        @Override
                        public Generic resolveFieldType(String fieldTypeDescriptor,
                                                        TypePool typePool,
                                                        Map<String, List<AnnotationToken>> annotationTokens,
                                                        FieldDescription.InDefinedShape definingField) {
                            return new TokenizedGenericType.Malformed(typePool, fieldTypeDescriptor);
                        }

                        @Override
                        public Generic resolveReturnType(String returnTypeDescriptor,
                                                         TypePool typePool,
                                                         Map<String, List<AnnotationToken>> annotationTokens,
                                                         MethodDescription.InDefinedShape definingMethod) {
                            return new TokenizedGenericType.Malformed(typePool, returnTypeDescriptor);
                        }

                        @Override
                        public TypeList.Generic resolveParameterTypes(List<String> parameterTypeDescriptors,
                                                                      TypePool typePool,
                                                                      Map<Integer, Map<String, List<AnnotationToken>>> annotationTokens,
                                                                      MethodDescription.InDefinedShape definingMethod) {
                            return new TokenizedGenericType.Malformed.TokenList(typePool, parameterTypeDescriptors);
                        }

                        @Override
                        public TypeList.Generic resolveExceptionTypes(List<String> exceptionTypeDescriptors,
                                                                      TypePool typePool,
                                                                      Map<Integer, Map<String, List<AnnotationToken>>> annotationTokens,
                                                                      MethodDescription.InDefinedShape definingMethod) {
                            return new TokenizedGenericType.Malformed.TokenList(typePool, exceptionTypeDescriptors);
                        }

                        @Override
                        public Generic resolveSuperClass(String superClassDescriptor,
                                                         TypePool typePool,
                                                         Map<String, List<AnnotationToken>> annotationTokens,
                                                         TypeDescription definingType) {
                            return new TokenizedGenericType.Malformed(typePool, superClassDescriptor);
                        }

                        @Override
                        public TypeList.Generic resolveInterfaceTypes(List<String> interfaceTypeDescriptors,
                                                                      TypePool typePool,
                                                                      Map<Integer, Map<String, List<AnnotationToken>>> annotationTokens,
                                                                      TypeDescription definingType) {
                            return new TokenizedGenericType.Malformed.TokenList(typePool, interfaceTypeDescriptors);
                        }

                        @Override
                        public TypeList.Generic resolveTypeVariables(TypePool typePool,
                                                                     TypeVariableSource typeVariableSource,
                                                                     Map<Integer, Map<String, List<AnnotationToken>>> annotationTokens,
                                                                     Map<Integer, Map<Integer, Map<String, List<AnnotationToken>>>> boundAnnotationTokens) {
                            throw new GenericSignatureFormatError();
                        }
                    }

                    /**
                     * A resolution of the generic types of a {@link TypeDescription}.
                     */
                    interface ForType extends Resolution {

                        /**
                         * Resolves the generic super type of the represented type.
                         *
                         * @param superClassDescriptor The descriptor of the raw super type.
                         * @param typePool             The type pool to be used for locating non-generic type descriptions.
                         * @param annotationTokens     A mapping of the super type's type annotation tokens.
                         * @param definingType         The type that defines this super type.
                         * @return A description of this type's generic super type.
                         */
                        Generic resolveSuperClass(String superClassDescriptor,
                                                  TypePool typePool,
                                                  Map<String, List<AnnotationToken>> annotationTokens,
                                                  TypeDescription definingType);

                        /**
                         * Resolves the generic interface types of the represented type.
                         *
                         * @param interfaceTypeDescriptors The descriptor of the raw interface types.
                         * @param typePool                 The type pool to be used for locating non-generic type descriptions.
                         * @param annotationTokens         A mapping of the interface types' type annotation tokens by their indices.
                         * @param definingType             The type that defines these interface type.
                         * @return A description of this type's generic interface types.
                         */
                        TypeList.Generic resolveInterfaceTypes(List<String> interfaceTypeDescriptors,
                                                               TypePool typePool,
                                                               Map<Integer, Map<String, List<AnnotationToken>>> annotationTokens,
                                                               TypeDescription definingType);

                        /**
                         * An implementation of a tokenized resolution of generic types of a {@link TypeDescription}.
                         */
                        @HashCodeAndEqualsPlugin.Enhance
                        class Tokenized implements ForType {

                            /**
                             * The super type's generic type token.
                             */
                            private final GenericTypeToken superClassToken;

                            /**
                             * The interface type's generic type tokens.
                             */
                            private final List<GenericTypeToken> interfaceTypeTokens;

                            /**
                             * The type variables generic type tokens.
                             */
                            private final List<OfFormalTypeVariable> typeVariableTokens;

                            /**
                             * Creates a new tokenized resolution of a {@link TypeDescription}'s generic signatures.
                             *
                             * @param superClassToken     The super class's generic type token.
                             * @param interfaceTypeTokens The interface type's generic type tokens.
                             * @param typeVariableTokens  The type variables generic type tokens.
                             */
                            public Tokenized(GenericTypeToken superClassToken,
                                             List<GenericTypeToken> interfaceTypeTokens,
                                             List<OfFormalTypeVariable> typeVariableTokens) {
                                this.superClassToken = superClassToken;
                                this.interfaceTypeTokens = interfaceTypeTokens;
                                this.typeVariableTokens = typeVariableTokens;
                            }

                            @Override
                            public Generic resolveSuperClass(String superClassDescriptor,
                                                             TypePool typePool,
                                                             Map<String, List<AnnotationToken>> annotationTokens,
                                                             TypeDescription definingType) {
                                return TokenizedGenericType.of(typePool, superClassToken, superClassDescriptor, annotationTokens, definingType);
                            }

                            @Override
                            public TypeList.Generic resolveInterfaceTypes(List<String> interfaceTypeDescriptors,
                                                                          TypePool typePool,
                                                                          Map<Integer, Map<String, List<AnnotationToken>>> annotationTokens,
                                                                          TypeDescription definingType) {
                                return new TokenizedGenericType.TokenList(typePool, interfaceTypeTokens, annotationTokens, interfaceTypeDescriptors, definingType);
                            }

                            @Override
                            public TypeList.Generic resolveTypeVariables(TypePool typePool,
                                                                         TypeVariableSource typeVariableSource,
                                                                         Map<Integer, Map<String, List<AnnotationToken>>> annotationTokens,
                                                                         Map<Integer, Map<Integer, Map<String, List<AnnotationToken>>>> boundAnnotationTokens) {
                                return new TokenizedGenericType.TypeVariableList(typePool, typeVariableTokens, typeVariableSource, annotationTokens, boundAnnotationTokens);
                            }
                        }
                    }

                    /**
                     * A resolution of the generic types of a {@link MethodDescription}.
                     */
                    interface ForMethod extends Resolution {

                        /**
                         * Resolves the return type of the represented method.
                         *
                         * @param returnTypeDescriptor The descriptor of the raw return type.
                         * @param typePool             The type pool to be used for locating non-generic type descriptions.
                         * @param annotationTokens     A mapping of the return type's type annotation tokens.
                         * @param definingMethod       The method that defines this return type.
                         * @return A description of this type's generic return type.
                         */
                        Generic resolveReturnType(String returnTypeDescriptor,
                                                  TypePool typePool,
                                                  Map<String, List<AnnotationToken>> annotationTokens,
                                                  MethodDescription.InDefinedShape definingMethod);

                        /**
                         * Resolves the generic parameter types of the represented method.
                         *
                         * @param parameterTypeDescriptors The descriptor of the raw parameter types.
                         * @param typePool                 The type pool to be used for locating non-generic type descriptions.
                         * @param annotationTokens         A mapping of the parameter types' type annotation tokens by their indices.
                         * @param definingMethod           The method that defines these parameter types.
                         * @return A description of this type's generic interface types.
                         */
                        TypeList.Generic resolveParameterTypes(List<String> parameterTypeDescriptors,
                                                               TypePool typePool,
                                                               Map<Integer, Map<String, List<AnnotationToken>>> annotationTokens,
                                                               MethodDescription.InDefinedShape definingMethod);

                        /**
                         * Resolves the generic parameter types of the represented method.
                         *
                         * @param exceptionTypeDescriptors The descriptor of the raw exception types.
                         * @param typePool                 The type pool to be used for locating non-generic type descriptions.
                         * @param annotationTokens         A mapping of the exception types' type annotation tokens by their indices.
                         * @param definingMethod           The method that defines these exception types.
                         * @return A description of this type's generic interface types.
                         */
                        TypeList.Generic resolveExceptionTypes(List<String> exceptionTypeDescriptors,
                                                               TypePool typePool,
                                                               Map<Integer, Map<String, List<AnnotationToken>>> annotationTokens,
                                                               MethodDescription.InDefinedShape definingMethod);

                        /**
                         * An implementation of a tokenized resolution of generic types of a {@link MethodDescription}.
                         */
                        @HashCodeAndEqualsPlugin.Enhance
                        class Tokenized implements ForMethod {

                            /**
                             * A token describing the represented method's return type.
                             */
                            private final GenericTypeToken returnTypeToken;

                            /**
                             * A token describing the represented method's parameter types.
                             */
                            private final List<GenericTypeToken> parameterTypeTokens;

                            /**
                             * A token describing the represented method's exception types.
                             */
                            private final List<GenericTypeToken> exceptionTypeTokens;

                            /**
                             * A token describing the represented method's type variables.
                             */
                            private final List<OfFormalTypeVariable> typeVariableTokens;

                            /**
                             * Creates a new tokenized resolution of a {@link MethodDescription}'s generic signatures.
                             *
                             * @param returnTypeToken     A token describing the represented method's return type.
                             * @param parameterTypeTokens A token describing the represented method's parameter types.
                             * @param exceptionTypeTokens A token describing the represented method's exception types.
                             * @param typeVariableTokens  A token describing the represented method's type variables.
                             */
                            public Tokenized(GenericTypeToken returnTypeToken,
                                             List<GenericTypeToken> parameterTypeTokens,
                                             List<GenericTypeToken> exceptionTypeTokens,
                                             List<OfFormalTypeVariable> typeVariableTokens) {
                                this.returnTypeToken = returnTypeToken;
                                this.parameterTypeTokens = parameterTypeTokens;
                                this.exceptionTypeTokens = exceptionTypeTokens;
                                this.typeVariableTokens = typeVariableTokens;
                            }

                            @Override
                            public Generic resolveReturnType(String returnTypeDescriptor,
                                                             TypePool typePool,
                                                             Map<String, List<AnnotationToken>> annotationTokens,
                                                             MethodDescription.InDefinedShape definingMethod) {
                                return TokenizedGenericType.of(typePool, returnTypeToken, returnTypeDescriptor, annotationTokens, definingMethod);
                            }

                            @Override
                            public TypeList.Generic resolveParameterTypes(List<String> parameterTypeDescriptors,
                                                                          TypePool typePool,
                                                                          Map<Integer, Map<String, List<AnnotationToken>>> annotationTokens,
                                                                          MethodDescription.InDefinedShape definingMethod) {
                                return new TokenizedGenericType.TokenList(typePool, parameterTypeTokens, annotationTokens, parameterTypeDescriptors, definingMethod);
                            }

                            @Override
                            public TypeList.Generic resolveExceptionTypes(List<String> exceptionTypeDescriptors,
                                                                          TypePool typePool,
                                                                          Map<Integer, Map<String, List<AnnotationToken>>> annotationTokens,
                                                                          MethodDescription.InDefinedShape definingMethod) {
                                // Generic signatures of methods are optional.
                                return exceptionTypeTokens.isEmpty()
                                        ? Raw.INSTANCE.resolveExceptionTypes(exceptionTypeDescriptors, typePool, annotationTokens, definingMethod)
                                        : new TokenizedGenericType.TokenList(typePool, exceptionTypeTokens, annotationTokens, exceptionTypeDescriptors, definingMethod);
                            }

                            @Override
                            public TypeList.Generic resolveTypeVariables(TypePool typePool,
                                                                         TypeVariableSource typeVariableSource,
                                                                         Map<Integer, Map<String, List<AnnotationToken>>> annotationTokens,
                                                                         Map<Integer, Map<Integer, Map<String, List<AnnotationToken>>>> boundAnnotationTokens) {
                                return new TokenizedGenericType.TypeVariableList(typePool, typeVariableTokens, typeVariableSource, annotationTokens, boundAnnotationTokens);
                            }
                        }
                    }

                    /**
                     * A resolution of the generic types of a {@link FieldDescription}.
                     */
                    interface ForField {

                        /**
                         * Resolves the field type of the represented field.
                         *
                         * @param fieldTypeDescriptor The descriptor of the raw field type.
                         * @param annotationTokens    A mapping of the represented types' type annotation tokens.
                         * @param typePool            The type pool to be used for locating non-generic type descriptions.
                         * @param definingField       The field that defines this type.   @return A description of this field's type.
                         * @return A generic type representation of the field's type.
                         */
                        Generic resolveFieldType(String fieldTypeDescriptor,
                                                 TypePool typePool,
                                                 Map<String, List<AnnotationToken>> annotationTokens,
                                                 FieldDescription.InDefinedShape definingField);

                        /**
                         * An implementation of a tokenized resolution of the generic type of a {@link FieldDescription}.
                         */
                        @HashCodeAndEqualsPlugin.Enhance
                        class Tokenized implements ForField {

                            /**
                             * The token of the represented field's type.
                             */
                            private final GenericTypeToken fieldTypeToken;

                            /**
                             * Creates a new tokenized resolution of a {@link FieldDescription}'s type.
                             *
                             * @param fieldTypeToken The token of the represented field's type.
                             */
                            public Tokenized(GenericTypeToken fieldTypeToken) {
                                this.fieldTypeToken = fieldTypeToken;
                            }

                            @Override
                            public Generic resolveFieldType(String fieldTypeDescriptor,
                                                            TypePool typePool,
                                                            Map<String, List<AnnotationToken>> annotationTokens,
                                                            FieldDescription.InDefinedShape definingField) {
                                return TokenizedGenericType.of(typePool, fieldTypeToken, fieldTypeDescriptor, annotationTokens, definingField.getDeclaringType());
                            }
                        }
                    }
                }

                /**
                 * A generic type token that represents a non-generic type.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                class ForRawType implements GenericTypeToken {

                    /**
                     * The name of the represented type.
                     */
                    private final String name;

                    /**
                     * Creates a new type token that represents a non-generic type.
                     *
                     * @param name The name of the represented type.
                     */
                    protected ForRawType(String name) {
                        this.name = name;
                    }

                    @Override
                    public Generic toGenericType(TypePool typePool,
                                                 TypeVariableSource typeVariableSource,
                                                 String typePath,
                                                 Map<String, List<AnnotationToken>> annotationTokens) {
                        return new Resolution.Raw.RawAnnotatedType(typePool,
                                typePath,
                                annotationTokens == null
                                        ? Collections.<String, List<AnnotationToken>>emptyMap()
                                        : annotationTokens,
                                typePool.describe(name).resolve());
                    }

                    @Override
                    public boolean isPrimaryBound(TypePool typePool) {
                        return !typePool.describe(name).resolve().isInterface();
                    }

                    @Override
                    public String getTypePathPrefix() {
                        throw new IllegalStateException("A non-generic type cannot be the owner of a nested type: " + this);
                    }
                }

                /**
                 * A generic type token that represents a type variable.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                class ForTypeVariable implements GenericTypeToken {

                    /**
                     * This type variable's nominal symbol.
                     */
                    private final String symbol;

                    /**
                     * Creates a generic type token that represents a type variable.
                     *
                     * @param symbol This type variable's nominal symbol.
                     */
                    protected ForTypeVariable(String symbol) {
                        this.symbol = symbol;
                    }

                    @Override
                    public Generic toGenericType(TypePool typePool, TypeVariableSource typeVariableSource, String typePath, Map<String, List<AnnotationToken>> annotationTokens) {
                        Generic typeVariable = typeVariableSource.findVariable(symbol);
                        return typeVariable == null
                                ? new UnresolvedTypeVariable(typeVariableSource, typePool, symbol, annotationTokens.get(typePath))
                                : new AnnotatedTypeVariable(typePool, annotationTokens.get(typePath), typeVariable);
                    }

                    @Override
                    public boolean isPrimaryBound(TypePool typePool) {
                        return true;
                    }

                    @Override
                    public String getTypePathPrefix() {
                        throw new IllegalStateException("A type variable cannot be the owner of a nested type: " + this);
                    }

                    /**
                     * An annotated representation of a formal type variable.
                     */
                    protected static class AnnotatedTypeVariable extends Generic.OfTypeVariable {

                        /**
                         * The type pool to use.
                         */
                        private final TypePool typePool;

                        /**
                         * The represented annotation tokens.
                         */
                        private final List<AnnotationToken> annotationTokens;

                        /**
                         * The represented type variable.
                         */
                        private final Generic typeVariable;

                        /**
                         * Creates a new annotated type variable.
                         *
                         * @param typePool         The type pool to use.
                         * @param annotationTokens The represented annotation tokens.
                         * @param typeVariable     The represented type variable.
                         */
                        protected AnnotatedTypeVariable(TypePool typePool, List<AnnotationToken> annotationTokens, Generic typeVariable) {
                            this.typePool = typePool;
                            this.annotationTokens = annotationTokens;
                            this.typeVariable = typeVariable;
                        }

                        @Override
                        public TypeList.Generic getUpperBounds() {
                            return typeVariable.getUpperBounds();
                        }

                        @Override
                        public TypeVariableSource getTypeVariableSource() {
                            return typeVariable.getTypeVariableSource();
                        }

                        @Override
                        public String getSymbol() {
                            return typeVariable.getSymbol();
                        }

                        @Override
                        public AnnotationList getDeclaredAnnotations() {
                            return LazyAnnotationDescription.asListOfNullable(typePool, annotationTokens);
                        }
                    }

                    /**
                     * Represents a type variable that a type references but that does not exist. Such type variables are only emitted by wrongful
                     * compilation either due to the isolated recompilation of outer classes or due to bugs in compilers.
                     */
                    protected static class UnresolvedTypeVariable extends Generic.OfTypeVariable {

                        /**
                         * The undeclared type variable's source.
                         */
                        private final TypeVariableSource typeVariableSource;

                        /**
                         * The type pool to use.
                         */
                        private final TypePool typePool;

                        /**
                         * The type variable's symbol.
                         */
                        private final String symbol;

                        /**
                         * The type variable's annotation tokens.
                         */
                        private final List<AnnotationToken> annotationTokens;

                        /**
                         * Creates an unresolved type variable.
                         *
                         * @param typeVariableSource The undeclared type variable's source.
                         * @param typePool           The type pool to use.
                         * @param symbol             The type variable's symbol.
                         * @param annotationTokens   The type variable's annotation tokens.
                         */
                        protected UnresolvedTypeVariable(TypeVariableSource typeVariableSource,
                                                         TypePool typePool,
                                                         String symbol,
                                                         List<AnnotationToken> annotationTokens) {
                            this.typeVariableSource = typeVariableSource;
                            this.typePool = typePool;
                            this.symbol = symbol;
                            this.annotationTokens = annotationTokens;
                        }

                        @Override
                        public TypeList.Generic getUpperBounds() {
                            throw new IllegalStateException("Cannot resolve bounds of unresolved type variable " + this + " by " + typeVariableSource);
                        }

                        @Override
                        public TypeVariableSource getTypeVariableSource() {
                            return typeVariableSource;
                        }

                        @Override
                        public String getSymbol() {
                            return symbol;
                        }

                        @Override
                        public AnnotationList getDeclaredAnnotations() {
                            return LazyAnnotationDescription.asListOfNullable(typePool, annotationTokens);
                        }
                    }

                    /**
                     * A generic type token that represent a formal type variable, i.e. a type variable including its upper bounds.
                     */
                    @HashCodeAndEqualsPlugin.Enhance
                    protected static class Formal implements GenericTypeToken.OfFormalTypeVariable {

                        /**
                         * This type variable's nominal symbol.
                         */
                        private final String symbol;

                        /**
                         * A list of tokens that represent this type variable's upper bounds.
                         */
                        private final List<GenericTypeToken> boundTypeTokens;

                        /**
                         * Creates generic type token that represent a formal type variable.
                         *
                         * @param symbol          This type variable's nominal symbol.
                         * @param boundTypeTokens A list of tokens that represent this type variable's upper bounds.
                         */
                        protected Formal(String symbol, List<GenericTypeToken> boundTypeTokens) {
                            this.symbol = symbol;
                            this.boundTypeTokens = boundTypeTokens;
                        }

                        @Override
                        public Generic toGenericType(TypePool typePool,
                                                     TypeVariableSource typeVariableSource,
                                                     Map<String, List<AnnotationToken>> annotationTokens,
                                                     Map<Integer, Map<String, List<AnnotationToken>>> boundaryAnnotationTokens) {
                            return new LazyTypeVariable(typePool,
                                    typeVariableSource,
                                    annotationTokens == null
                                            ? Collections.<String, List<AnnotationToken>>emptyMap()
                                            : annotationTokens,
                                    boundaryAnnotationTokens == null
                                            ? Collections.<Integer, Map<String, List<AnnotationToken>>>emptyMap()
                                            : boundaryAnnotationTokens,
                                    symbol,
                                    boundTypeTokens);
                        }

                        /**
                         * A type description that represents a type variable with bounds that are resolved lazily.
                         */
                        protected static class LazyTypeVariable extends Generic.OfTypeVariable {

                            /**
                             * The type pool to use for locating type descriptions.
                             */
                            private final TypePool typePool;

                            /**
                             * The type variable source to use for locating type variables.
                             */
                            private final TypeVariableSource typeVariableSource;

                            /**
                             * The type variable's type annotation tokens.
                             */
                            private final Map<String, List<AnnotationToken>> annotationTokens;

                            /**
                             * A mapping of the type variable bounds' type annotation tokens by their indices.
                             */
                            private final Map<Integer, Map<String, List<AnnotationToken>>> boundaryAnnotationTokens;

                            /**
                             * The type variable's symbol.
                             */
                            private final String symbol;

                            /**
                             * Tokenized representations of the type variables bound types.
                             */
                            private final List<GenericTypeToken> boundTypeTokens;

                            /**
                             * Creates a lazy type description of a type variables.
                             *
                             * @param typePool                 The type pool to use for locating type descriptions.
                             * @param typeVariableSource       The type variable source to use for locating type variables.
                             * @param annotationTokens         The type variable's type annotation tokens.
                             * @param boundaryAnnotationTokens A mapping of the type variable bounds' type annotation tokens by their indices.
                             * @param symbol                   The type variable's symbol.
                             * @param boundTypeTokens          Tokenized representations of the type variables bound types.
                             */
                            protected LazyTypeVariable(TypePool typePool,
                                                       TypeVariableSource typeVariableSource,
                                                       Map<String, List<AnnotationToken>> annotationTokens,
                                                       Map<Integer, Map<String, List<AnnotationToken>>> boundaryAnnotationTokens,
                                                       String symbol,
                                                       List<GenericTypeToken> boundTypeTokens) {
                                this.typePool = typePool;
                                this.typeVariableSource = typeVariableSource;
                                this.annotationTokens = annotationTokens;
                                this.boundaryAnnotationTokens = boundaryAnnotationTokens;
                                this.symbol = symbol;
                                this.boundTypeTokens = boundTypeTokens;
                            }

                            @Override
                            public TypeList.Generic getUpperBounds() {
                                return new LazyBoundTokenList(typePool, typeVariableSource, boundaryAnnotationTokens, boundTypeTokens);
                            }

                            @Override
                            public TypeVariableSource getTypeVariableSource() {
                                return typeVariableSource;
                            }

                            @Override
                            public String getSymbol() {
                                return symbol;
                            }

                            @Override
                            public AnnotationList getDeclaredAnnotations() {
                                return LazyAnnotationDescription.asListOfNullable(typePool, annotationTokens.get(EMPTY_TYPE_PATH));
                            }

                            /**
                             * A list representing a formal type variable's bounds.
                             */
                            protected static class LazyBoundTokenList extends TypeList.Generic.AbstractBase {

                                /**
                                 * The type pool to use.
                                 */
                                private final TypePool typePool;

                                /**
                                 * The type variable source for locating type variables.
                                 */
                                private final TypeVariableSource typeVariableSource;

                                /**
                                 * A mapping of the bound type's type annotations by their bound index.
                                 */
                                private final Map<Integer, Map<String, List<AnnotationToken>>> annotationTokens;

                                /**
                                 * The bound types in their tokenized form.
                                 */
                                private final List<GenericTypeToken> boundTypeTokens;

                                /**
                                 * Creates a new lazy bound token list for a type variable.
                                 *
                                 * @param typePool           The type pool to use.
                                 * @param typeVariableSource The type variable source for locating type variables.
                                 * @param annotationTokens   A mapping of the bound type's type annotations by their bound index.
                                 * @param boundTypeTokens    The bound types in their tokenized form.
                                 */
                                protected LazyBoundTokenList(TypePool typePool,
                                                             TypeVariableSource typeVariableSource,
                                                             Map<Integer, Map<String, List<AnnotationToken>>> annotationTokens,
                                                             List<GenericTypeToken> boundTypeTokens) {
                                    this.typePool = typePool;
                                    this.typeVariableSource = typeVariableSource;
                                    this.annotationTokens = annotationTokens;
                                    this.boundTypeTokens = boundTypeTokens;
                                }

                                @Override
                                public Generic get(int index) {
                                    // Avoid resolution of interface bound type unless a type annotation can be possibly resolved.
                                    Map<String, List<AnnotationToken>> annotationTokens = !this.annotationTokens.containsKey(index) && !this.annotationTokens.containsKey(index + 1)
                                            ? Collections.<String, List<AnnotationToken>>emptyMap()
                                            : this.annotationTokens.get(index + (boundTypeTokens.get(0).isPrimaryBound(typePool) ? 0 : 1));
                                    return boundTypeTokens.get(index).toGenericType(typePool,
                                            typeVariableSource,
                                            EMPTY_TYPE_PATH,
                                            annotationTokens == null
                                                    ? Collections.<String, List<AnnotationToken>>emptyMap()
                                                    : annotationTokens);
                                }

                                @Override
                                public int size() {
                                    return boundTypeTokens.size();
                                }
                            }
                        }
                    }
                }

                /**
                 * A generic type token that represents a generic array.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                class ForGenericArray implements GenericTypeToken {

                    /**
                     * The array's component type.
                     */
                    private final GenericTypeToken componentTypeToken;

                    /**
                     * Creates a generic type token that represents a generic array.
                     *
                     * @param componentTypeToken The array's component type.
                     */
                    protected ForGenericArray(GenericTypeToken componentTypeToken) {
                        this.componentTypeToken = componentTypeToken;
                    }

                    @Override
                    public Generic toGenericType(TypePool typePool, TypeVariableSource typeVariableSource, String typePath, Map<String, List<AnnotationToken>> annotationTokens) {
                        return new LazyGenericArray(typePool, typeVariableSource, typePath, annotationTokens, componentTypeToken);
                    }

                    @Override
                    public boolean isPrimaryBound(TypePool typePool) {
                        throw new IllegalStateException("A generic array type cannot be a type variable bound: " + this);
                    }

                    @Override
                    public String getTypePathPrefix() {
                        throw new IllegalStateException("A generic array type cannot be the owner of a nested type: " + this);
                    }

                    /**
                     * A generic type representation of a generic array.
                     */
                    protected static class LazyGenericArray extends Generic.OfGenericArray {

                        /**
                         * The type pool to use.
                         */
                        private final TypePool typePool;

                        /**
                         * The type variable source for locating type variables.
                         */
                        private final TypeVariableSource typeVariableSource;

                        /**
                         * This type's type path.
                         */
                        private final String typePath;

                        /**
                         * This type's type annotations.
                         */
                        private final Map<String, List<AnnotationToken>> annotationTokens;

                        /**
                         * A tokenized representation of this generic arrays's component type.
                         */
                        private final GenericTypeToken componentTypeToken;

                        /**
                         * Creates a new lazy generic array.
                         *
                         * @param typePool           The type pool to use.
                         * @param typeVariableSource The type variable source for locating type variables.
                         * @param typePath           This type's type path.
                         * @param annotationTokens   This type's type annotations.
                         * @param componentTypeToken A tokenized representation of this generic arrays's component type.
                         */
                        protected LazyGenericArray(TypePool typePool,
                                                   TypeVariableSource typeVariableSource,
                                                   String typePath,
                                                   Map<String, List<AnnotationToken>> annotationTokens,
                                                   GenericTypeToken componentTypeToken) {
                            this.typePool = typePool;
                            this.typeVariableSource = typeVariableSource;
                            this.typePath = typePath;
                            this.annotationTokens = annotationTokens;
                            this.componentTypeToken = componentTypeToken;
                        }

                        @Override
                        public Generic getComponentType() {
                            return componentTypeToken.toGenericType(typePool, typeVariableSource, typePath + COMPONENT_TYPE_PATH, annotationTokens);
                        }

                        @Override
                        public AnnotationList getDeclaredAnnotations() {
                            return LazyAnnotationDescription.asListOfNullable(typePool, annotationTokens.get(typePath));
                        }
                    }
                }

                /**
                 * A generic type token for a wildcard that is bound below.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                class ForLowerBoundWildcard implements GenericTypeToken {

                    /**
                     * A token that represents the wildcard's lower bound.
                     */
                    private final GenericTypeToken boundTypeToken;

                    /**
                     * Creates a generic type token for a wildcard that is bound below.
                     *
                     * @param boundTypeToken A token that represents the wildcard's lower bound.
                     */
                    protected ForLowerBoundWildcard(GenericTypeToken boundTypeToken) {
                        this.boundTypeToken = boundTypeToken;
                    }

                    @Override
                    public Generic toGenericType(TypePool typePool, TypeVariableSource typeVariableSource, String typePath, Map<String, List<AnnotationToken>> annotationTokens) {
                        return new LazyLowerBoundWildcard(typePool, typeVariableSource, typePath, annotationTokens, boundTypeToken);
                    }

                    @Override
                    public boolean isPrimaryBound(TypePool typePool) {
                        throw new IllegalStateException("A wildcard type cannot be a type variable bound: " + this);
                    }

                    @Override
                    public String getTypePathPrefix() {
                        throw new IllegalStateException("A lower bound wildcard cannot be the owner of a nested type: " + this);
                    }

                    /**
                     * A generic type representation of a lower bound wildcard.
                     */
                    protected static class LazyLowerBoundWildcard extends Generic.OfWildcardType {

                        /**
                         * The type pool to use.
                         */
                        private final TypePool typePool;

                        /**
                         * The type variable source for locating type variables.
                         */
                        private final TypeVariableSource typeVariableSource;

                        /**
                         * This type's type path.
                         */
                        private final String typePath;

                        /**
                         * This type's type annotations.
                         */
                        private final Map<String, List<AnnotationToken>> annotationTokens;

                        /**
                         * A tokenized representation of this wildcard's bound.
                         */
                        private final GenericTypeToken boundTypeToken;

                        /**
                         * Creates a new lazy lower bound wildcard.
                         *
                         * @param typePool           The type pool to use.
                         * @param typeVariableSource The type variable source for locating type variables.
                         * @param typePath           This type's type path.
                         * @param annotationTokens   This type's type annotations.
                         * @param boundTypeToken     A tokenized representation of this wildcard's bound.
                         */
                        protected LazyLowerBoundWildcard(TypePool typePool,
                                                         TypeVariableSource typeVariableSource,
                                                         String typePath,
                                                         Map<String, List<AnnotationToken>> annotationTokens,
                                                         GenericTypeToken boundTypeToken) {
                            this.typePool = typePool;
                            this.typeVariableSource = typeVariableSource;
                            this.typePath = typePath;
                            this.annotationTokens = annotationTokens;
                            this.boundTypeToken = boundTypeToken;
                        }

                        @Override
                        public TypeList.Generic getUpperBounds() {
                            return new TypeList.Generic.Explicit(Generic.OBJECT);
                        }

                        @Override
                        public TypeList.Generic getLowerBounds() {
                            return new LazyTokenList.ForWildcardBound(typePool, typeVariableSource, typePath, annotationTokens, boundTypeToken);
                        }

                        @Override
                        public AnnotationList getDeclaredAnnotations() {
                            return LazyAnnotationDescription.asListOfNullable(typePool, annotationTokens.get(typePath));
                        }
                    }
                }

                /**
                 * A generic type token for a wildcard that is bound above.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                class ForUpperBoundWildcard implements GenericTypeToken {

                    /**
                     * A token that represents the wildcard's upper bound.
                     */
                    private final GenericTypeToken boundTypeToken;

                    /**
                     * Creates a generic type token for a wildcard that is bound above.
                     *
                     * @param boundTypeToken A token that represents the wildcard's upper bound.
                     */
                    protected ForUpperBoundWildcard(GenericTypeToken boundTypeToken) {
                        this.boundTypeToken = boundTypeToken;
                    }

                    @Override
                    public Generic toGenericType(TypePool typePool,
                                                 TypeVariableSource typeVariableSource,
                                                 String typePath,
                                                 Map<String, List<AnnotationToken>> annotationTokens) {
                        return new LazyUpperBoundWildcard(typePool, typeVariableSource, typePath, annotationTokens, boundTypeToken);
                    }

                    @Override
                    public boolean isPrimaryBound(TypePool typePool) {
                        throw new IllegalStateException("A wildcard type cannot be a type variable bound: " + this);
                    }

                    @Override
                    public String getTypePathPrefix() {
                        throw new IllegalStateException("An upper bound wildcard cannot be the owner of a nested type: " + this);
                    }

                    /**
                     * A generic type representation of a tokenized wildcard with an upper bound.
                     */
                    protected static class LazyUpperBoundWildcard extends Generic.OfWildcardType {

                        /**
                         * The type pool to use.
                         */
                        private final TypePool typePool;

                        /**
                         * The type variable source for locating type variables.
                         */
                        private final TypeVariableSource typeVariableSource;

                        /**
                         * This type's type path.
                         */
                        private final String typePath;

                        /**
                         * This type's type annotations.
                         */
                        private final Map<String, List<AnnotationToken>> annotationTokens;

                        /**
                         * A tokenized representation of this wildcard's bound.
                         */
                        private final GenericTypeToken boundTypeToken;

                        /**
                         * Creates a new lazy upper bound wildcard.
                         *
                         * @param typePool           The type pool to use.
                         * @param typeVariableSource The type variable source for locating type variables.
                         * @param typePath           This type's type path.
                         * @param annotationTokens   This type's type annotations.
                         * @param boundTypeToken     A tokenized representation of this wildcard's bound.
                         */
                        protected LazyUpperBoundWildcard(TypePool typePool,
                                                         TypeVariableSource typeVariableSource,
                                                         String typePath,
                                                         Map<String, List<AnnotationToken>> annotationTokens,
                                                         GenericTypeToken boundTypeToken) {
                            this.typePool = typePool;
                            this.typeVariableSource = typeVariableSource;
                            this.typePath = typePath;
                            this.annotationTokens = annotationTokens;
                            this.boundTypeToken = boundTypeToken;
                        }

                        @Override
                        public TypeList.Generic getUpperBounds() {
                            return new LazyTokenList.ForWildcardBound(typePool, typeVariableSource, typePath, annotationTokens, boundTypeToken);
                        }

                        @Override
                        public TypeList.Generic getLowerBounds() {
                            return new TypeList.Generic.Empty();
                        }

                        @Override
                        public AnnotationList getDeclaredAnnotations() {
                            return LazyAnnotationDescription.asListOfNullable(typePool, annotationTokens.get(typePath));
                        }
                    }
                }

                /**
                 * A generic type token that represents a parameterized type.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                class ForParameterizedType implements GenericTypeToken {

                    /**
                     * The name of the parameterized type's erasure.
                     */
                    private final String name;

                    /**
                     * A list of tokens that represent the parameters of the represented type.
                     */
                    private final List<GenericTypeToken> parameterTypeTokens;

                    /**
                     * Creates a type token that represents a parameterized type.
                     *
                     * @param name                The name of the parameterized type's erasure.
                     * @param parameterTypeTokens A list of tokens that represent the parameters of the represented type.
                     */
                    protected ForParameterizedType(String name, List<GenericTypeToken> parameterTypeTokens) {
                        this.name = name;
                        this.parameterTypeTokens = parameterTypeTokens;
                    }

                    @Override
                    public Generic toGenericType(TypePool typePool, TypeVariableSource typeVariableSource, String typePath, Map<String, List<AnnotationToken>> annotationTokens) {
                        return new LazyParameterizedType(typePool, typeVariableSource, typePath, annotationTokens, name, parameterTypeTokens);
                    }

                    @Override
                    public boolean isPrimaryBound(TypePool typePool) {
                        return !typePool.describe(name).resolve().isInterface();
                    }

                    @Override
                    public String getTypePathPrefix() {
                        return String.valueOf(INNER_CLASS_PATH);
                    }

                    /**
                     * A generic type token to describe a parameterized type description with a generic owner type.
                     */
                    @HashCodeAndEqualsPlugin.Enhance
                    public static class Nested implements GenericTypeToken {

                        /**
                         * The name of the parameterized type's erasure.
                         */
                        private final String name;

                        /**
                         * A list of tokens that represent the parameters of the represented type.
                         */
                        private final List<GenericTypeToken> parameterTypeTokens;

                        /**
                         * A token that describes the described parameterized type's owner type.
                         */
                        private final GenericTypeToken ownerTypeToken;

                        /**
                         * Creates a type token that represents a parameterized type.
                         *
                         * @param name                The name of the parameterized type's erasure.
                         * @param parameterTypeTokens A list of tokens that represent the parameters of the represented type.
                         * @param ownerTypeToken      A token that describes the described parameterized type's owner type.
                         */
                        protected Nested(String name, List<GenericTypeToken> parameterTypeTokens, GenericTypeToken ownerTypeToken) {
                            this.name = name;
                            this.parameterTypeTokens = parameterTypeTokens;
                            this.ownerTypeToken = ownerTypeToken;
                        }

                        @Override
                        public Generic toGenericType(TypePool typePool,
                                                     TypeVariableSource typeVariableSource,
                                                     String typePath,
                                                     Map<String, List<AnnotationToken>> annotationTokens) {
                            return new LazyParameterizedType(typePool, typeVariableSource, typePath, annotationTokens, name, parameterTypeTokens, ownerTypeToken);
                        }

                        @Override
                        public String getTypePathPrefix() {
                            return ownerTypeToken.getTypePathPrefix() + INNER_CLASS_PATH;
                        }

                        @Override
                        public boolean isPrimaryBound(TypePool typePool) {
                            return !typePool.describe(name).resolve().isInterface();
                        }

                        /**
                         * A lazy description of a parameterized type with an owner type.
                         */
                        protected static class LazyParameterizedType extends Generic.OfParameterizedType {

                            /**
                             * The type pool that is used for locating a generic type.
                             */
                            private final TypePool typePool;

                            /**
                             * The type variable source to use for resolving type variables.
                             */
                            private final TypeVariableSource typeVariableSource;

                            /**
                             * This type's type path.
                             */
                            private final String typePath;

                            /**
                             * A mapping of type annotations for this type.
                             */
                            private final Map<String, List<AnnotationToken>> annotationTokens;

                            /**
                             * The binary name of this parameterized type's raw type.
                             */
                            private final String name;

                            /**
                             * Tokens that represent this parameterized type's parameters.
                             */
                            private final List<GenericTypeToken> parameterTypeTokens;

                            /**
                             * A token that represents this type's owner type.
                             */
                            private final GenericTypeToken ownerTypeToken;

                            /**
                             * Creates a new lazy parameterized type.
                             *
                             * @param typePool            The type pool that is used for locating a generic type.
                             * @param typeVariableSource  The type variable source to use for resolving type variables.
                             * @param typePath            This type's type path.
                             * @param annotationTokens    A mapping of type annotations for this type.
                             * @param name                The binary name of this parameterized type's raw type.
                             * @param parameterTypeTokens Tokens that represent this parameterized type's parameters.
                             * @param ownerTypeToken      A token that represents this type's owner type.
                             */
                            protected LazyParameterizedType(TypePool typePool,
                                                            TypeVariableSource typeVariableSource,
                                                            String typePath,
                                                            Map<String, List<AnnotationToken>> annotationTokens,
                                                            String name,
                                                            List<GenericTypeToken> parameterTypeTokens,
                                                            GenericTypeToken ownerTypeToken) {
                                this.typePool = typePool;
                                this.typeVariableSource = typeVariableSource;
                                this.typePath = typePath;
                                this.annotationTokens = annotationTokens;
                                this.name = name;
                                this.parameterTypeTokens = parameterTypeTokens;
                                this.ownerTypeToken = ownerTypeToken;
                            }

                            @Override
                            public TypeDescription asErasure() {
                                return typePool.describe(name).resolve();
                            }

                            @Override
                            public TypeList.Generic getTypeArguments() {
                                return new LazyTokenList(typePool, typeVariableSource, typePath + ownerTypeToken.getTypePathPrefix(), annotationTokens, parameterTypeTokens);
                            }

                            @Override
                            public Generic getOwnerType() {
                                return ownerTypeToken.toGenericType(typePool, typeVariableSource, typePath, annotationTokens);
                            }

                            @Override
                            public AnnotationList getDeclaredAnnotations() {
                                return LazyAnnotationDescription.asListOfNullable(typePool, annotationTokens.get(typePath + ownerTypeToken.getTypePathPrefix()));
                            }
                        }
                    }

                    /**
                     * A generic type description that represents a parameterized type <b>without</b> an enclosing generic owner type.
                     */
                    protected static class LazyParameterizedType extends Generic.OfParameterizedType {

                        /**
                         * The type pool that is used for locating a generic type.
                         */
                        private final TypePool typePool;

                        /**
                         * The type variable source to use for resolving type variables.
                         */
                        private final TypeVariableSource typeVariableSource;

                        /**
                         * This type's type path.
                         */
                        private final String typePath;

                        /**
                         * A mapping of the represent type's annotation tokens.
                         */
                        private final Map<String, List<AnnotationToken>> annotationTokens;

                        /**
                         * The binary name of the raw type.
                         */
                        private final String name;

                        /**
                         * A list of type tokens representing this type's bounds.
                         */
                        private final List<GenericTypeToken> parameterTypeTokens;

                        /**
                         * Creates a new description of a parameterized type.
                         *
                         * @param typePool            The type pool that is used for locating a generic type.
                         * @param typeVariableSource  The type variable source to use for resolving type variables.
                         * @param typePath            This type's type path.
                         * @param annotationTokens    A mapping of the represent type's annotation tokens,
                         * @param name                The binary name of the raw type.
                         * @param parameterTypeTokens A list of type tokens representing this type's bounds.
                         */
                        protected LazyParameterizedType(TypePool typePool,
                                                        TypeVariableSource typeVariableSource,
                                                        String typePath,
                                                        Map<String, List<AnnotationToken>> annotationTokens,
                                                        String name,
                                                        List<GenericTypeToken> parameterTypeTokens) {
                            this.typePool = typePool;
                            this.typeVariableSource = typeVariableSource;
                            this.typePath = typePath;
                            this.annotationTokens = annotationTokens;
                            this.name = name;
                            this.parameterTypeTokens = parameterTypeTokens;
                        }

                        @Override
                        public TypeDescription asErasure() {
                            return typePool.describe(name).resolve();
                        }

                        @Override
                        public TypeList.Generic getTypeArguments() {
                            return new LazyTokenList(typePool, typeVariableSource, typePath, annotationTokens, parameterTypeTokens);
                        }

                        @Override
                        public Generic getOwnerType() {
                            TypeDescription ownerType = typePool.describe(name).resolve().getEnclosingType();
                            return ownerType == null
                                    ? Generic.UNDEFINED
                                    : ownerType.asGenericType();
                        }

                        @Override
                        public AnnotationList getDeclaredAnnotations() {
                            return LazyAnnotationDescription.asListOfNullable(typePool, annotationTokens.get(typePath));
                        }
                    }
                }

                /**
                 * A lazy list of type tokens.
                 */
                class LazyTokenList extends TypeList.Generic.AbstractBase {

                    /**
                     * The type pool that is used for locating a generic type.
                     */
                    private final TypePool typePool;

                    /**
                     * The type variable source to use for resolving type variables.
                     */
                    private final TypeVariableSource typeVariableSource;

                    /**
                     * The represented types' type path to which an index step is added upon resolution.
                     */
                    private final String typePath;

                    /**
                     * A mapping of the represent types' annotation tokens.
                     */
                    private final Map<String, List<AnnotationToken>> annotationTokens;

                    /**
                     * A list of type tokens this list represents.
                     */
                    private final List<GenericTypeToken> genericTypeTokens;

                    /**
                     * Creates a new type list that represents a list of tokenized types.
                     *
                     * @param typePool           The type pool that is used for locating a generic type.
                     * @param typeVariableSource The type variable source to use for resolving type variables.
                     * @param typePath           The represented types' type path to which an index step is added upon resolution.
                     * @param annotationTokens   A mapping of the represent types' annotation tokens,
                     * @param genericTypeTokens  A list of type tokens this list represents.
                     */
                    protected LazyTokenList(TypePool typePool,
                                            TypeVariableSource typeVariableSource,
                                            String typePath,
                                            Map<String, List<AnnotationToken>> annotationTokens,
                                            List<GenericTypeToken> genericTypeTokens) {
                        this.typePool = typePool;
                        this.typeVariableSource = typeVariableSource;
                        this.typePath = typePath;
                        this.annotationTokens = annotationTokens;
                        this.genericTypeTokens = genericTypeTokens;
                    }

                    @Override
                    public Generic get(int index) {
                        return genericTypeTokens.get(index).toGenericType(typePool, typeVariableSource, typePath + index + INDEXED_TYPE_DELIMITER, annotationTokens);
                    }

                    @Override
                    public int size() {
                        return genericTypeTokens.size();
                    }

                    /**
                     * A generic type description representing a tokenized wildcard bound.
                     */
                    protected static class ForWildcardBound extends TypeList.Generic.AbstractBase {

                        /**
                         * The type pool that is used for locating a generic type.
                         */
                        private final TypePool typePool;

                        /**
                         * The type variable source to use for resolving type variables.
                         */
                        private final TypeVariableSource typeVariableSource;

                        /**
                         * The represented types' type path to which a wildcard step is added upon resolution.
                         */
                        private final String typePath;

                        /**
                         * A mapping of the represent types' annotation tokens.
                         */
                        private final Map<String, List<AnnotationToken>> annotationTokens;

                        /**
                         * A token representing the wildcard's bound.
                         */
                        private final GenericTypeToken genericTypeToken;

                        /**
                         * @param typePool           The type pool that is used for locating a generic type.
                         * @param typeVariableSource The type variable source to use for resolving type variables.
                         * @param typePath           The represented types' type path to which a wildcard step is added upon resolution.
                         * @param annotationTokens   A mapping of the represent types' annotation tokens,
                         * @param genericTypeToken   A token representing the wildcard's bound.
                         */
                        protected ForWildcardBound(TypePool typePool,
                                                   TypeVariableSource typeVariableSource,
                                                   String typePath,
                                                   Map<String, List<AnnotationToken>> annotationTokens,
                                                   GenericTypeToken genericTypeToken) {
                            this.typePool = typePool;
                            this.typeVariableSource = typeVariableSource;
                            this.typePath = typePath;
                            this.annotationTokens = annotationTokens;
                            this.genericTypeToken = genericTypeToken;
                        }

                        @Override
                        public Generic get(int index) {
                            if (index == 0) {
                                return genericTypeToken.toGenericType(typePool, typeVariableSource, typePath + WILDCARD_TYPE_PATH, annotationTokens);
                            } else {
                                throw new IndexOutOfBoundsException("index = " + index);
                            }
                        }

                        @Override
                        public int size() {
                            return 1;
                        }
                    }
                }
            }

            /**
             * A token for representing collected data on an annotation.
             */
            @HashCodeAndEqualsPlugin.Enhance
            protected static class AnnotationToken {

                /**
                 * The descriptor of the represented annotation.
                 */
                private final String descriptor;

                /**
                 * A map of annotation value names to their value representations.
                 */
                private final Map<String, AnnotationValue<?, ?>> values;

                /**
                 * Creates a new annotation token.
                 *
                 * @param descriptor The descriptor of the represented annotation.
                 * @param values     A map of annotation value names to their value representations.
                 */
                protected AnnotationToken(String descriptor, Map<String, AnnotationValue<?, ?>> values) {
                    this.descriptor = descriptor;
                    this.values = values;
                }

                /**
                 * Returns a map of annotation value names to their value representations.
                 *
                 * @return A map of annotation value names to their value representations.
                 */
                protected Map<String, AnnotationValue<?, ?>> getValues() {
                    return values;
                }

                /**
                 * Returns the annotation type's binary name.
                 *
                 * @return The annotation type's binary name.
                 */
                protected String getBinaryName() {
                    return descriptor.substring(1, descriptor.length() - 1).replace('/', '.');
                }

                /**
                 * Transforms this token into an annotation description.
                 *
                 * @param typePool The type pool to be used for looking up linked types.
                 * @return An optional description of this annotation's token.
                 */
                private Resolution toAnnotationDescription(TypePool typePool) {
                    TypePool.Resolution resolution = typePool.describe(getBinaryName());
                    return resolution.isResolved()
                            ? new Resolution.Simple(new LazyAnnotationDescription(typePool, resolution.resolve(), values))
                            : new Resolution.Illegal(getBinaryName());
                }

                /**
                 * A resolution for an annotation tokens. Any annotation is suppressed if its type is not available.
                 * This conforms to the handling of the Java reflection API.
                 */
                protected interface Resolution {

                    /**
                     * Returns {@code true} if the represented annotation could be resolved.
                     *
                     * @return {@code true} if the represented annotation could be resolved.
                     */
                    boolean isResolved();

                    /**
                     * Returns the resolved annotation. This method throws an exception if this instance is not resolved.
                     *
                     * @return The resolved annotation. This method throws an exception if this instance is not resolved.
                     */
                    AnnotationDescription resolve();

                    /**
                     * A simple resolved annotation.
                     */
                    @HashCodeAndEqualsPlugin.Enhance
                    class Simple implements Resolution {

                        /**
                         * The represented annotation description.
                         */
                        private final AnnotationDescription annotationDescription;

                        /**
                         * Creates a new simple resolution.
                         *
                         * @param annotationDescription The represented annotation description.
                         */
                        protected Simple(AnnotationDescription annotationDescription) {
                            this.annotationDescription = annotationDescription;
                        }

                        @Override
                        public boolean isResolved() {
                            return true;
                        }

                        @Override
                        public AnnotationDescription resolve() {
                            return annotationDescription;
                        }
                    }

                    /**
                     * An illegal resolution.
                     */
                    @HashCodeAndEqualsPlugin.Enhance
                    class Illegal implements Resolution {

                        /**
                         * The annotation's binary type name.
                         */
                        private final String annotationType;

                        /**
                         * Creates a new illegal resolution.
                         *
                         * @param annotationType The annotation's binary type name.
                         */
                        public Illegal(String annotationType) {
                            this.annotationType = annotationType;
                        }

                        @Override
                        public boolean isResolved() {
                            return false;
                        }

                        @Override
                        public AnnotationDescription resolve() {
                            throw new IllegalStateException("Annotation type is not available: " + annotationType);
                        }
                    }
                }
            }

            /**
             * A token for representing collected data on a field.
             */
            @HashCodeAndEqualsPlugin.Enhance
            protected static class FieldToken {

                /**
                 * The name of the field.
                 */
                private final String name;

                /**
                 * The modifiers of the represented field.
                 */
                private final int modifiers;

                /**
                 * The descriptor of the field.
                 */
                private final String descriptor;

                /**
                 * The field's generic signature as found in the class file or {@code null} if the field is not generic.
                 */
                private final String genericSignature;

                /**
                 * The resolution of this field's generic type.
                 */
                private final GenericTypeToken.Resolution.ForField signatureResolution;

                /**
                 * A mapping of the field type's type annotation tokens.
                 */
                private final Map<String, List<AnnotationToken>> typeAnnotationTokens;

                /**
                 * A list of annotation tokens representing the annotations of the represented field.
                 */
                private final List<AnnotationToken> annotationTokens;

                /**
                 * Creates a new field token.
                 *
                 * @param name                 The name of the field.
                 * @param modifiers            The modifiers of the represented field.
                 * @param descriptor           The descriptor of the field.
                 * @param genericSignature     The field's generic signature as found in the class file or {@code null} if the field is not generic.
                 * @param typeAnnotationTokens A mapping of the field type's type annotation tokens.
                 * @param annotationTokens     A list of annotation tokens representing the annotations of the represented field.
                 */
                protected FieldToken(String name,
                                     int modifiers,
                                     String descriptor,
                                     String genericSignature,
                                     Map<String, List<AnnotationToken>> typeAnnotationTokens,
                                     List<AnnotationToken> annotationTokens) {
                    this.modifiers = modifiers & ~Opcodes.ACC_DEPRECATED;
                    this.name = name;
                    this.descriptor = descriptor;
                    this.genericSignature = genericSignature;
                    signatureResolution = RAW_TYPES
                            ? GenericTypeToken.Resolution.Raw.INSTANCE
                            : GenericTypeExtractor.ForSignature.OfField.extract(genericSignature);
                    this.typeAnnotationTokens = typeAnnotationTokens;
                    this.annotationTokens = annotationTokens;
                }

                /**
                 * Transforms this token into a lazy field description.
                 *
                 * @param lazyTypeDescription The lazy type description to attach this field description to.
                 * @return A field description resembling this field token.
                 */
                private LazyFieldDescription toFieldDescription(LazyTypeDescription lazyTypeDescription) {
                    return lazyTypeDescription.new LazyFieldDescription(name,
                            modifiers,
                            descriptor,
                            genericSignature,
                            signatureResolution,
                            typeAnnotationTokens,
                            annotationTokens);
                }
            }

            /**
             * A token for representing collected data on a method.
             */
            @HashCodeAndEqualsPlugin.Enhance
            protected static class MethodToken {

                /**
                 * The internal name of the represented method.
                 */
                private final String name;

                /**
                 * The modifiers of the represented method.
                 */
                private final int modifiers;

                /**
                 * The descriptor of the represented method.
                 */
                private final String descriptor;

                /**
                 * The methods's generic signature as found in the class file or {@code null} if the method is not generic.
                 */
                private final String genericSignature;

                /**
                 * The generic type resolution of this method.
                 */
                private final GenericTypeToken.Resolution.ForMethod signatureResolution;

                /**
                 * An array of internal names of the exceptions of the represented method or {@code null} if there
                 * are no such exceptions.
                 */
                private final String[] exceptionName;

                /**
                 * A mapping of the type variables' type annotation tokens by their indices.
                 */
                private final Map<Integer, Map<String, List<AnnotationToken>>> typeVariableAnnotationTokens;

                /**
                 * A mapping of the type variables' type bounds' type annotation tokens by their indices and each variable's index.
                 */
                private final Map<Integer, Map<Integer, Map<String, List<AnnotationToken>>>> typeVariableBoundAnnotationTokens;

                /**
                 * A mapping of the return type's type variable tokens.
                 */
                private final Map<String, List<AnnotationToken>> returnTypeAnnotationTokens;

                /**
                 * A mapping of the parameter types' type annotation tokens by their indices.
                 */
                private final Map<Integer, Map<String, List<AnnotationToken>>> parameterTypeAnnotationTokens;

                /**
                 * A mapping of the exception types' type annotation tokens by their indices.
                 */
                private final Map<Integer, Map<String, List<AnnotationToken>>> exceptionTypeAnnotationTokens;

                /**
                 * A mapping of the receiver type's annotation tokens.
                 */
                private final Map<String, List<AnnotationToken>> receiverTypeAnnotationTokens;

                /**
                 * A list of annotation tokens that are present on the represented method.
                 */
                private final List<AnnotationToken> annotationTokens;

                /**
                 * A map of parameter indices to tokens that represent their annotations.
                 */
                private final Map<Integer, List<AnnotationToken>> parameterAnnotationTokens;

                /**
                 * A list of tokens describing meta data of the method's parameters.
                 */
                private final List<ParameterToken> parameterTokens;

                /**
                 * The default value of this method or {@code null} if there is no such value.
                 */
                private final AnnotationValue<?, ?> defaultValue;

                /**
                 * Creates a new method token.
                 *
                 * @param name                              The name of the method.
                 * @param modifiers                         The modifiers of the represented method.
                 * @param descriptor                        The descriptor of the represented method.
                 * @param genericSignature                  The methods's generic signature as found in the class file or {@code null} if the method is not generic.
                 * @param exceptionName                     An array of internal names of the exceptions of the represented method or {@code null} if
                 *                                          there are no such exceptions.
                 * @param typeVariableAnnotationTokens      A mapping of the type variables' type annotation tokens by their indices.
                 * @param typeVariableBoundAnnotationTokens A mapping of the type variables' type bounds' type annotation tokens by their
                 *                                          index and each variable's index.
                 * @param returnTypeAnnotationTokens        A mapping of the return type's type variable tokens.
                 * @param parameterTypeAnnotationTokens     A mapping of the parameter types' type annotation tokens by their indices.
                 * @param exceptionTypeAnnotationTokens     A mapping of the exception types' type annotation tokens by their indices.
                 * @param receiverTypeAnnotationTokens      A mapping of the receiver type's annotation tokens.
                 * @param annotationTokens                  A list of annotation tokens that are present on the represented method.
                 * @param parameterAnnotationTokens         A map of parameter indices to tokens that represent their annotations.
                 * @param parameterTokens                   A list of tokens describing meta data of the method's parameters.
                 * @param defaultValue                      The default value of this method or {@code null} if there is no such value.
                 */
                protected MethodToken(String name,
                                      int modifiers,
                                      String descriptor,
                                      String genericSignature,
                                      String[] exceptionName,
                                      Map<Integer, Map<String, List<AnnotationToken>>> typeVariableAnnotationTokens,
                                      Map<Integer, Map<Integer, Map<String, List<AnnotationToken>>>> typeVariableBoundAnnotationTokens,
                                      Map<String, List<AnnotationToken>> returnTypeAnnotationTokens,
                                      Map<Integer, Map<String, List<AnnotationToken>>> parameterTypeAnnotationTokens,
                                      Map<Integer, Map<String, List<AnnotationToken>>> exceptionTypeAnnotationTokens,
                                      Map<String, List<AnnotationToken>> receiverTypeAnnotationTokens,
                                      List<AnnotationToken> annotationTokens,
                                      Map<Integer, List<AnnotationToken>> parameterAnnotationTokens,
                                      List<ParameterToken> parameterTokens,
                                      AnnotationValue<?, ?> defaultValue) {
                    this.modifiers = modifiers & ~Opcodes.ACC_DEPRECATED;
                    this.name = name;
                    this.descriptor = descriptor;
                    this.genericSignature = genericSignature;
                    signatureResolution = RAW_TYPES
                            ? GenericTypeToken.Resolution.Raw.INSTANCE
                            : GenericTypeExtractor.ForSignature.OfMethod.extract(genericSignature);
                    this.exceptionName = exceptionName;
                    this.typeVariableAnnotationTokens = typeVariableAnnotationTokens;
                    this.typeVariableBoundAnnotationTokens = typeVariableBoundAnnotationTokens;
                    this.returnTypeAnnotationTokens = returnTypeAnnotationTokens;
                    this.parameterTypeAnnotationTokens = parameterTypeAnnotationTokens;
                    this.exceptionTypeAnnotationTokens = exceptionTypeAnnotationTokens;
                    this.receiverTypeAnnotationTokens = receiverTypeAnnotationTokens;
                    this.annotationTokens = annotationTokens;
                    this.parameterAnnotationTokens = parameterAnnotationTokens;
                    this.parameterTokens = parameterTokens;
                    this.defaultValue = defaultValue;
                }

                /**
                 * Transforms this method token to a method description that is attached to a lazy type description.
                 *
                 * @param lazyTypeDescription The lazy type description to attach this method description to.
                 * @return A method description representing this field token.
                 */
                private MethodDescription.InDefinedShape toMethodDescription(LazyTypeDescription lazyTypeDescription) {
                    return lazyTypeDescription.new LazyMethodDescription(name,
                            modifiers,
                            descriptor,
                            genericSignature,
                            signatureResolution,
                            exceptionName,
                            typeVariableAnnotationTokens,
                            typeVariableBoundAnnotationTokens,
                            returnTypeAnnotationTokens,
                            parameterTypeAnnotationTokens,
                            exceptionTypeAnnotationTokens,
                            receiverTypeAnnotationTokens,
                            annotationTokens,
                            parameterAnnotationTokens,
                            parameterTokens,
                            defaultValue);
                }

                /**
                 * A token representing a method's parameter.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                protected static class ParameterToken {

                    /**
                     * Donates an unknown name of a parameter.
                     */
                    protected static final String NO_NAME = null;

                    /**
                     * Donates an unknown modifier of a parameter.
                     */
                    protected static final Integer NO_MODIFIERS = null;

                    /**
                     * The name of the parameter or {@code null} if no explicit name for this parameter is known.
                     */
                    @HashCodeAndEqualsPlugin.ValueHandling(HashCodeAndEqualsPlugin.ValueHandling.Sort.REVERSE_NULLABILITY)
                    private final String name;

                    /**
                     * The modifiers of the parameter or {@code null} if no modifiers are known for this parameter.
                     */
                    @HashCodeAndEqualsPlugin.ValueHandling(HashCodeAndEqualsPlugin.ValueHandling.Sort.REVERSE_NULLABILITY)
                    private final Integer modifiers;

                    /**
                     * Creates a parameter token for a parameter without an explicit name and without specific modifiers.
                     */
                    protected ParameterToken() {
                        this(NO_NAME);
                    }

                    /**
                     * Creates a parameter token for a parameter with an explicit name and without specific modifiers.
                     *
                     * @param name The name of the parameter.
                     */
                    protected ParameterToken(String name) {
                        this(name, NO_MODIFIERS);
                    }

                    /**
                     * Creates a parameter token for a parameter with an explicit name and with specific modifiers.
                     *
                     * @param name      The name of the parameter.
                     * @param modifiers The modifiers of the parameter.
                     */
                    protected ParameterToken(String name, Integer modifiers) {
                        this.name = name;
                        this.modifiers = modifiers;
                    }

                    /**
                     * Returns the name of the parameter or {@code null} if there is no such name.
                     *
                     * @return The name of the parameter or {@code null} if there is no such name.
                     */
                    protected String getName() {
                        return name;
                    }

                    /**
                     * Returns the modifiers of the parameter or {@code null} if no modifiers are known.
                     *
                     * @return The modifiers of the parameter or {@code null} if no modifiers are known.
                     */
                    protected Integer getModifiers() {
                        return modifiers;
                    }
                }
            }

            /**
             * A lazy description of an annotation that looks up types from a type pool when required.
             */
            private static class LazyAnnotationDescription extends AnnotationDescription.AbstractBase {

                /**
                 * The type pool for looking up type references.
                 */
                protected final TypePool typePool;

                /**
                 * The type of this annotation.
                 */
                private final TypeDescription annotationType;

                /**
                 * A map of annotation values by their property name.
                 */
                protected final Map<String, AnnotationValue<?, ?>> values;

                /**
                 * Creates a new lazy annotation description.
                 *
                 * @param typePool       The type pool to be used for looking up linked types.
                 * @param annotationType The annotation's type.
                 * @param values         A map of annotation value names to their value representations.
                 */
                private LazyAnnotationDescription(TypePool typePool, TypeDescription annotationType, Map<String, AnnotationValue<?, ?>> values) {
                    this.typePool = typePool;
                    this.annotationType = annotationType;
                    this.values = values;
                }

                /**
                 * Represents a list of annotation tokens in form of a list of lazy type annotations. Any annotation with
                 * a type that cannot be loaded from the type pool is ignored and not included in the list. If the provided
                 * {@code tokens} are {@code null}, an empty list is returned.
                 *
                 * @param typePool The type pool to be used for looking up linked types.
                 * @param tokens   The tokens to represent in the list.
                 * @return A list of the loadable annotations.
                 */
                protected static AnnotationList asListOfNullable(TypePool typePool, List<? extends AnnotationToken> tokens) {
                    return tokens == null
                            ? new AnnotationList.Empty()
                            : asList(typePool, tokens);
                }

                /**
                 * Represents a list of annotation tokens in form of a list of lazy type annotations. Any annotation with
                 * a type that cannot be loaded from the type pool is ignored and not included in the list.
                 *
                 * @param typePool The type pool to be used for looking up linked types.
                 * @param tokens   The tokens to represent in the list.
                 * @return A list of the loadable annotations.
                 */
                protected static AnnotationList asList(TypePool typePool, List<? extends AnnotationToken> tokens) {
                    List<AnnotationDescription> annotationDescriptions = new ArrayList<AnnotationDescription>(tokens.size());
                    for (AnnotationToken token : tokens) {
                        AnnotationToken.Resolution resolution = token.toAnnotationDescription(typePool);
                        if (resolution.isResolved()) {
                            annotationDescriptions.add(resolution.resolve());
                        }
                    }
                    return new AnnotationList.Explicit(annotationDescriptions);
                }

                @Override
                public AnnotationValue<?, ?> getValue(MethodDescription.InDefinedShape property) {
                    if (!property.getDeclaringType().asErasure().equals(annotationType)) {
                        throw new IllegalArgumentException(property + " is not declared by " + getAnnotationType());
                    }
                    AnnotationValue<?, ?> annotationValue = values.get(property.getName());
                    if (annotationValue == null) {
                        annotationValue = getAnnotationType().getDeclaredMethods().filter(is(property)).getOnly().getDefaultValue();
                    }
                    if (annotationValue != null) {
                        return annotationValue;
                    }
                    throw new IllegalStateException(property + " is not defined on annotation");
                }

                @Override
                public TypeDescription getAnnotationType() {
                    return annotationType;
                }

                @Override
                public <T extends Annotation> Loadable<T> prepare(Class<T> annotationType) {
                    if (!this.annotationType.represents(annotationType)) {
                        throw new IllegalArgumentException(annotationType + " does not represent " + this.annotationType);
                    }
                    return new Loadable<T>(typePool, annotationType, values);
                }

                /**
                 * A loadable version of a lazy annotation description.
                 *
                 * @param <S> The annotation type.
                 */
                private static class Loadable<S extends Annotation> extends LazyAnnotationDescription implements AnnotationDescription.Loadable<S> {

                    /**
                     * The loaded annotation type.
                     */
                    private final Class<S> annotationType;

                    /**
                     * Creates a new loadable version of a lazy annotation.
                     *
                     * @param typePool       The type pool to be used for looking up linked types.
                     * @param annotationType The annotation's loaded type.
                     * @param values         A map of annotation value names to their value representations.
                     */
                    private Loadable(TypePool typePool, Class<S> annotationType, Map<String, AnnotationValue<?, ?>> values) {
                        super(typePool, new ForLoadedType(annotationType), values);
                        this.annotationType = annotationType;
                    }

                    @Override
                    public S load() throws ClassNotFoundException {
                        return AnnotationInvocationHandler.of(annotationType.getClassLoader(), annotationType, values);
                    }

                    @Override
                    public S loadSilent() {
                        try {
                            return load();
                        } catch (ClassNotFoundException exception) {
                            throw new IllegalStateException("Could not load annotation type or referenced type", exception);
                        }
                    }
                }
            }

            /**
             * An implementation of a {@link PackageDescription} that only
             * loads its annotations on requirement.
             */
            private static class LazyPackageDescription extends PackageDescription.AbstractBase {

                /**
                 * The type pool to use for look-ups.
                 */
                private final TypePool typePool;

                /**
                 * The name of the package.
                 */
                private final String name;

                /**
                 * Creates a new lazy package description.
                 *
                 * @param typePool The type pool to use for look-ups.
                 * @param name     The name of the package.
                 */
                private LazyPackageDescription(TypePool typePool, String name) {
                    this.typePool = typePool;
                    this.name = name;
                }

                @Override
                public AnnotationList getDeclaredAnnotations() {
                    Resolution resolution = typePool.describe(name + "." + PackageDescription.PACKAGE_CLASS_NAME);
                    return resolution.isResolved()
                            ? resolution.resolve().getDeclaredAnnotations()
                            : new AnnotationList.Empty();
                }

                @Override
                public String getName() {
                    return name;
                }
            }

            /**
             * A list that is constructing {@link LazyTypeDescription}s.
             */
            private static class LazyTypeList extends TypeList.AbstractBase {

                /**
                 * The type pool to use for locating types.
                 */
                private final TypePool typePool;

                /**
                 * A list of type descriptors that this list represents.
                 */
                private final List<String> descriptors;

                /**
                 * Creates a list of lazy type descriptions.
                 *
                 * @param typePool    The type pool to use for locating types.
                 * @param descriptors A list of type descriptors that this list represents.
                 */
                private LazyTypeList(TypePool typePool, List<String> descriptors) {
                    this.typePool = typePool;
                    this.descriptors = descriptors;
                }

                @Override
                public TypeDescription get(int index) {
                    return TokenizedGenericType.toErasure(typePool, descriptors.get(index));
                }

                @Override
                public int size() {
                    return descriptors.size();
                }

                @Override
                public String[] toInternalNames() {
                    String[] internalName = new String[descriptors.size()];
                    int index = 0;
                    for (String descriptor : descriptors) {
                        internalName[index++] = Type.getType(descriptor).getInternalName();
                    }
                    return internalName.length == 0
                            ? NO_INTERFACES
                            : internalName;
                }

                @Override
                public int getStackSize() {
                    int stackSize = 0;
                    for (String descriptor : descriptors) {
                        stackSize += Type.getType(descriptor).getSize();
                    }
                    return stackSize;
                }
            }

            /**
             * A representation of a generic type that is described by a {@link GenericTypeToken}.
             */
            private static class TokenizedGenericType extends Generic.LazyProjection.WithEagerNavigation {

                /**
                 * The type pool to use for locating referenced types.
                 */
                private final TypePool typePool;

                /**
                 * The token that describes the represented generic type.
                 */
                private final GenericTypeToken genericTypeToken;

                /**
                 * A descriptor of the generic type's raw type.
                 */
                private final String rawTypeDescriptor;

                /**
                 * The tokenized type's type annotation tokens.
                 */
                private final Map<String, List<AnnotationToken>> annotationTokens;

                /**
                 * The closest type variable source of this generic type's declaration context.
                 */
                private final TypeVariableSource typeVariableSource;

                /**
                 * Creates a new tokenized generic type.
                 *
                 * @param typePool           The type pool to use for locating referenced types.
                 * @param genericTypeToken   The token that describes the represented generic type.
                 * @param rawTypeDescriptor  A descriptor of the generic type's erasure.
                 * @param annotationTokens   The tokenized type's type annotation tokens.
                 * @param typeVariableSource The closest type variable source of this generic type's declaration context.
                 */
                protected TokenizedGenericType(TypePool typePool,
                                               GenericTypeToken genericTypeToken,
                                               String rawTypeDescriptor,
                                               Map<String, List<AnnotationToken>> annotationTokens,
                                               TypeVariableSource typeVariableSource) {
                    this.typePool = typePool;
                    this.genericTypeToken = genericTypeToken;
                    this.rawTypeDescriptor = rawTypeDescriptor;
                    this.annotationTokens = annotationTokens;
                    this.typeVariableSource = typeVariableSource;
                }

                /**
                 * Creates a new generic type description for a tokenized generic type.
                 *
                 * @param typePool           The type pool to use for locating referenced types.
                 * @param genericTypeToken   The token that describes the represented generic type.
                 * @param rawTypeDescriptor  A descriptor of the generic type's erasure.
                 * @param annotationTokens   The tokenized type's type annotation tokens or {@code null} if no such annotations are defined.
                 * @param typeVariableSource The closest type variable source of this generic type's declaration context.
                 * @return A suitable generic type.
                 */
                protected static Generic of(TypePool typePool,
                                            GenericTypeToken genericTypeToken,
                                            String rawTypeDescriptor,
                                            Map<String, List<AnnotationToken>> annotationTokens,
                                            TypeVariableSource typeVariableSource) {
                    return new TokenizedGenericType(typePool,
                            genericTypeToken,
                            rawTypeDescriptor,
                            annotationTokens == null
                                    ? Collections.<String, List<AnnotationToken>>emptyMap()
                                    : annotationTokens,
                            typeVariableSource);
                }

                /**
                 * Creates a type description from a descriptor by looking up the corresponding type.
                 *
                 * @param typePool   The type pool to use for locating a type.
                 * @param descriptor The descriptor to interpret.
                 * @return A description of the type represented by the descriptor.
                 */
                protected static TypeDescription toErasure(TypePool typePool, String descriptor) {
                    Type type = Type.getType(descriptor);
                    return typePool.describe(type.getSort() == Type.ARRAY
                            ? type.getInternalName().replace('/', '.')
                            : type.getClassName()).resolve();
                }

                @Override
                protected Generic resolve() {
                    return genericTypeToken.toGenericType(typePool, typeVariableSource, GenericTypeToken.EMPTY_TYPE_PATH, annotationTokens);
                }

                @Override
                public TypeDescription asErasure() {
                    return toErasure(typePool, rawTypeDescriptor);
                }

                @Override
                public AnnotationList getDeclaredAnnotations() {
                    return resolve().getDeclaredAnnotations();
                }

                /**
                 * A tokenized list of generic types.
                 */
                protected static class TokenList extends TypeList.Generic.AbstractBase {

                    /**
                     * The type pool to use for locating types.
                     */
                    private final TypePool typePool;

                    /**
                     * Type tokens that describe the represented generic types.
                     */
                    private final List<GenericTypeToken> genericTypeTokens;

                    /**
                     * A list of the generic types' erasures.
                     */
                    private final List<String> rawTypeDescriptors;

                    /**
                     * The closest type variable source of this generic type's declaration context.
                     */
                    private final TypeVariableSource typeVariableSource;

                    /**
                     * A mapping of each type's type annotation tokens by its index.
                     */
                    private final Map<Integer, Map<String, List<AnnotationToken>>> annotationTokens;

                    /**
                     * Creates a list of tokenized generic types.
                     *
                     * @param typePool           The type pool to use for locating type descriptions.
                     * @param genericTypeTokens  A list of tokens describing the represented generic types.
                     * @param annotationTokens   A mapping of each type's type annotation tokens by its index.
                     * @param rawTypeDescriptors A list of the generic types' erasures.
                     * @param typeVariableSource The closest type variable source of this generic type's declaration context.
                     */
                    private TokenList(TypePool typePool,
                                      List<GenericTypeToken> genericTypeTokens,
                                      Map<Integer, Map<String, List<AnnotationToken>>> annotationTokens,
                                      List<String> rawTypeDescriptors,
                                      TypeVariableSource typeVariableSource) {
                        this.typePool = typePool;
                        this.genericTypeTokens = genericTypeTokens;
                        this.annotationTokens = annotationTokens;
                        this.rawTypeDescriptors = rawTypeDescriptors;
                        this.typeVariableSource = typeVariableSource;
                    }

                    @Override
                    public Generic get(int index) {
                        return rawTypeDescriptors.size() == genericTypeTokens.size()
                                ? TokenizedGenericType.of(typePool, genericTypeTokens.get(index), rawTypeDescriptors.get(index), annotationTokens.get(index), typeVariableSource)
                                : TokenizedGenericType.toErasure(typePool, rawTypeDescriptors.get(index)).asGenericType();
                    }

                    @Override
                    public int size() {
                        return rawTypeDescriptors.size();
                    }

                    @Override
                    public TypeList asErasures() {
                        return new LazyTypeList(typePool, rawTypeDescriptors);
                    }
                }

                /**
                 * A list of tokenized type variables.
                 */
                protected static class TypeVariableList extends TypeList.Generic.AbstractBase {

                    /**
                     * The type pool to use for locating types.
                     */
                    private final TypePool typePool;

                    /**
                     * Type tokens that describe the represented type variables.
                     */
                    private final List<GenericTypeToken.OfFormalTypeVariable> typeVariables;

                    /**
                     * The type variable source of the represented type variables.
                     */
                    private final TypeVariableSource typeVariableSource;

                    /**
                     * A mapping of the type variables' type annotation tokens by their indices.
                     */
                    private final Map<Integer, Map<String, List<AnnotationToken>>> annotationTokens;

                    /**
                     * A mapping of the type variables' bound types' annotation tokens by their indices and each type variable's index..
                     */
                    private final Map<Integer, Map<Integer, Map<String, List<AnnotationToken>>>> boundAnnotationTokens;

                    /**
                     * Creates a list of type variables.
                     *
                     * @param typePool              The type pool to use for locating types.
                     * @param typeVariables         Type tokens that describe the represented generic types.
                     * @param typeVariableSource    The type variable source of the represented type variables.
                     * @param annotationTokens      A mapping of the type variables' type annotation tokens by their indices.
                     * @param boundAnnotationTokens A mapping of the type variables' bound types' annotation tokens by their indices
                     *                              and each type variable's index.
                     */
                    protected TypeVariableList(TypePool typePool,
                                               List<GenericTypeToken.OfFormalTypeVariable> typeVariables,
                                               TypeVariableSource typeVariableSource,
                                               Map<Integer, Map<String, List<AnnotationToken>>> annotationTokens,
                                               Map<Integer, Map<Integer, Map<String, List<AnnotationToken>>>> boundAnnotationTokens) {
                        this.typePool = typePool;
                        this.typeVariables = typeVariables;
                        this.typeVariableSource = typeVariableSource;
                        this.annotationTokens = annotationTokens;
                        this.boundAnnotationTokens = boundAnnotationTokens;
                    }

                    @Override
                    public Generic get(int index) {
                        return typeVariables.get(index).toGenericType(typePool, typeVariableSource, annotationTokens.get(index), boundAnnotationTokens.get(index));
                    }

                    @Override
                    public int size() {
                        return typeVariables.size();
                    }
                }

                /**
                 * A lazy description of a non-well-defined described generic type.
                 */
                protected static class Malformed extends LazyProjection.WithEagerNavigation {

                    /**
                     * The type pool to use for locating types.
                     */
                    private final TypePool typePool;

                    /**
                     * The descriptor of the type erasure.
                     */
                    private final String rawTypeDescriptor;

                    /**
                     * Creates a lazy description of a non-well-defined described generic type.
                     *
                     * @param typePool          The type pool to use for locating types.
                     * @param rawTypeDescriptor The descriptor of the type erasure.
                     */
                    protected Malformed(TypePool typePool, String rawTypeDescriptor) {
                        this.typePool = typePool;
                        this.rawTypeDescriptor = rawTypeDescriptor;
                    }

                    @Override
                    protected Generic resolve() {
                        throw new GenericSignatureFormatError();
                    }

                    @Override
                    public TypeDescription asErasure() {
                        return toErasure(typePool, rawTypeDescriptor);
                    }

                    @Override
                    public AnnotationList getDeclaredAnnotations() {
                        throw new GenericSignatureFormatError();
                    }

                    /**
                     * A tokenized list of non-well-defined generic types.
                     */
                    protected static class TokenList extends TypeList.Generic.AbstractBase {

                        /**
                         * The type pool to use for locating types.
                         */
                        private final TypePool typePool;

                        /**
                         * A list of descriptors of the list's types' erasures.
                         */
                        private final List<String> rawTypeDescriptors;

                        /**
                         * Creates a new tokenized list of generic types.
                         *
                         * @param typePool           The type pool to use for locating types.
                         * @param rawTypeDescriptors A list of descriptors of the list's types' erasures.
                         */
                        protected TokenList(TypePool typePool, List<String> rawTypeDescriptors) {
                            this.typePool = typePool;
                            this.rawTypeDescriptors = rawTypeDescriptors;
                        }

                        @Override
                        public Generic get(int index) {
                            return new Malformed(typePool, rawTypeDescriptors.get(index));
                        }

                        @Override
                        public int size() {
                            return rawTypeDescriptors.size();
                        }

                        @Override
                        public TypeList asErasures() {
                            return new LazyTypeList(typePool, rawTypeDescriptors);
                        }
                    }

                }
            }

            /**
             * A lazy field description that only resolved type references when required.
             */
            private class LazyFieldDescription extends FieldDescription.InDefinedShape.AbstractBase {

                /**
                 * The name of the field.
                 */
                private final String name;

                /**
                 * The modifiers of the field.
                 */
                private final int modifiers;

                /**
                 * The descriptor of this field's type.
                 */
                private final String descriptor;

                /**
                 * The field's generic signature as found in the class file or {@code null} if the field is not generic.
                 */
                private final String genericSignature;

                /**
                 * A resolution of this field's generic type.
                 */
                private final GenericTypeToken.Resolution.ForField signatureResolution;

                /**
                 * A mapping of the field type's type annotation tokens.
                 */
                private final Map<String, List<AnnotationToken>> typeAnnotationTokens;

                /**
                 * A list of annotation descriptions of this field.
                 */
                private final List<AnnotationToken> annotationTokens;

                /**
                 * Creates a new lazy field description.
                 *
                 * @param name                 The name of the field.
                 * @param modifiers            The modifiers of the field.
                 * @param descriptor           The descriptor of this field's type.
                 * @param genericSignature     The field's generic signature as found in the class file or {@code null} if the field is not generic.
                 * @param signatureResolution  A resolution of this field's generic type.
                 * @param typeAnnotationTokens A mapping of the field type's type annotation tokens.
                 * @param annotationTokens     A list of annotation descriptions of this field.
                 */
                private LazyFieldDescription(String name,
                                             int modifiers,
                                             String descriptor,
                                             String genericSignature,
                                             GenericTypeToken.Resolution.ForField signatureResolution,
                                             Map<String, List<AnnotationToken>> typeAnnotationTokens,
                                             List<AnnotationToken> annotationTokens) {
                    this.modifiers = modifiers;
                    this.name = name;
                    this.descriptor = descriptor;
                    this.genericSignature = genericSignature;
                    this.signatureResolution = signatureResolution;
                    this.typeAnnotationTokens = typeAnnotationTokens;
                    this.annotationTokens = annotationTokens;
                }

                @Override
                public Generic getType() {
                    return signatureResolution.resolveFieldType(descriptor, typePool, typeAnnotationTokens, this);
                }

                @Override
                public AnnotationList getDeclaredAnnotations() {
                    return LazyAnnotationDescription.asListOfNullable(typePool, annotationTokens);
                }

                @Override
                public String getName() {
                    return name;
                }

                @Override
                public TypeDescription getDeclaringType() {
                    return LazyTypeDescription.this;
                }

                @Override
                public int getModifiers() {
                    return modifiers;
                }

                @Override
                public String getGenericSignature() {
                    return genericSignature;
                }
            }

            /**
             * A lazy representation of a method that resolves references to types only on demand.
             */
            private class LazyMethodDescription extends MethodDescription.InDefinedShape.AbstractBase {

                /**
                 * The internal name of this method.
                 */
                private final String internalName;

                /**
                 * The modifiers of this method.
                 */
                private final int modifiers;

                /**
                 * The descriptor of the return type.
                 */
                private final String returnTypeDescriptor;

                /**
                 * The method's generic signature as found in the class file or {@code null} if the method is not generic.
                 */
                private final String genericSignature;

                /**
                 * The generic type token of this method.
                 */
                private final GenericTypeToken.Resolution.ForMethod signatureResolution;

                /**
                 * A list of type descriptions of this method's parameters.
                 */
                private final List<String> parameterTypeDescriptors;

                /**
                 * A list of type descriptions of this method's exception types.
                 */
                private final List<String> exceptionTypeDescriptors;

                /**
                 * A mapping of the type variables' type annotation tokens by their indices.
                 */
                private final Map<Integer, Map<String, List<AnnotationToken>>> typeVariableAnnotationTokens;

                /**
                 * A mapping of the type variables' type bounds' type annotation tokens by their indices and each variable's index.
                 */
                private final Map<Integer, Map<Integer, Map<String, List<AnnotationToken>>>> typeVariableBoundAnnotationTokens;

                /**
                 * A mapping of the return type's type variable tokens.
                 */
                private final Map<String, List<AnnotationToken>> returnTypeAnnotationTokens;

                /**
                 * A mapping of the parameter types' type annotation tokens by their indices.
                 */
                private final Map<Integer, Map<String, List<AnnotationToken>>> parameterTypeAnnotationTokens;

                /**
                 * A mapping of the exception types' type annotation tokens by their indices.
                 */
                private final Map<Integer, Map<String, List<AnnotationToken>>> exceptionTypeAnnotationTokens;

                /**
                 * A mapping of the receiver type's type annotation tokens.
                 */
                private final Map<String, List<AnnotationToken>> receiverTypeAnnotationTokens;

                /**
                 * The annotation tokens representing the method's annotations.
                 */
                private final List<AnnotationToken> annotationTokens;

                /**
                 * The annotation tokens representing the parameter's annotation. Every index can
                 * contain {@code null} if a parameter does not define any annotations.
                 */
                private final Map<Integer, List<AnnotationToken>> parameterAnnotationTokens;

                /**
                 * An array of parameter names which may be {@code null} if no explicit name is known for a parameter.
                 */
                private final String[] parameterNames;

                /**
                 * An array of parameter modifiers which may be {@code null} if no modifiers is known.
                 */
                private final Integer[] parameterModifiers;

                /**
                 * The default value of this method or {@code null} if no such value exists.
                 */
                private final AnnotationValue<?, ?> defaultValue;

                /**
                 * Creates a new lazy method description.
                 *
                 * @param internalName                      The internal name of this method.
                 * @param modifiers                         The modifiers of the represented method.
                 * @param descriptor                        The method descriptor of this method.
                 * @param genericSignature                  The method's generic signature as found in the class file or {@code null} if the method is not generic.
                 * @param signatureResolution               The generic type token of this method.
                 * @param exceptionTypeInternalName         The internal names of the exceptions that are declared by this
                 *                                          method or {@code null} if no exceptions are declared by this
                 *                                          method.
                 * @param typeVariableAnnotationTokens      A mapping of the type variables' type annotation tokens by their indices.
                 * @param typeVariableBoundAnnotationTokens A mapping of the type variables' type bounds' type annotation tokens by their
                 *                                          index and each variable's index.
                 * @param returnTypeAnnotationTokens        A mapping of the return type's type variable tokens.
                 * @param parameterTypeAnnotationTokens     A mapping of the parameter types' type annotation tokens by their indices.
                 * @param exceptionTypeAnnotationTokens     A mapping of the exception types' type annotation tokens by their indices.
                 * @param receiverTypeAnnotationTokens      A mapping of the receiver type's type annotation tokens.
                 * @param annotationTokens                  The annotation tokens representing the method's annotations.
                 * @param parameterAnnotationTokens         The annotation tokens representing the parameter's annotation. Every
                 *                                          index can contain {@code null} if a parameter does not define any annotations.
                 * @param parameterTokens                   A list of parameter tokens which might be empty or even out of sync
                 *                                          with the actual parameters if the debugging information found in a
                 *                                          class was corrupt.
                 * @param defaultValue                      The default value of this method or {@code null} if there is no
                 */
                private LazyMethodDescription(String internalName,
                                              int modifiers,
                                              String descriptor,
                                              String genericSignature,
                                              GenericTypeToken.Resolution.ForMethod signatureResolution,
                                              String[] exceptionTypeInternalName,
                                              Map<Integer, Map<String, List<AnnotationToken>>> typeVariableAnnotationTokens,
                                              Map<Integer, Map<Integer, Map<String, List<AnnotationToken>>>> typeVariableBoundAnnotationTokens,
                                              Map<String, List<AnnotationToken>> returnTypeAnnotationTokens,
                                              Map<Integer, Map<String, List<AnnotationToken>>> parameterTypeAnnotationTokens,
                                              Map<Integer, Map<String, List<AnnotationToken>>> exceptionTypeAnnotationTokens,
                                              Map<String, List<AnnotationToken>> receiverTypeAnnotationTokens,
                                              List<AnnotationToken> annotationTokens,
                                              Map<Integer, List<AnnotationToken>> parameterAnnotationTokens,
                                              List<MethodToken.ParameterToken> parameterTokens,
                                              AnnotationValue<?, ?> defaultValue) {
                    this.modifiers = modifiers;
                    this.internalName = internalName;
                    Type methodType = Type.getMethodType(descriptor);
                    Type returnType = methodType.getReturnType();
                    Type[] parameterType = methodType.getArgumentTypes();
                    returnTypeDescriptor = returnType.getDescriptor();
                    parameterTypeDescriptors = new ArrayList<String>(parameterType.length);
                    for (Type type : parameterType) {
                        parameterTypeDescriptors.add(type.getDescriptor());
                    }
                    this.genericSignature = genericSignature;
                    this.signatureResolution = signatureResolution;
                    if (exceptionTypeInternalName == null) {
                        exceptionTypeDescriptors = Collections.emptyList();
                    } else {
                        exceptionTypeDescriptors = new ArrayList<String>(exceptionTypeInternalName.length);
                        for (String anExceptionTypeInternalName : exceptionTypeInternalName) {
                            exceptionTypeDescriptors.add(Type.getObjectType(anExceptionTypeInternalName).getDescriptor());
                        }
                    }
                    this.typeVariableAnnotationTokens = typeVariableAnnotationTokens;
                    this.typeVariableBoundAnnotationTokens = typeVariableBoundAnnotationTokens;
                    this.returnTypeAnnotationTokens = returnTypeAnnotationTokens;
                    this.parameterTypeAnnotationTokens = parameterTypeAnnotationTokens;
                    this.exceptionTypeAnnotationTokens = exceptionTypeAnnotationTokens;
                    this.receiverTypeAnnotationTokens = receiverTypeAnnotationTokens;
                    this.annotationTokens = annotationTokens;
                    this.parameterAnnotationTokens = parameterAnnotationTokens;
                    parameterNames = new String[parameterType.length];
                    parameterModifiers = new Integer[parameterType.length];
                    if (parameterTokens.size() == parameterType.length) {
                        int index = 0;
                        for (MethodToken.ParameterToken parameterToken : parameterTokens) {
                            parameterNames[index] = parameterToken.getName();
                            parameterModifiers[index] = parameterToken.getModifiers();
                            index++;
                        }
                    }
                    this.defaultValue = defaultValue;
                }

                @Override
                public Generic getReturnType() {
                    return signatureResolution.resolveReturnType(returnTypeDescriptor, typePool, returnTypeAnnotationTokens, this);
                }

                @Override
                public TypeList.Generic getExceptionTypes() {
                    return signatureResolution.resolveExceptionTypes(exceptionTypeDescriptors, typePool, exceptionTypeAnnotationTokens, this);
                }

                @Override
                public ParameterList<ParameterDescription.InDefinedShape> getParameters() {
                    return new LazyParameterList();
                }

                @Override
                public AnnotationList getDeclaredAnnotations() {
                    return LazyAnnotationDescription.asList(typePool, annotationTokens);
                }

                @Override
                public String getInternalName() {
                    return internalName;
                }

                @Override
                public TypeDescription getDeclaringType() {
                    return LazyTypeDescription.this;
                }

                @Override
                public int getModifiers() {
                    return modifiers;
                }

                @Override
                public TypeList.Generic getTypeVariables() {
                    return signatureResolution.resolveTypeVariables(typePool, this, typeVariableAnnotationTokens, typeVariableBoundAnnotationTokens);
                }

                @Override
                public AnnotationValue<?, ?> getDefaultValue() {
                    return defaultValue;
                }

                @Override
                public Generic getReceiverType() {
                    if (isStatic()) {
                        return Generic.UNDEFINED;
                    } else if (isConstructor()) {
                        TypeDescription declaringType = getDeclaringType(), enclosingDeclaringType = declaringType.getEnclosingType();
                        if (enclosingDeclaringType == null) {
                            return declaringType.isGenerified()
                                    ? new LazyParameterizedReceiverType(declaringType)
                                    : new LazyNonGenericReceiverType(declaringType);
                        } else {
                            return !declaringType.isStatic() && declaringType.isGenerified()
                                    ? new LazyParameterizedReceiverType(enclosingDeclaringType)
                                    : new LazyNonGenericReceiverType(enclosingDeclaringType);
                        }
                    } else {
                        return LazyTypeDescription.this.isGenerified()
                                ? new LazyParameterizedReceiverType()
                                : new LazyNonGenericReceiverType();
                    }
                }

                @Override
                public String getGenericSignature() {
                    return genericSignature;
                }

                /**
                 * A lazy list of parameter descriptions for the enclosing method description.
                 */
                private class LazyParameterList extends ParameterList.AbstractBase<ParameterDescription.InDefinedShape> {

                    @Override
                    public ParameterDescription.InDefinedShape get(int index) {
                        return new LazyParameterDescription(index);
                    }

                    @Override
                    public boolean hasExplicitMetaData() {
                        for (int i = 0; i < size(); i++) {
                            if (parameterNames[i] == null || parameterModifiers[i] == null) {
                                return false;
                            }
                        }
                        return true;
                    }

                    @Override
                    public int size() {
                        return parameterTypeDescriptors.size();
                    }

                    @Override
                    public TypeList.Generic asTypeList() {
                        return signatureResolution.resolveParameterTypes(parameterTypeDescriptors, typePool, parameterTypeAnnotationTokens, LazyMethodDescription.this);
                    }
                }

                /**
                 * A lazy description of a parameters of the enclosing method.
                 */
                private class LazyParameterDescription extends ParameterDescription.InDefinedShape.AbstractBase {

                    /**
                     * The index of the described parameter.
                     */
                    private final int index;

                    /**
                     * Creates a new description for a given parameter of the enclosing method.
                     *
                     * @param index The index of the described parameter.
                     */
                    protected LazyParameterDescription(int index) {
                        this.index = index;
                    }

                    @Override
                    public MethodDescription.InDefinedShape getDeclaringMethod() {
                        return LazyMethodDescription.this;
                    }

                    @Override
                    public int getIndex() {
                        return index;
                    }

                    @Override
                    public boolean isNamed() {
                        return parameterNames[index] != null;
                    }

                    @Override
                    public boolean hasModifiers() {
                        return parameterModifiers[index] != null;
                    }

                    @Override
                    public String getName() {
                        return isNamed()
                                ? parameterNames[index]
                                : super.getName();
                    }

                    @Override
                    public int getModifiers() {
                        return hasModifiers()
                                ? parameterModifiers[index]
                                : super.getModifiers();
                    }

                    @Override
                    public Generic getType() {
                        return signatureResolution.resolveParameterTypes(parameterTypeDescriptors, typePool, parameterTypeAnnotationTokens, LazyMethodDescription.this).get(index);
                    }

                    @Override
                    public AnnotationList getDeclaredAnnotations() {
                        return LazyAnnotationDescription.asListOfNullable(typePool, parameterAnnotationTokens.get(index));
                    }
                }

                /**
                 * A lazy description of a parameterized receiver type.
                 */
                private class LazyParameterizedReceiverType extends Generic.OfParameterizedType {

                    /**
                     * The erasure of the type to be represented as a parameterized receiver type.
                     */
                    private final TypeDescription typeDescription;

                    /**
                     * Creates a new lazy parameterized receiver type of the method's declaring type.
                     */
                    protected LazyParameterizedReceiverType() {
                        this(LazyTypeDescription.this);
                    }

                    /**
                     * Creates a new lazy parameterized receiver type of the supplied receiver type.
                     *
                     * @param typeDescription The erasure of the type to be represented as a parameterized receiver type.
                     */
                    protected LazyParameterizedReceiverType(TypeDescription typeDescription) {
                        this.typeDescription = typeDescription;
                    }

                    @Override
                    public TypeList.Generic getTypeArguments() {
                        return new TypeArgumentList(typeDescription.getTypeVariables());
                    }

                    @Override
                    public Generic getOwnerType() {
                        TypeDescription declaringType = typeDescription.getDeclaringType();
                        if (declaringType == null) {
                            return Generic.UNDEFINED;
                        } else {
                            return !typeDescription.isStatic() && declaringType.isGenerified()
                                    ? new LazyParameterizedReceiverType(declaringType)
                                    : new LazyNonGenericReceiverType(declaringType);
                        }
                    }

                    @Override
                    public AnnotationList getDeclaredAnnotations() {
                        return LazyAnnotationDescription.asListOfNullable(typePool, receiverTypeAnnotationTokens.get(getTypePath()));
                    }

                    /**
                     * Returns the type path for this type.
                     *
                     * @return This type's type path.
                     */
                    private String getTypePath() {
                        StringBuilder typePath = new StringBuilder();
                        for (int index = 0; index < typeDescription.getInnerClassCount(); index++) {
                            typePath = typePath.append(GenericTypeToken.INNER_CLASS_PATH);
                        }
                        return typePath.toString();
                    }

                    @Override
                    public TypeDescription asErasure() {
                        return typeDescription;
                    }

                    /**
                     * A list of generic types representing the receiver type's type arguments.
                     */
                    protected class TypeArgumentList extends TypeList.Generic.AbstractBase {

                        /**
                         * The type variables of the represented receiver type.
                         */
                        private final List<? extends Generic> typeVariables;

                        /**
                         * Creates a new type argument list.
                         *
                         * @param typeVariables The type variables of the represented receiver type.
                         */
                        protected TypeArgumentList(List<? extends Generic> typeVariables) {
                            this.typeVariables = typeVariables;
                        }

                        @Override
                        public Generic get(int index) {
                            return new AnnotatedTypeVariable(typeVariables.get(index), index);
                        }

                        @Override
                        public int size() {
                            return typeVariables.size();
                        }

                        /**
                         * Represents a type variable as a type argument with type annotations.
                         */
                        protected class AnnotatedTypeVariable extends OfTypeVariable {

                            /**
                             * The type variable's description.
                             */
                            private final Generic typeVariable;

                            /**
                             * The type variable's index.
                             */
                            private final int index;

                            /**
                             * Creates a new description of an annotated type variable as a type argument.
                             *
                             * @param typeVariable The type variable's description.
                             * @param index        The type variable's index.
                             */
                            protected AnnotatedTypeVariable(Generic typeVariable, int index) {
                                this.typeVariable = typeVariable;
                                this.index = index;
                            }

                            @Override
                            public TypeList.Generic getUpperBounds() {
                                return typeVariable.getUpperBounds();
                            }

                            @Override
                            public TypeVariableSource getTypeVariableSource() {
                                return typeVariable.getTypeVariableSource();
                            }

                            @Override
                            public String getSymbol() {
                                return typeVariable.getSymbol();
                            }

                            @Override
                            public AnnotationList getDeclaredAnnotations() {
                                return LazyAnnotationDescription.asListOfNullable(typePool, receiverTypeAnnotationTokens.get(getTypePath()
                                        + index
                                        + GenericTypeToken.INDEXED_TYPE_DELIMITER));
                            }
                        }
                    }
                }

                /**
                 * A lazy description of a non-generic receiver type.
                 */
                protected class LazyNonGenericReceiverType extends Generic.OfNonGenericType {

                    /**
                     * The type description of the non-generic receiver type.
                     */
                    private final TypeDescription typeDescription;

                    /**
                     * Creates a new non-generic receiver type of the method's declaring type.
                     */
                    protected LazyNonGenericReceiverType() {
                        this(LazyTypeDescription.this);
                    }

                    /**
                     * Creates a new non-generic receiver type of the supplied type.
                     *
                     * @param typeDescription The type to represent as a non-generic receiver type.
                     */
                    protected LazyNonGenericReceiverType(TypeDescription typeDescription) {
                        this.typeDescription = typeDescription;
                    }

                    @Override
                    public Generic getOwnerType() {
                        TypeDescription declaringType = typeDescription.getDeclaringType();
                        return declaringType == null
                                ? Generic.UNDEFINED
                                : new LazyNonGenericReceiverType(declaringType);
                    }

                    @Override
                    public Generic getComponentType() {
                        return Generic.UNDEFINED;
                    }

                    @Override
                    public AnnotationList getDeclaredAnnotations() {
                        StringBuilder typePath = new StringBuilder();
                        for (int index = 0; index < typeDescription.getInnerClassCount(); index++) {
                            typePath = typePath.append(GenericTypeToken.INNER_CLASS_PATH);
                        }
                        return LazyAnnotationDescription.asListOfNullable(typePool, receiverTypeAnnotationTokens.get(typePath.toString()));
                    }

                    @Override
                    public TypeDescription asErasure() {
                        return typeDescription;
                    }
                }
            }
        }

        /**
         * A type extractor reads a class file and collects data that is relevant to create a type description.
         */
        protected class TypeExtractor extends ClassVisitor {

            /**
             * A mask that cuts off pseudo flags beyond the second byte that are inserted by ASM.
             */
            private static final int REAL_MODIFIER_MASK = 0xFFFF;

            /**
             * A mapping of the super types' type annotation tokens by their indices.
             */
            private final Map<Integer, Map<String, List<LazyTypeDescription.AnnotationToken>>> superTypeAnnotationTokens;

            /**
             * A mapping of the type variables' type annotation tokens by their indices.
             */
            private final Map<Integer, Map<String, List<LazyTypeDescription.AnnotationToken>>> typeVariableAnnotationTokens;

            /**
             * A mapping of the type variables' bounds' type annotation tokens by their indices and each variables index.
             */
            private final Map<Integer, Map<Integer, Map<String, List<LazyTypeDescription.AnnotationToken>>>> typeVariableBoundsAnnotationTokens;

            /**
             * A list of annotation tokens describing annotations that are found on the visited type.
             */
            private final List<LazyTypeDescription.AnnotationToken> annotationTokens;

            /**
             * A list of field tokens describing fields that are found on the visited type.
             */
            private final List<LazyTypeDescription.FieldToken> fieldTokens;

            /**
             * A list of method tokens describing annotations that are found on the visited type.
             */
            private final List<LazyTypeDescription.MethodToken> methodTokens;

            /**
             * The actual modifiers found for this type.
             */
            private int actualModifiers;

            /**
             * The modifiers found for this type.
             */
            private int modifiers;

            /**
             * The internal name found for this type.
             */
            private String internalName;

            /**
             * The internal name of the super type found for this type or {@code null} if no such type exists.
             */
            private String superClassName;

            /**
             * The generic signature of the type or {@code null} if it is not generic.
             */
            private String genericSignature;

            /**
             * A list of internal names of interfaces implemented by this type or {@code null} if no interfaces
             * are implemented.
             */
            private String[] interfaceName;

            /**
             * {@code true} if this type was found to represent an anonymous type.
             */
            private boolean anonymousType;

            /**
             * The declaration context found for this type.
             */
            private LazyTypeDescription.TypeContainment typeContainment;

            /**
             * The binary name of this type's declaring type or {@code null} if no such type exists.
             */
            private String declaringTypeName;

            /**
             * A list of descriptors representing the types that are declared by the parsed type.
             */
            private final List<String> declaredTypes;

            /**
             * Creates a new type extractor.
             */
            protected TypeExtractor() {
                super(Opcodes.ASM6);
                superTypeAnnotationTokens = new HashMap<Integer, Map<String, List<LazyTypeDescription.AnnotationToken>>>();
                typeVariableAnnotationTokens = new HashMap<Integer, Map<String, List<LazyTypeDescription.AnnotationToken>>>();
                typeVariableBoundsAnnotationTokens = new HashMap<Integer, Map<Integer, Map<String, List<LazyTypeDescription.AnnotationToken>>>>();
                annotationTokens = new ArrayList<LazyTypeDescription.AnnotationToken>();
                fieldTokens = new ArrayList<LazyTypeDescription.FieldToken>();
                methodTokens = new ArrayList<LazyTypeDescription.MethodToken>();
                anonymousType = false;
                typeContainment = LazyTypeDescription.TypeContainment.SelfContained.INSTANCE;
                declaredTypes = new ArrayList<String>();
            }

            @Override
            @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "The array is not to be modified by contract")
            public void visit(int classFileVersion,
                              int modifiers,
                              String internalName,
                              String genericSignature,
                              String superClassName,
                              String[] interfaceName) {
                this.modifiers = modifiers & REAL_MODIFIER_MASK;
                actualModifiers = modifiers;
                this.internalName = internalName;
                this.genericSignature = genericSignature;
                this.superClassName = superClassName;
                this.interfaceName = interfaceName;
            }

            @Override
            public void visitOuterClass(String typeName, String methodName, String methodDescriptor) {
                if (methodName != null) {
                    typeContainment = new LazyTypeDescription.TypeContainment.WithinMethod(typeName, methodName, methodDescriptor);
                } else if (typeName != null) {
                    typeContainment = new LazyTypeDescription.TypeContainment.WithinType(typeName, true);
                }
            }

            @Override
            public void visitInnerClass(String internalName, String outerName, String innerName, int modifiers) {
                if (internalName.equals(this.internalName)) {
                    this.modifiers = modifiers & REAL_MODIFIER_MASK;
                    if (innerName == null) {
                        anonymousType = true;
                    }
                    if (outerName != null) {
                        declaringTypeName = outerName;
                        if (typeContainment.isSelfContained()) {
                            typeContainment = new LazyTypeDescription.TypeContainment.WithinType(outerName, false);
                        }
                    }
                } else if (outerName != null && innerName != null && internalName.equals(this.internalName + "$" + innerName)) {
                    declaredTypes.add("L" + internalName + ";");
                }
            }

            @Override
            public AnnotationVisitor visitTypeAnnotation(int rawTypeReference, TypePath typePath, String descriptor, boolean visible) {
                AnnotationRegistrant annotationRegistrant;
                TypeReference typeReference = new TypeReference(rawTypeReference);
                switch (typeReference.getSort()) {
                    case TypeReference.CLASS_EXTENDS:
                        annotationRegistrant = new AnnotationRegistrant.ForTypeVariable.WithIndex(descriptor,
                                typePath,
                                typeReference.getSuperTypeIndex(),
                                superTypeAnnotationTokens);
                        break;
                    case TypeReference.CLASS_TYPE_PARAMETER:
                        annotationRegistrant = new AnnotationRegistrant.ForTypeVariable.WithIndex(descriptor,
                                typePath,
                                typeReference.getTypeParameterIndex(),
                                typeVariableAnnotationTokens);
                        break;
                    case TypeReference.CLASS_TYPE_PARAMETER_BOUND:
                        annotationRegistrant = new AnnotationRegistrant.ForTypeVariable.WithIndex.DoubleIndexed(descriptor,
                                typePath,
                                typeReference.getTypeParameterBoundIndex(),
                                typeReference.getTypeParameterIndex(),
                                typeVariableBoundsAnnotationTokens);
                        break;
                    default:
                        throw new IllegalArgumentException("Unexpected type reference: " + typeReference.getSort());
                }
                return new AnnotationExtractor(annotationRegistrant, new ComponentTypeLocator.ForAnnotationProperty(Default.this, descriptor));
            }

            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                return new AnnotationExtractor(descriptor, annotationTokens, new ComponentTypeLocator.ForAnnotationProperty(Default.this, descriptor));
            }

            @Override
            public FieldVisitor visitField(int modifiers, String internalName, String descriptor, String genericSignature, Object defaultValue) {
                return new FieldExtractor(modifiers & REAL_MODIFIER_MASK, internalName, descriptor, genericSignature);
            }

            @Override
            public MethodVisitor visitMethod(int modifiers, String internalName, String descriptor, String genericSignature, String[] exceptionName) {
                return internalName.equals(MethodDescription.TYPE_INITIALIZER_INTERNAL_NAME)
                        ? IGNORE_METHOD
                        : new MethodExtractor(modifiers & REAL_MODIFIER_MASK, internalName, descriptor, genericSignature, exceptionName);
            }

            /**
             * Creates a type description from all data that is currently collected. This method should only be invoked
             * after a class file was parsed fully.
             *
             * @return A type description reflecting the data that was collected by this instance.
             */
            protected TypeDescription toTypeDescription() {
                return new LazyTypeDescription(Default.this,
                        actualModifiers,
                        modifiers,
                        internalName,
                        superClassName,
                        interfaceName,
                        genericSignature,
                        typeContainment,
                        declaringTypeName,
                        declaredTypes,
                        anonymousType,
                        superTypeAnnotationTokens,
                        typeVariableAnnotationTokens,
                        typeVariableBoundsAnnotationTokens,
                        annotationTokens,
                        fieldTokens,
                        methodTokens);
            }

            /**
             * An annotation extractor reads an annotation found in a class field an collects data that
             * is relevant to creating a related annotation description.
             */
            protected class AnnotationExtractor extends AnnotationVisitor {

                /**
                 * The annotation registrant to register found annotation values on.
                 */
                private final AnnotationRegistrant annotationRegistrant;

                /**
                 * A locator for the component type of any found annotation value.
                 */
                private final ComponentTypeLocator componentTypeLocator;

                /**
                 * Creates a new annotation extractor for a byte code element without an index.
                 *
                 * @param descriptor           The annotation descriptor.
                 * @param annotationTokens     The collection for storing any discovered annotation tokens.
                 * @param componentTypeLocator The component type locator to use.
                 */
                protected AnnotationExtractor(String descriptor, List<LazyTypeDescription.AnnotationToken> annotationTokens, ComponentTypeLocator componentTypeLocator) {
                    this(new AnnotationRegistrant.ForByteCodeElement(descriptor, annotationTokens), componentTypeLocator);
                }

                /**
                 * Creates a new annotation extractor for a byte code element with an index.
                 *
                 * @param descriptor           The annotation descriptor.
                 * @param index                The index of the element for which the annotations are collected.
                 * @param annotationTokens     The collection for storing any discovered annotation tokens.
                 * @param componentTypeLocator The component type locator to use.
                 */
                protected AnnotationExtractor(String descriptor,
                                              int index,
                                              Map<Integer, List<LazyTypeDescription.AnnotationToken>> annotationTokens,
                                              ComponentTypeLocator componentTypeLocator) {
                    this(new AnnotationRegistrant.ForByteCodeElement.WithIndex(descriptor, index, annotationTokens), componentTypeLocator);
                }

                /**
                 * Creates a new annotation extractor.
                 *
                 * @param annotationRegistrant The annotation registrant to register found annotation values on.
                 * @param componentTypeLocator A locator for the component type of any found annotation value.
                 */
                protected AnnotationExtractor(AnnotationRegistrant annotationRegistrant, ComponentTypeLocator componentTypeLocator) {
                    super(Opcodes.ASM6);
                    this.annotationRegistrant = annotationRegistrant;
                    this.componentTypeLocator = componentTypeLocator;
                }

                @Override
                public void visit(String name, Object value) {
                    annotationRegistrant.register(name, value instanceof Type
                            ? new RawTypeValue(Default.this, (Type) value)
                            : AnnotationValue.ForConstant.of(value));
                }

                @Override
                public void visitEnum(String name, String descriptor, String value) {
                    annotationRegistrant.register(name, new RawEnumerationValue(Default.this, descriptor, value));
                }

                @Override
                public AnnotationVisitor visitAnnotation(String name, String descriptor) {
                    return new AnnotationExtractor(new AnnotationLookup(descriptor, name),
                            new ComponentTypeLocator.ForAnnotationProperty(TypePool.Default.this, descriptor));
                }

                @Override
                public AnnotationVisitor visitArray(String name) {
                    return new AnnotationExtractor(new ArrayLookup(name, componentTypeLocator.bind(name)), ComponentTypeLocator.Illegal.INSTANCE);
                }

                @Override
                public void visitEnd() {
                    annotationRegistrant.onComplete();
                }

                /**
                 * An annotation registrant for registering values of an array.
                 */
                protected class ArrayLookup implements AnnotationRegistrant {

                    /**
                     * The name of the annotation property the collected array is representing.
                     */
                    private final String name;

                    /**
                     * A lazy reference to resolve the component type of the collected array.
                     */
                    private final RawDescriptionArray.ComponentTypeReference componentTypeReference;

                    /**
                     * A list of all annotation values that are found on this array.
                     */
                    private final List<AnnotationValue<?, ?>> values;

                    /**
                     * Creates a new annotation registrant for an array lookup.
                     *
                     * @param name                   The name of the annotation property the collected array is representing.
                     * @param componentTypeReference A lazy reference to resolve the component type of the collected array.
                     */
                    protected ArrayLookup(String name, RawDescriptionArray.ComponentTypeReference componentTypeReference) {
                        this.name = name;
                        this.componentTypeReference = componentTypeReference;
                        values = new ArrayList<AnnotationValue<?, ?>>();
                    }

                    @Override
                    public void register(String ignored, AnnotationValue<?, ?> annotationValue) {
                        values.add(annotationValue);
                    }

                    @Override
                    public void onComplete() {
                        annotationRegistrant.register(name, new RawDescriptionArray(Default.this, componentTypeReference, values));
                    }
                }

                /**
                 * An annotation registrant for registering the values on an array that is itself an annotation property.
                 */
                protected class AnnotationLookup implements AnnotationRegistrant {

                    /**
                     * The descriptor of the original annotation for which the annotation values are looked up.
                     */
                    private final String descriptor;

                    /**
                     * The name of the original annotation for which the annotation values are looked up.
                     */
                    private final String name;

                    /**
                     * This annotation's values mapped by their attribute name.
                     */
                    private final Map<String, AnnotationValue<?, ?>> values;

                    /**
                     * Creates a new annotation registrant for a recursive annotation lookup.
                     *
                     * @param name       The name of the original annotation for which the annotation values are looked up.
                     * @param descriptor The descriptor of the original annotation for which the annotation values are looked up.
                     */
                    protected AnnotationLookup(String descriptor, String name) {
                        this.descriptor = descriptor;
                        this.name = name;
                        values = new HashMap<String, AnnotationValue<?, ?>>();
                    }

                    @Override
                    public void register(String name, AnnotationValue<?, ?> annotationValue) {
                        values.put(name, annotationValue);
                    }

                    @Override
                    public void onComplete() {
                        annotationRegistrant.register(name, new RawAnnotationValue(Default.this, new LazyTypeDescription.AnnotationToken(descriptor, values)));
                    }
                }
            }

            /**
             * A field extractor reads a field within a class file and collects data that is relevant
             * to creating a related field description.
             */
            protected class FieldExtractor extends FieldVisitor {

                /**
                 * The modifiers found on the field.
                 */
                private final int modifiers;

                /**
                 * The name of the field.
                 */
                private final String internalName;

                /**
                 * The descriptor of the field type.
                 */
                private final String descriptor;

                /**
                 * The generic signature of the field or {@code null} if it is not generic.
                 */
                private final String genericSignature;

                /**
                 * A mapping of the field type's type annotations.
                 */
                private final Map<String, List<LazyTypeDescription.AnnotationToken>> typeAnnotationTokens;

                /**
                 * A list of annotation tokens found for this field.
                 */
                private final List<LazyTypeDescription.AnnotationToken> annotationTokens;

                /**
                 * Creates a new field extractor.
                 *
                 * @param modifiers        The modifiers found for this field.
                 * @param internalName     The name of the field.
                 * @param descriptor       The descriptor of the field type.
                 * @param genericSignature The generic signature of the field or {@code null} if it is not generic.
                 */
                protected FieldExtractor(int modifiers,
                                         String internalName,
                                         String descriptor,
                                         String genericSignature) {
                    super(Opcodes.ASM6);
                    this.modifiers = modifiers;
                    this.internalName = internalName;
                    this.descriptor = descriptor;
                    this.genericSignature = genericSignature;
                    typeAnnotationTokens = new HashMap<String, List<LazyTypeDescription.AnnotationToken>>();
                    annotationTokens = new ArrayList<LazyTypeDescription.AnnotationToken>();
                }

                @Override
                public AnnotationVisitor visitTypeAnnotation(int rawTypeReference, TypePath typePath, String descriptor, boolean visible) {
                    AnnotationRegistrant annotationRegistrant;
                    TypeReference typeReference = new TypeReference(rawTypeReference);
                    switch (typeReference.getSort()) {
                        case TypeReference.FIELD:
                            annotationRegistrant = new AnnotationRegistrant.ForTypeVariable(descriptor, typePath, typeAnnotationTokens);
                            break;
                        default:
                            throw new IllegalStateException("Unexpected type reference on field: " + typeReference.getSort());
                    }
                    return new AnnotationExtractor(annotationRegistrant, new ComponentTypeLocator.ForAnnotationProperty(Default.this, descriptor));
                }

                @Override
                public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                    return new AnnotationExtractor(descriptor, annotationTokens, new ComponentTypeLocator.ForAnnotationProperty(Default.this, descriptor));
                }

                @Override
                public void visitEnd() {
                    fieldTokens.add(new LazyTypeDescription.FieldToken(internalName,
                            modifiers,
                            descriptor,
                            genericSignature,
                            typeAnnotationTokens,
                            annotationTokens));
                }
            }

            /**
             * A method extractor reads a method within a class file and collects data that is relevant
             * to creating a related method description.
             */
            protected class MethodExtractor extends MethodVisitor implements AnnotationRegistrant {

                /**
                 * The modifiers found for this method.
                 */
                private final int modifiers;

                /**
                 * The internal name found for this method.
                 */
                private final String internalName;

                /**
                 * The descriptor found for this method.
                 */
                private final String descriptor;

                /**
                 * The generic signature of the method or {@code null} if it is not generic.
                 */
                private final String genericSignature;

                /**
                 * An array of internal names of the exceptions of the found method
                 * or {@code null} if there are no such exceptions.
                 */
                private final String[] exceptionName;

                /**
                 * A mapping of the method's type variables' type annotations by their indices.
                 */
                private final Map<Integer, Map<String, List<LazyTypeDescription.AnnotationToken>>> typeVariableAnnotationTokens;

                /**
                 * A mapping of the method's type variables' bounds' type annotations by their indices and each variable's index.
                 */
                private final Map<Integer, Map<Integer, Map<String, List<LazyTypeDescription.AnnotationToken>>>> typeVariableBoundAnnotationTokens;

                /**
                 * A mapping of the method's return type's type annotations.
                 */
                private final Map<String, List<LazyTypeDescription.AnnotationToken>> returnTypeAnnotationTokens;

                /**
                 * A mapping of the parameters' type annotations by their indices.
                 */
                private final Map<Integer, Map<String, List<LazyTypeDescription.AnnotationToken>>> parameterTypeAnnotationTokens;

                /**
                 * A mapping of the exception types' type annotations by their indices.
                 */
                private final Map<Integer, Map<String, List<LazyTypeDescription.AnnotationToken>>> exceptionTypeAnnotationTokens;

                /**
                 * A mapping of the receiver type's type annotations.
                 */
                private final Map<String, List<LazyTypeDescription.AnnotationToken>> receiverTypeAnnotationTokens;

                /**
                 * A list of annotation tokens declared on the found method.
                 */
                private final List<LazyTypeDescription.AnnotationToken> annotationTokens;

                /**
                 * A mapping of parameter indices to annotation tokens found for the parameters at these indices.
                 */
                private final Map<Integer, List<LazyTypeDescription.AnnotationToken>> parameterAnnotationTokens;

                /**
                 * A list of tokens representing meta information of a parameter as it is available for method's
                 * that are compiled in the Java 8 version format.
                 */
                private final List<LazyTypeDescription.MethodToken.ParameterToken> parameterTokens;

                /**
                 * A bag of parameter meta information representing debugging information which allows to extract
                 * a method's parameter names.
                 */
                private final ParameterBag legacyParameterBag;

                /**
                 * The first label that is found in the method's body, if any, denoting the start of the method.
                 * This label can be used to identify names of local variables that describe the method's parameters.
                 */
                private Label firstLabel;

                /**
                 * A shift index for visible parameters that indicates a deviation of the actual parameter index.
                 */
                private int visibleParameterShift;

                /**
                 * A shift index for invisible parameters that indicates a deviation of the actual parameter index.
                 */
                private int invisibleParameterShift;

                /**
                 * The default value of the found method or {@code null} if no such value exists.
                 */
                private AnnotationValue<?, ?> defaultValue;

                /**
                 * Creates a method extractor.
                 *
                 * @param modifiers        The modifiers found for this method.
                 * @param internalName     The internal name found for this method.
                 * @param descriptor       The descriptor found for this method.
                 * @param genericSignature The generic signature of the method or {@code null} if it is not generic.
                 * @param exceptionName    An array of internal names of the exceptions of the found method
                 *                         or {@code null} if there are no such exceptions.
                 */
                protected MethodExtractor(int modifiers,
                                          String internalName,
                                          String descriptor,
                                          String genericSignature,
                                          String[] exceptionName) {
                    super(Opcodes.ASM6);
                    this.modifiers = modifiers;
                    this.internalName = internalName;
                    this.descriptor = descriptor;
                    this.genericSignature = genericSignature;
                    this.exceptionName = exceptionName;
                    typeVariableAnnotationTokens = new HashMap<Integer, Map<String, List<LazyTypeDescription.AnnotationToken>>>();
                    typeVariableBoundAnnotationTokens = new HashMap<Integer, Map<Integer, Map<String, List<LazyTypeDescription.AnnotationToken>>>>();
                    returnTypeAnnotationTokens = new HashMap<String, List<LazyTypeDescription.AnnotationToken>>();
                    parameterTypeAnnotationTokens = new HashMap<Integer, Map<String, List<LazyTypeDescription.AnnotationToken>>>();
                    exceptionTypeAnnotationTokens = new HashMap<Integer, Map<String, List<LazyTypeDescription.AnnotationToken>>>();
                    receiverTypeAnnotationTokens = new HashMap<String, List<LazyTypeDescription.AnnotationToken>>();
                    annotationTokens = new ArrayList<LazyTypeDescription.AnnotationToken>();
                    parameterAnnotationTokens = new HashMap<Integer, List<LazyTypeDescription.AnnotationToken>>();
                    parameterTokens = new ArrayList<LazyTypeDescription.MethodToken.ParameterToken>();
                    legacyParameterBag = new ParameterBag(Type.getMethodType(descriptor).getArgumentTypes());
                }

                @Override
                public AnnotationVisitor visitTypeAnnotation(int rawTypeReference, TypePath typePath, String descriptor, boolean visible) {
                    AnnotationRegistrant annotationRegistrant;
                    TypeReference typeReference = new TypeReference(rawTypeReference);
                    switch (typeReference.getSort()) {
                        case TypeReference.METHOD_TYPE_PARAMETER:
                            annotationRegistrant = new ForTypeVariable.WithIndex(descriptor,
                                    typePath,
                                    typeReference.getTypeParameterIndex(),
                                    typeVariableAnnotationTokens);
                            break;
                        case TypeReference.METHOD_TYPE_PARAMETER_BOUND:
                            annotationRegistrant = new ForTypeVariable.WithIndex.DoubleIndexed(descriptor,
                                    typePath,
                                    typeReference.getTypeParameterBoundIndex(),
                                    typeReference.getTypeParameterIndex(),
                                    typeVariableBoundAnnotationTokens);
                            break;
                        case TypeReference.METHOD_RETURN:
                            annotationRegistrant = new ForTypeVariable(descriptor,
                                    typePath,
                                    returnTypeAnnotationTokens);
                            break;
                        case TypeReference.METHOD_FORMAL_PARAMETER:
                            annotationRegistrant = new ForTypeVariable.WithIndex(descriptor,
                                    typePath,
                                    typeReference.getFormalParameterIndex(),
                                    parameterTypeAnnotationTokens);
                            break;
                        case TypeReference.THROWS:
                            annotationRegistrant = new ForTypeVariable.WithIndex(descriptor,
                                    typePath,
                                    typeReference.getExceptionIndex(),
                                    exceptionTypeAnnotationTokens);
                            break;
                        case TypeReference.METHOD_RECEIVER:
                            annotationRegistrant = new ForTypeVariable(descriptor,
                                    typePath,
                                    receiverTypeAnnotationTokens);
                            break;
                        default:
                            throw new IllegalStateException("Unexpected type reference on method: " + typeReference.getSort());
                    }
                    return new AnnotationExtractor(annotationRegistrant, new ComponentTypeLocator.ForAnnotationProperty(Default.this, descriptor));
                }

                @Override
                public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                    return new AnnotationExtractor(descriptor, annotationTokens, new ComponentTypeLocator.ForAnnotationProperty(Default.this, descriptor));
                }

                @Override
                public void visitAnnotableParameterCount(int count, boolean visible) {
                    if (visible) {
                        visibleParameterShift = Type.getMethodType(descriptor).getArgumentTypes().length - count;
                    } else {
                        invisibleParameterShift = Type.getMethodType(descriptor).getArgumentTypes().length - count;
                    }
                }

                @Override
                public AnnotationVisitor visitParameterAnnotation(int index, String descriptor, boolean visible) {
                    return new AnnotationExtractor(descriptor,
                            index + (visible ? visibleParameterShift : invisibleParameterShift),
                            parameterAnnotationTokens,
                            new ComponentTypeLocator.ForAnnotationProperty(Default.this, descriptor));
                }

                @Override
                public void visitLabel(Label label) {
                    if (readerMode.isExtended() && firstLabel == null) {
                        firstLabel = label;
                    }
                }

                @Override
                public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
                    if (readerMode.isExtended() && start == firstLabel) {
                        legacyParameterBag.register(index, name);
                    }
                }

                @Override
                public void visitParameter(String name, int modifiers) {
                    parameterTokens.add(new LazyTypeDescription.MethodToken.ParameterToken(name, modifiers));
                }

                @Override
                public AnnotationVisitor visitAnnotationDefault() {
                    return new AnnotationExtractor(this, new ComponentTypeLocator.ForArrayType(descriptor));
                }

                @Override
                public void register(String ignored, AnnotationValue<?, ?> annotationValue) {
                    defaultValue = annotationValue;
                }

                @Override
                public void onComplete() {
                    /* do nothing, as the register method is called at most once for default values */
                }

                @Override
                public void visitEnd() {
                    methodTokens.add(new LazyTypeDescription.MethodToken(internalName,
                            modifiers,
                            descriptor,
                            genericSignature,
                            exceptionName,
                            typeVariableAnnotationTokens,
                            typeVariableBoundAnnotationTokens,
                            returnTypeAnnotationTokens,
                            parameterTypeAnnotationTokens,
                            exceptionTypeAnnotationTokens,
                            receiverTypeAnnotationTokens,
                            annotationTokens,
                            parameterAnnotationTokens,
                            parameterTokens.isEmpty()
                                    ? legacyParameterBag.resolve((modifiers & Opcodes.ACC_STATIC) != 0)
                                    : parameterTokens,
                            defaultValue));
                }
            }
        }
    }

    /**
     * A lazy facade of a type pool that delegates any lookups to another type pool only if another value than the type's name is looked up.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class LazyFacade extends AbstractBase {

        /**
         * The type pool to delegate to.
         */
        private final TypePool typePool;

        /**
         * Creates a lazy facade for a type pool.
         *
         * @param typePool The type pool to delegate to.
         */
        public LazyFacade(TypePool typePool) {
            super(CacheProvider.NoOp.INSTANCE);
            this.typePool = typePool;
        }

        @Override
        protected Resolution doDescribe(String name) {
            return new LazyResolution(typePool, name);
        }

        @Override
        public void clear() {
            typePool.clear();
        }

        /**
         * The lazy resolution for a lazy facade for a type pool.
         */
        @HashCodeAndEqualsPlugin.Enhance
        protected static class LazyResolution implements Resolution {

            /**
             * The type pool to delegate to.
             */
            private final TypePool typePool;

            /**
             * The name of the type that is represented by this resolution.
             */
            private final String name;

            /**
             * Creates a lazy resolution for a lazy facade for a type pool.
             *
             * @param typePool The type pool to delegate to.
             * @param name     The name of the type that is represented by this resolution.
             */
            protected LazyResolution(TypePool typePool, String name) {
                this.typePool = typePool;
                this.name = name;
            }

            @Override
            public boolean isResolved() {
                return typePool.describe(name).isResolved();
            }

            @Override
            public TypeDescription resolve() {
                return new LazyTypeDescription(typePool, name);
            }
        }

        /**
         * A description of a type that delegates to another type pool once a property that is not the name is resolved.
         */
        protected static class LazyTypeDescription extends TypeDescription.AbstractBase.OfSimpleType.WithDelegation {

            /**
             * The type pool to delegate to.
             */
            private final TypePool typePool;

            /**
             * The name of the type that is represented by this resolution.
             */
            private final String name;

            /**
             * Creates a new lazy type resolution.
             *
             * @param typePool The type pool to delegate to.
             * @param name     The name of the type.
             */
            protected LazyTypeDescription(TypePool typePool, String name) {
                this.typePool = typePool;
                this.name = name;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            protected TypeDescription delegate() {
                return typePool.describe(name).resolve();
            }
        }
    }

    /**
     * A type pool that attempts to load a class.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class ClassLoading extends AbstractBase.Hierarchical {

        /**
         * Type-safe representation of the bootstrap class loader which is {@code null}.
         */
        private static final ClassLoader BOOTSTRAP_CLASS_LOADER = null;

        /**
         * The class loader to query.
         */
        private final ClassLoader classLoader;

        /**
         * Creates a class loadings type pool.
         *
         * @param cacheProvider The cache provider to use.
         * @param parent        The parent type pool.
         * @param classLoader   The class loader to use for locating files.
         */
        public ClassLoading(CacheProvider cacheProvider, TypePool parent, ClassLoader classLoader) {
            super(cacheProvider, parent);
            this.classLoader = classLoader;
        }

        /**
         * Returns a type pool that attempts type descriptions by loadings types from the given class loader.
         *
         * @param classLoader The class loader to use.
         * @return An class loading type pool.
         */
        public static TypePool of(ClassLoader classLoader) {
            return of(classLoader, Empty.INSTANCE);
        }

        /**
         * Returns a type pool that attempts type descriptions by loadings types from the given class loader.
         *
         * @param classLoader The class loader to use.
         * @param parent      The parent type pool to use.
         * @return An class loading type pool.
         */
        public static TypePool of(ClassLoader classLoader, TypePool parent) {
            return new ClassLoading(CacheProvider.NoOp.INSTANCE, parent, classLoader);
        }

        /**
         * Returns a type pool that attempts type descriptions by loadings types from the bootstrap class loader.
         *
         * @return An class loading type pool for the bootstrap class loader.
         */
        public static TypePool ofBootPath() {
            return of(BOOTSTRAP_CLASS_LOADER);
        }

        /**
         * Returns a type pool that attempts type descriptions by loadings types from the system class loader.
         *
         * @return An class loading type pool for the system class loader.
         */
        public static TypePool ofClassPath() {
            return of(ClassLoader.getSystemClassLoader());
        }

        @Override
        public Resolution doDescribe(String name) {
            try {
                return new Resolution.Simple(new TypeDescription.ForLoadedType(Class.forName(name, false, classLoader)));
            } catch (ClassNotFoundException ignored) {
                return new Resolution.Illegal(name);
            }
        }
    }

    /**
     * A type pool that supplies explicitly known type descriptions.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class Explicit extends AbstractBase.Hierarchical {

        /**
         * A mapping from type names to type descriptions of that name.
         */
        private final Map<String, TypeDescription> types;

        /**
         * Creates a new explicit type pool without a parent.
         *
         * @param types A mapping from type names to type descriptions of that name.
         */
        public Explicit(Map<String, TypeDescription> types) {
            this(Empty.INSTANCE, types);
        }

        /**
         * Creates a new explicit type pool.
         *
         * @param parent The parent type pool.
         * @param types  A mapping from type names to type descriptions of that name.
         */
        public Explicit(TypePool parent, Map<String, TypeDescription> types) {
            super(CacheProvider.NoOp.INSTANCE, parent);
            this.types = types;
        }

        @Override
        protected Resolution doDescribe(String name) {
            TypeDescription typeDescription = types.get(name);
            return typeDescription == null
                    ? new Resolution.Illegal(name)
                    : new Resolution.Simple(typeDescription);
        }
    }
}
