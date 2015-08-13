package net.bytebuddy.description.annotation;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.FilterableList;

import java.lang.annotation.Annotation;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Defines a list of annotation instances.
 */
public interface AnnotationList extends FilterableList<AnnotationDescription, AnnotationList> {


    /**
     * Checks if this list contains an annotation of the given type.
     *
     * @param annotationType The type to find in the list.
     * @return {@code true} if the list contains the annotation type.
     */
    boolean isAnnotationPresent(Class<? extends Annotation> annotationType);

    /**
     * Checks if this list contains an annotation of the given type.
     *
     * @param annotationType The type to find in the list.
     * @return {@code true} if the list contains the annotation type.
     */
    boolean isAnnotationPresent(TypeDescription annotationType);

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

    /**
     * Only retains annotations with the given retention policy.
     *
     * @param matcher A matcher for the required retention policy.
     * @return A of annotations only with elements
     */
    AnnotationList visibility(ElementMatcher<? super RetentionPolicy> matcher);

    /**
     * An abstract base implementation of an annotation list.
     */
    abstract class AbstractBase extends FilterableList.AbstractBase<AnnotationDescription, AnnotationList> implements AnnotationList {

        @Override
        public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
            for (AnnotationDescription annotation : this) {
                if (annotation.getAnnotationType().represents(annotationType)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean isAnnotationPresent(TypeDescription annotationType) {
            for (AnnotationDescription annotation : this) {
                if (annotation.getAnnotationType().equals(annotationType)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public <T extends Annotation> AnnotationDescription.Loadable<T> ofType(Class<T> annotationType) {
            for (AnnotationDescription annotation : this) {
                if (annotation.getAnnotationType().represents(annotationType)) {
                    return annotation.prepare(annotationType);
                }
            }
            return null;
        }

        @Override
        public AnnotationList inherited(Set<? extends TypeDescription> ignoredTypes) {
            List<AnnotationDescription> inherited = new ArrayList<AnnotationDescription>(size());
            for (AnnotationDescription annotation : this) {
                if (!ignoredTypes.contains(annotation.getAnnotationType()) && annotation.isInherited()) {
                    inherited.add(annotation);
                }
            }
            return wrap(inherited);
        }

        @Override
        public AnnotationList visibility(ElementMatcher<? super RetentionPolicy> matcher) {
            List<AnnotationDescription> annotationDescriptions = new ArrayList<AnnotationDescription>(size());
            for (AnnotationDescription annotation : this) {
                if (matcher.matches(annotation.getRetention())) {
                    annotationDescriptions.add(annotation);
                }
            }
            return wrap(annotationDescriptions);
        }

        @Override
        protected AnnotationList wrap(List<AnnotationDescription> values) {
            return new Explicit(values);
        }
    }

    /**
     * Describes an array of loaded {@link java.lang.annotation.Annotation}s as an annotation list.
     */
    class ForLoadedAnnotation extends AbstractBase {

        /**
         * The represented annotations.
         */
        private final List<? extends Annotation> annotations;

        /**
         * Creates a new list of loaded annotations.
         *
         * @param annotation The represented annotations.
         */
        public ForLoadedAnnotation(Annotation... annotation) {
            this(Arrays.asList(annotation));
        }

        /**
         * Creates a new list of loaded annotations.
         *
         * @param annotations The represented annotations.
         */
        public ForLoadedAnnotation(List<? extends Annotation> annotations) {
            this.annotations = annotations;
        }

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

        @Override
        public AnnotationDescription get(int index) {
            return AnnotationDescription.ForLoadedAnnotation.of(annotations.get(index));
        }

        @Override
        public int size() {
            return annotations.size();
        }
    }

    /**
     * Represents a list of explicitly provided annotation descriptions.
     */
    class Explicit extends AbstractBase {

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

        @Override
        public AnnotationDescription get(int index) {
            return annotationDescriptions.get(index);
        }

        @Override
        public int size() {
            return annotationDescriptions.size();
        }
    }

    /**
     * Represents an empty annotation list.
     */
    class Empty extends FilterableList.Empty<AnnotationDescription, AnnotationList> implements AnnotationList {

        /**
         * Creates a list of empty annotation lists of the given dimension.
         *
         * @param length The length of the list.
         * @return A list of empty annotation lists of the given length.
         */
        public static List<AnnotationList> asList(int length) {
            List<AnnotationList> result = new ArrayList<AnnotationList>(length);
            for (int i = 0; i < length; i++) {
                result.add(new AnnotationList.Empty());
            }
            return result;
        }

        @Override
        public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
            return false;
        }

        @Override
        public boolean isAnnotationPresent(TypeDescription annotationType) {
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
        public AnnotationList visibility(ElementMatcher<? super RetentionPolicy> matcher) {
            return this;
        }
    }
}
