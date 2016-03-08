package net.bytebuddy.asm;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.implementation.bytecode.StackSize;
import org.objectweb.asm.*;

import java.io.IOException;
import java.lang.annotation.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Advice implements AsmVisitorWrapper.ForDeclaredMethods.MethodVisitorWrapper {

    private final Dispatcher.Resolved.ForMethodEnter methodEnter;

    private final Dispatcher.Resolved.ForMethodExit methodExit;

    private final byte[] binaryRepresentation;

    protected Advice(Dispatcher.Resolved.ForMethodEnter methodEnter, Dispatcher.Resolved.ForMethodExit methodExit, byte[] binaryRepresentation) {
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
            if (!methodEnter.isAlive() && !methodExit.isAlive()) {
                throw new IllegalArgumentException("No advice defined by " + typeDescription);
            }
            Dispatcher.Resolved.ForMethodEnter resolved = methodEnter.asMethodEnter();
            return new Advice(methodEnter.asMethodEnter(), methodExit.asMethodExitTo(resolved), classFileLocator.locate(typeDescription.getName()).resolve());
        } catch (IOException exception) {
            throw new IllegalStateException("Error reading class file of " + typeDescription, exception);
        }
    }

    private static Dispatcher resolve(Class<? extends Annotation> annotation, Dispatcher dispatcher, MethodDescription.InDefinedShape methodDescription) {
        if (methodDescription.getDeclaredAnnotations().isAnnotationPresent(annotation)) {
            if (dispatcher.isAlive()) {
                throw new IllegalStateException("Duplicate advice for " + dispatcher + " and " + methodDescription);
            } else if (!methodDescription.isStatic()) {
                throw new IllegalStateException("Advice for " + methodDescription + " is not static");
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

        private final MethodDescription.InDefinedShape instrumentedMethod;

        private final ClassReader classReader;

        protected AsmAdvice(MethodVisitor methodVisitor, MethodDescription.InDefinedShape instrumentedMethod) {
            super(Opcodes.ASM5, methodVisitor);
            this.instrumentedMethod = instrumentedMethod;
            classReader = new ClassReader(binaryRepresentation);
        }

        @Override
        public void visitCode() {
            super.visitCode();
            onMethodEntry();
        }

        protected void onMethodEntry() {
            classReader.accept(new CodeCopier(methodEnter), ClassReader.SKIP_DEBUG);
        }

        @Override
        public void visitVarInsn(int opcode, int offset) {
            super.visitVarInsn(opcode, offset < instrumentedMethod.getStackSize()
                    ? offset
                    : offset + methodEnter.getEnterType().getStackSize().getSize());
        }

        @Override
        public void visitInsn(int opcode) {
            switch (opcode) {
                case Opcodes.RETURN:
                    onMethodExit();
                    break;
                case Opcodes.IRETURN:
                    onMethodExit(Opcodes.ISTORE, Opcodes.ILOAD);
                    break;
                case Opcodes.FRETURN:
                    onMethodExit(Opcodes.FSTORE, Opcodes.FLOAD);
                    break;
                case Opcodes.DRETURN:
                    onMethodExit(Opcodes.DSTORE, Opcodes.DLOAD);
                    break;
                case Opcodes.LRETURN:
                    onMethodExit(Opcodes.LSTORE, Opcodes.LLOAD);
                    break;
                case Opcodes.ATHROW:
                    if (methodExit.isSkipException()) {
                        break;
                    } else if (instrumentedMethod.getReturnType().represents(boolean.class)
                            || instrumentedMethod.getReturnType().represents(byte.class)
                            || instrumentedMethod.getReturnType().represents(short.class)
                            || instrumentedMethod.getReturnType().represents(char.class)
                            || instrumentedMethod.getReturnType().represents(int.class)) {
                        mv.visitInsn(Opcodes.ICONST_0);
                        topValue(Opcodes.ISTORE);
                    } else if (instrumentedMethod.getReturnType().represents(long.class)) {
                        mv.visitInsn(Opcodes.LCONST_0);
                        topValue(Opcodes.LSTORE);
                    } else if (instrumentedMethod.getReturnType().represents(float.class)) {
                        mv.visitInsn(Opcodes.FCONST_0);
                        topValue(Opcodes.FSTORE);
                    } else if (instrumentedMethod.getReturnType().represents(double.class)) {
                        mv.visitInsn(Opcodes.DCONST_0);
                        topValue(Opcodes.DSTORE);
                    } else if (!instrumentedMethod.getReturnType().represents(void.class)) {
                        mv.visitInsn(Opcodes.ACONST_NULL);
                        topValue(Opcodes.ASTORE);
                    }
                    topValue(Opcodes.ASTORE, instrumentedMethod.getReturnType().getStackSize().getSize());
                    onMethodExit();
                    topValue(Opcodes.ALOAD, instrumentedMethod.getReturnType().getStackSize().getSize());
                    break;
                case Opcodes.ARETURN:
                    onMethodExit(Opcodes.ASTORE, Opcodes.ALOAD);
                    break;
            }
            mv.visitInsn(opcode);
        }

        private void onMethodExit(int store, int load) {
            topValue(store);
            mv.visitInsn(Opcodes.ACONST_NULL);
            topValue(Opcodes.ASTORE, instrumentedMethod.getReturnType().getStackSize().getSize());
            onMethodExit();
            topValue(load);
        }

        private void topValue(int opcode) {
            topValue(opcode, 0);
        }

        private void topValue(int opcode, int offset) {
            mv.visitVarInsn(opcode, instrumentedMethod.getStackSize() + methodEnter.getEnterType().getStackSize().getSize() + offset);
        }

        protected void onMethodExit() {
            classReader.accept(new CodeCopier(methodExit), ClassReader.SKIP_DEBUG);
        }

        protected class CodeCopier extends ClassVisitor {

            private final Dispatcher.Resolved dispatcher;

            protected CodeCopier(Dispatcher.Resolved dispatcher) {
                super(Opcodes.ASM5);
                this.dispatcher = dispatcher;
            }

            @Override
            public MethodVisitor visitMethod(int modifiers, String internalName, String descriptor, String signature, String[] exception) {
                return dispatcher.apply(internalName, descriptor, AsmAdvice.this.mv, instrumentedMethod);
            }
        }
    }

    protected interface Dispatcher {

        MethodVisitor IGNORE_METHOD = null;

        boolean isAlive();

        Resolved.ForMethodEnter asMethodEnter();

        Resolved.ForMethodExit asMethodExitTo(Resolved.ForMethodEnter dispatcher);

        interface Resolved {

            MethodVisitor apply(String internalName, String descriptor, MethodVisitor methodVisitor, MethodDescription.InDefinedShape instrumentedMethod);

            interface ForMethodEnter extends Resolved {

                TypeDescription getEnterType();
            }

            interface ForMethodExit extends Resolved {

                boolean isSkipException();
            }
        }

        interface OffsetMapping {

            int resolve(MethodDescription.InDefinedShape instrumentedMethod, StackSize offset);

            interface Factory {

                OffsetMapping UNDEFINED = null;

                OffsetMapping make(ParameterDescription.InDefinedShape parameterDescription);
            }

            class ForParameter implements OffsetMapping {

                private final int index;

                private final TypeDescription targetType;

                protected ForParameter(int index, TypeDescription targetType) {
                    this.index = index;
                    this.targetType = targetType;
                }

                @Override
                public int resolve(MethodDescription.InDefinedShape instrumentedMethod, StackSize offset) {
                    ParameterList<?> parameters = instrumentedMethod.getParameters();
                    if (parameters.size() <= index) {
                        throw new IllegalStateException(instrumentedMethod + " does not define an index " + index);
                    } else if (!parameters.get(index).getType().asErasure().isAssignableTo(targetType)) {
                        throw new IllegalStateException("Cannot assign " + parameters.get(index) + " to " + targetType);
                    }
                    return parameters.get(index).getOffset();
                }

                protected enum Factory implements OffsetMapping.Factory {

                    INSTANCE;

                    @Override
                    public OffsetMapping make(ParameterDescription.InDefinedShape parameterDescription) {
                        AnnotationDescription.Loadable<Argument> argument = parameterDescription.getDeclaredAnnotations().ofType(Argument.class);
                        return argument == null
                                ? UNDEFINED
                                : new ForParameter(argument.loadSilent().value(), parameterDescription.getType().asErasure());
                    }
                }
            }

            class ForThisReference implements OffsetMapping {

                private final TypeDescription targetType;

                public ForThisReference(TypeDescription targetType) {
                    this.targetType = targetType;
                }

                @Override
                public int resolve(MethodDescription.InDefinedShape instrumentedMethod, StackSize offset) {
                    if (instrumentedMethod.isStatic()) {
                        throw new IllegalStateException("Cannot map this reference for static method " + instrumentedMethod);
                    } else if (!instrumentedMethod.getDeclaringType().isAssignableTo(targetType)) {
                        throw new IllegalStateException("Cannot assign this reference of " + instrumentedMethod + " to " + targetType);
                    }
                    return 0;
                }

                protected enum Factory implements OffsetMapping.Factory {

                    INSTANCE;

                    @Override
                    public OffsetMapping make(ParameterDescription.InDefinedShape parameterDescription) {
                        return parameterDescription.getDeclaredAnnotations().isAnnotationPresent(This.class)
                                ? new ForThisReference(parameterDescription.getType().asErasure())
                                : UNDEFINED;
                    }
                }
            }

            enum ForEnterValue implements OffsetMapping {

                INSTANCE;

                @Override
                public int resolve(MethodDescription.InDefinedShape instrumentedMethod, StackSize offset) {
                    return instrumentedMethod.getStackSize();
                }

                protected static class Factory implements OffsetMapping.Factory {

                    private final TypeDescription enterType;

                    protected Factory(TypeDescription enterType) {
                        this.enterType = enterType;
                    }

                    @Override
                    public OffsetMapping make(ParameterDescription.InDefinedShape parameterDescription) {
                        if (parameterDescription.getDeclaredAnnotations().isAnnotationPresent(Enter.class)) {
                            if (!enterType.isAssignableTo(parameterDescription.getType().asErasure())) {
                                throw new IllegalStateException("Cannot assign " + parameterDescription + " to supplied type " + enterType);
                            }
                            return ForEnterValue.INSTANCE;
                        } else {
                            return UNDEFINED;
                        }
                    }
                }
            }

            class ForReturnValue implements OffsetMapping {

                private final TypeDescription targetType;

                protected ForReturnValue(TypeDescription targetType) {
                    this.targetType = targetType;
                }

                @Override
                public int resolve(MethodDescription.InDefinedShape instrumentedMethod, StackSize offset) {
                    if (!instrumentedMethod.getReturnType().asErasure().isAssignableTo(targetType)) {
                        throw new IllegalStateException("Cannot assign return type of " + instrumentedMethod + " to " + targetType);
                    }
                    return instrumentedMethod.getStackSize() + offset.getSize();
                }

                protected enum Factory implements OffsetMapping.Factory {

                    INSTANCE;

                    @Override
                    public OffsetMapping make(ParameterDescription.InDefinedShape parameterDescription) {
                        return parameterDescription.getDeclaredAnnotations().isAnnotationPresent(Return.class)
                                ? new ForReturnValue(parameterDescription.getType().asErasure())
                                : UNDEFINED;
                    }
                }
            }

            enum ForException implements OffsetMapping, Factory {

                INSTANCE;

                @Override
                public OffsetMapping make(ParameterDescription.InDefinedShape parameterDescription) {
                    if (parameterDescription.getDeclaredAnnotations().isAnnotationPresent(Thrown.class)) {
                        if (!parameterDescription.getType().asErasure().isAssignableFrom(Throwable.class)) {
                            throw new IllegalStateException("Parameter type must be a supertype of Throwable for " + parameterDescription);
                        }
                        return this;
                    } else {
                        return UNDEFINED;
                    }
                }

                @Override
                public int resolve(MethodDescription.InDefinedShape instrumentedMethod, StackSize offset) {
                    return instrumentedMethod.getStackSize() + offset.getSize() + instrumentedMethod.getReturnType().getStackSize().getSize();
                }
            }

            class Illegal implements Factory {

                private final List<? extends Class<? extends Annotation>> annotations;

                //@SafeVarargs
                protected Illegal(Class<? extends Annotation>... annotation) {
                    this(Arrays.asList(annotation));
                }

                protected Illegal(List<? extends Class<? extends Annotation>> annotations) {
                    this.annotations = annotations;
                }

                @Override
                public OffsetMapping make(ParameterDescription.InDefinedShape parameterDescription) {
                    for (Class<? extends Annotation> annotation : annotations) {
                        if (parameterDescription.getDeclaredAnnotations().isAnnotationPresent(annotation)) {
                            throw new IllegalStateException("Illegal annotation " + annotation + " for " + parameterDescription);
                        }
                    }
                    return UNDEFINED;
                }
            }
        }

        enum Inactive implements Dispatcher, Resolved.ForMethodEnter, Resolved.ForMethodExit {

            INSTANCE;

            @Override
            public boolean isAlive() {
                return false;
            }

            @Override
            public boolean isSkipException() {
                return true;
            }

            @Override
            public TypeDescription getEnterType() {
                return TypeDescription.VOID;
            }

            @Override
            public Resolved.ForMethodEnter asMethodEnter() {
                return this;
            }

            @Override
            public Resolved.ForMethodExit asMethodExitTo(Resolved.ForMethodEnter dispatcher) {
                return this;
            }

            @Override
            public MethodVisitor apply(String internalName, String descriptor, MethodVisitor methodVisitor, MethodDescription.InDefinedShape instrumentedMethod) {
                return IGNORE_METHOD;
            }
        }

        class ForMethod implements Dispatcher {

            protected final MethodDescription.InDefinedShape inlinedMethod;

            protected ForMethod(MethodDescription.InDefinedShape inlinedMethod) {
                this.inlinedMethod = inlinedMethod;
            }

            @Override
            public boolean isAlive() {
                return true;
            }

            @Override
            public Dispatcher.Resolved.ForMethodEnter asMethodEnter() {
                return new ForMethodEnter(inlinedMethod);
            }

            @Override
            public Dispatcher.Resolved.ForMethodExit asMethodExitTo(Dispatcher.Resolved.ForMethodEnter dispatcher) {
                return new ForMethodExit(inlinedMethod, dispatcher.getEnterType());
            }

            protected abstract static class Resolved implements Dispatcher.Resolved {

                protected final MethodDescription.InDefinedShape inlinedMethod;

                protected final Map<Integer, OffsetMapping> offsetMappings;

                protected Resolved(MethodDescription.InDefinedShape inlinedMethod, OffsetMapping.Factory... factory) {
                    this.inlinedMethod = inlinedMethod;
                    offsetMappings = new HashMap<Integer, OffsetMapping>();
                    for (ParameterDescription.InDefinedShape parameterDescription : inlinedMethod.getParameters()) {
                        OffsetMapping offsetMapping = OffsetMapping.Factory.UNDEFINED;
                        for (OffsetMapping.Factory aFactory : factory) {
                            OffsetMapping possible = aFactory.make(parameterDescription);
                            if (possible != null) {
                                if (offsetMapping == null) {
                                    offsetMapping = possible;
                                } else {
                                    throw new IllegalStateException(parameterDescription + " is bound to both " + possible + " and " + offsetMapping);
                                }
                            }
                        }
                        offsetMappings.put(parameterDescription.getOffset(), offsetMapping == null
                                ? new OffsetMapping.ForParameter(parameterDescription.getIndex(), parameterDescription.getType().asErasure())
                                : offsetMapping);
                    }
                }

                @Override
                public MethodVisitor apply(String internalName, String descriptor, MethodVisitor methodVisitor, MethodDescription.InDefinedShape instrumentedMethod) {
                    return inlinedMethod.getInternalName().equals(internalName) && inlinedMethod.getDescriptor().equals(descriptor)
                            ? inline(methodVisitor, instrumentedMethod)
                            : IGNORE_METHOD;
                }

                protected abstract MethodVisitor inline(MethodVisitor methodVisitor, MethodDescription.InDefinedShape instrumentedMethod);
            }

            protected static class ForMethodEnter extends Resolved implements Dispatcher.Resolved.ForMethodEnter {

                @SuppressWarnings("all") // In absence of @SafeVarargs for Java 6
                protected ForMethodEnter(MethodDescription.InDefinedShape inlinedMethod) {
                    super(inlinedMethod,
                            OffsetMapping.ForParameter.Factory.INSTANCE,
                            OffsetMapping.ForThisReference.Factory.INSTANCE,
                            new OffsetMapping.Illegal(Thrown.class, Enter.class, Return.class));
                }

                @Override
                public TypeDescription getEnterType() {
                    return inlinedMethod.getReturnType().asErasure();
                }

                @Override
                protected MethodVisitor inline(MethodVisitor methodVisitor, MethodDescription.InDefinedShape instrumentedMethod) {
                    Map<Integer, Integer> offsetMappings = new HashMap<Integer, Integer>();
                    for (Map.Entry<Integer, OffsetMapping> entry : this.offsetMappings.entrySet()) {
                        offsetMappings.put(entry.getKey(), entry.getValue().resolve(instrumentedMethod, StackSize.ZERO));
                    }
                    return new CodeTranslationVisitor.ReturnValueRetaining(methodVisitor, instrumentedMethod, inlinedMethod, offsetMappings);
                }
            }

            protected static class ForMethodExit extends Resolved implements Dispatcher.Resolved.ForMethodExit {

                private final StackSize additionalSize;

                @SuppressWarnings("all") // In absence of @SafeVarargs for Java 6
                protected ForMethodExit(MethodDescription.InDefinedShape inlinedMethod, TypeDescription enterType) {
                    super(inlinedMethod,
                            OffsetMapping.ForParameter.Factory.INSTANCE,
                            OffsetMapping.ForThisReference.Factory.INSTANCE,
                            new OffsetMapping.ForEnterValue.Factory(enterType),
                            OffsetMapping.ForReturnValue.Factory.INSTANCE,
                            inlinedMethod.getDeclaredAnnotations().ofType(OnMethodExit.class).loadSilent().onThrowable()
                                    ? OffsetMapping.ForException.INSTANCE
                                    : new OffsetMapping.Illegal(Thrown.class));
                    additionalSize = enterType.getStackSize();
                }

                @Override
                public boolean isSkipException() {
                    return !inlinedMethod.getDeclaredAnnotations().ofType(OnMethodExit.class).loadSilent().onThrowable();
                }

                @Override
                protected MethodVisitor inline(MethodVisitor methodVisitor, MethodDescription.InDefinedShape instrumentedMethod) {
                    Map<Integer, Integer> offsetMappings = new HashMap<Integer, Integer>();
                    for (Map.Entry<Integer, OffsetMapping> entry : this.offsetMappings.entrySet()) {
                        offsetMappings.put(entry.getKey(), entry.getValue().resolve(instrumentedMethod, additionalSize));
                    }
                    return new CodeTranslationVisitor.ReturnValueDiscarding(methodVisitor, instrumentedMethod, inlinedMethod, offsetMappings, additionalSize);
                }
            }

            protected abstract static class CodeTranslationVisitor extends MethodVisitor {

                private static final AnnotationVisitor IGNORE_ANNOTATION = null;

                protected final MethodDescription.InDefinedShape instrumentedMethod;

                protected final MethodDescription.InDefinedShape inlinedMethod;

                private final Map<Integer, Integer> offsetMappings;

                protected final Label endOfMethod;

                protected CodeTranslationVisitor(MethodVisitor methodVisitor,
                                                 MethodDescription.InDefinedShape instrumentedMethod,
                                                 MethodDescription.InDefinedShape inlinedMethod,
                                                 Map<Integer, Integer> offsetMappings) {
                    super(Opcodes.ASM5, methodVisitor);
                    this.instrumentedMethod = instrumentedMethod;
                    this.inlinedMethod = inlinedMethod;
                    this.offsetMappings = offsetMappings;
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
                public AnnotationVisitor visitParameterAnnotation(int index, String descriptor, boolean visible) {
                    return IGNORE_ANNOTATION;
                }

                @Override
                public void visitAttribute(Attribute attr) {
                    /* do nothing */
                }

                @Override
                public void visitCode() {
                    /* do nothing */
                }

                @Override
                public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
                    /* do nothing */
                }

                @Override
                public void visitLineNumber(int line, Label start) {
                    /* do nothing */
                }

                @Override
                public void visitEnd() {
                    mv.visitLabel(endOfMethod);
                }

                @Override
                public void visitMaxs(int maxStack, int maxLocals) {
                    /* do nothing */
                }

                @Override
                public void visitVarInsn(int opcode, int offset) {
                    Integer mapped = offsetMappings.get(offset);
                    mv.visitVarInsn(opcode, mapped == null
                            ? adjust(offset + instrumentedMethod.getStackSize() - inlinedMethod.getStackSize())
                            : mapped);
                }

                protected abstract int adjust(int offset);

                @Override
                public abstract void visitInsn(int opcode);

                protected static class ReturnValueRetaining extends CodeTranslationVisitor {

                    protected ReturnValueRetaining(MethodVisitor methodVisitor,
                                                   MethodDescription.InDefinedShape instrumentedMethod,
                                                   MethodDescription.InDefinedShape inlinedMethod,
                                                   Map<Integer, Integer> offsetTranslations) {
                        super(methodVisitor, instrumentedMethod, inlinedMethod, offsetTranslations);
                    }

                    @Override
                    public void visitInsn(int opcode) {
                        switch (opcode) {
                            case Opcodes.RETURN:
                                break;
                            case Opcodes.IRETURN:
                                mv.visitVarInsn(Opcodes.ISTORE, instrumentedMethod.getStackSize());
                                break;
                            case Opcodes.LRETURN:
                                mv.visitVarInsn(Opcodes.LSTORE, instrumentedMethod.getStackSize());
                                break;
                            case Opcodes.ARETURN:
                                mv.visitVarInsn(Opcodes.ASTORE, instrumentedMethod.getStackSize());
                                break;
                            case Opcodes.FRETURN:
                                mv.visitVarInsn(Opcodes.FSTORE, instrumentedMethod.getStackSize());
                                break;
                            case Opcodes.DRETURN:
                                mv.visitVarInsn(Opcodes.DSTORE, instrumentedMethod.getStackSize());
                                break;
                            default:
                                mv.visitInsn(opcode);
                                return;
                        }
                        mv.visitJumpInsn(Opcodes.GOTO, endOfMethod);
                    }

                    @Override
                    protected int adjust(int offset) {
                        return offset;
                    }
                }

                protected static class ReturnValueDiscarding extends CodeTranslationVisitor {

                    private final StackSize additionalSize;

                    protected ReturnValueDiscarding(MethodVisitor methodVisitor,
                                                    MethodDescription.InDefinedShape instrumentedMethod,
                                                    MethodDescription.InDefinedShape inlinedMethod,
                                                    Map<Integer, Integer> offsetMappings,
                                                    StackSize additionalSize) {
                        super(methodVisitor, instrumentedMethod, inlinedMethod, offsetMappings);
                        this.additionalSize = additionalSize;
                    }

                    @Override
                    public void visitInsn(int opcode) {
                        switch (opcode) {
                            case Opcodes.RETURN:
                                break;
                            case Opcodes.IRETURN:
                            case Opcodes.ARETURN:
                            case Opcodes.FRETURN:
                                mv.visitInsn(Opcodes.POP);
                                break;
                            case Opcodes.LRETURN:
                            case Opcodes.DRETURN:
                                mv.visitInsn(Opcodes.POP2);
                                break;
                            default:
                                mv.visitInsn(opcode);
                                return;
                        }
                        mv.visitJumpInsn(Opcodes.GOTO, endOfMethod);
                    }

                    @Override
                    protected int adjust(int offset) {
                        return offset + instrumentedMethod.getReturnType().getStackSize().getSize() + additionalSize.getSize() + 1;
                    }
                }
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

        boolean onThrowable() default true;
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
    public @interface Enter {
        /* empty */
    }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public @interface Return {
        /* empty */
    }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public @interface Thrown {
        /* empty */
    }
}
