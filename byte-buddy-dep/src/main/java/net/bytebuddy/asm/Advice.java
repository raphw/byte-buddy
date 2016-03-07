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
            return new Advice(methodEnter, methodExit.asExitFrom(methodEnter), classFileLocator.locate(typeDescription.getName()).resolve());
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
        public void visitVarInsn(int opcode, int offset) {
            if (offset >= instrumentedMethod.getStackSize()) {
                offset += methodEnter.getValueOffset() + instrumentedMethod.getReturnType().asErasure().getStackSize().getSize();
            }
            super.visitVarInsn(opcode, offset);
        }

        @Override
        public void visitInsn(int opcode) {
            switch (opcode) {
                case Opcodes.RETURN:
                    classReader.accept(new CodeCopier(methodExit), ClassReader.SKIP_DEBUG);
                    break;
                case Opcodes.IRETURN:
                    super.visitInsn(Opcodes.DUP);
                    super.visitVarInsn(Opcodes.ISTORE, instrumentedMethod.getStackSize() + methodEnter.getValueOffset());
                    classReader.accept(new CodeCopier(methodExit), ClassReader.SKIP_DEBUG);
                    break;
                case Opcodes.FRETURN:
                    super.visitInsn(Opcodes.DUP);
                    super.visitVarInsn(Opcodes.FSTORE, instrumentedMethod.getStackSize() + methodEnter.getValueOffset());
                    classReader.accept(new CodeCopier(methodExit), ClassReader.SKIP_DEBUG);
                    break;
                case Opcodes.DRETURN:
                    super.visitInsn(Opcodes.DUP2);
                    super.visitVarInsn(Opcodes.DSTORE, instrumentedMethod.getStackSize() + methodEnter.getValueOffset());
                    classReader.accept(new CodeCopier(methodExit), ClassReader.SKIP_DEBUG);
                    break;
                case Opcodes.LRETURN:
                    super.visitInsn(Opcodes.DUP2);
                    super.visitVarInsn(Opcodes.LSTORE, instrumentedMethod.getStackSize() + methodEnter.getValueOffset());
                    classReader.accept(new CodeCopier(methodExit), ClassReader.SKIP_DEBUG);
                    break;
                case Opcodes.ATHROW:
                    if (methodExit.isSkipException()) {
                        break;
                    }
                case Opcodes.ARETURN:
                    super.visitInsn(Opcodes.DUP);
                    super.visitVarInsn(Opcodes.ASTORE, instrumentedMethod.getStackSize() + methodEnter.getValueOffset());
                    classReader.accept(new CodeCopier(methodExit), ClassReader.SKIP_DEBUG);
                    break;
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
                        ? new TransferringVisitor(dispatcher.getInlinedMethod())
                        : IGNORE_METHOD;
            }

            protected class TransferringVisitor extends MethodVisitor {

                private final Map<Integer, AccessMapping> accessMappings;

                private final int offset;

                private final Label endOfMethod;

                protected TransferringVisitor(MethodDescription inlinedMethod) {
                    super(Opcodes.ASM5, AsmAdvice.this.mv);
                    accessMappings = dispatcher.toAccessMapping(instrumentedMethod);
                    offset = instrumentedMethod.getStackSize() + instrumentedMethod.getReturnType().getStackSize().getSize() - inlinedMethod.getStackSize();
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
                    /* do nothing */
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
                            break;
                        case Opcodes.IRETURN:
                            storeReturnValue(Opcodes.ISTORE);
                            break;
                        case Opcodes.FRETURN:
                            storeReturnValue(Opcodes.FSTORE);
                            break;
                        case Opcodes.ARETURN:
                            storeReturnValue(Opcodes.ASTORE);
                            break;
                        case Opcodes.LRETURN:
                            storeReturnValue(Opcodes.LSTORE);
                            break;
                        case Opcodes.DRETURN:
                            storeReturnValue(Opcodes.DSTORE);
                            break;
                        default:
                            super.visitInsn(opcode);
                            return;
                    }
                    super.visitJumpInsn(Opcodes.GOTO, endOfMethod);
                }

                private void storeReturnValue(int opcode) {
                    super.visitVarInsn(opcode, instrumentedMethod.getStackSize() + instrumentedMethod.getReturnType().asErasure().getStackSize().getSize());
                }

                @Override
                public void visitVarInsn(int opcode, int offset) {
                    AccessMapping accessMapping = accessMappings.get(offset);
                    if (accessMapping != null) {
                        accessMapping.apply(mv, opcode);
                    } else {
                        super.visitVarInsn(opcode, offset + this.offset);
                    }
                }
            }
        }
    }

    protected interface Dispatcher {

        boolean isActive();

        boolean isInlined(String internalName, String descriptor);

        MethodDescription getInlinedMethod();

        Dispatcher asExitFrom(Dispatcher dispatcher);

        int getValueOffset();

        Map<Integer, AccessMapping> toAccessMapping(MethodDescription instrumentedMethod);

        boolean isSkipException();

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
            public Dispatcher asExitFrom(Dispatcher dispatcher) {
                if (!dispatcher.getInlinedMethod().getReturnType().represents(void.class)) {
                    throw new IllegalStateException(dispatcher + " supplies return value that is never read");
                }
                return this;
            }



            @Override
            public int getValueOffset() {
                return 0;
            }

            @Override
            public Map<Integer, AccessMapping> toAccessMapping(MethodDescription instrumentedMethod) {
                throw new IllegalStateException();
            }

            @Override
            public boolean isSkipException() {
                return true;
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
            public Dispatcher asExitFrom(Dispatcher dispatcher) {
                if (!methodDescription.getReturnType().represents(void.class)) {
                    throw new IllegalStateException(); // TODO: Message
                }
                boolean valueParameterExpected = dispatcher.isActive() && !dispatcher.getInlinedMethod().getReturnType().asErasure().represents(void.class);
                for (ParameterDescription parameter : methodDescription.getParameters()) {
                    if (parameter.getDeclaredAnnotations().isAnnotationPresent(EntranceValue.class)) {
                        if (valueParameterExpected) {
                            if (!dispatcher.getInlinedMethod().getReturnType().asErasure().isAssignableTo(parameter.getType().asErasure())) {
                                throw new IllegalStateException(); // TODO: Message
                            }
                            valueParameterExpected = false;
                        } else {
                            throw new IllegalStateException(); // TODO: Message
                        }
                    }
                }
                return !dispatcher.isActive() || dispatcher.getInlinedMethod().getReturnType().asErasure().represents(void.class)
                        ? this
                        : new WithReturnValue(this, dispatcher.getInlinedMethod().getReturnType().asErasure());
            }

            @Override
            public int getValueOffset() {
                return 0;
            }

            @Override
            public Map<Integer, AccessMapping> toAccessMapping(MethodDescription instrumentedMethod) {
                Map<Integer, AccessMapping> accessMappings = new HashMap<Integer, AccessMapping>();
                for (ParameterDescription parameter : methodDescription.getParameters()) {
                    if (parameter.getDeclaredAnnotations().isAnnotationPresent(This.class)) {
                        if (instrumentedMethod.isStatic()) {
                            throw new IllegalStateException("Static methods do not imply a this reference for " + parameter);
                        }
                        accessMappings.put(parameter.getOffset(), AccessMapping.ForMethodArgument.ofThisReference());
                    } else if (parameter.getDeclaredAnnotations().isAnnotationPresent(EntranceValue.class)) {
                        accessMappings.put(parameter.getOffset(), new AccessMapping.ForEntranceValue(instrumentedMethod));
                    } else if (parameter.getDeclaredAnnotations().isAnnotationPresent(ReturnValue.class)) {
                        accessMappings.put(parameter.getOffset(), new AccessMapping.ForReturnValue(instrumentedMethod));
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
                return accessMappings;
            }

            @Override
            public boolean isSkipException() {
                return !methodDescription.getDeclaredAnnotations().ofType(OnMethodExit.class).loadSilent().onException();
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
                public Dispatcher asExitFrom(Dispatcher dispatcher) {
                    throw new IllegalStateException();
                }

                @Override
                public int getValueOffset() {
                    return suppliedType.getStackSize().getSize();
                }

                @Override
                public Map<Integer, AccessMapping> toAccessMapping(MethodDescription instrumentedMethod) {
                    return delegator.toAccessMapping(instrumentedMethod);
                }

                @Override
                public boolean isSkipException() {
                    return delegator.isSkipException();
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

        class ForEntranceValue implements AccessMapping {

            private final MethodDescription instrumentedMethod;

            public ForEntranceValue(MethodDescription instrumentedMethod) {
                this.instrumentedMethod = instrumentedMethod;
            }

            @Override
            public void apply(MethodVisitor methodVisitor, int opcode) {
                methodVisitor.visitVarInsn(opcode, instrumentedMethod.getStackSize() + instrumentedMethod.getReturnType().getStackSize().getSize());
            }
        }

        class ForReturnValue implements AccessMapping {

            private final MethodDescription instrumentedMethod;


            public ForReturnValue(MethodDescription instrumentedMethod) {
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

         boolean onException() default true;
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
    public @interface EntranceValue {
        /* empty */
    }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public @interface ReturnValue {
        /* empty */
    }
}
