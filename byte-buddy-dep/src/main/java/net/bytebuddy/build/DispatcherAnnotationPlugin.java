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
import net.bytebuddy.utility.dispatcher.JavaDispatcher;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * A plugin that adds a {@link net.bytebuddy.utility.dispatcher.JavaDispatcher.Proxied} annotation to any method of an
 * enhanced type where the annotation is not set. This aids in supporting obfuscators that rename methods which
 * makes reflection-based lookup by the proxy interface's name impossible.
 */
@HashCodeAndEqualsPlugin.Enhance
public class DispatcherAnnotationPlugin extends Plugin.ForElementMatcher implements MethodAttributeAppender.Factory, MethodAttributeAppender {

    /**
     * Creates a new dispatcher annotation plugin.
     */
    public DispatcherAnnotationPlugin() {
        super(isAnnotatedWith(JavaDispatcher.Proxied.class));
    }

    /**
     * {@inheritDoc}
     */
    public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassFileLocator classFileLocator) {
        return builder.visit(new MemberAttributeExtension.ForMethod()
                .attribute(this)
                .on(not(isAnnotatedWith(JavaDispatcher.Proxied.class)).<MethodDescription>and(isAbstract())));
    }

    /**
     * {@inheritDoc}
     */
    public void close() {
        /* do nothing */
    }

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
        AnnotationVisitor visitor = methodVisitor.visitAnnotation(Type.getDescriptor(JavaDispatcher.Proxied.class), true);
        if (visitor != null) {
            visitor.visit("value", methodDescription.getName());
            visitor.visitEnd();
        }
    }
}
