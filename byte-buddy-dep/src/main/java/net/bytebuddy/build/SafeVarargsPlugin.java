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

import net.bytebuddy.asm.MemberAttributeExtension;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.attribute.AnnotationValueFilter;
import net.bytebuddy.implementation.attribute.MethodAttributeAppender;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static net.bytebuddy.matcher.ElementMatchers.declaresMethod;
import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;

/**
 * A plugin that allows for adding a {@code java.lang.SafeVarargs} annotation even if compiled prior to Java 7
 * which introduces this annotation. As the annotation is not present on previous JVM versions, it is ignored
 * at runtime for older JVM versions what makes this approach feasible.
 */
@HashCodeAndEqualsPlugin.Enhance
public class SafeVarargsPlugin extends Plugin.ForElementMatcher {

    /**
     * Creates a new plugin for creating repeated annotations.
     */
    public SafeVarargsPlugin() {
        super(declaresMethod(isAnnotatedWith(Enhance.class)));
    }

    /**
     * {@inheritDoc}
     */
    public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassFileLocator classFileLocator) {
        return builder.visit(new MemberAttributeExtension.ForMethod()
                .attribute(SafeVarargsAppender.INSTANCE)
                .on(isAnnotatedWith(Enhance.class)));
    }

    /**
     * {@inheritDoc}
     */
    public void close() {
        /* do nothing */
    }

    /**
     * Defines a safe-varargs annotation.
     */
    @Documented
    @Target({ElementType.CONSTRUCTOR, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Enhance {
        /* empty */
    }

    /**
     * Appends the varargs annotation.
     */
    protected enum SafeVarargsAppender implements MethodAttributeAppender, MethodAttributeAppender.Factory {

        /**
         * The singleton instance.
         */
        INSTANCE;

        /**
         * {@inheritDoc}
         */
        public MethodAttributeAppender make(TypeDescription typeDescription) {
            return this;
        }

        /**
         * {@inheritDoc}
         */
        public void apply(MethodVisitor methodVisitor, MethodDescription methodDescription, AnnotationValueFilter annotationValueFilter) {
            if (!methodDescription.isVarArgs()) {
                throw new IllegalStateException(methodDescription + " does not have variable arguments");
            } else if (!methodDescription.isConstructor() && !methodDescription.isStatic() && !methodDescription.isFinal()) {
                throw new IllegalStateException(methodDescription + " is neither a constructor or final and cannot declare safe varargs");
            }
            AnnotationVisitor visitor = methodVisitor.visitAnnotation("Ljava/lang/SafeVarargs;", true);
            if (visitor != null) {
                visitor.visitEnd();
            }
        }
    }
}
