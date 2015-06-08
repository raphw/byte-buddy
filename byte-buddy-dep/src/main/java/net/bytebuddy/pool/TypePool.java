package net.bytebuddy.pool;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
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
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeList;
import net.bytebuddy.description.type.generic.TypeVariableSource;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.matcher.FilterableList;
import net.bytebuddy.utility.PropertyDispatcher;
import org.objectweb.asm.*;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.MalformedParameterizedTypeException;
import java.lang.reflect.Proxy;
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
     * {@link net.bytebuddy.pool.TypePool.Resolution} represents this type. Otherwise, an illegal resolution is
     * returned.
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
         * Determines if this resolution represents a {@link TypeDescription}.
         *
         * @return {@code true} if the queried type could be resolved.
         */
        boolean isResolved();

        /**
         * Resolves this resolution to the represented type description. This method must only be invoked if this
         * instance could be resolved. Otherwise, an exception is thrown on calling this method.
         *
         * @return The type description that is represented by this resolution.
         */
        TypeDescription resolve();

        /**
         * A simple resolution that represents a given {@link TypeDescription}.
         */
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

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && typeDescription.equals(((Simple) other).typeDescription);
            }

            @Override
            public int hashCode() {
                return typeDescription.hashCode();
            }

            @Override
            public String toString() {
                return "TypePool.Resolution.Simple{" +
                        "typeDescription=" + typeDescription +
                        '}';
            }
        }

        /**
         * A canonical representation of a non-successful resolution of a {@link net.bytebuddy.pool.TypePool}.
         */
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

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && name.equals(((Illegal) other).name);
            }

            @Override
            public int hashCode() {
                return name.hashCode();
            }

            @Override
            public String toString() {
                return "TypePool.Resolution.Illegal{" +
                        "name='" + name + '\'' +
                        '}';
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
        Resolution NOTHING = null;

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
                return NOTHING;
            }

            @Override
            public Resolution register(String name, Resolution resolution) {
                return resolution;
            }

            @Override
            public void clear() {
                /* do nothing */
            }

            @Override
            public String toString() {
                return "TypePool.CacheProvider.NoOp." + name();
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

            @Override
            public Resolution find(String name) {
                return cache.get(name);
            }

            @Override
            public Resolution register(String name, Resolution resolution) {
                Resolution cached = cache.putIfAbsent(name, resolution);
                return cached == NOTHING ? resolution : cached;
            }

            @Override
            public void clear() {
                cache.clear();
            }

            @Override
            public String toString() {
                return "TypePool.CacheProvider.Simple{cache=" + cache + '}';
            }
        }
    }

    /**
     * A base implementation of a {@link net.bytebuddy.pool.TypePool} that is managing a cache provider and
     * that handles the description of array and primitive types.
     */
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
                name = primitiveName == null ? name.substring(1, name.length() - 1) : primitiveName;
            }
            TypeDescription typeDescription = PRIMITIVE_TYPES.get(name);
            Resolution resolution = typeDescription == null
                    ? cacheProvider.find(name)
                    : new Resolution.Simple(typeDescription);
            if (resolution == null) {
                resolution = cacheProvider.register(name, doDescribe(name));
            }
            return ArrayTypeResolution.of(resolution, arity);
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

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && cacheProvider.equals(((AbstractBase) other).cacheProvider);
        }

        @Override
        public int hashCode() {
            return cacheProvider.hashCode();
        }

        /**
         * A resolution for a type that, if resolved, represents an array type.
         */
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

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && arity == ((ArrayTypeResolution) other).arity
                        && resolution.equals(((ArrayTypeResolution) other).resolution);
            }

            @Override
            public int hashCode() {
                int result = resolution.hashCode();
                result = 31 * result + arity;
                return result;
            }

            @Override
            public String toString() {
                return "TypePool.AbstractBase.ArrayTypeResolution{" +
                        "resolution=" + resolution +
                        ", arity=" + arity +
                        '}';
            }
        }

        /**
         * Represents a nested annotation value.
         */
        protected static class RawAnnotationValue implements AnnotationDescription.AnnotationValue<AnnotationDescription, Annotation> {

            /**
             * The type pool to use for looking up types.
             */
            private final TypePool typePool;

            /**
             * The annotation token that represents the nested invocation.
             */
            private final LazyTypeDescription.AnnotationToken annotationToken;

            /**
             * Creates a new annotation value for a nested annotation.
             *
             * @param typePool        The type pool to use for looking up types.
             * @param annotationToken The token that represents the annotation.
             */
            public RawAnnotationValue(TypePool typePool, LazyTypeDescription.AnnotationToken annotationToken) {
                this.typePool = typePool;
                this.annotationToken = annotationToken;
            }

            @Override
            public AnnotationDescription resolve() {
                return annotationToken.toAnnotationDescription(typePool);
            }

            @Override
            @SuppressWarnings("unchecked")
            public Loaded<Annotation> load(ClassLoader classLoader) throws ClassNotFoundException {
                Class<?> type = classLoader.loadClass(annotationToken.getDescriptor()
                        .substring(1, annotationToken.getDescriptor().length() - 1)
                        .replace('/', '.'));
                if (type.isAnnotation()) {
                    return new ForAnnotation.Loaded<Annotation>((Annotation) Proxy.newProxyInstance(classLoader,
                            new Class<?>[]{type},
                            AnnotationDescription.AnnotationInvocationHandler.of(classLoader,
                                    (Class<? extends Annotation>) type,
                                    annotationToken.getValues())));
                } else {
                    return new ForAnnotation.IncompatibleRuntimeType(type);
                }
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && annotationToken.equals(((RawAnnotationValue) other).annotationToken);
            }

            @Override
            public int hashCode() {
                return annotationToken.hashCode();
            }

            @Override
            public String toString() {
                return "TypePool.AbstractBase.RawAnnotationValue{" +
                        "annotationToken=" + annotationToken +
                        '}';
            }
        }

        /**
         * Represents an enumeration value of an annotation.
         */
        protected static class RawEnumerationValue implements AnnotationDescription.AnnotationValue<EnumerationDescription, Enum<?>> {

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
                Class<?> type = classLoader.loadClass(descriptor.substring(1, descriptor.length() - 1).replace('/', '.'));
                try {
                    return type.isEnum()
                            ? new ForEnumeration.Loaded(Enum.valueOf((Class) type, value))
                            : new ForEnumeration.IncompatibleRuntimeType(type);
                } catch (IllegalArgumentException ignored) {
                    return new ForEnumeration.UnknownRuntimeEnumeration((Class) type, value);
                }
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && descriptor.equals(((RawEnumerationValue) other).descriptor)
                        && value.equals(((RawEnumerationValue) other).value);
            }

            @Override
            public int hashCode() {
                return 31 * descriptor.hashCode() + value.hashCode();
            }

            @Override
            public String toString() {
                return "TypePool.LazyTypeDescription.AnnotationValue.ForEnumeration{" +
                        "descriptor='" + descriptor + '\'' +
                        ", value='" + value + '\'' +
                        '}';
            }

            /**
             * An enumeration description where any type references are only resolved on demand.
             */
            protected class LazyEnumerationDescription extends EnumerationDescription.AbstractEnumerationDescription {

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
        protected static class RawTypeValue implements AnnotationDescription.AnnotationValue<TypeDescription, Class<?>> {

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
            public RawTypeValue(TypePool typePool, Type type) {
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
            public AnnotationDescription.AnnotationValue.Loaded<Class<?>> load(ClassLoader classLoader) throws ClassNotFoundException {
                return new Loaded(Class.forName(name, NO_INITIALIZATION, classLoader));
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && name.equals(((RawTypeValue) other).name);
            }

            @Override
            public int hashCode() {
                return name.hashCode();
            }

            @Override
            public String toString() {
                return "TypePool.LazyTypeDescription.AnnotationValue.ForType{" +
                        "name='" + name + '\'' +
                        '}';
            }

            /**
             * Represents a loaded annotation property that represents a type.
             */
            protected class Loaded implements AnnotationDescription.AnnotationValue.Loaded<Class<?>> {

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
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (!(other instanceof AnnotationDescription.AnnotationValue.Loaded<?>)) return false;
                    AnnotationDescription.AnnotationValue.Loaded<?> loadedOther = (AnnotationDescription.AnnotationValue.Loaded<?>) other;
                    return loadedOther.getState().isResolved() && type.equals(loadedOther.resolve());
                }

                @Override
                public int hashCode() {
                    return type.hashCode();
                }

                @Override
                public String toString() {
                    return type.toString();
                }
            }
        }

        /**
         * Represents an array that is referenced by an annotation which does not contain primitive values.
         */
        protected static class RawNonPrimitiveArray implements AnnotationDescription.AnnotationValue<Object[], Object[]> {

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
            private List<AnnotationDescription.AnnotationValue<?, ?>> value;

            /**
             * Creates a new array value representation of a complex array.
             *
             * @param typePool               The type pool to use for looking up types.
             * @param componentTypeReference A lazy reference to the component type of this array.
             * @param value                  A list of all values of this annotation.
             */
            public RawNonPrimitiveArray(TypePool typePool,
                                        ComponentTypeReference componentTypeReference,
                                        List<AnnotationDescription.AnnotationValue<?, ?>> value) {
                this.typePool = typePool;
                this.value = value;
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
                Object[] array = (Object[]) Array.newInstance(componentType, value.size());
                int index = 0;
                for (AnnotationDescription.AnnotationValue<?, ?> annotationValue : value) {
                    Array.set(array, index++, annotationValue.resolve());
                }
                return array;
            }

            @Override
            public AnnotationDescription.AnnotationValue.Loaded<Object[]> load(ClassLoader classLoader) throws ClassNotFoundException {
                List<AnnotationDescription.AnnotationValue.Loaded<?>> loadedValues = new ArrayList<AnnotationDescription.AnnotationValue.Loaded<?>>(value.size());
                for (AnnotationDescription.AnnotationValue<?, ?> value : this.value) {
                    loadedValues.add(value.load(classLoader));
                }
                return new Loaded(classLoader.loadClass(componentTypeReference.lookup()), loadedValues);
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && componentTypeReference.equals(((RawNonPrimitiveArray) other).componentTypeReference)
                        && value.equals(((RawNonPrimitiveArray) other).value);
            }

            @Override
            public int hashCode() {
                return 31 * value.hashCode() + componentTypeReference.hashCode();
            }

            @Override
            public String toString() {
                return "TypePool.LazyTypeDescription.AnnotationValue.ForComplexArray{" +
                        "value=" + value +
                        ", componentTypeReference=" + componentTypeReference +
                        '}';
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
            protected class Loaded implements AnnotationDescription.AnnotationValue.Loaded<Object[]> {

                /**
                 * The array's loaded component type.
                 */
                private final Class<?> componentType;

                /**
                 * A list of loaded values of the represented complex array.
                 */
                private final List<AnnotationDescription.AnnotationValue.Loaded<?>> values;

                /**
                 * Creates a new representation of an annotation property representing an array of
                 * non-trivial values.
                 *
                 * @param componentType The array's loaded component type.
                 * @param values        A list of loaded values of the represented complex array.
                 */
                public Loaded(Class<?> componentType, List<AnnotationDescription.AnnotationValue.Loaded<?>> values) {
                    this.componentType = componentType;
                    this.values = values;
                }

                @Override
                public State getState() {
                    for (AnnotationDescription.AnnotationValue.Loaded<?> value : values) {
                        if (!value.getState().isResolved()) {
                            return State.NON_RESOLVED;
                        }
                    }
                    return State.RESOLVED;
                }

                @Override
                public Object[] resolve() {
                    Object[] array = (Object[]) Array.newInstance(componentType, values.size());
                    int index = 0;
                    for (AnnotationDescription.AnnotationValue.Loaded<?> annotationValue : values) {
                        Array.set(array, index++, annotationValue.resolve());
                    }
                    return array;
                }

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (!(other instanceof AnnotationDescription.AnnotationValue.Loaded<?>)) return false;
                    AnnotationDescription.AnnotationValue.Loaded<?> loadedOther = (AnnotationDescription.AnnotationValue.Loaded<?>) other;
                    if (!loadedOther.getState().isResolved()) return false;
                    Object otherValue = loadedOther.resolve();
                    if (!(otherValue instanceof Object[])) return false;
                    Object[] otherArrayValue = (Object[]) otherValue;
                    if (values.size() != otherArrayValue.length) return false;
                    Iterator<AnnotationDescription.AnnotationValue.Loaded<?>> iterator = values.iterator();
                    for (Object value : otherArrayValue) {
                        AnnotationDescription.AnnotationValue.Loaded<?> self = iterator.next();
                        if (!self.getState().isResolved() || !self.resolve().equals(value)) {
                            return false;
                        }
                    }
                    return true;
                }

                @Override
                public int hashCode() {
                    int result = 1;
                    for (AnnotationDescription.AnnotationValue.Loaded<?> value : values) {
                        result = 31 * result + value.hashCode();
                    }
                    return result;
                }

                @Override
                public String toString() {
                    StringBuilder stringBuilder = new StringBuilder("[");
                    for (AnnotationDescription.AnnotationValue.Loaded<?> value : values) {
                        stringBuilder.append(value.toString());
                    }
                    return stringBuilder.append("]").toString();
                }
            }
        }
    }

    /**
     * A default implementation of a {@link net.bytebuddy.pool.TypePool} that models binary data in the
     * Java byte code format into a {@link TypeDescription}. The data lookup
     * is delegated to a {@link net.bytebuddy.dynamic.ClassFileLocator}.
     */
    class Default extends AbstractBase {

        /**
         * Indicates that a visited method should be ignored.
         */
        private static final MethodVisitor IGNORE_METHOD = null;

        /**
         * The ASM version that is applied when reading class files.
         */
        private static final int ASM_API_VERSION = Opcodes.ASM5;

        /**
         * A flag to indicate ASM that no automatic calculations are requested.
         */
        private static final int ASM_MANUAL_FLAG = 0;

        /**
         * The locator to query for finding binary data of a type.
         */
        private final ClassFileLocator classFileLocator;

        /**
         * Creates a new default type pool.
         *
         * @param cacheProvider    The cache provider to be used.
         * @param classFileLocator The class file locator to be used.
         */
        public Default(CacheProvider cacheProvider, ClassFileLocator classFileLocator) {
            super(cacheProvider);
            this.classFileLocator = classFileLocator;
        }

        /**
         * Creates a default {@link net.bytebuddy.pool.TypePool} that looks up data by querying the system class
         * loader.
         *
         * @return A type pool that reads its data from the system class path.
         */
        public static TypePool ofClassPath() {
            return new Default(new CacheProvider.Simple(), ClassFileLocator.ForClassLoader.ofClassPath());
        }

        @Override
        protected Resolution doDescribe(String name) {
            try {
                ClassFileLocator.Resolution resolution = classFileLocator.locate(name);
                return resolution.isResolved()
                        ? new Resolution.Simple(parse(resolution.resolve()))
                        : new Resolution.Illegal(name);
            } catch (IOException e) {
                throw new IllegalStateException("Error while reading class file", e);
            }
        }

        /**
         * Parses a binary representation and transforms it into a type description.
         *
         * @param binaryRepresentation The binary data to be parsed.
         * @return A type description of the binary data.
         */
        private TypeDescription parse(byte[] binaryRepresentation) {
            ClassReader classReader = new ClassReader(binaryRepresentation);
            TypeExtractor typeExtractor = new TypeExtractor();
            classReader.accept(typeExtractor, ASM_MANUAL_FLAG);
            return typeExtractor.toTypeDescription();
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && super.equals(other)
                    && classFileLocator.equals(((Default) other).classFileLocator);
        }

        @Override
        public int hashCode() {
            return 31 * super.hashCode() + classFileLocator.hashCode();
        }

        @Override
        public String toString() {
            return "TypePool.Default{" +
                    "classFileLocator=" + classFileLocator +
                    ", cacheProvider=" + cacheProvider +
                    '}';
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
            void register(String name, AnnotationDescription.AnnotationValue<?, ?> annotationValue);

            /**
             * Called once all annotation values are visited.
             */
            void onComplete();
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
            RawNonPrimitiveArray.ComponentTypeReference bind(String name);

            /**
             * A component type locator which cannot legally resolve an array's component type.
             */
            enum Illegal implements ComponentTypeLocator {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public RawNonPrimitiveArray.ComponentTypeReference bind(String name) {
                    throw new IllegalStateException("Unexpected lookup of component type for " + name);
                }

                @Override
                public String toString() {
                    return "TypePool.Default.ComponentTypeLocator.Illegal." + name();
                }
            }

            /**
             * A component type locator that lazily analyses an annotation for resolving an annotation property's
             * array value's component type.
             */
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
                public RawNonPrimitiveArray.ComponentTypeReference bind(String name) {
                    return new Bound(name);
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && annotationName.equals(((ForAnnotationProperty) other).annotationName)
                            && typePool.equals(((ForAnnotationProperty) other).typePool);
                }

                @Override
                public int hashCode() {
                    int result = typePool.hashCode();
                    result = 31 * result + annotationName.hashCode();
                    return result;
                }

                @Override
                public String toString() {
                    return "TypePool.Default.ComponentTypeLocator.ForAnnotationProperty{" +
                            "typePool=" + typePool +
                            ", annotationName='" + annotationName + '\'' +
                            '}';
                }

                /**
                 * A bound representation of a
                 * {@link net.bytebuddy.pool.TypePool.Default.ComponentTypeLocator.ForAnnotationProperty}.
                 */
                protected class Bound implements RawNonPrimitiveArray.ComponentTypeReference {

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
                                .getComponentType()
                                .getName();
                    }

                    @Override
                    public boolean equals(Object other) {
                        return this == other || !(other == null || getClass() != other.getClass())
                                && name.equals(((Bound) other).name)
                                && ForAnnotationProperty.this.equals(((Bound) other).getOuter());
                    }

                    @Override
                    public int hashCode() {
                        return name.hashCode() + 31 * ForAnnotationProperty.this.hashCode();
                    }

                    /**
                     * Returns the outer instance.
                     *
                     * @return The outer instance.
                     */
                    private ForAnnotationProperty getOuter() {
                        return ForAnnotationProperty.this;
                    }

                    @Override
                    public String toString() {
                        return "TypePool.Default.ComponentTypeLocator.ForAnnotationProperty.Bound{" +
                                "name='" + name + '\'' +
                                '}';
                    }
                }
            }

            /**
             * A component type locator that locates an array type by a method's return value from its method descriptor.
             */
            class ForArrayType implements ComponentTypeLocator, RawNonPrimitiveArray.ComponentTypeReference {

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
                public RawNonPrimitiveArray.ComponentTypeReference bind(String name) {
                    return this;
                }

                @Override
                public String lookup() {
                    return componentType;
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && componentType.equals(((ForArrayType) other).componentType);
                }

                @Override
                public int hashCode() {
                    return componentType.hashCode();
                }

                @Override
                public String toString() {
                    return "TypePool.Default.ComponentTypeLocator.ForArrayType{" +
                            "componentType='" + componentType + '\'' +
                            '}';
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
                parameterRegistry = new HashMap<Integer, String>(parameterType.length);
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

            @Override
            public String toString() {
                return "TypePool.Default.ParameterBag{" +
                        "parameterType=" + Arrays.toString(parameterType) +
                        ", parameterRegistry=" + parameterRegistry +
                        '}';
            }
        }

        protected interface GenericTypeRegistrant {

            void register(LazyTypeDescription.GenericTypeToken token);

            class RejectingSignatureVisitor extends SignatureVisitor {

                private static final String MESSAGE = "Unexpected token in generic signature";

                public RejectingSignatureVisitor() {
                    super(ASM_API_VERSION);
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

        protected static class GenericTypeExtractor extends GenericTypeRegistrant.RejectingSignatureVisitor implements GenericTypeRegistrant {

            private final GenericTypeRegistrant genericTypeRegistrant;

            private IncompleteToken incompleteToken;

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
                genericTypeRegistrant.register(new LazyTypeDescription.GenericTypeToken.ForArray(componentTypeToken));
            }

            @Override
            public void visitClassType(String name) {
                incompleteToken = new IncompleteToken.ForTopLevelClass(name);
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

            protected interface IncompleteToken {

                SignatureVisitor appendLowerBound();

                SignatureVisitor appendUpperBound();

                SignatureVisitor appendDirectBound();

                void appendPlaceholder();

                boolean isParameterized();

                String getName();

                LazyTypeDescription.GenericTypeToken toToken();

                abstract class AbstractBase implements IncompleteToken {

                    protected final List<LazyTypeDescription.GenericTypeToken> parameters;

                    public AbstractBase() {
                        parameters = new LinkedList<LazyTypeDescription.GenericTypeToken>();
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

                    protected class ForDirectBound implements GenericTypeRegistrant {

                        @Override
                        public void register(LazyTypeDescription.GenericTypeToken token) {
                            parameters.add(token);
                        }
                    }

                    protected class ForUpperBound implements GenericTypeRegistrant {

                        @Override
                        public void register(LazyTypeDescription.GenericTypeToken token) {
                            parameters.add(new LazyTypeDescription.GenericTypeToken.ForUpperBoundWildcard(token));
                        }
                    }

                    protected class ForLowerBound implements GenericTypeRegistrant {

                        @Override
                        public void register(LazyTypeDescription.GenericTypeToken token) {
                            parameters.add(new LazyTypeDescription.GenericTypeToken.ForLowerBoundWildcard(token));
                        }
                    }
                }

                class ForTopLevelClass extends AbstractBase {

                    private final String internalName;

                    public ForTopLevelClass(String internalName) {
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

                class ForInnerClass extends AbstractBase {

                    private static final char INNER_CLASS_SEPERATOR = '$';

                    private final String internalName;

                    private final IncompleteToken outerClassToken;

                    public ForInnerClass(String internalName, IncompleteToken outerClassToken) {
                        this.internalName = internalName;
                        this.outerClassToken = outerClassToken;
                    }

                    @Override
                    public LazyTypeDescription.GenericTypeToken toToken() {
                        return isParameterized() || outerClassToken.isParameterized()
                                ? new LazyTypeDescription.GenericTypeToken.ForParameterizedType.Nested(getName(), parameters, outerClassToken.toToken())
                                : new LazyTypeDescription.GenericTypeToken.ForRawType(getName());
                    }

                    @Override
                    public boolean isParameterized() {
                        return !parameters.isEmpty() || !outerClassToken.isParameterized();
                    }

                    @Override
                    public String getName() {
                        return outerClassToken.getName() + INNER_CLASS_SEPERATOR + internalName.replace('/', '.');
                    }
                }
            }

            protected abstract static class ForSignature<T extends LazyTypeDescription.GenericTypeToken.Resolution>
                    extends RejectingSignatureVisitor
                    implements GenericTypeRegistrant {

                protected static <S extends LazyTypeDescription.GenericTypeToken.Resolution> S extract(String genericSignature, ForSignature<S> visitor) {
                    SignatureReader signatureReader = new SignatureReader(genericSignature);
                    signatureReader.accept(visitor);
                    return visitor.resolve();
                }

                protected final List<LazyTypeDescription.GenericTypeToken> typeVariableTokens;

                private String currentTypeParameter;

                private List<LazyTypeDescription.GenericTypeToken> currentBounds;

                public ForSignature() {
                    typeVariableTokens = new LinkedList<LazyTypeDescription.GenericTypeToken>();
                }

                @Override
                public void visitFormalTypeParameter(String name) {
                    collectTypeParameter();
                    currentTypeParameter = name;
                    currentBounds = new LinkedList<LazyTypeDescription.GenericTypeToken>();
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
                    currentBounds.add(token);
                }

                protected void collectTypeParameter() {
                    if (currentTypeParameter != null) {
                        typeVariableTokens.add(new LazyTypeDescription.GenericTypeToken.ForTypeVariable.Formal(currentTypeParameter, currentBounds));
                    }
                }

                public abstract T resolve();

                protected static class OfType extends ForSignature<LazyTypeDescription.GenericTypeToken.Resolution.ForType> {

                    public static LazyTypeDescription.GenericTypeToken.Resolution.ForType extract(String genericSignature) {
                        try {
                            return genericSignature == null
                                    ? LazyTypeDescription.GenericTypeToken.Resolution.Raw.INSTANCE
                                    : ForSignature.extract(genericSignature, new OfType());
                        } catch (RuntimeException ignored) {
                            return LazyTypeDescription.GenericTypeToken.Resolution.Defective.INSTANCE;
                        }
                    }

                    private LazyTypeDescription.GenericTypeToken superTypeToken;

                    private final List<LazyTypeDescription.GenericTypeToken> interfaceTypeTokens;

                    protected OfType() {
                        interfaceTypeTokens = new LinkedList<LazyTypeDescription.GenericTypeToken>();
                    }

                    @Override
                    public SignatureVisitor visitSuperclass() {
                        collectTypeParameter();
                        return new GenericTypeExtractor(new SuperTypeRegistrant());
                    }

                    @Override
                    public SignatureVisitor visitInterface() {
                        return new GenericTypeExtractor(new InterfaceTypeRegistrant());
                    }

                    @Override
                    public LazyTypeDescription.GenericTypeToken.Resolution.ForType resolve() {
                        return new LazyTypeDescription.GenericTypeToken.Resolution.ForType.Tokenized(superTypeToken, interfaceTypeTokens, typeVariableTokens);
                    }

                    protected class SuperTypeRegistrant implements GenericTypeRegistrant {

                        @Override
                        public void register(LazyTypeDescription.GenericTypeToken token) {
                            superTypeToken = token;
                        }
                    }

                    protected class InterfaceTypeRegistrant implements GenericTypeRegistrant {

                        @Override
                        public void register(LazyTypeDescription.GenericTypeToken token) {
                            interfaceTypeTokens.add(token);
                        }
                    }
                }

                protected static class OfMethod extends ForSignature<LazyTypeDescription.GenericTypeToken.Resolution.ForMethod> {

                    public static LazyTypeDescription.GenericTypeToken.Resolution.ForMethod extract(String genericSignature) {
                        try {
                            return genericSignature == null
                                    ? LazyTypeDescription.GenericTypeToken.Resolution.Raw.INSTANCE
                                    : ForSignature.extract(genericSignature, new OfMethod());
                        } catch (RuntimeException ignored) {
                            return LazyTypeDescription.GenericTypeToken.Resolution.Defective.INSTANCE;
                        }
                    }

                    private LazyTypeDescription.GenericTypeToken returnTypeToken;

                    private final List<LazyTypeDescription.GenericTypeToken> parameterTypeTokens;

                    private final List<LazyTypeDescription.GenericTypeToken> exceptionTypeTokens;

                    public OfMethod() {
                        parameterTypeTokens = new LinkedList<LazyTypeDescription.GenericTypeToken>();
                        exceptionTypeTokens = new LinkedList<LazyTypeDescription.GenericTypeToken>();
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
                        return new GenericTypeExtractor(new InterfaceTypeRegistrant());
                    }

                    @Override
                    public LazyTypeDescription.GenericTypeToken.Resolution.ForMethod resolve() {
                        return new LazyTypeDescription.GenericTypeToken.Resolution.ForMethod.Tokenized(returnTypeToken,
                                parameterTypeTokens,
                                exceptionTypeTokens,
                                typeVariableTokens);
                    }

                    protected class ParameterTypeRegistrant implements GenericTypeRegistrant {

                        @Override
                        public void register(LazyTypeDescription.GenericTypeToken token) {
                            parameterTypeTokens.add(token);
                        }
                    }

                    protected class ReturnTypeTypeRegistrant implements GenericTypeRegistrant {

                        @Override
                        public void register(LazyTypeDescription.GenericTypeToken token) {
                            returnTypeToken = token;
                        }
                    }

                    protected class InterfaceTypeRegistrant implements GenericTypeRegistrant {

                        @Override
                        public void register(LazyTypeDescription.GenericTypeToken token) {
                            exceptionTypeTokens.add(token);
                        }
                    }
                }

                protected static class OfField implements GenericTypeRegistrant {

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
                                return LazyTypeDescription.GenericTypeToken.Resolution.Defective.INSTANCE;
                            }
                        }
                    }

                    private LazyTypeDescription.GenericTypeToken fieldTypeToken;

                    @Override
                    public void register(LazyTypeDescription.GenericTypeToken token) {
                        fieldTypeToken = token;
                    }

                    protected LazyTypeDescription.GenericTypeToken.Resolution.ForField resolve() {
                        return new LazyTypeDescription.GenericTypeToken.Resolution.ForField.Tokenized(fieldTypeToken);
                    }
                }
            }
        }

        /**
         * A type extractor reads a class file and collects data that is relevant to create a type description.
         */
        protected class TypeExtractor extends ClassVisitor {

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
            private String superTypeName;

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
            private LazyTypeDescription.DeclarationContext declarationContext;

            /**
             * Creates a new type extractor.
             */
            protected TypeExtractor() {
                super(ASM_API_VERSION);
                annotationTokens = new LinkedList<LazyTypeDescription.AnnotationToken>();
                fieldTokens = new LinkedList<LazyTypeDescription.FieldToken>();
                methodTokens = new LinkedList<LazyTypeDescription.MethodToken>();
                anonymousType = false;
                declarationContext = LazyTypeDescription.DeclarationContext.SelfDeclared.INSTANCE;
            }

            @Override
            public void visit(int classFileVersion,
                              int modifiers,
                              String internalName,
                              String genericSignature,
                              String superTypeName,
                              String[] interfaceName) {
                this.modifiers = modifiers;
                this.internalName = internalName;
                this.genericSignature = genericSignature;
                this.superTypeName = superTypeName;
                this.interfaceName = interfaceName;
            }

            @Override
            public void visitOuterClass(String typeName, String methodName, String methodDescriptor) {
                if (methodName != null) {
                    declarationContext = new LazyTypeDescription.DeclarationContext.DeclaredInMethod(typeName,
                            methodName,
                            methodDescriptor);
                } else if (typeName != null) {
                    declarationContext = new LazyTypeDescription.DeclarationContext.DeclaredInType(typeName);
                }
            }

            @Override
            public void visitInnerClass(String internalName, String outerName, String innerName, int modifiers) {
                if (internalName.equals(this.internalName)) {
                    this.modifiers = modifiers;
                    if (innerName == null) {
                        anonymousType = true;
                    }
                    if (declarationContext.isSelfDeclared()) {
                        declarationContext = new LazyTypeDescription.DeclarationContext.DeclaredInType(outerName);
                    }
                }
            }

            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                return new AnnotationExtractor(new OnTypeCollector(descriptor),
                        new ComponentTypeLocator.ForAnnotationProperty(Default.this, descriptor));
            }

            @Override
            public FieldVisitor visitField(int modifiers,
                                           String internalName,
                                           String descriptor,
                                           String genericSignature,
                                           Object defaultValue) {
                return new FieldExtractor(modifiers, internalName, descriptor, genericSignature);
            }

            @Override
            public MethodVisitor visitMethod(int modifiers,
                                             String internalName,
                                             String descriptor,
                                             String genericSignature,
                                             String[] exceptionName) {
                return internalName.equals(MethodDescription.TYPE_INITIALIZER_INTERNAL_NAME)
                        ? IGNORE_METHOD
                        : new MethodExtractor(modifiers, internalName, descriptor, genericSignature, exceptionName);
            }

            /**
             * Creates a type description from all data that is currently collected. This method should only be invoked
             * after a class file was parsed fully.
             *
             * @return A type description reflecting the data that was collected by this instance.
             */
            protected TypeDescription toTypeDescription() {
                return new LazyTypeDescription(Default.this,
                        modifiers,
                        internalName,
                        superTypeName,
                        interfaceName,
                        GenericTypeExtractor.ForSignature.OfType.extract(genericSignature),
                        declarationContext,
                        anonymousType,
                        annotationTokens,
                        fieldTokens,
                        methodTokens);
            }

            @Override
            public String toString() {
                return "TypePool.Default.TypeExtractor{" +
                        "typePool=" + Default.this +
                        ", annotationTokens=" + annotationTokens +
                        ", fieldTokens=" + fieldTokens +
                        ", methodTokens=" + methodTokens +
                        ", modifiers=" + modifiers +
                        ", internalName='" + internalName + '\'' +
                        ", superTypeName='" + superTypeName + '\'' +
                        ", genericSignature='" + genericSignature + '\'' +
                        ", interfaceName=" + Arrays.toString(interfaceName) +
                        ", anonymousType=" + anonymousType +
                        ", declarationContext=" + declarationContext +
                        '}';
            }

            /**
             * An annotation registrant that collects annotations found on a type.
             */
            protected class OnTypeCollector implements AnnotationRegistrant {

                /**
                 * The descriptor of the annotation that is being collected.
                 */
                private final String descriptor;

                /**
                 * The values that were collected so far.
                 */
                private final Map<String, AnnotationDescription.AnnotationValue<?, ?>> values;

                /**
                 * Creates a new on type collector.
                 *
                 * @param descriptor The descriptor of the annotation that is being collected.
                 */
                protected OnTypeCollector(String descriptor) {
                    this.descriptor = descriptor;
                    values = new HashMap<String, AnnotationDescription.AnnotationValue<?, ?>>();
                }

                @Override
                public void register(String name, AnnotationDescription.AnnotationValue<?, ?> annotationValue) {
                    values.put(name, annotationValue);
                }

                @Override
                public void onComplete() {
                    annotationTokens.add(new LazyTypeDescription.AnnotationToken(descriptor, values));
                }

                @Override
                public String toString() {
                    return "TypePool.Default.TypeExtractor.OnTypeCollector{" +
                            "typeExtractor=" + TypeExtractor.this +
                            ", descriptor='" + descriptor + '\'' +
                            ", values=" + values +
                            '}';
                }
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
                 * Creates a new annotation extractor.
                 *
                 * @param annotationRegistrant The annotation registrant to register found annotation values on.
                 * @param componentTypeLocator A locator for the component type of any found annotation value.
                 */
                protected AnnotationExtractor(AnnotationRegistrant annotationRegistrant,
                                              ComponentTypeLocator componentTypeLocator) {
                    super(ASM_API_VERSION);
                    this.annotationRegistrant = annotationRegistrant;
                    this.componentTypeLocator = componentTypeLocator;
                }

                @Override
                public void visit(String name, Object value) {
                    AnnotationDescription.AnnotationValue<?, ?> annotationValue;
                    if (value instanceof Type) {
                        annotationValue = new RawTypeValue(Default.this, (Type) value);
                    } else if (value.getClass().isArray()) {
                        annotationValue = new AnnotationDescription.AnnotationValue.Trivial<Object>(value);
                    } else {
                        annotationValue = new AnnotationDescription.AnnotationValue.Trivial<Object>(value);
                    }
                    annotationRegistrant.register(name, annotationValue);
                }

                @Override
                public void visitEnum(String name, String descriptor, String value) {
                    annotationRegistrant.register(name, new RawEnumerationValue(Default.this, descriptor, value));
                }

                @Override
                public AnnotationVisitor visitAnnotation(String name, String descriptor) {
                    return new AnnotationExtractor(new AnnotationLookup(name, descriptor),
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

                @Override
                public String toString() {
                    return "TypePool.Default.TypeExtractor.AnnotationExtractor{" +
                            "typeExtractor=" + TypeExtractor.this +
                            "annotationRegistrant=" + annotationRegistrant +
                            ", componentTypeLocator=" + componentTypeLocator +
                            '}';
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
                    private final RawNonPrimitiveArray.ComponentTypeReference componentTypeReference;

                    /**
                     * A list of all annotation values that are found on this array.
                     */
                    private final List<AnnotationDescription.AnnotationValue<?, ?>> values;

                    /**
                     * Creates a new annotation registrant for an array lookup.
                     *
                     * @param name                   The name of the annotation property the collected array is representing.
                     * @param componentTypeReference A lazy reference to resolve the component type of the collected array.
                     */
                    protected ArrayLookup(String name,
                                          RawNonPrimitiveArray.ComponentTypeReference componentTypeReference) {
                        this.name = name;
                        this.componentTypeReference = componentTypeReference;
                        values = new LinkedList<AnnotationDescription.AnnotationValue<?, ?>>();
                    }

                    @Override
                    public void register(String ignored, AnnotationDescription.AnnotationValue<?, ?> annotationValue) {
                        values.add(annotationValue);
                    }

                    @Override
                    public void onComplete() {
                        annotationRegistrant.register(name, new RawNonPrimitiveArray(Default.this, componentTypeReference, values));
                    }

                    @Override
                    public String toString() {
                        return "TypePool.Default.TypeExtractor.AnnotationExtractor.ArrayLookup{" +
                                "annotationExtractor=" + AnnotationExtractor.this +
                                ", name='" + name + '\'' +
                                ", componentTypeReference=" + componentTypeReference +
                                ", values=" + values +
                                '}';
                    }
                }

                /**
                 * An annotation registrant for registering the values on an array that is itself an annotation property.
                 */
                protected class AnnotationLookup implements AnnotationRegistrant {

                    /**
                     * The name of the original annotation for which the annotation values are looked up.
                     */
                    private final String name;

                    /**
                     * The descriptor of the original annotation for which the annotation values are looked up.
                     */
                    private final String descriptor;

                    /**
                     * A mapping of annotation property values to their values.
                     */
                    private final Map<String, AnnotationDescription.AnnotationValue<?, ?>> values;

                    /**
                     * Creates a new annotation registrant for a recursive annotation lookup.
                     *
                     * @param name       The name of the original annotation for which the annotation values are
                     *                   looked up.
                     * @param descriptor The descriptor of the original annotation for which the annotation values are
                     *                   looked up.
                     */
                    protected AnnotationLookup(String name, String descriptor) {
                        this.name = name;
                        this.descriptor = descriptor;
                        values = new HashMap<String, AnnotationDescription.AnnotationValue<?, ?>>();
                    }

                    @Override
                    public void register(String name, AnnotationDescription.AnnotationValue<?, ?> annotationValue) {
                        values.put(name, annotationValue);
                    }

                    @Override
                    public void onComplete() {
                        annotationRegistrant.register(name, new RawAnnotationValue(Default.this, new LazyTypeDescription.AnnotationToken(descriptor, values)));
                    }

                    @Override
                    public String toString() {
                        return "TypePool.Default.TypeExtractor.AnnotationExtractor.AnnotationLookup{" +
                                "annotationExtractor=" + AnnotationExtractor.this +
                                ", name='" + name + '\'' +
                                ", descriptor='" + descriptor + '\'' +
                                ", values=" + values +
                                '}';
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

                /**
                 * The generic signature of the field or {@code null} if it is not generic.
                 */
                private final String descriptor;

                /**
                 * The generic signature of the field or {@code null} if it is not generic.
                 */
                private final String genericSignature;

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
                    super(ASM_API_VERSION);
                    this.modifiers = modifiers;
                    this.internalName = internalName;
                    this.descriptor = descriptor;
                    this.genericSignature = genericSignature;
                    annotationTokens = new LinkedList<LazyTypeDescription.AnnotationToken>();
                }

                @Override
                public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                    return new AnnotationExtractor(new OnFieldCollector(descriptor),
                            new ComponentTypeLocator.ForAnnotationProperty(Default.this, descriptor));
                }

                @Override
                public void visitEnd() {
                    fieldTokens.add(new LazyTypeDescription.FieldToken(modifiers,
                            internalName,
                            descriptor,
                            GenericTypeExtractor.ForSignature.OfField.extract(genericSignature),
                            annotationTokens));
                }

                @Override
                public String toString() {
                    return "TypePool.Default.TypeExtractor.FieldExtractor{" +
                            "typeExtractor=" + TypeExtractor.this +
                            ", modifiers=" + modifiers +
                            ", internalName='" + internalName + '\'' +
                            ", descriptor='" + descriptor + '\'' +
                            ", genericSignature='" + genericSignature + '\'' +
                            ", annotationTokens=" + annotationTokens +
                            '}';
                }

                /**
                 * An annotation registrant that collects annotations that are declared on a field.
                 */
                protected class OnFieldCollector implements AnnotationRegistrant {

                    /**
                     * The annotation descriptor.
                     */
                    private final String descriptor;

                    /**
                     * A mapping of annotation property names to their values.
                     */
                    private final Map<String, AnnotationDescription.AnnotationValue<?, ?>> values;

                    /**
                     * Creates a new annotation field registrant.
                     *
                     * @param descriptor The descriptor of the annotation.
                     */
                    protected OnFieldCollector(String descriptor) {
                        this.descriptor = descriptor;
                        values = new HashMap<String, AnnotationDescription.AnnotationValue<?, ?>>();
                    }

                    @Override
                    public void register(String name, AnnotationDescription.AnnotationValue<?, ?> annotationValue) {
                        values.put(name, annotationValue);
                    }

                    @Override
                    public void onComplete() {
                        annotationTokens.add(new LazyTypeDescription.AnnotationToken(descriptor, values));
                    }

                    @Override
                    public String toString() {
                        return "TypePool.Default.TypeExtractor.FieldExtractor.OnFieldCollector{" +
                                "fieldExtractor=" + FieldExtractor.this +
                                ", descriptor='" + descriptor + '\'' +
                                ", values=" + values +
                                '}';
                    }
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
                 * The default value of the found method or {@code null} if no such value exists.
                 */
                private AnnotationDescription.AnnotationValue<?, ?> defaultValue;

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
                    super(ASM_API_VERSION);
                    this.modifiers = modifiers;
                    this.internalName = internalName;
                    this.descriptor = descriptor;
                    this.genericSignature = genericSignature;
                    this.exceptionName = exceptionName;
                    annotationTokens = new LinkedList<LazyTypeDescription.AnnotationToken>();
                    Type[] parameterTypes = Type.getMethodType(descriptor).getArgumentTypes();
                    parameterAnnotationTokens = new HashMap<Integer, List<LazyTypeDescription.AnnotationToken>>(parameterTypes.length);
                    for (int i = 0; i < parameterTypes.length; i++) {
                        parameterAnnotationTokens.put(i, new LinkedList<LazyTypeDescription.AnnotationToken>());
                    }
                    parameterTokens = new ArrayList<LazyTypeDescription.MethodToken.ParameterToken>(parameterTypes.length);
                    legacyParameterBag = new ParameterBag(parameterTypes);
                }

                @Override
                public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                    return new AnnotationExtractor(new OnMethodCollector(descriptor),
                            new ComponentTypeLocator.ForAnnotationProperty(Default.this, descriptor));
                }

                @Override
                public AnnotationVisitor visitParameterAnnotation(int index, String descriptor, boolean visible) {
                    return new AnnotationExtractor(new OnMethodParameterCollector(descriptor, index),
                            new ComponentTypeLocator.ForAnnotationProperty(Default.this, descriptor));
                }

                @Override
                public void visitLabel(Label label) {
                    if (firstLabel == null) {
                        firstLabel = label;
                    }
                }

                @Override
                public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
                    if (start == firstLabel) {
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
                public void register(String ignored, AnnotationDescription.AnnotationValue<?, ?> annotationValue) {
                    defaultValue = annotationValue;
                }

                @Override
                public void onComplete() {
                    /* do nothing, as the register method is called at most once for default values */
                }

                @Override
                public void visitEnd() {
                    methodTokens.add(new LazyTypeDescription.MethodToken(modifiers,
                            internalName,
                            descriptor,
                            GenericTypeExtractor.ForSignature.OfMethod.extract(genericSignature),
                            exceptionName,
                            annotationTokens,
                            parameterAnnotationTokens,
                            parameterTokens.isEmpty()
                                    ? legacyParameterBag.resolve((modifiers & Opcodes.ACC_STATIC) != 0)
                                    : parameterTokens,
                            defaultValue));
                }

                @Override
                public String toString() {
                    return "TypePool.Default.TypeExtractor.MethodExtractor{" +
                            "typeExtractor=" + TypeExtractor.this +
                            ", modifiers=" + modifiers +
                            ", internalName='" + internalName + '\'' +
                            ", descriptor='" + descriptor + '\'' +
                            ", genericSignature='" + genericSignature + '\'' +
                            ", exceptionName=" + Arrays.toString(exceptionName) +
                            ", annotationTokens=" + annotationTokens +
                            ", parameterAnnotationTokens=" + parameterAnnotationTokens +
                            ", parameterTokens=" + parameterTokens +
                            ", legacyParameterBag=" + legacyParameterBag +
                            ", firstLabel=" + firstLabel +
                            ", defaultValue=" + defaultValue +
                            '}';
                }

                /**
                 * An annotation registrant for annotations found on the method itself.
                 */
                protected class OnMethodCollector implements AnnotationRegistrant {

                    /**
                     * The descriptor of the annotation.
                     */
                    private final String descriptor;

                    /**
                     * A mapping of annotation properties to their values.
                     */
                    private final Map<String, AnnotationDescription.AnnotationValue<?, ?>> values;

                    /**
                     * Creates a new method annotation registrant.
                     *
                     * @param descriptor The descriptor of the annotation.
                     */
                    protected OnMethodCollector(String descriptor) {
                        this.descriptor = descriptor;
                        values = new HashMap<String, AnnotationDescription.AnnotationValue<?, ?>>();
                    }

                    @Override
                    public void register(String name, AnnotationDescription.AnnotationValue<?, ?> annotationValue) {
                        values.put(name, annotationValue);
                    }

                    @Override
                    public void onComplete() {
                        annotationTokens.add(new LazyTypeDescription.AnnotationToken(descriptor, values));
                    }

                    @Override
                    public String toString() {
                        return "TypePool.Default.TypeExtractor.MethodExtractor.OnMethodCollector{" +
                                "methodExtractor=" + MethodExtractor.this +
                                ", descriptor='" + descriptor + '\'' +
                                ", values=" + values +
                                '}';
                    }
                }

                /**
                 * An annotation registrant that collects annotations that are found on a specific parameter.
                 */
                protected class OnMethodParameterCollector implements AnnotationRegistrant {

                    /**
                     * The descriptor of the annotation.
                     */
                    private final String descriptor;

                    /**
                     * The index of the parameter of this annotation.
                     */
                    private final int index;

                    /**
                     * A mapping of annotation properties to their values.
                     */
                    private final Map<String, AnnotationDescription.AnnotationValue<?, ?>> values;

                    /**
                     * Creates a new method parameter annotation registrant.
                     *
                     * @param descriptor The descriptor of the annotation.
                     * @param index      The index of the parameter of this annotation.
                     */
                    protected OnMethodParameterCollector(String descriptor, int index) {
                        this.descriptor = descriptor;
                        this.index = index;
                        values = new HashMap<String, AnnotationDescription.AnnotationValue<?, ?>>();
                    }

                    @Override
                    public void register(String name, AnnotationDescription.AnnotationValue<?, ?> annotationValue) {
                        values.put(name, annotationValue);
                    }

                    @Override
                    public void onComplete() {
                        parameterAnnotationTokens.get(index).add(new LazyTypeDescription.AnnotationToken(descriptor, values));
                    }

                    @Override
                    public String toString() {
                        return "TypePool.Default.TypeExtractor.MethodExtractor.OnMethodParameterCollector{" +
                                "methodExtractor=" + MethodExtractor.this +
                                ", descriptor='" + descriptor + '\'' +
                                ", index=" + index +
                                ", values=" + values +
                                '}';
                    }
                }
            }
        }
    }

    /**
     * A type description that looks up any referenced {@link net.bytebuddy.description.ByteCodeElement} or
     * {@link AnnotationDescription} by querying a type pool at lookup time.
     */
    class LazyTypeDescription extends TypeDescription.AbstractTypeDescription.OfSimpleType {

        /**
         * The type pool to be used for looking up linked types.
         */
        private final TypePool typePool;

        /**
         * The modifiers of this type.
         */
        private final int modifiers;

        /**
         * The binary name of this type.
         */
        private final String name;

        private final String superTypeDescriptor;

        private final GenericTypeToken.Resolution.ForType signatureResolution;

        private final List<String> interfaceTypeDescriptors;

        /**
         * The declaration context of this type.
         */
        private final DeclarationContext declarationContext;

        /**
         * {@code true} if this type is an anonymous type.
         */
        private final boolean anonymousType;

        /**
         * A list of annotation descriptions that are declared by this type.
         */
        private final List<AnnotationDescription> declaredAnnotations;

        /**
         * A list of field descriptions that are declared by this type.
         */
        private final List<FieldDescription> declaredFields;

        /**
         * A list of method descriptions that are declared by this type.
         */
        private final List<MethodDescription> declaredMethods;

        protected LazyTypeDescription(TypePool typePool,
                                      int modifiers,
                                      String name,
                                      String superTypeInternalName,
                                      String[] interfaceInternalName,
                                      GenericTypeToken.Resolution.ForType signatureResolution,
                                      DeclarationContext declarationContext,
                                      boolean anonymousType,
                                      List<AnnotationToken> annotationTokens,
                                      List<FieldToken> fieldTokens,
                                      List<MethodToken> methodTokens) {
            this.typePool = typePool;
            this.modifiers = modifiers;
            this.name = Type.getObjectType(name).getClassName();
            this.superTypeDescriptor = superTypeInternalName == null
                    ? null
                    : Type.getObjectType(superTypeInternalName).getDescriptor();
            this.signatureResolution = signatureResolution;
            if (interfaceInternalName == null) {
                interfaceTypeDescriptors = Collections.<String>emptyList();
            } else {
                interfaceTypeDescriptors = new ArrayList<String>(interfaceInternalName.length);
                for (String internalName : interfaceInternalName) {
                    interfaceTypeDescriptors.add(Type.getObjectType(internalName).getDescriptor());
                }
            }
            this.declarationContext = declarationContext;
            this.anonymousType = anonymousType;
            declaredAnnotations = new ArrayList<AnnotationDescription>(annotationTokens.size());
            for (AnnotationToken annotationToken : annotationTokens) {
                declaredAnnotations.add(annotationToken.toAnnotationDescription(typePool));
            }
            declaredFields = new ArrayList<FieldDescription>(fieldTokens.size());
            for (FieldToken fieldToken : fieldTokens) {
                declaredFields.add(fieldToken.toFieldDescription(this));
            }
            declaredMethods = new ArrayList<MethodDescription>(methodTokens.size());
            for (MethodToken methodToken : methodTokens) {
                declaredMethods.add(methodToken.toMethodDescription(this));
            }
        }

        @Override
        public GenericTypeDescription getSuperTypeGen() {
            return superTypeDescriptor == null || isInterface()
                    ? null
                    : signatureResolution.resolveSuperType(superTypeDescriptor, typePool, this);
        }

        @Override
        public GenericTypeList getInterfacesGen() {
            return signatureResolution.resolveInterfaceTypes(interfaceTypeDescriptors, typePool, this);
        }

        @Override
        public MethodDescription getEnclosingMethod() {
            return declarationContext.getEnclosingMethod(typePool);
        }

        @Override
        public TypeDescription getEnclosingType() {
            return declarationContext.getEnclosingType(typePool);
        }

        @Override
        public boolean isAnonymousClass() {
            return anonymousType;
        }

        @Override
        public boolean isLocalClass() {
            return !anonymousType && declarationContext.isDeclaredInMethod();
        }

        @Override
        public boolean isMemberClass() {
            return declarationContext.isDeclaredInType();
        }

        @Override
        public FieldList getDeclaredFields() {
            return new FieldList.Explicit(declaredFields);
        }

        @Override
        public MethodList getDeclaredMethods() {
            return new MethodList.Explicit(declaredMethods);
        }

        @Override
        public PackageDescription getPackage() {
            String packageName = getPackageName();
            return packageName == null
                    ? null
                    : new LazyPackageDescription(typePool, packageName);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public TypeDescription getDeclaringType() {
            return declarationContext.isDeclaredInType()
                    ? declarationContext.getEnclosingType(typePool)
                    : null;
        }

        @Override
        public int getModifiers() {
            return modifiers;
        }

        @Override
        public AnnotationList getDeclaredAnnotations() {
            return new AnnotationList.Explicit(declaredAnnotations);
        }

        @Override
        public GenericTypeList getTypeVariables() {
            return signatureResolution.resolveTypeVariables(typePool, this);
        }

        /**
         * A declaration context encapsulates information about whether a type was declared within another type
         * or within a method of another type.
         */
        protected interface DeclarationContext {

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
             * Returns {@code true} if this instance represents a self declared type.
             *
             * @return {@code true} if this instance represents a self declared type.
             */
            boolean isSelfDeclared();

            /**
             * Returns {@code true} if this instance represents a type that was declared within another type but not
             * within a method.
             *
             * @return {@code true} if this instance represents a type that was declared within another type but not
             * within a method.
             */
            boolean isDeclaredInType();

            /**
             * Returns {@code true} if this instance represents a type that was declared within a method.
             *
             * @return {@code true} if this instance represents a type that was declared within a method.
             */
            boolean isDeclaredInMethod();

            /**
             * Represents a self-declared type that is not defined within another type.
             */
            enum SelfDeclared implements DeclarationContext {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public MethodDescription getEnclosingMethod(TypePool typePool) {
                    return null;
                }

                @Override
                public TypeDescription getEnclosingType(TypePool typePool) {
                    return null;
                }

                @Override
                public boolean isSelfDeclared() {
                    return true;
                }

                @Override
                public boolean isDeclaredInType() {
                    return false;
                }

                @Override
                public boolean isDeclaredInMethod() {
                    return false;
                }

                @Override
                public String toString() {
                    return "TypePool.LazyTypeDescription.DeclarationContext.SelfDeclared." + name();
                }
            }

            /**
             * A declaration context representing a type that is declared within another type but not within
             * a method.
             */
            class DeclaredInType implements DeclarationContext {

                /**
                 * The binary name of the referenced type.
                 */
                private final String name;

                /**
                 * Creates a new declaration context for a type that is declared within another type.
                 *
                 * @param internalName The internal name of the declaring type.
                 */
                public DeclaredInType(String internalName) {
                    name = internalName.replace('/', '.');
                }

                @Override
                public MethodDescription getEnclosingMethod(TypePool typePool) {
                    return null;
                }

                @Override
                public TypeDescription getEnclosingType(TypePool typePool) {
                    return typePool.describe(name).resolve();
                }

                @Override
                public boolean isSelfDeclared() {
                    return false;
                }

                @Override
                public boolean isDeclaredInType() {
                    return true;
                }

                @Override
                public boolean isDeclaredInMethod() {
                    return false;
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && name.equals(((DeclaredInType) other).name);
                }

                @Override
                public int hashCode() {
                    return name.hashCode();
                }

                @Override
                public String toString() {
                    return "TypePool.LazyTypeDescription.DeclarationContext.DeclaredInType{" +
                            "name='" + name + '\'' +
                            '}';
                }
            }

            /**
             * A declaration context representing a type that is declared within a method of another type.
             */
            class DeclaredInMethod implements DeclarationContext {

                /**
                 * The binary name of the declaring type.
                 */
                private final String name;

                /**
                 * The name of the method that is declaring a type.
                 */
                private final String methodName;

                /**
                 * The descriptor of the method that is declaring a type.
                 */
                private final String methodDescriptor;

                /**
                 * Creates a new declaration context for a method that declares a type.
                 *
                 * @param internalName     The internal name of the declaring type.
                 * @param methodName       The name of the method that is declaring a type.
                 * @param methodDescriptor The descriptor of the method that is declaring a type.
                 */
                public DeclaredInMethod(String internalName, String methodName, String methodDescriptor) {
                    name = internalName.replace('/', '.');
                    this.methodName = methodName;
                    this.methodDescriptor = methodDescriptor;
                }

                @Override
                public MethodDescription getEnclosingMethod(TypePool typePool) {
                    return getEnclosingType(typePool).getDeclaredMethods()
                            .filter((MethodDescription.CONSTRUCTOR_INTERNAL_NAME.equals(methodName)
                                    ? isConstructor()
                                    : ElementMatchers.<MethodDescription>named(methodName))
                                    .<MethodDescription>and(hasDescriptor(methodDescriptor))).getOnly();
                }

                @Override
                public TypeDescription getEnclosingType(TypePool typePool) {
                    return typePool.describe(name).resolve();
                }

                @Override
                public boolean isSelfDeclared() {
                    return false;
                }

                @Override
                public boolean isDeclaredInType() {
                    return false;
                }

                @Override
                public boolean isDeclaredInMethod() {
                    return true;
                }

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (other == null || getClass() != other.getClass()) return false;
                    DeclaredInMethod that = (DeclaredInMethod) other;
                    return methodDescriptor.equals(that.methodDescriptor)
                            && methodName.equals(that.methodName)
                            && name.equals(that.name);
                }

                @Override
                public int hashCode() {
                    int result = name.hashCode();
                    result = 31 * result + methodName.hashCode();
                    result = 31 * result + methodDescriptor.hashCode();
                    return result;
                }

                @Override
                public String toString() {
                    return "TypePool.LazyTypeDescription.DeclarationContext.DeclaredInMethod{" +
                            "name='" + name + '\'' +
                            ", methodName='" + methodName + '\'' +
                            ", methodDescriptor='" + methodDescriptor + '\'' +
                            '}';
                }
            }
        }

        /**
         * A token for representing collected data on an annotation.
         */
        protected static class AnnotationToken {

            /**
             * The descriptor of the represented annotation.
             */
            private final String descriptor;

            /**
             * A map of annotation value names to their value representations.
             */
            private final Map<String, AnnotationDescription.AnnotationValue<?, ?>> values;

            /**
             * Creates a new annotation token.
             *
             * @param descriptor The descriptor of the represented annotation.
             * @param values     A map of annotation value names to their value representations.
             */
            protected AnnotationToken(String descriptor, Map<String, AnnotationDescription.AnnotationValue<?, ?>> values) {
                this.descriptor = descriptor;
                this.values = values;
            }

            /**
             * Returns the descriptor of the represented annotation.
             *
             * @return The descriptor of the represented annotation.
             */
            public String getDescriptor() {
                return descriptor;
            }

            /**
             * Returns a map of annotation value names to their value representations.
             *
             * @return A map of annotation value names to their value representations.
             */
            public Map<String, AnnotationDescription.AnnotationValue<?, ?>> getValues() {
                return values;
            }

            /**
             * Transforms this token into an annotation description.
             *
             * @param typePool The type pool to be used for looking up linked types.
             * @return An annotation description that resembles this token.
             */
            private AnnotationDescription toAnnotationDescription(TypePool typePool) {
                return new LazyAnnotationDescription(typePool, descriptor, values);
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                AnnotationToken that = (AnnotationToken) other;
                return descriptor.equals(that.descriptor)
                        && values.equals(that.values);
            }

            @Override
            public int hashCode() {
                int result = descriptor.hashCode();
                result = 31 * result + values.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "TypePool.LazyTypeDescription.AnnotationToken{" +
                        "descriptor='" + descriptor + '\'' +
                        ", values=" + values +
                        '}';
            }
        }

        /**
         * A token for representing collected data on a field.
         */
        protected static class FieldToken {

            /**
             * The modifiers of the represented field.
             */
            private final int modifiers;

            /**
             * The name of the field.
             */
            private final String name;

            /**
             * The descriptor of the field.
             */
            private final String descriptor;

            private final GenericTypeToken.Resolution.ForField signatureResoltion;

            /**
             * A list of annotation tokens representing the annotations of the represented field.
             */
            private final List<AnnotationToken> annotationTokens;

            protected FieldToken(int modifiers,
                                 String name,
                                 String descriptor,
                                 GenericTypeToken.Resolution.ForField signatureResoltion,
                                 List<AnnotationToken> annotationTokens) {
                this.modifiers = modifiers;
                this.name = name;
                this.descriptor = descriptor;
                this.signatureResoltion = signatureResoltion;
                this.annotationTokens = annotationTokens;
            }

            /**
             * Returns the modifiers of the represented field.
             *
             * @return The modifiers of the represented field.
             */
            protected int getModifiers() {
                return modifiers;
            }

            /**
             * Returns the name of the represented field.
             *
             * @return The name of the represented field.
             */
            protected String getName() {
                return name;
            }

            /**
             * Returns the descriptor of the represented field.
             *
             * @return The descriptor of the represented field.
             */
            protected String getDescriptor() {
                return descriptor;
            }

            protected GenericTypeToken.Resolution.ForField getSignatureResolution() {
                return signatureResoltion;
            }

            /**
             * Returns a list of annotation tokens of the represented field.
             *
             * @return A list of annotation tokens of the represented field.
             */
            protected List<AnnotationToken> getAnnotationTokens() {
                return annotationTokens;
            }

            /**
             * Transforms this token into a lazy field description.
             *
             * @param lazyTypeDescription The lazy type description to attach this field description to.
             * @return A field description resembling this field token.
             */
            private FieldDescription toFieldDescription(LazyTypeDescription lazyTypeDescription) {
                return lazyTypeDescription.new LazyFieldDescription(getModifiers(),
                        getName(),
                        getDescriptor(),
                        getSignatureResolution(),
                        getAnnotationTokens());
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                FieldToken that = (FieldToken) other;
                return modifiers == that.modifiers
                        && annotationTokens.equals(that.annotationTokens)
                        && descriptor.equals(that.descriptor)
                        && signatureResoltion.equals(that.signatureResoltion)
                        && name.equals(that.name);
            }

            @Override
            public int hashCode() {
                int result = modifiers;
                result = 31 * result + name.hashCode();
                result = 31 * result + descriptor.hashCode();
                result = 31 * result + signatureResoltion.hashCode();
                result = 31 * result + annotationTokens.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "TypePool.LazyTypeDescription.FieldToken{" +
                        "modifiers=" + modifiers +
                        ", name='" + name + '\'' +
                        ", descriptor='" + descriptor + '\'' +
                        ", signatureResoltion=" + signatureResoltion +
                        ", annotationTokens=" + annotationTokens +
                        '}';
            }
        }

        /**
         * A token for representing collected data on a method.
         */
        protected static class MethodToken {

            /**
             * The modifiers of the represented method.
             */
            private final int modifiers;

            /**
             * The internal name of the represented method.
             */
            private final String name;

            /**
             * The descriptor of the represented method.
             */
            private final String descriptor;

            private final GenericTypeToken.Resolution.ForMethod signatureResolution;

            /**
             * An array of internal names of the exceptions of the represented method or {@code null} if there
             * are no such exceptions.
             */
            private final String[] exceptionName;

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
            private final AnnotationDescription.AnnotationValue<?, ?> defaultValue;

            protected MethodToken(int modifiers,
                                  String name,
                                  String descriptor,
                                  GenericTypeToken.Resolution.ForMethod signatureResolution,
                                  String[] exceptionName,
                                  List<AnnotationToken> annotationTokens,
                                  Map<Integer, List<AnnotationToken>> parameterAnnotationTokens,
                                  List<ParameterToken> parameterTokens,
                                  AnnotationDescription.AnnotationValue<?, ?> defaultValue) {
                this.modifiers = modifiers;
                this.name = name;
                this.descriptor = descriptor;
                this.signatureResolution = signatureResolution;
                this.exceptionName = exceptionName;
                this.annotationTokens = annotationTokens;
                this.parameterAnnotationTokens = parameterAnnotationTokens;
                this.parameterTokens = parameterTokens;
                this.defaultValue = defaultValue;
            }

            /**
             * Returns the modifiers of the represented method.
             *
             * @return The modifiers of the represented method.
             */
            protected int getModifiers() {
                return modifiers;
            }

            /**
             * Returns the internal name of the represented method.
             *
             * @return The internal name of the represented method.
             */
            protected String getName() {
                return name;
            }

            /**
             * Returns the descriptor of the represented method.
             *
             * @return The descriptor of the represented method.
             */
            protected String getDescriptor() {
                return descriptor;
            }

            protected GenericTypeToken.Resolution.ForMethod getSignatureResolution() {
                return signatureResolution;
            }

            /**
             * Returns the internal names of the exception type declared of the represented method.
             *
             * @return The internal names of the exception type declared of the represented method.
             */
            protected String[] getExceptionName() {
                return exceptionName;
            }

            /**
             * Returns a list of annotation tokens declared by the represented method.
             *
             * @return A list of annotation tokens declared by the represented method.
             */
            protected List<AnnotationToken> getAnnotationTokens() {
                return annotationTokens;
            }

            /**
             * Returns a map of parameter type indices to a list of annotation tokens representing these annotations.
             *
             * @return A map of parameter type indices to a list of annotation tokens representing these annotations.
             */
            protected Map<Integer, List<AnnotationToken>> getParameterAnnotationTokens() {
                return parameterAnnotationTokens;
            }

            /**
             * Returns the parameter tokens for this type. These tokens might be out of sync with the method's
             * parameters if the meta information attached to a method is not available or corrupt.
             *
             * @return A list of parameter tokens to the described method.
             */
            protected List<ParameterToken> getParameterTokens() {
                return parameterTokens;
            }

            /**
             * Returns the default value of the represented method or {@code null} if no such values exists.
             *
             * @return The default value of the represented method or {@code null} if no such values exists.
             */
            protected AnnotationDescription.AnnotationValue<?, ?> getDefaultValue() {
                return defaultValue;
            }

            /**
             * Transforms this method token to a method description that is attached to a lazy type description.
             *
             * @param lazyTypeDescription The lazy type description to attach this method description to.
             * @return A method description representing this field token.
             */
            private MethodDescription toMethodDescription(LazyTypeDescription lazyTypeDescription) {
                return lazyTypeDescription.new LazyMethodDescription(getModifiers(),
                        getName(),
                        getDescriptor(),
                        getSignatureResolution(),
                        getExceptionName(),
                        getAnnotationTokens(),
                        getParameterAnnotationTokens(),
                        getParameterTokens(),
                        getDefaultValue());
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                MethodToken that = (MethodToken) other;
                return modifiers == that.modifiers
                        && annotationTokens.equals(that.annotationTokens)
                        && defaultValue.equals(that.defaultValue)
                        && descriptor.equals(that.descriptor)
                        && parameterTokens.equals(that.parameterTokens)
                        && signatureResolution.equals(that.signatureResolution)
                        && Arrays.equals(exceptionName, that.exceptionName)
                        && name.equals(that.name)
                        && parameterAnnotationTokens.equals(that.parameterAnnotationTokens);
            }

            @Override
            public int hashCode() {
                int result = modifiers;
                result = 31 * result + name.hashCode();
                result = 31 * result + descriptor.hashCode();
                result = 31 * result + signatureResolution.hashCode();
                result = 31 * result + Arrays.hashCode(exceptionName);
                result = 31 * result + annotationTokens.hashCode();
                result = 31 * result + parameterAnnotationTokens.hashCode();
                result = 31 * result + parameterTokens.hashCode();
                result = 31 * result + defaultValue.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "TypePool.LazyTypeDescription.MethodToken{" +
                        "modifiers=" + modifiers +
                        ", name='" + name + '\'' +
                        ", descriptor='" + descriptor + '\'' +
                        ", signatureResolution=" + signatureResolution +
                        ", exceptionName=" + Arrays.toString(exceptionName) +
                        ", annotationTokens=" + annotationTokens +
                        ", parameterAnnotationTokens=" + parameterAnnotationTokens +
                        ", parameterTokens=" + parameterTokens +
                        ", defaultValue=" + defaultValue +
                        '}';
            }

            /**
             * A token representing a method's parameter.
             */
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
                private final String name;

                /**
                 * The modifiers of the parameter or {@code null} if no modifiers are known for this parameter.
                 */
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

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (other == null || getClass() != other.getClass()) return false;
                    ParameterToken that = ((ParameterToken) other);
                    return !(modifiers != null ? !modifiers.equals(that.modifiers) : that.modifiers != null)
                            && !(name != null ? !name.equals(that.name) : that.name != null);
                }

                @Override
                public int hashCode() {
                    int result = name != null ? name.hashCode() : 0;
                    result = 31 * result + (modifiers != null ? modifiers.hashCode() : 0);
                    return result;
                }

                @Override
                public String toString() {
                    return "TypePool.LazyTypeDescription.MethodToken.ParameterToken{" +
                            "name='" + name + '\'' +
                            ", modifiers=" + modifiers +
                            '}';
                }
            }
        }

        protected interface GenericTypeToken {

            Sort getSort();

            GenericTypeDescription toGenericType(TypePool typePool, TypeVariableSource typeVariableSource);

            interface Resolution {

                GenericTypeList resolveTypeVariables(TypePool typePool, TypeVariableSource typeVariableSource);

                interface ForType extends Resolution {

                    GenericTypeDescription resolveSuperType(String superTypeDescriptor, TypePool typePool, TypeDescription definingType);

                    GenericTypeList resolveInterfaceTypes(List<String> interfaceTypeDescriptors, TypePool typePool, TypeDescription definingType);

                    class Tokenized implements ForType {

                        private final GenericTypeToken superTypeToken;

                        private final List<GenericTypeToken> interfaceTypeTokens;

                        private final List<GenericTypeToken> typeVariableTokens;

                        public Tokenized(GenericTypeToken superTypeToken,
                                         List<GenericTypeToken> interfaceTypeTokens,
                                         List<GenericTypeToken> typeVariableTokens) {
                            this.superTypeToken = superTypeToken;
                            this.interfaceTypeTokens = interfaceTypeTokens;
                            this.typeVariableTokens = typeVariableTokens;
                        }

                        @Override
                        public GenericTypeDescription resolveSuperType(String superTypeDescriptor, TypePool typePool, TypeDescription definingType) {
                            return new TokenizedGenericType(typePool, superTypeToken, superTypeDescriptor, definingType);
                        }

                        @Override
                        public GenericTypeList resolveInterfaceTypes(List<String> interfaceTypeDescriptors, TypePool typePool, TypeDescription definingType) {
                            return TokenizedGenericType.TokenList.of(typePool, interfaceTypeTokens, interfaceTypeDescriptors, definingType);
                        }

                        @Override
                        public GenericTypeList resolveTypeVariables(TypePool typePool, TypeVariableSource typeVariableSource) {
                            return TokenizedGenericType.TypeVariableList.of(typePool, typeVariableTokens, typeVariableSource);
                        }
                    }
                }

                interface ForMethod extends Resolution {

                    GenericTypeDescription resolveReturnType(String returnTypeDescriptor, TypePool typePool, MethodDescription definingMethod);

                    GenericTypeList resolveParameterTypes(List<String> parameterTypeDescriptors, TypePool typePool, MethodDescription definingMethod);

                    GenericTypeList resolveExceptionTypes(List<String> exceptionTypeDescriptors, TypePool typePool, MethodDescription definingMethod);

                    class Tokenized implements ForMethod {

                        private final GenericTypeToken returnTypeToken;

                        private final List<GenericTypeToken> parameterTypeTokens;

                        private final List<GenericTypeToken> exceptionTypeTokens;

                        private final List<GenericTypeToken> typeVariableTokens;

                        public Tokenized(GenericTypeToken returnTypeToken,
                                         List<GenericTypeToken> parameterTypeTokens,
                                         List<GenericTypeToken> exceptionTypeTokens,
                                         List<GenericTypeToken> typeVariableTokens) {
                            this.returnTypeToken = returnTypeToken;
                            this.parameterTypeTokens = parameterTypeTokens;
                            this.exceptionTypeTokens = exceptionTypeTokens;
                            this.typeVariableTokens = typeVariableTokens;
                        }

                        @Override
                        public GenericTypeDescription resolveReturnType(String returnTypeDescriptor, TypePool typePool, MethodDescription definingMethod) {
                            return new TokenizedGenericType(typePool, returnTypeToken, returnTypeDescriptor, definingMethod);
                        }

                        @Override
                        public GenericTypeList resolveParameterTypes(List<String> parameterTypeDescriptors, TypePool typePool, MethodDescription definingMethod) {
                            return TokenizedGenericType.TokenList.of(typePool, parameterTypeTokens, parameterTypeDescriptors, definingMethod);
                        }

                        @Override
                        public GenericTypeList resolveExceptionTypes(List<String> exceptionTypeDescriptors, TypePool typePool, MethodDescription definingMethod) {
                            return TokenizedGenericType.TokenList.of(typePool, exceptionTypeTokens, exceptionTypeDescriptors, definingMethod);
                        }

                        @Override
                        public GenericTypeList resolveTypeVariables(TypePool typePool, TypeVariableSource typeVariableSource) {
                            return TokenizedGenericType.TypeVariableList.of(typePool, typeVariableTokens, typeVariableSource);
                        }
                    }
                }

                interface ForField {

                    GenericTypeDescription resolveFieldType(String fieldTypeDescriptor, TypePool typePool, FieldDescription definingField);

                    class Tokenized implements ForField {

                        private final GenericTypeToken fieldTypeToken;

                        public Tokenized(GenericTypeToken fieldTypeToken) {
                            this.fieldTypeToken = fieldTypeToken;
                        }

                        @Override
                        public GenericTypeDescription resolveFieldType(String fieldTypeDescriptor, TypePool typePool, FieldDescription definingField) {
                            return new TokenizedGenericType(typePool, fieldTypeToken, fieldTypeDescriptor, definingField.getDeclaringType());
                        }
                    }
                }

                enum Raw implements ForType, ForMethod, ForField {

                    INSTANCE;

                    @Override
                    public GenericTypeDescription resolveFieldType(String fieldTypeDescriptor, TypePool typePool, FieldDescription definingField) {
                        return TokenizedGenericType.toRawType(typePool, fieldTypeDescriptor);
                    }

                    @Override
                    public GenericTypeDescription resolveReturnType(String returnTypeDescriptor, TypePool typePool, MethodDescription definingMethod) {
                        return TokenizedGenericType.toRawType(typePool, returnTypeDescriptor);
                    }

                    @Override
                    public GenericTypeList resolveParameterTypes(List<String> parameterTypeDescriptors, TypePool typePool, MethodDescription definingMethod) {
                        return LazyTypeList.of(typePool, parameterTypeDescriptors).asGenericTypes();
                    }

                    @Override
                    public GenericTypeList resolveExceptionTypes(List<String> exceptionTypeDescriptors, TypePool typePool, MethodDescription definingMethod) {
                        return LazyTypeList.of(typePool, exceptionTypeDescriptors).asGenericTypes();
                    }

                    @Override
                    public GenericTypeDescription resolveSuperType(String superTypeDescriptor, TypePool typePool, TypeDescription definingType) {
                        return TokenizedGenericType.toRawType(typePool, superTypeDescriptor);
                    }

                    @Override
                    public GenericTypeList resolveInterfaceTypes(List<String> interfaceTypeDescriptors, TypePool typePool, TypeDescription definingType) {
                        return LazyTypeList.of(typePool, interfaceTypeDescriptors).asGenericTypes();
                    }

                    @Override
                    public GenericTypeList resolveTypeVariables(TypePool typePool, TypeVariableSource typeVariableSource) {
                        return new GenericTypeList.Empty();
                    }
                }

                enum Defective implements ForType, ForMethod, ForField {

                    INSTANCE;

                    @Override
                    public GenericTypeDescription resolveFieldType(String fieldTypeDescriptor, TypePool typePool, FieldDescription definingField) {
                        return new TokenizedGenericType.Defective(typePool, fieldTypeDescriptor);
                    }

                    @Override
                    public GenericTypeDescription resolveReturnType(String returnTypeDescriptor, TypePool typePool, MethodDescription definingMethod) {
                        return new TokenizedGenericType.Defective(typePool, returnTypeDescriptor);
                    }

                    @Override
                    public GenericTypeList resolveParameterTypes(List<String> parameterTypeDescriptors, TypePool typePool, MethodDescription definingMethod) {
                        return new TokenizedGenericType.Defective.TokenList(typePool, parameterTypeDescriptors);
                    }

                    @Override
                    public GenericTypeList resolveExceptionTypes(List<String> exceptionTypeDescriptors, TypePool typePool, MethodDescription definingMethod) {
                        return new TokenizedGenericType.Defective.TokenList(typePool, exceptionTypeDescriptors);
                    }

                    @Override
                    public GenericTypeDescription resolveSuperType(String superTypeDescriptor, TypePool typePool, TypeDescription definingType) {
                        return new TokenizedGenericType.Defective(typePool, superTypeDescriptor);
                    }

                    @Override
                    public GenericTypeList resolveInterfaceTypes(List<String> interfaceTypeDescriptors, TypePool typePool, TypeDescription definingType) {
                        return new TokenizedGenericType.Defective.TokenList(typePool, interfaceTypeDescriptors);
                    }

                    @Override
                    public GenericTypeList resolveTypeVariables(TypePool typePool, TypeVariableSource typeVariableSource) {
                        throw new MalformedParameterizedTypeException();
                    }
                }
            }

            enum ForPrimitiveType implements GenericTypeToken {

                BOOLEAN(boolean.class),
                BYTE(byte.class),
                SHORT(short.class),
                CHAR(char.class),
                INTEGER(int.class),
                LONG(long.class),
                FLOAT(float.class),
                DOUBLE(double.class),
                VOID(void.class);

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
                            throw new IllegalArgumentException("Not a valid descriptor: " + descriptor);
                    }
                }

                private final TypeDescription typeDescription;

                ForPrimitiveType(Class<?> type) {
                    typeDescription = new TypeDescription.ForLoadedType(type);
                }

                @Override
                public Sort getSort() {
                    return Sort.RAW;
                }

                @Override
                public GenericTypeDescription toGenericType(TypePool typePool, TypeVariableSource typeVariableSource) {
                    return typeDescription;
                }
            }

            class ForRawType implements GenericTypeToken {

                private final String name;

                public ForRawType(String name) {
                    this.name = name;
                }

                @Override
                public Sort getSort() {
                    return Sort.RAW;
                }

                @Override
                public GenericTypeDescription toGenericType(TypePool typePool, TypeVariableSource typeVariableSource) {
                    return typePool.describe(name).resolve();
                }
            }

            class ForTypeVariable implements GenericTypeToken {

                private final String symbol;

                public ForTypeVariable(String symbol) {
                    this.symbol = symbol;
                }

                @Override
                public Sort getSort() {
                    return Sort.VARIABLE;
                }

                @Override
                public GenericTypeDescription toGenericType(TypePool typePool, TypeVariableSource typeVariableSource) {
                    GenericTypeDescription typeVariable = typeVariableSource.findVariable(symbol);
                    if (typeVariable == null) {
                        throw new IllegalStateException("Cannot resolve type variable '" + symbol + "' for " + typeVariableSource);
                    } else {
                        return typeVariable;
                    }
                }

                public static class Formal implements GenericTypeToken {

                    private final String symbol;

                    private final List<GenericTypeToken> bounds;

                    public Formal(String symbol, List<GenericTypeToken> bounds) {
                        this.symbol = symbol;
                        this.bounds = bounds;
                    }

                    @Override
                    public Sort getSort() {
                        return Sort.VARIABLE;
                    }

                    @Override
                    public GenericTypeDescription toGenericType(TypePool typePool, TypeVariableSource typeVariableSource) {
                        return new LazyTypeVariable(typePool, typeVariableSource);
                    }

                    protected class LazyTypeVariable extends GenericTypeDescription.ForTypeVariable {

                        private final TypePool typePool;

                        private final TypeVariableSource typeVariableSource;

                        protected LazyTypeVariable(TypePool typePool, TypeVariableSource typeVariableSource) {
                            this.typePool = typePool;
                            this.typeVariableSource = typeVariableSource;
                        }

                        @Override
                        public GenericTypeList getUpperBounds() {
                            List<GenericTypeDescription> genericTypeDescriptions = new ArrayList<GenericTypeDescription>(bounds.size());
                            for (GenericTypeToken bound : bounds) {
                                genericTypeDescriptions.add(bound.toGenericType(typePool, typeVariableSource));
                            }
                            return new GenericTypeList.Explicit(genericTypeDescriptions);
                        }

                        @Override
                        public TypeVariableSource getVariableSource() {
                            return typeVariableSource;
                        }

                        @Override
                        public String getSymbol() {
                            return symbol;
                        }
                    }
                }
            }

            class ForArray implements GenericTypeToken {

                private final GenericTypeToken componentTypeToken;

                public ForArray(GenericTypeToken componentTypeToken) {
                    this.componentTypeToken = componentTypeToken;
                }

                @Override
                public Sort getSort() {
                    return Sort.GENERIC_ARRAY;
                }

                @Override
                public GenericTypeDescription toGenericType(TypePool typePool, TypeVariableSource typeVariableSource) {
                    return GenericTypeDescription.ForGenericArray.Latent.of(componentTypeToken.toGenericType(typePool, typeVariableSource), 1);
                }
            }

            class ForLowerBoundWildcard implements GenericTypeToken {

                private final GenericTypeToken baseType;

                public ForLowerBoundWildcard(GenericTypeToken baseType) {
                    this.baseType = baseType;
                }

                @Override
                public Sort getSort() {
                    return Sort.WILDCARD;
                }

                @Override
                public GenericTypeDescription toGenericType(TypePool typePool, TypeVariableSource typeVariableSource) {
                    return GenericTypeDescription.ForWildcardType.Latent.boundedBelow(baseType.toGenericType(typePool, typeVariableSource));
                }
            }

            class ForUpperBoundWildcard implements GenericTypeToken {

                private final GenericTypeToken baseType;

                public ForUpperBoundWildcard(GenericTypeToken baseType) {
                    this.baseType = baseType;
                }

                @Override
                public Sort getSort() {
                    return Sort.WILDCARD;
                }

                @Override
                public GenericTypeDescription toGenericType(TypePool typePool, TypeVariableSource typeVariableSource) {
                    return GenericTypeDescription.ForWildcardType.Latent.boundedAbove(baseType.toGenericType(typePool, typeVariableSource));
                }
            }

            enum ForUnboundWildcard implements GenericTypeToken {

                INSTANCE;

                @Override
                public Sort getSort() {
                    return Sort.WILDCARD;
                }

                @Override
                public GenericTypeDescription toGenericType(TypePool typePool, TypeVariableSource typeVariableSource) {
                    return GenericTypeDescription.ForWildcardType.Latent.unbounded();
                }
            }

            class ForParameterizedType implements GenericTypeToken {

                private final String name;

                private final List<GenericTypeToken> parameters;

                public ForParameterizedType(String name, List<GenericTypeToken> parameters) {
                    this.name = name;
                    this.parameters = parameters;
                }

                @Override
                public Sort getSort() {
                    return Sort.PARAMETERIZED;
                }

                @Override
                public GenericTypeDescription toGenericType(TypePool typePool, TypeVariableSource typeVariableSource) {
                    return new LazyParameterizedType(typePool, typeVariableSource);
                }

                protected class LazyParameterizedType extends GenericTypeDescription.ForParameterizedType {

                    private final TypePool typePool;

                    private final TypeVariableSource typeVariableSource;

                    public LazyParameterizedType(TypePool typePool, TypeVariableSource typeVariableSource) {
                        this.typePool = typePool;
                        this.typeVariableSource = typeVariableSource;
                    }

                    @Override
                    public TypeDescription asRawType() {
                        return typePool.describe(name).resolve();
                    }

                    @Override
                    public GenericTypeList getParameters() {
                        List<GenericTypeDescription> genericTypeDescriptions = new ArrayList<GenericTypeDescription>(parameters.size());
                        for (GenericTypeToken parameter : parameters) {
                            genericTypeDescriptions.add(parameter.toGenericType(typePool, typeVariableSource));
                        }
                        return new GenericTypeList.Explicit(genericTypeDescriptions);
                    }

                    @Override
                    public GenericTypeDescription getOwnerType() {
                        return typePool.describe(name).resolve().getEnclosingType();
                    }
                }

                public static class Nested implements GenericTypeToken {

                    private final String name;

                    private final List<GenericTypeToken> parameters;

                    private final GenericTypeToken ownerType;

                    public Nested(String name, List<GenericTypeToken> parameters, GenericTypeToken ownerType) {
                        this.name = name;
                        this.parameters = parameters;
                        this.ownerType = ownerType;
                    }

                    @Override
                    public Sort getSort() {
                        return Sort.PARAMETERIZED;
                    }

                    @Override
                    public GenericTypeDescription toGenericType(TypePool typePool, TypeVariableSource typeVariableSource) {
                        return new LazyParameterizedType(typePool, typeVariableSource);
                    }

                    protected class LazyParameterizedType extends GenericTypeDescription.ForParameterizedType {

                        private final TypePool typePool;

                        private final TypeVariableSource typeVariableSource;

                        public LazyParameterizedType(TypePool typePool, TypeVariableSource typeVariableSource) {
                            this.typePool = typePool;
                            this.typeVariableSource = typeVariableSource;
                        }

                        @Override
                        public TypeDescription asRawType() {
                            return typePool.describe(name).resolve();
                        }

                        @Override
                        public GenericTypeList getParameters() {
                            List<GenericTypeDescription> genericTypeDescriptions = new ArrayList<GenericTypeDescription>(parameters.size());
                            for (GenericTypeToken parameter : parameters) {
                                genericTypeDescriptions.add(parameter.toGenericType(typePool, typeVariableSource));
                            }
                            return new GenericTypeList.Explicit(genericTypeDescriptions);
                        }

                        @Override
                        public GenericTypeDescription getOwnerType() {
                            return ownerType.toGenericType(typePool, typeVariableSource);
                        }
                    }
                }
            }
        }

        /**
         * A lazy description of an annotation that looks up types from a type pool when required.
         */
        private static class LazyAnnotationDescription extends AnnotationDescription.AbstractAnnotationDescription {

            /**
             * The type pool for looking up type references.
             */
            protected final TypePool typePool;

            /**
             * A map of annotation values by their property name.
             */
            protected final Map<String, AnnotationValue<?, ?>> values;

            /**
             * The descriptor of this annotation.
             */
            private final String descriptor;

            /**
             * Creates a new lazy annotation description.
             *
             * @param typePool   The type pool to be used for looking up linked types.
             * @param descriptor The descriptor of the annotation type.
             * @param values     A map of annotation value names to their value representations.
             */
            private LazyAnnotationDescription(TypePool typePool,
                                              String descriptor,
                                              Map<String, AnnotationValue<?, ?>> values) {
                this.typePool = typePool;
                this.descriptor = descriptor;
                this.values = values;
            }

            @Override
            public Object getValue(MethodDescription methodDescription) {
                if (!methodDescription.getDeclaringType().getDescriptor().equals(descriptor)) {
                    throw new IllegalArgumentException(methodDescription + " is not declared by " + getAnnotationType());
                }
                AnnotationValue<?, ?> annotationValue = values.get(methodDescription.getName());
                Object value = annotationValue == null
                        ? getAnnotationType().getDeclaredMethods().filter(is(methodDescription)).getOnly().getDefaultValue()
                        : annotationValue.resolve();
                if (value == null) {
                    throw new IllegalStateException(methodDescription + " is not defined on annotation");
                }
                return PropertyDispatcher.of(value.getClass()).conditionalClone(value);
            }

            @Override
            public TypeDescription getAnnotationType() {
                return typePool.describe(descriptor.substring(1, descriptor.length() - 1).replace('/', '.')).resolve();
            }

            @Override
            public <T extends Annotation> Loadable<T> prepare(Class<T> annotationType) {
                return new Loadable<T>(typePool, descriptor, values, annotationType);
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
                 * @param descriptor     The descriptor of the represented annotation.
                 * @param values         A map of annotation value names to their value representations.
                 * @param annotationType The loaded annotation type.
                 */
                private Loadable(TypePool typePool,
                                 String descriptor,
                                 Map<String, AnnotationValue<?, ?>> values,
                                 Class<S> annotationType) {
                    super(typePool, descriptor, values);
                    if (!Type.getDescriptor(annotationType).equals(descriptor)) {
                        throw new IllegalArgumentException(annotationType + " does not correspond to " + descriptor);
                    }
                    this.annotationType = annotationType;
                }

                @Override
                public S load() throws ClassNotFoundException {
                    return load(annotationType.getClassLoader());
                }

                @Override
                @SuppressWarnings("unchecked")
                public S load(ClassLoader classLoader) throws ClassNotFoundException {
                    return (S) Proxy.newProxyInstance(classLoader,
                            new Class<?>[]{annotationType},
                            AnnotationInvocationHandler.of(annotationType.getClassLoader(), annotationType, values));
                }

                @Override
                public S loadSilent() {
                    try {
                        return load();
                    } catch (ClassNotFoundException e) {
                        throw new IllegalStateException(ForLoadedAnnotation.ERROR_MESSAGE, e);
                    }
                }

                @Override
                public S loadSilent(ClassLoader classLoader) {
                    try {
                        return load(classLoader);
                    } catch (ClassNotFoundException e) {
                        throw new IllegalStateException(ForLoadedAnnotation.ERROR_MESSAGE, e);
                    }
                }
            }
        }

        /**
         * An implementation of a {@link PackageDescription} that only
         * loads its annotations on requirement.
         */
        private static class LazyPackageDescription extends PackageDescription.AbstractPackageDescription {

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

            @Override
            public boolean isSealed() {
                return false;
            }
        }

        /**
         * A lazy field description that only resolved type references when required.
         */
        private class LazyFieldDescription extends FieldDescription.AbstractFieldDescription {

            /**
             * The modifiers of the field.
             */
            private final int modifiers;

            /**
             * The name of the field.
             */
            private final String name;

            private final String fieldTypeDescriptor;

            private final GenericTypeToken.Resolution.ForField signatureResolution;

            /**
             * A list of annotation descriptions of this field.
             */
            private final List<AnnotationDescription> declaredAnnotations;

            private LazyFieldDescription(int modifiers,
                                         String name,
                                         String descriptor,
                                         GenericTypeToken.Resolution.ForField signatureResolution,
                                         List<AnnotationToken> annotationTokens) {
                this.modifiers = modifiers;
                this.name = name;
                fieldTypeDescriptor = descriptor;
                this.signatureResolution = signatureResolution;
                declaredAnnotations = new ArrayList<AnnotationDescription>(annotationTokens.size());
                for (AnnotationToken annotationToken : annotationTokens) {
                    declaredAnnotations.add(annotationToken.toAnnotationDescription(typePool));
                }
            }

            @Override
            public GenericTypeDescription getFieldTypeGen() {
                return signatureResolution.resolveFieldType(fieldTypeDescriptor, typePool, this);
            }

            @Override
            public AnnotationList getDeclaredAnnotations() {
                return new AnnotationList.Explicit(declaredAnnotations);
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
        }

        /**
         * A lazy representation of a method that resolves references to types only on demand.
         */
        private class LazyMethodDescription extends MethodDescription.AbstractMethodDescription {

            /**
             * The modifiers of this method.
             */
            private final int modifiers;

            /**
             * The internal name of this method.
             */
            private final String internalName;

            private final String returnTypeDescriptor;

            private final GenericTypeToken.Resolution.ForMethod signatureResolution;

            private final List<String> parameterTypeDescriptors;

            private final List<String> exceptionTypeDescriptors;

            /**
             * A list of annotation descriptions that are declared by this method.
             */
            private final List<AnnotationDescription> declaredAnnotations;

            /**
             * A nested list of annotation descriptions that are declared by the parameters of this
             * method in their oder.
             */
            private final List<List<AnnotationDescription>> declaredParameterAnnotations;

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
            private final AnnotationDescription.AnnotationValue<?, ?> defaultValue;

            /**
             * Creates a new lazy method description.
             *
             * @param modifiers                 The modifiers of the represented method.
             * @param internalName              The internal name of this method.
             * @param methodDescriptor          The method descriptor of this method.
             * @param exceptionTypeInternalName The internal names of the exceptions that are declared by this
             *                                  method or {@code null} if no exceptions are declared by this
             *                                  method.
             * @param annotationTokens          A list of annotation tokens representing annotations that are declared
             *                                  by this method.
             * @param parameterAnnotationTokens A nested list of annotation tokens representing annotations that are
             *                                  declared by the fields of this method.
             * @param parameterTokens           A list of parameter tokens which might be empty or even out of sync
             *                                  with the actual parameters if the debugging information found in a
             *                                  class was corrupt.
             * @param defaultValue              The default value of this method or {@code null} if there is no
             *                                  such value.
             */
            private LazyMethodDescription(int modifiers,
                                          String internalName,
                                          String methodDescriptor,
                                          GenericTypeToken.Resolution.ForMethod signatureResolution,
                                          String[] exceptionTypeInternalName,
                                          List<AnnotationToken> annotationTokens,
                                          Map<Integer, List<AnnotationToken>> parameterAnnotationTokens,
                                          List<MethodToken.ParameterToken> parameterTokens,
                                          AnnotationDescription.AnnotationValue<?, ?> defaultValue) {
                this.modifiers = modifiers;
                this.internalName = internalName;
                Type methodType = Type.getMethodType(methodDescriptor);
                Type returnType = methodType.getReturnType();
                Type[] parameterType = methodType.getArgumentTypes();
                returnTypeDescriptor = returnType.getDescriptor();
                parameterTypeDescriptors = new ArrayList<String>(parameterType.length);
                for (Type type : parameterType) {
                    parameterTypeDescriptors.add(type.getDescriptor());
                }
                this.signatureResolution = signatureResolution;
                if (exceptionTypeInternalName == null) {
                    exceptionTypeDescriptors = Collections.emptyList();
                } else {
                    exceptionTypeDescriptors = new ArrayList<String>(exceptionTypeInternalName.length);
                    for (String anExceptionTypeInternalName : exceptionTypeInternalName) {
                        exceptionTypeDescriptors.add(Type.getObjectType(anExceptionTypeInternalName).getDescriptor());
                    }
                }
                declaredAnnotations = new ArrayList<AnnotationDescription>(annotationTokens.size());
                for (AnnotationToken annotationToken : annotationTokens) {
                    declaredAnnotations.add(annotationToken.toAnnotationDescription(typePool));
                }
                declaredParameterAnnotations = new ArrayList<List<AnnotationDescription>>(parameterType.length);
                for (int index = 0; index < parameterType.length; index++) {
                    List<AnnotationToken> tokens = parameterAnnotationTokens.get(index);
                    List<AnnotationDescription> annotationDescriptions;
                    annotationDescriptions = new ArrayList<AnnotationDescription>(tokens.size());
                    for (AnnotationToken annotationToken : tokens) {
                        annotationDescriptions.add(annotationToken.toAnnotationDescription(typePool));
                    }
                    declaredParameterAnnotations.add(annotationDescriptions);
                }
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
            public GenericTypeDescription getReturnTypeGen() {
                return signatureResolution.resolveReturnType(returnTypeDescriptor, typePool, this);
            }

            @Override
            public GenericTypeList getExceptionTypesGen() {
                return signatureResolution.resolveExceptionTypes(exceptionTypeDescriptors, typePool, this);
            }

            @Override
            public ParameterList getParameters() {
                return new LazyParameterList();
            }

            @Override
            public AnnotationList getDeclaredAnnotations() {
                return new AnnotationList.Explicit(declaredAnnotations);
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
            public GenericTypeList getTypeVariables() {
                return signatureResolution.resolveTypeVariables(typePool, this);
            }

            @Override
            public Object getDefaultValue() {
                return defaultValue == null
                        ? null
                        : defaultValue.resolve();
            }

            /**
             * A lazy list of parameter descriptions for the enclosing method description.
             */
            private class LazyParameterList extends FilterableList.AbstractBase<ParameterDescription, ParameterList> implements ParameterList {

                @Override
                protected ParameterList wrap(List<ParameterDescription> values) {
                    return new Explicit(values);
                }

                @Override
                public ParameterDescription get(int index) {
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
                public TypeList asTypeList() {
                    return LazyTypeList.of(typePool, parameterTypeDescriptors);
                }

                @Override
                public GenericTypeList asTypeListGen() {
                    return signatureResolution.resolveParameterTypes(parameterTypeDescriptors, typePool, LazyMethodDescription.this);
                }
            }

            /**
             * A lazy description of a parameters of the enclosing method.
             */
            private class LazyParameterDescription extends ParameterDescription.AbstractParameterDescription {

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
                public TypeDescription getType() {
                    return TokenizedGenericType.toRawType(typePool, parameterTypeDescriptors.get(index));
                }

                @Override
                public MethodDescription getDeclaringMethod() {
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
                public GenericTypeDescription getTypeGen() {
                    return signatureResolution.resolveParameterTypes(parameterTypeDescriptors, typePool, LazyMethodDescription.this).get(index);
                }

                @Override
                public AnnotationList getDeclaredAnnotations() {
                    return new AnnotationList.Explicit(declaredParameterAnnotations.get(index));
                }
            }
        }

        /**
         * A list that is constructing {@link net.bytebuddy.pool.TypePool.LazyTypeDescription}s.
         */
        private static class LazyTypeList extends TypeList.AbstractBase {

            private final TypePool typePool;

            private final List<String> descriptors;

            protected static TypeList of(TypePool typePool, List<String> descriptors) {
                return descriptors.isEmpty()
                        ? new TypeList.Empty()
                        : new LazyTypeList(typePool, descriptors);
            }

            private LazyTypeList(TypePool typePool, List<String> descriptors) {
                this.typePool = typePool;
                this.descriptors = descriptors;
            }

            @Override
            public TypeDescription get(int index) {
                return TokenizedGenericType.toRawType(typePool, descriptors.get(index));
            }

            @Override
            public int size() {
                return descriptors.size();
            }

            @Override
            public String[] toInternalNames() {
                if (descriptors.isEmpty()) {
                    return null;
                } else {
                    String[] internalName = new String[descriptors.size()];
                    int index = 0;
                    for (String descriptor : descriptors) {
                        internalName[index++] = Type.getType(descriptor).getInternalName();
                    }
                    return internalName;
                }
            }

            @Override
            public int getStackSize() {
                int stackSize = 0;
                for (String descriptor : descriptors) {
                    stackSize += Type.getType(descriptor).getSize();
                }
                return stackSize;
            }

            @Override
            public GenericTypeList asGenericTypes() {
                return new Generified();
            }

            private class Generified extends GenericTypeList.AbstractBase {

                @Override
                public GenericTypeDescription get(int index) {
                    return LazyTypeList.this.get(index);
                }

                @Override
                public int size() {
                    return LazyTypeList.this.size();
                }

                @Override
                public TypeList asRawTypes() {
                    return LazyTypeList.this;
                }
            }
        }

        private static class TokenizedGenericType extends GenericTypeDescription.LazyProjection {

            protected static TypeDescription toRawType(TypePool typePool, String descriptor) {
                Type type = Type.getType(descriptor);
                return typePool.describe(type.getSort() == Type.ARRAY
                        ? type.getInternalName().replace('/', '.')
                        : type.getClassName()).resolve();
            }

            private final TypePool typePool;

            private final GenericTypeToken genericTypeToken;

            private final String rawTypeDescriptor;

            private final TypeVariableSource typeVariableSource;

            protected TokenizedGenericType(TypePool typePool, GenericTypeToken genericTypeToken, String rawTypeDescriptor, TypeVariableSource typeVariableSource) {
                this.typePool = typePool;
                this.genericTypeToken = genericTypeToken;
                this.rawTypeDescriptor = rawTypeDescriptor;
                this.typeVariableSource = typeVariableSource;
            }

            @Override
            public Sort getSort() {
                return genericTypeToken.getSort();
            }

            @Override
            protected GenericTypeDescription resolve() {
                return genericTypeToken.toGenericType(typePool, typeVariableSource);
            }

            @Override
            public TypeDescription asRawType() {
                return toRawType(typePool, rawTypeDescriptor);
            }

            protected static class TokenList extends GenericTypeList.AbstractBase {

                private final TypePool typePool;

                private final List<GenericTypeToken> genericTypeTokens;

                private final List<String> rawTypeDescriptors;

                private final TypeVariableSource typeVariableSource;

                protected static GenericTypeList of(TypePool typePool,
                                                    List<GenericTypeToken> genericTypeTokens,
                                                    List<String> rawTypeDescriptors,
                                                    TypeVariableSource typeVariableSource) {
                    return rawTypeDescriptors.isEmpty()
                            ? new GenericTypeList.Empty()
                            : new TokenList(typePool, genericTypeTokens, rawTypeDescriptors, typeVariableSource);
                }

                private TokenList(TypePool typePool,
                                  List<GenericTypeToken> genericTypeTokens,
                                  List<String> rawTypeDescriptors,
                                  TypeVariableSource typeVariableSource) {
                    this.typePool = typePool;
                    this.genericTypeTokens = genericTypeTokens;
                    this.rawTypeDescriptors = rawTypeDescriptors;
                    this.typeVariableSource = typeVariableSource;
                }

                @Override
                public GenericTypeDescription get(int index) {
                    return new TokenizedGenericType(typePool, genericTypeTokens.get(index), rawTypeDescriptors.get(index), typeVariableSource);
                }

                @Override
                public int size() {
                    return rawTypeDescriptors.size();
                }

                @Override
                public TypeList asRawTypes() {
                    return LazyTypeList.of(typePool, rawTypeDescriptors);
                }
            }

            protected static class TypeVariableList extends GenericTypeList.AbstractBase {

                protected static GenericTypeList of(TypePool typePool,
                                                    List<GenericTypeToken> typeVariables,
                                                    TypeVariableSource typeVariableSource) {
                    return typeVariables.isEmpty()
                            ? new GenericTypeList.Empty()
                            : new TypeVariableList(typePool, typeVariables, typeVariableSource);
                }

                private final TypePool typePool;

                private final List<GenericTypeToken> typeVariables;

                private final TypeVariableSource typeVariableSource;

                private TypeVariableList(TypePool typePool,
                                         List<GenericTypeToken> typeVariables,
                                         TypeVariableSource typeVariableSource) {
                    this.typePool = typePool;
                    this.typeVariables = typeVariables;
                    this.typeVariableSource = typeVariableSource;
                }

                @Override
                public GenericTypeDescription get(int index) {
                    return typeVariables.get(index).toGenericType(typePool, typeVariableSource);
                }

                @Override
                public int size() {
                    return typeVariables.size();
                }

                @Override
                public TypeList asRawTypes() {
                    List<TypeDescription> typeDescriptions = new ArrayList<TypeDescription>();
                    for (GenericTypeDescription typeVariable : this) {
                        typeDescriptions.add(typeVariable.asRawType());
                    }
                    return new TypeList.Explicit(typeDescriptions);
                }
            }

            protected static class Defective extends GenericTypeDescription.LazyProjection {

                private final TypePool typePool;

                private final String rawTypeDescriptor;

                protected Defective(TypePool typePool, String rawTypeDescriptor) {
                    this.typePool = typePool;
                    this.rawTypeDescriptor = rawTypeDescriptor;
                }

                @Override
                protected GenericTypeDescription resolve() {
                    throw new MalformedParameterizedTypeException();
                }

                @Override
                public TypeDescription asRawType() {
                    return toRawType(typePool, rawTypeDescriptor);
                }

                protected static class TokenList extends GenericTypeList.AbstractBase {

                    private final TypePool typePool;

                    private final List<String> rawTypeDescriptors;

                    private TokenList(TypePool typePool, List<String> rawTypeDescriptors) {
                        this.typePool = typePool;
                        this.rawTypeDescriptors = rawTypeDescriptors;
                    }

                    @Override
                    public GenericTypeDescription get(int index) {
                        return new TokenizedGenericType.Defective(typePool, rawTypeDescriptors.get(index));
                    }

                    @Override
                    public int size() {
                        return rawTypeDescriptors.size();
                    }

                    @Override
                    public TypeList asRawTypes() {
                        return LazyTypeList.of(typePool, rawTypeDescriptors);
                    }
                }

            }
        }
    }
}
