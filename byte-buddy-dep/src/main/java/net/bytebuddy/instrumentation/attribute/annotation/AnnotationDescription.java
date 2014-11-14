package net.bytebuddy.instrumentation.attribute.annotation;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import net.bytebuddy.utility.PropertyDispatcher;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * An annotation description describes {@link java.lang.annotation.Annotation} meta data of a class without this class
 * being required to be loaded. All values of an annotation are therefore represented in unloaded state:
 * <ul>
 * <li>{@link java.lang.Class} instances are represented as {@link net.bytebuddy.instrumentation.type.TypeDescription}s.</li>
 * <li>{@link java.lang.Enum} instances are represented as
 * {@link net.bytebuddy.instrumentation.attribute.annotation.AnnotationDescription.EnumerationValue}s.</li>
 * <li>{@link java.lang.annotation.Annotation}s are described as
 * {@link net.bytebuddy.instrumentation.attribute.annotation.AnnotationDescription}s.</li>
 * <li>All primitive types are represented as their wrapper types.</li>
 * </ul>
 * An annotation can however be loaded in order to access unwrapped values. This will cause a loading of the classes
 * of these values.
 */
public interface AnnotationDescription {

    /**
     * Returns the value of the given method for this annotation value. Note that all return values are wrapped as
     * described by {@link net.bytebuddy.instrumentation.attribute.annotation.AnnotationDescription}.
     *
     * @param methodDescription The method for the value to be requested.
     * @return The value for the given method.
     */
    Object getValue(MethodDescription methodDescription);

    /**
     * Returns the value of the given method for this annotation value and performs a casting to the given value. Note
     * that all return values are wrapped described by
     * {@link net.bytebuddy.instrumentation.attribute.annotation.AnnotationDescription}.
     *
     * @param methodDescription The method for the value to be requested.
     * @param type              The type to which the returned value should be casted.
     * @param <T>               The given type of the return value.
     * @return The value for the given method casted to {@code type}.
     */
    <T> T getValue(MethodDescription methodDescription, Class<T> type);

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
     * Represents a value of an {@link java.lang.Enum} which is a value of an
     * {@link net.bytebuddy.instrumentation.attribute.annotation.AnnotationDescription}.
     */
    static interface EnumerationValue {

        /**
         * Returns the name of this instance's enumeration value.
         *
         * @return The name of this enumeration constant.
         */
        String getValue();

        /**
         * Returns the type of this enumeration.
         *
         * @return The type of this enumeration.
         */
        TypeDescription getEnumerationType();

        /**
         * Prepares this enumeration value to be loaded.
         *
         * @param type A type constant representing the enumeration value.
         * @param <T>  The enumeration type.
         * @return The loaded enumeration constant corresponding to this value.
         */
        <T extends Enum<T>> T load(Class<T> type);

        /**
         * An adapter implementation of an enumeration value.
         */
        abstract static class AbstractEnumerationValue implements EnumerationValue {

            @Override
            public boolean equals(Object other) {
                return other == this || other instanceof EnumerationValue
                        && (((EnumerationValue) other)).getEnumerationType().equals(getEnumerationType())
                        && (((EnumerationValue) other)).getValue().equals(getValue());
            }

            @Override
            public int hashCode() {
                return getValue().hashCode() + 31 * getEnumerationType().hashCode();
            }

            @Override
            public String toString() {
                return getValue();
            }
        }

        /**
         * An enumeration value representing a loaded enumeration.
         */
        static class ForLoadedEnumeration extends AbstractEnumerationValue {

            /**
             * The enumeration value.
             */
            private final Enum<?> value;

            /**
             * Creates a new enumeration value representation for a loaded enumeration.
             *
             * @param value The value to represent.
             */
            public ForLoadedEnumeration(Enum<?> value) {
                this.value = value;
            }

            /**
             * Enlists a given array of loaded enumerations as enumeration values.
             *
             * @param enumerations The enumerations to represent.
             * @return A list of the given enumerations.
             */
            public static List<EnumerationValue> asList(Enum<?>[] enumerations) {
                List<EnumerationValue> result = new ArrayList<EnumerationValue>(enumerations.length);
                for (Enum<?> enumeration : enumerations) {
                    result.add(new ForLoadedEnumeration(enumeration));
                }
                return result;
            }

            @Override
            public String getValue() {
                return value.name();
            }

            @Override
            public TypeDescription getEnumerationType() {
                return new TypeDescription.ForLoadedType(value.getDeclaringClass());
            }

            @Override
            @SuppressWarnings("unchecked")
            public <T extends Enum<T>> T load(Class<T> type) {
                if (value.getDeclaringClass() != type) {
                    throw new IllegalArgumentException(type + " does not represent " + value);
                }
                return (T) value;
            }
        }
    }

    /**
     * An annotation description that is linked to a given loaded annotation type which allows its representation
     * as a fully loaded instance.
     *
     * @param <S> The annotation type.
     */
    static interface Loadable<S extends Annotation> extends AnnotationDescription {

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
         *
         * @param classLoader The class loader to be used for loading the annotation's linked types.
         * @return A loaded version of this annotation description.
         * @throws java.lang.ClassNotFoundException If any linked classes of the annotation cannot be loaded.
         */
        S load(ClassLoader classLoader) throws ClassNotFoundException;

        /**
         * Loads this annotation description. This causes all classes referenced by the annotation value to be loaded.
         * Without specifying a class loader, the annotation's class loader which was used to prepare this instance
         * is used. Any {@link java.lang.ClassNotFoundException} is wrapped in an {@link java.lang.IllegalStateException}.
         *
         * @return A loaded version of this annotation description.
         */
        S loadSilent();

        /**
         * Loads this annotation description. This causes all classes referenced by the annotation value to be loaded.
         * Any {@link java.lang.ClassNotFoundException} is wrapped in an {@link java.lang.IllegalStateException}.
         *
         * @param classLoader The class loader to be used for loading the annotation's linked types.
         * @return A loaded version of this annotation description.
         */
        S loadSilent(ClassLoader classLoader);
    }

    /**
     * An adapter implementaton of an annotation.
     */
    abstract static class AbstractAnnotationDescription implements AnnotationDescription {

        @Override
        public <T> T getValue(MethodDescription methodDescription, Class<T> type) {
            return type.cast(getValue(methodDescription));
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
            if (!annotationDescription.getAnnotationType().equals(getAnnotationType())) {
                return false;
            }
            for (MethodDescription methodDescription : getAnnotationType().getDeclaredMethods()) {
                Object value = getValue(methodDescription);
                if (!PropertyDispatcher.of(value.getClass()).equals(value, annotationDescription.getValue(methodDescription))) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hashCode = 0;
            for (MethodDescription methodDescription : getAnnotationType().getDeclaredMethods()) {
                Object value = getValue(methodDescription);
                hashCode += 31 * PropertyDispatcher.of(value.getClass()).hashCode(value);
            }
            return hashCode;
        }

        @Override
        public String toString() {
            StringBuilder toString = new StringBuilder();
            toString.append('@');
            toString.append(getAnnotationType().getName());
            toString.append('(');
            boolean firstMember = true;
            for (MethodDescription methodDescription : getAnnotationType().getDeclaredMethods()) {
                if (firstMember) {
                    firstMember = false;
                } else {
                    toString.append(", ");
                }
                toString.append(methodDescription.getName());
                toString.append('=');
                Object value = getValue(methodDescription);
                toString.append(PropertyDispatcher.of(value.getClass()).toString(value));
            }
            toString.append(')');
            return toString.toString();
        }

        /**
         * An abstract implementation of a loadable annotation description.
         *
         * @param <S> The annotation type this instance was prepared for.
         */
        public abstract static class ForPrepared<S extends Annotation> extends AbstractAnnotationDescription implements Loadable<S> {

            /**
             * The error message to be displayed on a {@link java.lang.ClassNotFoundException}.
             */
            public static final String ERROR_MESSAGE = "Could not load a type that is linked by the annotation value";

            @Override
            public S loadSilent() {
                try {
                    return load();
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException(ERROR_MESSAGE, e);
                }
            }

            @Override
            public S loadSilent(ClassLoader classLoader) {
                try {
                    return load(classLoader);
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException(ERROR_MESSAGE, e);
                }
            }
        }
    }

    /**
     * A description of an already loaded annotation.
     *
     * @param <S> The type of the annotation.
     */
    static class ForLoadedAnnotation<S extends Annotation> extends AbstractAnnotationDescription.ForPrepared<S> implements Loadable<S> {

        /**
         * The represented annotation value.
         */
        private final S annotation;

        /**
         * Creates a new annotation description for a loaded annotation.
         *
         * @param annotation The annotation to represent.
         */
        protected ForLoadedAnnotation(S annotation) {
            this.annotation = annotation;
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

        /**
         * A helper method for converting a loaded type into a representation that is also capable of representing
         * unloaded descriptions of annotation values as specified by
         * {@link net.bytebuddy.instrumentation.attribute.annotation.AnnotationDescription}.
         *
         * @param value           The loaded value.
         * @param typeDescription The annotation type of the value. This cannot be inferred as enumerations
         *                        can implement annotation interfaces and because annotations could be implemented as
         *                        an enumeration what creates an ambiguity.
         * @return The wrapped representation as specified by
         * {@link net.bytebuddy.instrumentation.attribute.annotation.AnnotationDescription}.
         */
        public static Object wrap(Object value, TypeDescription typeDescription) {
            // Because enums can implement annotation interfaces, the enum property needs to be checked first.
            if (typeDescription.represents(Class.class)) {
                value = new TypeDescription.ForLoadedType((Class<?>) value);
            } else if (typeDescription.represents(Class[].class)) {
                value = new TypeList.ForLoadedType((Class<?>[]) value)
                        .toArray(new TypeDescription[((Class<?>[]) value).length]);
            } else if (typeDescription.isAssignableTo(Enum.class)) {
                value = new EnumerationValue.ForLoadedEnumeration((Enum<?>) value);
            } else if (typeDescription.isAssignableTo(Enum[].class)) {
                value = EnumerationValue.ForLoadedEnumeration.asList((Enum<?>[]) value)
                        .toArray(new EnumerationValue[((Enum<?>[]) value).length]);
            } else if (typeDescription.isAssignableTo(Annotation.class)) {
                value = ForLoadedAnnotation.of((Annotation) value);
            } else if (typeDescription.isAssignableTo(Annotation[].class)) {
                value = new AnnotationList.ForLoadedAnnotation((Annotation[]) value)
                        .toArray(new AnnotationDescription[((Annotation[]) value).length]);
            }
            return value;
        }

        @Override
        public S load() {
            return annotation;
        }

        @Override
        public S load(ClassLoader classLoader) {
            return annotation;
        }

        @Override
        public Object getValue(MethodDescription methodDescription) {
            if (!methodDescription.getDeclaringType().represents(annotation.annotationType())) {
                throw new IllegalArgumentException(methodDescription + " does not represent " + annotation.annotationType());
            }
            try {
                boolean visible = methodDescription.isVisibleTo(new TypeDescription.ForLoadedType(getClass()));
                Method method = methodDescription instanceof MethodDescription.ForLoadedMethod
                        ? ((MethodDescription.ForLoadedMethod) methodDescription).getLoadedMethod()
                        : null;
                if (method == null || (!visible && !method.isAccessible())) {
                    method = annotation.annotationType().getDeclaredMethod(methodDescription.getName());
                    if (!visible) {
                        method.setAccessible(true);
                    }
                }
                return wrap(method.invoke(annotation), methodDescription.getReturnType());
            } catch (Exception e) {
                throw new IllegalStateException("Cannot access annotation property " + methodDescription, e);
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T extends Annotation> Loadable<T> prepare(Class<T> annotationType) {
            if (!annotation.annotationType().equals(annotationType)) {
                throw new IllegalArgumentException("Annotation is not of type " + annotationType);
            }
            return (Loadable<T>) this;
        }

        @Override
        public TypeDescription getAnnotationType() {
            return new TypeDescription.ForLoadedType(annotation.annotationType());
        }
    }
}
