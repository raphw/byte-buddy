package net.bytebuddy.asm.advice;

import net.bytebuddy.asm.AsmVisitorWrapper;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import org.objectweb.asm.*;

import java.io.IOException;
import java.lang.annotation.*;
import java.util.HashMap;
import java.util.Map;

public class Advice implements AsmVisitorWrapper.ForDeclaredMethods.MethodVisitorWrapper {

    private static final AnnotationVisitor IGNORE_ANNOTATION = null;

    private static final MethodVisitor IGNORE_METHOD = null;

    private final Map<String, MethodDescription> methodEnter;

    private final Map<String, MethodDescription> methodExit;

    private final byte[] binaryRepresentation;

    protected Advice(Map<String, MethodDescription> methodEnter, Map<String, MethodDescription> methodExit, byte[] binaryRepresentation) {
        this.methodEnter = methodEnter;
        this.methodExit = methodExit;
        this.binaryRepresentation = binaryRepresentation;
    }

    public static AsmVisitorWrapper.ForDeclaredMethods.MethodVisitorWrapper to(Class<?> type) {
        return to(type, ClassFileLocator.ForClassLoader.of(type.getClassLoader()));
    }

    public static AsmVisitorWrapper.ForDeclaredMethods.MethodVisitorWrapper to(Class<?> type, ClassFileLocator classFileLocator) {
        return to(new TypeDescription.ForLoadedType(type), classFileLocator);
    }

    public static AsmVisitorWrapper.ForDeclaredMethods.MethodVisitorWrapper to(TypeDescription typeDescription, ClassFileLocator classFileLocator) {
        try {
            Map<String, MethodDescription> methodEnter = new HashMap<String, MethodDescription>(), methodExit = new HashMap<String, MethodDescription>();
            for (MethodDescription methodDescription : typeDescription.getDeclaredMethods()) {
                considerAdvice(OnMethodEnter.class, methodEnter, methodDescription);
                considerAdvice(OnMethodExit.class, methodExit, methodDescription);
            }
            if (methodEnter.isEmpty() && methodExit.isEmpty()) {
                throw new IllegalArgumentException("No advice defined by " + typeDescription);
            }
            return new Advice(methodEnter, methodExit, classFileLocator.locate(typeDescription.getName()).resolve());
        } catch (IOException exception) {
            throw new IllegalStateException("Error reading class file of " + typeDescription, exception);
        }
    }

    private static void considerAdvice(Class<? extends Annotation> annotation, Map<String, MethodDescription> methods, MethodDescription methodDescription) {
        if (methodDescription.getDeclaredAnnotations().isAnnotationPresent(annotation)) {
            if (!methodDescription.isStatic()) {
                throw new IllegalStateException("Advice is not static: " + methodDescription);
            }
            methods.put(methodDescription.getInternalName() + methodDescription.getDescriptor(), methodDescription);
        }
    }

    @Override
    public MethodVisitor wrap(TypeDescription instrumentedType, MethodDescription.InDefinedShape methodDescription, MethodVisitor methodVisitor) {
        return new AsmAdvice(methodVisitor, methodDescription);
    }

    protected class AsmAdvice extends MethodVisitor {

        private static final int NO_VALUE = -1;

        private final MethodDescription instrumentedMethod;

        private final ClassReader classReader;

        private int maxStack = NO_VALUE;

        private int maxLocals = NO_VALUE;

        protected AsmAdvice(MethodVisitor methodVisitor, MethodDescription methodDescription) {
            super(Opcodes.ASM5, methodVisitor);
            classReader = new ClassReader(binaryRepresentation);
            this.instrumentedMethod = methodDescription;
        }

        @Override
        public void visitCode() {
            super.visitCode();
            classReader.accept(new CodeCopier(methodEnter), ClassReader.SKIP_DEBUG);
        }

        @Override
        public void visitInsn(int opcode) {
            switch (opcode) {
                case Opcodes.RETURN:
                    classReader.accept(new CodeCopier(methodExit), ClassReader.SKIP_DEBUG);
                    break;
                case Opcodes.IRETURN:
                case Opcodes.FRETURN:
                case Opcodes.ARETURN:
                case Opcodes.ATHROW:
                    classReader.accept(new CodeCopier(methodExit, 1), ClassReader.SKIP_DEBUG);
                    break;
                case Opcodes.DRETURN:
                case Opcodes.LRETURN:
                    classReader.accept(new CodeCopier(methodExit, 2), ClassReader.SKIP_DEBUG);
                    break;
            }
            // TODO: Adapt stack map frames to include additional frame!
            super.visitInsn(opcode);
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            super.visitMaxs(Math.max(maxStack, this.maxStack), Math.max(maxLocals, this.maxLocals));
        }

        protected class CodeCopier extends ClassVisitor {

            private final int stackIncrement;

            private final Map<String, MethodDescription> methods;

            protected CodeCopier(Map<String, MethodDescription> methods) {
                this(methods, 0);
            }

            protected CodeCopier(Map<String, MethodDescription> methods, int stackIncrement) {
                super(Opcodes.ASM5);
                this.stackIncrement = stackIncrement;
                this.methods = methods;
            }

            @Override
            public MethodVisitor visitMethod(int modifiers, String internalName, String descriptor, String signature, String[] exception) {
                MethodDescription methodDescription = methods.get(internalName + descriptor);
                return methodDescription == null
                        ? IGNORE_METHOD
                        : new TransferringVisitor(AsmAdvice.this.mv, methodDescription);
            }

            protected class TransferringVisitor extends MethodVisitor {

                private final Map<Integer, ParameterDescription> parameters;

                private final int offsetCorrection;

                private final Label endOfMethod;

                protected TransferringVisitor(MethodVisitor methodVisitor, MethodDescription inlinedMethod) {
                    super(Opcodes.ASM5, methodVisitor);
                    parameters = new HashMap<Integer, ParameterDescription>();
                    for (ParameterDescription parameter : inlinedMethod.getParameters()) {
                        parameters.put(parameter.getOffset(), parameter);
                    }
                    offsetCorrection = instrumentedMethod.getStackSize() - inlinedMethod.getStackSize();
                    endOfMethod = new Label();
                }

                @Override
                public void visitParameter(String name, int modifiers) {
                    /* do nothing */
                }

                @Override
                public AnnotationVisitor visitAnnotationDefault() {
                    return IGNORE_ANNOTATION;
                }

                @Override
                public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                    return IGNORE_ANNOTATION;
                }

                @Override
                public AnnotationVisitor visitTypeAnnotation(int typeReference, TypePath typePath, String descriptor, boolean visible) {
                    return IGNORE_ANNOTATION;
                }

                @Override
                public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
                    return IGNORE_ANNOTATION;
                }

                @Override
                public void visitAttribute(Attribute attr) {
                    /* do nothing */
                }

                @Override
                public void visitLineNumber(int line, Label start) {
                    /* do nothing */
                }

                @Override
                public void visitCode() {
                    /* do nothing */
                }

                @Override
                public void visitEnd() {
                    /* do nothing */
                }

                @Override
                public void visitMaxs(int maxStack, int maxLocals) {
                    AsmAdvice.this.maxStack = maxStack + stackIncrement;
                    AsmAdvice.this.maxLocals = maxLocals;
                    super.visitLabel(endOfMethod);
                }

                @Override
                public void visitInsn(int opcode) {
                    switch (opcode) {
                        case Opcodes.RETURN:
                            super.visitJumpInsn(Opcodes.GOTO, endOfMethod);
                            break;
                        case Opcodes.IRETURN:
                        case Opcodes.FRETURN:
                        case Opcodes.ARETURN:
                            super.visitInsn(Opcodes.POP);
                            super.visitJumpInsn(Opcodes.GOTO, endOfMethod);
                            break;
                        case Opcodes.LRETURN:
                        case Opcodes.DRETURN:
                            super.visitInsn(Opcodes.POP2);
                            super.visitJumpInsn(Opcodes.GOTO, endOfMethod);
                            break;
                        default:
                            super.visitInsn(opcode);
                    }
                }

                @Override
                public void visitVarInsn(int opcode, int offset) {
                    ParameterDescription parameterDescription = parameters.get(offset);
                    if (parameterDescription != null) {
                        AnnotationDescription.Loadable<Argument> argument = parameterDescription.getDeclaredAnnotations().ofType(Argument.class);
                        int targetIndex = argument == null
                                ? parameterDescription.getIndex()
                                : argument.loadSilent().value();
                        ParameterList<?> targetParameters = instrumentedMethod.getParameters();
                        if (targetIndex >= targetParameters.size()) {
                            throw new IllegalStateException(instrumentedMethod + " does not define a parameter with index " + targetIndex);
                        } else if (!targetParameters.get(targetIndex).getType().asErasure().isAssignableTo(parameterDescription.getType().asErasure())) {
                            throw new IllegalStateException("Cannot assign " + targetParameters.get(targetIndex) + " to " + parameterDescription);
                        }
                        offset = targetParameters.get(targetIndex).getOffset();
                    } else {
                        offset += offsetCorrection;
                    }
                    super.visitVarInsn(opcode, offset);
                }
            }
        }
    }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface OnMethodEnter {

    }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface OnMethodExit {

    }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public @interface Argument {

        int value();
    }
}
