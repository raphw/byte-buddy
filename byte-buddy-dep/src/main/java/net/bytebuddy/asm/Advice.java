package net.bytebuddy.asm;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
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

    private final Dispatcher methodEnter;

    private final Dispatcher methodExit;

    private final byte[] binaryRepresentation;

    protected Advice(Dispatcher methodEnter, Dispatcher methodExit, byte[] binaryRepresentation) {
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
            Dispatcher methodEnter = Dispatcher.Inactive.INSTANCE, methodExit = Dispatcher.Inactive.INSTANCE;
            for (MethodDescription.InDefinedShape methodDescription : typeDescription.getDeclaredMethods()) {
                methodEnter = resolve(OnMethodEnter.class, methodEnter, methodDescription);
                methodExit = resolve(OnMethodExit.class, methodExit, methodDescription);
            }
            if (!methodEnter.isActive() && !methodExit.isActive()) {
                throw new IllegalArgumentException("No advice defined by " + typeDescription);
            }
            return new Advice(methodEnter, methodExit.bindTo(methodEnter), classFileLocator.locate(typeDescription.getName()).resolve());
        } catch (IOException exception) {
            throw new IllegalStateException("Error reading class file of " + typeDescription, exception);
        }
    }

    private static Dispatcher resolve(Class<? extends Annotation> annotation, Dispatcher dispatcher, MethodDescription.InDefinedShape methodDescription) {
        if (methodDescription.getDeclaredAnnotations().isAnnotationPresent(annotation)) {
            if (dispatcher.isActive()) {
                throw new IllegalArgumentException("Duplicate advice for " + dispatcher + " and " + methodDescription);
            } else if (!methodDescription.isStatic()) {
                throw new IllegalArgumentException("Advice for " + methodDescription + " is not static");
            }
            return new Dispatcher.ForMethod(methodDescription);
        } else {
            return dispatcher;
        }
    }

    @Override
    public MethodVisitor wrap(TypeDescription instrumentedType, MethodDescription.InDefinedShape methodDescription, MethodVisitor methodVisitor) {
        return new AsmAdvice(methodVisitor, methodDescription);
    }

    protected class AsmAdvice extends MethodVisitor {

        private final MethodDescription instrumentedMethod;

        private final ClassReader classReader;

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
                case Opcodes.IRETURN:
                case Opcodes.FRETURN:
                case Opcodes.DRETURN:
                case Opcodes.LRETURN:
                case Opcodes.ARETURN:
                case Opcodes.ATHROW:
                    classReader.accept(new CodeCopier(methodExit), ClassReader.SKIP_DEBUG);
            }
            super.visitInsn(opcode);
        }

        protected class CodeCopier extends ClassVisitor {

            private final Dispatcher dispatcher;

            protected CodeCopier(Dispatcher dispatcher) {
                super(Opcodes.ASM5);
                this.dispatcher = dispatcher;
            }

            @Override
            public MethodVisitor visitMethod(int modifiers, String internalName, String descriptor, String signature, String[] exception) {
                return dispatcher.isInlined(internalName, descriptor)
                        ? new TransferringVisitor(AsmAdvice.this.mv, dispatcher.getInlinedMethod())
                        : IGNORE_METHOD;
            }

            protected class TransferringVisitor extends MethodVisitor {

                private final Map<Integer, AccessMapping> accessMappings;

                private final int offsetCorrection;

                private final Label endOfMethod;

                protected TransferringVisitor(MethodVisitor methodVisitor, MethodDescription inlinedMethod) {
                    super(Opcodes.ASM5, methodVisitor);
                    accessMappings = new HashMap<Integer, AccessMapping>();
                    for (ParameterDescription parameter : inlinedMethod.getParameters()) {
                        if (parameter.getDeclaredAnnotations().isAnnotationPresent(This.class)) {
                            if (instrumentedMethod.isStatic()) {
                                throw new IllegalStateException("Static methods do not imply a this reference for " + parameter);
                            }
                            accessMappings.put(parameter.getOffset(), AccessMapping.ForMethodArgument.ofThisReference());
                        } else if (parameter.getDeclaredAnnotations().isAnnotationPresent(Value.class)) {
                            accessMappings.put(parameter.getOffset(), new AccessMapping.ForValue(instrumentedMethod));
                        } else {
                            AnnotationDescription.Loadable<Argument> argument = parameter.getDeclaredAnnotations().ofType(Argument.class);
                            int index = argument == null
                                    ? parameter.getIndex()
                                    : argument.loadSilent().value();
                            if (instrumentedMethod.getParameters().size() <= index) {
                                throw new IllegalStateException(instrumentedMethod + " does not define a parameter of index " + index);
                            } else if (!instrumentedMethod.getParameters().get(index).getType().asErasure().isAssignableTo(parameter.getType().asErasure())) {
                                throw new IllegalStateException(parameter + " is not assignable to " + instrumentedMethod.getParameters().get(index));
                            }
                            accessMappings.put(parameter.getOffset(), new AccessMapping.ForMethodArgument(instrumentedMethod.getParameters().get(index).getOffset()));
                        }
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
                public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
                    return IGNORE_ANNOTATION;
                }

                @Override
                public AnnotationVisitor visitTypeAnnotation(int typeReference, TypePath typePath, String descriptor, boolean visible) {
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
                    dispatcher.prepare(mv, instrumentedMethod);
                }

                @Override
                public void visitMaxs(int maxStack, int maxLocals) {
                    /* do nothing */
                }

                @Override
                public void visitEnd() {
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
                            super.visitJumpInsn(Opcodes.GOTO, endOfMethod);
                            break;
                        case Opcodes.LRETURN:
                        case Opcodes.DRETURN:
                            super.visitJumpInsn(Opcodes.GOTO, endOfMethod);
                            break;
                        default:
                            super.visitInsn(opcode);
                    }
                }

                @Override
                public void visitVarInsn(int opcode, int offset) {
                    AccessMapping accessMapping = accessMappings.get(offset);
                    if (accessMapping != null) {
                        accessMapping.apply(mv, opcode);
                    } else {
                        super.visitVarInsn(opcode, offset + offsetCorrection);
                    }
                }
            }
        }
    }

    protected interface Dispatcher {

        boolean isActive();

        boolean isInlined(String internalName, String descriptor);

        MethodDescription getInlinedMethod();

        Dispatcher bindTo(Dispatcher methodEnter);

        void prepare(MethodVisitor methodVisitor, MethodDescription instrumentedMethod); // TODO: Does not work, stack not empty on "exit" method.

        enum Inactive implements Dispatcher {

            INSTANCE;

            @Override
            public boolean isActive() {
                return false;
            }

            @Override
            public boolean isInlined(String internalName, String descriptor) {
                return false;
            }

            @Override
            public MethodDescription getInlinedMethod() {
                // TODO: Is this meaningful?
                throw new IllegalStateException();
            }

            @Override
            public Dispatcher bindTo(Dispatcher methodEnter) {
                if (!methodEnter.getInlinedMethod().getReturnType().represents(void.class)) {
                    throw new IllegalStateException(methodEnter + " supplies return value that is never read");
                }
                return this;
            }

            @Override
            public void prepare(MethodVisitor methodVisitor, MethodDescription instrumentedMethod) {
                throw new IllegalStateException(); // TODO: Message
            }
        }

        class ForMethod implements Dispatcher {

            private final MethodDescription.InDefinedShape methodDescription;

            public ForMethod(MethodDescription.InDefinedShape methodDescription) {
                this.methodDescription = methodDescription;
            }

            @Override
            public boolean isActive() {
                return true;
            }

            @Override
            public boolean isInlined(String internalName, String descriptor) {
                return methodDescription.getInternalName().equals(internalName) && methodDescription.getDescriptor().equals(descriptor);
            }

            @Override
            public MethodDescription getInlinedMethod() {
                return methodDescription;
            }

            @Override
            public Dispatcher bindTo(Dispatcher methodEnter) {
                if (!methodDescription.getReturnType().represents(void.class)) {
                    throw new IllegalStateException(); // TODO: Message
                }
                TypeDescription suppliedType = methodEnter.getInlinedMethod().getReturnType().asErasure();
                boolean valueParameterExpected = !suppliedType.represents(void.class);
                for (ParameterDescription parameter : methodDescription.getParameters()) {
                    if (parameter.getDeclaredAnnotations().isAnnotationPresent(Value.class)) {
                        if (valueParameterExpected) {
                            if (!suppliedType.isAssignableTo(parameter.getType().asErasure())) {
                                throw new IllegalStateException(); // TODO: Message
                            }
                            valueParameterExpected = false;
                        } else {
                            throw new IllegalStateException(); // TODO: Message
                        }
                    }
                }
                return suppliedType.represents(void.class)
                        ? this
                        : new WithReturnValue(this, suppliedType);
            }

            @Override
            public void prepare(MethodVisitor methodVisitor, MethodDescription instrumentedMethod) {
                /* do nothing */
            }

            protected static class WithReturnValue implements Dispatcher {

                private final Dispatcher delegator;

                private final TypeDescription suppliedType;

                protected WithReturnValue(Dispatcher delegator, TypeDescription suppliedType) {
                    this.delegator = delegator;
                    this.suppliedType = suppliedType;
                }

                @Override
                public boolean isActive() {
                    return delegator.isActive();
                }

                @Override
                public boolean isInlined(String internalName, String descriptor) {
                    return delegator.isInlined(internalName, descriptor);
                }

                @Override
                public MethodDescription getInlinedMethod() {
                    return delegator.getInlinedMethod();
                }

                @Override
                public Dispatcher bindTo(Dispatcher methodEnter) {
                    throw new IllegalStateException();
                }

                @Override
                public void prepare(MethodVisitor methodVisitor, MethodDescription instrumentedMethod) {
                    methodVisitor.visitVarInsn(Type.getType(suppliedType.getDescriptor()).getOpcode(Opcodes.ASTORE), instrumentedMethod.getStackSize());
                }
            }
        }
    }

    protected interface AccessMapping {

        void apply(MethodVisitor methodVisitor, int opcode);

        class ForMethodArgument implements AccessMapping {

            public static AccessMapping ofThisReference() {
                return new ForMethodArgument(0);
            }

            private final int offset;

            protected ForMethodArgument(int offset) {
                this.offset = offset;
            }

            @Override
            public void apply(MethodVisitor methodVisitor, int opcode) {
                methodVisitor.visitVarInsn(opcode, offset);
            }
        }

        class ForValue implements AccessMapping {

            private final MethodDescription instrumentedMethod;

            public ForValue(MethodDescription instrumentedMethod) {
                this.instrumentedMethod = instrumentedMethod;
            }

            @Override
            public void apply(MethodVisitor methodVisitor, int opcode) {
                methodVisitor.visitVarInsn(opcode, instrumentedMethod.getStackSize());
            }
        }
    }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface OnMethodEnter {
        /* empty */
    }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface OnMethodExit {

        // boolean exceptional() default true;
    }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public @interface Argument {

        int value();
    }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public @interface This {
        /* empty */
    }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public @interface Value {
        /* empty */
    }
}
