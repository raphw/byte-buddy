package net.bytebuddy.instrumentation.attribute.annotation;

import net.bytebuddy.instrumentation.method.MethodList;

import java.lang.annotation.Annotation;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public interface AnnotationList extends List<AnnotationDescription> {

    boolean isAnnotationPresent(AnnotationDescription annotationDescription);

    <T extends Annotation> boolean isAnnotationPresent(Class<T> annotationType);

    <T extends Annotation> AnnotationDescription.Loadable<T> ofType(Class<T> annotationType);

    static class ForLoadedAnnotation extends AbstractList<AnnotationDescription> implements AnnotationList {

        public static List<AnnotationList> asList(Annotation[][] annotations) {
            List<AnnotationList> result = new ArrayList<AnnotationList>(annotations.length);
            for (Annotation[] annotation : annotations) {
                result.add(new ForLoadedAnnotation(annotation));
            }
            return result;
        }

        private final Annotation[] annotation;

        public ForLoadedAnnotation(Annotation[] annotation) {
            this.annotation = annotation;
        }

        @Override
        public AnnotationDescription get(int index) {
            return null;
        }

        @Override
        public int size() {
            return annotation.length;
        }

        @Override
        public boolean isAnnotationPresent(AnnotationDescription annotationDescription) {
            for (Annotation anAnnotation : annotation) {
                if (annotationDescription.getAnnotationType().equals(new MethodList.ForLoadedType(anAnnotation.annotationType()))) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public <T extends Annotation> boolean isAnnotationPresent(Class<T> annotationType) {
            for (Annotation anAnnotation : annotation) {
                if (anAnnotation.annotationType().equals(annotationType)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public <T extends Annotation> AnnotationDescription.Loadable<T> ofType(Class<T> annotationType) {
            for (Annotation anAnnotation : annotation) {
                if (anAnnotation.annotationType().equals(annotationType)) {
                    return AnnotationDescription.ForLoadedAnnotation.of(annotationType.cast(anAnnotation));
                }
            }
            return null;
        }
    }

    static class Empty extends AbstractList<AnnotationDescription> implements AnnotationList {

        public static List<AnnotationList> asList(int length) {
            List<AnnotationList> result = new ArrayList<AnnotationList>(length);
            for (int i = 0; i < length; i++) {
                result.add(new Empty());
            }
            return result;
        }

        @Override
        public AnnotationDescription get(int index) {
            throw new NoSuchElementException();
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isAnnotationPresent(AnnotationDescription annotationDescription) {
            return false;
        }

        @Override
        public <T extends Annotation> boolean isAnnotationPresent(Class<T> annotationType) {
            return false;
        }

        @Override
        public <T extends Annotation> AnnotationDescription.Loadable<T> ofType(Class<T> annotationType) {
            return null;
        }
    }
}
