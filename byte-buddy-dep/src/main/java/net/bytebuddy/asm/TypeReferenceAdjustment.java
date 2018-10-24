/*
 * Copyright 2014 - 2018 Rafael Winterhalter
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
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.OpenedClassReader;
import org.objectweb.asm.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static net.bytebuddy.matcher.ElementMatchers.none;

/**
 * Adds an attribute value for all inner classes that are referenced by the instrumented type. Adding such attributes
 * is formally required by the Java virtual machine specification but it is not enforced by the verifier. As a result,
 * many alternative JVM languages do not correctly implement the attribute. By adding this visitor, a type's inner
 * class attribute can be repaired or created by registering this visitor wrapper.
 */
@HashCodeAndEqualsPlugin.Enhance
public class TypeReferenceAdjustment extends AsmVisitorWrapper.AbstractBase {

    /**
     * {@code true} if the visitor should throw an exception if a type reference cannot be located.
     */
    private final boolean strict;

    /**
     * A filter for excluding types from type reference analysis.
     */
    private final ElementMatcher.Junction<? super TypeDescription> filter;

    /**
     * Creates a type reference adjustment.
     *
     * @param strict {@code true} if the visitor should throw an exception if a type reference cannot be located.
     * @param filter A filter for excluding types from type reference analysis.
     */
    protected TypeReferenceAdjustment(boolean strict, ElementMatcher.Junction<? super TypeDescription> filter) {
        this.strict = strict;
        this.filter = filter;
    }

    /**
     * Creates a strict type reference adjustment that throws an exception if a type reference cannot be resolved
     * in the supplied type pool.
     *
     * @return A strict type reference adjustment.
     */
    public static TypeReferenceAdjustment strict() {
        return new TypeReferenceAdjustment(true, none());
    }

    /**
     * Creates a strict type reference adjustment that ignores type references that cannot be resolved
     * in the supplied type pool.
     *
     * @return A relaxed type reference adjustment.
     */
    public static TypeReferenceAdjustment relaxed() {
        return new TypeReferenceAdjustment(false, none());
    }

    /**
     * Excludes all matched types from being added as an attribute.
     *
     * @param filter A filter for excluding types from the attribute generation.
     * @return A new type reference adjustment that uses the supplied filter for excluding types.
     */
    public TypeReferenceAdjustment filter(ElementMatcher<? super TypeDescription> filter) {
        return new TypeReferenceAdjustment(strict, this.filter.<TypeDescription>or(filter));
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
        return new TypeReferenceClassVisitor(classVisitor, strict, filter, typePool);
    }

    /**
     * A class visitor that collects all type references and all inner class references.
     */
    protected static class TypeReferenceClassVisitor extends ClassVisitor {

        /**
         * Indicates that an annotation is not of interest.
         */
        private static final AnnotationVisitor IGNORE_ANNOTATION = null;

        /**
         * Indicates that a field is not of interest.
         */
        private static final FieldVisitor IGNORE_FIELD = null;

        /**
         * Indicates that a method is not of interest.
         */
        private static final MethodVisitor IGNORE_METHOD = null;

        /**
         * {@code true} if the visitor should throw an exception if a type reference cannot be located.
         */
        private final boolean strict;

        /**
         * A filter for excluding types from type reference analysis.
         */
        private final ElementMatcher<? super TypeDescription> filter;

        /**
         * The type pool to use for locating types.
         */
        private final TypePool typePool;

        /**
         * A set of inner class names that have been observed within the processed class file.
         */
        private final Set<String> observedTypes;

        /**
         * A set of inner class names that were added as inner class attribute values.
         */
        private final Set<String> visitedInnerTypes;

        /**
         * Creates a type reference class visitor.
         *
         * @param classVisitor {@code true} if the visitor should throw an exception if a type reference cannot be located.
         * @param strict       {@code true} if the visitor should throw an exception if a type reference cannot be located.
         * @param filter       A filter for excluding types from type reference analysis.
         * @param typePool     The type pool to use for locating types.
         */
        protected TypeReferenceClassVisitor(ClassVisitor classVisitor, boolean strict, ElementMatcher<? super TypeDescription> filter, TypePool typePool) {
            super(OpenedClassReader.ASM_API, classVisitor);
            this.typePool = typePool;
            this.strict = strict;
            this.filter = filter;
            observedTypes = new HashSet<String>();
            visitedInnerTypes = new HashSet<String>();
        }

        @Override
        public void visit(int version,
                          int modifiers,
                          String internalName,
                          String genericSignature,
                          String superClassInternalName,
                          String[] interfaceInternalName) {
            if (superClassInternalName != null) {
                observedTypes.add(superClassInternalName);
            }
            if (interfaceInternalName != null) {
                observedTypes.addAll(Arrays.asList(interfaceInternalName));
            }
            super.visit(version, modifiers, internalName, genericSignature, superClassInternalName, interfaceInternalName);
        }

        @Override
        public void visitNestHost(String nestHost) {
            observedTypes.add(nestHost);
            super.visitNestHost(nestHost);
        }

        @Override
        public void visitOuterClass(String ownerTypeInternalName, String methodName, String methodDescriptor) {
            observedTypes.add(ownerTypeInternalName);
            super.visitOuterClass(ownerTypeInternalName, methodName, methodDescriptor);
        }

        @Override
        public void visitNestMember(String nestMember) {
            observedTypes.add(nestMember);
            super.visitNestMember(nestMember);
        }

        @Override
        public void visitInnerClass(String internalName, String outerName, String innerName, int modifiers) {
            visitedInnerTypes.add(internalName);
            super.visitInnerClass(internalName, outerName, innerName, modifiers);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            observedTypes.add(Type.getType(descriptor).getInternalName());
            AnnotationVisitor annotationVisitor = super.visitAnnotation(descriptor, visible);
            if (annotationVisitor != null) {
                return new TypeReferenceAnnotationVisitor(annotationVisitor);
            } else {
                return IGNORE_ANNOTATION;
            }
        }

        @Override
        public AnnotationVisitor visitTypeAnnotation(int typeReference, TypePath typePath, String descriptor, boolean visible) {
            observedTypes.add(Type.getType(descriptor).getInternalName());
            AnnotationVisitor annotationVisitor = super.visitTypeAnnotation(typeReference, typePath, descriptor, visible);
            if (annotationVisitor != null) {
                return new TypeReferenceAnnotationVisitor(annotationVisitor);
            } else {
                return IGNORE_ANNOTATION;
            }
        }

        @Override
        public FieldVisitor visitField(int modifiers, String name, String descriptor, String signature, Object defaultValue) {
            FieldVisitor fieldVisitor = super.visitField(modifiers, name, descriptor, signature, defaultValue);
            if (fieldVisitor != null) {
                resolve(Type.getType(descriptor));
                return new TypeReferenceFieldVisitor(fieldVisitor);
            } else {
                return IGNORE_FIELD;
            }
        }

        @Override
        public MethodVisitor visitMethod(int modifiers, String internalName, String descriptor, String signature, String[] exceptionInternalName) {
            MethodVisitor methodVisitor = super.visitMethod(modifiers, internalName, descriptor, signature, exceptionInternalName);
            if (methodVisitor != null) {
                resolve(Type.getType(descriptor));
                if (exceptionInternalName != null) {
                    observedTypes.addAll(Arrays.asList(exceptionInternalName));
                }
                return new TypeReferenceMethodVisitor(methodVisitor);
            } else {
                return IGNORE_METHOD;
            }
        }

        @Override
        public void visitEnd() {
            for (String observedType : observedTypes) {
                if (visitedInnerTypes.add(observedType)) {
                    TypePool.Resolution resolution = typePool.describe(observedType.replace('/', '.'));
                    if (resolution.isResolved()) {
                        TypeDescription typeDescription = resolution.resolve();
                        if (!filter.matches(typeDescription)) {
                            while (typeDescription != null && typeDescription.isNestedClass()) {
                                super.visitInnerClass(typeDescription.getInternalName(),
                                        typeDescription.isMemberType()
                                                ? typeDescription.getDeclaringType().getInternalName()
                                                : null,
                                        typeDescription.isAnonymousType()
                                                ? null
                                                : typeDescription.getSimpleName(),
                                        typeDescription.getModifiers());
                                try {
                                    do {
                                        typeDescription = typeDescription.getEnclosingType();
                                    } while (typeDescription != null && !visitedInnerTypes.add(typeDescription.getInternalName()));
                                } catch (RuntimeException exception) {
                                    if (strict) {
                                        throw exception;
                                    } else {
                                        break;
                                    }
                                }
                            }
                        }
                    } else if (strict) {
                        throw new IllegalStateException("Could not locate type for: " + observedType.replace('/', '.'));
                    }
                }
            }
            super.visitEnd();
        }

        /**
         * Resolves all type references that are referenced by a {@link Type} value.
         *
         * @param type The type to resolve.
         */
        protected void resolve(Type type) {
            if (type.getSort() == Type.METHOD) {
                resolve(type.getReturnType());
                for (Type argumentType : type.getArgumentTypes()) {
                    resolve(argumentType);
                }
            } else {
                while (type.getSort() == Type.ARRAY) {
                    type = type.getElementType();
                }
                if (type.getSort() == Type.OBJECT) {
                    observedTypes.add(type.getInternalName());
                }
            }
        }

        /**
         * Resolves all type references that are referenced by a {@link Handle} value.
         *
         * @param handle The handle to resolve.
         */
        protected void resolve(Handle handle) {
            observedTypes.add(handle.getOwner());
            Type methodType = Type.getType(handle.getDesc());
            resolve(methodType.getReturnType());
            for (Type type : methodType.getArgumentTypes()) {
                resolve(type);
            }
        }

        /**
         * Resolves all type references that are referenced by a {@link ConstantDynamic} value.
         *
         * @param constant The dynamic constant to resolve.
         */
        protected void resolve(ConstantDynamic constant) {
            Type methodType = Type.getType(constant.getDescriptor());
            resolve(methodType.getReturnType());
            for (Type type : methodType.getArgumentTypes()) {
                resolve(type);
            }
            resolve(constant.getBootstrapMethod());
            for (int index = 0; index < constant.getBootstrapMethodArgumentCount(); index++) {
                resolve(constant.getBootstrapMethodArgument(index));
            }
        }

        /**
         * Resolves an internal name to its element type.
         *
         * @param internalName The internal name to resolve.
         */
        protected void resolveInternalName(String internalName) {
            while (internalName.startsWith("[")) {
                internalName = internalName.substring(1);
            }
            observedTypes.add(internalName);
        }

        /**
         * Resolves all type references that are referenced by any ASM constant value.
         *
         * @param value The unknown constant value to resolve.
         */
        protected void resolve(Object value) {
            if (value instanceof Type) {
                resolve((Type) value);
            } else if (value instanceof Handle) {
                resolve((Handle) value);
            } else if (value instanceof ConstantDynamic) {
                resolve((ConstantDynamic) value);
            }
        }

        /**
         * An annotation visitor that collects all type references.
         */
        protected class TypeReferenceAnnotationVisitor extends AnnotationVisitor {

            /**
             * Creates a new type reference-collecting annotation visitor.
             *
             * @param annotationVisitor The annotation visitor to delegate to.
             */
            protected TypeReferenceAnnotationVisitor(AnnotationVisitor annotationVisitor) {
                super(OpenedClassReader.ASM_API, annotationVisitor);
            }

            @Override
            public void visit(String name, Object value) {
                resolve(value);
                super.visit(name, value);
            }

            @Override
            public void visitEnum(String name, String descriptor, String value) {
                observedTypes.add(Type.getType(descriptor).getInternalName());
                super.visitEnum(name, descriptor, value);
            }

            @Override
            public AnnotationVisitor visitAnnotation(String name, String descriptor) {
                observedTypes.add(Type.getType(descriptor).getInternalName());
                AnnotationVisitor annotationVisitor = super.visitAnnotation(name, descriptor);
                if (annotationVisitor != null) {
                    return new TypeReferenceAnnotationVisitor(annotationVisitor);
                } else {
                    return IGNORE_ANNOTATION;
                }
            }

            @Override
            public AnnotationVisitor visitArray(String name) {
                AnnotationVisitor annotationVisitor = super.visitArray(name);
                if (annotationVisitor != null) {
                    return new TypeReferenceAnnotationVisitor(annotationVisitor);
                } else {
                    return IGNORE_ANNOTATION;
                }
            }
        }

        /**
         * A field visitor that collects all type references.
         */
        protected class TypeReferenceFieldVisitor extends FieldVisitor {

            /**
             * Creates a new type reference-collecting field visitor.
             *
             * @param fieldVisitor The field visitor to delegate to.
             */
            protected TypeReferenceFieldVisitor(FieldVisitor fieldVisitor) {
                super(OpenedClassReader.ASM_API, fieldVisitor);
            }

            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                observedTypes.add(Type.getType(descriptor).getInternalName());
                AnnotationVisitor annotationVisitor = super.visitAnnotation(descriptor, visible);
                if (annotationVisitor != null) {
                    return new TypeReferenceAnnotationVisitor(annotationVisitor);
                } else {
                    return IGNORE_ANNOTATION;
                }
            }
        }

        /**
         * A method visitor that collects all type references.
         */
        protected class TypeReferenceMethodVisitor extends MethodVisitor {

            /**
             * Creates a new type reference-collecting method visitor.
             *
             * @param methodVisitor The method visitor to delegate to.
             */
            protected TypeReferenceMethodVisitor(MethodVisitor methodVisitor) {
                super(OpenedClassReader.ASM_API, methodVisitor);
            }

            @Override
            public AnnotationVisitor visitAnnotationDefault() {
                AnnotationVisitor annotationVisitor = super.visitAnnotationDefault();
                if (annotationVisitor != null) {
                    return new TypeReferenceAnnotationVisitor(annotationVisitor);
                } else {
                    return IGNORE_ANNOTATION;
                }
            }

            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                observedTypes.add(Type.getType(descriptor).getInternalName());
                AnnotationVisitor annotationVisitor = super.visitAnnotation(descriptor, visible);
                if (annotationVisitor != null) {
                    return new TypeReferenceAnnotationVisitor(annotationVisitor);
                } else {
                    return IGNORE_ANNOTATION;
                }
            }

            @Override
            public AnnotationVisitor visitTypeAnnotation(int typeReference, TypePath typePath, String descriptor, boolean visible) {
                observedTypes.add(Type.getType(descriptor).getInternalName());
                AnnotationVisitor annotationVisitor = super.visitTypeAnnotation(typeReference, typePath, descriptor, visible);
                if (annotationVisitor != null) {
                    return new TypeReferenceAnnotationVisitor(annotationVisitor);
                } else {
                    return IGNORE_ANNOTATION;
                }
            }

            @Override
            public AnnotationVisitor visitParameterAnnotation(int index, String descriptor, boolean visible) {
                observedTypes.add(Type.getType(descriptor).getInternalName());
                AnnotationVisitor annotationVisitor = super.visitParameterAnnotation(index, descriptor, visible);
                if (annotationVisitor != null) {
                    return new TypeReferenceAnnotationVisitor(annotationVisitor);
                } else {
                    return IGNORE_ANNOTATION;
                }
            }

            @Override
            public AnnotationVisitor visitInsnAnnotation(int typeReference, TypePath typePath, String descriptor, boolean visible) {
                observedTypes.add(Type.getType(descriptor).getInternalName());
                AnnotationVisitor annotationVisitor = super.visitInsnAnnotation(typeReference, typePath, descriptor, visible);
                if (annotationVisitor != null) {
                    return new TypeReferenceAnnotationVisitor(annotationVisitor);
                } else {
                    return IGNORE_ANNOTATION;
                }
            }

            @Override
            public AnnotationVisitor visitTryCatchAnnotation(int typeReference, TypePath typePath, String descriptor, boolean visible) {
                observedTypes.add(Type.getType(descriptor).getInternalName());
                AnnotationVisitor annotationVisitor = super.visitTryCatchAnnotation(typeReference, typePath, descriptor, visible);
                if (annotationVisitor != null) {
                    return new TypeReferenceAnnotationVisitor(annotationVisitor);
                } else {
                    return IGNORE_ANNOTATION;
                }
            }

            @Override
            public AnnotationVisitor visitLocalVariableAnnotation(int typeReference,
                                                                  TypePath typePath,
                                                                  Label[] start,
                                                                  Label[] end,
                                                                  int[] index,
                                                                  String descriptor,
                                                                  boolean visible) {
                observedTypes.add(Type.getType(descriptor).getInternalName());
                AnnotationVisitor annotationVisitor = super.visitLocalVariableAnnotation(typeReference, typePath, start, end, index, descriptor, visible);
                if (annotationVisitor != null) {
                    return new TypeReferenceAnnotationVisitor(annotationVisitor);
                } else {
                    return IGNORE_ANNOTATION;
                }
            }

            @Override
            public void visitTypeInsn(int opcode, String internalName) {
                resolveInternalName(internalName);
                super.visitTypeInsn(opcode, internalName);
            }

            @Override
            public void visitFieldInsn(int opcode, String ownerInternalName, String name, String descriptor) {
                resolveInternalName(ownerInternalName);
                resolve(Type.getType(descriptor));
                super.visitFieldInsn(opcode, ownerInternalName, name, descriptor);
            }

            @Override
            public void visitMethodInsn(int opcode, String ownerInternalName, String name, String descriptor, boolean isInterface) {
                resolveInternalName(ownerInternalName);
                resolve(Type.getType(descriptor));
                super.visitMethodInsn(opcode, ownerInternalName, name, descriptor, isInterface);
            }

            @Override
            public void visitInvokeDynamicInsn(String name, String descriptor, Handle handle, Object[] argument) {
                resolve(Type.getType(descriptor));
                resolve(handle);
                for (Object anArgument : argument) {
                    resolve(anArgument);
                }
                super.visitInvokeDynamicInsn(name, descriptor, handle, argument);
            }

            @Override
            public void visitLdcInsn(Object value) {
                resolve(value);
                super.visitLdcInsn(value);
            }

            @Override
            public void visitMultiANewArrayInsn(String descriptor, int dimension) {
                resolve(Type.getType(descriptor));
                super.visitMultiANewArrayInsn(descriptor, dimension);
            }

            @Override
            public void visitTryCatchBlock(Label start, Label end, Label handler, String typeInternalName) {
                if (typeInternalName != null) {
                    observedTypes.add(typeInternalName);
                }
                super.visitTryCatchBlock(start, end, handler, typeInternalName);
            }
        }
    }
}
