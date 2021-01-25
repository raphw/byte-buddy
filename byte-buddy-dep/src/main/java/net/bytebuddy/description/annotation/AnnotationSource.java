package net.bytebuddy.description.annotation;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;

import java.util.Arrays;
import java.util.List;

/**
 * Describes a declaration source for annotations. 代表了注解源码的模型
 */
public interface AnnotationSource {

    /**
     * Returns a list of annotations that are declared by this instance. 返回所有标注的注解，类似Class.getgetDeclaredAnnotations()。但是AnnotationList是一个集合，是Bytebuddy定义的，方便对一组注解管理，和之前的TypeList类似
     *
     * @return A list of declared annotations.
     */
    AnnotationList getDeclaredAnnotations();

    /**
     * An annotation source that does not declare any annotations. 枚举类，代表着没有注解
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
     * An annotation source that declares a given list of annotations. 意思是明确的，后面还出现了很多Explicit，因为模糊就意味着无法准确的生成字节码
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
