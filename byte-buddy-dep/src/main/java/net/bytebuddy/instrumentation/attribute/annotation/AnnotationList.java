package net.bytebuddy.instrumentation.attribute.annotation;

import net.bytebuddy.instrumentation.type.TypeDescription;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.util.*;

/**
 * Defines a list of annotation instances.
 */
public interface AnnotationList extends List<AnnotationDescription> {

    /**
     * Checks if this list contains an annotation of the given type.
     *
     * @param annotationType The type to find in the list.
     * @return {@code true} if the list contains the annotation type.
     */
    boolean isAnnotationPresent(Class<? extends Annotation> annotationType);

    /**
     * Finds the first annotation of the given type and returns it.
     *
     * @param annotationType The type to be found in the list.
     * @param <T>            The annotation type.
     * @return The annotation value or {@code null} if no such annotation was found.
     */
    <T extends Annotation> AnnotationDescription.Loadable<T> ofType(Class<T> annotationType);

    /**
     * Returns only annotations that are marked as {@link java.lang.annotation.Inherited} as long as they are not
     * contained by the set of ignored annotation types.
     *
     * @param ignoredTypes A list of annotation types to be ignored from the lookup.
     * @return A list of all inherited annotations besides of the given ignored types.
     */
    AnnotationList inherited(Set<? extends TypeDescription> ignoredTypes);

    @Override
    AnnotationList subList(int fromIndex, int toIndex);

    /**
     * Describes an array of loaded {@link java.lang.annotation.Annotation}s as an annotatoon list.
     */
    static class ForLoadedAnnotation extends AbstractList<AnnotationDescription> implements AnnotationList {

        /**
         * Creates a list of annotation lists representing the given loaded annotations.
         *
         * @param annotations The annotations to represent where each dimension is converted into a list.
         * @return A list of annotation lists representing the given annotations.
         */
        public static List<AnnotationList> asList(Annotation[][] annotations) {
            List<AnnotationList> result = new ArrayList<AnnotationList>(annotations.length);
            for (Annotation[] annotation : annotations) {
                result.add(new ForLoadedAnnotation(annotation));
            }
            return result;
        }

        /**
         * The represented annotations.
         */
        private final Annotation[] annotation;

        /**
         * Creates a new list of loaded annotations.
         *
         * @param annotation The represented annotations.
         */
        public ForLoadedAnnotation(Annotation... annotation) {
            this.annotation = annotation;
        }

        @Override
        public AnnotationDescription get(int index) {
            return AnnotationDescription.ForLoadedAnnotation.of(annotation[index]);
        }

        @Override
        public int size() {
            return annotation.length;
        }

        @Override
        public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
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

        @Override
        public AnnotationList subList(int fromIndex, int toIndex) {
            return new Explicit(super.subList(fromIndex, toIndex));
        }
    }

    /**
     * Represents an empty annotation list.
     */
    static class Empty extends AbstractList<AnnotationDescription> implements AnnotationList {

        /**
         * Creates a list of empty annotation lists of the given dimension.
         *
         * @param length The length of the list.
         * @return A list of empty annotation lists of the given length.
         */
        public static List<AnnotationList> asList(int length) {
            List<AnnotationList> result = new ArrayList<AnnotationList>(length);
            for (int i = 0; i < length; i++) {
                result.add(new Empty());
            }
            return result;
        }

        @Override
        public AnnotationDescription get(int index) {
            throw new IndexOutOfBoundsException();
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
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

        @Override
        public AnnotationList subList(int fromIndex, int toIndex) {
            if (fromIndex == toIndex && toIndex == 0) {
                return this;
            } else if (fromIndex > toIndex) {
                throw new IllegalArgumentException("fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")");
            } else {
                throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
            }
        }
    }

    /**
     * Represents a list of explicitly provided annotation descriptions.
     */
    static class Explicit extends AbstractList<AnnotationDescription> implements AnnotationList {

        /**
         * Creates a list of annotation lists for a given multidimensional list of annotation descriptions.
         *
         * @param annotations The list of annotations to represent as a list of annotation lists.
         * @return The list of annotation lists.
         */
        public static List<AnnotationList> asList(List<? extends List<? extends AnnotationDescription>> annotations) {
            List<AnnotationList> result = new ArrayList<AnnotationList>(annotations.size());
            for (List<? extends AnnotationDescription> annotation : annotations) {
                result.add(new Explicit(annotation));
            }
            return result;
        }

        /**
         * The list of represented annotation descriptions.
         */
        private final List<? extends AnnotationDescription> annotationDescriptions;

        /**
         * Creates a new list of annotation descriptions.
         *
         * @param annotationDescriptions The list of represented annotation descriptions.
         */
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
        public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
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

        @Override
        public AnnotationList subList(int fromIndex, int toIndex) {
            return new Explicit(super.subList(fromIndex, toIndex));
        }
    }
}
