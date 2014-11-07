package net.bytebuddy.instrumentation.attribute.annotation;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public interface AnnotationDescription {

    <T> T getValue(MethodDescription methodDescription, Class<T> type);

    Object getValue(MethodDescription methodDescription);

    TypeDescription getAnnotationType();

    <T extends Annotation> Loadable<T> prepare(Class<T> annotationType);

    static interface EnumerationValue {

        String getValue();

        TypeDescription getEnumerationType();

        <T extends Enum<T>> Loadable<T> prepare(Class<T> type);

        static interface Loadable<S extends Enum<S>> extends EnumerationValue {

            S load();
        }

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
        }

        static class ForLoadedEnumeration<S extends Enum<S>> extends AbstractEnumerationValue implements Loadable<S> {

            public static <T extends Enum<T>> Loadable<T> of(T value) {
                return new ForLoadedEnumeration<T>(value);
            }

            @SuppressWarnings("unchecked")
            protected static EnumerationValue ofWildcard(Enum<?> value) {
                return new ForLoadedEnumeration(value);
            }

            private final S value;

            protected ForLoadedEnumeration(S value) {
                this.value = value;
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
            public <T extends Enum<T>> Loadable<T> prepare(Class<T> type) {
                if (type != value.getDeclaringClass()) {
                    throw new IllegalArgumentException();
                }
                return (Loadable<T>) this;
            }

            @Override
            public S load() {
                return value;
            }

            public static List<EnumerationValue> asList(Enum<?>[] enumerations) {
                List<EnumerationValue> result = new ArrayList<EnumerationValue>(enumerations.length);
                for (Enum<?> enumeration : enumerations) {
                    result.add(ofWildcard(enumeration));
                }
                return result;
            }
        }
    }

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
                if (!annotationDescription.getValue(methodDescription).equals(getValue(methodDescription))) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hashCode = 0;
            for (MethodDescription methodDescription : getAnnotationType().getDeclaredMethods()) {
                hashCode += 31 * getValue(methodDescription).hashCode();
            }
            return hashCode;
        }
    }

    static interface Loadable<S extends Annotation> extends AnnotationDescription {

        S load();
    }

    static class ForLoadedAnnotation<S extends Annotation> extends AbstractAnnotationDescription implements Loadable<S> {

        private final S annotation;

        public static <U extends Annotation> Loadable<U> of(U annotation) {
            return new ForLoadedAnnotation<U>(annotation);
        }

        protected ForLoadedAnnotation(S annotation) {
            this.annotation = annotation;
        }

        @Override
        public S load() {
            return annotation;
        }

        @Override
        public Object getValue(MethodDescription methodDescription) {
            try {
                return new TypeWrapper((methodDescription instanceof MethodDescription.ForLoadedMethod
                        ? ((MethodDescription.ForLoadedMethod) methodDescription).getLoadedMethod()
                        : annotation.annotationType().getDeclaredMethod(methodDescription.getName())).invoke(annotation)).apply();
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Cannot access annotation property " + methodDescription, e);
            } catch (InvocationTargetException e) {
                throw new IllegalArgumentException("Error on accessing annotation property " + methodDescription, e);
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException("Cannot invoke property on annotation " + methodDescription, e);
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

        public static class TypeWrapper {

            private final Object value;

            public TypeWrapper(Object value) {
                this.value = value;
            }

            public Object apply() {
                Object value = this.value;
                if (value instanceof Class<?>) {
                    value = new TypeDescription.ForLoadedType((Class<?>) value);
                } else if (value instanceof Class<?>[]) {
                    value = new TypeList.ForLoadedType((Class<?>[]) value).toArray(new TypeDescription[((Class<?>[]) value).length]);
                } else if (value instanceof Enum<?>) {
                    value = EnumerationValue.ForLoadedEnumeration.ofWildcard((Enum<?>) value);
                } else if (value instanceof Enum<?>[]) {
                    value = EnumerationValue.ForLoadedEnumeration.asList((Enum<?>[]) value);
                } else if (value instanceof Annotation) {
                    value = ForLoadedAnnotation.of((Annotation) value);
                } else if (value instanceof Annotation[]) {
                    value = new AnnotationList.ForLoadedAnnotation((Annotation[]) value).toArray(new AnnotationDescription[((Annotation[]) value).length]);
                }
                return value;
            }
        }
    }
}
