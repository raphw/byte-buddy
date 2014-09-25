package net.bytebuddy.instrumentation.attribute.annotation;

import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;

public interface AnnotationDescription {

    <T> T getValue(String attributeName, Class<T> type);

    TypeDescription getAnnotationType();

    <T extends Annotation> Loadable<T> prepare(Class<T> annotationType);

    static interface Loadable<S extends Annotation> extends AnnotationDescription {

        S load();
    }

    static class ForLoadedAnnotation<S extends Annotation> implements Loadable<S> {

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
        public <T> T getValue(String attributeName, Class<T> type) {
            try {
                Object value = annotation.annotationType().getDeclaredMethod(attributeName).invoke(annotation);
                if (value instanceof Class) {
                    value = new TypeDescription.ForLoadedType((Class<?>) value);
                } else if (value instanceof Class[]) {
                    value = new TypeList.ForLoadedType((Class<?>[]) value).toArray(new TypeDescription[((Class<?>[]) value).length]);
                }
                return type.cast(value);
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException("Unknown attribute " + attributeName + " for " + annotation, e);
            } catch (InvocationTargetException e) {
                throw new IllegalStateException("Cannot receive attribute " + attributeName + " of " + annotation, e);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Cannot access " + attributeName + " of " + annotation, e);
            } catch (ClassCastException e) {
                throw new IllegalStateException(attributeName + " of " + annotation + " is not of type " + type, e);
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
