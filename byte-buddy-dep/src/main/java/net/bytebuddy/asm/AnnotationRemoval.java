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
package net.bytebuddy.asm;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.CompoundList;
import net.bytebuddy.utility.OpenedClassReader;
import net.bytebuddy.utility.nullability.MaybeNull;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

import java.util.HashMap;
import java.util.Map;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * A visitor wrapper that removes annotations from the instrumented type.
 */
@HashCodeAndEqualsPlugin.Enhance
public class AnnotationRemoval extends AsmVisitorWrapper.AbstractBase {

    /**
     * Indicates that neither method annotations and method parameter annotations should be considered.
     */
    private static final int METHOD_NONE = -4;

    /**
     * Indicates that bother method annotations and method parameter annotations should be considered.
     */
    private static final int METHOD_ALL = -3;

    /**
     * Indicates that only method annotations should be considered.
     */
    private static final int METHOD_ONLY = -2;

    /**
     * Indicates that only method parameter annotations should be considered.
     */
    private static final int METHOD_PARAMETERS = -1;

    /**
     * {@code true} if annotations on the type should be removed.
     */
    private final boolean type;

    /**
     * Matches fields from which annotations should be removed.
     */
    private final ElementMatcher<? super FieldDescription.InDefinedShape> fieldMatcher;

    /**
     * Matches methods from which annotations should be removed.
     */
    private final ElementMatcher<? super MethodDescription> methodMatcher;

    /**
     * Matches annotations that should be removed.
     */
    private final ElementMatcher<? super AnnotationDescription> annotationMatcher;

    /**
     * Indices the method parameter index from which annotations should be removed,
     * or a negative value to indicate different treatment.
     */
    private final int parameters;

    /**
     * Creates a visitor for annotation removal.
     *
     * @param type              {@code true} if annotations on the type should be removed.
     * @param fieldMatcher      Matches fields from which annotations should be removed.
     * @param methodMatcher     Matches methods from which annotations should be removed.
     * @param annotationMatcher Matches annotations that should be removed.
     * @param parameters        Indices the method parameter index from which annotations should be removed,
     *                          or a negative value to indicate different treatment.
     */
    protected AnnotationRemoval(boolean type,
                                ElementMatcher<? super FieldDescription.InDefinedShape.InDefinedShape> fieldMatcher,
                                ElementMatcher<? super MethodDescription> methodMatcher,
                                ElementMatcher<? super AnnotationDescription> annotationMatcher,
                                int parameters) {
        this.type = type;
        this.fieldMatcher = fieldMatcher;
        this.methodMatcher = methodMatcher;
        this.annotationMatcher = annotationMatcher;
        this.parameters = parameters;
    }

    /**
     * Creates a visitor that removes all annotations that match the specified matcher from the instrumented type.
     *
     * @param matcher The matcher to indicate what annotations to remove.
     * @return A visitor that removes the specified annotations.
     */
    public static AnnotationRemoval strip(ElementMatcher<? super AnnotationDescription> matcher) {
        return new AnnotationRemoval(true, any(), any(), matcher, METHOD_ALL);
    }

    /**
     * Creates a visitor that only removes annotations from the type.
     *
     * @return An appropriate visitor for annotation removal.
     */
    public AsmVisitorWrapper onType() {
        return new AnnotationRemoval(true, none(), none(), annotationMatcher, METHOD_NONE);
    }

    /**
     * Creates a visitor that only removes annotations from fields that match the specified matcher.
     *
     * @param matcher A matcher that indicates from what fields annotations should be removed.
     * @return An appropriate visitor for annotation removal.
     */
    public AsmVisitorWrapper onFields(ElementMatcher<? super FieldDescription> matcher) {
        return new AnnotationRemoval(false, matcher, none(), annotationMatcher, METHOD_NONE);
    }

    /**
     * Creates a visitor that removes annotations from methods that match the specified matcher.
     *
     * @param matcher A matcher that indicates from what methods annotations should be removed.
     * @return An appropriate visitor for annotation removal.
     */
    public AsmVisitorWrapper onMethods(ElementMatcher<? super MethodDescription> matcher) {
        return onInvokables(isMethod().and(matcher));
    }

    /**
     * Creates a visitor that removes annotations from methods and their parameters that match the specified matcher.
     *
     * @param matcher A matcher that indicates from what methods annotations should be removed.
     * @return An appropriate visitor for annotation removal.
     */
    public AsmVisitorWrapper onMethodsAndParameters(ElementMatcher<? super MethodDescription> matcher) {
        return onInvokablesAndParameters(isMethod().and(matcher));
    }

    /**
     * Creates a visitor that removes annotations from method parameters where the method matches the specified matcher.
     *
     * @param matcher A matcher that indicates from what methods annotations should be removed.
     * @return An appropriate visitor for annotation removal.
     */
    public AsmVisitorWrapper onMethodParameters(ElementMatcher<? super MethodDescription> matcher) {
        return onInvokableParameters(isMethod().and(matcher));
    }

    /**
     * Creates a visitor that removes annotations from the method parameters with the given index
     * where the method matches the specified matcher.
     *
     * @param matcher   A matcher that indicates from what methods annotations should be removed.
     * @param parameter The index of the parameter of which to remove annotations.
     * @return An appropriate visitor for annotation removal.
     */
    public AsmVisitorWrapper onMethodParameter(ElementMatcher<? super MethodDescription> matcher, int parameter) {
        return onInvokableParameter(isMethod().and(matcher), parameter);
    }

    /**
     * Creates a visitor that removes annotations from constructors that match the specified matcher.
     *
     * @param matcher A matcher that indicates from what constructors annotations should be removed.
     * @return An appropriate visitor for annotation removal.
     */
    public AsmVisitorWrapper onConstructors(ElementMatcher<? super MethodDescription> matcher) {
        return onInvokables(isConstructor().and(matcher));
    }

    /**
     * Creates a visitor that removes annotations from constructors and their parameters that match the specified matcher.
     *
     * @param matcher A matcher that indicates from what constructors annotations should be removed.
     * @return An appropriate visitor for annotation removal.
     */
    public AsmVisitorWrapper onConstructorsAndParameters(ElementMatcher<? super MethodDescription> matcher) {
        return onInvokablesAndParameters(isConstructor().and(matcher));
    }

    /**
     * Creates a visitor that removes annotations from constructor parameters where the constructor matches the specified matcher.
     *
     * @param matcher A matcher that indicates from what constructors annotations should be removed.
     * @return An appropriate visitor for annotation removal.
     */
    public AsmVisitorWrapper onConstructorParameters(ElementMatcher<? super MethodDescription> matcher) {
        return onInvokableParameters(isConstructor().and(matcher));
    }

    /**
     * Creates a visitor that removes annotations from the constructor parameters with the given index
     * where the constructor matches the specified matcher.
     *
     * @param matcher   A matcher that indicates from what constructors annotations should be removed.
     * @param parameter The index of the parameter of which to remove annotations.
     * @return An appropriate visitor for annotation removal.
     */
    public AsmVisitorWrapper onConstructorParameter(ElementMatcher<? super MethodDescription> matcher, int parameter) {
        return onInvokableParameter(isConstructor().and(matcher), parameter);
    }

    /**
     * Creates a visitor that removes annotations from constructors or methods that match the specified matcher.
     *
     * @param matcher A matcher that indicates from what constructors or methods annotations should be removed.
     * @return An appropriate visitor for annotation removal.
     */
    public AsmVisitorWrapper onInvokables(ElementMatcher<? super MethodDescription> matcher) {
        return new AnnotationRemoval(false, none(), matcher, annotationMatcher, METHOD_ONLY);
    }

    /**
     * Creates a visitor that removes annotations from constructors or methods and their parameters
     * that match the specified matcher.
     *
     * @param matcher A matcher that indicates from what constructors or methods annotations should be removed.
     * @return An appropriate visitor for annotation removal.
     */
    public AsmVisitorWrapper onInvokablesAndParameters(ElementMatcher<? super MethodDescription> matcher) {
        return new AnnotationRemoval(false, none(), matcher, annotationMatcher, METHOD_ALL);
    }

    /**
     * Creates a visitor that removes annotations from constructor or method parameters where the
     * constructor or method matches the specified matcher.
     *
     * @param matcher A matcher that indicates from what constructors or methods annotations should be removed.
     * @return An appropriate visitor for annotation removal.
     */
    public AsmVisitorWrapper onInvokableParameters(ElementMatcher<? super MethodDescription> matcher) {
        return new AnnotationRemoval(false, none(), matcher, annotationMatcher, METHOD_PARAMETERS);
    }

    /**
     * Creates a visitor that removes annotations from the constructor or method parameters with the given index
     * where the constructor or method matches the specified matcher.
     *
     * @param matcher   A matcher that indicates from what constructors or methods annotations should be removed.
     * @param parameter The index of the parameter of which to remove annotations.
     * @return An appropriate visitor for annotation removal.
     */
    public AsmVisitorWrapper onInvokableParameter(ElementMatcher<? super MethodDescription> matcher, int parameter) {
        if (parameter < 0) {
            throw new IllegalArgumentException("Parameter index cannot be negative: " + parameter);
        }
        return new AnnotationRemoval(false, none(), matcher, annotationMatcher, parameter);
    }

    /**
     * {@inheritDoc}
     */
    public ClassVisitor wrap(TypeDescription instrumentedType,
                             ClassVisitor classVisitor,
                             Implementation.Context implementationContext,
                             TypePool typePool,
                             FieldList<FieldDescription.InDefinedShape> fields,
                             MethodList<?> methods,
                             int writerFlags,
                             int readerFlags) {
        Map<String, AnnotationDescription> mappedAnnotations = new HashMap<String, AnnotationDescription>();
        if (type) {
            for (AnnotationDescription annotation : instrumentedType.getDeclaredAnnotations()) {
                mappedAnnotations.put(annotation.getAnnotationType().getDescriptor(), annotation);
            }
        }
        Map<String, FieldDescription.InDefinedShape> mappedFields = new HashMap<String, FieldDescription.InDefinedShape>();
        for (FieldDescription.InDefinedShape fieldDescription : fields) {
            mappedFields.put(fieldDescription.getInternalName() + fieldDescription.getDescriptor(), fieldDescription);
        }
        Map<String, MethodDescription> mappedMethods = new HashMap<String, MethodDescription>();
        for (MethodDescription methodDescription : CompoundList.<MethodDescription>of(methods, new MethodDescription.Latent.TypeInitializer(instrumentedType))) {
            mappedMethods.put(methodDescription.getInternalName() + methodDescription.getDescriptor(), methodDescription);
        }
        return new AnnotationRemovingClassVisitor(classVisitor, fieldMatcher, methodMatcher, annotationMatcher, parameters, mappedFields, mappedMethods, mappedAnnotations);
    }

    /**
     * A class visitor that removes annotations.
     */
    private static class AnnotationRemovingClassVisitor extends ClassVisitor {

        /**
         * Matches fields from which annotations should be removed.
         */
        private final ElementMatcher<? super FieldDescription.InDefinedShape> fieldMatcher;

        /**
         * Matches methods from which annotations should be removed.
         */
        private final ElementMatcher<? super MethodDescription> methodMatcher;

        /**
         * Matches annotations that should be removed.
         */
        private final ElementMatcher<? super AnnotationDescription> annotationMatcher;

        /**
         * Indices the method parameter index from which annotations should be removed,
         * or a negative value to indicate different treatment.
         */
        private final int parameters;

        /**
         * A map of internal field names and descriptors to consider for removal.
         */
        private final Map<String, FieldDescription.InDefinedShape> fields;

        /**
         * A map of internal method names and descriptors to consider for removal.
         */
        private final Map<String, MethodDescription> methods;

        /**
         * A map of annotation type descriptors names and descriptors to consider for removal.
         */
        private final Map<String, AnnotationDescription> annotations;

        /**
         * Creates a class visitor for annotation removal.
         *
         * @param classVisitor      The class visitor to delegate to.
         * @param fieldMatcher      Matches fields from which annotations should be removed.
         * @param methodMatcher     Matches methods from which annotations should be removed.
         * @param annotationMatcher Matches annotations that should be removed.
         * @param parameters        Indices the method parameter index from which annotations should be removed,
         *                          or a negative value to indicate different treatment.
         * @param fields            A map of internal field names and descriptors to consider for removal.
         * @param methods           A map of internal method names and descriptors to consider for removal.
         * @param annotations       A map of annotation type descriptors names and descriptors to consider for removal.
         */
        private AnnotationRemovingClassVisitor(ClassVisitor classVisitor,
                                               ElementMatcher<? super FieldDescription.InDefinedShape> fieldMatcher,
                                               ElementMatcher<? super MethodDescription> methodMatcher,
                                               ElementMatcher<? super AnnotationDescription> annotationMatcher,
                                               int parameters,
                                               Map<String, FieldDescription.InDefinedShape> fields,
                                               Map<String, MethodDescription> methods,
                                               Map<String, AnnotationDescription> annotations) {
            super(OpenedClassReader.ASM_API, classVisitor);
            this.fieldMatcher = fieldMatcher;
            this.methodMatcher = methodMatcher;
            this.annotationMatcher = annotationMatcher;
            this.parameters = parameters;
            this.fields = fields;
            this.methods = methods;
            this.annotations = annotations;
        }

        @Override
        @MaybeNull
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            AnnotationDescription annotation = annotations.get(descriptor);
            return annotation != null && annotationMatcher.matches(annotation)
                    ? null
                    : super.visitAnnotation(descriptor, visible);
        }

        @Override
        @MaybeNull
        public FieldVisitor visitField(int modifiers, String internalName, String descriptor, String signature, Object value) {
            FieldVisitor fieldVisitor = super.visitField(modifiers, internalName, descriptor, signature, value);
            if (fieldVisitor == null) {
                return null;
            }
            FieldDescription.InDefinedShape fieldDescription = fields.get(internalName + descriptor);
            if (fieldDescription != null && fieldMatcher.matches(fieldDescription)) {
                Map<String, AnnotationDescription> mappedAnnotations = new HashMap<String, AnnotationDescription>();
                for (AnnotationDescription annotation : fieldDescription.getDeclaredAnnotations()) {
                    mappedAnnotations.put(annotation.getAnnotationType().getDescriptor(), annotation);
                }
                return new AnnotationRemovingFieldVisitor(fieldVisitor, annotationMatcher, mappedAnnotations);
            } else {
                return fieldVisitor;
            }
        }

        @Override
        @MaybeNull
        public MethodVisitor visitMethod(int modifiers, String internalName, String descriptor, String signature, String[] exception) {
            MethodVisitor methodVisitor = super.visitMethod(modifiers, internalName, descriptor, signature, exception);
            if (methodVisitor == null) {
                return null;
            }
            MethodDescription methodDescription = methods.get(internalName + descriptor);
            if (methodDescription != null && methodMatcher.matches(methodDescription)) {
                Map<Integer, Map<String, AnnotationDescription>> mappedParameterAnnotations = new HashMap<Integer, Map<String, AnnotationDescription>>();
                if (parameters >= 0 || parameters == METHOD_PARAMETERS || parameters == METHOD_ALL) {
                    for (ParameterDescription parameter : methodDescription.getParameters()) {
                        Map<String, AnnotationDescription> mappedAnnotations = new HashMap<String, AnnotationDescription>();
                        if (parameter.getIndex() == parameters || parameters < 0) {
                            for (AnnotationDescription annotation : parameter.getDeclaredAnnotations()) {
                                mappedAnnotations.put(annotation.getAnnotationType().getDescriptor(), annotation);
                            }
                        }
                        mappedParameterAnnotations.put(parameter.getIndex(), mappedAnnotations);
                    }
                }
                Map<String, AnnotationDescription> mappedAnnotations = new HashMap<String, AnnotationDescription>();
                if (parameters == METHOD_ONLY || parameters == METHOD_ALL) {
                    for (AnnotationDescription annotation : methodDescription.getDeclaredAnnotations()) {
                        mappedAnnotations.put(annotation.getAnnotationType().getDescriptor(), annotation);
                    }
                }
                return new AnnotationRemovingMethodVisitor(methodVisitor, annotationMatcher, mappedParameterAnnotations, mappedAnnotations);
            } else {
                return methodVisitor;
            }
        }
    }

    /**
     * A field visitor that removes annotations.
     */
    private static class AnnotationRemovingFieldVisitor extends FieldVisitor {

        /**
         * Matches annotations that should be removed.
         */
        private final ElementMatcher<? super AnnotationDescription> annotationMatcher;

        /**
         * A map of annotation type descriptors names and descriptors to consider for removal.
         */
        private final Map<String, AnnotationDescription> annotations;

        /**
         * Creates a visitor for removing annotations from fields.
         *
         * @param fieldVisitor      The field visitor to delegate to.
         * @param annotationMatcher AMatches annotations that should be removed.
         * @param annotations       A map of annotation type descriptors names and descriptors to consider for removal.
         */
        private AnnotationRemovingFieldVisitor(FieldVisitor fieldVisitor,
                                               ElementMatcher<? super AnnotationDescription> annotationMatcher,
                                               Map<String, AnnotationDescription> annotations) {
            super(OpenedClassReader.ASM_API, fieldVisitor);
            this.annotationMatcher = annotationMatcher;
            this.annotations = annotations;
        }

        @Override
        @MaybeNull
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            AnnotationDescription annotation = annotations.get(descriptor);
            return annotation != null && annotationMatcher.matches(annotation)
                    ? null
                    : super.visitAnnotation(descriptor, visible);
        }
    }

    /**
     * Creates a visitor for removing annotations from methods and method parameters.
     */
    private static class AnnotationRemovingMethodVisitor extends MethodVisitor {

        /**
         * Matches annotations that should be removed.
         */
        private final ElementMatcher<? super AnnotationDescription> annotationMatcher;

        /**
         * A map of parameter indices to maps of annotation type descriptors names and descriptors to consider
         * for removal.
         */
        private final Map<Integer, Map<String, AnnotationDescription>> parameterAnnotations;

        /**
         * A map of annotation type descriptors names and descriptors to consider for removal.
         */
        private final Map<String, AnnotationDescription> annotations;

        /**
         * Creates an annotation removing method visitor.
         *
         * @param methodVisitor        The method visitor to delegate to.
         * @param annotationMatcher    Matches annotations that should be removed.
         * @param parameterAnnotations A map of parameter indices to maps of annotation type descriptors names and
         *                             descriptors to consider for removal.
         * @param annotations          A map of annotation type descriptors names and descriptors to consider for removal.
         */
        private AnnotationRemovingMethodVisitor(MethodVisitor methodVisitor,
                                                ElementMatcher<? super AnnotationDescription> annotationMatcher,
                                                Map<Integer, Map<String, AnnotationDescription>> parameterAnnotations,
                                                Map<String, AnnotationDescription> annotations) {
            super(OpenedClassReader.ASM_API, methodVisitor);
            this.annotationMatcher = annotationMatcher;
            this.parameterAnnotations = parameterAnnotations;
            this.annotations = annotations;
        }

        @Override
        @MaybeNull
        public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
            Map<String, AnnotationDescription> annotations = parameterAnnotations.get(parameter);
            if (annotations != null) {
                AnnotationDescription annotation = annotations.get(descriptor);
                return annotation != null && annotationMatcher.matches(annotation)
                        ? null
                        : super.visitParameterAnnotation(parameter, descriptor, visible);
            } else {
                return super.visitParameterAnnotation(parameter, descriptor, visible);
            }
        }

        @Override
        @MaybeNull
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            AnnotationDescription annotation = annotations.get(descriptor);
            return annotation != null && annotationMatcher.matches(annotation)
                    ? null
                    : super.visitAnnotation(descriptor, visible);
        }
    }
}
