package net.bytebuddy.description.annotation;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;

import java.util.Arrays;
import java.util.List;

/**
 * Describes a declaration source for annotations.
 */
public interface AnnotationSource {

    /**
     * Returns a list of annotations that are declared by this instance.
     *
     * @return A list of declared annotations.
     */
    AnnotationList getDeclaredAnnotations();

    /**
     * An annotation source that does not declare any annotations.
     */
    enum Empty implements AnnotationSource {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public AnnotationList getDeclaredAnnotations() {
            return new AnnotationList.Empty();
        }
    }

    /**
     * An annotation source that declares a given list of annotations.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class Explicit implements AnnotationSource {

        /**
         * The represented annotations.
         */
        private final List<? extends AnnotationDescription> annotations;

        /**
         * Creates a new explicit annotation source.
         *
         * @param annotation The represented annotations.
         */
        public Explicit(AnnotationDescription... annotation) {
            this(Arrays.asList(annotation));
        }

        /**
         * Creates a new explicit annotation source.
         *
         * @param annotations The represented annotations.
         */
        public Explicit(List<? extends AnnotationDescription> annotations) {
            this.annotations = annotations;
        }

        @Override
        public AnnotationList getDeclaredAnnotations() {
            return new AnnotationList.Explicit(annotations);
        }
    }
}
