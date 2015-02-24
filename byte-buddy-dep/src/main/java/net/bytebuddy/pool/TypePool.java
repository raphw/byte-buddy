package net.bytebuddy.pool;

import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.instrumentation.attribute.annotation.AnnotationDescription;
import net.bytebuddy.instrumentation.attribute.annotation.AnnotationList;
import net.bytebuddy.instrumentation.field.FieldDescription;
import net.bytebuddy.instrumentation.field.FieldList;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodList;
import net.bytebuddy.instrumentation.type.PackageDescription;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.matcher.FilterableList;
import net.bytebuddy.utility.PropertyDispatcher;
import org.objectweb.asm.*;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.annotation.AnnotationTypeMismatchException;
import java.lang.annotation.IncompleteAnnotationException;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * A type pool allows the retreival of {@link net.bytebuddy.instrumentation.type.TypeDescription} by its name.
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
    static interface Resolution {

        /**
         * Determines if this resolution represents a {@link net.bytebuddy.instrumentation.type.TypeDescription}.
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
         * A simple resolution that represents a given {@link net.bytebuddy.instrumentation.type.TypeDescription}.
         */
        static class Simple implements Resolution {

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
        static class Illegal implements Resolution {

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
    static interface CacheProvider {

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
        static enum NoOp implements CacheProvider {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public Resolution find(String name) {
                return null;
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
        static class Simple implements CacheProvider {

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
                return cached == null ? resolution : cached;
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
    abstract static class AbstractBase implements TypePool {

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

        /**
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
    }

    /**
     * A default implementation of a {@link net.bytebuddy.pool.TypePool} that models binary data in the
     * Java byte code format into a {@link net.bytebuddy.instrumentation.type.TypeDescription}. The data lookup
     * is delegated to a {@link net.bytebuddy.dynamic.ClassFileLocator}.
     */
    static class Default extends AbstractBase {

        /**
         * The ASM version that is applied when reading class files.
         */
        private static final int ASM_VERSION = Opcodes.ASM5;

        /**
         * A flag to indicate ASM that no automatic calculations are requested.
         */
        private static final int ASM_MANUAL = 0;

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
            classReader.accept(typeExtractor, ASM_MANUAL);
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
        protected static interface AnnotationRegistrant {

            /**
             * Registers an annotation value.
             *
             * @param name            The name of the annotation value.
             * @param annotationValue The value of the annotation.
             */
            void register(String name, LazyTypeDescription.AnnotationValue<?, ?> annotationValue);

            /**
             * Called once all annotation values are visited.
             */
            void onComplete();
        }

        /**
         * A component type locator allows for the lazy location of an array's component type.
         */
        protected static interface ComponentTypeLocator {

            /**
             * Binds this component type to a given property name of an annotation.
             *
             * @param name The name of an annotation property which the returned component type reference should
             *             query for resolving an array's component type.
             * @return A component type reference to an annotation value's component type.
             */
            LazyTypeDescription.AnnotationValue.ForComplexArray.ComponentTypeReference bind(String name);

            /**
             * A component type locator which cannot legally resolve an array's component type.
             */
            static enum Illegal implements ComponentTypeLocator {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public LazyTypeDescription.AnnotationValue.ForComplexArray.ComponentTypeReference bind(String name) {
                    throw new IllegalStateException("Unexpected lookup of component type for " + name);
                }
            }

            /**
             * A component type locator that lazily analyses an annotation for resolving an annotation property's
             * array value's component type.
             */
            static class ForAnnotationProperty implements ComponentTypeLocator {

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
                public LazyTypeDescription.AnnotationValue.ForComplexArray.ComponentTypeReference bind(String name) {
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
                protected class Bound implements LazyTypeDescription.AnnotationValue.ForComplexArray.ComponentTypeReference {

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
            static class ForArrayType implements ComponentTypeLocator, LazyTypeDescription.AnnotationValue.ForComplexArray.ComponentTypeReference {

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
                public LazyTypeDescription.AnnotationValue.ForComplexArray.ComponentTypeReference bind(String name) {
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
                super(ASM_VERSION);
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
                if (internalName.equals(MethodDescription.TYPE_INITIALIZER_INTERNAL_NAME)) {
                    return null;
                }
                return new MethodExtractor(modifiers, internalName, descriptor, genericSignature, exceptionName);
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
                        genericSignature,
                        interfaceName,
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
                private final Map<String, LazyTypeDescription.AnnotationValue<?, ?>> values;

                /**
                 * Creates a new on type collector.
                 *
                 * @param descriptor The descriptor of the annotation that is being collected.
                 */
                protected OnTypeCollector(String descriptor) {
                    this.descriptor = descriptor;
                    values = new HashMap<String, LazyTypeDescription.AnnotationValue<?, ?>>();
                }

                @Override
                public void register(String name, LazyTypeDescription.AnnotationValue<?, ?> annotationValue) {
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
                    super(ASM_VERSION);
                    this.annotationRegistrant = annotationRegistrant;
                    this.componentTypeLocator = componentTypeLocator;
                }

                @Override
                public void visit(String name, Object value) {
                    LazyTypeDescription.AnnotationValue<?, ?> annotationValue;
                    if (value instanceof Type) {
                        annotationValue = new LazyTypeDescription.AnnotationValue.ForType((Type) value);
                    } else if (value.getClass().isArray()) {
                        annotationValue = new LazyTypeDescription.AnnotationValue.Trivial<Object>(value);
                    } else {
                        annotationValue = new LazyTypeDescription.AnnotationValue.Trivial<Object>(value);
                    }
                    annotationRegistrant.register(name, annotationValue);
                }

                @Override
                public void visitEnum(String name, String descriptor, String value) {
                    annotationRegistrant.register(name, new LazyTypeDescription.AnnotationValue.ForEnumeration(descriptor, value));
                }

                @Override
                public AnnotationVisitor visitAnnotation(String name, String descriptor) {
                    return new AnnotationExtractor(new AnnotationLookup(name, descriptor),
                            new ComponentTypeLocator.ForAnnotationProperty(TypePool.Default.this, descriptor));
                }

                @Override
                public AnnotationVisitor visitArray(String name) {
                    return new AnnotationExtractor(new ArrayLookup(name, componentTypeLocator.bind(name)),
                            ComponentTypeLocator.Illegal.INSTANCE);
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
                    private final LazyTypeDescription.AnnotationValue.ForComplexArray.ComponentTypeReference componentTypeReference;

                    /**
                     * A list of all annotation values that are found on this array.
                     */
                    private final List<LazyTypeDescription.AnnotationValue<?, ?>> values;

                    /**
                     * Creates a new annotation registrant for an array lookup.
                     *
                     * @param name                   The name of the annotation property the collected array is representing.
                     * @param componentTypeReference A lazy reference to resolve the component type of the collected array.
                     */
                    protected ArrayLookup(String name,
                                          LazyTypeDescription.AnnotationValue.ForComplexArray.ComponentTypeReference componentTypeReference) {
                        this.name = name;
                        this.componentTypeReference = componentTypeReference;
                        values = new LinkedList<LazyTypeDescription.AnnotationValue<?, ?>>();
                    }

                    @Override
                    public void register(String ignored, LazyTypeDescription.AnnotationValue<?, ?> annotationValue) {
                        values.add(annotationValue);
                    }

                    @Override
                    public void onComplete() {
                        annotationRegistrant.register(name, new LazyTypeDescription.AnnotationValue.ForComplexArray(componentTypeReference, values));
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
                    private final Map<String, LazyTypeDescription.AnnotationValue<?, ?>> values;

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
                        values = new HashMap<String, LazyTypeDescription.AnnotationValue<?, ?>>();
                    }

                    @Override
                    public void register(String name, LazyTypeDescription.AnnotationValue<?, ?> annotationValue) {
                        values.put(name, annotationValue);
                    }

                    @Override
                    public void onComplete() {
                        annotationRegistrant.register(name, new LazyTypeDescription.AnnotationValue
                                .ForAnnotation(new LazyTypeDescription.AnnotationToken(descriptor, values)));
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
                    super(ASM_VERSION);
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
                            genericSignature,
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
                    private final Map<String, LazyTypeDescription.AnnotationValue<?, ?>> values;

                    /**
                     * Creates a new annotation field registrant.
                     *
                     * @param descriptor The descriptor of the annotation.
                     */
                    protected OnFieldCollector(String descriptor) {
                        this.descriptor = descriptor;
                        values = new HashMap<String, LazyTypeDescription.AnnotationValue<?, ?>>();
                    }

                    @Override
                    public void register(String name, LazyTypeDescription.AnnotationValue<?, ?> annotationValue) {
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
                 * The default value of the found method or {@code null} if no such value exists.
                 */
                private LazyTypeDescription.AnnotationValue<?, ?> defaultValue;

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
                    super(ASM_VERSION);
                    this.modifiers = modifiers;
                    this.internalName = internalName;
                    this.descriptor = descriptor;
                    this.genericSignature = genericSignature;
                    this.exceptionName = exceptionName;
                    annotationTokens = new LinkedList<LazyTypeDescription.AnnotationToken>();
                    parameterAnnotationTokens = new HashMap<Integer, List<LazyTypeDescription.AnnotationToken>>();
                    for (int i = 0; i < Type.getMethodType(descriptor).getArgumentTypes().length; i++) {
                        parameterAnnotationTokens.put(i, new LinkedList<LazyTypeDescription.AnnotationToken>());
                    }
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
                public AnnotationVisitor visitAnnotationDefault() {
                    return new AnnotationExtractor(this, new ComponentTypeLocator.ForArrayType(descriptor));
                }

                @Override
                public void register(String ignored, LazyTypeDescription.AnnotationValue<?, ?> annotationValue) {
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
                            genericSignature,
                            exceptionName,
                            annotationTokens,
                            parameterAnnotationTokens,
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
                    private final Map<String, LazyTypeDescription.AnnotationValue<?, ?>> values;

                    /**
                     * Creates a new method annotation registrant.
                     *
                     * @param descriptor The descriptor of the annotation.
                     */
                    protected OnMethodCollector(String descriptor) {
                        this.descriptor = descriptor;
                        values = new HashMap<String, LazyTypeDescription.AnnotationValue<?, ?>>();
                    }

                    @Override
                    public void register(String name, LazyTypeDescription.AnnotationValue<?, ?> annotationValue) {
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
                    private final Map<String, LazyTypeDescription.AnnotationValue<?, ?>> values;

                    /**
                     * Creates a new method parameter annotation registrant.
                     *
                     * @param descriptor The descriptor of the annotation.
                     * @param index      The index of the parameter of this annotation.
                     */
                    protected OnMethodParameterCollector(String descriptor, int index) {
                        this.descriptor = descriptor;
                        this.index = index;
                        values = new HashMap<String, LazyTypeDescription.AnnotationValue<?, ?>>();
                    }

                    @Override
                    public void register(String name, LazyTypeDescription.AnnotationValue<?, ?> annotationValue) {
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
     * A type description that looks up any referenced {@link net.bytebuddy.instrumentation.ByteCodeElement}s or
     * {@link net.bytebuddy.instrumentation.attribute.annotation.AnnotationDescription}s by querying a type pool
     * at lookup time.
     */
    static class LazyTypeDescription extends TypeDescription.AbstractTypeDescription.OfSimpleType {

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

        /**
         * The binary name of the super type of this type or {@code null} if no such type exists.
         */
        private final String superTypeName;

        /**
         * An array of internal names of all interfaces implemented by this type or {@code null} if no such
         * interfaces exist.
         */
        private final String[] interfaceInternalName;

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

        /**
         * The generic signature of the type or {@code null} if the code is not generic.
         */
        private final String genericSignature;

        /**
         * Creates a new lazy type description.
         *
         * @param typePool           The type pool to be used for looking up linked types.
         * @param modifiers          The modifiers of this type.
         * @param name               The internal name of this type.
         * @param superTypeName      The internal name of this type's super type or {@code null} if no such type
         *                           exists.
         * @param genericSignature   The generic signature of the type or {@code null} if the code is not generic.
         * @param interfaceName      An array of the internal names of all implemented interfaces or {@code null} if no
         *                           interfaces are implemented.
         * @param declarationContext The declaration context of this type.
         * @param anonymousType      {@code true} if this type is an anonymous type.
         * @param annotationTokens   A list of annotation tokens representing methods that are declared by this type.
         * @param fieldTokens        A list of field tokens representing methods that are declared by this type.
         * @param methodTokens       A list of method tokens representing methods that are declared by this type.
         */
        protected LazyTypeDescription(TypePool typePool,
                                      int modifiers,
                                      String name,
                                      String superTypeName,
                                      String genericSignature,
                                      String[] interfaceName,
                                      DeclarationContext declarationContext,
                                      boolean anonymousType,
                                      List<AnnotationToken> annotationTokens,
                                      List<FieldToken> fieldTokens,
                                      List<MethodToken> methodTokens) {
            this.typePool = typePool;
            this.modifiers = modifiers;
            this.name = name.replace('/', '.');
            this.superTypeName = superTypeName == null ? null : superTypeName.replace('/', '.');
            this.interfaceInternalName = interfaceName;
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
            this.genericSignature = genericSignature;
        }

        @Override
        public TypeDescription getSupertype() {
            return superTypeName == null || isInterface()
                    ? null
                    : typePool.describe(superTypeName).resolve();
        }

        @Override
        public TypeList getInterfaces() {
            return interfaceInternalName == null
                    ? new TypeList.Empty()
                    : new LazyTypeList(interfaceInternalName);
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
        public String getCanonicalName() {
            return name.replace('$', '.');
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
                    : new LazyPackageDescription(packageName);
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
        public String getGenericSignature() {
            return genericSignature;
        }

        /**
         * A declaration context encapsulates information about whether a type was declared within another type
         * or within a method of another type.
         */
        protected static interface DeclarationContext {

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
            static enum SelfDeclared implements DeclarationContext {

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

            }

            /**
             * A declaration context representing a type that is declared within another type but not within
             * a method.
             */
            static class DeclaredInType implements DeclarationContext {

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
            static class DeclaredInMethod implements DeclarationContext {

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
         * A value of a {@link net.bytebuddy.pool.TypePool.LazyTypeDescription.LazyAnnotationDescription} which
         * is not yet loaded by a class loader.
         *
         * @param <T> The type of the unloaded value of this annotation.
         * @param <S> The type of the loaded value of this annotation.
         */
        protected static interface AnnotationValue<T, S> {

            /**
             * Resolves the unloaded value of this annotation.
             *
             * @param typePool The type pool to be used for looking up linked types.
             * @return The unloaded value of this annotation.
             */
            T resolve(TypePool typePool);

            /**
             * Returns the loaded value of this annotation.
             *
             * @param classLoader The class loader for loading this value.
             * @return The loaded value of this annotation.
             * @throws ClassNotFoundException If a type that represents a loaded value cannot be found.
             */
            Loaded<S> load(ClassLoader classLoader) throws ClassNotFoundException;

            /**
             * A loaded variant of a {@link net.bytebuddy.pool.TypePool.LazyTypeDescription.AnnotationValue}. While
             * implementations of this value are required to be processed successfully by a
             * {@link java.lang.ClassLoader} they might still be unresolved. Typical errors on loading an annotation
             * value are:
             * <ul>
             * <li>{@link java.lang.annotation.IncompleteAnnotationException}: An annotation does not define a value
             * even though no default value for a property is provided.</li>
             * <li>{@link java.lang.EnumConstantNotPresentException}: An annotation defines an unknown value for
             * a known enumeration.</li>
             * <li>{@link java.lang.annotation.AnnotationTypeMismatchException}: An annotation property is not
             * of the expected type.</li>
             * </ul>
             * Implementations of this interface must implement methods for {@link Object#hashCode()} and
             * {@link Object#toString()} that resemble those used for the annotation values of an actual
             * {@link java.lang.annotation.Annotation} implementation. Also, instances must implement
             * {@link java.lang.Object#equals(Object)} to return {@code true} for other instances of
             * this interface that represent the same annotation value.
             *
             * @param <U> The type of the loaded value of this annotation.
             */
            static interface Loaded<U> {

                /**
                 * Returns the state of the represented loaded annotation value.
                 *
                 * @return The state represented by this instance.
                 */
                State getState();

                /**
                 * Resolves the value to the actual value of an annotation. Calling this method might throw a runtime
                 * exception if this value is either not defined or not resolved.
                 *
                 * @return The actual annotation value represented by this instance.
                 */
                U resolve();

                /**
                 * Represents the state of a
                 * {@link net.bytebuddy.pool.TypePool.LazyTypeDescription.AnnotationValue.Loaded} annotation property.
                 */
                static enum State {

                    /**
                     * A non-defined annotation value describes an annotation property which is missing such that
                     * an {@link java.lang.annotation.IncompleteAnnotationException} would be thrown.
                     */
                    NON_DEFINED,

                    /**
                     * A non-resolved annotation value describes an annotation property which does not represent a
                     * valid value but an exceptional state.
                     */
                    NON_RESOLVED,

                    /**
                     * A resolved annotation value describes an annotation property with an actual value.
                     */
                    RESOLVED;

                    /**
                     * Returns {@code true} if the related annotation value is defined, i.e. either represents
                     * an actual value or an exceptional state.
                     *
                     * @return {@code true} if the related annotation value is defined.
                     */
                    public boolean isDefined() {
                        return this != NON_DEFINED;
                    }

                    /**
                     * Returns {@code true} if the related annotation value is resolved, i.e. represents an actual
                     * value.
                     *
                     * @return {@code true} if the related annotation value is resolved.
                     */
                    public boolean isResolved() {
                        return this == RESOLVED;
                    }
                }
            }

            /**
             * Represents a primitive value, a {@link java.lang.String} or an array of the latter types.
             *
             * @param <U> The type where primitive values are represented by their boxed type.
             */
            static class Trivial<U> implements AnnotationValue<U, U> {

                /**
                 * The represented value.
                 */
                private final U value;

                /**
                 * A property dispatcher for the given value.
                 */
                private final PropertyDispatcher propertyDispatcher;

                /**
                 * Creates a new representation of a trivial annotation value.
                 *
                 * @param value The value to represent.
                 */
                public Trivial(U value) {
                    this.value = value;
                    propertyDispatcher = PropertyDispatcher.of(value.getClass());
                }

                @Override
                public U resolve(TypePool typePool) {
                    return value;
                }

                @Override
                public AnnotationValue.Loaded<U> load(ClassLoader classLoader) {
                    return new Loaded<U>(value, propertyDispatcher);
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && propertyDispatcher.equals(value, ((Trivial) other).value);
                }

                @Override
                public int hashCode() {
                    return propertyDispatcher.hashCode(value);
                }

                @Override
                public String toString() {
                    return "TypePool.LazyTypeDescription.AnnotationValue.Trivial{" +
                            "value=" + value +
                            ", propertyDispatcher=" + propertyDispatcher +
                            '}';
                }

                /**
                 * Represents a trivial loaded value.
                 *
                 * @param <V> The annotation properties type.
                 */
                protected static class Loaded<V> implements AnnotationValue.Loaded<V> {

                    /**
                     * The represented value.
                     */
                    private final V value;

                    /**
                     * The property dispatcher for computing this value's hash code, string and equals implementation.
                     */
                    private final PropertyDispatcher propertyDispatcher;

                    /**
                     * Creates a new trivial loaded annotation value representation.
                     *
                     * @param value              The represented value.
                     * @param propertyDispatcher The property dispatcher for computing this value's hash
                     *                           code, string and equals implementation.
                     */
                    protected Loaded(V value, PropertyDispatcher propertyDispatcher) {
                        this.value = value;
                        this.propertyDispatcher = propertyDispatcher;
                    }

                    @Override
                    public State getState() {
                        return State.RESOLVED;
                    }

                    @Override
                    public V resolve() {
                        return propertyDispatcher.conditionalClone(value);
                    }

                    @Override
                    public int hashCode() {
                        return propertyDispatcher.hashCode(value);
                    }

                    @Override
                    public boolean equals(Object other) {
                        if (this == other) return true;
                        if (!(other instanceof AnnotationValue.Loaded<?>)) return false;
                        AnnotationValue.Loaded<?> loadedOther = (AnnotationValue.Loaded<?>) other;
                        return loadedOther.getState().isResolved() && propertyDispatcher.equals(value, loadedOther.resolve());
                    }

                    @Override
                    public String toString() {
                        return propertyDispatcher.toString(value);
                    }
                }
            }

            /**
             * Represents a nested annotation value.
             */
            static class ForAnnotation implements AnnotationValue<AnnotationDescription, Annotation> {

                /**
                 * The annotation token that represents the nested invocation.
                 */
                private final AnnotationToken annotationToken;

                /**
                 * Creates a new annotation value for a nested annotation.
                 *
                 * @param annotationToken The token that represents the annotation.
                 */
                public ForAnnotation(AnnotationToken annotationToken) {
                    this.annotationToken = annotationToken;
                }

                @Override
                public AnnotationDescription resolve(TypePool typePool) {
                    return annotationToken.toAnnotationDescription(typePool);
                }

                @Override
                @SuppressWarnings("unchecked")
                public Loaded<Annotation> load(ClassLoader classLoader) throws ClassNotFoundException {
                    Class<?> type = classLoader.loadClass(annotationToken.getDescriptor()
                            .substring(1, annotationToken.getDescriptor().length() - 1)
                            .replace('/', '.'));
                    return type.isAnnotation()
                            ? new LegalRuntimeType(classLoader, (Class<? extends Annotation>) type, annotationToken.getValues())
                            : new IncompatibleRuntimeType(type);
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && annotationToken.equals(((ForAnnotation) other).annotationToken);
                }

                @Override
                public int hashCode() {
                    return annotationToken.hashCode();
                }

                @Override
                public String toString() {
                    return "TypePool.LazyTypeDescription.AnnotationValue.ForAnnotation{" +
                            "annotationToken=" + annotationToken +
                            '}';
                }

                /**
                 * Represents an annotation which was loaded representing a runtime type which is itself an annotation.
                 */
                protected static class LegalRuntimeType implements Loaded<Annotation> {

                    /**
                     * The loaded annotation.
                     */
                    private final Annotation annotation;

                    /**
                     * Creates a representation for an annotation value.
                     *
                     * @param classLoader      The class loader for loading the annotation's implementing type and its values.
                     * @param annotationType   The loaded annotation type.
                     * @param annotationValues The values of the annotation.
                     * @throws ClassNotFoundException If a linked class cannot be found.
                     */
                    public LegalRuntimeType(ClassLoader classLoader,
                                            Class<? extends Annotation> annotationType,
                                            Map<String, AnnotationValue<?, ?>> annotationValues) throws ClassNotFoundException {
                        annotation = (Annotation) Proxy.newProxyInstance(classLoader,
                                new Class<?>[]{annotationType},
                                new AnnotationInvocationHandler(classLoader, annotationType, annotationValues));
                    }

                    @Override
                    public State getState() {
                        return State.RESOLVED;
                    }

                    @Override
                    public Annotation resolve() {
                        return annotation;
                    }

                    @Override
                    public boolean equals(Object other) {
                        if (this == other) return true;
                        if (!(other instanceof AnnotationValue.Loaded<?>)) return false;
                        AnnotationValue.Loaded<?> loadedOther = (AnnotationValue.Loaded<?>) other;
                        return loadedOther.getState().isResolved() && annotation.equals(loadedOther.resolve());
                    }

                    @Override
                    public int hashCode() {
                        return annotation.hashCode();
                    }

                    @Override
                    public String toString() {
                        return annotation.toString();
                    }
                }

                /**
                 * <p>
                 * Represents an annotation value which was attempted to ba loaded by a type that does not represent
                 * an annotation value.
                 * </p>
                 * <p>
                 * <b>Note</b>: Neither of {@link Object#hashCode()}, {@link Object#toString()} and
                 * {@link java.lang.Object#equals(Object)} are implemented specifically what resembles the way
                 * such exceptional states are represented in the Open JDK's annotation implementations.
                 * </p>
                 */
                protected static class IncompatibleRuntimeType implements Loaded<Annotation> {

                    /**
                     * The incompatible runtime type which is not an annotation type.
                     */
                    private final Class<?> incompatibleType;

                    /**
                     * Creates a new representation for an annotation with an incompatible runtime type.
                     *
                     * @param incompatibleType The incompatible runtime type which is not an annotation type.
                     */
                    public IncompatibleRuntimeType(Class<?> incompatibleType) {
                        this.incompatibleType = incompatibleType;
                    }

                    @Override
                    public State getState() {
                        return State.NON_RESOLVED;
                    }

                    @Override
                    public Annotation resolve() {
                        throw new IncompatibleClassChangeError("Not an annotation type: " + incompatibleType.toString());
                    }
                }
            }

            /**
             * Represents an enumeration value of an annotation.
             */
            static class ForEnumeration implements AnnotationValue<AnnotationDescription.EnumerationValue, Enum<?>> {

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
                 * @param descriptor The descriptor of the enumeration type.
                 * @param value      The name of the enumeration.
                 */
                public ForEnumeration(String descriptor, String value) {
                    this.descriptor = descriptor;
                    this.value = value;
                }

                @Override
                public AnnotationDescription.EnumerationValue resolve(TypePool typePool) {
                    return new LazyEnumerationValue(typePool);
                }

                @Override
                @SuppressWarnings("unchecked")
                public Loaded<Enum<?>> load(ClassLoader classLoader) throws ClassNotFoundException {
                    Class<?> enumType = classLoader.loadClass(descriptor
                            .substring(1, descriptor.length() - 1).replace('/', '.'));
                    try {
                        return enumType.isEnum()
                                ? new LegalRuntimeEnumeration(Enum.valueOf((Class) enumType, value))
                                : new IncompatibleRuntimeType(enumType);
                    } catch (IllegalArgumentException ignored) {
                        return new UnknownRuntimeEnumeration((Class) enumType, value);
                    }
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && descriptor.equals(((ForEnumeration) other).descriptor)
                            && value.equals(((ForEnumeration) other).value);
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
                 * Represents an annotation's enumeration value which could be successfully resolved for a runtime
                 * type.
                 */
                protected static class LegalRuntimeEnumeration implements Loaded<Enum<?>> {

                    /**
                     * The represented value.
                     */
                    private final Enum<?> value;

                    /**
                     * Creates a representation for an enumeration value.
                     *
                     * @param value The represented value.
                     */
                    public LegalRuntimeEnumeration(Enum<?> value) {
                        this.value = value;
                    }

                    @Override
                    public State getState() {
                        return State.RESOLVED;
                    }

                    @Override
                    public Enum<?> resolve() {
                        return value;
                    }

                    @Override
                    public boolean equals(Object other) {
                        if (this == other) return true;
                        if (!(other instanceof Loaded<?>)) return false;
                        Loaded<?> loadedOther = (Loaded<?>) other;
                        return loadedOther.getState().isResolved() && value.equals(loadedOther.resolve());
                    }

                    @Override
                    public int hashCode() {
                        return value.hashCode();
                    }

                    @Override
                    public String toString() {
                        return value.toString();
                    }
                }

                /**
                 * <p>
                 * Represents an annotation's enumeration value for a constant that does not exist for the runtime
                 * enumeration type.
                 * </p>
                 * <p>
                 * <b>Note</b>: Neither of {@link Object#hashCode()}, {@link Object#toString()} and
                 * {@link java.lang.Object#equals(Object)} are implemented specifically what resembles the way
                 * such exceptional states are represented in the Open JDK's annotation implementations.
                 * </p>
                 */
                protected static class UnknownRuntimeEnumeration implements Loaded<Enum<?>> {

                    /**
                     * The loaded enumeration type.
                     */
                    private final Class<? extends Enum<?>> enumType;

                    /**
                     * The value for which no enumeration constant exists at runtime.
                     */
                    private final String value;

                    /**
                     * Creates a new representation for an unknown enumeration constant of an annotation.
                     *
                     * @param enumType The loaded enumeration type.
                     * @param value    The value for which no enumeration constant exists at runtime.
                     */
                    public UnknownRuntimeEnumeration(Class<? extends Enum<?>> enumType, String value) {
                        this.enumType = enumType;
                        this.value = value;
                    }

                    @Override
                    public State getState() {
                        return State.NON_RESOLVED;
                    }

                    @Override
                    public Enum<?> resolve() {
                        throw new EnumConstantNotPresentException(enumType, value);
                    }
                }

                /**
                 * <p>
                 * Represents an annotation's enumeration value for a runtime type that is not an enumeration type.
                 * </p>
                 * <p>
                 * <b>Note</b>: Neither of {@link Object#hashCode()}, {@link Object#toString()} and
                 * {@link java.lang.Object#equals(Object)} are implemented specifically what resembles the way
                 * such exceptional states are represented in the Open JDK's annotation implementations.
                 * </p>
                 */
                protected static class IncompatibleRuntimeType implements Loaded<Enum<?>> {

                    /**
                     * The runtime type which is not an enumeration type.
                     */
                    private final Class<?> type;

                    /**
                     * Creates a new representation for an incompatible runtime type.
                     *
                     * @param type The runtime type which is not an enumeration type.
                     */
                    public IncompatibleRuntimeType(Class<?> type) {
                        this.type = type;
                    }

                    @Override
                    public State getState() {
                        return State.NON_RESOLVED;
                    }

                    @Override
                    public Enum<?> resolve() {
                        throw new IncompatibleClassChangeError("Not an enumeration type: " + type.toString());
                    }
                }

                /**
                 * An enumeration description where any type references are only resolved on demand.
                 */
                protected class LazyEnumerationValue extends AnnotationDescription.EnumerationValue.AbstractEnumerationValue {

                    /**
                     * The type pool to query for resolving type references.
                     */
                    private final TypePool typePool;

                    /**
                     * Creates a new lazy enumeration value.
                     *
                     * @param typePool The type pool to query for resolving type references.
                     */
                    protected LazyEnumerationValue(TypePool typePool) {
                        this.typePool = typePool;
                    }

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
            static class ForType implements AnnotationValue<TypeDescription, Class<?>> {

                /**
                 * A convenience reference indicating that a loaded type should not be initialized.
                 */
                private static final boolean NO_INITIALIZATION = false;

                /**
                 * The binary name of the type.
                 */
                private final String name;

                /**
                 * Represents a type value of an annotation.
                 *
                 * @param type A type representation of the type that is referenced by the annotation..
                 */
                public ForType(Type type) {
                    name = type.getSort() == Type.ARRAY
                            ? type.getInternalName().replace('/', '.')
                            : type.getClassName();
                }

                @Override
                public TypeDescription resolve(TypePool typePool) {
                    return typePool.describe(name).resolve();
                }

                @Override
                public AnnotationValue.Loaded<Class<?>> load(ClassLoader classLoader) throws ClassNotFoundException {
                    return new Loaded(Class.forName(name, NO_INITIALIZATION, classLoader));
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && name.equals(((ForType) other).name);
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
                protected static class Loaded implements AnnotationValue.Loaded<Class<?>> {

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
                        if (!(other instanceof AnnotationValue.Loaded<?>)) return false;
                        AnnotationValue.Loaded<?> loadedOther = (AnnotationValue.Loaded<?>) other;
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
             * Represents an array that is referenced by an annotation which does not contain primitive values or
             * {@link java.lang.String} values.
             */
            static class ForComplexArray implements AnnotationValue<Object[], Object[]> {

                /**
                 * A reference to the component type.
                 */
                private final ComponentTypeReference componentTypeReference;

                /**
                 * A list of all values of this array value in their order.
                 */
                private List<AnnotationValue<?, ?>> value;

                /**
                 * Creates a new array value representation of a complex array.
                 *
                 * @param componentTypeReference A lazy reference to the component type of this array.
                 * @param value                  A list of all values of this annotation.
                 */
                public ForComplexArray(ComponentTypeReference componentTypeReference,
                                       List<AnnotationValue<?, ?>> value) {
                    this.value = value;
                    this.componentTypeReference = componentTypeReference;
                }

                @Override
                public Object[] resolve(TypePool typePool) {
                    TypeDescription componentTypeDescription = typePool
                            .describe(componentTypeReference.lookup())
                            .resolve();
                    Class<?> componentType;
                    if (componentTypeDescription.represents(Class.class)) {
                        componentType = TypeDescription.class;
                    } else if (componentTypeDescription.isAssignableTo(Enum.class)) {
                        componentType = AnnotationDescription.EnumerationValue.class;
                    } else if (componentTypeDescription.isAssignableTo(Annotation.class)) {
                        componentType = AnnotationDescription.class;
                    } else if (componentTypeDescription.represents(String.class)) {
                        componentType = String.class;
                    } else {
                        throw new IllegalStateException("Unexpected complex array component type " + componentTypeDescription);
                    }
                    Object[] array = (Object[]) Array.newInstance(componentType, value.size());
                    int index = 0;
                    for (AnnotationValue<?, ?> annotationValue : value) {
                        Array.set(array, index++, annotationValue.resolve(typePool));
                    }
                    return array;
                }

                @Override
                public AnnotationValue.Loaded<Object[]> load(ClassLoader classLoader) throws ClassNotFoundException {
                    List<AnnotationValue.Loaded<?>> loadedValues = new ArrayList<AnnotationValue.Loaded<?>>(value.size());
                    for (AnnotationValue<?, ?> value : this.value) {
                        loadedValues.add(value.load(classLoader));
                    }
                    return new Loaded(classLoader.loadClass(componentTypeReference.lookup()), loadedValues);
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && componentTypeReference.equals(((ForComplexArray) other).componentTypeReference)
                            && value.equals(((ForComplexArray) other).value);
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
                public static interface ComponentTypeReference {

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
                protected static class Loaded implements AnnotationValue.Loaded<Object[]> {

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
                                return State.NON_RESOLVED;
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
                    public boolean equals(Object other) {
                        if (this == other) return true;
                        if (!(other instanceof AnnotationValue.Loaded<?>)) return false;
                        AnnotationValue.Loaded<?> loadedOther = (AnnotationValue.Loaded<?>) other;
                        if (!loadedOther.getState().isResolved()) return false;
                        Object otherValue = loadedOther.resolve();
                        if (!(otherValue instanceof Object[])) return false;
                        Object[] otherArrayValue = (Object[]) otherValue;
                        if (values.size() != otherArrayValue.length) return false;
                        Iterator<AnnotationValue.Loaded<?>> iterator = values.iterator();
                        for (Object value : otherArrayValue) {
                            AnnotationValue.Loaded<?> self = iterator.next();
                            if (!self.getState().isResolved() || !self.resolve().equals(value)) {
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
                        StringBuilder stringBuilder = new StringBuilder("[");
                        for (AnnotationValue.Loaded<?> value : values) {
                            stringBuilder.append(value.toString());
                        }
                        return stringBuilder.append("]").toString();
                    }
                }
            }
        }

        /**
         * An invocation handler to represent a loaded annotation of a
         * {@link net.bytebuddy.pool.TypePool.LazyTypeDescription.LazyAnnotationDescription}.
         */
        protected static class AnnotationInvocationHandler implements InvocationHandler {

            /**
             * The name of the {@link Object#hashCode()} method.
             */
            private static final String HASH_CODE = "hashCode";

            /**
             * The name of the {@link Object#equals(Object)} method.
             */
            private static final String EQUALS = "equals";

            /**
             * The name of the {@link Object#toString()} method.
             */
            private static final String TO_STRING = "toString";

            /**
             * The class loader to use.
             */
            private final ClassLoader classLoader;

            /**
             * The loaded annotation type.
             */
            private final Class<? extends Annotation> annotationType;

            /**
             * A sorted list of values of this annotation.
             */
            private final LinkedHashMap<Method, AnnotationValue.Loaded<?>> values;

            /**
             * Creates a new invocation handler.
             *
             * @param classLoader    The class loader for loading this value.
             * @param annotationType The loaded annotation type.
             * @param values         A sorted list of values of this annotation.
             * @throws java.lang.ClassNotFoundException If an annotation value cannot be loaded.
             */
            public AnnotationInvocationHandler(ClassLoader classLoader,
                                               Class<? extends Annotation> annotationType,
                                               Map<String, AnnotationValue<?, ?>> values) throws ClassNotFoundException {
                this.classLoader = classLoader;
                this.annotationType = annotationType;
                Method[] declaredMethod = annotationType.getDeclaredMethods();
                this.values = new LinkedHashMap<Method, AnnotationValue.Loaded<?>>(declaredMethod.length);
                TypeDescription thisType = new ForLoadedType(getClass());
                for (Method method : declaredMethod) {
                    if (!new MethodDescription.ForLoadedMethod(method).isVisibleTo(thisType)) {
                        method.setAccessible(true);
                    }
                    AnnotationValue<?, ?> annotationValue = values.get(method.getName());
                    this.values.put(method, annotationValue == null
                            ? DefaultValue.of(method)
                            : annotationValue.load(classLoader));
                }
            }

            /**
             * Resolves any primitive type to its wrapper type.
             *
             * @param type The type to resolve.
             * @return The resolved type.
             */
            private static Class<?> asWrapper(Class<?> type) {
                if (type.isPrimitive()) {
                    if (type == boolean.class) {
                        return Boolean.class;
                    } else if (type == byte.class) {
                        return Byte.class;
                    } else if (type == short.class) {
                        return Short.class;
                    } else if (type == char.class) {
                        return Character.class;
                    } else if (type == int.class) {
                        return Integer.class;
                    } else if (type == long.class) {
                        return Long.class;
                    } else if (type == float.class) {
                        return Float.class;
                    } else if (type == double.class) {
                        return Double.class;
                    }
                }
                return type;
            }

            @Override
            public Object invoke(Object proxy, Method method, Object[] arguments) {
                if (method.getDeclaringClass() != annotationType) {
                    if (method.getName().equals(HASH_CODE)) {
                        return hashCodeRepresentation();
                    } else if (method.getName().equals(EQUALS) && method.getParameterTypes().length == 1) {
                        return equalsRepresentation(proxy, arguments[0]);
                    } else if (method.getName().equals(TO_STRING)) {
                        return toStringRepresentation();
                    } else /* method.getName().equals("annotationType") */ {
                        return annotationType;
                    }
                }
                Object value = values.get(method).resolve();
                if (!asWrapper(method.getReturnType()).isAssignableFrom(value.getClass())) {
                    throw new AnnotationTypeMismatchException(method, value.getClass().toString());
                }
                return value;
            }

            /**
             * Returns the string representation of the represented annotation.
             *
             * @return The string representation of the represented annotation.
             */
            protected String toStringRepresentation() {
                StringBuilder toString = new StringBuilder();
                toString.append('@');
                toString.append(annotationType.getName());
                toString.append('(');
                boolean firstMember = true;
                for (Map.Entry<Method, AnnotationValue.Loaded<?>> entry : values.entrySet()) {
                    if (!entry.getValue().getState().isDefined()) {
                        continue;
                    }
                    if (firstMember) {
                        firstMember = false;
                    } else {
                        toString.append(", ");
                    }
                    toString.append(entry.getKey().getName());
                    toString.append('=');
                    toString.append(entry.getValue().toString());
                }
                toString.append(')');
                return toString.toString();
            }

            /**
             * Returns the hash code of the represented annotation.
             *
             * @return The hash code of the represented annotation.
             */
            private int hashCodeRepresentation() {
                int hashCode = 0;
                for (Map.Entry<Method, AnnotationValue.Loaded<?>> entry : values.entrySet()) {
                    if (!entry.getValue().getState().isDefined()) {
                        continue;
                    }
                    hashCode += (127 * entry.getKey().getName().hashCode()) ^ entry.getValue().hashCode();
                }
                return hashCode;
            }

            /**
             * Checks if another instance is equal to this instance.
             *
             * @param self  The annotation proxy instance.
             * @param other The instance to be examined for equality to the represented instance.
             * @return {@code true} if the given instance is equal to the represented instance.
             */
            private boolean equalsRepresentation(Object self, Object other) {
                if (self == other) {
                    return true;
                } else if (!annotationType.isInstance(other)) {
                    return false;
                } else if (Proxy.isProxyClass(other.getClass())) {
                    InvocationHandler invocationHandler = Proxy.getInvocationHandler(other);
                    if (invocationHandler instanceof AnnotationInvocationHandler) {
                        return invocationHandler.equals(this);
                    }
                }
                try {
                    for (Map.Entry<Method, AnnotationValue.Loaded<?>> entry : values.entrySet()) {
                        if (entry.getValue().getState().isResolved()) {
                            try {
                                if (!PropertyDispatcher.of(entry.getKey().getReturnType())
                                        .equals(entry.getValue().resolve(), entry.getKey().invoke(other))) {
                                    return false;
                                }
                            } catch (RuntimeException e) {
                                return false; // Incomplete annotations are not equal to one another.
                            }
                        } else {
                            return false;
                        }
                    }
                } catch (InvocationTargetException ignored) {
                    return false;
                } catch (IllegalAccessException e) {
                    throw new AssertionError(e);
                }
                return true;
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (!(other instanceof AnnotationInvocationHandler)) return false;
                AnnotationInvocationHandler that = (AnnotationInvocationHandler) other;
                if (!annotationType.equals(that.annotationType)) {
                    return false;
                }
                for (Map.Entry<Method, ?> entry : values.entrySet()) {
                    Object value = that.values.get(entry.getKey());
                    if (!PropertyDispatcher.of(value.getClass()).equals(value, entry.getValue())) {
                        return false;
                    }
                }
                return true;
            }

            @Override
            public int hashCode() {
                int result = annotationType.hashCode();
                result = 31 * result + values.hashCode();
                for (Map.Entry<Method, ?> entry : values.entrySet()) {
                    result = 31 * result + PropertyDispatcher.of(entry.getValue().getClass()).hashCode(entry.getValue());
                }
                return result;
            }

            @Override
            public String toString() {
                return "TypePool.LazyTypeDescription.AnnotationInvocationHandler{" +
                        "annotationType=" + annotationType +
                        ", classLoader=" + classLoader +
                        ", values=" + values +
                        '}';
            }

            /**
             * Represents a default value for an annotation property that is not explicitly defined by
             * an annotation.
             */
            protected static class DefaultValue implements AnnotationValue.Loaded<Object> {

                /**
                 * The represented default value.
                 */
                private final Object defaultValue;

                /**
                 * The property dispatcher for the type of the default value.
                 */
                private final PropertyDispatcher propertyDispatcher;

                /**
                 * Creates a new representation of an existant default value.
                 *
                 * @param defaultValue The represented, non-{@code null} default value.
                 */
                private DefaultValue(Object defaultValue) {
                    this.defaultValue = defaultValue;
                    propertyDispatcher = PropertyDispatcher.of(defaultValue.getClass());
                }

                /**
                 * Creates a default value representation for a given method which might or might not provide such
                 * a default value.
                 *
                 * @param method The method for which a default value is to be extracted.
                 * @return An annotation value representation for the given method.
                 */
                @SuppressWarnings("unchecked")
                protected static AnnotationValue.Loaded<?> of(Method method) {
                    Object defaultValue = method.getDefaultValue();
                    return defaultValue == null
                            ? new Missing((Class<? extends Annotation>) method.getDeclaringClass(), method.getName())
                            : new DefaultValue(defaultValue);
                }

                @Override
                public State getState() {
                    return State.RESOLVED;
                }

                @Override
                public Object resolve() {
                    return propertyDispatcher.conditionalClone(defaultValue);
                }

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (!(other instanceof AnnotationValue.Loaded<?>)) return false;
                    AnnotationValue.Loaded<?> loaded = (AnnotationValue.Loaded<?>) other;
                    return loaded.getState().isResolved() && propertyDispatcher.equals(defaultValue, loaded.resolve());
                }

                @Override
                public int hashCode() {
                    return propertyDispatcher.hashCode(defaultValue);
                }

                @Override
                public String toString() {
                    return propertyDispatcher.toString(defaultValue);
                }
            }

            /**
             * Represents a missing annotation property which is not represented by a default value.
             */
            private static class Missing implements AnnotationValue.Loaded<Void> {

                /**
                 * The annotation type.
                 */
                private final Class<? extends Annotation> annotationType;

                /**
                 * The name of the property without an annotation value.
                 */
                private final String property;

                /**
                 * Creates a new representation for a missing annotation property.
                 *
                 * @param annotationType The annotation type.
                 * @param property       The name of the property without an annotation value.
                 */
                private Missing(Class<? extends Annotation> annotationType, String property) {
                    this.annotationType = annotationType;
                    this.property = property;
                }

                @Override
                public State getState() {
                    return State.NON_DEFINED;
                }

                @Override
                public Void resolve() {
                    throw new IncompleteAnnotationException(annotationType, property);
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
            public Map<String, AnnotationValue<?, ?>> getValues() {
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

            /**
             * The generic signature of the field or {@code null} if it is not generic.
             */
            private final String genericSignature;

            /**
             * A list of annotation tokens representing the annotations of the represented field.
             */
            private final List<AnnotationToken> annotationTokens;

            /**
             * Creates a new field token.
             *
             * @param modifiers        The modifiers of the represented field.
             * @param name             The name of the field.
             * @param descriptor       The descriptor of the field.
             * @param genericSignature The generic signature of the field or {@code null} if it is not generic.
             * @param annotationTokens A list of annotation tokens representing the annotations of the
             *                         represented field.
             */
            protected FieldToken(int modifiers,
                                 String name,
                                 String descriptor,
                                 String genericSignature,
                                 List<AnnotationToken> annotationTokens) {
                this.modifiers = modifiers;
                this.name = name;
                this.descriptor = descriptor;
                this.genericSignature = genericSignature;
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

            /**
             * Returns the generic signature of the field or {@code null} if it is not generic.
             *
             * @return The generic signature of the field or {@code null} if it is not generic.
             */
            protected String getGenericSignature() {
                return genericSignature;
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
                        getGenericSignature(),
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
                        && !(genericSignature != null ? !genericSignature.equals(that.genericSignature) : that.genericSignature != null)
                        && name.equals(that.name);
            }

            @Override
            public int hashCode() {
                int result = modifiers;
                result = 31 * result + name.hashCode();
                result = 31 * result + descriptor.hashCode();
                result = 31 * result + (genericSignature != null ? genericSignature.hashCode() : 0);
                result = 31 * result + annotationTokens.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "TypePool.LazyTypeDescription.FieldToken{" +
                        "modifiers=" + modifiers +
                        ", name='" + name + '\'' +
                        ", descriptor='" + descriptor + '\'' +
                        ", genericSignature='" + genericSignature + '\'' +
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

            /**
             * The generic signature of the method or {@code null} if it is not generic.
             */
            private final String genericSignature;

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
             * The default value of this method or {@code null} if there is no such value.
             */
            private final AnnotationValue<?, ?> defaultValue;

            /**
             * Creates a new method token.
             *
             * @param modifiers                 The modifiers of the represented method.
             * @param name                      The internal name of the represented method.
             * @param descriptor                The descriptor of the represented method.
             * @param genericSignature          The generic signature of the method or {@code null} if it is not generic.
             * @param exceptionName             An array of internal names of the exceptions of the represented method
             *                                  or {@code null} if there are no such exceptions.
             * @param annotationTokens          A list of annotation tokens that are present on the represented method.
             * @param parameterAnnotationTokens A map of parameter indices to tokens that represent their annotations.
             * @param defaultValue              The default value of this method or {@code null} if there is no
             *                                  such value.
             */
            protected MethodToken(int modifiers,
                                  String name,
                                  String descriptor,
                                  String genericSignature,
                                  String[] exceptionName,
                                  List<AnnotationToken> annotationTokens,
                                  Map<Integer, List<AnnotationToken>> parameterAnnotationTokens,
                                  AnnotationValue<?, ?> defaultValue) {
                this.modifiers = modifiers;
                this.name = name;
                this.descriptor = descriptor;
                this.genericSignature = genericSignature;
                this.exceptionName = exceptionName;
                this.annotationTokens = annotationTokens;
                this.parameterAnnotationTokens = parameterAnnotationTokens;
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

            /**
             * Returns the generic signature of the method or {@code null} if it is not generic.
             *
             * @return The generic signature of the method or {@code null} if it is not generic.
             */
            protected String getGenericSignature() {
                return genericSignature;
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
             * Returns the default value of the represented method or {@code null} if no such values exists.
             *
             * @return The default value of the represented method or {@code null} if no such values exists.
             */
            protected AnnotationValue<?, ?> getDefaultValue() {
                return defaultValue;
            }

            /**
             * Trnasforms this method token to a method description that is attached to a lazy type description.
             *
             * @param lazyTypeDescription The lazy type description to attach this method description to.
             * @return A method description representing this field token.
             */
            private MethodDescription toMethodDescription(LazyTypeDescription lazyTypeDescription) {
                return lazyTypeDescription.new LazyMethodDescription(getModifiers(),
                        getName(),
                        getDescriptor(),
                        getGenericSignature(),
                        getExceptionName(),
                        getAnnotationTokens(),
                        getParameterAnnotationTokens(),
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
                        && !(genericSignature != null ? !genericSignature.equals(that.genericSignature) : that.genericSignature != null)
                        && Arrays.equals(exceptionName, that.exceptionName)
                        && name.equals(that.name)
                        && parameterAnnotationTokens.equals(that.parameterAnnotationTokens);
            }

            @Override
            public int hashCode() {
                int result = modifiers;
                result = 31 * result + name.hashCode();
                result = 31 * result + descriptor.hashCode();
                result = 31 * result + (genericSignature != null ? genericSignature.hashCode() : 0);
                result = 31 * result + Arrays.hashCode(exceptionName);
                result = 31 * result + annotationTokens.hashCode();
                result = 31 * result + parameterAnnotationTokens.hashCode();
                result = 31 * result + defaultValue.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "TypePool.LazyTypeDescription.MethodToken{" +
                        "modifiers=" + modifiers +
                        ", name='" + name + '\'' +
                        ", descriptor='" + descriptor + '\'' +
                        ", genericSignature='" + genericSignature + '\'' +
                        ", exceptionName=" + Arrays.toString(exceptionName) +
                        ", annotationTokens=" + annotationTokens +
                        ", parameterAnnotationTokens=" + parameterAnnotationTokens +
                        ", defaultValue=" + defaultValue +
                        '}';
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
                        : annotationValue.resolve(typePool);
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
            private static class Loadable<S extends Annotation> extends LazyAnnotationDescription
                    implements AnnotationDescription.Loadable<S> {

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
                    return (S) Proxy.newProxyInstance(classLoader, new Class<?>[]{annotationType},
                            new AnnotationInvocationHandler(annotationType.getClassLoader(), annotationType, values));
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

            /**
             * The binary name of the field's type.
             */
            private final String fieldTypeName;

            /**
             * The generic signature of the method or {@code null} if it is not generic.
             */
            private final String genericSignature;

            /**
             * A list of annotation descriptions of this field.
             */
            private final List<AnnotationDescription> declaredAnnotations;

            /**
             * Creaes a lazy field description.
             *
             * @param modifiers        The modifiers of the represented field.
             * @param name             The name of the field.
             * @param descriptor       The descriptor of the field.
             * @param genericSignature The generic signature of the field or {@code null} if it is not generic.
             * @param annotationTokens A list of annotation tokens representing annotations that are declared by this field.
             */
            private LazyFieldDescription(int modifiers,
                                         String name,
                                         String descriptor,
                                         String genericSignature,
                                         List<AnnotationToken> annotationTokens) {
                this.modifiers = modifiers;
                this.name = name;
                Type fieldType = Type.getType(descriptor);
                fieldTypeName = fieldType.getSort() == Type.ARRAY
                        ? fieldType.getInternalName().replace('/', '.')
                        : fieldType.getClassName();
                this.genericSignature = genericSignature;
                declaredAnnotations = new ArrayList<AnnotationDescription>(annotationTokens.size());
                for (AnnotationToken annotationToken : annotationTokens) {
                    declaredAnnotations.add(annotationToken.toAnnotationDescription(typePool));
                }
            }

            @Override
            public TypeDescription getFieldType() {
                return typePool.describe(fieldTypeName).resolve();
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

            @Override
            public String getGenericSignature() {
                return genericSignature;
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

            /**
             * The binary name of the return type of this method.
             */
            private final String returnTypeName;

            /**
             * The generic signature of the method or {@code null} if it is not generic.
             */
            private final String genericSignature;

            /**
             * A list of parameter type of this method.
             */
            private final TypeList parameterTypes;

            /**
             * A list of exception types of this method.
             */
            private final TypeList exceptionTypes;

            /**
             * A list of annotation descriptions that are declared by this method.
             */
            private final List<AnnotationDescription> declaredAnnotations;

            /**
             * A nested list of annotation descriptions that are declard by the parameters of this
             * method in their oder.
             */
            private final List<List<AnnotationDescription>> declaredParameterAnnotations;

            /**
             * The default value of this method or {@code null} if no such value exists.
             */
            private final AnnotationValue<?, ?> defaultValue;

            /**
             * Creates a new lazy method description.
             *
             * @param modifiers                 The modifiers of the represented method.
             * @param internalName              The internal name of this method.
             * @param methodDescriptor          The method descriptor of this method.
             * @param genericSignature          The generic signature of the method or {@code null} if it is not generic.
             * @param exceptionInternalName     The internal names of the exceptions that are declared by this
             *                                  method or {@code null} if no exceptions are declared by this
             *                                  method.
             * @param annotationTokens          A list of annotation tokens representing annotations that are declared
             *                                  by this method.
             * @param parameterAnnotationTokens A nested list of annotation tokens representing annotations that are
             *                                  declared by the fields of this method.
             * @param defaultValue              The default value of this method or {@code null} if there is no
             *                                  such value.
             */
            private LazyMethodDescription(int modifiers,
                                          String internalName,
                                          String methodDescriptor,
                                          String genericSignature,
                                          String[] exceptionInternalName,
                                          List<AnnotationToken> annotationTokens,
                                          Map<Integer, List<AnnotationToken>> parameterAnnotationTokens,
                                          AnnotationValue<?, ?> defaultValue) {
                this.modifiers = modifiers;
                this.internalName = internalName;
                Type returnType = Type.getReturnType(methodDescriptor);
                returnTypeName = returnType.getSort() == Type.ARRAY
                        ? returnType.getDescriptor().replace('/', '.')
                        : returnType.getClassName();
                parameterTypes = new LazyTypeList(methodDescriptor);
                this.genericSignature = genericSignature;
                exceptionTypes = exceptionInternalName == null
                        ? new TypeList.Empty()
                        : new LazyTypeList(exceptionInternalName);
                declaredAnnotations = new ArrayList<AnnotationDescription>(annotationTokens.size());
                for (AnnotationToken annotationToken : annotationTokens) {
                    declaredAnnotations.add(annotationToken.toAnnotationDescription(typePool));
                }
                declaredParameterAnnotations = new ArrayList<List<AnnotationDescription>>(parameterTypes.size());
                for (int index = 0; index < parameterTypes.size(); index++) {
                    List<AnnotationToken> tokens = parameterAnnotationTokens.get(index);
                    List<AnnotationDescription> annotationDescriptions;
                    annotationDescriptions = new ArrayList<AnnotationDescription>(tokens.size());
                    for (AnnotationToken annotationToken : tokens) {
                        annotationDescriptions.add(annotationToken.toAnnotationDescription(typePool));
                    }
                    declaredParameterAnnotations.add(annotationDescriptions);
                }
                this.defaultValue = defaultValue;
            }

            @Override
            public TypeDescription getReturnType() {
                return typePool.describe(returnTypeName).resolve();
            }

            @Override
            public TypeList getParameterTypes() {
                return parameterTypes;
            }

            @Override
            public TypeList getExceptionTypes() {
                return exceptionTypes;
            }

            @Override
            public boolean isConstructor() {
                return internalName.equals(MethodDescription.CONSTRUCTOR_INTERNAL_NAME);
            }

            @Override
            public boolean isTypeInitializer() {
                return false;
            }

            @Override
            public List<AnnotationList> getParameterAnnotations() {
                return AnnotationList.Explicit.asList(declaredParameterAnnotations);
            }

            @Override
            public AnnotationList getDeclaredAnnotations() {
                return new AnnotationList.Explicit(declaredAnnotations);
            }

            @Override
            public boolean represents(Method method) {
                return equals(new ForLoadedMethod(method));
            }

            @Override
            public boolean represents(Constructor<?> constructor) {
                return equals(new ForLoadedConstructor(constructor));
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
            public Object getDefaultValue() {
                return defaultValue == null
                        ? null
                        : defaultValue.resolve(typePool);
            }

            @Override
            public String getGenericSignature() {
                return genericSignature;
            }
        }

        /**
         * An implementation of a {@link net.bytebuddy.instrumentation.type.PackageDescription} that only
         * loads its annotations on requirement.
         */
        private class LazyPackageDescription extends PackageDescription.AbstractPackageDescription {

            /**
             * The name of the {@code package-info} class that represents a compiled Java package.
             */
            private static final String PACKAGE_INFO = ".package-info";

            /**
             * The name of the package.
             */
            private final String name;

            /**
             * Creates a new lazy package description.
             *
             * @param name The name of the package.
             */
            private LazyPackageDescription(String name) {
                this.name = name;
            }

            @Override
            public AnnotationList getDeclaredAnnotations() {
                Resolution resolution = typePool.describe(name + PACKAGE_INFO);
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
         * A list that is constructing {@link net.bytebuddy.pool.TypePool.LazyTypeDescription}s.
         */
        protected class LazyTypeList extends FilterableList.AbstractBase<TypeDescription, TypeList> implements TypeList {

            /**
             * A list of binary names of the represented types.
             */
            private final String[] name;

            /**
             * A list of internal names of the represented types.
             */
            private final String[] internalName;

            /**
             * The stack size of all types in this list.
             */
            private final int stackSize;

            /**
             * Creates a new type list for a method's parameter types.
             *
             * @param methodDescriptor The method which arguments should be represented in this type list.
             */
            protected LazyTypeList(String methodDescriptor) {
                Type[] parameterType = Type.getArgumentTypes(methodDescriptor);
                name = new String[parameterType.length];
                internalName = new String[parameterType.length];
                int index = 0, stackSize = 0;
                for (Type aParameterType : parameterType) {
                    name[index] = aParameterType.getSort() == Type.ARRAY
                            ? aParameterType.getInternalName().replace('/', '.')
                            : aParameterType.getClassName();
                    internalName[index] = aParameterType.getSort() == Type.ARRAY
                            ? aParameterType.getInternalName().replace('/', '.')
                            : aParameterType.getClassName();
                    stackSize += aParameterType.getSize();
                    index++;
                }
                this.stackSize = stackSize;
            }

            /**
             * Creates a new type list for a list of internal names.
             *
             * @param internalName The internal names to represent by this type list.
             */
            protected LazyTypeList(String[] internalName) {
                name = new String[internalName.length];
                this.internalName = internalName;
                int index = 0;
                for (String anInternalName : internalName) {
                    name[index++] = anInternalName.replace('/', '.');
                }
                stackSize = index;
            }

            @Override
            public TypeDescription get(int index) {
                return typePool.describe(name[index]).resolve();
            }

            @Override
            public int size() {
                return name.length;
            }

            @Override
            public String[] toInternalNames() {
                return internalName.length == 0 ? null : internalName;
            }

            @Override
            public int getStackSize() {
                return stackSize;
            }

            @Override
            protected TypeList wrap(List<TypeDescription> values) {
                return new Explicit(values);
            }
        }
    }
}
