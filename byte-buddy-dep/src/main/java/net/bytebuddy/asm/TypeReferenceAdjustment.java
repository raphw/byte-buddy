package net.bytebuddy.asm;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.CompoundList;
import net.bytebuddy.utility.OpenedClassReader;
import org.objectweb.asm.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class TypeReferenceAdjustment extends AsmVisitorWrapper.AbstractBase {

    private boolean strict;

    @Override
    public ClassVisitor wrap(TypeDescription instrumentedType,
                             ClassVisitor classVisitor,
                             Implementation.Context implementationContext,
                             TypePool typePool,
                             FieldList<FieldDescription.InDefinedShape> fields,
                             MethodList<?> methods,
                             int writerFlags,
                             int readerFlags) {
        return new TypeReferenceClassVisitor(classVisitor, typePool, strict);
    }

    protected static class TypeReferenceClassVisitor extends ClassVisitor {

        private final TypePool typePool;

        private final boolean strict;

        private final Set<String> observedTypes;

        private final Set<String> visitedInnerTypes;

        protected TypeReferenceClassVisitor(ClassVisitor classVisitor, TypePool typePool, boolean strict) {
            super(OpenedClassReader.ASM_API, classVisitor);
            this.typePool = typePool;
            this.strict = strict;
            observedTypes = new HashSet<>();
            visitedInnerTypes = new HashSet<>();
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName,
                          String[] interfaces) {
            if (superName != null) {
                observedTypes.add(superName);
            }
            if (interfaces != null) {
                observedTypes.addAll(Arrays.asList(interfaces));
            }
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public void visitNestHostExperimental(String nestHost) {
            observedTypes.add(nestHost);
            super.visitNestHostExperimental(nestHost);
        }

        @Override
        public void visitOuterClass(String owner, String name, String descriptor) {
            observedTypes.add(owner);
            super.visitOuterClass(owner, name, descriptor);
        }

        @Override
        public void visitNestMemberExperimental(String nestMember) {
            observedTypes.add(nestMember);
            super.visitNestMemberExperimental(nestMember);
        }

        @Override
        public void visitInnerClass(String name, String outerName, String innerName, int access) {
            visitedInnerTypes.add(name);
            super.visitInnerClass(name, outerName, innerName, access);
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
            FieldVisitor fieldVisitor = super.visitField(access, name, descriptor, signature, value);
            if (fieldVisitor != null) {
                Type type = Type.getType(descriptor);
                if (type.getSort() == Type.OBJECT) {
                    while (type.getSort() == Type.ARRAY) {
                        type = type.getElementType();
                    }
                    observedTypes.add(type.getInternalName());
                }
                return new TypeReferenceFieldVisitor(fieldVisitor);
            } else {
                return null;
            }
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            MethodVisitor methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
            if (methodVisitor != null) {
                Type methodType = Type.getType(descriptor), returnType = methodType.getReturnType();
                Type[] parameterType = methodType.getArgumentTypes();
                for (Type type : CompoundList.of(returnType, Arrays.asList(parameterType))) {
                    while (type.getSort() == Type.ARRAY) {
                        type = type.getElementType();
                    }
                    if (type.getSort() == Type.OBJECT) {
                        observedTypes.add(type.getInternalName());
                    }
                }
                if (exceptions != null) {
                    observedTypes.addAll(Arrays.asList(exceptions));
                }
                return new TypeReferenceMethodVisitor(methodVisitor);
            } else {
                return null;
            }
        }

        @Override
        public void visitEnd() {
            for (String observedType : observedTypes) {
                if (visitedInnerTypes.add(observedType)) {
                    TypePool.Resolution resolution = typePool.describe(observedType.replace('/', '.'));
                    if (resolution.isResolved()) {
                        TypeDescription typeDescription = resolution.resolve();
                        while (typeDescription != null && typeDescription.isInnerClass()) {
                            super.visitInnerClass(typeDescription.getInternalName(),
                                    typeDescription.isMemberType()
                                            ? typeDescription.getDeclaringType().getInternalName()
                                            : null,
                                    typeDescription.isAnonymousType()
                                            ? null
                                            : typeDescription.getSimpleName(),
                                    typeDescription.getModifiers());
                            typeDescription = typeDescription.getEnclosingType();
                            if (visitedInnerTypes.contains(typeDescription.getInternalName())) {
                                break;
                            }
                        }
                    } else if (strict) {
                        throw new IllegalStateException("Could not locate type for: " + observedType.replace('/', '.'));
                    }
                }
            }
            super.visitEnd();
        }

        protected class TypeReferenceFieldVisitor extends FieldVisitor {

            protected TypeReferenceFieldVisitor(FieldVisitor fieldVisitor) {
                super(OpenedClassReader.ASM_API, fieldVisitor);
            }
        }

        protected class TypeReferenceMethodVisitor extends MethodVisitor {

            protected TypeReferenceMethodVisitor(MethodVisitor methodVisitor) {
                super(OpenedClassReader.ASM_API, methodVisitor);
            }

            @Override
            public void visitTypeInsn(int opcode, String internalName) {
                String compoundTypeName = internalName;
                while (compoundTypeName.startsWith("[")) {
                    compoundTypeName = compoundTypeName.substring(1);
                }
                observedTypes.add(compoundTypeName);
                super.visitTypeInsn(opcode, internalName);
            }

            @Override
            public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                Type type = Type.getType(descriptor);
                if (type.getSort() == Type.OBJECT) {
                    while (type.getSort() == Type.ARRAY) {
                        type = type.getElementType();
                    }
                    observedTypes.add(type.getInternalName());
                }
                super.visitFieldInsn(opcode, owner, name, descriptor);
            }

            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                Type methodType = Type.getType(descriptor), returnType = methodType.getReturnType();
                Type[] parameterType = methodType.getArgumentTypes();
                for (Type type : CompoundList.of(returnType, Arrays.asList(parameterType))) {
                    while (type.getSort() == Type.ARRAY) {
                        type = type.getElementType();
                    }
                    if (type.getSort() == Type.OBJECT) {
                        observedTypes.add(type.getInternalName());
                    }
                }
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            }

            @Override
            public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle,
                                               Object... bootstrapMethodArguments) {
                Type methodType = Type.getType(descriptor), returnType = methodType.getReturnType();
                Type[] parameterType = methodType.getArgumentTypes();
                for (Type type : CompoundList.of(returnType, Arrays.asList(parameterType))) {
                    while (type.getSort() == Type.ARRAY) {
                        type = type.getElementType();
                    }
                    if (type.getSort() == Type.OBJECT) {
                        observedTypes.add(type.getInternalName());
                    }
                }
                super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
            }

            @Override
            public void visitLdcInsn(Object value) {
                if (value instanceof Type) {
                    Type type = (Type) value;
                    while (type.getSort() == Type.ARRAY) {
                        type = type.getElementType();
                    }
                    if (type.getSort() == Type.OBJECT) {
                        observedTypes.add(type.getInternalName());
                    }
                }
                super.visitLdcInsn(value);
            }

            @Override
            public void visitMultiANewArrayInsn(String descriptor, int dimensions) {
                Type type = Type.getType(descriptor);
                while (type.getSort() == Type.ARRAY) {
                    type = type.getElementType();
                }
                if (type.getSort() == Type.OBJECT) {
                    observedTypes.add(type.getInternalName());
                }
                super.visitMultiANewArrayInsn(descriptor, dimensions);
            }

            @Override
            public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
                observedTypes.add(type);
                super.visitTryCatchBlock(start, end, handler, type);
            }
        }
    }
}