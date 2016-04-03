package net.bytebuddy.asm;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.scaffold.FieldLocator;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.matcher.ElementMatcher;
import org.objectweb.asm.*;

import java.io.IOException;
import java.lang.annotation.*;
import java.util.*;

import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * <p>
 * Advice wrappers copy the code of blueprint methods to be executed before and/or after a matched method. To achieve this, a {@code static}
 * method of a class is annotated by {@link OnMethodEnter} and/or {@link OnMethodExit} and provided to an instance of this class.
 * </p>
 * <p>
 * A method that is annotated with {@link OnMethodEnter} can annotate its parameters with {@link Argument} where field access to this parameter
 * is substituted with access to the specified argument of the instrumented method. Alternatively, a parameter can be annotated by {@link This}
 * where the {@code this} reference of the instrumented method is read when the parameter is accessed. This mechanism can also be used to assign a
 * new value to the {@code this} reference of an instrumented method. If no annotation is used on a parameter, it is assigned the {@code n}-th
 * parameter of the instrumented method for the {@code n}-th parameter of the advice method. All parameters must declare the exact same type as
 * the parameters of the instrumented type or the method's declaring type for the {@link This} reference respectively if they are not marked as
 * <i>read-only</i>. In the latter case, it suffices that a parameter type is a super type of the corresponding type of the instrumented method.
 * </p>
 * <p>
 * A method that is annotated with {@link OnMethodExit} can equally annotate its parameters with {@link Argument} and {@link This}. Additionally,
 * it can annotate a parameter with {@link Return} to receive the original method's return value. By reassigning the return value, it is possible
 * to replace the returned value. If an instrumented method does not return a value, this annotation must not be used. If a method returns
 * exceptionally, the parameter is set to its default value, i.e. to {@code 0} for primitive types and to {@code null} for reference types. The
 * parameter's type must be equal to the instrumented method's return type if it is not set to <i>read-only</i> where it suffices to declare the
 * parameter type to be of any super type to the instrumented method's return type. An exception can be read by annotating a parameter of type
 * {@link Throwable} annotated with {@link Thrown} which is assigned the thrown {@link Throwable} or {@code null} if a method returns normally.
 * Doing so, it is possible to exchange a thrown exception with any checked or unchecked exception.Finally, if a method annotated with
 * {@link OnMethodEnter} exists and this method returns a value, this value can be accessed by a parameter annotated with {@link Enter}.
 * This parameter must declare the same type as type being returned by the method annotated with {@link OnMethodEnter}. If the parameter is marked
 * to be <i>read-only</i>, it suffices that the annotated parameter is of a super type of the return type of the method annotated by
 * {@link OnMethodEnter}. If no such method exists or this method returns {@code void}, no such parameter must be declared. Any return value
 * of a method that is annotated by {@link OnMethodExit} is discarded.
 * </p>
 * <p>
 * If any advice method throws an exception, the method is terminated prematurely. If the method annotated by {@link OnMethodEnter} throws an exception,
 * the method annotated by {@link OnMethodExit} method is not invoked. If the instrumented method throws an exception, the method that is annotated by
 * {@link OnMethodExit} is only invoked if the {@link OnMethodExit#onThrowable()} property is set to {@code true} what is the default. If this property
 * is set to {@code false}, the {@link Thrown} annotation must not be used on any parameter.
 * </p>
 * <p>
 * Byte Buddy does not assert the visibility of any types that are referenced within the advice methods. It is the responsibility of the user of this
 * class to assure that all types referenced within the advice methods are visible to the instrumented class. Failing to do so results in a
 * {@link IllegalAccessError} at the instrumented class's runtime.
 * </p>
 * <p>
 * <b>Important</b>: Since Java 6, class files contain <i>stack map frames</i> embedded into a method's byte code. When advice methods are compiled
 * with a class file version less then Java 6 but are used for a class file that was compiled to Java 6 or newer, these stack map frames must be
 * computed by ASM by using the {@link ClassWriter#COMPUTE_FRAMES} option. If the advice methods do not contain any branching instructions, this is
 * not required. No action is required if the advice methods are at least compiled with Java 6 but are used on classes older than Java 6.
 * </p>
 */
public class Advice implements AsmVisitorWrapper.ForDeclaredMethods.MethodVisitorWrapper {

    /**
     * The dispatcher for instrumenting the instrumented method upon entering.
     */
    private final Dispatcher.Resolved.ForMethodEnter methodEnter;

    /**
     * The dispatcher for instrumenting the instrumented method upon exiting.
     */
    private final Dispatcher.Resolved.ForMethodExit methodExit;

    /**
     * The binary representation of the class containing the advice methods.
     */
    private final byte[] binaryRepresentation;

    /**
     * Creates a new advice.
     *
     * @param methodEnter          The dispatcher for instrumenting the instrumented method upon entering.
     * @param methodExit           The dispatcher for instrumenting the instrumented method upon exiting.
     * @param binaryRepresentation The binary representation of the class containing the advice methods.
     */
    protected Advice(Dispatcher.Resolved.ForMethodEnter methodEnter, Dispatcher.Resolved.ForMethodExit methodExit, byte[] binaryRepresentation) {
        this.methodEnter = methodEnter;
        this.methodExit = methodExit;
        this.binaryRepresentation = binaryRepresentation;
    }

    /**
     * Implements advice where every matched method is advised by the given type's advisory methods. The advises binary representation is
     * accessed by querying the class loader of the supplied class for a class file.
     *
     * @param type The type declaring the advice.
     * @return A method visitor wrapper representing the supplied advice.
     */
    public static Advice to(Class<?> type) {
        return to(type, ClassFileLocator.ForClassLoader.of(type.getClassLoader()));
    }

    /**
     * Implements advice where every matched method is advised by the given type's advisory methods.
     *
     * @param type             The type declaring the advice.
     * @param classFileLocator The class file locator for locating the advisory class's class file.
     * @return A method visitor wrapper representing the supplied advice.
     */
    public static Advice to(Class<?> type, ClassFileLocator classFileLocator) {
        return to(new TypeDescription.ForLoadedType(type), classFileLocator);
    }

    /**
     * Implements advice where every matched method is advised by the given type's advisory methods.
     *
     * @param typeDescription  A description of the type declaring the advice.
     * @param classFileLocator The class file locator for locating the advisory class's class file.
     * @return A method visitor wrapper representing the supplied advice.
     */
    public static Advice to(TypeDescription typeDescription, ClassFileLocator classFileLocator) {
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

    /**
     * Checks if a given method represents advise and does some basic validation.
     *
     * @param annotation        The annotation that indicates a given type of advise.
     * @param dispatcher        Any previous dispatcher.
     * @param methodDescription A description of the method considered as advise.
     * @return A dispatcher for the given method or the supplied dispatcher if the given method is not intended to be used as advise.
     */
    private static Dispatcher resolve(Class<? extends Annotation> annotation, Dispatcher dispatcher, MethodDescription.InDefinedShape methodDescription) {
        if (methodDescription.getDeclaredAnnotations().isAnnotationPresent(annotation)) {
            if (dispatcher.isAlive()) {
                throw new IllegalStateException("Duplicate advice for " + dispatcher + " and " + methodDescription);
            } else if (!methodDescription.isStatic()) {
                throw new IllegalStateException("Advice for " + methodDescription + " is not static");
            }
            return new Dispatcher.Active(methodDescription);
        } else {
            return dispatcher;
        }
    }

    /**
     * Returns an ASM visitor wrapper that matches the given matcher and applies this advice to the matched methods.
     *
     * @param matcher The matcher identifying methods to apply the advice to.
     * @return A suitable ASM visitor wrapper with the <i>compute frames</i> option enabled.
     */
    public AsmVisitorWrapper.ForDeclaredMethods on(ElementMatcher<? super MethodDescription.InDefinedShape> matcher) {
        return new AsmVisitorWrapper.ForDeclaredMethods().method(matcher, this);
    }

    @Override
    public MethodVisitor wrap(TypeDescription instrumentedType, MethodDescription.InDefinedShape methodDescription, MethodVisitor methodVisitor) {
        if (methodDescription.isAbstract() || methodDescription.isNative()) {
            throw new IllegalStateException("Cannot advice abstract or native method " + methodDescription);
        }
        return methodExit.isSkipThrowable()
                ? new AdviceVisitor.WithoutExceptionHandling(methodVisitor, methodDescription, methodEnter, methodExit, binaryRepresentation)
                : new AdviceVisitor.WithExceptionHandling(methodVisitor, methodDescription, methodEnter, methodExit, binaryRepresentation);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        Advice advice = (Advice) other;
        return methodEnter.equals(advice.methodEnter)
                && methodExit.equals(advice.methodExit)
                && Arrays.equals(binaryRepresentation, advice.binaryRepresentation);
    }

    @Override
    public int hashCode() {
        int result = methodEnter.hashCode();
        result = 31 * result + methodExit.hashCode();
        result = 31 * result + Arrays.hashCode(binaryRepresentation);
        return result;
    }

    @Override
    public String toString() {
        return "Advice{" +
                "methodEnter=" + methodEnter +
                ", methodExit=" + methodExit +
                ", binaryRepresentation=<" + binaryRepresentation.length + " bytes>" +
                '}';
    }

    /**
     * A translator for frame found in parsed code.
     */
    protected static class FrameTranslator {

        /**
         * An empty array indicating an empty frame.
         */
        private static final Object[] EMPTY = new Object[0];

        /**
         * The instrumented method.
         */
        private final MethodDescription.InDefinedShape instrumentedMethod;

        /**
         * A list of intermediate types to be considered as part of the instrumented method's steady signature.
         */
        private final TypeList intermediateTypes;

        /**
         * The maximum stack size required by a visited advice method.
         */
        private int stackSize;

        /**
         * The maximum length of the local variable array required by a visited advice method.
         */
        private int localVariableLength;

        /**
         * Creates a new frame translator.
         *
         * @param instrumentedMethod The instrumented method.
         * @param intermediateTypes  A list of intermediate types to be considered as part of the instrumented method's steady signature.
         */
        protected FrameTranslator(MethodDescription.InDefinedShape instrumentedMethod, TypeList intermediateTypes) {
            this.instrumentedMethod = instrumentedMethod;
            this.intermediateTypes = intermediateTypes;
            stackSize = 1; // Minimum for pushing exceptions onto the stack.
        }

        /**
         * Translates a type into a representation of its form inside a stack map frame.
         *
         * @param typeDescription The type to translate.
         * @return A stack entry representation of the supplied type.
         */
        private static Object toFrame(TypeDescription typeDescription) {
            if (typeDescription.represents(boolean.class)
                    || typeDescription.represents(byte.class)
                    || typeDescription.represents(short.class)
                    || typeDescription.represents(char.class)
                    || typeDescription.represents(int.class)) {
                return Opcodes.INTEGER;
            } else if (typeDescription.represents(long.class)) {
                return Opcodes.LONG;
            } else if (typeDescription.represents(float.class)) {
                return Opcodes.FLOAT;
            } else if (typeDescription.represents(double.class)) {
                return Opcodes.DOUBLE;
            } else {
                return typeDescription.getInternalName();
            }
        }

        /**
         * Binds this frame translator to an advice method.
         *
         * @param methodDescription The advice method.
         * @return A bound version of this frame translator.
         */
        protected Bound bindEntry(MethodDescription.InDefinedShape methodDescription) {
            return new Bound(methodDescription, new TypeList.Empty(), methodDescription.getReturnType().represents(void.class)
                    ? new TypeList.Empty()
                    : new TypeList.Explicit(methodDescription.getReturnType().asErasure()));
        }

        /**
         * Binds this frame translator to an advice method.
         *
         * @param methodDescription The advice method.
         * @param enterType         The type that is returned by the enter method, if any.
         * @return A bound version of this frame translator.
         */
        protected Bound bindExit(MethodDescription.InDefinedShape methodDescription, TypeDescription enterType) {
            List<TypeDescription> typeDescriptions = new ArrayList<TypeDescription>(3);
            if (!enterType.represents(void.class)) {
                typeDescriptions.add(enterType);
            }
            if (!instrumentedMethod.getReturnType().represents(void.class)) {
                typeDescriptions.add(instrumentedMethod.getReturnType().asErasure());
            }
            typeDescriptions.add(TypeDescription.THROWABLE);
            return new Bound(methodDescription, new TypeList.Explicit(typeDescriptions), Collections.<TypeDescription>emptyList());
        }

        /**
         * Computes the compound stack size of the instrumented method.
         *
         * @param stackSize The stack size of the instrumented method without instrumentation.
         * @return The stack size including the instrumented advice.
         */
        protected int compoundStackSize(int stackSize) {
            return Math.max(this.stackSize, stackSize);
        }

        /**
         * Computes the compound local variable length of the instrumented method.
         *
         * @param localVariableLength The local variable length of the instrumented method without instrumentation.
         * @return The stack size including the instrumented advice.
         */
        protected int compoundLocalVariableSize(int localVariableLength) {
            return Math.max(this.localVariableLength, localVariableLength
                    + instrumentedMethod.getReturnType().getStackSize().getSize()
                    + StackSize.SINGLE.getSize()
                    + intermediateTypes.getStackSize());
        }

        /**
         * Injects a frame that describes the method when entering its surrounding exception handler.
         *
         * @param methodVisitor The method visitor to write the frame to.
         */
        protected void injectHandlerFrame(MethodVisitor methodVisitor) {
            methodVisitor.visitFrame(Opcodes.F_SAME1, 0, EMPTY, 1, new Object[]{Type.getInternalName(Throwable.class)});
        }

        /**
         * Injects a frame that describes the method after completion.
         *
         * @param methodVisitor The method visitor to write the frame to.
         */
        protected void injectCompletionFrame(MethodVisitor methodVisitor) {
            Object[] local = instrumentedMethod.getReturnType().represents(void.class)
                    ? new Object[]{Type.getInternalName(Throwable.class)}
                    : new Object[]{toFrame(instrumentedMethod.getReturnType().asErasure()), Type.getInternalName(Throwable.class)};
            methodVisitor.visitFrame(Opcodes.F_APPEND, local.length, local, 0, EMPTY);
        }

        /**
         * Translates an existing frame.
         *
         * @param methodVisitor       The method visitor to append the frame to.
         * @param type                The type of method frame.
         * @param localVariableLength The number of local variables of the original frame.
         * @param localVariable       An array containing the local variable types.
         * @param stackSize           The size of the current operand stack.
         * @param stack               An array containing the types on the operand stack.
         */
        protected void translateFrame(MethodVisitor methodVisitor,
                                      int type,
                                      int localVariableLength,
                                      Object[] localVariable,
                                      int stackSize,
                                      Object[] stack) {
            translateFrame(methodVisitor, instrumentedMethod, intermediateTypes, type, localVariableLength, localVariable, stackSize, stack);
        }

        /**
         * Translates an existing frame.
         *
         * @param methodVisitor       The method visitor to append the frame to.
         * @param methodDescription   The method for which this frame was originally written.
         * @param intermediateTypes   The intermediate types to be considered as part of the instrumented method's signature.
         * @param type                The type of method frame.
         * @param localVariableLength The number of local variables of the original frame.
         * @param localVariable       An array containing the local variable types.
         * @param stackSize           The size of the current operand stack.
         * @param stack               An array containing the types on the operand stack.
         */
        private void translateFrame(MethodVisitor methodVisitor,
                                    MethodDescription.InDefinedShape methodDescription,
                                    TypeList intermediateTypes,
                                    int type,
                                    int localVariableLength,
                                    Object[] localVariable,
                                    int stackSize,
                                    Object[] stack) {
            switch (type) {
                case Opcodes.F_SAME:
                case Opcodes.F_SAME1:
                case Opcodes.F_APPEND:
                case Opcodes.F_CHOP:
                    break;
                case Opcodes.F_FULL:
                case Opcodes.F_NEW:
                    Object[] translated = new Object[localVariableLength
                            - methodDescription.getParameters().size()
                            - (methodDescription.isStatic() ? 0 : 1)
                            + instrumentedMethod.getParameters().size()
                            + (instrumentedMethod.isStatic() ? 0 : 1)
                            + intermediateTypes.size()];
                    int index = 0;
                    if (!instrumentedMethod.isStatic()) {
                        translated[index++] = toFrame(instrumentedMethod.getDeclaringType());
                    }
                    for (TypeDescription typeDescription : instrumentedMethod.getParameters().asTypeList().asErasures()) {
                        translated[index++] = toFrame(typeDescription);
                    }
                    for (TypeDescription typeDescription : intermediateTypes) {
                        translated[index++] = toFrame(typeDescription);
                    }
                    System.arraycopy(localVariable,
                            methodDescription.getParameters().size() + (methodDescription.isStatic() ? 0 : 1),
                            translated,
                            index,
                            translated.length - index);
                    localVariableLength = translated.length;
                    localVariable = translated;
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected frame type: " + type);
            }
            methodVisitor.visitFrame(type, localVariableLength, localVariable, stackSize, stack);
        }

        @Override
        public String toString() {
            return "Advice.FrameTranslator{" +
                    "instrumentedMethod=" + instrumentedMethod +
                    ", intermediateTypes=" + intermediateTypes +
                    ", stackSize=" + stackSize +
                    ", localVariableLength=" + localVariableLength +
                    '}';
        }

        /**
         * A frame translator that is bound to an advice method.
         */
        protected class Bound {

            /**
             * The method description for which frames are translated.
             */
            private final MethodDescription.InDefinedShape methodDescription;

            /**
             * A list of intermediate types to be considered as part of the instrumented method's steady signature.
             */
            private final TypeList intermediateTypes;

            /**
             * The types that this method yields as a result.
             */
            private final List<? extends TypeDescription> yieldedTypes;

            /**
             * Creates a new bound frame translator.
             *
             * @param methodDescription The method description for which frames are translated.
             * @param intermediateTypes A list of intermediate types to be considered as part of the instrumented method's steady signature.
             * @param yieldedTypes      The types that this method yields as a result.
             */
            protected Bound(MethodDescription.InDefinedShape methodDescription,
                            TypeList intermediateTypes,
                            List<? extends TypeDescription> yieldedTypes) {
                this.methodDescription = methodDescription;
                this.intermediateTypes = intermediateTypes;
                this.yieldedTypes = yieldedTypes;
                if (yieldedTypes.size() > 3) {
                    throw new UnsupportedOperationException("Did not implement support for more then three yielded types: " + yieldedTypes);
                }
            }

            /**
             * Records the maximum stack size and the maximum length of a local variable array that were discovered in a class file.
             *
             * @param stackSize           The maximum discovered stack size.
             * @param localVariableLength The maximum length of the local variable array.
             */
            protected void recordMaxima(int stackSize, int localVariableLength) {
                FrameTranslator.this.stackSize = Math.max(FrameTranslator.this.stackSize, stackSize);
                FrameTranslator.this.localVariableLength = Math.max(FrameTranslator.this.localVariableLength, localVariableLength
                        - methodDescription.getStackSize()
                        + instrumentedMethod.getStackSize()
                        + intermediateTypes.getStackSize());
            }

            /**
             * Translates an existing frame.
             *
             * @param methodVisitor       The method visitor to append the frame to.
             * @param type                The type of method frame.
             * @param localVariableLength The number of local variables of the original frame.
             * @param localVariable       An array containing the local variable types.
             * @param stackSize           The size of the current operand stack.
             * @param stack               An array containing the types on the operand stack.
             */
            protected void translateFrame(MethodVisitor methodVisitor,
                                          int type,
                                          int localVariableLength,
                                          Object[] localVariable,
                                          int stackSize,
                                          Object[] stack) {
                FrameTranslator.this.translateFrame(methodVisitor,
                        methodDescription,
                        intermediateTypes,
                        type,
                        localVariableLength,
                        localVariable,
                        stackSize,
                        stack);
            }

            /**
             * Injects a frame that describes the method when entering its surrounding exception handler.
             *
             * @param methodVisitor The method visitor to write the frame to.
             */
            protected void injectHandlerFrame(MethodVisitor methodVisitor) {
                FrameTranslator.this.injectHandlerFrame(methodVisitor);
            }

            /**
             * Injects a frame that describes the method after completion.
             *
             * @param methodVisitor The method visitor to write the frame to.
             */
            protected void injectCompletionFrame(MethodVisitor methodVisitor) {
                Object[] local = new Object[yieldedTypes.size()];
                int index = 0;
                for (TypeDescription typeDescription : yieldedTypes) {
                    local[index++] = toFrame(typeDescription);
                }
                methodVisitor.visitFrame(Opcodes.F_APPEND, local.length, local, 0, EMPTY);
            }

            @Override
            public String toString() {
                return "Advice.FrameTranslator.Bound{" +
                        "frameTranslator=" + FrameTranslator.this +
                        ", methodDescription=" + methodDescription +
                        ", intermediateTypes=" + intermediateTypes +
                        ", yieldedTypes=" + yieldedTypes +
                        '}';
            }
        }
    }

    /**
     * A method visitor that weaves the advise methods' byte codes.
     */
    protected abstract static class AdviceVisitor extends MethodVisitor {

        /**
         * Indicates a zero offset.
         */
        private static final int NO_OFFSET = 0;

        /**
         * A description of the instrumented method.
         */
        protected final MethodDescription.InDefinedShape instrumentedMethod;

        /**
         * The dispatcher to be used for method entry.
         */
        private final Dispatcher.Resolved.ForMethodEnter methodEnter;

        /**
         * The dispatcher to be used for method exit.
         */
        private final Dispatcher.Resolved.ForMethodExit methodExit;

        /**
         * A reader for traversing the advise methods' class file.
         */
        private final ClassReader classReader;

        /**
         * The frame translator to use.
         */
        protected final FrameTranslator frameTranslator;

        /**
         * Creates an advise visitor.
         *
         * @param methodVisitor        The method visitor for the instrumented method.
         * @param instrumentedMethod   A description of the instrumented method.
         * @param methodEnter          The dispatcher to be used for method entry.
         * @param methodExit           The dispatcher to be used for method exit.
         * @param binaryRepresentation The binary representation of the advise methods' class file.
         */
        protected AdviceVisitor(MethodVisitor methodVisitor,
                                MethodDescription.InDefinedShape instrumentedMethod,
                                Dispatcher.Resolved.ForMethodEnter methodEnter,
                                Dispatcher.Resolved.ForMethodExit methodExit,
                                byte[] binaryRepresentation) {
            super(Opcodes.ASM5, methodVisitor);
            this.instrumentedMethod = instrumentedMethod;
            this.methodEnter = methodEnter;
            this.methodExit = methodExit;
            classReader = new ClassReader(binaryRepresentation);
            frameTranslator = new FrameTranslator(instrumentedMethod, methodEnter.getEnterType().represents(void.class)
                    ? new TypeList.Empty()
                    : new TypeList.Explicit(methodEnter.getEnterType()));
        }

        @Override
        public void visitCode() {
            super.visitCode();
            onMethodStart();
        }

        /**
         * Writes the advise for entering the instrumented method.
         */
        protected abstract void onMethodStart();

        @Override
        public void visitVarInsn(int opcode, int offset) {
            super.visitVarInsn(opcode, offset < instrumentedMethod.getStackSize()
                    ? offset
                    : offset + methodEnter.getEnterType().getStackSize().getSize());
        }

        @Override
        @SuppressFBWarnings(value = "SF_SWITCH_NO_DEFAULT", justification = "Switch is supposed to fall through")
        public void visitInsn(int opcode) {
            switch (opcode) {
                case Opcodes.RETURN:
                    mv.visitInsn(Opcodes.ACONST_NULL);
                    variable(Opcodes.ASTORE);
                    frameTranslator.injectCompletionFrame(mv);
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
                case Opcodes.ARETURN:
                    onMethodExit(Opcodes.ASTORE, Opcodes.ALOAD);
                    break;
            }
            mv.visitInsn(opcode);
        }

        /**
         * Writes the advise for the instrumented method's end.
         *
         * @param store The return type's store instruction.
         * @param load  The return type's load instruction.
         */
        private void onMethodExit(int store, int load) {
            variable(store);
            mv.visitInsn(Opcodes.ACONST_NULL);
            variable(Opcodes.ASTORE, instrumentedMethod.getReturnType().getStackSize().getSize());
            frameTranslator.injectCompletionFrame(mv);
            onMethodExit();
            variable(load);
        }

        /**
         * Writes the advise for exiting the instrumented method.
         */
        protected abstract void onMethodExit();

        /**
         * Access the first variable after the instrumented variables and return type are stored.
         *
         * @param opcode The opcode for accessing the variable.
         */
        protected void variable(int opcode) {
            variable(opcode, NO_OFFSET);
        }

        /**
         * Access the first variable after the instrumented variables and return type are stored.
         *
         * @param opcode The opcode for accessing the variable.
         * @param offset The additional offset of the variable.
         */
        protected void variable(int opcode, int offset) {
            mv.visitVarInsn(opcode, instrumentedMethod.getStackSize() + methodEnter.getEnterType().getStackSize().getSize() + offset);
        }

        @Override
        public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
            frameTranslator.translateFrame(mv, type, nLocal, local, nStack, stack);
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            onMethodEnd();
            super.visitMaxs(frameTranslator.compoundStackSize(maxStack), frameTranslator.compoundLocalVariableSize(maxLocals));
        }

        /**
         * Writes the advise for completing the instrumented method.
         */
        protected abstract void onMethodEnd();

        /**
         * Appends the enter advise's byte code.
         */
        protected void appendEnter() {
            append(methodEnter);
        }

        /**
         * Appends the exit advise's byte code.
         */
        protected void appendExit() {
            append(methodExit);
        }

        /**
         * Appends the byte code of the supplied dispatcher.
         *
         * @param dispatcher The dispatcher for which the byte code should be appended.
         */
        private void append(Dispatcher.Resolved dispatcher) {
            classReader.accept(new CodeCopier(dispatcher), ClassReader.SKIP_DEBUG);
        }

        /**
         * A visitor for copying an advise method's byte code.
         */
        protected class CodeCopier extends ClassVisitor {

            /**
             * The dispatcher to use.
             */
            private final Dispatcher.Resolved dispatcher;

            /**
             * Creates a new code copier.
             *
             * @param dispatcher The dispatcher to use.
             */
            protected CodeCopier(Dispatcher.Resolved dispatcher) {
                super(Opcodes.ASM5);
                this.dispatcher = dispatcher;
            }

            @Override
            public MethodVisitor visitMethod(int modifiers, String internalName, String descriptor, String signature, String[] exception) {
                return dispatcher.apply(internalName, descriptor, mv, frameTranslator, instrumentedMethod);
            }

            @Override
            public String toString() {
                return "Advice.AdviceVisitor.CodeCopier{" +
                        "outer=" + AdviceVisitor.this +
                        ", dispatcher=" + dispatcher +
                        '}';
            }
        }

        /**
         * An advise visitor that captures exceptions by weaving try-catch blocks around user code.
         */
        protected static class WithExceptionHandling extends AdviceVisitor {

            /**
             * Indicates that any throwable should be captured.
             */
            private static final String ANY_THROWABLE = null;

            /**
             * The label indicating the start of the exception handler.
             */
            private final Label handler;

            /**
             * The current label that indicates the next section that terminates user code.
             */
            private Label userEnd;

            /**
             * Creates a new advise visitor that captures exception by weaving try-catch blocks around user code.
             *
             * @param methodVisitor        The method visitor for the instrumented method.
             * @param instrumentedMethod   A description of the instrumented method.
             * @param methodEnter          The dispatcher to be used for method entry.
             * @param methodExit           The dispatcher to be used for method exit.
             * @param binaryRepresentation The binary representation of the advise methods' class file.
             */
            protected WithExceptionHandling(MethodVisitor methodVisitor,
                                            MethodDescription.InDefinedShape instrumentedMethod,
                                            Dispatcher.Resolved.ForMethodEnter methodEnter,
                                            Dispatcher.Resolved.ForMethodExit methodExit,
                                            byte[] binaryRepresentation) {
                super(methodVisitor, instrumentedMethod, methodEnter, methodExit, binaryRepresentation);
                handler = new Label();
            }

            @Override
            protected void onMethodStart() {
                appendEnter();
                Label userStart = new Label();
                userEnd = new Label();
                mv.visitTryCatchBlock(userStart, userEnd, handler, null);
                mv.visitLabel(userStart);
            }

            @Override
            protected void onMethodExit() {
                mv.visitLabel(userEnd);
                appendExit();
                Label userStart = new Label();
                userEnd = new Label();
                mv.visitTryCatchBlock(userStart, userEnd, handler, ANY_THROWABLE);
                mv.visitLabel(userStart);
            }

            @Override
            protected void onMethodEnd() {
                mv.visitLabel(userEnd);
                mv.visitLabel(handler);
                frameTranslator.injectHandlerFrame(mv);
                variable(Opcodes.ASTORE, instrumentedMethod.getReturnType().getStackSize().getSize());
                storeDefaultReturn();
                appendExit();
                variable(Opcodes.ALOAD, instrumentedMethod.getReturnType().getStackSize().getSize());
                mv.visitInsn(Opcodes.ATHROW);
            }

            /**
             * Stores a default return value in the designated slot of the local variable array.
             */
            private void storeDefaultReturn() {
                if (instrumentedMethod.getReturnType().represents(boolean.class)
                        || instrumentedMethod.getReturnType().represents(byte.class)
                        || instrumentedMethod.getReturnType().represents(short.class)
                        || instrumentedMethod.getReturnType().represents(char.class)
                        || instrumentedMethod.getReturnType().represents(int.class)) {
                    mv.visitInsn(Opcodes.ICONST_0);
                    variable(Opcodes.ISTORE);
                } else if (instrumentedMethod.getReturnType().represents(long.class)) {
                    mv.visitInsn(Opcodes.LCONST_0);
                    variable(Opcodes.LSTORE);
                } else if (instrumentedMethod.getReturnType().represents(float.class)) {
                    mv.visitInsn(Opcodes.FCONST_0);
                    variable(Opcodes.FSTORE);
                } else if (instrumentedMethod.getReturnType().represents(double.class)) {
                    mv.visitInsn(Opcodes.DCONST_0);
                    variable(Opcodes.DSTORE);
                } else if (!instrumentedMethod.getReturnType().represents(void.class)) {
                    mv.visitInsn(Opcodes.ACONST_NULL);
                    variable(Opcodes.ASTORE);
                }
            }

            @Override
            public String toString() {
                return "Advice.AdviceVisitor.WithExceptionHandling{" +
                        "instrumentedMethod=" + instrumentedMethod +
                        '}';
            }
        }

        /**
         * An advise visitor that does not capture exceptions.
         */
        protected static class WithoutExceptionHandling extends AdviceVisitor {

            /**
             * Creates a new advise visitor that does not capture exceptions.
             *
             * @param methodVisitor        The method visitor for the instrumented method.
             * @param instrumentedMethod   A description of the instrumented method.
             * @param methodEnter          The dispatcher to be used for method entry.
             * @param methodExit           The dispatcher to be used for method exit.
             * @param binaryRepresentation The binary representation of the advise methods' class file.
             */
            protected WithoutExceptionHandling(MethodVisitor methodVisitor,
                                               MethodDescription.InDefinedShape instrumentedMethod,
                                               Dispatcher.Resolved.ForMethodEnter methodEnter,
                                               Dispatcher.Resolved.ForMethodExit methodExit,
                                               byte[] binaryRepresentation) {
                super(methodVisitor, instrumentedMethod, methodEnter, methodExit, binaryRepresentation);
            }

            @Override
            protected void onMethodStart() {
                appendEnter();
            }

            @Override
            protected void onMethodExit() {
                appendExit();
            }

            @Override
            protected void onMethodEnd() {
                /* do nothing */
            }

            @Override
            public String toString() {
                return "Advice.AdviceVisitor.WithoutExceptionHandling{" +
                        "instrumentedMethod=" + instrumentedMethod +
                        '}';
            }
        }
    }

    /**
     * A dispatcher for implementing advise.
     */
    protected interface Dispatcher {

        /**
         * Indicates that a method does not represent advise and does not need to be visited.
         */
        MethodVisitor IGNORE_METHOD = null;

        /**
         * Returns {@code true} if this dispatcher is alive.
         *
         * @return {@code true} if this dispatcher is alive.
         */
        boolean isAlive();

        /**
         * Resolves this dispatcher as a dispatcher for entering a method.
         *
         * @return This dispatcher as a dispatcher for entering a method.
         */
        Resolved.ForMethodEnter asMethodEnter();

        /**
         * Resolves this dispatcher as a dispatcher for exiting a method.
         *
         * @param dispatcher The dispatcher for entering a method.
         * @return This dispatcher as a dispatcher for exiting a method.
         */
        Resolved.ForMethodExit asMethodExitTo(Resolved.ForMethodEnter dispatcher);

        /**
         * Represents a resolved dispatcher.
         */
        interface Resolved {

            /**
             * Applies this dispatcher for a method that is discovered in the advice class's class file.
             *
             * @param internalName       The discovered method's internal name.
             * @param descriptor         The discovered method's descriptor.
             * @param methodVisitor      The method visitor for writing the instrumented method.
             * @param frameTranslator    The frame translator to use.
             * @param instrumentedMethod A description of the instrumented method.
             * @return A method visitor for reading the discovered method or {@code null} if the discovered method is of no interest.
             */
            MethodVisitor apply(String internalName,
                                String descriptor,
                                MethodVisitor methodVisitor,
                                FrameTranslator frameTranslator,
                                MethodDescription.InDefinedShape instrumentedMethod);

            /**
             * Represents a resolved dispatcher for entering a method.
             */
            interface ForMethodEnter extends Resolved {

                /**
                 * Returns the type that this dispatcher supplies as a result of its advise or a description of {@code void} if
                 * no type is supplied as a result of the enter advise.
                 *
                 * @return The type that this dispatcher supplies as a result of its advise or a description of {@code void}.
                 */
                TypeDescription getEnterType();
            }

            /**
             * Represents a resolved dispatcher for exiting a method.
             */
            interface ForMethodExit extends Resolved {

                /**
                 * Indicates if this advise requires to be called when the instrumented method terminates exceptionally.
                 *
                 * @return {@code true} if this advise requires to be called when the instrumented method terminates exceptionally.
                 */
                boolean isSkipThrowable();
            }
        }

        /**
         * An implementation for inactive devise that does not write any byte code.
         */
        enum Inactive implements Dispatcher, Resolved.ForMethodEnter, Resolved.ForMethodExit {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public boolean isAlive() {
                return false;
            }

            @Override
            public boolean isSkipThrowable() {
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
            public MethodVisitor apply(String internalName,
                                       String descriptor,
                                       MethodVisitor methodVisitor,
                                       FrameTranslator frameTranslator,
                                       MethodDescription.InDefinedShape instrumentedMethod) {
                return IGNORE_METHOD;
            }

            @Override
            public String toString() {
                return "Advice.Dispatcher.Inactive." + name();
            }
        }

        /**
         * A dispatcher for active advise.
         */
        class Active implements Dispatcher {

            /**
             * The advise method.
             */
            protected final MethodDescription.InDefinedShape adviseMethod;

            /**
             * Creates a dispatcher for active advise.
             *
             * @param adviseMethod The advise method.
             */
            protected Active(MethodDescription.InDefinedShape adviseMethod) {
                this.adviseMethod = adviseMethod;
            }

            @Override
            public boolean isAlive() {
                return true;
            }

            @Override
            public Dispatcher.Resolved.ForMethodEnter asMethodEnter() {
                return new Resolved.ForMethodEnter(adviseMethod);
            }

            @Override
            public Dispatcher.Resolved.ForMethodExit asMethodExitTo(Dispatcher.Resolved.ForMethodEnter dispatcher) {
                return new Resolved.ForMethodExit(adviseMethod, dispatcher.getEnterType());
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass()) && adviseMethod.equals(((Active) other).adviseMethod);
            }

            @Override
            public int hashCode() {
                return adviseMethod.hashCode();
            }

            @Override
            public String toString() {
                return "Advice.Dispatcher.Active{" +
                        "adviseMethod=" + adviseMethod +
                        '}';
            }

            /**
             * A resolved version of a dispatcher.
             */
            protected abstract static class Resolved implements Dispatcher.Resolved {

                /**
                 * Indicates a read-only mapping for an offset.
                 */
                private static final boolean READ_ONLY = true;

                /**
                 * The represented advise method.
                 */
                protected final MethodDescription.InDefinedShape adviseMethod;

                /**
                 * An unresolved mapping of offsets of the advise method based on the annotations discovered on each method parameter.
                 */
                protected final Map<Integer, OffsetMapping> offsetMappings;

                /**
                 * Creates a new resolved version of a dispatcher.
                 *
                 * @param adviseMethod The represented advise method.
                 * @param factory      An unresolved mapping of offsets of the advise method based on the annotations discovered on each method parameter.
                 */
                protected Resolved(MethodDescription.InDefinedShape adviseMethod, OffsetMapping.Factory... factory) {
                    this.adviseMethod = adviseMethod;
                    offsetMappings = new HashMap<Integer, OffsetMapping>();
                    for (ParameterDescription.InDefinedShape parameterDescription : adviseMethod.getParameters()) {
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
                                ? new OffsetMapping.ForParameter(parameterDescription.getIndex(), READ_ONLY, parameterDescription.getType().asErasure())
                                : offsetMapping);
                    }
                }

                @Override
                public MethodVisitor apply(String internalName,
                                           String descriptor,
                                           MethodVisitor methodVisitor,
                                           FrameTranslator frameTranslator,
                                           MethodDescription.InDefinedShape instrumentedMethod) {
                    return adviseMethod.getInternalName().equals(internalName) && adviseMethod.getDescriptor().equals(descriptor)
                            ? apply(methodVisitor, frameTranslator, instrumentedMethod)
                            : IGNORE_METHOD;
                }

                /**
                 * Applies a resolution for a given instrumented method.
                 *
                 * @param methodVisitor      A method visitor for writing byte code to the instrumented method.
                 * @param frameTranslator    The frame translator to use.
                 * @param instrumentedMethod A description of the instrumented method.
                 * @return A method visitor for visiting the advise method's byte code.
                 */
                protected abstract MethodVisitor apply(MethodVisitor methodVisitor,
                                                       FrameTranslator frameTranslator,
                                                       MethodDescription.InDefinedShape instrumentedMethod);

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (other == null || getClass() != other.getClass()) return false;
                    Resolved resolved = (Resolved) other;
                    return adviseMethod.equals(resolved.adviseMethod) && offsetMappings.equals(resolved.offsetMappings);
                }

                @Override
                public int hashCode() {
                    int result = adviseMethod.hashCode();
                    result = 31 * result + offsetMappings.hashCode();
                    return result;
                }

                /**
                 * Represents an offset mapping for an advise method to an alternative offset.
                 */
                interface OffsetMapping {

                    /**
                     * Resolves an offset mapping to a given target offset.
                     *
                     * @param instrumentedMethod The instrumented method for which the mapping is to be resolved.
                     * @param enterType          The type returned by the enter advice or {@code void} if there is no enter type to consider.
                     * @return A suitable target mapping.
                     */
                    Target resolve(MethodDescription.InDefinedShape instrumentedMethod, TypeDescription enterType);

                    /**
                     * A target offset of an offset mapping.
                     */
                    interface Target {

                        /**
                         * Applies this offset mapping for a {@link MethodVisitor#visitVarInsn(int, int)} instruction.
                         *
                         * @param methodVisitor The method visitor onto which this offset mapping is to be applied.
                         * @param opcode        The opcode of the original instruction.
                         */
                        void apply(MethodVisitor methodVisitor, int opcode);

                        /**
                         * Loads a default value onto the stack or pops the accessed value off it.
                         */
                        enum ForDefaultValue implements Target {

                            /**
                             * The singleton instance.
                             */
                            INSTANCE;

                            @Override
                            public void apply(MethodVisitor methodVisitor, int opcode) {
                                switch (opcode) {
                                    case Opcodes.ALOAD:
                                        methodVisitor.visitInsn(Opcodes.ACONST_NULL);
                                        break;
                                    case Opcodes.ILOAD:
                                        methodVisitor.visitInsn(Opcodes.ICONST_0);
                                        break;
                                    case Opcodes.LLOAD:
                                        methodVisitor.visitInsn(Opcodes.LCONST_0);
                                        break;
                                    case Opcodes.FLOAD:
                                        methodVisitor.visitInsn(Opcodes.FCONST_0);
                                        break;
                                    case Opcodes.DLOAD:
                                        methodVisitor.visitInsn(Opcodes.DCONST_0);
                                        break;
                                    case Opcodes.ISTORE:
                                    case Opcodes.FSTORE:
                                    case Opcodes.ASTORE:
                                        methodVisitor.visitInsn(Opcodes.POP);
                                        break;
                                    case Opcodes.LSTORE:
                                    case Opcodes.DSTORE:
                                        methodVisitor.visitInsn(Opcodes.POP2);
                                        break;
                                    default:
                                        throw new IllegalStateException("Unexpected opcode: " + opcode);
                                }
                            }

                            @Override
                            public String toString() {
                                return "Advice.Dispatcher.Active.Resolved.OffsetMapping.Target.ForDefaultValue." + name();
                            }
                        }

                        /**
                         * A read-write target mapping.
                         */
                        class ForParameter implements Target {

                            /**
                             * The mapped offset.
                             */
                            private final int offset;

                            /**
                             * Creates a new read-write target mapping.
                             *
                             * @param offset The mapped offset.
                             */
                            protected ForParameter(int offset) {
                                this.offset = offset;
                            }

                            @Override
                            public void apply(MethodVisitor methodVisitor, int opcode) {
                                methodVisitor.visitVarInsn(opcode, offset);
                            }

                            @Override
                            public boolean equals(Object object) {
                                if (this == object) return true;
                                if (object == null || getClass() != object.getClass()) return false;
                                ForParameter forParameter = (ForParameter) object;
                                return offset == forParameter.offset;
                            }

                            @Override
                            public int hashCode() {
                                return offset;
                            }

                            @Override
                            public String toString() {
                                return "Advice.Dispatcher.Active.Resolved.OffsetMapping.Target.ForParameter{" +
                                        "offset=" + offset +
                                        '}';
                            }
                        }

                        /**
                         * A read-only target mapping.
                         */
                        class ForReadOnlyParameter implements Target {

                            /**
                             * The mapped offset.
                             */
                            private final int offset;

                            /**
                             * Creates a new read-only target mapping.
                             *
                             * @param offset The mapped offset.
                             */
                            protected ForReadOnlyParameter(int offset) {
                                this.offset = offset;
                            }

                            @Override
                            public void apply(MethodVisitor methodVisitor, int opcode) {
                                switch (opcode) {
                                    case Opcodes.ISTORE:
                                    case Opcodes.LSTORE:
                                    case Opcodes.FSTORE:
                                    case Opcodes.DSTORE:
                                    case Opcodes.ASTORE:
                                        throw new IllegalStateException("Cannot write to read-only parameter at offset " + offset);
                                    case Opcodes.ILOAD:
                                    case Opcodes.LLOAD:
                                    case Opcodes.FLOAD:
                                    case Opcodes.DLOAD:
                                    case Opcodes.ALOAD:
                                        methodVisitor.visitVarInsn(opcode, offset);
                                        break;
                                    default:
                                        throw new IllegalArgumentException("Did not expect opcode: " + opcode);
                                }
                            }

                            @Override
                            public boolean equals(Object object) {
                                if (this == object) return true;
                                if (object == null || getClass() != object.getClass()) return false;
                                ForReadOnlyParameter forReadOnlyParameter = (ForReadOnlyParameter) object;
                                return offset == forReadOnlyParameter.offset;
                            }

                            @Override
                            public int hashCode() {
                                return offset;
                            }

                            @Override
                            public String toString() {
                                return "Advice.Dispatcher.Active.Resolved.OffsetMapping.Target.ForReadOnlyParameter{" +
                                        "offset=" + offset +
                                        '}';
                            }
                        }

                        /**
                         * An offset mapping for a field.
                         */
                        class ForField implements Target {

                            /**
                             * The field being read.
                             */
                            private final FieldDescription fieldDescription;

                            /**
                             * Creates a new offset mapping for a field.
                             *
                             * @param fieldDescription The field being read.
                             */
                            protected ForField(FieldDescription fieldDescription) {
                                this.fieldDescription = fieldDescription;
                            }

                            @Override
                            public void apply(MethodVisitor methodVisitor, int opcode) {
                                switch (opcode) {
                                    case Opcodes.ISTORE:
                                    case Opcodes.ASTORE:
                                    case Opcodes.FSTORE:
                                    case Opcodes.LSTORE:
                                    case Opcodes.DSTORE:
                                        throw new IllegalStateException("Cannot write to field: " + fieldDescription);
                                    case Opcodes.ILOAD:
                                    case Opcodes.FLOAD:
                                    case Opcodes.ALOAD:
                                    case Opcodes.LLOAD:
                                    case Opcodes.DLOAD:
                                        int accessOpcode;
                                        if (fieldDescription.isStatic()) {
                                            accessOpcode = Opcodes.GETSTATIC;
                                        } else {
                                            methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                                            accessOpcode = Opcodes.GETFIELD;
                                        }
                                        methodVisitor.visitFieldInsn(accessOpcode,
                                                fieldDescription.getDeclaringType().asErasure().getInternalName(),
                                                fieldDescription.getInternalName(),
                                                fieldDescription.getDescriptor());
                                        break;
                                    default:
                                        throw new IllegalArgumentException("Did not expect opcode: " + opcode);
                                }
                            }

                            @Override
                            public boolean equals(Object object) {
                                if (this == object) return true;
                                if (object == null || getClass() != object.getClass()) return false;
                                ForField forField = (ForField) object;
                                return fieldDescription.equals(forField.fieldDescription);
                            }

                            @Override
                            public int hashCode() {
                                return fieldDescription.hashCode();
                            }

                            @Override
                            public String toString() {
                                return "Advice.Dispatcher.Active.Resolved.OffsetMapping.Target.ForField{" +
                                        "fieldDescription=" + fieldDescription +
                                        '}';
                            }
                        }

                        /**
                         * An offset mapping for a constant pool value.
                         */
                        class ForConstantPoolValue implements Target {

                            /**
                             * The constant pool value.
                             */
                            private final Object value;

                            /**
                             * Creates a mapping for a constant pool value.
                             *
                             * @param value The constant pool value.
                             */
                            protected ForConstantPoolValue(Object value) {
                                this.value = value;
                            }

                            @Override
                            public void apply(MethodVisitor methodVisitor, int opcode) {
                                methodVisitor.visitLdcInsn(value);
                            }

                            @Override
                            public boolean equals(Object object) {
                                if (this == object) return true;
                                if (object == null || getClass() != object.getClass()) return false;
                                ForConstantPoolValue that = (ForConstantPoolValue) object;
                                return value.equals(that.value);
                            }

                            @Override
                            public int hashCode() {
                                return value.hashCode();
                            }

                            @Override
                            public String toString() {
                                return "Advice.Dispatcher.Active.Resolved.OffsetMapping.Target.ForConstantPoolValue{" +
                                        "value=" + value +
                                        '}';
                            }
                        }
                    }

                    /**
                     * Represents a factory for creating a {@link OffsetMapping} for a given parameter.
                     */
                    interface Factory {

                        /**
                         * Indicates that an offset mapping is undefined.
                         */
                        OffsetMapping UNDEFINED = null;

                        /**
                         * Creates a new offset mapping for the supplied parameter if possible.
                         *
                         * @param parameterDescription The parameter description for which to resolve an offset mapping.
                         * @return A resolved offset mapping or {@code null} if no mapping can be resolved for this parameter.
                         */
                        OffsetMapping make(ParameterDescription.InDefinedShape parameterDescription);
                    }

                    /**
                     * An offset mapping for a given parameter of the instrumented method.
                     */
                    class ForParameter implements OffsetMapping {

                        /**
                         * The index of the parameter.
                         */
                        private final int index;

                        /**
                         * Determines if the parameter is to be treated as read-only.
                         */
                        private final boolean readOnly;

                        /**
                         * The type expected by the advise method.
                         */
                        private final TypeDescription targetType;

                        /**
                         * Creates a new offset mapping for a parameter.
                         *
                         * @param argument   The annotation for which the mapping is to be created.
                         * @param targetType Determines if the parameter is to be treated as read-only.
                         */
                        protected ForParameter(Argument argument, TypeDescription targetType) {
                            this(argument.value(), argument.readOnly(), targetType);
                        }

                        /**
                         * Creates a new offset mapping for a parameter of the instrumented method.
                         *
                         * @param index      The index of the parameter.
                         * @param readOnly   Determines if the parameter is to be treated as read-only.
                         * @param targetType The type expected by the advise method.
                         */
                        protected ForParameter(int index, boolean readOnly, TypeDescription targetType) {
                            this.index = index;
                            this.readOnly = readOnly;
                            this.targetType = targetType;
                        }

                        @Override
                        public Target resolve(MethodDescription.InDefinedShape instrumentedMethod, TypeDescription enterType) {
                            ParameterList<?> parameters = instrumentedMethod.getParameters();
                            if (parameters.size() <= index) {
                                throw new IllegalStateException(instrumentedMethod + " does not define an index " + index);
                            } else if (!readOnly && !parameters.get(index).getType().asErasure().equals(targetType)) {
                                throw new IllegalStateException("read-only " + targetType + " is not equal to type of " + parameters.get(index));
                            } else if (readOnly && !parameters.get(index).getType().asErasure().isAssignableTo(targetType)) {
                                throw new IllegalStateException(targetType + " is not assignable to " + parameters.get(index));
                            }
                            return readOnly
                                    ? new Target.ForReadOnlyParameter(parameters.get(index).getOffset())
                                    : new Target.ForParameter(parameters.get(index).getOffset());
                        }

                        @Override
                        public boolean equals(Object object) {
                            if (this == object) return true;
                            if (object == null || getClass() != object.getClass()) return false;
                            ForParameter that = (ForParameter) object;
                            return index == that.index
                                    && readOnly == that.readOnly
                                    && targetType.equals(that.targetType);
                        }

                        @Override
                        public int hashCode() {
                            int result = index;
                            result = 31 * result + (readOnly ? 1 : 0);
                            result = 31 * result + targetType.hashCode();
                            return result;
                        }

                        @Override
                        public String toString() {
                            return "Advice.Dispatcher.Active.Resolved.OffsetMapping.ForParameter{" +
                                    "index=" + index +
                                    ", readOnly=" + readOnly +
                                    ", targetType=" + targetType +
                                    '}';
                        }

                        /**
                         * A factory for creating a {@link ForParameter} offset mapping.
                         */
                        protected enum Factory implements OffsetMapping.Factory {

                            /**
                             * The singleton instance.
                             */
                            INSTANCE;

                            @Override
                            public OffsetMapping make(ParameterDescription.InDefinedShape parameterDescription) {
                                AnnotationDescription.Loadable<Argument> annotation = parameterDescription.getDeclaredAnnotations().ofType(Argument.class);
                                return annotation == null
                                        ? UNDEFINED
                                        : new ForParameter(annotation.loadSilent(), parameterDescription.getType().asErasure());
                            }

                            @Override
                            public String toString() {
                                return "Advice.Dispatcher.Active.Resolved.OffsetMapping.ForParameter.Factory." + name();
                            }
                        }
                    }

                    /**
                     * An offset mapping that provides access to the {@code this} reference of the instrumented method.
                     */
                    class ForThisReference implements OffsetMapping {

                        /**
                         * The offset of the this reference in a Java method.
                         */
                        private static final int THIS_REFERENCE = 0;

                        /**
                         * Determines if the parameter is to be treated as read-only.
                         */
                        private final boolean readOnly;

                        /**
                         * The type that the advise method expects for the {@code this} reference.
                         */
                        private final TypeDescription targetType;

                        /**
                         * Creates a new offset mapping for a {@code this} reference.
                         *
                         * @param readOnly   Determines if the parameter is to be treated as read-only.
                         * @param targetType The type that the advise method expects for the {@code this} reference.
                         */
                        protected ForThisReference(boolean readOnly, TypeDescription targetType) {
                            this.readOnly = readOnly;
                            this.targetType = targetType;
                        }

                        @Override
                        public Target resolve(MethodDescription.InDefinedShape instrumentedMethod, TypeDescription enterType) {
                            if (instrumentedMethod.isStatic()) {
                                throw new IllegalStateException("Cannot map this reference for static method " + instrumentedMethod);
                            } else if (!readOnly && !instrumentedMethod.getDeclaringType().equals(targetType)) {
                                throw new IllegalStateException("Declaring type of " + instrumentedMethod + " is not equal to read-only " + targetType);
                            } else if (readOnly && !instrumentedMethod.getDeclaringType().isAssignableTo(targetType)) {
                                throw new IllegalStateException("Declaring type of " + instrumentedMethod + " is not assignable to " + targetType);
                            }
                            return readOnly
                                    ? new Target.ForReadOnlyParameter(THIS_REFERENCE)
                                    : new Target.ForParameter(THIS_REFERENCE);
                        }

                        @Override
                        public boolean equals(Object object) {
                            if (this == object) return true;
                            if (object == null || getClass() != object.getClass()) return false;
                            ForThisReference that = (ForThisReference) object;
                            return readOnly == that.readOnly
                                    && targetType.equals(that.targetType);
                        }

                        @Override
                        public int hashCode() {
                            int result = (readOnly ? 1 : 0);
                            result = 31 * result + targetType.hashCode();
                            return result;
                        }

                        @Override
                        public String toString() {
                            return "Advice.Dispatcher.Active.Resolved.OffsetMapping.ForThisReference{" +
                                    "readOnly=" + readOnly +
                                    ", targetType=" + targetType +
                                    '}';
                        }

                        /**
                         * A factory for creating a {@link ForThisReference} offset mapping.
                         */
                        protected enum Factory implements OffsetMapping.Factory {

                            /**
                             * The singleton instance.
                             */
                            INSTANCE;

                            @Override
                            public OffsetMapping make(ParameterDescription.InDefinedShape parameterDescription) {
                                AnnotationDescription.Loadable<This> annotation = parameterDescription.getDeclaredAnnotations().ofType(This.class);
                                return annotation == null
                                        ? UNDEFINED
                                        : new ForThisReference(annotation.loadSilent().readOnly(), parameterDescription.getType().asErasure());
                            }

                            @Override
                            public String toString() {
                                return "Advice.Dispatcher.Active.Resolved.OffsetMapping.ForThisReference.Factory." + name();
                            }
                        }
                    }

                    /**
                     * An offset mapping for a field.
                     */
                    abstract class ForField implements OffsetMapping {

                        /**
                         * The name of the field.
                         */
                        protected final String name;

                        /**
                         * The expected type that the field can be assigned to.
                         */
                        protected final TypeDescription targetType;

                        /**
                         * Creates an offset mapping for a field.
                         *
                         * @param name       The name of the field.
                         * @param targetType The expected type that the field can be assigned to.
                         */
                        protected ForField(String name, TypeDescription targetType) {
                            this.name = name;
                            this.targetType = targetType;
                        }

                        @Override
                        public boolean equals(Object object) {
                            if (this == object) return true;
                            if (object == null || getClass() != object.getClass()) return false;
                            ForField forField = (ForField) object;
                            return name.equals(forField.name) && targetType.equals(forField.targetType);
                        }

                        @Override
                        public int hashCode() {
                            int result = name.hashCode();
                            result = 31 * result + targetType.hashCode();
                            return result;
                        }

                        @Override
                        public Target resolve(MethodDescription.InDefinedShape instrumentedMethod, TypeDescription enterType) {
                            FieldLocator.Resolution resolution = fieldLocator(instrumentedMethod.getDeclaringType()).locate(name);
                            if (!resolution.isResolved()) {
                                throw new IllegalStateException("Cannot locate field named " + name + " for " + instrumentedMethod);
                            } else if (!resolution.getField().getType().asErasure().isAssignableTo(targetType)) {
                                throw new IllegalStateException("Cannot assign type of field " + resolution.getField() + " to " + targetType);
                            } else if (!resolution.getField().isStatic() && instrumentedMethod.isStatic()) {
                                throw new IllegalStateException("Cannot read non-static field " + resolution.getField() + " from static method " + instrumentedMethod);
                            }
                            return new Target.ForField(resolution.getField());
                        }

                        /**
                         * Returns a field locator for this instance.
                         *
                         * @param instrumentedType The instrumented type.
                         * @return An appropriate field locator.
                         */
                        protected abstract FieldLocator fieldLocator(TypeDescription instrumentedType);

                        /**
                         * An offset mapping for a field with an implicit declaring type.
                         */
                        protected static class WithImplicitType extends ForField {

                            /**
                             * Creates an offset mapping for a field with an implicit declaring type.
                             *
                             * @param name       The name of the field.
                             * @param targetType The expected type that the field can be assigned to.
                             */
                            protected WithImplicitType(String name, TypeDescription targetType) {
                                super(name, targetType);
                            }

                            @Override
                            protected FieldLocator fieldLocator(TypeDescription instrumentedType) {
                                return new FieldLocator.ForClassHierarchy(instrumentedType);
                            }

                            @Override
                            public String toString() {
                                return "Advice.Dispatcher.Active.Resolved.OffsetMapping.ForField.WithImplicitType{" +
                                        "name=" + name +
                                        ", targetType=" + targetType +
                                        '}';
                            }
                        }

                        /**
                         * An offset mapping for a field with an explicit declaring type.
                         */
                        protected static class WithExplicitType extends ForField {

                            /**
                             * The type declaring the field.
                             */
                            private final TypeDescription explicitType;

                            /**
                             * Creates an offset mapping for a field with an explicit declaring type.
                             *
                             * @param name        The name of the field.
                             * @param targetType  The expected type that the field can be assigned to.
                             * @param locatedType The type declaring the field.
                             */
                            protected WithExplicitType(String name, TypeDescription targetType, TypeDescription locatedType) {
                                super(name, targetType);
                                this.explicitType = locatedType;
                            }

                            @Override
                            protected FieldLocator fieldLocator(TypeDescription instrumentedType) {
                                if (!instrumentedType.isAssignableTo(explicitType)) {
                                    throw new IllegalStateException(explicitType + " is no super type of " + instrumentedType);
                                }
                                return new FieldLocator.ForExactType(explicitType);
                            }

                            @Override
                            public boolean equals(Object object) {
                                if (this == object) return true;
                                if (object == null || getClass() != object.getClass()) return false;
                                if (!super.equals(object)) return false;
                                WithExplicitType that = (WithExplicitType) object;
                                return explicitType.equals(that.explicitType);
                            }

                            @Override
                            public int hashCode() {
                                int result = super.hashCode();
                                result = 31 * result + explicitType.hashCode();
                                return result;
                            }

                            @Override
                            public String toString() {
                                return "Advice.Dispatcher.Active.Resolved.OffsetMapping.ForField.WithExplicitType{" +
                                        "name=" + name +
                                        ", targetType=" + targetType +
                                        ", explicitType=" + explicitType +
                                        '}';
                            }
                        }

                        /**
                         * A factory for a {@link ForField} offset mapping.
                         */
                        protected enum Factory implements OffsetMapping.Factory {

                            /**
                             * The singleton instance.
                             */
                            INSTANCE;

                            /**
                             * Creates a new factory for a {@link ForField} offset mapping.
                             */
                            Factory() {
                                MethodList<MethodDescription.InDefinedShape> methods = new TypeDescription.ForLoadedType(FieldValue.class).getDeclaredMethods();
                                value = methods.filter(named("value")).getOnly();
                                definingType = methods.filter(named("declaringType")).getOnly();
                            }

                            /**
                             * The {@link FieldValue#value()} method.
                             */
                            private final MethodDescription.InDefinedShape value;

                            /**
                             * The {@link FieldValue#declaringType()}} method.
                             */
                            private final MethodDescription.InDefinedShape definingType;

                            @Override
                            public OffsetMapping make(ParameterDescription.InDefinedShape parameterDescription) {
                                AnnotationDescription annotation = parameterDescription.getDeclaredAnnotations().ofType(FieldValue.class);
                                if (annotation == null) {
                                    return UNDEFINED;
                                } else {
                                    TypeDescription definingType = annotation.getValue(this.definingType, TypeDescription.class);
                                    String name = annotation.getValue(value, String.class);
                                    TypeDescription targetType = parameterDescription.getType().asErasure();
                                    return definingType.represents(void.class)
                                            ? new WithImplicitType(name, targetType)
                                            : new WithExplicitType(name, targetType, definingType);
                                }
                            }

                            @Override
                            public String toString() {
                                return "Advice.Dispatcher.Active.Resolved.OffsetMapping.ForField.Factory." + name();
                            }
                        }
                    }

                    /**
                     * An offset mapping for the {@link Advice.Origin} annotation.
                     */
                    class ForOrigin implements OffsetMapping {

                        /**
                         * The delimiter character.
                         */
                        private static final char DELIMITER = '#';

                        /**
                         * The escape character.
                         */
                        private static final char ESCAPE = '\\';

                        /**
                         * The method name symbol.
                         */
                        private static final char METHOD_NAME = 'm';

                        /**
                         * The type name symbol.
                         */
                        private static final char TYPE_NAME = 't';

                        /**
                         * The descriptor symbol.
                         */
                        private static final char DESCRIPTOR = 'd';

                        /**
                         * The renderers to apply.
                         */
                        private final List<Renderer> renderers;

                        /**
                         * Creates a new offset mapping for an origin value.
                         *
                         * @param renderers The renderers to apply.
                         */
                        protected ForOrigin(List<Renderer> renderers) {
                            this.renderers = renderers;
                        }

                        /**
                         * Parses a pattern of an origin annotation.
                         *
                         * @param pattern The supplied pattern.
                         * @return An appropriate offset mapping.
                         */
                        protected static OffsetMapping parse(String pattern) {
                            if (pattern.equals(Origin.DEFAULT)) {
                                return new ForOrigin(Collections.<Renderer>singletonList(Renderer.ForStringRepresentation.INSTANCE));
                            } else {
                                List<Renderer> renderers = new ArrayList<Renderer>(pattern.length());
                                int from = 0;
                                for (int to = pattern.indexOf(DELIMITER); to != -1; to = pattern.indexOf(DELIMITER, from)) {
                                    if (to != 0 && pattern.charAt(to - 1) == ESCAPE && (to == 1 || pattern.charAt(to - 2) != ESCAPE)) {
                                        renderers.add(new Renderer.ForConstantValue(pattern.substring(from, Math.max(0, to - 1)) + DELIMITER));
                                        from = to + 1;
                                        continue;
                                    } else if (pattern.length() == to + 1) {
                                        throw new IllegalStateException("Missing sort descriptor for " + pattern + " at index " + to);
                                    }
                                    renderers.add(new Renderer.ForConstantValue(pattern.substring(from, to).replace("" + ESCAPE + ESCAPE, "" + ESCAPE)));
                                    switch (pattern.charAt(to + 1)) {
                                        case METHOD_NAME:
                                            renderers.add(Renderer.ForMethodName.INSTANCE);
                                            break;
                                        case TYPE_NAME:
                                            renderers.add(Renderer.ForTypeName.INSTANCE);
                                            break;
                                        case DESCRIPTOR:
                                            renderers.add(Renderer.ForDescriptor.INSTANCE);
                                            break;
                                        default:
                                            throw new IllegalStateException("Illegal sort descriptor " + pattern.charAt(to + 1) + " for " + pattern);
                                    }
                                    from = to + 2;
                                }
                                renderers.add(new Renderer.ForConstantValue(pattern.substring(from)));
                                return new ForOrigin(renderers);
                            }
                        }

                        @Override
                        public Target resolve(MethodDescription.InDefinedShape instrumentedMethod, TypeDescription enterType) {
                            StringBuilder stringBuilder = new StringBuilder();
                            for (Renderer renderer : renderers) {
                                stringBuilder.append(renderer.apply(instrumentedMethod));
                            }
                            return new Target.ForConstantPoolValue(stringBuilder.toString());
                        }

                        @Override
                        public boolean equals(Object object) {
                            if (this == object) return true;
                            if (object == null || getClass() != object.getClass()) return false;
                            ForOrigin forOrigin = (ForOrigin) object;
                            return renderers.equals(forOrigin.renderers);
                        }

                        @Override
                        public int hashCode() {
                            return renderers.hashCode();
                        }

                        @Override
                        public String toString() {
                            return "Advice.Dispatcher.Active.Resolved.OffsetMapping.ForOrigin{" +
                                    "renderers=" + renderers +
                                    '}';
                        }

                        /**
                         * A renderer for an origin pattern element.
                         */
                        protected interface Renderer {

                            /**
                             * Returns a string representation for this renderer.
                             *
                             * @param instrumentedMethod The method being rendered.
                             * @return The string representation.
                             */
                            String apply(MethodDescription.InDefinedShape instrumentedMethod);

                            /**
                             * A renderer for a method's internal name.
                             */
                            enum ForMethodName implements Renderer {

                                /**
                                 * The singleton instance.
                                 */
                                INSTANCE;

                                @Override
                                public String apply(MethodDescription.InDefinedShape instrumentedMethod) {
                                    return instrumentedMethod.getInternalName();
                                }

                                @Override
                                public String toString() {
                                    return "Advice.Dispatcher.Active.Resolved.OffsetMapping.ForOrigin.Renderer.ForMethodName." + name();
                                }
                            }

                            /**
                             * A renderer for a method declaring type's binary name.
                             */
                            enum ForTypeName implements Renderer {

                                /**
                                 * The singleton instance.
                                 */
                                INSTANCE;

                                @Override
                                public String apply(MethodDescription.InDefinedShape instrumentedMethod) {
                                    return instrumentedMethod.getDeclaringType().getName();
                                }

                                @Override
                                public String toString() {
                                    return "Advice.Dispatcher.Active.Resolved.OffsetMapping.ForOrigin.Renderer.ForTypeName." + name();
                                }
                            }

                            /**
                             * A renderer for a method descriptor.
                             */
                            enum ForDescriptor implements Renderer {

                                /**
                                 * The singleton instance.
                                 */
                                INSTANCE;

                                @Override
                                public String apply(MethodDescription.InDefinedShape instrumentedMethod) {
                                    return instrumentedMethod.getDescriptor();
                                }

                                @Override
                                public String toString() {
                                    return "Advice.Dispatcher.Active.Resolved.OffsetMapping.ForOrigin.Renderer.ForDescriptor." + name();
                                }
                            }

                            /**
                             * A renderer for a method's {@link Object#toString()} representation.
                             */
                            enum ForStringRepresentation implements Renderer {

                                /**
                                 * The singleton instance.
                                 */
                                INSTANCE;

                                @Override
                                public String apply(MethodDescription.InDefinedShape instrumentedMethod) {
                                    return instrumentedMethod.toString();
                                }

                                @Override
                                public String toString() {
                                    return "Advice.Dispatcher.Active.Resolved.OffsetMapping.ForOrigin.Renderer.ForStringRepresentation." + name();
                                }
                            }

                            /**
                             * A renderer for a constant value.
                             */
                            class ForConstantValue implements Renderer {

                                /**
                                 * The constant value.
                                 */
                                private final String value;

                                /**
                                 * Creates a new renderer for a constant value.
                                 *
                                 * @param value The constant value.
                                 */
                                protected ForConstantValue(String value) {
                                    this.value = value;
                                }

                                @Override
                                public String apply(MethodDescription.InDefinedShape instrumentedMethod) {
                                    return value;
                                }

                                @Override
                                public boolean equals(Object object) {
                                    if (this == object) return true;
                                    if (object == null || getClass() != object.getClass()) return false;
                                    ForConstantValue that = (ForConstantValue) object;
                                    return value.equals(that.value);
                                }

                                @Override
                                public int hashCode() {
                                    return value.hashCode();
                                }

                                @Override
                                public String toString() {
                                    return "Advice.Dispatcher.Active.Resolved.OffsetMapping.ForOrigin.Renderer.ForConstantValue{" +
                                            "value='" + value + '\'' +
                                            '}';
                                }
                            }
                        }

                        /**
                         * A factory for a method origin.
                         */
                        protected enum Factory implements OffsetMapping.Factory {

                            /**
                             * The singleton instance.
                             */
                            INSTANCE;

                            @Override
                            public OffsetMapping make(ParameterDescription.InDefinedShape parameterDescription) {
                                AnnotationDescription.Loadable<Origin> origin = parameterDescription.getDeclaredAnnotations().ofType(Origin.class);
                                if (origin == null) {
                                    return UNDEFINED;
                                } else {
                                    if (!parameterDescription.getType().asErasure().isAssignableFrom(String.class)) {
                                        throw new IllegalStateException("Non-String type " + parameterDescription + " for origin annotation");
                                    }
                                    return ForOrigin.parse(origin.loadSilent().value());
                                }
                            }

                            @Override
                            public String toString() {
                                return "Advice.Dispatcher.Active.Resolved.OffsetMapping.ForOrigin.Factory." + name();
                            }
                        }
                    }

                    /**
                     * An offset mapping for a parameter where assignments are fully ignored and that always return the parameter type's default value.
                     */
                    enum ForIgnored implements OffsetMapping, Factory {

                        /**
                         * The singleton instance.
                         */
                        INSTANCE;

                        @Override
                        public Target resolve(MethodDescription.InDefinedShape instrumentedMethod, TypeDescription enterType) {
                            return Target.ForDefaultValue.INSTANCE;
                        }

                        @Override
                        public OffsetMapping make(ParameterDescription.InDefinedShape parameterDescription) {
                            return parameterDescription.getDeclaredAnnotations().isAnnotationPresent(Ignored.class)
                                    ? this
                                    : UNDEFINED;
                        }

                        @Override
                        public String toString() {
                            return "Advice.Dispatcher.Active.Resolved.OffsetMapping.ForIgnored." + name();
                        }
                    }

                    /**
                     * An offset mapping that provides access to the value that is returned by the enter advise.
                     */
                    enum ForEnterValue implements OffsetMapping {

                        /**
                         * Enables writing to the mapped offset.
                         */
                        WRITABLE(false),

                        /**
                         * Only allows for reading the mapped offset.
                         */
                        READ_ONLY(true);

                        /**
                         * Determines if the parameter is to be treated as read-only.
                         */
                        private final boolean readOnly;

                        /**
                         * Creates a new offset mapping for an enter value.
                         *
                         * @param readOnly Determines if the parameter is to be treated as read-only.
                         */
                        ForEnterValue(boolean readOnly) {
                            this.readOnly = readOnly;
                        }

                        /**
                         * Resolves an offset mapping for an enter value.
                         *
                         * @param readOnly {@code true} if the value is to be treated as read-only.
                         * @return An appropriate offset mapping.
                         */
                        public static OffsetMapping of(boolean readOnly) {
                            return readOnly
                                    ? READ_ONLY
                                    : WRITABLE;
                        }

                        @Override
                        public Target resolve(MethodDescription.InDefinedShape instrumentedMethod, TypeDescription enterType) {
                            return readOnly
                                    ? new Target.ForReadOnlyParameter(instrumentedMethod.getStackSize())
                                    : new Target.ForParameter(instrumentedMethod.getStackSize());
                        }

                        @Override
                        public String toString() {
                            return "Advice.Dispatcher.Active.Resolved.OffsetMapping.ForEnterValue." + name();
                        }

                        /**
                         * A factory for creating a {@link ForEnterValue} offset mapping.
                         */
                        protected static class Factory implements OffsetMapping.Factory {

                            /**
                             * The supplied type of the enter method.
                             */
                            private final TypeDescription enterType;

                            /**
                             * Creates a new factory for creating a {@link ForEnterValue} offset mapping.
                             *
                             * @param enterType The supplied type of the enter method.
                             */
                            protected Factory(TypeDescription enterType) {
                                this.enterType = enterType;
                            }

                            @Override
                            public OffsetMapping make(ParameterDescription.InDefinedShape parameterDescription) {
                                AnnotationDescription.Loadable<Enter> annotation = parameterDescription.getDeclaredAnnotations().ofType(Enter.class);
                                if (annotation != null) {
                                    boolean readOnly = annotation.loadSilent().readOnly();
                                    if (!readOnly && !enterType.equals(parameterDescription.getType().asErasure())) {
                                        throw new IllegalStateException("read-only type of " + parameterDescription + " does not equal " + enterType);
                                    } else if (readOnly && !enterType.isAssignableTo(parameterDescription.getType().asErasure())) {
                                        throw new IllegalStateException("Cannot assign the type of " + parameterDescription + " to supplied type " + enterType);
                                    }
                                    return ForEnterValue.of(readOnly);
                                } else {
                                    return UNDEFINED;
                                }
                            }

                            @Override
                            public boolean equals(Object other) {
                                return this == other || !(other == null || getClass() != other.getClass())
                                        && enterType.equals(((Factory) other).enterType);
                            }

                            @Override
                            public int hashCode() {
                                return enterType.hashCode();
                            }

                            @Override
                            public String toString() {
                                return "Advice.Dispatcher.Active.Resolved.OffsetMapping.ForEnterValue.Factory{" +
                                        "enterType=" + enterType +
                                        '}';
                            }
                        }
                    }

                    /**
                     * An offset mapping that provides access to the value that is returned by the instrumented method.
                     */
                    class ForReturnValue implements OffsetMapping {

                        /**
                         * Determines if the parameter is to be treated as read-only.
                         */
                        private final boolean readOnly;

                        /**
                         * The type that the advise method expects for the {@code this} reference.
                         */
                        private final TypeDescription targetType;

                        /**
                         * Creates an offset mapping for accessing the return type of the instrumented method.
                         *
                         * @param readOnly   Determines if the parameter is to be treated as read-only.
                         * @param targetType The expected target type of the return type.
                         */
                        protected ForReturnValue(boolean readOnly, TypeDescription targetType) {
                            this.readOnly = readOnly;
                            this.targetType = targetType;
                        }

                        @Override
                        public Target resolve(MethodDescription.InDefinedShape instrumentedMethod, TypeDescription enterType) {
                            if (!readOnly && !instrumentedMethod.getReturnType().asErasure().equals(targetType)) {
                                throw new IllegalStateException("read-only return type of " + instrumentedMethod + " is not equal to " + targetType);
                            } else if (readOnly && !instrumentedMethod.getReturnType().asErasure().isAssignableTo(targetType)) {
                                throw new IllegalStateException("Cannot assign return type of " + instrumentedMethod + " to " + targetType);
                            }
                            return readOnly
                                    ? new Target.ForReadOnlyParameter(instrumentedMethod.getStackSize() + enterType.getStackSize().getSize())
                                    : new Target.ForParameter(instrumentedMethod.getStackSize() + enterType.getStackSize().getSize());
                        }

                        @Override
                        public boolean equals(Object other) {
                            if (this == other) return true;
                            if (other == null || getClass() != other.getClass()) return false;
                            ForReturnValue that = (ForReturnValue) other;
                            return readOnly == that.readOnly && targetType.equals(that.targetType);
                        }

                        @Override
                        public int hashCode() {
                            return (readOnly ? 1 : 0) + 31 * targetType.hashCode();
                        }

                        @Override
                        public String toString() {
                            return "Advice.Dispatcher.Active.Resolved.OffsetMapping.ForReturnValue{" +
                                    "readOnly=" + readOnly +
                                    ", targetType=" + targetType +
                                    '}';
                        }

                        /**
                         * A factory for creating a {@link ForReturnValue} offset mapping.
                         */
                        protected enum Factory implements OffsetMapping.Factory {

                            /**
                             * The singleton instance.
                             */
                            INSTANCE;

                            @Override
                            public OffsetMapping make(ParameterDescription.InDefinedShape parameterDescription) {
                                AnnotationDescription.Loadable<Return> annotation = parameterDescription.getDeclaredAnnotations().ofType(Return.class);
                                return annotation == null
                                        ? UNDEFINED
                                        : new ForReturnValue(annotation.loadSilent().readOnly(), parameterDescription.getType().asErasure());
                            }

                            @Override
                            public String toString() {
                                return "Advice.Dispatcher.Active.Resolved.OffsetMapping.ForReturnValue.Factory." + name();
                            }
                        }
                    }

                    /**
                     * An offset mapping for accessing a {@link Throwable} of the instrumented method.
                     */
                    enum ForThrowable implements OffsetMapping, Factory {

                        /**
                         * The singleton instance.
                         */
                        INSTANCE;

                        @Override
                        public OffsetMapping make(ParameterDescription.InDefinedShape parameterDescription) {
                            if (parameterDescription.getDeclaredAnnotations().isAnnotationPresent(Thrown.class)) {
                                if (!parameterDescription.getType().represents(Throwable.class)) {
                                    throw new IllegalStateException("Parameter must be of type Throwable for " + parameterDescription);
                                }
                                return this;
                            } else {
                                return UNDEFINED;
                            }
                        }

                        @Override
                        public Target resolve(MethodDescription.InDefinedShape instrumentedMethod, TypeDescription enterType) {
                            return new Target.ForParameter(instrumentedMethod.getStackSize()
                                    + enterType.getStackSize().getSize()
                                    + instrumentedMethod.getReturnType().getStackSize().getSize());
                        }

                        @Override
                        public String toString() {
                            return "Advice.Dispatcher.Active.Resolved.OffsetMapping.ForThrowable." + name();
                        }
                    }

                    /**
                     * Represents a factory that throws an exception for a given set of illegal parameter annotations.
                     */
                    class Illegal implements Factory {

                        /**
                         * The set of illegal annotations.
                         */
                        private final List<? extends Class<? extends Annotation>> annotations;

                        /**
                         * Creates a new factory for restricting the use of illegal annotation types.
                         *
                         * @param annotation The set of illegal annotations.
                         */
                        //@SafeVarargs
                        protected Illegal(Class<? extends Annotation>... annotation) {
                            this(Arrays.asList(annotation));
                        }

                        /**
                         * Creates a new factory for restricting the use of illegal annotation types.
                         *
                         * @param annotations The set of illegal annotations.
                         */
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

                        @Override
                        public boolean equals(Object other) {
                            if (this == other) return true;
                            if (other == null || getClass() != other.getClass()) return false;
                            Illegal illegal = (Illegal) other;
                            return annotations.equals(illegal.annotations);
                        }

                        @Override
                        public int hashCode() {
                            return annotations.hashCode();
                        }

                        @Override
                        public String toString() {
                            return "Advice.Dispatcher.Active.Resolved.OffsetMapping.Illegal{" +
                                    "annotations=" + annotations +
                                    '}';
                        }
                    }
                }

                /**
                 * A resolved dispatcher for implementing method enter advise.
                 */
                protected static class ForMethodEnter extends Resolved implements Dispatcher.Resolved.ForMethodEnter {

                    /**
                     * The {@code suppress} property of the {@link OnMethodEnter} type.
                     */
                    private static final MethodDescription.InDefinedShape SUPPRESS;

                    /*
                     * Extracts the suppress property.
                     */
                    static {
                        SUPPRESS = new TypeDescription.ForLoadedType(OnMethodEnter.class).getDeclaredMethods().filter(named("suppress")).getOnly();
                    }

                    /**
                     * Creates a new resolved dispatcher for implementing method enter advise.
                     *
                     * @param adviseMethod The represented advise method.
                     */
                    @SuppressWarnings("all") // In absence of @SafeVarargs for Java 6
                    protected ForMethodEnter(MethodDescription.InDefinedShape adviseMethod) {
                        super(adviseMethod,
                                OffsetMapping.ForParameter.Factory.INSTANCE,
                                OffsetMapping.ForThisReference.Factory.INSTANCE,
                                OffsetMapping.ForField.Factory.INSTANCE,
                                OffsetMapping.ForOrigin.Factory.INSTANCE,
                                OffsetMapping.ForIgnored.INSTANCE,
                                new OffsetMapping.Illegal(Thrown.class, Enter.class, Return.class));
                    }

                    @Override
                    public TypeDescription getEnterType() {
                        return adviseMethod.getReturnType().asErasure();
                    }

                    @Override
                    protected MethodVisitor apply(MethodVisitor methodVisitor,
                                                  FrameTranslator frameTranslator,
                                                  MethodDescription.InDefinedShape instrumentedMethod) {
                        Map<Integer, OffsetMapping.Target> offsetMappings = new HashMap<Integer, OffsetMapping.Target>();
                        for (Map.Entry<Integer, OffsetMapping> entry : this.offsetMappings.entrySet()) {
                            offsetMappings.put(entry.getKey(), entry.getValue().resolve(instrumentedMethod, TypeDescription.VOID));
                        }
                        return new CodeTranslationVisitor.ForMethodEnter(methodVisitor,
                                frameTranslator,
                                instrumentedMethod,
                                adviseMethod,
                                offsetMappings,
                                adviseMethod.getDeclaredAnnotations().ofType(OnMethodEnter.class).getValue(SUPPRESS, TypeDescription.class));
                    }

                    @Override
                    public String toString() {
                        return "Advice.Dispatcher.Active.Resolved.ForMethodEnter{" +
                                "adviseMethod=" + adviseMethod +
                                ", offsetMappings=" + offsetMappings +
                                '}';
                    }
                }

                /**
                 * A resolved dispatcher for implementing method exit advise.
                 */
                protected static class ForMethodExit extends Resolved implements Dispatcher.Resolved.ForMethodExit {

                    /**
                     * The {@code suppress} method of the {@link OnMethodExit} annotation.
                     */
                    private static final MethodDescription.InDefinedShape SUPPRESS;

                    /*
                     * Extracts the suppress method.
                     */
                    static {
                        SUPPRESS = new TypeDescription.ForLoadedType(OnMethodExit.class).getDeclaredMethods().filter(named("suppress")).getOnly();
                    }

                    /**
                     * The additional stack size to consider when accessing the local variable array.
                     */
                    private final TypeDescription enterType;

                    /**
                     * Creates a new resolved dispatcher for implementing method exit advise.
                     *
                     * @param adviseMethod The represented advise method.
                     * @param enterType    The type of the value supplied by the enter advise method or a description of {@code void} if
                     *                     no such value exists.
                     */
                    @SuppressWarnings("all") // In absence of @SafeVarargs for Java 6
                    protected ForMethodExit(MethodDescription.InDefinedShape adviseMethod, TypeDescription enterType) {
                        super(adviseMethod,
                                OffsetMapping.ForParameter.Factory.INSTANCE,
                                OffsetMapping.ForThisReference.Factory.INSTANCE,
                                OffsetMapping.ForField.Factory.INSTANCE,
                                OffsetMapping.ForOrigin.Factory.INSTANCE,
                                OffsetMapping.ForIgnored.INSTANCE,
                                new OffsetMapping.ForEnterValue.Factory(enterType),
                                OffsetMapping.ForReturnValue.Factory.INSTANCE,
                                adviseMethod.getDeclaredAnnotations().ofType(OnMethodExit.class).loadSilent().onThrowable()
                                        ? OffsetMapping.ForThrowable.INSTANCE
                                        : new OffsetMapping.Illegal(Thrown.class));
                        this.enterType = enterType;
                    }

                    @Override
                    public boolean isSkipThrowable() {
                        return !adviseMethod.getDeclaredAnnotations().ofType(OnMethodExit.class).loadSilent().onThrowable();
                    }

                    @Override
                    protected MethodVisitor apply(MethodVisitor methodVisitor, FrameTranslator frameTranslator, MethodDescription.InDefinedShape instrumentedMethod) {
                        Map<Integer, OffsetMapping.Target> offsetMappings = new HashMap<Integer, OffsetMapping.Target>();
                        for (Map.Entry<Integer, OffsetMapping> entry : this.offsetMappings.entrySet()) {
                            offsetMappings.put(entry.getKey(), entry.getValue().resolve(instrumentedMethod, enterType));
                        }
                        return new CodeTranslationVisitor.ForMethodExit(methodVisitor,
                                frameTranslator,
                                instrumentedMethod,
                                adviseMethod,
                                offsetMappings,
                                adviseMethod.getDeclaredAnnotations().ofType(OnMethodExit.class).getValue(SUPPRESS, TypeDescription.class),
                                enterType);
                    }

                    @Override
                    public boolean equals(Object other) {
                        return this == other || !(other == null || getClass() != other.getClass())
                                && super.equals(other)
                                && enterType == ((Resolved.ForMethodExit) other).enterType;
                    }

                    @Override
                    public int hashCode() {
                        int result = super.hashCode();
                        result = 31 * result + enterType.hashCode();
                        return result;
                    }

                    @Override
                    public String toString() {
                        return "Advice.Dispatcher.Active.Resolved.ForMethodExit{" +
                                "adviseMethod=" + adviseMethod +
                                ", offsetMappings=" + offsetMappings +
                                ", enterType=" + enterType +
                                '}';
                    }
                }
            }

            /**
             * A producer for a default return value if this is applicable.
             */
            interface ReturnValueProducer {

                /**
                 * Sets a default return value for an advised method.
                 *
                 * @param methodVisitor The instrumented method's method visitor.
                 */
                void makeDefault(MethodVisitor methodVisitor);
            }

            /**
             * A visitor for translating an advise method's byte code for inlining into the instrumented method.
             */
            protected abstract static class CodeTranslationVisitor extends MethodVisitor implements ReturnValueProducer {

                /**
                 * Indicates that an annotation should not be read.
                 */
                private static final AnnotationVisitor IGNORE_ANNOTATION = null;

                /**
                 * The frame translator to use.
                 */
                protected final FrameTranslator.Bound frameTranslator;

                /**
                 * The instrumented method.
                 */
                protected final MethodDescription.InDefinedShape instrumentedMethod;

                /**
                 * The advise method.
                 */
                protected final MethodDescription.InDefinedShape adviseMethod;

                /**
                 * A mapping of offsets to resolved target offsets in the instrumented method.
                 */
                private final Map<Integer, Resolved.OffsetMapping.Target> offsetMappings;

                /**
                 * A handler for optionally suppressing exceptions.
                 */
                private final SuppressionHandler suppressionHandler;

                /**
                 * A label indicating the end of the advise byte code.
                 */
                protected final Label endOfMethod;

                /**
                 * Creates a new code translation visitor.
                 *
                 * @param methodVisitor      A method visitor for writing the instrumented method's byte code.
                 * @param frameTranslator    The frame translator to use.
                 * @param instrumentedMethod The instrumented method.
                 * @param adviseMethod       The advise method.
                 * @param offsetMappings     A mapping of offsets to resolved target offsets in the instrumented method.
                 * @param throwableType      A throwable type to be suppressed or {@link NoSuppression} if no suppression should be applied.
                 */
                protected CodeTranslationVisitor(MethodVisitor methodVisitor,
                                                 FrameTranslator.Bound frameTranslator,
                                                 MethodDescription.InDefinedShape instrumentedMethod,
                                                 MethodDescription.InDefinedShape adviseMethod,
                                                 Map<Integer, Resolved.OffsetMapping.Target> offsetMappings,
                                                 TypeDescription throwableType) {
                    super(Opcodes.ASM5, methodVisitor);
                    this.frameTranslator = frameTranslator;
                    this.instrumentedMethod = instrumentedMethod;
                    this.adviseMethod = adviseMethod;
                    this.offsetMappings = offsetMappings;
                    suppressionHandler = throwableType.represents(NoSuppression.class)
                            ? SuppressionHandler.NoOp.INSTANCE
                            : new SuppressionHandler.Suppressing(throwableType);
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
                    suppressionHandler.onStart(mv, frameTranslator, endOfMethod);
                }

                @Override
                public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
                    frameTranslator.translateFrame(mv, type, nLocal, local, nStack, stack);
                }

                @Override
                public void visitLineNumber(int line, Label start) {
                    /* do nothing */
                }

                @Override
                public void visitEnd() {
                    mv.visitLabel(endOfMethod);
                    frameTranslator.injectCompletionFrame(mv);
                    suppressionHandler.onEnd(mv, frameTranslator, this);
                }

                @Override
                public void visitMaxs(int maxStack, int maxLocals) {
                    frameTranslator.recordMaxima(maxStack, maxLocals);
                }

                @Override
                public void visitVarInsn(int opcode, int offset) {
                    Resolved.OffsetMapping.Target target = offsetMappings.get(offset);
                    if (target != null) {
                        target.apply(mv, opcode);
                    } else {
                        mv.visitVarInsn(opcode, adjust(offset + instrumentedMethod.getStackSize() - adviseMethod.getStackSize()));
                    }
                }

                /**
                 * Adjusts the offset of a variable instruction within the advise method such that no arguments to
                 * the instrumented method are overridden.
                 *
                 * @param offset The original offset.
                 * @return The adjusted offset.
                 */
                protected abstract int adjust(int offset);

                @Override
                public abstract void visitInsn(int opcode);

                /**
                 * A suppression handler for optionally suppressing exceptions.
                 */
                protected interface SuppressionHandler {

                    /**
                     * Invoked at the start of a method.
                     *
                     * @param methodVisitor   The method visitor of the instrumented method.
                     * @param frameTranslator The frame translator to use.
                     * @param endOfMethod     A label indicating the end of the method.
                     */
                    void onStart(MethodVisitor methodVisitor, FrameTranslator.Bound frameTranslator, Label endOfMethod);

                    /**
                     * Invoked at the end of a method.
                     *
                     * @param methodVisitor       The method visitor of the instrumented method.
                     * @param frameTranslator     The frame translator to use.
                     * @param returnValueProducer A producer for defining a default return value of the advised method.
                     */
                    void onEnd(MethodVisitor methodVisitor, FrameTranslator.Bound frameTranslator, ReturnValueProducer returnValueProducer);

                    /**
                     * A non-operational suppression handler that does not suppress any method.
                     */
                    enum NoOp implements SuppressionHandler {

                        /**
                         * The singleton instance.
                         */
                        INSTANCE;

                        @Override
                        public void onStart(MethodVisitor methodVisitor, FrameTranslator.Bound frameTranslator, Label endOfMethod) {
                            /* do nothing */
                        }

                        @Override
                        public void onEnd(MethodVisitor methodVisitor, FrameTranslator.Bound frameTranslator, ReturnValueProducer returnValueProducer) {
                            /* do nothing */
                        }

                        @Override
                        public String toString() {
                            return "Advice.Dispatcher.Active.CodeTranslationVisitor.SuppressionHandler.NoOp." + name();
                        }
                    }

                    /**
                     * A suppression handler that suppresses a given throwable type.
                     */
                    class Suppressing implements SuppressionHandler {

                        /**
                         * The suppressed throwable type.
                         */
                        private final TypeDescription throwableType;

                        /**
                         * A label indicating the start of the method.
                         */
                        private final Label startOfMethod;

                        /**
                         * A label indicating the exception handler.
                         */
                        private final Label handler;

                        /**
                         * Creates a new suppressing suppression handler.
                         *
                         * @param throwableType The suppressed throwable type.
                         */
                        protected Suppressing(TypeDescription throwableType) {
                            this.throwableType = throwableType;
                            startOfMethod = new Label();
                            handler = new Label();
                        }

                        @Override
                        public void onStart(MethodVisitor methodVisitor, FrameTranslator.Bound frameTranslator, Label endOfMethod) {
                            methodVisitor.visitTryCatchBlock(startOfMethod, endOfMethod, handler, throwableType.getInternalName());
                            methodVisitor.visitLabel(startOfMethod);
                        }

                        @Override
                        public void onEnd(MethodVisitor methodVisitor, FrameTranslator.Bound frameTranslator, ReturnValueProducer returnValueProducer) {
                            Label endOfHandler = new Label();
                            methodVisitor.visitJumpInsn(Opcodes.GOTO, endOfHandler);
                            methodVisitor.visitLabel(handler);
                            frameTranslator.injectHandlerFrame(methodVisitor);
                            methodVisitor.visitInsn(Opcodes.POP);
                            returnValueProducer.makeDefault(methodVisitor);
                            methodVisitor.visitLabel(endOfHandler);
                            frameTranslator.injectCompletionFrame(methodVisitor);
                        }

                        @Override
                        public boolean equals(Object object) {
                            if (this == object) return true;
                            if (object == null || getClass() != object.getClass()) return false;
                            Suppressing that = (Suppressing) object;
                            return throwableType.equals(that.throwableType);
                        }

                        @Override
                        public int hashCode() {
                            return throwableType.hashCode();
                        }

                        @Override
                        public String toString() {
                            return "Advice.Dispatcher.Active.CodeTranslationVisitor.SuppressionHandler.Suppressing{" +
                                    "throwableType=" + throwableType +
                                    ", startOfMethod=" + startOfMethod +
                                    ", handler=" + handler +
                                    '}';
                        }
                    }
                }

                /**
                 * A code translation visitor that retains the return value of the represented advise method.
                 */
                protected static class ForMethodEnter extends CodeTranslationVisitor {

                    /**
                     * Creates a new code translation visitor that retains the return value of the enter advise.
                     *
                     * @param methodVisitor      A method visitor for writing the instrumented method's byte code.
                     * @param frameTranslator    The frame translator to use.
                     * @param instrumentedMethod The instrumented method.
                     * @param adviseMethod       The advise method.
                     * @param offsetMappings     A mapping of offsets of the advise methods to their corresponding offsets in the instrumented method.
                     * @param throwableType      A throwable type to be suppressed or {@link NoSuppression} if no suppression should be applied.
                     */
                    protected ForMethodEnter(MethodVisitor methodVisitor,
                                             FrameTranslator frameTranslator,
                                             MethodDescription.InDefinedShape instrumentedMethod,
                                             MethodDescription.InDefinedShape adviseMethod,
                                             Map<Integer, Resolved.OffsetMapping.Target> offsetMappings,
                                             TypeDescription throwableType) {
                        super(methodVisitor, frameTranslator.bindEntry(adviseMethod), instrumentedMethod, adviseMethod, offsetMappings, throwableType);
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

                    @Override
                    public void makeDefault(MethodVisitor methodVisitor) {
                        if (adviseMethod.getReturnType().represents(boolean.class)
                                || adviseMethod.getReturnType().represents(byte.class)
                                || adviseMethod.getReturnType().represents(short.class)
                                || adviseMethod.getReturnType().represents(char.class)
                                || adviseMethod.getReturnType().represents(int.class)) {
                            methodVisitor.visitInsn(Opcodes.ICONST_0);
                            methodVisitor.visitVarInsn(Opcodes.ISTORE, instrumentedMethod.getStackSize());
                        } else if (adviseMethod.getReturnType().represents(long.class)) {
                            methodVisitor.visitInsn(Opcodes.LCONST_0);
                            methodVisitor.visitVarInsn(Opcodes.LSTORE, instrumentedMethod.getStackSize());
                        } else if (adviseMethod.getReturnType().represents(float.class)) {
                            methodVisitor.visitInsn(Opcodes.FCONST_0);
                            methodVisitor.visitVarInsn(Opcodes.FSTORE, instrumentedMethod.getStackSize());
                        } else if (adviseMethod.getReturnType().represents(double.class)) {
                            methodVisitor.visitInsn(Opcodes.DCONST_0);
                            methodVisitor.visitVarInsn(Opcodes.DSTORE, instrumentedMethod.getStackSize());
                        } else if (!adviseMethod.getReturnType().represents(void.class)) {
                            methodVisitor.visitInsn(Opcodes.ACONST_NULL);
                            methodVisitor.visitVarInsn(Opcodes.ASTORE, instrumentedMethod.getStackSize());
                        }
                    }

                    @Override
                    public String toString() {
                        return "Advice.Dispatcher.Active.CodeTranslationVisitor.ForMethodEnter{" +
                                "instrumentedMethod=" + instrumentedMethod +
                                ", adviseMethod=" + adviseMethod +
                                '}';
                    }
                }

                /**
                 * A code translation visitor that discards the return value of the represented advise method.
                 */
                protected static class ForMethodExit extends CodeTranslationVisitor {

                    /**
                     * The size of the exception slot for the instrumented method's potential exception.
                     */
                    private static final int EXCEPTION_SIZE = 1;

                    /**
                     * The type returned by the method method entry advice if any.
                     */
                    private final TypeDescription enterType;

                    /**
                     * Creates a new code translation visitor that retains the return value of the enter advise.
                     *
                     * @param methodVisitor      A method visitor for writing the instrumented method's byte code.
                     * @param frameTranslator    The frame translator to use.
                     * @param instrumentedMethod The instrumented method.
                     * @param adviseMethod       The advise method.
                     * @param offsetMappings     A mapping of offsets of the advise methods to their corresponding offsets in the instrumented method.
                     * @param throwableType      A throwable type to be suppressed or {@link NoSuppression} if no suppression should be applied.
                     * @param enterType          The type returned by the method method entry advice if any.
                     */
                    protected ForMethodExit(MethodVisitor methodVisitor,
                                            FrameTranslator frameTranslator,
                                            MethodDescription.InDefinedShape instrumentedMethod,
                                            MethodDescription.InDefinedShape adviseMethod,
                                            Map<Integer, Resolved.OffsetMapping.Target> offsetMappings,
                                            TypeDescription throwableType,
                                            TypeDescription enterType) {
                        super(methodVisitor,
                                frameTranslator.bindExit(adviseMethod, enterType),
                                instrumentedMethod,
                                adviseMethod,
                                offsetMappings,
                                throwableType);
                        this.enterType = enterType;
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
                        return offset + instrumentedMethod.getReturnType().getStackSize().getSize() + enterType.getStackSize().getSize() + EXCEPTION_SIZE;
                    }

                    @Override
                    public void makeDefault(MethodVisitor methodVisitor) {
                        /* do nothing */
                    }

                    @Override
                    public String toString() {
                        return "Advice.Dispatcher.Active.CodeTranslationVisitor.ForMethodExit{" +
                                "instrumentedMethod=" + instrumentedMethod +
                                ", adviseMethod=" + adviseMethod +
                                ", enterType=" + enterType +
                                '}';
                    }
                }
            }
        }
    }

    /**
     * <p>
     * Indicates that this method should be inlined before the matched method is invoked. Any class must declare
     * at most one method with this annotation. The annotated method must be static. When instrumenting constructors,
     * the {@code this} values can only be accessed for writing fields but not for reading fields or invoking methods.
     * </p>
     * <p>
     * The annotated method can return a value that is made accessible to another method annotated by {@link OnMethodExit}.
     * </p>
     *
     * @see Advice
     * @see Argument
     * @see This
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface OnMethodEnter {

        /**
         * Indicates that this advice should suppress any {@link Throwable} type being thrown during the advice's execution.
         *
         * @return The type of {@link Throwable} to suppress.
         */
        Class<? extends Throwable> suppress() default NoSuppression.class;
    }

    /**
     * <p>
     * Indicates that this method should be inlined before the matched method is invoked. Any class must declare
     * at most one method with this annotation. The annotated method must be static.
     * </p>
     * <p>
     * The annotated method can imply to not be invoked when the instrumented method terminates exceptionally by
     * setting the {@link OnMethodExit#onThrowable()} property.
     * </p>
     *
     * @see Advice
     * @see Argument
     * @see This
     * @see Enter
     * @see Return
     * @see Thrown
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface OnMethodExit {

        /**
         * Indicates that the advise method should also be called when a method terminates exceptionally.
         *
         * @return {@code true} if the advise method should be invoked when a method terminates exceptionally.
         */
        boolean onThrowable() default true;

        /**
         * Indicates that this advice should suppress any {@link Throwable} type being thrown during the advice's execution.
         *
         * @return The type of {@link Throwable} to suppress.
         */
        Class<? extends Throwable> suppress() default NoSuppression.class;
    }

    /**
     * Indicates that the annotated parameter should be mapped to the parameter with index {@link Argument#value()} of
     * the instrumented method.
     *
     * @see Advice
     * @see OnMethodEnter
     * @see OnMethodExit
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public @interface Argument {

        /**
         * Returns the index of the mapped parameter.
         *
         * @return The index of the mapped parameter.
         */
        int value();

        /**
         * Indicates if it is possible to write to this parameter. If this property is set to {@code false}, the annotated
         * type must be equal to the parameter of the instrumented method. If this property is set to {@code true}, the
         * annotated parameter can be any super type of the instrumented methods parameter.
         *
         * @return {@code true} if this parameter is read-only.
         */
        boolean readOnly() default true;
    }

    /**
     * <p>
     * Indicates that the annotated parameter should be mapped to the {@code this} reference of the instrumented method.
     * </p>
     * <p>
     * <b>Important</b>: Parameters with this option must not be used when from a constructor in combination with
     * {@link OnMethodEnter} where the {@code this} reference is not available.
     * </p>
     *
     * @see Advice
     * @see OnMethodEnter
     * @see OnMethodExit
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public @interface This {

        /**
         * Indicates if it is possible to write to this parameter. If this property is set to {@code false}, the annotated
         * type must be equal to the parameter of the instrumented method. If this property is set to {@code true}, the
         * annotated parameter can be any super type of the instrumented methods parameter.
         *
         * @return {@code true} if this parameter is read-only.
         */
        boolean readOnly() default true;
    }

    /**
     * Indicates that the annotated parameter should be mapped to a field in the scope of the instrumented method. All
     * field references are always <i>read-only</i>.
     * <p>
     * <b>Important</b>: Parameters with this option must not be used when from a constructor in combination with
     * {@link OnMethodEnter} and a non-static field where the {@code this} reference is not available.
     * </p>
     *
     * @see Advice
     * @see OnMethodEnter
     * @see OnMethodExit
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public @interface FieldValue {

        /**
         * Returns the name of the field.
         *
         * @return The name of the field.
         */
        String value();

        /**
         * Returns the type that declares the field that should be mapped to the annotated parameter. If this property
         * is set to {@code void}, the field is looked up implicitly within the instrumented class's class hierarchy.
         *
         * @return The type that declares the field or {@code void} if this type should be determined implicitly.
         */
        Class<?> declaringType() default void.class;
    }

    /**
     * Indicates that the annotated parameter should be mapped to a string representation of the instrumented method.
     *
     * @see Advice
     * @see OnMethodEnter
     * @see OnMethodExit
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public @interface Origin {

        /**
         * Indicates that the origin string should be indicated by the {@link Object#toString()} representation of the instrumented method.
         */
        String DEFAULT = "";

        /**
         * Returns the pattern the annotated parameter should be assigned. By default, the {@link Origin#toString()} representation
         * of the method is assigned. Alternatively, a pattern can be assigned where {@code #t} inserts the method's declaring type,
         * {@code #m} inserts the name of the method ({@code <init>} for constructors and {@code <clinit>} for static initializers)
         * and {@code #d} for the method's descriptor. Any other {@code #} character must be escaped by {@code \} which can be
         * escaped by itself.
         *
         * @return The pattern the annotated parameter should be assigned.
         */
        String value() default DEFAULT;
    }

    /**
     * Indicates that the annotated parameter should always return a default value (i.e. {@code 0} for numeric values, {@code false}
     * for {@code boolean} types and {@code null} for reference types).
     *
     * @see Advice
     * @see OnMethodEnter
     * @see OnMethodExit
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public @interface Ignored {
        /* empty */
    }

    /**
     * Indicates that the annotated parameter should be mapped to the value that is returned by the advise method that is annotated
     * by {@link OnMethodEnter}.
     *
     * @see Advice
     * @see OnMethodExit
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public @interface Enter {

        /**
         * Indicates if it is possible to write to this parameter. If this property is set to {@code false}, the annotated
         * type must be equal to the parameter of the instrumented method. If this property is set to {@code true}, the
         * annotated parameter can be any super type of the instrumented methods parameter.
         *
         * @return {@code true} if this parameter is read-only.
         */
        boolean readOnly() default true;
    }

    /**
     * Indicates that the annotated parameter should be mapped to the return value of the instrumented method. If the instrumented
     * method terminates exceptionally, the type's default value is assigned to the parameter, i.e. {@code 0} for numeric types
     * and {@code null} for reference types.
     *
     * @see Advice
     * @see OnMethodExit
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public @interface Return {

        /**
         * Indicates if it is possible to write to this parameter. If this property is set to {@code false}, the annotated
         * type must be equal to the parameter of the instrumented method. If this property is set to {@code true}, the
         * annotated parameter can be any super type of the instrumented methods parameter.
         *
         * @return {@code true} if this parameter is read-only.
         */
        boolean readOnly() default true;
    }

    /**
     * Indicates that the annotated parameter should be mapped to the return value of the instrumented method. For this to be valid,
     * the parameter must be of type {@link Throwable}. If the instrumented method terminates regularly, {@code null} is assigned to
     * the annotated parameter.
     *
     * @see Advice
     * @see OnMethodExit
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public @interface Thrown {
        /* empty */
    }

    /**
     * A marker class that indicates that an advice method does not suppress any {@link Throwable}.
     */
    private static class NoSuppression extends Throwable {

        /**
         * A private constructor as this class is not supposed to be invoked.
         */
        private NoSuppression() {
            throw new UnsupportedOperationException("This marker class is not supposed to be instantiated");
        }
    }
}
