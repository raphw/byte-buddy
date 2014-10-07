package net.bytebuddy.instrumentation.attribute.annotation;

import net.bytebuddy.instrumentation.type.TypeDescription;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.util.*;

public interface AnnotationList extends List<AnnotationDescription> {

    <T extends Annotation> boolean isAnnotationPresent(Class<T> annotationType);

    <T extends Annotation> AnnotationDescription.Loadable<T> ofType(Class<T> annotationType);

    AnnotationList inherited(Set<? extends TypeDescription> ignoredTypes);

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

        @Override
        public AnnotationList inherited(Set<? extends TypeDescription> ignoredTypes) {
            List<Annotation> inherited = new LinkedList<Annotation>();
            for (Annotation annotation : this.annotation) {
                if (!ignoredTypes.contains(new TypeDescription.ForLoadedType(annotation.annotationType()))
                        && annotation.annotationType().isAnnotationPresent(Inherited.class)) {
                    inherited.add(annotation);
                }
            }
            return new ForLoadedAnnotation(inherited.toArray(new Annotation[inherited.size()]));
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
        public <T extends Annotation> boolean isAnnotationPresent(Class<T> annotationType) {
            return false;
        }

        @Override
        public <T extends Annotation> AnnotationDescription.Loadable<T> ofType(Class<T> annotationType) {
            return null;
        }

        @Override
        public AnnotationList inherited(Set<? extends TypeDescription> ignoredTypes) {
            return this;
        }
    }

    static class Explicit extends AbstractList<AnnotationDescription> implements AnnotationList {

        public static List<AnnotationList> asList(List<? extends List<? extends AnnotationDescription>> annotations) {
            List<AnnotationList> result = new ArrayList<AnnotationList>(annotations.size());
            for (List<? extends AnnotationDescription> annotation : annotations) {
                result.add(new Explicit(annotation));
            }
            return result;
        }

        private final List<? extends AnnotationDescription> annotationDescriptions;

        public Explicit(List<? extends AnnotationDescription> annotationDescriptions) {
            this.annotationDescriptions = annotationDescriptions;
        }

        @Override
        public AnnotationDescription get(int index) {
            return annotationDescriptions.get(index);
        }

        @Override
        public int size() {
            return annotationDescriptions.size();
        }

        @Override
        public <T extends Annotation> boolean isAnnotationPresent(Class<T> annotationType) {
            for (AnnotationDescription annotationDescription : annotationDescriptions) {
                if (annotationDescription.getAnnotationType().represents(annotationType)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public <T extends Annotation> AnnotationDescription.Loadable<T> ofType(Class<T> annotationType) {
            for (AnnotationDescription annotationDescription : annotationDescriptions) {
                if (annotationDescription.getAnnotationType().represents(annotationType)) {
                    return annotationDescription.prepare(annotationType);
                }
            }
            return null;
        }

        @Override
        public AnnotationList inherited(Set<? extends TypeDescription> ignoredTypes) {
            List<AnnotationDescription> inherited = new LinkedList<AnnotationDescription>();
            for (AnnotationDescription annotation : annotationDescriptions) {
                TypeDescription annotationType = annotation.getAnnotationType();
                if (!ignoredTypes.contains(annotationType) && annotationType.getDeclaredAnnotations().isAnnotationPresent(Inherited.class)) {
                    inherited.add(annotation);
                }
            }
            return new Explicit(new ArrayList<AnnotationDescription>(inherited));
        }
    }
}
