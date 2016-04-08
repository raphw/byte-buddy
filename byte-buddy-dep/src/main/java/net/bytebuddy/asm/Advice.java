package net.bytebuddy.asm;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.scaffold.FieldLocator;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.utility.CompoundList;
import org.objectweb.asm.*;

import java.io.IOException;
import java.io.Serializable;
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
    protected Advice(Dispatcher.Resolved.ForMethodEnter methodEnter,
                     Dispatcher.Resolved.ForMethodExit methodExit,
                     byte[] binaryRepresentation) {
        this.methodEnter = methodEnter;
        this.methodExit = methodExit;
        this.binaryRepresentation = binaryRepresentation;
    }

    /**
     * Implements advice where every matched method is advised by the given type's advisory methods. The advices binary representation is
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
        return to(typeDescription, classFileLocator, Collections.<Dispatcher.OffsetMapping.Factory>emptyList());
    }

    protected static Advice to(TypeDescription typeDescription,
                               ClassFileLocator classFileLocator,
                               List<? extends Dispatcher.OffsetMapping.Factory> userFactories) {
        try {
            Dispatcher.Unresolved methodEnter = Dispatcher.Inactive.INSTANCE, methodExit = Dispatcher.Inactive.INSTANCE;
            for (MethodDescription.InDefinedShape methodDescription : typeDescription.getDeclaredMethods()) {
                methodEnter = locate(OnMethodEnter.class, methodEnter, methodDescription);
                methodExit = locate(OnMethodExit.class, methodExit, methodDescription);
            }
            if (!methodEnter.isAlive() && !methodExit.isAlive()) {
                throw new IllegalArgumentException("No advice defined by " + typeDescription);
            }
            Dispatcher.Resolved.ForMethodEnter resolved = methodEnter.asMethodEnter(userFactories);
            return new Advice(resolved, methodExit.asMethodExitTo(userFactories, resolved), classFileLocator.locate(typeDescription.getName()).resolve());
        } catch (IOException exception) {
            throw new IllegalStateException("Error reading class file of " + typeDescription, exception);
        }
    }

    /**
     * Checks if a given method represents advice and does some basic validation.
     *
     * @param annotation        The annotation that indicates a given type of advice.
     * @param dispatcher        Any previous dispatcher.
     * @param methodDescription A description of the method considered as advice.
     * @return A dispatcher for the given method or the supplied dispatcher if the given method is not intended to be used as advice.
     */
    private static Dispatcher.Unresolved locate(Class<? extends Annotation> annotation,
                                                Dispatcher.Unresolved dispatcher,
                                                MethodDescription.InDefinedShape methodDescription) {
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

    public static WithCustomMapping withCustomMapping() {
        return new WithCustomMapping();
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
    public MethodVisitor wrap(TypeDescription instrumentedType,
                              MethodDescription.InDefinedShape methodDescription,
                              MethodVisitor methodVisitor,
                              int writerFlags,
                              int readerFlags) {
        if (methodDescription.isAbstract() || methodDescription.isNative()) {
            throw new IllegalStateException("Cannot advice abstract or native method " + methodDescription);
        } else if (!methodExit.isAlive()) {
            return new AdviceVisitor.WithoutExitAdvice(methodVisitor,
                    methodDescription,
                    methodEnter,
                    binaryRepresentation,
                    writerFlags,
                    readerFlags);
        } else if (methodExit.isSkipThrowable()) {
            return new AdviceVisitor.WithExitAdvice.WithoutExceptionHandling(methodVisitor,
                    methodDescription,
                    methodEnter,
                    methodExit,
                    binaryRepresentation,
                    writerFlags,
                    readerFlags);
        } else if (methodDescription.isConstructor()) {
            throw new IllegalStateException("Cannot catch exception during constructor call for " + methodDescription);
        } else {
            return new AdviceVisitor.WithExitAdvice.WithExceptionHandling(methodVisitor,
                    methodDescription,
                    methodEnter,
                    methodExit,
                    binaryRepresentation,
                    writerFlags,
                    readerFlags);
        }
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
     * A meta data handler that is responsible for translating stack map frames and adjusting size requirements.
     */
    protected interface MetaDataHandler {

        /**
         * Translates a frame.
         *
         * @param methodVisitor       The method visitor to write the frame to.
         * @param frameType           The frame's type.
         * @param localVariableLength The local variable length.
         * @param localVariable       An array containing the types of the current local variables.
         * @param stackSize           The size of the operand stack.
         * @param stack               An array containing the types of the current operand stack.
         */
        void translateFrame(MethodVisitor methodVisitor, int frameType, int localVariableLength, Object[] localVariable, int stackSize, Object[] stack);

        /**
         * Injects a frame for a method's exception handler.
         *
         * @param methodVisitor The method visitor to write the frame to.
         */
        void injectHandlerFrame(MethodVisitor methodVisitor);

        /**
         * Injects a frame after a method's completion.
         *
         * @param methodVisitor The method visitor to write the frame to.
         * @param secondary     {@code true} if the frame is written a second time.
         */
        void injectCompletionFrame(MethodVisitor methodVisitor, boolean secondary);

        /**
         * A meta data handler for the instrumented method.
         */
        interface ForInstrumentedMethod extends MetaDataHandler {

            /**
             * Indicates that a size is not computed but handled directly by ASM.
             */
            int UNDEFINED_SIZE = -1;

            /**
             * Binds this meta data handler for the entry advice.
             *
             * @param adviceMethod The entry advice method.
             * @return An appropriate meta data handler for the enter method.
             */
            ForAdvice bindEntry(MethodDescription.InDefinedShape adviceMethod);

            /**
             * Binds this meta data handler for the exit advice.
             *
             * @param adviceMethod The exit advice method.
             * @return An appropriate meta data handler for the enter method.
             */
            ForAdvice bindExit(MethodDescription.InDefinedShape adviceMethod);

            /**
             * Computes a compound stack size for the advice and the translated instrumented method.
             *
             * @param stackSize The required stack size of the instrumented method before translation.
             * @return The stack size required by the instrumented method and its advice methods.
             */
            int compoundStackSize(int stackSize);

            /**
             * Computes a compound local variable array length for the advice and the translated instrumented method.
             *
             * @param localVariableLength The required local variable array length of the instrumented method before translation.
             * @return The local variable length required by the instrumented method and its advice methods.
             */
            int compoundLocalVariableLength(int localVariableLength);

            /**
             * Returns the reader hint to apply when parsing the advice method.
             *
             * @return The reader hint for parsing the advice method.
             */
            int getReaderHint();
        }

        /**
         * A meta data handler for an advice method.
         */
        interface ForAdvice extends MetaDataHandler {

            /**
             * Records the maximum values for stack size and local variable array which are required by the advice method
             * for its individual execution without translation.
             *
             * @param stackSize           The minimum required stack size.
             * @param localVariableLength The minimum required length of the local variable array.
             */
            void recordMaxima(int stackSize, int localVariableLength);

            void recordStackPadding(int padding);
        }

        /**
         * A non-operational meta data handler that does not translate any frames and does not compute stack sizes.
         */
        enum NoOp implements ForInstrumentedMethod, ForAdvice {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public void recordMaxima(int maxStack, int maxLocals) {
                /* do nothing */
            }

            @Override
            public void recordStackPadding(int padding) {
                /* do nothing */
            }

            @Override
            public int compoundStackSize(int stackSize) {
                return UNDEFINED_SIZE;
            }

            @Override
            public int compoundLocalVariableLength(int localVariableLength) {
                return UNDEFINED_SIZE;
            }

            @Override
            public ForAdvice bindEntry(MethodDescription.InDefinedShape adviceMethod) {
                return this;
            }

            @Override
            public ForAdvice bindExit(MethodDescription.InDefinedShape adviceMethod) {
                return this;
            }

            @Override
            public int getReaderHint() {
                return ClassReader.SKIP_FRAMES;
            }

            @Override
            public void translateFrame(MethodVisitor methodVisitor, int frameType, int localVariableLength, Object[] localVariable, int stackSize, Object[] stack) {
                /* do nothing */
            }

            @Override
            public void injectHandlerFrame(MethodVisitor methodVisitor) {
                /* do nothing */
            }

            @Override
            public void injectCompletionFrame(MethodVisitor methodVisitor, boolean secondary) {
                /* do nothing */
            }

            @Override
            public String toString() {
                return "Advice.MetaDataHandler.NoOp." + name();
            }
        }

        /**
         * A default implementation of a meta data handler for an instrumented method.
         */
        abstract class Default implements ForInstrumentedMethod {

            /**
             * An empty array indicating an empty frame.
             */
            private static final Object[] EMPTY = new Object[0];

            /**
             * The instrumented method.
             */
            protected final MethodDescription.InDefinedShape instrumentedMethod;

            /**
             * A list of intermediate types to be considered as part of the instrumented method's steady signature.
             */
            protected final TypeList requiredTypes;

            /**
             * The types that are expected to be added after the instrumented method returns.
             */
            protected final TypeList yieldedTypes;

            /**
             * {@code true} if the meta data handler is expected to expand its frames.
             */
            private final boolean expandFrames;

            /**
             * The current frame's size divergence from the original local variable array.
             */
            private int currentFrameDivergence;

            /**
             * Creates a new default meta data handler.
             *
             * @param instrumentedMethod The instrumented method.
             * @param requiredTypes      A list of intermediate types to be considered as part of the instrumented method's steady signature.
             * @param yieldedTypes       The types that are expected to be added after the instrumented method returns.
             * @param expandFrames       {@code true} if the meta data handler is expected to expand its frames.
             */
            protected Default(MethodDescription.InDefinedShape instrumentedMethod,
                              List<? extends TypeDescription> requiredTypes,
                              List<? extends TypeDescription> yieldedTypes,
                              boolean expandFrames) {
                this.instrumentedMethod = instrumentedMethod;
                this.requiredTypes = new TypeList.Explicit(requiredTypes);
                this.yieldedTypes = new TypeList.Explicit(yieldedTypes);
                this.expandFrames = expandFrames;
            }

            /**
             * Creates an appropriate meta data handler for an instrumented method based on the context of the method creation.
             *
             * @param instrumentedMethod The instrumented method.
             * @param requiredTypes      The additional types that are added by the entry advice.
             * @param yieldedTypes       The types that are expected to be added after the instrumented method returns.
             * @param writerFlags        The ASM flags supplied to the {@link ClassWriter}.
             * @param readerFlags        The ASM flags supplied to the {@link ClassReader}.
             * @return An appropriate meta data handler.
             */
            protected static ForInstrumentedMethod of(MethodDescription.InDefinedShape instrumentedMethod,
                                                      List<? extends TypeDescription> requiredTypes,
                                                      List<? extends TypeDescription> yieldedTypes,
                                                      int writerFlags,
                                                      int readerFlags) {
                if ((writerFlags & ClassWriter.COMPUTE_FRAMES) != 0) {
                    return NoOp.INSTANCE;
                } else if ((writerFlags & ClassWriter.COMPUTE_MAXS) != 0) {
                    return new Default.WithoutStackSizeComputation(instrumentedMethod,
                            requiredTypes,
                            yieldedTypes,
                            (readerFlags & ClassReader.EXPAND_FRAMES) != 0);
                } else {
                    return new Default.WithStackSizeComputation(instrumentedMethod,
                            requiredTypes,
                            yieldedTypes,
                            (readerFlags & ClassReader.EXPAND_FRAMES) != 0);
                }
            }

            /**
             * Translates a type into a representation of its form inside a stack map frame.
             *
             * @param typeDescription The type to translate.
             * @return A stack entry representation of the supplied type.
             */
            protected static Object toFrame(TypeDescription typeDescription) {
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

            @Override
            public ForAdvice bindEntry(MethodDescription.InDefinedShape methodDescription) {
                return bind(methodDescription, new TypeList.Empty(), methodDescription.getReturnType().represents(void.class)
                        ? Collections.<TypeDescription>emptyList()
                        : Collections.singletonList(methodDescription.getReturnType().asErasure()), TranslationMode.ENTRY);
            }

            @Override
            public ForAdvice bindExit(MethodDescription.InDefinedShape methodDescription) {
                return bind(methodDescription, new TypeList.Explicit(
                        CompoundList.of(this.requiredTypes, instrumentedMethod.getReturnType().represents(void.class)
                                ? Collections.singletonList(TypeDescription.THROWABLE)
                                : Arrays.asList(instrumentedMethod.getReturnType().asErasure(), TypeDescription.THROWABLE))
                ), Collections.<TypeDescription>emptyList(), TranslationMode.EXIT);
            }

            /**
             * Binds the given advice method to an appropriate meta data handler.
             *
             * @param methodDescription The advice method.
             * @param requiredTypes     The expected types that the advice method requires additionally to the instrumented method's parameters.
             * @param yieldedTypes      The types this advice method yields as additional parameters.
             * @param translationMode   The translation mode to apply for this advice.
             * @return An appropriate meta data handler.
             */
            protected abstract ForAdvice bind(MethodDescription.InDefinedShape methodDescription,
                                              TypeList requiredTypes,
                                              List<? extends TypeDescription> yieldedTypes,
                                              TranslationMode translationMode);

            @Override
            public int getReaderHint() {
                return expandFrames ? ClassReader.EXPAND_FRAMES : AsmVisitorWrapper.NO_FLAGS;
            }

            @Override
            public void translateFrame(MethodVisitor methodVisitor,
                                       int type,
                                       int localVariableLength,
                                       Object[] localVariable,
                                       int stackSize,
                                       Object[] stack) {
                translateFrame(methodVisitor,
                        TranslationMode.COPY,
                        instrumentedMethod,
                        requiredTypes,
                        type,
                        localVariableLength,
                        localVariable,
                        stackSize,
                        stack);
            }

            /**
             * Translates a frame.
             *
             * @param methodVisitor       The method visitor to write the frame to.
             * @param translationMode     The translation mode to apply.
             * @param methodDescription   The method description for which the frame is written.
             * @param additionalTypes     The additional types to consider part of the instrumented method's parameters.
             * @param frameType           The frame's type.
             * @param localVariableLength The local variable length.
             * @param localVariable       An array containing the types of the current local variables.
             * @param stackSize           The size of the operand stack.
             * @param stack               An array containing the types of the current operand stack.
             */
            protected void translateFrame(MethodVisitor methodVisitor,
                                          TranslationMode translationMode,
                                          MethodDescription.InDefinedShape methodDescription,
                                          TypeList additionalTypes,
                                          int frameType,
                                          int localVariableLength,
                                          Object[] localVariable,
                                          int stackSize,
                                          Object[] stack) {
                switch (frameType) {
                    case Opcodes.F_SAME:
                    case Opcodes.F_SAME1:
                        break;
                    case Opcodes.F_APPEND:
                        currentFrameDivergence += localVariableLength;
                        break;
                    case Opcodes.F_CHOP:
                        currentFrameDivergence -= localVariableLength;
                        break;
                    case Opcodes.F_FULL:
                    case Opcodes.F_NEW:
                        Object[] translated = new Object[localVariableLength
                                - methodDescription.getParameters().size()
                                - (methodDescription.isStatic() ? 0 : 1)
                                + instrumentedMethod.getParameters().size()
                                + (instrumentedMethod.isStatic() ? 0 : 1)
                                + additionalTypes.size()];
                        int index = translationMode.copy(instrumentedMethod, methodDescription, localVariable, translated);
                        for (TypeDescription typeDescription : additionalTypes) {
                            translated[index++] = toFrame(typeDescription);
                        }
                        System.arraycopy(localVariable,
                                methodDescription.getParameters().size() + (methodDescription.isStatic() ? 0 : 1),
                                translated,
                                index,
                                translated.length - index);
                        localVariableLength = translated.length;
                        localVariable = translated;
                        currentFrameDivergence = translated.length - index;
                        break;
                    default:
                        throw new IllegalArgumentException("Unexpected frame frameType: " + frameType);
                }
                methodVisitor.visitFrame(frameType, localVariableLength, localVariable, stackSize, stack);
            }

            @Override
            public void injectHandlerFrame(MethodVisitor methodVisitor) {
                if (!expandFrames && currentFrameDivergence == 0) {
                    methodVisitor.visitFrame(Opcodes.F_SAME1, 0, EMPTY, 1, new Object[]{Type.getInternalName(Throwable.class)});
                } else {
                    injectFullFrame(methodVisitor, requiredTypes, true);
                }
            }

            @Override
            public void injectCompletionFrame(MethodVisitor methodVisitor, boolean secondary) {
                if (!expandFrames && currentFrameDivergence == 0 && (secondary || !instrumentedMethod.isConstructor())) {
                    if (secondary) {
                        methodVisitor.visitFrame(Opcodes.F_SAME, 0, EMPTY, 0, EMPTY);
                    } else {
                        Object[] local = new Object[yieldedTypes.size()];
                        int index = 0;
                        for (TypeDescription typeDescription : yieldedTypes) {
                            local[index++] = toFrame(typeDescription);
                        }
                        methodVisitor.visitFrame(Opcodes.F_APPEND, local.length, local, 0, EMPTY);
                    }
                } else {
                    injectFullFrame(methodVisitor, CompoundList.of(requiredTypes, yieldedTypes), false);
                }
            }

            /**
             * Injects a full frame.
             *
             * @param methodVisitor    The method visitor for which the frame should be written.
             * @param additionalTypes  The additional types that are considered to be part of the method's parameters.
             * @param exceptionOnStack {@code true} if there is a {@link Throwable} on the operand stack.
             */
            protected void injectFullFrame(MethodVisitor methodVisitor, List<? extends TypeDescription> additionalTypes, boolean exceptionOnStack) {
                Object[] localVariable = new Object[instrumentedMethod.getParameters().size() + (instrumentedMethod.isStatic() ? 0 : 1) + additionalTypes.size()];
                int index = 0;
                if (!instrumentedMethod.isStatic()) {
                    localVariable[index++] = toFrame(instrumentedMethod.getDeclaringType());
                }
                for (TypeDescription typeDescription : instrumentedMethod.getParameters().asTypeList().asErasures()) {
                    localVariable[index++] = toFrame(typeDescription);
                }
                for (TypeDescription typeDescription : additionalTypes) {
                    localVariable[index++] = toFrame(typeDescription);
                }
                Object[] stackType = exceptionOnStack
                        ? new Object[]{Type.getInternalName(Throwable.class)}
                        : EMPTY;
                methodVisitor.visitFrame(expandFrames ? Opcodes.F_NEW : Opcodes.F_FULL, localVariable.length, localVariable, stackType.length, stackType);
                currentFrameDivergence = 0;
            }

            /**
             * A translation mode that determines how the fixed frames of the instrumented method are written.
             */
            protected enum TranslationMode {

                /**
                 * A translation mode that simply copies the original frames which are available when translating frames of the instrumented method.
                 */
                COPY {
                    @Override
                    protected int copy(MethodDescription.InDefinedShape instrumentedMethod,
                                       MethodDescription.InDefinedShape methodDescription,
                                       Object[] localVariable,
                                       Object[] translated) {
                        int length = instrumentedMethod.getParameters().size() + (instrumentedMethod.isStatic() ? 0 : 1);
                        System.arraycopy(localVariable, 0, translated, 0, length);
                        return length;
                    }
                },

                /**
                 * A translation mode for the entry advice that considers that the {@code this} reference might not be initialized for a constructor.
                 */
                ENTRY {
                    @Override
                    protected int copy(MethodDescription.InDefinedShape instrumentedMethod,
                                       MethodDescription.InDefinedShape methodDescription,
                                       Object[] localVariable,
                                       Object[] translated) {
                        int index = 0;
                        if (!instrumentedMethod.isStatic()) {
                            translated[index++] = instrumentedMethod.isConstructor()
                                    ? Opcodes.UNINITIALIZED_THIS
                                    : toFrame(instrumentedMethod.getDeclaringType());
                        }
                        for (TypeDescription typeDescription : instrumentedMethod.getParameters().asTypeList().asErasures()) {
                            translated[index++] = toFrame(typeDescription);
                        }
                        return index;

                    }
                },

                /**
                 * A translation mode for an exit advice where the {@code this} reference is always initialized.
                 */
                EXIT {
                    @Override
                    protected int copy(MethodDescription.InDefinedShape instrumentedMethod,
                                       MethodDescription.InDefinedShape methodDescription,
                                       Object[] localVariable,
                                       Object[] translated) {
                        int index = 0;
                        if (!instrumentedMethod.isStatic()) {
                            translated[index++] = toFrame(instrumentedMethod.getDeclaringType());
                        }
                        for (TypeDescription typeDescription : instrumentedMethod.getParameters().asTypeList().asErasures()) {
                            translated[index++] = toFrame(typeDescription);
                        }
                        return index;
                    }
                };

                /**
                 * Copies the fixed parameters of the instrumented method onto the operand stack.
                 *
                 * @param instrumentedMethod The instrumented method.
                 * @param methodDescription  The method for which a frame is created.
                 * @param localVariable      The original local variable array.
                 * @param translated         The array containing the translated frames.
                 * @return The amount of frames added to the translated frame array.
                 */
                protected abstract int copy(MethodDescription.InDefinedShape instrumentedMethod,
                                            MethodDescription.InDefinedShape methodDescription,
                                            Object[] localVariable,
                                            Object[] translated);
            }

            /**
             * A meta data handler that is bound to an advice method.
             */
            protected abstract class ForAdvice implements MetaDataHandler.ForAdvice {

                /**
                 * The method description for which frames are translated.
                 */
                protected final MethodDescription.InDefinedShape methodDescription;

                /**
                 * A list of intermediate types to be considered as part of the instrumented method's steady signature.
                 */
                protected final TypeList requiredTypes;

                /**
                 * The types that this method yields as a result.
                 */
                private final List<? extends TypeDescription> yieldedTypes;

                /**
                 * The translation mode to apply for this advice method. Should be either {@link TranslationMode#ENTRY} or {@link TranslationMode#EXIT}.
                 */
                protected final TranslationMode translationMode;

                /**
                 * Creates a new meta data handler for an advice method.
                 *
                 * @param methodDescription The method description for which frames are translated.
                 * @param requiredTypes     A list of expected types to be considered as part of the instrumented method's steady signature.
                 * @param yieldedTypes      The types that this method yields as a result.
                 * @param translationMode   The translation mode to apply for this advice method. Should be
                 *                          either {@link TranslationMode#ENTRY} or {@link TranslationMode#EXIT}.
                 */
                protected ForAdvice(MethodDescription.InDefinedShape methodDescription,
                                    TypeList requiredTypes,
                                    List<? extends TypeDescription> yieldedTypes,
                                    TranslationMode translationMode) {
                    this.methodDescription = methodDescription;
                    this.requiredTypes = requiredTypes;
                    this.yieldedTypes = yieldedTypes;
                    this.translationMode = translationMode;
                }

                @Override
                public void translateFrame(MethodVisitor methodVisitor,
                                           int type,
                                           int localVariableLength,
                                           Object[] localVariable,
                                           int stackSize,
                                           Object[] stack) {
                    Default.this.translateFrame(methodVisitor,
                            translationMode,
                            methodDescription,
                            requiredTypes,
                            type,
                            localVariableLength,
                            localVariable,
                            stackSize,
                            stack);
                }

                @Override
                public void injectHandlerFrame(MethodVisitor methodVisitor) {
                    if (!expandFrames && currentFrameDivergence == 0) {
                        methodVisitor.visitFrame(Opcodes.F_SAME1, 0, EMPTY, 1, new Object[]{Type.getInternalName(Throwable.class)});
                    } else {
                        injectFullFrame(methodVisitor, requiredTypes, true);
                    }
                }

                @Override
                public void injectCompletionFrame(MethodVisitor methodVisitor, boolean secondary) {
                    if ((!expandFrames && currentFrameDivergence == 0 && yieldedTypes.size() < 4)) {
                        if (secondary || yieldedTypes.isEmpty()) {
                            methodVisitor.visitFrame(Opcodes.F_SAME, 0, EMPTY, 0, EMPTY);
                        } else {
                            Object[] local = new Object[yieldedTypes.size()];
                            int index = 0;
                            for (TypeDescription typeDescription : yieldedTypes) {
                                local[index++] = toFrame(typeDescription);
                            }
                            methodVisitor.visitFrame(Opcodes.F_APPEND, local.length, local, 0, EMPTY);
                        }
                    } else {
                        injectFullFrame(methodVisitor, CompoundList.of(requiredTypes, yieldedTypes), false);
                    }
                }
            }

            /**
             * A default meta data handler that recomputes the space requirements of an instrumented method.
             */
            protected static class WithStackSizeComputation extends Default {

                /**
                 * The maximum stack size required by a visited advice method.
                 */
                private int stackSize;

                /**
                 * The maximum length of the local variable array required by a visited advice method.
                 */
                private int localVariableLength;

                /**
                 * Creates a new default meta data handler that recomputes the space requirements of an instrumented method.
                 *
                 * @param instrumentedMethod The instrumented method.
                 * @param requiredTypes      The types this meta data handler expects to be available additionally to the instrumented method's parameters.
                 * @param yieldedTypes       The types that are expected to be added after the instrumented method returns.
                 * @param expandFrames       {@code true} if this meta data handler is expected to expand its written frames.
                 */
                protected WithStackSizeComputation(MethodDescription.InDefinedShape instrumentedMethod,
                                                   List<? extends TypeDescription> requiredTypes,
                                                   List<? extends TypeDescription> yieldedTypes,
                                                   boolean expandFrames) {
                    super(instrumentedMethod, requiredTypes, yieldedTypes, expandFrames);
                    stackSize = Math.max(instrumentedMethod.getReturnType().getStackSize().getSize(), StackSize.SINGLE.getSize());
                }

                @Override
                public int compoundStackSize(int stackSize) {
                    return Math.max(this.stackSize, stackSize);
                }

                @Override
                public int compoundLocalVariableLength(int localVariableLength) {
                    return Math.max(this.localVariableLength, localVariableLength
                            + instrumentedMethod.getReturnType().getStackSize().getSize()
                            + StackSize.SINGLE.getSize()
                            + requiredTypes.getStackSize());
                }

                @Override
                protected Default.ForAdvice bind(MethodDescription.InDefinedShape methodDescription,
                                                 TypeList requiredTypes,
                                                 List<? extends TypeDescription> yieldedTypes,
                                                 TranslationMode translationMode) {
                    if (translationMode == TranslationMode.ENTRY) {
                        stackSize = Math.max(methodDescription.getReturnType().getStackSize().getSize(), stackSize);
                    }
                    return new ForAdvice(methodDescription, requiredTypes, yieldedTypes, translationMode);
                }

                @Override
                public String toString() {
                    return "Advice.MetaDataHandler.Default.WithStackSizeComputation{" +
                            "instrumentedMethod=" + instrumentedMethod +
                            ", stackSize=" + stackSize +
                            ", localVariableLength=" + localVariableLength +
                            "}";
                }

                /**
                 * A meta data handler for an advice method that records size requirements.
                 */
                protected class ForAdvice extends Default.ForAdvice {

                    private int padding;

                    /**
                     * Creates a new meta data handler for an advice method.
                     *
                     * @param methodDescription The advice method.
                     * @param requiredTypes     The types that this method expects to exist in addition to the method parameter types.
                     * @param yieldedTypes      The types yielded by this advice method.
                     * @param translationMode   The translation mode for this meta data handler.
                     */
                    protected ForAdvice(MethodDescription.InDefinedShape methodDescription,
                                        TypeList requiredTypes,
                                        List<? extends TypeDescription> yieldedTypes,
                                        TranslationMode translationMode) {
                        super(methodDescription, requiredTypes, yieldedTypes, translationMode);
                    }

                    @Override
                    public void recordMaxima(int stackSize, int localVariableLength) {
                        WithStackSizeComputation.this.stackSize = Math.max(WithStackSizeComputation.this.stackSize, stackSize);
                        WithStackSizeComputation.this.localVariableLength = Math.max(WithStackSizeComputation.this.localVariableLength, localVariableLength
                                - methodDescription.getStackSize()
                                + instrumentedMethod.getStackSize()
                                + requiredTypes.getStackSize());
                    }

                    @Override
                    public void recordStackPadding(int padding) {
                        this.padding = Math.max(this.padding, padding);
                    }

                    @Override
                    public String toString() {
                        return "Advice.MetaDataHandler.Default.WithStackSizeComputation.ForAdvice{" +
                                "instrumentedMethod=" + instrumentedMethod +
                                ", methodDescription=" + methodDescription +
                                ", padding=" + padding +
                                "}";
                    }
                }
            }

            /**
             * A default meta data handler that does not recompute the space requirements of an instrumented method.
             */
            protected static class WithoutStackSizeComputation extends Default {

                /**
                 * Creates a new default meta data handler that does not recompute the space requirements of an instrumented method.
                 *
                 * @param instrumentedMethod The instrumented method.
                 * @param requiredTypes      The types this meta data handler expects to be available additionally to the instrumented method's parameters.
                 * @param yieldedTypes       The types that are expected to be added after the instrumented method returns.
                 * @param expandFrames       {@code true} if this meta data handler is expected to expand its written frames.
                 */
                protected WithoutStackSizeComputation(MethodDescription.InDefinedShape instrumentedMethod,
                                                      List<? extends TypeDescription> requiredTypes,
                                                      List<? extends TypeDescription> yieldedTypes,
                                                      boolean expandFrames) {
                    super(instrumentedMethod, requiredTypes, yieldedTypes, expandFrames);
                }

                @Override
                public int compoundStackSize(int stackSize) {
                    return UNDEFINED_SIZE;
                }

                @Override
                public int compoundLocalVariableLength(int localVariableLength) {
                    return UNDEFINED_SIZE;
                }

                @Override
                protected Default.ForAdvice bind(MethodDescription.InDefinedShape methodDescription,
                                                 TypeList requiredTypes,
                                                 List<? extends TypeDescription> yieldedTypes,
                                                 TranslationMode translationMode) {
                    return new ForAdvice(methodDescription, requiredTypes, yieldedTypes, translationMode);
                }

                @Override
                public String toString() {
                    return "Advice.MetaDataHandler.Default.WithoutStackSizeComputation{" +
                            "instrumentedMethod=" + instrumentedMethod +
                            "}";
                }

                /**
                 * A meta data handler for an advice method that does not record size requirements.
                 */
                protected class ForAdvice extends Default.ForAdvice {

                    /**
                     * Creates a new meta data handler for an advice method.
                     *
                     * @param methodDescription The advice method.
                     * @param requiredTypes     The types that this method expects to exist in addition to the method parameter types.
                     * @param yieldedTypes      The types yielded by this advice method.
                     * @param translationMode   The translation mode for this meta data handler.
                     */
                    protected ForAdvice(MethodDescription.InDefinedShape methodDescription,
                                        TypeList requiredTypes,
                                        List<? extends TypeDescription> yieldedTypes,
                                        TranslationMode translationMode) {
                        super(methodDescription, requiredTypes, yieldedTypes, translationMode);
                    }

                    @Override
                    public void recordMaxima(int maxStack, int maxLocals) {
                        /* do nothing */
                    }

                    @Override
                    public void recordStackPadding(int padding) {
                        /* do nothing */
                    }

                    @Override
                    public String toString() {
                        return "Advice.MetaDataHandler.Default.WithoutStackSizeComputation.ForAdvice{" +
                                "instrumentedMethod=" + instrumentedMethod +
                                ", methodDescription=" + methodDescription +
                                "}";
                    }
                }
            }
        }
    }

    /**
     * A method visitor that weaves the advice methods' byte codes.
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
         * A reader for traversing the advice methods' class file.
         */
        private final ClassReader classReader;

        /**
         * A handler to use for translating meta embedded in the byte code.
         */
        protected final MetaDataHandler.ForInstrumentedMethod metaDataHandler;

        /**
         * Creates a new advice visitor.
         *
         * @param methodVisitor        The method visitor to which all instructions are written.
         * @param instrumentedMethod   The instrumented method.
         * @param methodEnter          The method enter advice.
         * @param yieldedTypes         The types that are expected to be added after the instrumented method returns.
         * @param binaryRepresentation The binary representation of the advice class.
         * @param writerFlags          The ASM writer flags that were set.
         * @param readerFlags          The ASM reader flags that were set.
         */
        protected AdviceVisitor(MethodVisitor methodVisitor,
                                MethodDescription.InDefinedShape instrumentedMethod,
                                Dispatcher.Resolved.ForMethodEnter methodEnter,
                                List<? extends TypeDescription> yieldedTypes,
                                byte[] binaryRepresentation,
                                int writerFlags,
                                int readerFlags) {
            super(Opcodes.ASM5, methodVisitor);
            this.instrumentedMethod = instrumentedMethod;
            this.methodEnter = methodEnter;
            classReader = new ClassReader(binaryRepresentation);
            metaDataHandler = MetaDataHandler.Default.of(instrumentedMethod, methodEnter.getEnterType().represents(void.class)
                    ? Collections.<TypeDescription>emptyList()
                    : Collections.singletonList(methodEnter.getEnterType()), yieldedTypes, writerFlags, readerFlags);
        }

        @Override
        public void visitCode() {
            super.visitCode();
            if (methodEnter.isAlive()) {
                append(methodEnter);
            }
            onUserStart();
        }

        /**
         * Writes the advice for entering the instrumented method.
         */
        protected abstract void onUserStart();

        @Override
        public void visitVarInsn(int opcode, int offset) {
            super.visitVarInsn(opcode, offset < instrumentedMethod.getStackSize()
                    ? offset
                    : offset + methodEnter.getEnterType().getStackSize().getSize());
        }

        @Override
        public void visitIincInsn(int offset, int increment) {
            super.visitIincInsn(offset < instrumentedMethod.getStackSize()
                    ? offset
                    : offset + methodEnter.getEnterType().getStackSize().getSize(), increment);
        }

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
        public void visitFrame(int frameType, int localVariableLength, Object[] localVariable, int stackSize, Object[] stack) {
            metaDataHandler.translateFrame(mv, frameType, localVariableLength, localVariable, stackSize, stack);
        }

        @Override
        public void visitMaxs(int stackSize, int localVariableLength) {
            onUserEnd();
            super.visitMaxs(metaDataHandler.compoundStackSize(stackSize), metaDataHandler.compoundLocalVariableLength(localVariableLength));
        }

        /**
         * Writes the advice for completing the instrumented method.
         */
        protected abstract void onUserEnd();

        /**
         * Applies the supplied advice dispatcher onto the target method visitor.
         *
         * @param dispatcher The dispatcher to apply.
         */
        protected void append(Dispatcher.Resolved dispatcher) {
            classReader.accept(new CodeCopier(dispatcher), ClassReader.SKIP_DEBUG | metaDataHandler.getReaderHint());
        }

        /**
         * A visitor for copying an advice method's byte code.
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
                return dispatcher.apply(internalName, descriptor, mv, metaDataHandler, instrumentedMethod);
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
         * An advice visitor that does not apply exit advice.
         */
        protected static class WithoutExitAdvice extends AdviceVisitor {

            /**
             * Creates an advice visitor that does not apply exit advice.
             *
             * @param methodVisitor        The method visitor for the instrumented method.
             * @param instrumentedMethod   A description of the instrumented method.
             * @param methodEnter          The dispatcher to be used for method entry.
             * @param binaryRepresentation The binary representation of the advice methods' class file.
             * @param writerFlags          The ASM writer flags that were set.
             * @param readerFlags          The ASM reader flags that were set.
             */
            public WithoutExitAdvice(MethodVisitor methodVisitor,
                                     MethodDescription.InDefinedShape instrumentedMethod,
                                     Dispatcher.Resolved.ForMethodEnter methodEnter,
                                     byte[] binaryRepresentation,
                                     int writerFlags,
                                     int readerFlags) {
                super(methodVisitor,
                        instrumentedMethod,
                        methodEnter,
                        Collections.<TypeDescription>emptyList(),
                        binaryRepresentation,
                        writerFlags,
                        readerFlags);
            }

            @Override
            protected void onUserStart() {
                /* empty */
            }

            @Override
            protected void onUserEnd() {
                /* empty */
            }

            @Override
            public String toString() {
                return "Advice.AdviceVisitor.WithoutExitAdvice{" +
                        ", instrumentedMethod=" + instrumentedMethod +
                        "}";
            }
        }

        /**
         * An advice visitor that applies exit advice.
         */
        protected abstract static class WithExitAdvice extends AdviceVisitor {

            /**
             * The dispatcher to be used for method exit.
             */
            private final Dispatcher.Resolved.ForMethodExit methodExit;

            /**
             * A label that indicates the end of the method.
             */
            protected final Label endOfMethod;

            /**
             * Creates an advice visitor that applies exit advice.
             *
             * @param methodVisitor        The method visitor for the instrumented method.
             * @param instrumentedMethod   A description of the instrumented method.
             * @param methodEnter          The dispatcher to be used for method entry.
             * @param methodExit           The dispatcher to be used for method exit.
             * @param yieldedTypes         The types that are expected to be added after the instrumented method returns.
             * @param binaryRepresentation The binary representation of the advice methods' class file.
             * @param writerFlags          The ASM writer flags that were set.
             * @param readerFlags          The ASM reader flags that were set.
             */
            public WithExitAdvice(MethodVisitor methodVisitor,
                                  MethodDescription.InDefinedShape instrumentedMethod,
                                  Dispatcher.Resolved.ForMethodEnter methodEnter,
                                  Dispatcher.Resolved.ForMethodExit methodExit,
                                  List<? extends TypeDescription> yieldedTypes,
                                  byte[] binaryRepresentation,
                                  int writerFlags,
                                  int readerFlags) {
                super(methodVisitor, instrumentedMethod, methodEnter, yieldedTypes, binaryRepresentation, writerFlags, readerFlags);
                this.methodExit = methodExit;
                endOfMethod = new Label();
            }

            @Override
            public void visitInsn(int opcode) {
                switch (opcode) {
                    case Opcodes.RETURN:
                        break;
                    case Opcodes.IRETURN:
                        variable(Opcodes.ISTORE);
                        break;
                    case Opcodes.FRETURN:
                        variable(Opcodes.FSTORE);
                        break;
                    case Opcodes.DRETURN:
                        variable(Opcodes.DSTORE);
                        break;
                    case Opcodes.LRETURN:
                        variable(Opcodes.LSTORE);
                        break;
                    case Opcodes.ARETURN:
                        variable(Opcodes.ASTORE);
                        break;
                    default:
                        mv.visitInsn(opcode);
                        return;
                }
                onUserReturn();
                mv.visitJumpInsn(Opcodes.GOTO, endOfMethod);
            }

            @Override
            protected void onUserEnd() {
                onUserExit();
                mv.visitLabel(endOfMethod);
                metaDataHandler.injectCompletionFrame(mv, false);
                append(methodExit);
                onAdviceExit();
                if (instrumentedMethod.getReturnType().represents(void.class)) {
                    mv.visitInsn(Opcodes.RETURN);
                } else {
                    Type returnType = Type.getType(instrumentedMethod.getReturnType().asErasure().getDescriptor());
                    variable(returnType.getOpcode(Opcodes.ILOAD));
                    mv.visitInsn(returnType.getOpcode(Opcodes.IRETURN));
                }
                onMethodExit();
            }

            /**
             * Invoked when the user method issues a return statement before applying the exit handler.
             */
            protected abstract void onUserReturn();

            /**
             * Invoked on completing to write the translated user code.
             */
            protected abstract void onUserExit();

            /**
             * Invoked on completing the inlining of the exit advice.
             */
            protected abstract void onAdviceExit();

            /**
             * Invoked on completing the method's code.
             */
            protected abstract void onMethodExit();

            /**
             * An advice visitor that captures exceptions by weaving try-catch blocks around user code.
             */
            protected static class WithExceptionHandling extends WithExitAdvice {

                /**
                 * Indicates that any throwable should be captured.
                 */
                private static final String ANY_THROWABLE = null;

                /**
                 * Indicates the start of the user method.
                 */
                private final Label userStart;

                /**
                 * Indicates the end of the user method.
                 */
                private final Label userEnd;

                /**
                 * Indicates the position of a handler for rethrowing an exception that was thrown by the user method.
                 */
                private final Label exceptionalReturn;

                /**
                 * Creates a new advice visitor that captures exception by weaving try-catch blocks around user code.
                 *
                 * @param methodVisitor        The method visitor for the instrumented method.
                 * @param instrumentedMethod   A description of the instrumented method.
                 * @param methodEnter          The dispatcher to be used for method entry.
                 * @param methodExit           The dispatcher to be used for method exit.
                 * @param binaryRepresentation The binary representation of the advice methods' class file.
                 * @param writerFlags          The ASM writer flags that were set.
                 * @param readerFlags          The ASM reader flags that were set.
                 */
                protected WithExceptionHandling(MethodVisitor methodVisitor,
                                                MethodDescription.InDefinedShape instrumentedMethod,
                                                Dispatcher.Resolved.ForMethodEnter methodEnter,
                                                Dispatcher.Resolved.ForMethodExit methodExit,
                                                byte[] binaryRepresentation,
                                                int writerFlags,
                                                int readerFlags) {
                    super(methodVisitor,
                            instrumentedMethod,
                            methodEnter,
                            methodExit,
                            instrumentedMethod.getReturnType().represents(void.class)
                                    ? Collections.singletonList(TypeDescription.THROWABLE)
                                    : Arrays.asList(instrumentedMethod.getReturnType().asErasure(), TypeDescription.THROWABLE),
                            binaryRepresentation,
                            writerFlags,
                            readerFlags);
                    userStart = new Label();
                    userEnd = new Label();
                    exceptionalReturn = new Label();
                }

                @Override
                protected void onUserStart() {
                    mv.visitTryCatchBlock(userStart, userEnd, userEnd, ANY_THROWABLE);
                    mv.visitLabel(userStart);
                }

                @Override
                protected void onUserReturn() {
                    mv.visitInsn(Opcodes.ACONST_NULL);
                    variable(Opcodes.ASTORE, instrumentedMethod.getReturnType().getStackSize().getSize());
                }

                @Override
                protected void onUserExit() {
                    mv.visitLabel(userEnd);
                    metaDataHandler.injectHandlerFrame(mv);
                    variable(Opcodes.ASTORE, instrumentedMethod.getReturnType().getStackSize().getSize());
                    storeDefaultReturn();
                    mv.visitJumpInsn(Opcodes.GOTO, endOfMethod);
                }

                @Override
                protected void onAdviceExit() {
                    variable(Opcodes.ALOAD, instrumentedMethod.getReturnType().getStackSize().getSize());
                    mv.visitJumpInsn(Opcodes.IFNONNULL, exceptionalReturn);
                }

                @Override
                protected void onMethodExit() {
                    mv.visitLabel(exceptionalReturn);
                    metaDataHandler.injectCompletionFrame(mv, true);
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
                    return "Advice.AdviceVisitor.WithExitAdvice.WithExceptionHandling{" +
                            "instrumentedMethod=" + instrumentedMethod +
                            "}";
                }
            }

            /**
             * An advice visitor that does not capture exceptions.
             */
            protected static class WithoutExceptionHandling extends WithExitAdvice {

                /**
                 * Creates a new advice visitor that does not capture exceptions.
                 *
                 * @param methodVisitor        The method visitor for the instrumented method.
                 * @param instrumentedMethod   A description of the instrumented method.
                 * @param methodEnter          The dispatcher to be used for method entry.
                 * @param methodExit           The dispatcher to be used for method exit.
                 * @param binaryRepresentation The binary representation of the advice methods' class file.
                 * @param writerFlags          The ASM writer flags that were set.
                 * @param readerFlags          The ASM reader flags that were set.
                 */
                protected WithoutExceptionHandling(MethodVisitor methodVisitor,
                                                   MethodDescription.InDefinedShape instrumentedMethod,
                                                   Dispatcher.Resolved.ForMethodEnter methodEnter,
                                                   Dispatcher.Resolved.ForMethodExit methodExit,
                                                   byte[] binaryRepresentation,
                                                   int writerFlags,
                                                   int readerFlags) {
                    super(methodVisitor,
                            instrumentedMethod,
                            methodEnter,
                            methodExit,
                            instrumentedMethod.getReturnType().represents(void.class)
                                    ? Collections.<TypeDescription>emptyList()
                                    : Collections.singletonList(instrumentedMethod.getReturnType().asErasure()),
                            binaryRepresentation,
                            writerFlags,
                            readerFlags);
                }

                @Override
                protected void onUserStart() {
                    /* empty */
                }

                @Override
                protected void onUserReturn() {
                    /* empty */
                }

                @Override
                protected void onUserExit() {
                    /* empty */
                }

                @Override
                protected void onAdviceExit() {
                    /* empty */
                }

                @Override
                protected void onMethodExit() {
                    /* empty */
                }

                @Override
                public String toString() {
                    return "Advice.AdviceVisitor.WithExitAdvice.WithoutExceptionHandling{" +
                            "instrumentedMethod=" + instrumentedMethod +
                            "}";
                }
            }
        }
    }

    /**
     * A dispatcher for implementing advice.
     */
    protected interface Dispatcher {

        /**
         * Indicates that a method does not represent advice and does not need to be visited.
         */
        MethodVisitor IGNORE_METHOD = null;

        /**
         * Returns {@code true} if this dispatcher is alive.
         *
         * @return {@code true} if this dispatcher is alive.
         */
        boolean isAlive();

        /**
         * A dispatcher that is not yet resolved.
         */
        interface Unresolved extends Dispatcher {

            /**
             * Resolves this dispatcher as a dispatcher for entering a method.
             *
             * @return This dispatcher as a dispatcher for entering a method.
             */
            Resolved.ForMethodEnter asMethodEnter(List<? extends OffsetMapping.Factory> userFactories);

            /**
             * Resolves this dispatcher as a dispatcher for exiting a method.
             *
             * @param dispatcher The dispatcher for entering a method.
             * @return This dispatcher as a dispatcher for exiting a method.
             */
            Resolved.ForMethodExit asMethodExitTo(List<? extends OffsetMapping.Factory> userFactories, Resolved.ForMethodEnter dispatcher);
        }

        /**
         * Represents an offset mapping for an advice method to an alternative offset.
         */
        interface OffsetMapping {

            /**
             * Resolves an offset mapping to a given target offset.
             *
             * @param instrumentedMethod The instrumented method for which the mapping is to be resolved.
             * @param context            The context in which the offset mapping is applied.
             * @return A suitable target mapping.
             */
            Target resolve(MethodDescription.InDefinedShape instrumentedMethod, Context context);

            /**
             * A context for applying an {@link OffsetMapping}.
             */
            interface Context {

                /**
                 * Returns {@code true} if the advice is applied on a fully initialized instance, i.e. describes if the {@code this}
                 * instance is available or still uninitialized during calling the advice.
                 *
                 * @return {@code true} if the advice is applied onto a fully initialized method.
                 */
                boolean isInitialized();

                /**
                 * Returns the padding before writing additional values that this context applies.
                 *
                 * @return The required padding for this context.
                 */
                int getPadding();

                /**
                 * A context for an offset mapping describing a method entry.
                 */
                enum ForMethodEntry implements Context {

                    /**
                     * Describes a context for a method entry that is not a constructor.
                     */
                    INITIALIZED(true),

                    /**
                     * Describes a context for a method entry that is a constructor.
                     */
                    NON_INITIALIZED(false);

                    /**
                     * Resolves an appropriate method entry context for the supplied instrumented method.
                     *
                     * @param instrumentedMethod The instrumented method.
                     * @return An appropriate context.
                     */
                    protected static Context of(MethodDescription.InDefinedShape instrumentedMethod) {
                        return instrumentedMethod.isConstructor()
                                ? NON_INITIALIZED
                                : INITIALIZED;
                    }

                    /**
                     * {@code true} if the method is no constructor, i.e. is invoked for an initialized instance upon entry.
                     */
                    private final boolean initialized;

                    /**
                     * Creates a new context for a method entry.
                     *
                     * @param initialized {@code true} if the method is no constructor, i.e. is invoked for an initialized instance upon entry.
                     */
                    ForMethodEntry(boolean initialized) {
                        this.initialized = initialized;
                    }

                    @Override
                    public boolean isInitialized() {
                        return initialized;
                    }

                    @Override
                    public int getPadding() {
                        return StackSize.ZERO.getSize();
                    }

                    @Override
                    public String toString() {
                        return "Advice.Dispatcher.Active.Resolved.OffsetMapping.Context.ForMethodEntry." + name();
                    }
                }

                /**
                 * A context for an offset mapping describing a method exit.
                 */
                enum ForMethodExit implements Context {

                    /**
                     * A method exit with a zero sized padding.
                     */
                    ZERO(StackSize.ZERO),

                    /**
                     * A method exit with a single slot padding.
                     */
                    SINGLE(StackSize.SINGLE),

                    /**
                     * A method exit with a double slot padding.
                     */
                    DOUBLE(StackSize.DOUBLE);

                    /**
                     * The padding implied by this method exit.
                     */
                    private final StackSize stackSize;

                    /**
                     * Creates a new context for a method exit.
                     *
                     * @param stackSize The padding implied by this method exit.
                     */
                    ForMethodExit(StackSize stackSize) {
                        this.stackSize = stackSize;
                    }

                    /**
                     * Resolves an appropriate method exit context for the supplied entry method type.
                     *
                     * @param typeDescription The type that is returned by the enter method.
                     * @return An appropriate context for the supplied entry method type.
                     */
                    protected static Context of(TypeDescription typeDescription) {
                        switch (typeDescription.getStackSize()) {
                            case ZERO:
                                return ZERO;
                            case SINGLE:
                                return SINGLE;
                            case DOUBLE:
                                return DOUBLE;
                            default:
                                throw new IllegalStateException("Unknown stack size: " + typeDescription);
                        }
                    }

                    @Override
                    public boolean isInitialized() {
                        return true;
                    }

                    @Override
                    public int getPadding() {
                        return stackSize.getSize();
                    }

                    @Override
                    public String toString() {
                        return "Advice.Dispatcher.Active.Resolved.OffsetMapping.Context.ForMethodExit." + name();
                    }
                }
            }

            /**
             * A target offset of an offset mapping.
             */
            interface Target {

                int NO_PADDING = 0;

                /**
                 * Applies this offset mapping for a {@link MethodVisitor#visitVarInsn(int, int)} instruction.
                 *
                 * @param methodVisitor The method visitor onto which this offset mapping is to be applied.
                 * @param opcode        The opcode of the original instruction.
                 */
                int resolveAccess(MethodVisitor methodVisitor, int opcode);

                /**
                 * Applies this offset mapping for a {@link MethodVisitor#visitIincInsn(int, int)} instruction.
                 *
                 * @param methodVisitor The method visitor onto which this offset mapping is to be applied.
                 * @param increment     The value with which to increment the targeted value.
                 */
                int resolveIncrement(MethodVisitor methodVisitor, int increment);

                /**
                 * Loads a default value onto the stack or pops the accessed value off it.
                 */
                enum ForDefaultValue implements Target {

                    /**
                     * The singleton instance.
                     */
                    INSTANCE;

                    @Override
                    public int resolveAccess(MethodVisitor methodVisitor, int opcode) {
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
                        return NO_PADDING;
                    }

                    @Override
                    public int resolveIncrement(MethodVisitor methodVisitor, int increment) {
                        return NO_PADDING;
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
                    public int resolveAccess(MethodVisitor methodVisitor, int opcode) {
                        methodVisitor.visitVarInsn(opcode, offset);
                        return NO_PADDING;
                    }

                    @Override
                    public int resolveIncrement(MethodVisitor methodVisitor, int increment) {
                        methodVisitor.visitIincInsn(offset, increment);
                        return NO_PADDING;
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
                    public int resolveAccess(MethodVisitor methodVisitor, int opcode) {
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
                        return NO_PADDING;
                    }

                    @Override
                    public int resolveIncrement(MethodVisitor methodVisitor, int increment) {
                        throw new IllegalStateException("Cannot write to read-only parameter at offset " + offset);
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
                    public int resolveAccess(MethodVisitor methodVisitor, int opcode) {
                        switch (opcode) {
                            case Opcodes.ISTORE:
                            case Opcodes.ASTORE:
                            case Opcodes.FSTORE:
                                if (fieldDescription.isStatic()) {
                                    methodVisitor.visitFieldInsn(Opcodes.PUTFIELD,
                                            fieldDescription.getDeclaringType().asErasure().getInternalName(),
                                            fieldDescription.getInternalName(),
                                            fieldDescription.getDescriptor());
                                    return NO_PADDING;
                                } else {
                                    methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                                    methodVisitor.visitInsn(Opcodes.DUP_X1);
                                    methodVisitor.visitFieldInsn(Opcodes.PUTFIELD,
                                            fieldDescription.getDeclaringType().asErasure().getInternalName(),
                                            fieldDescription.getInternalName(),
                                            fieldDescription.getDescriptor());
                                    methodVisitor.visitInsn(Opcodes.POP);
                                    return 2;
                                }
                            case Opcodes.LSTORE:
                            case Opcodes.DSTORE:
                                if (fieldDescription.isStatic()) {
                                    methodVisitor.visitFieldInsn(Opcodes.PUTSTATIC,
                                            fieldDescription.getDeclaringType().asErasure().getInternalName(),
                                            fieldDescription.getInternalName(),
                                            fieldDescription.getDescriptor());
                                    return NO_PADDING;
                                } else {
                                    methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                                    methodVisitor.visitInsn(Opcodes.DUP_X2);
                                    methodVisitor.visitFieldInsn(Opcodes.PUTFIELD,
                                            fieldDescription.getDeclaringType().asErasure().getInternalName(),
                                            fieldDescription.getInternalName(),
                                            fieldDescription.getDescriptor());
                                    methodVisitor.visitInsn(Opcodes.POP2);
                                    return 3;
                                }
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
                                return NO_PADDING;
                            default:
                                throw new IllegalArgumentException("Did not expect opcode: " + opcode);
                        }
                    }

                    @Override
                    public int resolveIncrement(MethodVisitor methodVisitor, int increment) {
                        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                        methodVisitor.visitInsn(Opcodes.DUP_X1);
                        methodVisitor.visitFieldInsn(Opcodes.PUTFIELD,
                                fieldDescription.getDeclaringType().asErasure().getInternalName(),
                                fieldDescription.getInternalName(),
                                fieldDescription.getDescriptor());
                        methodVisitor.visitInsn(Opcodes.POP);
                        return 2;
                    }

                    @Override
                    public boolean equals(Object object) {
                        if (this == object) return true;
                        if (object == null || getClass() != object.getClass()) return false;
                        ForReadOnlyField forReadOnlyField = (ForReadOnlyField) object;
                        return fieldDescription.equals(forReadOnlyField.fieldDescription);
                    }

                    @Override
                    public int hashCode() {
                        return fieldDescription.hashCode();
                    }

                    @Override
                    public String toString() {
                        return "Advice.Dispatcher.Active.Resolved.OffsetMapping.Target.ForReadOnlyField{" +
                                "fieldDescription=" + fieldDescription +
                                '}';
                    }
                }

                /**
                 * An offset mapping for a field.
                 */
                class ForReadOnlyField implements Target {

                    /**
                     * The field being read.
                     */
                    private final FieldDescription fieldDescription;

                    /**
                     * Creates a new offset mapping for a field.
                     *
                     * @param fieldDescription The field being read.
                     */
                    protected ForReadOnlyField(FieldDescription fieldDescription) {
                        this.fieldDescription = fieldDescription;
                    }

                    @Override
                    public int resolveAccess(MethodVisitor methodVisitor, int opcode) {
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
                        return NO_PADDING;
                    }

                    @Override
                    public int resolveIncrement(MethodVisitor methodVisitor, int increment) {
                        throw new IllegalStateException("Cannot write to field: " + fieldDescription);
                    }

                    @Override
                    public boolean equals(Object object) {
                        if (this == object) return true;
                        if (object == null || getClass() != object.getClass()) return false;
                        ForReadOnlyField forReadOnlyField = (ForReadOnlyField) object;
                        return fieldDescription.equals(forReadOnlyField.fieldDescription);
                    }

                    @Override
                    public int hashCode() {
                        return fieldDescription.hashCode();
                    }

                    @Override
                    public String toString() {
                        return "Advice.Dispatcher.Active.Resolved.OffsetMapping.Target.ForReadOnlyField{" +
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
                    public int resolveAccess(MethodVisitor methodVisitor, int opcode) {
                        switch (opcode) {
                            case Opcodes.ISTORE:
                            case Opcodes.ASTORE:
                            case Opcodes.FSTORE:
                            case Opcodes.LSTORE:
                            case Opcodes.DSTORE:
                                throw new IllegalStateException("Cannot write to fixed value: " + value);
                            case Opcodes.ILOAD:
                            case Opcodes.FLOAD:
                            case Opcodes.ALOAD:
                            case Opcodes.LLOAD:
                            case Opcodes.DLOAD:
                                methodVisitor.visitLdcInsn(value);
                                break;
                            default:
                                throw new IllegalArgumentException("Did not expect opcode: " + opcode);
                        }
                        return NO_PADDING;
                    }

                    @Override
                    public int resolveIncrement(MethodVisitor methodVisitor, int increment) {
                        throw new IllegalStateException("Cannot write to fixed value: " + value);
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

                class ForBoxedParameter implements Target {

                    private final int offset;

                    private final BoxingDispatcher boxingDispatcher;

                    protected ForBoxedParameter(int offset, BoxingDispatcher boxingDispatcher) {
                        this.offset = offset;
                        this.boxingDispatcher = boxingDispatcher;
                    }

                    protected static Target of(int offset, TypeDefinition type) {
                        return new ForBoxedParameter(offset, BoxingDispatcher.of(type));
                    }

                    @Override
                    public int resolveAccess(MethodVisitor methodVisitor, int opcode) {
                        switch (opcode) {
                            case Opcodes.ALOAD:
                                boxingDispatcher.loadBoxed(methodVisitor, offset);
                                break;
                            case Opcodes.ILOAD:
                            case Opcodes.LLOAD:
                            case Opcodes.FLOAD:
                            case Opcodes.DLOAD:
                            case Opcodes.ISTORE:
                            case Opcodes.FSTORE:
                            case Opcodes.ASTORE:
                            case Opcodes.LSTORE:
                            case Opcodes.DSTORE:
                                throw new IllegalStateException(); // TODO
                            default:
                                throw new IllegalStateException("Unexpected opcode: " + opcode);
                        }
                        return boxingDispatcher.getStackSize().getSize() - 1;
                    }

                    @Override
                    public int resolveIncrement(MethodVisitor methodVisitor, int increment) {
                        throw new IllegalStateException(); // TODO
                    }

                    protected enum BoxingDispatcher {

                        BOOLEAN(Opcodes.ILOAD, Boolean.class, boolean.class),
                        BYTE(Opcodes.ILOAD, Byte.class, byte.class),
                        SHORT(Opcodes.ILOAD, Short.class, short.class),
                        CHARACTER(Opcodes.ILOAD, Character.class, char.class),
                        INTEGER(Opcodes.ILOAD, Integer.class, int.class),
                        LONG(Opcodes.ILOAD, Long.class, long.class),
                        FLOAT(Opcodes.ILOAD, Float.class, float.class),
                        DOUBLE(Opcodes.ILOAD, Double.class, double.class);

                        private static final String VALUE_OF = "valueOf";

                        private final int opcode;

                        private final String owner;

                        private final String descriptor;

                        private final StackSize stackSize;

                        BoxingDispatcher(int opcode, Class<?> wrapperType, Class<?> primitiveType) {
                            this.opcode = opcode;
                            owner = Type.getInternalName(wrapperType);
                            descriptor = Type.getMethodDescriptor(Type.getType(wrapperType), Type.getType(primitiveType));
                            stackSize = StackSize.of(primitiveType);
                        }

                        protected static BoxingDispatcher of(TypeDefinition typeDefinition) {
                            if (typeDefinition.represents(boolean.class)) {
                                return BOOLEAN;
                            } else if (typeDefinition.represents(byte.class)) {
                                return BYTE;
                            } else if (typeDefinition.represents(short.class)) {
                                return SHORT;
                            } else if (typeDefinition.represents(char.class)) {
                                return CHARACTER;
                            } else if (typeDefinition.represents(int.class)) {
                                return INTEGER;
                            } else if (typeDefinition.represents(long.class)) {
                                return LONG;
                            } else if (typeDefinition.represents(float.class)) {
                                return FLOAT;
                            } else if (typeDefinition.represents(double.class)) {
                                return DOUBLE;
                            } else {
                                throw new IllegalArgumentException(); // TODO
                            }
                        }

                        protected void loadBoxed(MethodVisitor methodVisitor, int offset) {
                            methodVisitor.visitVarInsn(opcode, offset);
                            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, owner, VALUE_OF, descriptor, false);
                        }

                        protected StackSize getStackSize() {
                            return stackSize;
                        }
                    }
                }

                class ForBoxedArguments implements Target {

                    private final List<ParameterDescription.InDefinedShape> parameters;

                    protected ForBoxedArguments(List<ParameterDescription.InDefinedShape> parameters) {
                        this.parameters = parameters;
                    }

                    @Override
                    public int resolveAccess(MethodVisitor methodVisitor, int opcode) {
                        switch (opcode) {
                            case Opcodes.ALOAD:
                                loadInteger(methodVisitor, parameters.size());
                                methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, TypeDescription.OBJECT.getInternalName());
                                StackSize stackSize = StackSize.ZERO;
                                for (ParameterDescription parameter : parameters) {
                                    methodVisitor.visitInsn(Opcodes.DUP);
                                    loadInteger(methodVisitor, parameter.getIndex());
                                    ForBoxedParameter.BoxingDispatcher.of(parameter.getType()).loadBoxed(methodVisitor, parameter.getOffset());
                                    methodVisitor.visitInsn(Opcodes.AASTORE);
                                    stackSize = stackSize.maximum(parameter.getType().getStackSize());
                                }
                                return stackSize.getSize() + 2;
                            case Opcodes.ILOAD:
                            case Opcodes.LLOAD:
                            case Opcodes.FLOAD:
                            case Opcodes.DLOAD:
                            case Opcodes.ISTORE:
                            case Opcodes.FSTORE:
                            case Opcodes.ASTORE:
                            case Opcodes.LSTORE:
                            case Opcodes.DSTORE:
                                throw new IllegalStateException(); // TODO
                            default:
                                throw new IllegalStateException("Unexpected opcode: " + opcode);
                        }
                    }

                    private static void loadInteger(MethodVisitor methodVisitor, int value) {
                        switch (value) {
                            case 0:
                                methodVisitor.visitInsn(Opcodes.ICONST_0);
                                break;
                            case 1:
                                methodVisitor.visitInsn(Opcodes.ICONST_1);
                                break;
                            case 2:
                                methodVisitor.visitInsn(Opcodes.ICONST_2);
                                break;
                            case 3:
                                methodVisitor.visitInsn(Opcodes.ICONST_3);
                                break;
                            case 4:
                                methodVisitor.visitInsn(Opcodes.ICONST_4);
                                break;
                            case 5:
                                methodVisitor.visitInsn(Opcodes.ICONST_5);
                                break;
                            default:
                                if (value < Byte.MAX_VALUE) {
                                    methodVisitor.visitIntInsn(Opcodes.BIPUSH, value);
                                } else if (value < Short.MAX_VALUE) {
                                    methodVisitor.visitIntInsn(Opcodes.SIPUSH, value);
                                } else {
                                    methodVisitor.visitLdcInsn(value);
                                }
                        }
                    }

                    @Override
                    public int resolveIncrement(MethodVisitor methodVisitor, int increment) {
                        throw new IllegalStateException(); // TODO
                    }
                }

                enum ForNullConstant implements Target {

                    INSTANCE;

                    @Override
                    public int resolveAccess(MethodVisitor methodVisitor, int opcode) {
                        switch (opcode) {
                            case Opcodes.ALOAD:
                                methodVisitor.visitInsn(Opcodes.ACONST_NULL);
                                break;
                            case Opcodes.ILOAD:
                            case Opcodes.LLOAD:
                            case Opcodes.FLOAD:
                            case Opcodes.DLOAD:
                            case Opcodes.ISTORE:
                            case Opcodes.FSTORE:
                            case Opcodes.ASTORE:
                            case Opcodes.LSTORE:
                            case Opcodes.DSTORE:
                                throw new IllegalStateException(); // TODO
                            default:
                                throw new IllegalStateException("Unexpected opcode: " + opcode);
                        }
                        return NO_PADDING;
                    }

                    @Override
                    public int resolveIncrement(MethodVisitor methodVisitor, int increment) {
                        throw new IllegalStateException(); // TODO
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
                 * The type expected by the advice method.
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
                 * @param targetType The type expected by the advice method.
                 */
                protected ForParameter(int index, boolean readOnly, TypeDescription targetType) {
                    this.index = index;
                    this.readOnly = readOnly;
                    this.targetType = targetType;
                }

                @Override
                public Target resolve(MethodDescription.InDefinedShape instrumentedMethod, Context context) {
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
                 * The type that the advice method expects for the {@code this} reference.
                 */
                private final TypeDescription targetType;

                /**
                 * Creates a new offset mapping for a {@code this} reference.
                 *
                 * @param readOnly   Determines if the parameter is to be treated as read-only.
                 * @param targetType The type that the advice method expects for the {@code this} reference.
                 */
                protected ForThisReference(boolean readOnly, TypeDescription targetType) {
                    this.readOnly = readOnly;
                    this.targetType = targetType;
                }

                @Override
                public Target resolve(MethodDescription.InDefinedShape instrumentedMethod, Context context) {
                    if (instrumentedMethod.isStatic()) {
                        throw new IllegalStateException("Cannot map this reference for static method " + instrumentedMethod);
                    } else if (!readOnly && !instrumentedMethod.getDeclaringType().equals(targetType)) {
                        throw new IllegalStateException("Declaring type of " + instrumentedMethod + " is not equal to read-only " + targetType);
                    } else if (readOnly && !instrumentedMethod.getDeclaringType().isAssignableTo(targetType)) {
                        throw new IllegalStateException("Declaring type of " + instrumentedMethod + " is not assignable to " + targetType);
                    } else if (!context.isInitialized()) {
                        throw new IllegalStateException("Cannot access this reference before calling constructor: " + instrumentedMethod);
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

                protected final boolean readOnly;

                /**
                 * Creates an offset mapping for a field.
                 *
                 * @param name       The name of the field.
                 * @param targetType The expected type that the field can be assigned to.
                 * @param readOnly
                 */
                protected ForField(String name, TypeDescription targetType, boolean readOnly) {
                    this.name = name;
                    this.targetType = targetType;
                    this.readOnly = readOnly;
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
                public Target resolve(MethodDescription.InDefinedShape instrumentedMethod, Context context) {
                    FieldLocator.Resolution resolution = fieldLocator(instrumentedMethod.getDeclaringType()).locate(name);
                    if (!resolution.isResolved()) {
                        throw new IllegalStateException("Cannot locate field named " + name + " for " + instrumentedMethod);
                    } else if (readOnly && !resolution.getField().getType().asErasure().isAssignableTo(targetType)) {
                        throw new IllegalStateException("Cannot assign type of read-only field " + resolution.getField() + " to " + targetType);
                    } else if (!readOnly && !resolution.getField().getType().asErasure().equals(targetType)) {
                        throw new IllegalStateException("Type of field " + resolution.getField() + " is not equal to " + targetType);
                    } else if (!resolution.getField().isStatic() && instrumentedMethod.isStatic()) {
                        throw new IllegalStateException("Cannot read non-static field " + resolution.getField() + " from static method " + instrumentedMethod);
                    } else if (!context.isInitialized() && !resolution.getField().isStatic()) {
                        throw new IllegalStateException("Cannot access non-static field before calling constructor: " + instrumentedMethod);
                    }
                    return readOnly
                            ? new Target.ForReadOnlyField(resolution.getField())
                            : new Target.ForField(resolution.getField());
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
                    protected WithImplicitType(String name, TypeDescription targetType, boolean readOnly) {
                        super(name, targetType, readOnly);
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
                    protected WithExplicitType(String name, TypeDescription targetType, TypeDescription locatedType, boolean readOnly) {
                        super(name, targetType, readOnly);
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
                     * The {@link FieldValue#value()} method.
                     */
                    private final MethodDescription.InDefinedShape value;

                    /**
                     * The {@link FieldValue#declaringType()}} method.
                     */
                    private final MethodDescription.InDefinedShape definingType;

                    private final MethodDescription.InDefinedShape readOnly;

                    /**
                     * Creates a new factory for a {@link ForField} offset mapping.
                     */
                    Factory() {
                        MethodList<MethodDescription.InDefinedShape> methods = new TypeDescription.ForLoadedType(FieldValue.class).getDeclaredMethods();
                        value = methods.filter(named("value")).getOnly();
                        definingType = methods.filter(named("declaringType")).getOnly();
                        readOnly = methods.filter(named("readOnly")).getOnly();
                    }

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
                                    ? new WithImplicitType(name, targetType, annotation.getValue(readOnly, Boolean.class))
                                    : new WithExplicitType(name, targetType, definingType, annotation.getValue(readOnly, Boolean.class));
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
                public Target resolve(MethodDescription.InDefinedShape instrumentedMethod, Context context) {
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
                public Target resolve(MethodDescription.InDefinedShape instrumentedMethod, Context context) {
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
             * An offset mapping that provides access to the value that is returned by the enter advice.
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
                public Target resolve(MethodDescription.InDefinedShape instrumentedMethod, Context context) {
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
                 * The type that the advice method expects for the {@code this} reference.
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
                public Target resolve(MethodDescription.InDefinedShape instrumentedMethod, Context context) {
                    if (!readOnly && !instrumentedMethod.getReturnType().asErasure().equals(targetType)) {
                        throw new IllegalStateException("read-only return type of " + instrumentedMethod + " is not equal to " + targetType);
                    } else if (readOnly && !instrumentedMethod.getReturnType().asErasure().isAssignableTo(targetType)) {
                        throw new IllegalStateException("Cannot assign return type of " + instrumentedMethod + " to " + targetType);
                    }
                    return readOnly
                            ? new Target.ForReadOnlyParameter(instrumentedMethod.getStackSize() + context.getPadding())
                            : new Target.ForParameter(instrumentedMethod.getStackSize() + context.getPadding());
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

            enum ForBoxedReturnValue implements OffsetMapping, Factory {

                INSTANCE;

                @Override
                public Target resolve(MethodDescription.InDefinedShape instrumentedMethod, Context context) {
                    if (instrumentedMethod.getReturnType().represents(void.class)) {
                        return Target.ForNullConstant.INSTANCE;
                    } else if (instrumentedMethod.getReturnType().isPrimitive()) {
                        return new Target.ForReadOnlyParameter(instrumentedMethod.getStackSize() + context.getPadding());
                    } else {
                        return Target.ForBoxedParameter.of(instrumentedMethod.getStackSize() + context.getPadding(), instrumentedMethod.getReturnType());
                    }
                }

                @Override
                public OffsetMapping make(ParameterDescription.InDefinedShape parameterDescription) {
                    if (!parameterDescription.getDeclaredAnnotations().isAnnotationPresent(BoxedReturn.class)) {
                        return UNDEFINED;
                    } else if (!parameterDescription.getType().represents(Object.class)) {
                        throw new IllegalStateException(); // TODO
                    } else {
                        return this;
                    }
                }
            }

            enum ForBoxedArguments implements OffsetMapping, Factory {

                INSTANCE;

                @Override
                public Target resolve(MethodDescription.InDefinedShape instrumentedMethod, Context context) {
                    return new Target.ForBoxedArguments(instrumentedMethod.getParameters());
                }

                @Override
                public OffsetMapping make(ParameterDescription.InDefinedShape parameterDescription) {
                    if (!parameterDescription.getDeclaredAnnotations().isAnnotationPresent(BoxedArguments.class)) {
                        return UNDEFINED;
                    } else if (!parameterDescription.getType().represents(Object[].class)) {
                        throw new IllegalStateException(); // TODO
                    } else {
                        return this;
                    }
                }
            }

            enum ForOriginType implements OffsetMapping, Factory {

                INSTANCE;

                @Override
                public Target resolve(MethodDescription.InDefinedShape instrumentedMethod, Context context) {
                    return new Target.ForConstantPoolValue(Type.getType(instrumentedMethod.getDeclaringType().getDescriptor()));
                }

                @Override
                public OffsetMapping make(ParameterDescription.InDefinedShape parameterDescription) {
                    if (!parameterDescription.getDeclaredAnnotations().isAnnotationPresent(OriginType.class)) {
                        return UNDEFINED;
                    } else if (!parameterDescription.getType().represents(Class.class)) {
                        throw new IllegalStateException(); // TODO
                    } else {
                        return this;
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
                public Target resolve(MethodDescription.InDefinedShape instrumentedMethod, Context context) {
                    return new Target.ForParameter(instrumentedMethod.getStackSize()
                            + context.getPadding()
                            + instrumentedMethod.getReturnType().getStackSize().getSize());
                }

                @Override
                public String toString() {
                    return "Advice.Dispatcher.Active.Resolved.OffsetMapping.ForThrowable." + name();
                }
            }

            class ForUserValue implements OffsetMapping {

                private final ParameterDescription.InDefinedShape mappedParameter;

                private final AnnotationDescription userAnnotation;

                private final DynamicValue<?> dynamicValue;

                protected ForUserValue(ParameterDescription.InDefinedShape mappedParameter,
                                       AnnotationDescription userAnnotation,
                                       DynamicValue<?> dynamicValue) {
                    this.mappedParameter = mappedParameter;
                    this.userAnnotation = userAnnotation;
                    this.dynamicValue = dynamicValue;
                }

                @Override
                public Target resolve(MethodDescription.InDefinedShape instrumentedMethod, Context context) {
                    Object userValue = dynamicValue.resolve(instrumentedMethod, mappedParameter, userAnnotation, context.isInitialized());
                    if ((instrumentedMethod.getReturnType().represents(String.class) && !(userValue instanceof String))
                            || (instrumentedMethod.getReturnType().represents(Class.class) && !(userValue instanceof TypeDescription || userValue instanceof Class))
                            || (instrumentedMethod.getReturnType().isPrimitive() && !instrumentedMethod.getReturnType().asErasure().isInstanceOrWrapper(userValue))) {
                        throw new IllegalStateException("Cannot map " + userValue + " as constant value of " + instrumentedMethod.getReturnType());
                    } else if (userValue instanceof TypeDescription) {
                        userValue = Type.getType(((TypeDescription) userValue).getDescriptor());
                    } else if (userValue instanceof Class) {
                        userValue = Type.getType((Class<?>) userValue);
                    }
                    return new Target.ForConstantPoolValue(userValue);
                }

                @Override
                public boolean equals(Object object) {
                    if (this == object) return true;
                    if (object == null || getClass() != object.getClass()) return false;
                    ForUserValue that = (ForUserValue) object;
                    return mappedParameter.equals(that.mappedParameter)
                            && userAnnotation.equals(that.userAnnotation)
                            && dynamicValue.equals(that.dynamicValue);
                }

                @Override
                public int hashCode() {
                    int result = mappedParameter.hashCode();
                    result = 31 * result + userAnnotation.hashCode();
                    result = 31 * result + dynamicValue.hashCode();
                    return result;
                }

                @Override
                public String toString() {
                    return "Advice.Dispatcher.OffsetMapping.ForUserValue{" +
                            "mappedParameter=" + mappedParameter +
                            ", userAnnotation=" + userAnnotation +
                            ", dynamicValue=" + dynamicValue +
                            '}';
                }

                protected static class Factory implements OffsetMapping.Factory {

                    private final TypeDescription annotationType;

                    private final DynamicValue<?> dynamicValue;

                    protected Factory(TypeDescription annotationType, DynamicValue<?> dynamicValue) {
                        this.annotationType = annotationType;
                        this.dynamicValue = dynamicValue;
                    }

                    @Override
                    public OffsetMapping make(ParameterDescription.InDefinedShape parameterDescription) {
                        AnnotationDescription annotation = parameterDescription.getDeclaredAnnotations().ofType(annotationType);
                        return annotation == null
                                ? UNDEFINED
                                : new ForUserValue(parameterDescription, annotation, dynamicValue);
                    }

                    @Override
                    public boolean equals(Object object) {
                        if (this == object) return true;
                        if (object == null || getClass() != object.getClass()) return false;
                        Factory factory = (Factory) object;
                        return annotationType.equals(factory.annotationType) && dynamicValue.equals(factory.dynamicValue);
                    }

                    @Override
                    public int hashCode() {
                        int result = annotationType.hashCode();
                        result = 31 * result + dynamicValue.hashCode();
                        return result;
                    }

                    @Override
                    public String toString() {
                        return "Advice.Dispatcher.OffsetMapping.ForUserValue.Factory{" +
                                "annotationType=" + annotationType +
                                ", dynamicValue=" + dynamicValue +
                                '}';
                    }
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
         * Represents a resolved dispatcher.
         */
        interface Resolved extends Dispatcher {

            /**
             * Applies this dispatcher for a method that is discovered in the advice class's class file.
             *
             * @param internalName       The discovered method's internal name.
             * @param descriptor         The discovered method's descriptor.
             * @param methodVisitor      The method visitor for writing the instrumented method.
             * @param metaDataHandler    A handler for translating meta data that is embedded into the instrumented method's byte code.
             * @param instrumentedMethod A description of the instrumented method.
             * @return A method visitor for reading the discovered method or {@code null} if the discovered method is of no interest.
             */
            MethodVisitor apply(String internalName,
                                String descriptor,
                                MethodVisitor methodVisitor,
                                MetaDataHandler.ForInstrumentedMethod metaDataHandler,
                                MethodDescription.InDefinedShape instrumentedMethod);

            /**
             * Represents a resolved dispatcher for entering a method.
             */
            interface ForMethodEnter extends Resolved {

                /**
                 * Returns the type that this dispatcher supplies as a result of its advice or a description of {@code void} if
                 * no type is supplied as a result of the enter advice.
                 *
                 * @return The type that this dispatcher supplies as a result of its advice or a description of {@code void}.
                 */
                TypeDescription getEnterType();
            }

            /**
             * Represents a resolved dispatcher for exiting a method.
             */
            interface ForMethodExit extends Resolved {

                /**
                 * Indicates if this advice requires to be called when the instrumented method terminates exceptionally.
                 *
                 * @return {@code true} if this advice requires to be called when the instrumented method terminates exceptionally.
                 */
                boolean isSkipThrowable();
            }
        }

        /**
         * An implementation for inactive devise that does not write any byte code.
         */
        enum Inactive implements Dispatcher.Unresolved, Resolved.ForMethodEnter, Resolved.ForMethodExit {

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
            public Resolved.ForMethodEnter asMethodEnter(List<? extends OffsetMapping.Factory> userFactories) {
                return this;
            }

            @Override
            public Resolved.ForMethodExit asMethodExitTo(List<? extends OffsetMapping.Factory> userFactories, ForMethodEnter dispatcher) {
                return this;
            }

            @Override
            public MethodVisitor apply(String internalName,
                                       String descriptor,
                                       MethodVisitor methodVisitor,
                                       MetaDataHandler.ForInstrumentedMethod metaDataHandler,
                                       MethodDescription.InDefinedShape instrumentedMethod) {
                return IGNORE_METHOD;
            }

            @Override
            public String toString() {
                return "Advice.Dispatcher.Inactive." + name();
            }
        }

        /**
         * A dispatcher for active advice.
         */
        class Active implements Unresolved {

            /**
             * The advice method.
             */
            protected final MethodDescription.InDefinedShape adviceMethod;

            /**
             * Creates a dispatcher for active advice.
             *
             * @param adviceMethod The advice method.
             */
            protected Active(MethodDescription.InDefinedShape adviceMethod) {
                this.adviceMethod = adviceMethod;
            }

            @Override
            public boolean isAlive() {
                return true;
            }

            @Override
            public Dispatcher.Resolved.ForMethodEnter asMethodEnter(List<? extends OffsetMapping.Factory> userFactories) {
                return new Resolved.ForMethodEnter(adviceMethod, userFactories);
            }

            @Override
            public Dispatcher.Resolved.ForMethodExit asMethodExitTo(List<? extends OffsetMapping.Factory> userFactories,
                                                                    Dispatcher.Resolved.ForMethodEnter dispatcher) {
                return Resolved.ForMethodExit.of(adviceMethod, userFactories, dispatcher.getEnterType());
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass()) && adviceMethod.equals(((Active) other).adviceMethod);
            }

            @Override
            public int hashCode() {
                return adviceMethod.hashCode();
            }

            @Override
            public String toString() {
                return "Advice.Dispatcher.Active{" +
                        "adviceMethod=" + adviceMethod +
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
                 * The represented advice method.
                 */
                protected final MethodDescription.InDefinedShape adviceMethod;

                /**
                 * An unresolved mapping of offsets of the advice method based on the annotations discovered on each method parameter.
                 */
                protected final Map<Integer, OffsetMapping> offsetMappings;

                /**
                 * Creates a new resolved version of a dispatcher.
                 *
                 * @param adviceMethod The represented advice method.
                 */
                protected Resolved(MethodDescription.InDefinedShape adviceMethod, List<OffsetMapping.Factory> factories) {
                    this.adviceMethod = adviceMethod;
                    offsetMappings = new HashMap<Integer, OffsetMapping>();
                    for (ParameterDescription.InDefinedShape parameterDescription : adviceMethod.getParameters()) {
                        OffsetMapping offsetMapping = OffsetMapping.Factory.UNDEFINED;
                        for (OffsetMapping.Factory factory : factories) {
                            OffsetMapping possible = factory.make(parameterDescription);
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
                public boolean isAlive() {
                    return true;
                }

                @Override
                public MethodVisitor apply(String internalName,
                                           String descriptor,
                                           MethodVisitor methodVisitor,
                                           MetaDataHandler.ForInstrumentedMethod metaDataHandler,
                                           MethodDescription.InDefinedShape instrumentedMethod) {
                    return adviceMethod.getInternalName().equals(internalName) && adviceMethod.getDescriptor().equals(descriptor)
                            ? apply(methodVisitor, metaDataHandler, instrumentedMethod)
                            : IGNORE_METHOD;
                }

                /**
                 * Applies a resolution for a given instrumented method.
                 *
                 * @param methodVisitor      A method visitor for writing byte code to the instrumented method.
                 * @param metaDataHandler    A handler for translating meta data that is embedded into the instrumented method's byte code.
                 * @param instrumentedMethod A description of the instrumented method.
                 * @return A method visitor for visiting the advice method's byte code.
                 */
                protected abstract MethodVisitor apply(MethodVisitor methodVisitor,
                                                       MetaDataHandler.ForInstrumentedMethod metaDataHandler,
                                                       MethodDescription.InDefinedShape instrumentedMethod);

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (other == null || getClass() != other.getClass()) return false;
                    Active.Resolved resolved = (Active.Resolved) other;
                    return adviceMethod.equals(resolved.adviceMethod) && offsetMappings.equals(resolved.offsetMappings);
                }

                @Override
                public int hashCode() {
                    int result = adviceMethod.hashCode();
                    result = 31 * result + offsetMappings.hashCode();
                    return result;
                }

                /**
                 * A resolved dispatcher for implementing method enter advice.
                 */
                protected static class ForMethodEnter extends Active.Resolved implements Dispatcher.Resolved.ForMethodEnter {

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
                     * Creates a new resolved dispatcher for implementing method enter advice.
                     *
                     * @param adviceMethod The represented advice method.
                     */
                    @SuppressWarnings("all") // In absence of @SafeVarargs for Java 6
                    protected ForMethodEnter(MethodDescription.InDefinedShape adviceMethod, List<? extends OffsetMapping.Factory> userFactories) {
                        super(adviceMethod,
                                CompoundList.of(Arrays.asList(OffsetMapping.ForParameter.Factory.INSTANCE,
                                        OffsetMapping.ForBoxedArguments.INSTANCE,
                                        OffsetMapping.ForThisReference.Factory.INSTANCE,
                                        OffsetMapping.ForField.Factory.INSTANCE,
                                        OffsetMapping.ForOrigin.Factory.INSTANCE,
                                        OffsetMapping.ForOriginType.INSTANCE,
                                        OffsetMapping.ForIgnored.INSTANCE,
                                        new OffsetMapping.Illegal(Thrown.class, Enter.class, Return.class)), userFactories));
                    }

                    @Override
                    public TypeDescription getEnterType() {
                        return adviceMethod.getReturnType().asErasure();
                    }

                    @Override
                    protected MethodVisitor apply(MethodVisitor methodVisitor,
                                                  MetaDataHandler.ForInstrumentedMethod metaDataHandler,
                                                  MethodDescription.InDefinedShape instrumentedMethod) {
                        Map<Integer, OffsetMapping.Target> offsetMappings = new HashMap<Integer, OffsetMapping.Target>();
                        for (Map.Entry<Integer, OffsetMapping> entry : this.offsetMappings.entrySet()) {
                            offsetMappings.put(entry.getKey(), entry.getValue().resolve(instrumentedMethod, OffsetMapping.Context.ForMethodEntry.of(instrumentedMethod)));
                        }
                        return new CodeTranslationVisitor.ForMethodEnter(methodVisitor,
                                metaDataHandler.bindEntry(adviceMethod),
                                instrumentedMethod,
                                adviceMethod,
                                offsetMappings,
                                adviceMethod.getDeclaredAnnotations().ofType(OnMethodEnter.class).getValue(SUPPRESS, TypeDescription.class));
                    }

                    @Override
                    public String toString() {
                        return "Advice.Dispatcher.Active.Resolved.ForMethodEnter{" +
                                "adviceMethod=" + adviceMethod +
                                ", offsetMappings=" + offsetMappings +
                                '}';
                    }
                }

                /**
                 * A resolved dispatcher for implementing method exit advice.
                 */
                protected abstract static class ForMethodExit extends Active.Resolved implements Dispatcher.Resolved.ForMethodExit {

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
                     * Creates a new resolved dispatcher for implementing method exit advice.
                     *
                     * @param adviceMethod The represented advice method.
                     * @param enterType    The type of the value supplied by the enter advice method or
                     *                     a description of {@code void} if no such value exists.
                     */
                    @SuppressWarnings("all") // In absence of @SafeVarargs for Java 6
                    protected ForMethodExit(MethodDescription.InDefinedShape adviceMethod,
                                            List<? extends OffsetMapping.Factory> userFactories,
                                            TypeDescription enterType) {
                        super(adviceMethod,
                                CompoundList.of(Arrays.asList(OffsetMapping.ForParameter.Factory.INSTANCE,
                                        OffsetMapping.ForBoxedArguments.INSTANCE,
                                        OffsetMapping.ForThisReference.Factory.INSTANCE,
                                        OffsetMapping.ForField.Factory.INSTANCE,
                                        OffsetMapping.ForOrigin.Factory.INSTANCE,
                                        OffsetMapping.ForOriginType.INSTANCE,
                                        OffsetMapping.ForIgnored.INSTANCE,
                                        new OffsetMapping.ForEnterValue.Factory(enterType),
                                        OffsetMapping.ForReturnValue.Factory.INSTANCE,
                                        OffsetMapping.ForBoxedReturnValue.INSTANCE,
                                        adviceMethod.getDeclaredAnnotations().ofType(OnMethodExit.class).loadSilent().onThrowable()
                                                ? OffsetMapping.ForThrowable.INSTANCE
                                                : new OffsetMapping.Illegal(Thrown.class)), userFactories));
                        this.enterType = enterType;
                    }

                    /**
                     * Resolves exit advice that handles exceptions depending on the specification of the exit advice.
                     *
                     * @param adviceMethod The advice method.
                     * @param enterType    The type of the value supplied by the enter advice method or
                     *                     a description of {@code void} if no such value exists.
                     * @return An appropriate exit handler.
                     */
                    protected static Resolved.ForMethodExit of(MethodDescription.InDefinedShape adviceMethod,
                                                               List<? extends OffsetMapping.Factory> userFactories,
                                                               TypeDescription enterType) {
                        return adviceMethod.getDeclaredAnnotations().ofType(OnMethodExit.class).loadSilent().onThrowable()
                                ? new WithExceptionHandler(adviceMethod, userFactories, enterType)
                                : new WithoutExceptionHandler(adviceMethod, userFactories, enterType);
                    }

                    @Override
                    protected MethodVisitor apply(MethodVisitor methodVisitor,
                                                  MetaDataHandler.ForInstrumentedMethod metaDataHandler,
                                                  MethodDescription.InDefinedShape instrumentedMethod) {
                        Map<Integer, OffsetMapping.Target> offsetMappings = new HashMap<Integer, OffsetMapping.Target>();
                        for (Map.Entry<Integer, OffsetMapping> entry : this.offsetMappings.entrySet()) {
                            offsetMappings.put(entry.getKey(), entry.getValue().resolve(instrumentedMethod, OffsetMapping.Context.ForMethodExit.of(enterType)));
                        }
                        return new CodeTranslationVisitor.ForMethodExit(methodVisitor,
                                metaDataHandler.bindExit(adviceMethod),
                                instrumentedMethod,
                                adviceMethod,
                                offsetMappings,
                                adviceMethod.getDeclaredAnnotations().ofType(OnMethodExit.class).getValue(SUPPRESS, TypeDescription.class),
                                enterType.getStackSize().getSize() + getAdditionalPadding().getSize());
                    }

                    /**
                     * Returns the additional padding this exit advice implies.
                     *
                     * @return The additional padding this exit advice implies.
                     */
                    protected abstract StackSize getAdditionalPadding();

                    @Override
                    public boolean equals(Object other) {
                        return this == other || !(other == null || getClass() != other.getClass())
                                && super.equals(other)
                                && enterType == ((Active.Resolved.ForMethodExit) other).enterType;
                    }

                    @Override
                    public int hashCode() {
                        int result = super.hashCode();
                        result = 31 * result + enterType.hashCode();
                        return result;
                    }

                    /**
                     * Implementation of exit advice that handles exceptions.
                     */
                    protected static class WithExceptionHandler extends Active.Resolved.ForMethodExit {

                        /**
                         * Creates a new resolved dispatcher for implementing method exit advice that handles exceptions.
                         *
                         * @param adviceMethod The represented advice method.
                         * @param enterType    The type of the value supplied by the enter advice method or
                         *                     a description of {@code void} if no such value exists.
                         */
                        protected WithExceptionHandler(MethodDescription.InDefinedShape adviceMethod,
                                                       List<? extends OffsetMapping.Factory> userFactories,
                                                       TypeDescription enterType) {
                            super(adviceMethod, userFactories, enterType);
                        }

                        @Override
                        protected StackSize getAdditionalPadding() {
                            return StackSize.SINGLE;
                        }

                        @Override
                        public boolean isSkipThrowable() {
                            return false;
                        }

                        @Override
                        public String toString() {
                            return "Advice.Dispatcher.Active.Resolved.ForMethodExit.WithExceptionHandler{" +
                                    "adviceMethod=" + adviceMethod +
                                    ", offsetMappings=" + offsetMappings +
                                    '}';
                        }
                    }

                    /**
                     * Implementation of exit advice that ignores exceptions.
                     */
                    protected static class WithoutExceptionHandler extends Active.Resolved.ForMethodExit {

                        /**
                         * Creates a new resolved dispatcher for implementing method exit advice that does not handle exceptions.
                         *
                         * @param adviceMethod The represented advice method.
                         * @param enterType    The type of the value supplied by the enter advice method or a description of {@code void} if no such value exists.
                         */
                        protected WithoutExceptionHandler(MethodDescription.InDefinedShape adviceMethod,
                                                          List<? extends OffsetMapping.Factory> userFactories,
                                                          TypeDescription enterType) {
                            super(adviceMethod, userFactories, enterType);
                        }

                        @Override
                        protected StackSize getAdditionalPadding() {
                            return StackSize.SINGLE;
                        }

                        @Override
                        public boolean isSkipThrowable() {
                            return true;
                        }

                        @Override
                        public String toString() {
                            return "Advice.Dispatcher.Active.Resolved.ForMethodExit.WithoutExceptionHandler{" +
                                    "adviceMethod=" + adviceMethod +
                                    ", offsetMappings=" + offsetMappings +
                                    '}';
                        }
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
             * A visitor for translating an advice method's byte code for inlining into the instrumented method.
             */
            protected abstract static class CodeTranslationVisitor extends MethodVisitor implements ReturnValueProducer {

                /**
                 * Indicates that an annotation should not be read.
                 */
                private static final AnnotationVisitor IGNORE_ANNOTATION = null;

                /**
                 * A handler for translating meta data found in the byte code.
                 */
                protected final MetaDataHandler.ForAdvice metaDataHandler;

                /**
                 * The instrumented method.
                 */
                protected final MethodDescription.InDefinedShape instrumentedMethod;

                /**
                 * The advice method.
                 */
                protected final MethodDescription.InDefinedShape adviceMethod;

                /**
                 * A mapping of offsets to resolved target offsets in the instrumented method.
                 */
                private final Map<Integer, Resolved.OffsetMapping.Target> offsetMappings;

                /**
                 * A handler for optionally suppressing exceptions.
                 */
                private final SuppressionHandler suppressionHandler;

                /**
                 * A label indicating the end of the advice byte code.
                 */
                protected final Label endOfMethod;

                /**
                 * Creates a new code translation visitor.
                 *
                 * @param methodVisitor      A method visitor for writing the instrumented method's byte code.
                 * @param metaDataHandler    A handler for translating meta data found in the byte code.
                 * @param instrumentedMethod The instrumented method.
                 * @param adviceMethod       The advice method.
                 * @param offsetMappings     A mapping of offsets to resolved target offsets in the instrumented method.
                 * @param throwableType      A throwable type to be suppressed or {@link NoSuppression} if no suppression should be applied.
                 */
                protected CodeTranslationVisitor(MethodVisitor methodVisitor,
                                                 MetaDataHandler.ForAdvice metaDataHandler,
                                                 MethodDescription.InDefinedShape instrumentedMethod,
                                                 MethodDescription.InDefinedShape adviceMethod,
                                                 Map<Integer, Resolved.OffsetMapping.Target> offsetMappings,
                                                 TypeDescription throwableType) {
                    super(Opcodes.ASM5, methodVisitor);
                    this.metaDataHandler = metaDataHandler;
                    this.instrumentedMethod = instrumentedMethod;
                    this.adviceMethod = adviceMethod;
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
                public void visitAttribute(Attribute attribute) {
                    /* do nothing */
                }

                @Override
                public void visitCode() {
                    suppressionHandler.onStart(mv, metaDataHandler);
                }

                @Override
                public void visitFrame(int frameType, int localVariableLength, Object[] localVariable, int stackSize, Object[] stack) {
                    metaDataHandler.translateFrame(mv, frameType, localVariableLength, localVariable, stackSize, stack);
                }

                @Override
                public void visitLineNumber(int line, Label start) {
                    /* do nothing */
                }

                @Override
                public void visitEnd() {
                    suppressionHandler.onEnd(mv, metaDataHandler, this);
                    mv.visitLabel(endOfMethod);
                    metaDataHandler.injectCompletionFrame(mv, false);
                }

                @Override
                public void visitMaxs(int stackSize, int localVariableLength) {
                    metaDataHandler.recordMaxima(stackSize, localVariableLength);
                }

                @Override
                public void visitVarInsn(int opcode, int offset) {
                    Resolved.OffsetMapping.Target target = offsetMappings.get(offset);
                    if (target != null) {
                        metaDataHandler.recordStackPadding(target.resolveAccess(mv, opcode));
                    } else {
                        mv.visitVarInsn(opcode, adjust(offset + instrumentedMethod.getStackSize() - adviceMethod.getStackSize()));
                    }
                }

                @Override
                public void visitIincInsn(int offset, int increment) {
                    Resolved.OffsetMapping.Target target = offsetMappings.get(offset);
                    if (target != null) {
                        metaDataHandler.recordStackPadding(target.resolveIncrement(mv, increment));
                    } else {
                        mv.visitIincInsn(adjust(offset + instrumentedMethod.getStackSize() - adviceMethod.getStackSize()), increment);
                    }
                }

                /**
                 * Adjusts the offset of a variable instruction within the advice method such that no arguments to
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
                     * @param metaDataHandler The meta data handler to use for translating meta data.
                     */
                    void onStart(MethodVisitor methodVisitor, MetaDataHandler.ForAdvice metaDataHandler);

                    /**
                     * Invoked at the end of a method.
                     *
                     * @param methodVisitor       The method visitor of the instrumented method.
                     * @param metaDataHandler     The meta data handler to use for translating meta data.
                     * @param returnValueProducer A producer for defining a default return value of the advised method.
                     */
                    void onEnd(MethodVisitor methodVisitor, MetaDataHandler.ForAdvice metaDataHandler, ReturnValueProducer returnValueProducer);

                    /**
                     * A non-operational suppression handler that does not suppress any method.
                     */
                    enum NoOp implements SuppressionHandler {

                        /**
                         * The singleton instance.
                         */
                        INSTANCE;

                        @Override
                        public void onStart(MethodVisitor methodVisitor, MetaDataHandler.ForAdvice metaDataHandler) {
                            /* do nothing */
                        }

                        @Override
                        public void onEnd(MethodVisitor methodVisitor, MetaDataHandler.ForAdvice metaDataHandler, ReturnValueProducer returnValueProducer) {
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
                         * A label indicating the end of the method.
                         */
                        private final Label endOfMethod;

                        /**
                         * Creates a new suppressing suppression handler.
                         *
                         * @param throwableType The suppressed throwable type.
                         */
                        protected Suppressing(TypeDescription throwableType) {
                            this.throwableType = throwableType;
                            startOfMethod = new Label();
                            endOfMethod = new Label();
                        }

                        @Override
                        public void onStart(MethodVisitor methodVisitor, MetaDataHandler.ForAdvice metaDataHandler) {
                            methodVisitor.visitTryCatchBlock(startOfMethod, endOfMethod, endOfMethod, throwableType.getInternalName());
                            methodVisitor.visitLabel(startOfMethod);
                        }

                        @Override
                        public void onEnd(MethodVisitor methodVisitor, MetaDataHandler.ForAdvice metaDataHandler, ReturnValueProducer returnValueProducer) {
                            Label endOfHandler = new Label();
                            methodVisitor.visitLabel(endOfMethod);
                            metaDataHandler.injectHandlerFrame(methodVisitor);
                            methodVisitor.visitInsn(Opcodes.POP);
                            returnValueProducer.makeDefault(methodVisitor);
                            methodVisitor.visitLabel(endOfHandler);
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
                                    ", endOfMethod=" + endOfMethod +
                                    '}';
                        }
                    }
                }

                /**
                 * A code translation visitor that retains the return value of the represented advice method.
                 */
                protected static class ForMethodEnter extends CodeTranslationVisitor {

                    /**
                     * Creates a code translation visitor for translating exit advice.
                     *
                     * @param methodVisitor      A method visitor for writing the instrumented method's byte code.
                     * @param metaDataHandler    A handler for translating meta data found in the byte code.
                     * @param instrumentedMethod The instrumented method.
                     * @param adviceMethod       The advice method.
                     * @param offsetMappings     A mapping of offsets to resolved target offsets in the instrumented method.
                     * @param throwableType      A throwable type to be suppressed or {@link NoSuppression} if no suppression should be applied.
                     */
                    protected ForMethodEnter(MethodVisitor methodVisitor,
                                             MetaDataHandler.ForAdvice metaDataHandler,
                                             MethodDescription.InDefinedShape instrumentedMethod,
                                             MethodDescription.InDefinedShape adviceMethod,
                                             Map<Integer, Resolved.OffsetMapping.Target> offsetMappings,
                                             TypeDescription throwableType) {
                        super(methodVisitor, metaDataHandler, instrumentedMethod, adviceMethod, offsetMappings, throwableType);
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
                        if (adviceMethod.getReturnType().represents(boolean.class)
                                || adviceMethod.getReturnType().represents(byte.class)
                                || adviceMethod.getReturnType().represents(short.class)
                                || adviceMethod.getReturnType().represents(char.class)
                                || adviceMethod.getReturnType().represents(int.class)) {
                            methodVisitor.visitInsn(Opcodes.ICONST_0);
                            methodVisitor.visitVarInsn(Opcodes.ISTORE, instrumentedMethod.getStackSize());
                        } else if (adviceMethod.getReturnType().represents(long.class)) {
                            methodVisitor.visitInsn(Opcodes.LCONST_0);
                            methodVisitor.visitVarInsn(Opcodes.LSTORE, instrumentedMethod.getStackSize());
                        } else if (adviceMethod.getReturnType().represents(float.class)) {
                            methodVisitor.visitInsn(Opcodes.FCONST_0);
                            methodVisitor.visitVarInsn(Opcodes.FSTORE, instrumentedMethod.getStackSize());
                        } else if (adviceMethod.getReturnType().represents(double.class)) {
                            methodVisitor.visitInsn(Opcodes.DCONST_0);
                            methodVisitor.visitVarInsn(Opcodes.DSTORE, instrumentedMethod.getStackSize());
                        } else if (!adviceMethod.getReturnType().represents(void.class)) {
                            methodVisitor.visitInsn(Opcodes.ACONST_NULL);
                            methodVisitor.visitVarInsn(Opcodes.ASTORE, instrumentedMethod.getStackSize());
                        }
                    }

                    @Override
                    public String toString() {
                        return "Advice.Dispatcher.Active.CodeTranslationVisitor.ForMethodEnter{" +
                                "instrumentedMethod=" + instrumentedMethod +
                                ", adviceMethod=" + adviceMethod +
                                '}';
                    }
                }

                /**
                 * A code translation visitor that discards the return value of the represented advice method.
                 */
                protected static class ForMethodExit extends CodeTranslationVisitor {

                    /**
                     * The padding after the instrumented method's arguments in the local variable array.
                     */
                    private final int padding;

                    /**
                     * Creates a code translation visitor for translating exit advice.
                     *
                     * @param methodVisitor      A method visitor for writing the instrumented method's byte code.
                     * @param metaDataHandler    A handler for translating meta data found in the byte code.
                     * @param instrumentedMethod The instrumented method.
                     * @param adviceMethod       The advice method.
                     * @param offsetMappings     A mapping of offsets to resolved target offsets in the instrumented method.
                     * @param throwableType      A throwable type to be suppressed or {@link NoSuppression} if no suppression should be applied.
                     * @param padding            The padding after the instrumented method's arguments in the local variable array.
                     */
                    protected ForMethodExit(MethodVisitor methodVisitor,
                                            MetaDataHandler.ForAdvice metaDataHandler,
                                            MethodDescription.InDefinedShape instrumentedMethod,
                                            MethodDescription.InDefinedShape adviceMethod,
                                            Map<Integer, Resolved.OffsetMapping.Target> offsetMappings,
                                            TypeDescription throwableType,
                                            int padding) {
                        super(methodVisitor,
                                metaDataHandler,
                                instrumentedMethod,
                                adviceMethod,
                                offsetMappings,
                                throwableType);
                        this.padding = padding;
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
                        return instrumentedMethod.getReturnType().getStackSize().getSize() + padding + offset;
                    }

                    @Override
                    public void makeDefault(MethodVisitor methodVisitor) {
                        /* do nothing */
                    }

                    @Override
                    public String toString() {
                        return "Advice.Dispatcher.Active.CodeTranslationVisitor.ForMethodExit{" +
                                "instrumentedMethod=" + instrumentedMethod +
                                ", adviceMethod=" + adviceMethod +
                                ", padding=" + padding +
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
         * Indicates that the advice method should also be called when a method terminates exceptionally. This property must
         * not be set to {@code true} when defining advice for a constructor.
         *
         * @return {@code true} if the advice method should be invoked when a method terminates exceptionally.
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

        boolean readOnly() default true;
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

    public @interface OriginType {

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
     * Indicates that the annotated parameter should be mapped to the value that is returned by the advice method that is annotated
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

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public @interface BoxedReturn {

    }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public @interface BoxedArguments {

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

    @SuppressWarnings("unused")
    public interface DynamicValue<T extends Annotation> {

        Serializable resolve(MethodDescription.InDefinedShape instrumentedMethod,
                             ParameterDescription.InDefinedShape mappedParameter,
                             AnnotationDescription annotation,
                             boolean initialized);
    }

    public static class WithCustomMapping {

        private final Map<TypeDescription, DynamicValue<?>> dynamicValues;

        protected WithCustomMapping() {
            this(Collections.<TypeDescription, DynamicValue<?>>emptyMap());
        }

        protected WithCustomMapping(Map<TypeDescription, DynamicValue<?>> dynamicValues) {
            this.dynamicValues = dynamicValues;
        }

        public <T extends Annotation> WithCustomMapping bind(Class<? extends T> type, DynamicValue<T> dynamicValue) {
            return bind(new TypeDescription.ForLoadedType(type), dynamicValue);
        }

        public WithCustomMapping bind(TypeDescription type, DynamicValue<?> dynamicValue) {
            Map<TypeDescription, DynamicValue<?>> dynamicValues = new HashMap<TypeDescription, Advice.DynamicValue<?>>(this.dynamicValues);
            if (!type.isAnnotation()) {
                throw new IllegalArgumentException("Not an annotation type: " + type);
            } else if (dynamicValues.put(type, dynamicValue) != null) {
                throw new IllegalArgumentException("Annotation-type already mapped: " + type);
            }
            return new WithCustomMapping(dynamicValues);
        }

        /**
         * Implements advice where every matched method is advised by the given type's advisory methods. The advices binary representation is
         * accessed by querying the class loader of the supplied class for a class file.
         *
         * @param type The type declaring the advice.
         * @return A method visitor wrapper representing the supplied advice.
         */
        public Advice to(Class<?> type) {
            return to(type, ClassFileLocator.ForClassLoader.of(type.getClassLoader()));
        }

        /**
         * Implements advice where every matched method is advised by the given type's advisory methods.
         *
         * @param type             The type declaring the advice.
         * @param classFileLocator The class file locator for locating the advisory class's class file.
         * @return A method visitor wrapper representing the supplied advice.
         */
        public Advice to(Class<?> type, ClassFileLocator classFileLocator) {
            return to(new TypeDescription.ForLoadedType(type), classFileLocator);
        }

        public Advice to(TypeDescription typeDescription, ClassFileLocator classFileLocator) {
            List<Dispatcher.OffsetMapping.Factory> userFactories = new ArrayList<Dispatcher.OffsetMapping.Factory>(dynamicValues.size());
            for (Map.Entry<TypeDescription, DynamicValue<?>> entry : dynamicValues.entrySet()) {
                userFactories.add(new Dispatcher.OffsetMapping.ForUserValue.Factory(entry.getKey(), entry.getValue()));
            }
            return Advice.to(typeDescription, classFileLocator, userFactories);
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (object == null || getClass() != object.getClass()) return false;
            WithCustomMapping that = (WithCustomMapping) object;
            return dynamicValues.equals(that.dynamicValues);
        }

        @Override
        public int hashCode() {
            return dynamicValues.hashCode();
        }

        @Override
        public String toString() {
            return "Advice.WithCustomMapping{" +
                    "dynamicValues=" + dynamicValues +
                    '}';
        }
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
