/*
 * Copyright 2014 - Present Rafael Winterhalter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bytebuddy.build;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.attribute.AnnotationValueFilter;
import net.bytebuddy.implementation.attribute.TypeAttributeAppender;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;

import java.lang.annotation.*;

import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;
import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * A plugin that allows for adding a {@code java.lang.annotation.Repeatable} annotation even if compiled prior to
 * Java 8 which introduces this annotation. As the annotation is not present on previous JVM versions, it is ignored
 * at runtime for older JVM versions what makes this approach feasible.
 */
@HashCodeAndEqualsPlugin.Enhance
public class RepeatedAnnotationPlugin extends Plugin.ForElementMatcher {

    /**
     * A description of the {@link Enhance#value()} method.
     */
    private static final MethodDescription.InDefinedShape VALUE = TypeDescription.ForLoadedType.of(Enhance.class)
            .getDeclaredMethods()
            .filter(named("value"))
            .getOnly();

    /**
     * Creates a new plugin for creating repeated annotations.
     */
    public RepeatedAnnotationPlugin() {
        super(isAnnotatedWith(Enhance.class));
    }

    /**
     * {@inheritDoc}
     */
    @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", justification = "Assuming component type for array type.")
    public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassFileLocator classFileLocator) {
        TypeDescription target = typeDescription.getDeclaredAnnotations()
                .ofType(Enhance.class)
                .getValue(VALUE)
                .resolve(TypeDescription.class);
        if (!target.isAnnotation()) {
            throw new IllegalStateException("Expected " + target + " to be an annotation type");
        } else if (target.getDeclaredMethods().size() != 1
                || target.getDeclaredMethods().filter(named("value")).size() != 1
                || !target.getDeclaredMethods().filter(named("value")).getOnly().getReturnType().isArray()
                || !target.getDeclaredMethods().filter(named("value")).getOnly().getReturnType().getComponentType().asErasure().equals(typeDescription)) {
            throw new IllegalStateException("Expected " + target + " to declare exactly one property named value of an array type");
        }
        return builder.attribute(new RepeatedAnnotationAppender(target));
    }

    /**
     * {@inheritDoc}
     */
    public void close() {
        /* do nothing */
    }

    /**
     * Indicates that the annotated annotation should be repeatable by the supplied annotation.
     */
    @Documented
    @Target(ElementType.ANNOTATION_TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Enhance {

        /**
         * The repeating annotation type.
         *
         * @return The repeating annotation type.
         */
        Class<? extends Annotation> value();
    }

    /**
     * A type attribute appender that adds a repeated annotation for a target type.
     */
    @HashCodeAndEqualsPlugin.Enhance
    protected static class RepeatedAnnotationAppender implements TypeAttributeAppender {

        /**
         * The repeated type.
         */
        private final TypeDescription target;

        /**
         * Creates a new appender.
         *
         * @param target The repeated type.
         */
        protected RepeatedAnnotationAppender(TypeDescription target) {
            this.target = target;
        }

        /**
         * {@inheritDoc}
         */
        public void apply(ClassVisitor classVisitor, TypeDescription instrumentedType, AnnotationValueFilter annotationValueFilter) {
            AnnotationVisitor visitor = classVisitor.visitAnnotation("Ljava/lang/annotation/Repeatable;", true);
            if (visitor != null) {
                visitor.visit("value", Type.getType(target.getDescriptor()));
                visitor.visitEnd();
            }
        }
    }
}
