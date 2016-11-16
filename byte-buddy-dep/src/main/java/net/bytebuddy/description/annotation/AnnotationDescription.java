package net.bytebuddy.description.annotation;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.description.enumeration.EnumerationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.utility.privilege.SetAccessibleAction;

import java.lang.annotation.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.util.*;

import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * An annotation description describes {@link java.lang.annotation.Annotation} meta data of a class without this class
 * being required to be loaded. All values of an annotation are therefore represented in unloaded state:
 * <ul>
 * <li>{@link java.lang.Class} instances are represented as {@link TypeDescription}s.</li>
 * <li>{@link java.lang.Enum} instances are represented as
 * {@link net.bytebuddy.description.enumeration.EnumerationDescription}s.</li>
 * <li>{@link java.lang.annotation.Annotation}s are described as
 * {@link AnnotationDescription}s.</li>
 * <li>All primitive types are represented as their wrapper types.</li>
 * </ul>
 * An annotation can however be loaded in order to access unwrapped values. This will cause a loading of the classes
 * of these values.
 */
public interface AnnotationDescription {

    /**
     * Indicates an inexistent annotation in a type-safe manner.
     */
    AnnotationDescription.Loadable<?> UNDEFINED = null;

    /**
     * Returns the value of this annotation.
     *
     * @param property The property being accessed.
     * @return The value for the supplied property.
     */
    AnnotationValue<?, ?> getValue(MethodDescription.InDefinedShape property);

    /**
     * Returns a description of the annotation type of this annotation.
     *
     * @return A description of the annotation type of this annotation.
     */
    TypeDescription getAnnotationType();

    /**
     * Links this annotation description to a given annotation type such that it can be loaded. This does not cause
     * the values of this annotation to be loaded.
     *
     * @param annotationType The loaded annotation type of this annotation description.
     * @param <T>            The type of the annotation.
     * @return A loadable version of this annotation description.
     */
    <T extends Annotation> Loadable<T> prepare(Class<T> annotationType);

    /**
     * Returns this annotation's retention policy.
     *
     * @return This annotation's retention policy.
     */
    RetentionPolicy getRetention();

    /**
     * Returns a set of all {@link ElementType}s that can declare this annotation.
     *
     * @return A set of all element types that can declare this annotation.
     */
    Set<ElementType> getElementTypes();

    /**
     * Checks if this annotation is inherited.
     *
     * @return {@code true} if this annotation is inherited.
     * @see Inherited
     */
    boolean isInherited();

    /**
     * Checks if this annotation is documented.
     *
     * @return {@code true} if this annotation is documented.
     * @see Documented
     */
    boolean isDocumented();

    /**
     * An annotation description that is linked to a given loaded annotation type which allows its representation
     * as a fully loaded instance.
     *
     * @param <S> The annotation type.
     */
    interface Loadable<S extends Annotation> extends AnnotationDescription {

        /**
         * Loads this annotation description. This causes all classes referenced by the annotation value to be loaded.
         * Without specifying a class loader, the annotation's class loader which was used to prepare this instance
         * is used.
         *
         * @return A loaded version of this annotation description.
         * @throws java.lang.ClassNotFoundException If any linked classes of the annotation cannot be loaded.
         */
        S load() throws ClassNotFoundException;

        /**
         * Loads this annotation description. This causes all classes referenced by the annotation value to be loaded.
         * Without specifying a class loader, the annotation's class loader which was used to prepare this instance
         * is used. Any {@link java.lang.ClassNotFoundException} is wrapped in an {@link java.lang.IllegalStateException}.
         *
         * @return A loaded version of this annotation description.
         */
        S loadSilent();
    }

    /**
     * An {@link java.lang.reflect.InvocationHandler} for implementing annotations.
     *
     * @param <T> The type of the handled annotation.
     */
    class AnnotationInvocationHandler<T extends Annotation> implements InvocationHandler {

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
         * @param annotationType The loaded annotation type.
         * @param values         A sorted list of values of this annotation.
         */
        protected AnnotationInvocationHandler(Class<T> annotationType, LinkedHashMap<Method, AnnotationValue.Loaded<?>> values) {
            this.annotationType = annotationType;
            this.values = values;
        }

        /**
         * Creates a proxy instance for the supplied annotation type and values.
         *
         * @param classLoader    The class loader that should be used for loading the annotation's values.
         * @param annotationType The annotation's type.
         * @param values         The values that the annotation contains.
         * @param <S>            The type of the handled annotation.
         * @return A proxy for the annotation type and values.
         * @throws ClassNotFoundException If the class of an instance that is contained by this annotation could not be found.
         */
        @SuppressWarnings("unchecked")
        public static <S extends Annotation> S of(ClassLoader classLoader,
                                                  Class<S> annotationType,
                                                  Map<String, ? extends AnnotationValue<?, ?>> values) throws ClassNotFoundException {
            LinkedHashMap<Method, AnnotationValue.Loaded<?>> loadedValues = new LinkedHashMap<Method, AnnotationValue.Loaded<?>>();
            for (Method method : annotationType.getDeclaredMethods()) {
                AnnotationValue<?, ?> annotationValue = values.get(method.getName());
                loadedValues.put(method, (annotationValue == null
                        ? defaultValueOf(method)
                        : annotationValue).load(classLoader));
            }
            return (S) Proxy.newProxyInstance(classLoader, new Class<?>[]{annotationType}, new AnnotationInvocationHandler<S>(annotationType, loadedValues));
        }

        /**
         * Creates a default value for the given method.
         *
         * @param method The method from which to attempt the extraction of a default value.
         * @return A default value representation.
         */
        private static AnnotationValue<?, ?> defaultValueOf(Method method) {
            Object defaultValue = method.getDefaultValue();
            return defaultValue == null
                    ? MissingValue.of(method)
                    : AnnotationDescription.ForLoadedAnnotation.asValue(defaultValue, method.getReturnType());
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
        public Object invoke(Object proxy, Method method, Object[] argument) {
            if (method.getDeclaringClass() != annotationType) {
                if (method.getName().equals(HASH_CODE)) {
                    return hashCodeRepresentation();
                } else if (method.getName().equals(EQUALS) && method.getParameterTypes().length == 1) {
                    return equalsRepresentation(proxy, argument[0]);
                } else if (method.getName().equals(TO_STRING)) {
                    return toStringRepresentation();
                } else /* if (method.getName().equals("annotationType")) */ {
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
                toString.append(entry.getKey().getName())
                        .append('=')
                        .append(entry.getValue().toString());
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
                    try {
                        if (!entry.getValue().represents(entry.getKey().invoke(other))) {
                            return false;
                        }
                    } catch (RuntimeException exception) {
                        return false; // Incomplete annotations are not equal to one another.
                    }
                }
                return true;
            } catch (InvocationTargetException ignored) {
                return false;
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException("Could not access annotation property", exception);
            }
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof AnnotationInvocationHandler)) return false;
            AnnotationInvocationHandler that = (AnnotationInvocationHandler) other;
            if (!annotationType.equals(that.annotationType)) {
                return false;
            }
            for (Map.Entry<Method, AnnotationValue.Loaded<?>> entry : values.entrySet()) {
                if (!entry.getValue().equals(that.values.get(entry.getKey()))) {
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
                result = 31 * result + entry.getValue().hashCode();
            }
            return result;
        }

        @Override
        public String toString() {
            return "TypePool.LazyTypeDescription.AnnotationInvocationHandler{" +
                    "annotationType=" + annotationType +
                    ", values=" + values +
                    '}';
        }

        /**
         * Represents a missing annotation property which is not represented by a default value.
         */
        protected static class MissingValue extends AnnotationValue.Loaded.AbstractBase<Void> implements AnnotationValue<Void, Void> {

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
            protected MissingValue(Class<? extends Annotation> annotationType, String property) {
                this.annotationType = annotationType;
                this.property = property;
            }

            /**
             * Creates a missing value for the supplied annotation property.
             * @param method A method representing an annotation property.
             * @return An annotation value for a missing property.
             */
            @SuppressWarnings("unchecked")
            protected static AnnotationValue<?, ?> of(Method method) {
                return new MissingValue((Class<? extends Annotation>) method.getDeclaringClass(), method.getName());
            }

            @Override
            public State getState() {
                return State.UNDEFINED;
            }

            @Override
            public boolean represents(Object value) {
                return false;
            }

            @Override
            public Loaded<Void> load(ClassLoader classLoader) {
                return this;
            }

            @Override
            public Loaded<Void> loadSilent(ClassLoader classLoader) {
                return this;
            }

            @Override
            public Void resolve() {
                throw new IncompleteAnnotationException(annotationType, property);
            }

            /* does intentionally not implement hashCode, equals and toString */
        }
    }

    /**
     * An adapter implementation of an annotation.
     */
    abstract class AbstractBase implements AnnotationDescription {

        /**
         * An array containing all element types that are a legal annotation target when such a target
         * is not specified explicitly.
         */
        private static final ElementType[] DEFAULT_TARGET = new ElementType[]{ElementType.ANNOTATION_TYPE,
                ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.LOCAL_VARIABLE, ElementType.METHOD,
                ElementType.PACKAGE, ElementType.PARAMETER, ElementType.TYPE};

        @Override
        public RetentionPolicy getRetention() {
            AnnotationDescription.Loadable<Retention> retention = getAnnotationType().getDeclaredAnnotations().ofType(Retention.class);
            return retention == null
                    ? RetentionPolicy.CLASS
                    : retention.loadSilent().value();
        }

        @Override
        public Set<ElementType> getElementTypes() {
            AnnotationDescription.Loadable<Target> target = getAnnotationType().getDeclaredAnnotations().ofType(Target.class);
            return new HashSet<ElementType>(Arrays.asList(target == null
                    ? DEFAULT_TARGET
                    : target.loadSilent().value()));
        }

        @Override
        public boolean isInherited() {
            return getAnnotationType().getDeclaredAnnotations().isAnnotationPresent(Inherited.class);
        }

        @Override
        public boolean isDocumented() {
            return getAnnotationType().getDeclaredAnnotations().isAnnotationPresent(Documented.class);
        }

        @Override
        public boolean equals(Object other) {
            if (other == this) {
                return true;
            }
            if (!(other instanceof AnnotationDescription)) {
                return false;
            }
            AnnotationDescription annotationDescription = ((AnnotationDescription) other);
            TypeDescription annotationType = getAnnotationType();
            if (!annotationDescription.getAnnotationType().equals(annotationType)) {
                return false;
            }
            for (MethodDescription.InDefinedShape methodDescription : annotationType.getDeclaredMethods()) {
                if (!getValue(methodDescription).equals(annotationDescription.getValue(methodDescription))) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hashCode = 0;
            for (MethodDescription.InDefinedShape methodDescription : getAnnotationType().getDeclaredMethods()) {
                hashCode += 31 * getValue(methodDescription).hashCode();
            }
            return hashCode;
        }

        @Override
        public String toString() {
            TypeDescription annotationType = getAnnotationType();
            StringBuilder toString = new StringBuilder().append('@').append(annotationType.getName()).append('(');
            boolean firstMember = true;
            for (MethodDescription.InDefinedShape methodDescription : annotationType.getDeclaredMethods()) {
                if (firstMember) {
                    firstMember = false;
                } else {
                    toString.append(", ");
                }
                toString.append(methodDescription.getName()).append('=').append(getValue(methodDescription));
            }
            return toString.append(')').toString();
        }

        /**
         * An abstract implementation of a loadable annotation description.
         *
         * @param <S> The annotation type this instance was prepared for.
         */
        public abstract static class ForPrepared<S extends Annotation> extends AbstractBase implements Loadable<S> {

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
     * A description of an already loaded annotation.
     *
     * @param <S> The type of the annotation.
     */
    class ForLoadedAnnotation<S extends Annotation> extends AbstractBase.ForPrepared<S> {

        /**
         * The represented annotation value.
         */
        private final S annotation;

        /**
         * The annotation's loaded type which might be loaded by a different class loader than the value's
         * annotation type but must be structually equal to it.
         */
        private final Class<S> annotationType;

        /**
         * Creates a new annotation description for a loaded annotation.
         *
         * @param annotation The annotation to represent.
         */
        @SuppressWarnings("unchecked")
        protected ForLoadedAnnotation(S annotation) {
            this(annotation, (Class<S>) annotation.annotationType());
        }

        /**
         * Creates a new annotation description for a loaded annotation.
         *
         * @param annotation     The annotation to represent.
         * @param annotationType The annotation's loaded type which might be loaded by a different class loader than the value's
         *                       annotation type but must be structually equal to it.
         */
        private ForLoadedAnnotation(S annotation, Class<S> annotationType) {
            this.annotation = annotation;
            this.annotationType = annotationType;
        }

        /**
         * Creates a description of the given annotation.
         *
         * @param annotation The annotation to be described.
         * @param <U>        The type of the annotation.
         * @return A description of the given annotation.
         */
        public static <U extends Annotation> Loadable<U> of(U annotation) {
            return new ForLoadedAnnotation<U>(annotation);
        }

        @Override
        public S load() throws ClassNotFoundException {
            return annotationType == annotation.annotationType()
                    ? annotation
                    : AnnotationInvocationHandler.of(annotationType.getClassLoader(), annotationType, asValue(annotation));
        }

        /**
         * Extracts the annotation values of an annotation into a property map.
         *
         * @param annotation The annotation to convert.
         * @return A mapping of property names to their annotation value.
         */
        private static Map<String, AnnotationValue<?, ?>> asValue(Annotation annotation) {
            Map<String, AnnotationValue<?, ?>> annotationValues = new HashMap<String, AnnotationValue<?, ?>>();
            for (Method property : annotation.annotationType().getDeclaredMethods()) {
                try {
                    annotationValues.put(property.getName(), asValue(property.invoke(annotation), property.getReturnType()));
                } catch (InvocationTargetException exception) {
                    throw new IllegalStateException("Cannot read " + property, exception.getCause());
                } catch (IllegalAccessException exception) {
                    throw new IllegalStateException("Cannot access " + property, exception);
                }
            }
            return annotationValues;
        }

        /**
         * Transforms an annotation property to an annotation value.
         *
         * @param type  The annotation's type.
         * @param value The annotations value.
         * @return An annotation value representation.
         */
        @SuppressWarnings("unchecked")
        public static AnnotationValue<?, ?> asValue(Object value, Class<?> type) {
            // Because enums can implement annotation interfaces, the enum property needs to be checked first.
            if (Enum.class.isAssignableFrom(type)) {
                return AnnotationValue.ForEnumerationDescription.<Enum>of(new EnumerationDescription.ForLoadedEnumeration((Enum) value));
            } else if (Enum[].class.isAssignableFrom(type)) {
                Enum<?>[] element = (Enum<?>[]) value;
                EnumerationDescription[] enumerationDescription = new EnumerationDescription[element.length];
                int index = 0;
                for (Enum<?> anElement : element) {
                    enumerationDescription[index++] = new EnumerationDescription.ForLoadedEnumeration(anElement);
                }
                return AnnotationValue.ForDescriptionArray.<Enum>of(new TypeDescription.ForLoadedType(type.getComponentType()), enumerationDescription);
            } else if (Annotation.class.isAssignableFrom(type)) {
                return AnnotationValue.ForAnnotationDescription.<Annotation>of(new TypeDescription.ForLoadedType(type), asValue((Annotation) value));
            } else if (Annotation[].class.isAssignableFrom(type)) {
                Annotation[] element = (Annotation[]) value;
                AnnotationDescription[] annotationDescription = new AnnotationDescription[element.length];
                int index = 0;
                for (Annotation anElement : element) {
                    annotationDescription[index++] = new AnnotationDescription.Latent(new TypeDescription.ForLoadedType(type.getComponentType()), asValue(anElement));
                }
                return AnnotationValue.ForDescriptionArray.of(new TypeDescription.ForLoadedType(type.getComponentType()), annotationDescription);
            } else if (Class.class.isAssignableFrom(type)) {
                return AnnotationValue.ForTypeDescription.<Class>of(new TypeDescription.ForLoadedType((Class<?>) value));
            } else if (Class[].class.isAssignableFrom(type)) {
                Class<?>[] element = (Class<?>[]) value;
                TypeDescription[] typeDescription = new TypeDescription[element.length];
                int index = 0;
                for (Class<?> anElement : element) {
                    typeDescription[index++] = new TypeDescription.ForLoadedType(anElement);
                }
                return AnnotationValue.ForDescriptionArray.of(typeDescription);
            } else {
                return AnnotationValue.ForConstant.of(value);
            }
        }

        @Override
        @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Exception should always be wrapped for clarity")
        public AnnotationValue<?, ?> getValue(MethodDescription.InDefinedShape property) {
            if (!property.getDeclaringType().represents(annotation.annotationType())) {
                throw new IllegalArgumentException(property + " does not represent " + annotation.annotationType());
            }
            try {
                boolean accessible = property.getDeclaringType().isPublic(); // method is required to be public
                Method method = property instanceof MethodDescription.ForLoadedMethod
                        ? ((MethodDescription.ForLoadedMethod) property).getLoadedMethod()
                        : null;
                if (method == null || method.getDeclaringClass() != annotation.annotationType() || (!accessible && !method.isAccessible())) {
                    method = annotation.annotationType().getDeclaredMethod(property.getName());
                    if (!accessible) {
                        AccessController.doPrivileged(new SetAccessibleAction<Method>(method));
                    }
                }
                return asValue(method.invoke(annotation), method.getReturnType());
            } catch (Exception exception) {
                throw new IllegalStateException("Cannot access annotation property " + property, exception.getCause());
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends Annotation> Loadable<T> prepare(Class<T> annotationType) {
            if (!annotation.annotationType().getName().equals(annotationType.getName())) {
                throw new IllegalArgumentException(annotationType + " does not represent " + annotation.annotationType());
            }
            return annotationType == annotation.annotationType()
                    ? (Loadable<T>) this
                    : new ForLoadedAnnotation<T>((T) annotation, annotationType);
        }

        @Override
        public TypeDescription getAnnotationType() {
            return new TypeDescription.ForLoadedType(annotation.annotationType());
        }
    }

    /**
     * A latent description of an annotation value that is defined explicitly.
     */
    class Latent extends AbstractBase {

        /**
         * The type of the annotation.
         */
        private final TypeDescription annotationType;

        /**
         * The values of the annotation mapped by their property name.
         */
        private final Map<String, ? extends AnnotationValue<?, ?>> annotationValues;

        /**
         * Creates a new latent annotation description.
         *
         * @param annotationType   The type of the annotation.
         * @param annotationValues The values of the annotation mapped by their property name.
         */
        protected Latent(TypeDescription annotationType, Map<String, ? extends AnnotationValue<?, ?>> annotationValues) {
            this.annotationType = annotationType;
            this.annotationValues = annotationValues;
        }

        @Override
        public AnnotationValue<?, ?> getValue(MethodDescription.InDefinedShape property) {
            AnnotationValue<?, ?> value = annotationValues.get(property.getName());
            if (value != null) {
                return value;
            }
            AnnotationValue<?, ?> defaultValue = property.getDefaultValue();
            if (defaultValue != null) {
                return defaultValue;
            }
            throw new IllegalArgumentException("No value defined for: " + property);
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
            return new Loadable<T>(annotationType);
        }

        /**
         * A loadable annotation description of a latent annotation description.
         *
         * @param <S> The annotation type.
         */
        protected class Loadable<S extends Annotation> extends AbstractBase.ForPrepared<S> {

            /**
             * The annotation type.
             */
            private final Class<S> annotationType;

            /**
             * Creates a loadable version of a latent annotation description.
             *
             * @param annotationType The annotation type.
             */
            protected Loadable(Class<S> annotationType) {
                this.annotationType = annotationType;
            }

            @Override
            public S load() throws ClassNotFoundException {
                return AnnotationDescription.AnnotationInvocationHandler.of(annotationType.getClassLoader(), annotationType, annotationValues);
            }

            @Override
            public AnnotationValue<?, ?> getValue(MethodDescription.InDefinedShape property) {
                return Latent.this.getValue(property);
            }

            @Override
            public TypeDescription getAnnotationType() {
                return new TypeDescription.ForLoadedType(annotationType);
            }

            @Override
            public <T extends Annotation> Loadable<T> prepare(Class<T> annotationType) {
                return Latent.this.prepare(annotationType);
            }
        }
    }

    /**
     * A builder for pragmatically creating {@link net.bytebuddy.description.annotation.AnnotationDescription}.
     */
    class Builder {

        /**
         * The annotation type.
         */
        private final TypeDescription annotationType;

        /**
         * A mapping of annotation properties to their annotation values.
         */
        private final Map<String, AnnotationValue<?, ?>> annotationValues;

        /**
         * Creates a builder for an annotation description.
         *
         * @param annotationType   The annotation type.
         * @param annotationValues A mapping of annotation properties to their annotation values.
         */
        protected Builder(TypeDescription annotationType, Map<String, AnnotationValue<?, ?>> annotationValues) {
            this.annotationType = annotationType;
            this.annotationValues = annotationValues;
        }

        /**
         * Creates a builder for creating an annotation of the given type.
         *
         * @param annotationType The annotation type.
         * @return A builder for creating an annotation of the given type.
         */
        public static Builder ofType(Class<? extends Annotation> annotationType) {
            return ofType(new TypeDescription.ForLoadedType(annotationType));
        }

        /**
         * Creates a builder for creating an annotation of the given type.
         *
         * @param annotationType A description of the annotation type.
         * @return A builder for creating an annotation of the given type.
         */
        public static Builder ofType(TypeDescription annotationType) {
            if (!annotationType.isAnnotation()) {
                throw new IllegalArgumentException("Not an annotation type: " + annotationType);
            }
            return new Builder(annotationType, Collections.<String, AnnotationValue<?, ?>>emptyMap());
        }

        /**
         * Returns a builder with the additional, given property.
         *
         * @param property The name of the property to define.
         * @param value    An explicit description of the annotation value.
         * @return A builder with the additional, given property.
         */
        public Builder define(String property, AnnotationValue<?, ?> value) {
            MethodList<?> methodDescriptions = annotationType.getDeclaredMethods().filter(named(property));
            if (methodDescriptions.isEmpty()) {
                throw new IllegalArgumentException(annotationType + " does not define a property named " + property);
            } else if (!methodDescriptions.getOnly().getReturnType().asErasure().isAnnotationValue(value.resolve())) {
                throw new IllegalArgumentException(value + " cannot be assigned to " + property);
            }
            Map<String, AnnotationValue<?, ?>> annotationValues = new HashMap<String, AnnotationValue<?, ?>>();
            annotationValues.putAll(this.annotationValues);
            if (annotationValues.put(methodDescriptions.getOnly().getName(), value) != null) {
                throw new IllegalArgumentException("Property already defined: " + property);
            }
            return new Builder(annotationType, annotationValues);
        }

        /**
         * Returns a builder with the additional enumeration property.
         *
         * @param property The name of the property to define.
         * @param value    The enumeration value to define.
         * @return A builder with the additional enumeration property.
         */
        public Builder define(String property, Enum<?> value) {
            return define(property, new EnumerationDescription.ForLoadedEnumeration(value));
        }

        /**
         * Returns a builder with the additional enumeration property.
         *
         * @param property        The name of the property to define.
         * @param enumerationType The type of the enumeration.
         * @param value           The enumeration value to define.
         * @return A builder with the additional enumeration property.
         */
        public Builder define(String property, TypeDescription enumerationType, String value) {
            return define(property, new EnumerationDescription.Latent(enumerationType, value));
        }

        /**
         * Returns a builder with the additional enumeration property.
         *
         * @param property The name of the property to define.
         * @param value    A description of the enumeration value to define.
         * @return A builder with the additional enumeration property.
         */
        @SuppressWarnings("unchecked")
        public Builder define(String property, EnumerationDescription value) {
            return define(property, AnnotationValue.ForEnumerationDescription.<Enum>of(value));
        }

        /**
         * Returns a builder with the additional annotation property.
         *
         * @param property   The name of the property to define.
         * @param annotation The annotation value to define.
         * @return A builder with the additional annotation property.
         */
        public Builder define(String property, Annotation annotation) {
            return define(property, new ForLoadedAnnotation<Annotation>(annotation));
        }

        /**
         * Returns a builder with the additional annotation property.
         *
         * @param property              The name of the property to define.
         * @param annotationDescription A description of the annotation value to define.
         * @return A builder with the additional annotation property.
         */
        public Builder define(String property, AnnotationDescription annotationDescription) {
            return define(property, new AnnotationValue.ForAnnotationDescription<Annotation>(annotationDescription));
        }

        /**
         * Returns a builder with the additional class property.
         *
         * @param property The name of the property to define.
         * @param type     The class value to define.
         * @return A builder with the additional class property.
         */
        public Builder define(String property, Class<?> type) {
            return define(property, new TypeDescription.ForLoadedType(type));
        }

        /**
         * Returns a builder with the additional class property.
         *
         * @param property        The name of the property to define.
         * @param typeDescription A description of the type to define as a property value.
         * @return A builder with the additional class property.
         */
        @SuppressWarnings("unchecked")
        public Builder define(String property, TypeDescription typeDescription) {
            return define(property, AnnotationValue.ForTypeDescription.<Class>of(typeDescription));
        }

        /**
         * Returns a builder with the additional enumeration array property.
         *
         * @param property        The name of the property to define.
         * @param enumerationType The type of the enumeration, i.e. the component type of the enumeration array.
         * @param value           The enumeration values to be contained by the array.
         * @param <T>             The enumeration type.
         * @return A builder with the additional class property.
         */
        @SuppressWarnings("unchecked")
        public <T extends Enum<?>> Builder defineEnumerationArray(String property, Class<T> enumerationType, T... value) {
            EnumerationDescription[] enumerationDescription = new EnumerationDescription[value.length];
            int index = 0;
            for (T aValue : value) {
                enumerationDescription[index++] = new EnumerationDescription.ForLoadedEnumeration(aValue);
            }
            return defineEnumerationArray(property, new TypeDescription.ForLoadedType(enumerationType), enumerationDescription);
        }

        /**
         * Returns a builder with the additional enumeration array property.
         *
         * @param property        The name of the property to define.
         * @param enumerationType The type of the enumerations, i.e. is the component type of the enumeration array.
         * @param value           The enumeration values to be contained by the array.
         * @return A builder with the additional enumeration property.
         */
        public Builder defineEnumerationArray(String property, TypeDescription enumerationType, String... value) {
            if (!enumerationType.isEnum()) {
                throw new IllegalArgumentException("Not an enumeration type: " + enumerationType);
            }
            EnumerationDescription[] enumerationDescription = new EnumerationDescription[value.length];
            for (int i = 0; i < value.length; i++) {
                enumerationDescription[i] = new EnumerationDescription.Latent(enumerationType, value[i]);
            }
            return defineEnumerationArray(property, enumerationType, enumerationDescription);
        }

        /**
         * Returns a builder with the additional enumeration array property.
         *
         * @param property        The name of the property to define.
         * @param enumerationType The type of the enumerations, i.e. the component type of the enumeration array.
         * @param value           Descriptions of the enumerations to be contained by the array.
         * @return A builder with the additional enumeration property.
         */
        @SuppressWarnings("unchecked")
        public Builder defineEnumerationArray(String property, TypeDescription enumerationType, EnumerationDescription... value) {
            return define(property, AnnotationValue.ForDescriptionArray.<Enum>of(enumerationType, value));
        }

        /**
         * Returns a builder with the additional annotation array property.
         *
         * @param property       The name of the property to define.
         * @param annotationType The type of the annotations, i.e. the component type of the enumeration array.
         * @param annotation     The annotation values to be contained by the array.
         * @param <T>            The annotation type.
         * @return A builder with the additional annotation property.
         */
        @SuppressWarnings("unchecked")
        public <T extends Annotation> Builder defineAnnotationArray(String property, Class<T> annotationType, T... annotation) {
            return defineAnnotationArray(property,
                    new TypeDescription.ForLoadedType(annotationType),
                    new AnnotationList.ForLoadedAnnotations(annotation).toArray(new AnnotationDescription[annotation.length]));
        }

        /**
         * Returns a builder with the additional annotation array property.
         *
         * @param property              The name of the property to define.
         * @param annotationType        The type of the annotations, i.e. the component type of the enumeration array.
         * @param annotationDescription Descriptions of the annotation values to be contained by the array.
         * @return A builder with the additional annotation property.
         */
        public Builder defineAnnotationArray(String property, TypeDescription annotationType, AnnotationDescription... annotationDescription) {
            return define(property, AnnotationValue.ForDescriptionArray.of(annotationType, annotationDescription));
        }

        /**
         * Returns a builder with the additional type array property.
         *
         * @param property The name of the property to define.
         * @param type     The types that should be contained by the array.
         * @return A builder with the additional type array property.
         */
        public Builder defineTypeArray(String property, Class<?>... type) {
            return defineTypeArray(property, new TypeList.ForLoadedTypes(type).toArray(new TypeDescription[type.length]));
        }

        /**
         * Returns a builder with the additional type array property.
         *
         * @param property        The name of the property to define.
         * @param typeDescription Descriptions of the types that should be contained by the array.
         * @return A builder with the additional type array property.
         */
        @SuppressWarnings("unchecked")
        public Builder defineTypeArray(String property, TypeDescription... typeDescription) {
            return define(property, AnnotationValue.ForDescriptionArray.of(typeDescription));
        }

        /**
         * Returns a builder with the additional {@code boolean} property.
         *
         * @param property The name of the property to define.
         * @param value    The {@code boolean} value to define for the property.
         * @return A builder with the additional {@code boolean} property.
         */
        public Builder define(String property, boolean value) {
            return define(property, AnnotationValue.ForConstant.of(value));
        }

        /**
         * Returns a builder with the additional {@code byte} property.
         *
         * @param property The name of the property to define.
         * @param value    The {@code byte} value to define for the property.
         * @return A builder with the additional {@code byte} property.
         */
        public Builder define(String property, byte value) {
            return define(property, AnnotationValue.ForConstant.of(value));
        }

        /**
         * Returns a builder with the additional {@code char} property.
         *
         * @param property The name of the property to define.
         * @param value    The {@code char} value to define for the property.
         * @return A builder with the additional {@code char} property.
         */
        public Builder define(String property, char value) {
            return define(property, AnnotationValue.ForConstant.of(value));
        }

        /**
         * Returns a builder with the additional {@code short} property.
         *
         * @param property The name of the property to define.
         * @param value    The {@code short} value to define for the property.
         * @return A builder with the additional {@code short} property.
         */
        public Builder define(String property, short value) {
            return define(property, AnnotationValue.ForConstant.of(value));
        }

        /**
         * Returns a builder with the additional {@code int} property.
         *
         * @param property The name of the property to define.
         * @param value    The {@code int} value to define for the property.
         * @return A builder with the additional {@code int} property.
         */
        public Builder define(String property, int value) {
            return define(property, AnnotationValue.ForConstant.of(value));
        }

        /**
         * Returns a builder with the additional {@code long} property.
         *
         * @param property The name of the property to define.
         * @param value    The {@code long} value to define for the property.
         * @return A builder with the additional {@code long} property.
         */
        public Builder define(String property, long value) {
            return define(property, AnnotationValue.ForConstant.of(value));
        }

        /**
         * Returns a builder with the additional {@code float} property.
         *
         * @param property The name of the property to define.
         * @param value    The {@code float} value to define for the property.
         * @return A builder with the additional {@code float} property.
         */
        public Builder define(String property, float value) {
            return define(property, AnnotationValue.ForConstant.of(value));
        }

        /**
         * Returns a builder with the additional {@code double} property.
         *
         * @param property The name of the property to define.
         * @param value    The {@code double} value to define for the property.
         * @return A builder with the additional {@code double} property.
         */
        public Builder define(String property, double value) {
            return define(property, AnnotationValue.ForConstant.of(value));
        }

        /**
         * Returns a builder with the additional {@link java.lang.String} property.
         *
         * @param property The name of the property to define.
         * @param value    The {@link java.lang.String} value to define for the property.
         * @return A builder with the additional {@link java.lang.String} property.
         */
        public Builder define(String property, String value) {
            return define(property, AnnotationValue.ForConstant.of(value));
        }

        /**
         * Returns a builder with the additional {@code boolean} array property.
         *
         * @param property The name of the property to define.
         * @param value    The {@code boolean} values to define for the property.
         * @return A builder with the additional {@code boolean} array property.
         */
        public Builder defineArray(String property, boolean... value) {
            return define(property, AnnotationValue.ForConstant.of(value));
        }

        /**
         * Returns a builder with the additional {@code byte} array property.
         *
         * @param property The name of the property to define.
         * @param value    The {@code byte} values to define for the property.
         * @return A builder with the additional {@code byte} array property.
         */
        public Builder defineArray(String property, byte... value) {
            return define(property, AnnotationValue.ForConstant.of(value));
        }

        /**
         * Returns a builder with the additional {@code char} array property.
         *
         * @param property The name of the property to define.
         * @param value    The {@code char} values to define for the property.
         * @return A builder with the additional {@code char} array property.
         */
        public Builder defineArray(String property, char... value) {
            return define(property, AnnotationValue.ForConstant.of(value));
        }

        /**
         * Returns a builder with the additional {@code short} array property.
         *
         * @param property The name of the property to define.
         * @param value    The {@code short} values to define for the property.
         * @return A builder with the additional {@code short} array property.
         */
        public Builder defineArray(String property, short... value) {
            return define(property, AnnotationValue.ForConstant.of(value));
        }

        /**
         * Returns a builder with the additional {@code int} array property.
         *
         * @param property The name of the property to define.
         * @param value    The {@code int} values to define for the property.
         * @return A builder with the additional {@code int} array property.
         */
        public Builder defineArray(String property, int... value) {
            return define(property, AnnotationValue.ForConstant.of(value));
        }

        /**
         * Returns a builder with the additional {@code long} array property.
         *
         * @param property The name of the property to define.
         * @param value    The {@code long} values to define for the property.
         * @return A builder with the additional {@code long} array property.
         */
        public Builder defineArray(String property, long... value) {
            return define(property, AnnotationValue.ForConstant.of(value));
        }

        /**
         * Returns a builder with the additional {@code float} array property.
         *
         * @param property The name of the property to define.
         * @param value    The {@code float} values to define for the property.
         * @return A builder with the additional {@code float} array property.
         */
        public Builder defineArray(String property, float... value) {
            return define(property, AnnotationValue.ForConstant.of(value));
        }

        /**
         * Returns a builder with the additional {@code double} array property.
         *
         * @param property The name of the property to define.
         * @param value    The {@code double} values to define for the property.
         * @return A builder with the additional {@code double} array property.
         */
        public Builder defineArray(String property, double... value) {
            return define(property, AnnotationValue.ForConstant.of(value));
        }

        /**
         * Returns a builder with the additional {@link java.lang.String} array property.
         *
         * @param property The name of the property to define.
         * @param value    The {@link java.lang.String} array value to define for the property.
         * @return A builder with the additional {@link java.lang.String} array property.
         */
        public Builder defineArray(String property, String... value) {
            return define(property, AnnotationValue.ForConstant.of(value));
        }

        /**
         * Creates an annotation description for the values that were defined for this builder.
         *
         * @return An appropriate annotation description.
         */
        public AnnotationDescription build() {
            for (MethodDescription methodDescription : annotationType.getDeclaredMethods()) {
                if (annotationValues.get(methodDescription.getName()) == null && methodDescription.getDefaultValue() == null) {
                    throw new IllegalStateException("No value or default value defined for " + methodDescription.getName());
                }
            }
            return new Latent(annotationType, annotationValues);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && annotationType.equals(((Builder) other).annotationType)
                    && annotationValues.equals(((Builder) other).annotationValues);
        }

        @Override
        public int hashCode() {
            int result = annotationType.hashCode();
            result = 31 * result + annotationValues.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "AnnotationDescription.Builder{" +
                    "annotationType=" + annotationType +
                    ", annotationValues=" + annotationValues +
                    '}';
        }
    }
}
