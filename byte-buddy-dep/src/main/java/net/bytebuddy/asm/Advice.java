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
import net.bytebuddy.utility.ExceptionTableSensitiveMethodVisitor;
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

    /**
     * Creates a new advice.
     *
     * @param typeDescription  A description of the type declaring the advice.
     * @param classFileLocator The class file locator for locating the advisory class's class file.
     * @param userFactories    A list of custom factories for user generated offset mappings.
     * @return A method visitor wrapper representing the supplied advice.
     */
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

    /**
     * Allows for the configuration of custom annotations that are then bound to a dynamically computed, constant value.
     *
     * @return A builder for an {@link Advice} instrumentation with custom values.
     * @see DynamicValue
     */
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
            int UNDEFINED_SIZE = Short.MAX_VALUE;

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

            /**
             * Records a minimum padding additionally to the computed stack size that is required for implementing this advice method.
             *
             * @param padding The minimum required padding.
             */
            void recordPadding(int padding);
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
            public void recordPadding(int padding) {
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
                              TypeList requiredTypes,
                              TypeList yieldedTypes,
                              boolean expandFrames) {
                this.instrumentedMethod = instrumentedMethod;
                this.requiredTypes = requiredTypes;
                this.yieldedTypes = yieldedTypes;
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
                            new TypeList.Explicit(requiredTypes),
                            new TypeList.Explicit(yieldedTypes),
                            (readerFlags & ClassReader.EXPAND_FRAMES) != 0);
                } else {
                    return new Default.WithStackSizeComputation(instrumentedMethod,
                            new TypeList.Explicit(requiredTypes),
                            new TypeList.Explicit(yieldedTypes),
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
            public ForAdvice bindEntry(MethodDescription.InDefinedShape adviceMethod) {
                return bind(adviceMethod, new TypeList.Empty(), requiredTypes, TranslationMode.ENTRY);
            }

            @Override
            public ForAdvice bindExit(MethodDescription.InDefinedShape adviceMethod) {
                return bind(adviceMethod, new TypeList.Explicit(CompoundList.of(requiredTypes, yieldedTypes)), new TypeList.Empty(), TranslationMode.EXIT);
            }

            /**
             * Binds the given advice method to an appropriate meta data handler.
             *
             * @param adviceMethod The advice method.
             * @param requiredTypes     The expected types that the advice method requires additionally to the instrumented method's parameters.
             * @param yieldedTypes      The types this advice method yields as additional parameters.
             * @param translationMode   The translation mode to apply for this advice.
             * @return An appropriate meta data handler.
             */
            protected abstract ForAdvice bind(MethodDescription.InDefinedShape adviceMethod,
                                              TypeList requiredTypes,
                                              TypeList yieldedTypes,
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
                Object[] localVariable = new Object[instrumentedMethod.getParameters().size()
                        + (instrumentedMethod.isStatic() ? 0 : 1)
                        + additionalTypes.size()];
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
                private final TypeList yieldedTypes;

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
                                    TypeList yieldedTypes,
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
                                                   TypeList requiredTypes,
                                                   TypeList yieldedTypes,
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
                protected Default.ForAdvice bind(MethodDescription.InDefinedShape adviceMethod,
                                                 TypeList requiredTypes,
                                                 TypeList yieldedTypes,
                                                 TranslationMode translationMode) {
                    if (translationMode == TranslationMode.ENTRY) {
                        stackSize = Math.max(adviceMethod.getReturnType().getStackSize().getSize(), stackSize);
                    }
                    return new ForAdvice(adviceMethod, requiredTypes, yieldedTypes, translationMode);
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

                    /**
                     * The padding that this advice method requires additionally to its computed size.
                     */
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
                                        TypeList yieldedTypes,
                                        TranslationMode translationMode) {
                        super(methodDescription, requiredTypes, yieldedTypes, translationMode);
                    }

                    @Override
                    public void recordMaxima(int stackSize, int localVariableLength) {
                        WithStackSizeComputation.this.stackSize = Math.max(WithStackSizeComputation.this.stackSize, stackSize) + padding;
                        WithStackSizeComputation.this.localVariableLength = Math.max(WithStackSizeComputation.this.localVariableLength, localVariableLength
                                - methodDescription.getStackSize()
                                + instrumentedMethod.getStackSize()
                                + requiredTypes.getStackSize());
                    }

                    @Override
                    public void recordPadding(int padding) {
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
                                                      TypeList requiredTypes,
                                                      TypeList yieldedTypes,
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
                protected Default.ForAdvice bind(MethodDescription.InDefinedShape adviceMethod,
                                                 TypeList requiredTypes,
                                                 TypeList yieldedTypes,
                                                 TranslationMode translationMode) {
                    return new ForAdvice(adviceMethod, requiredTypes, yieldedTypes, translationMode);
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
                                        TypeList yieldedTypes,
                                        TranslationMode translationMode) {
                        super(methodDescription, requiredTypes, yieldedTypes, translationMode);
                    }

                    @Override
                    public void recordMaxima(int maxStack, int maxLocals) {
                        /* do nothing */
                    }

                    @Override
                    public void recordPadding(int padding) {
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
    protected abstract static class AdviceVisitor extends ExceptionTableSensitiveMethodVisitor {

        /**
         * Indicates a zero offset.
         */
        private static final int NO_OFFSET = 0;

        /**
         * A description of the instrumented method.
         */
        protected final MethodDescription.InDefinedShape instrumentedMethod;

        /**
         * The required padding before using local variables after the instrumented method's arguments.
         */
        private final int padding;

        /**
         * The dispatcher to be used for method entry.
         */
        private final Dispatcher.Bound methodEnter;

        /**
         * The dispatcher to be used for method exit.
         */
        protected final Dispatcher.Bound methodExit;

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
         * @param methodExit           The method exit advice.
         * @param yieldedTypes         The types that are expected to be added after the instrumented method returns.
         * @param binaryRepresentation The binary representation of the advice class.
         * @param writerFlags          The ASM writer flags that were set.
         * @param readerFlags          The ASM reader flags that were set.
         */
        protected AdviceVisitor(MethodVisitor methodVisitor,
                                MethodDescription.InDefinedShape instrumentedMethod,
                                Dispatcher.Resolved.ForMethodEnter methodEnter,
                                Dispatcher.Resolved.ForMethodExit methodExit,
                                List<? extends TypeDescription> yieldedTypes,
                                byte[] binaryRepresentation,
                                int writerFlags,
                                int readerFlags) {
            super(Opcodes.ASM5, methodVisitor);
            this.instrumentedMethod = instrumentedMethod;
            padding = methodEnter.getEnterType().getStackSize().getSize(); // TODO: doubled enter?
            metaDataHandler = MetaDataHandler.Default.of(instrumentedMethod, methodEnter.getEnterType().represents(void.class)
                    ? Collections.<TypeDescription>emptyList()
                    : Collections.singletonList(methodEnter.getEnterType()), yieldedTypes, writerFlags, readerFlags);
            ClassReader classReader = new ClassReader(binaryRepresentation);
            this.methodEnter = methodEnter.bind(instrumentedMethod, methodVisitor, metaDataHandler, classReader);
            this.methodExit = methodExit.bind(instrumentedMethod, methodVisitor, metaDataHandler, classReader);
        }

        @Override
        protected void onAfterExceptionTable() {
            methodEnter.prepare();
            onUserPrepare();
            methodExit.prepare();
            methodEnter.apply();
            onUserStart();
        }

        /**
         * Invoked when the user method's exception handler (if any) is supposed to be prepared.
         */
        protected abstract void onUserPrepare();

        /**
         * Writes the advice for entering the instrumented method.
         */
        protected abstract void onUserStart();

        @Override
        protected void onVisitVarInsn(int opcode, int offset) {
            mv.visitVarInsn(opcode, offset < instrumentedMethod.getStackSize()
                    ? offset
                    : padding + offset);
        }

        @Override
        protected void onVisitIincInsn(int offset, int increment) {
            mv.visitIincInsn(offset < instrumentedMethod.getStackSize()
                    ? offset
                    : padding + offset, increment);
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
            mv.visitVarInsn(opcode, instrumentedMethod.getStackSize() + padding + offset);
        }

        @Override
        public void visitFrame(int frameType, int localVariableLength, Object[] localVariable, int stackSize, Object[] stack) {
            metaDataHandler.translateFrame(mv, frameType, localVariableLength, localVariable, stackSize, stack);
        }

        @Override
        public void visitMaxs(int stackSize, int localVariableLength) {
            onUserEnd();
            mv.visitMaxs(metaDataHandler.compoundStackSize(stackSize), metaDataHandler.compoundLocalVariableLength(localVariableLength));
        }

        /**
         * Writes the advice for completing the instrumented method.
         */
        protected abstract void onUserEnd();

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
                        Dispatcher.Inactive.INSTANCE,
                        Collections.<TypeDescription>emptyList(),
                        binaryRepresentation,
                        writerFlags,
                        readerFlags);
            }

            @Override
            protected void onUserPrepare() {
                /* do nothing */
            }

            @Override
            protected void onUserStart() {
                /* do nothing */
            }

            @Override
            protected void onUserEnd() {
                /* do nothing */
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
                super(methodVisitor, instrumentedMethod, methodEnter, methodExit, yieldedTypes, binaryRepresentation, writerFlags, readerFlags);
                endOfMethod = new Label();
            }

            @Override
            protected void onVisitInsn(int opcode) {
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
                methodExit.apply();
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
                protected void onUserPrepare() {
                    mv.visitTryCatchBlock(userStart, userEnd, userEnd, ANY_THROWABLE);
                }

                @Override
                protected void onUserStart() {
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
                protected void onUserPrepare() {
                    /* empty */
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
         * Expresses that an annotation should not be visited.
         */
        AnnotationVisitor IGNORE_ANNOTATION = null;

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
             * @param userFactories A list of custom factories for binding parameters of an advice method.
             * @return This dispatcher as a dispatcher for entering a method.
             */
            Resolved.ForMethodEnter asMethodEnter(List<? extends OffsetMapping.Factory> userFactories);

            /**
             * Resolves this dispatcher as a dispatcher for exiting a method.
             *
             * @param userFactories A list of custom factories for binding parameters of an advice method.
             * @param dispatcher    The dispatcher for entering a method.
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
                        return "Advice.Dispatcher.OffsetMapping.Context.ForMethodEntry." + name();
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
                        return "Advice.Dispatcher.OffsetMapping.Context.ForMethodExit." + name();
                    }
                }
            }

            /**
             * A target offset of an offset mapping.
             */
            interface Target {

                /**
                 * Indicates that applying this target does not require any additional padding.
                 */
                int NO_PADDING = 0;

                /**
                 * Applies this offset mapping for a {@link MethodVisitor#visitVarInsn(int, int)} instruction.
                 *
                 * @param methodVisitor The method visitor onto which this offset mapping is to be applied.
                 * @param opcode        The opcode of the original instruction.
                 * @return The required padding to the advice's total stack size.
                 */
                int resolveAccess(MethodVisitor methodVisitor, int opcode);

                /**
                 * Applies this offset mapping for a {@link MethodVisitor#visitIincInsn(int, int)} instruction.
                 *
                 * @param methodVisitor The method visitor onto which this offset mapping is to be applied.
                 * @param increment     The value with which to increment the targeted value.
                 * @return The required padding to the advice's total stack size.
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
                        return "Advice.Dispatcher.OffsetMapping.Target.ForDefaultValue." + name();
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
                        return "Advice.Dispatcher.OffsetMapping.Target.ForParameter{" +
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
                        return "Advice.Dispatcher.OffsetMapping.Target.ForReadOnlyParameter{" +
                                "offset=" + offset +
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
                        return "Advice.Dispatcher.OffsetMapping.Target.ForReadOnlyField{" +
                                "fieldDescription=" + fieldDescription +
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
                                if (!fieldDescription.isStatic()) {
                                    methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                                    methodVisitor.visitInsn(Opcodes.DUP_X1);
                                    methodVisitor.visitInsn(Opcodes.POP);
                                    accessField(methodVisitor, Opcodes.PUTFIELD);
                                    return 2;
                                }
                            case Opcodes.LSTORE:
                            case Opcodes.DSTORE:
                                if (!fieldDescription.isStatic()) {
                                    methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                                    methodVisitor.visitInsn(Opcodes.DUP_X2);
                                    methodVisitor.visitInsn(Opcodes.POP);
                                    accessField(methodVisitor, Opcodes.PUTFIELD);
                                    return 2;
                                }
                                accessField(methodVisitor, Opcodes.PUTSTATIC);
                                return NO_PADDING;
                            case Opcodes.ILOAD:
                            case Opcodes.FLOAD:
                            case Opcodes.ALOAD:
                            case Opcodes.LLOAD:
                            case Opcodes.DLOAD:
                                if (fieldDescription.isStatic()) {
                                    accessField(methodVisitor, Opcodes.GETSTATIC);
                                } else {
                                    methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                                    accessField(methodVisitor, Opcodes.GETFIELD);
                                }
                                return NO_PADDING;
                            default:
                                throw new IllegalArgumentException("Did not expect opcode: " + opcode);
                        }
                    }

                    @Override
                    public int resolveIncrement(MethodVisitor methodVisitor, int increment) {
                        if (fieldDescription.isStatic()) {
                            accessField(methodVisitor, Opcodes.GETSTATIC);
                            methodVisitor.visitInsn(Opcodes.ICONST_1);
                            methodVisitor.visitInsn(Opcodes.IADD);
                            accessField(methodVisitor, Opcodes.PUTSTATIC);
                            return NO_PADDING;
                        } else {
                            methodVisitor.visitIntInsn(Opcodes.ALOAD, 0);
                            methodVisitor.visitInsn(Opcodes.DUP);
                            accessField(methodVisitor, Opcodes.GETFIELD);
                            methodVisitor.visitInsn(Opcodes.ICONST_1);
                            methodVisitor.visitInsn(Opcodes.IADD);
                            accessField(methodVisitor, Opcodes.PUTFIELD);
                            return 2;
                        }
                    }

                    /**
                     * Accesses a field.
                     *
                     * @param methodVisitor The method visitor for which to access the field.
                     * @param opcode        The opcode for accessing the field.
                     */
                    private void accessField(MethodVisitor methodVisitor, int opcode) {
                        methodVisitor.visitFieldInsn(opcode,
                                fieldDescription.getDeclaringType().asErasure().getInternalName(),
                                fieldDescription.getInternalName(),
                                fieldDescription.getDescriptor());
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
                        return "Advice.Dispatcher.OffsetMapping.Target.ForField{" +
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
                        return "Advice.Dispatcher.OffsetMapping.Target.ForConstantPoolValue{" +
                                "value=" + value +
                                '}';
                    }
                }

                /**
                 * A target for an offset mapping that boxes a primitive parameter value.
                 */
                class ForBoxedParameter implements Target {

                    /**
                     * The parameters offset.
                     */
                    private final int offset;

                    /**
                     * A dispatcher for boxing the primitive value.
                     */
                    private final BoxingDispatcher boxingDispatcher;

                    /**
                     * Creates a new offset mapping for boxing a primitive parameter value.
                     *
                     * @param offset           The parameters offset.
                     * @param boxingDispatcher A dispatcher for boxing the primitive value.
                     */
                    protected ForBoxedParameter(int offset, BoxingDispatcher boxingDispatcher) {
                        this.offset = offset;
                        this.boxingDispatcher = boxingDispatcher;
                    }

                    /**
                     * Resolves a target representing an assignment of a boxed, primitive parameter value.
                     *
                     * @param offset The parameter's offset.
                     * @param type   The primitive type of the parameter being boxed.
                     * @return An appropriate target.
                     */
                    protected static Target of(int offset, TypeDefinition type) {
                        return new ForBoxedParameter(offset, BoxingDispatcher.of(type));
                    }

                    @Override
                    public int resolveAccess(MethodVisitor methodVisitor, int opcode) {
                        switch (opcode) {
                            case Opcodes.ALOAD:
                                boxingDispatcher.loadBoxed(methodVisitor, offset);
                                break;
                            default:
                                throw new IllegalStateException("Unexpected opcode: " + opcode);
                        }
                        return boxingDispatcher.getStackSize().getSize() - 1;
                    }

                    @Override
                    public int resolveIncrement(MethodVisitor methodVisitor, int increment) {
                        throw new IllegalStateException("Cannot increment a boxed parameter");
                    }

                    @Override
                    public boolean equals(Object object) {
                        if (this == object) return true;
                        if (object == null || getClass() != object.getClass()) return false;
                        ForBoxedParameter that = (ForBoxedParameter) object;
                        return offset == that.offset && boxingDispatcher == that.boxingDispatcher;
                    }

                    @Override
                    public int hashCode() {
                        int result = offset;
                        result = 31 * result + boxingDispatcher.hashCode();
                        return result;
                    }

                    @Override
                    public String toString() {
                        return "Advice.Dispatcher.OffsetMapping.Target.ForBoxedParameter{" +
                                "offset=" + offset +
                                ", boxingDispatcher=" + boxingDispatcher +
                                '}';
                    }

                    /**
                     * A dispatcher for boxing a primitive value.
                     */
                    protected enum BoxingDispatcher {

                        /**
                         * A boxing dispatcher for the {@code boolean} type.
                         */
                        BOOLEAN(Opcodes.ILOAD, Boolean.class, boolean.class),

                        /**
                         * A boxing dispatcher for the {@code byte} type.
                         */
                        BYTE(Opcodes.ILOAD, Byte.class, byte.class),

                        /**
                         * A boxing dispatcher for the {@code short} type.
                         */
                        SHORT(Opcodes.ILOAD, Short.class, short.class),

                        /**
                         * A boxing dispatcher for the {@code char} type.
                         */
                        CHARACTER(Opcodes.ILOAD, Character.class, char.class),

                        /**
                         * A boxing dispatcher for the {@code int} type.
                         */
                        INTEGER(Opcodes.ILOAD, Integer.class, int.class),

                        /**
                         * A boxing dispatcher for the {@code long} type.
                         */
                        LONG(Opcodes.LLOAD, Long.class, long.class),

                        /**
                         * A boxing dispatcher for the {@code float} type.
                         */
                        FLOAT(Opcodes.FLOAD, Float.class, float.class),

                        /**
                         * A boxing dispatcher for the {@code double} type.
                         */
                        DOUBLE(Opcodes.DLOAD, Double.class, double.class);

                        /**
                         * The name of the boxing method of a wrapper type.
                         */
                        private static final String VALUE_OF = "valueOf";

                        /**
                         * The opcode to use for loading a value of this type.
                         */
                        private final int opcode;

                        /**
                         * The name of the wrapper type.
                         */
                        private final String owner;

                        /**
                         * The descriptor of the boxing method.
                         */
                        private final String descriptor;

                        /**
                         * The required stack size of the unboxed value.
                         */
                        private final StackSize stackSize;

                        /**
                         * Creates a new boxing dispatcher.
                         *
                         * @param opcode        The opcode to use for loading a value of this type.
                         * @param wrapperType   The represented wrapper type.
                         * @param primitiveType The represented primitive type.
                         */
                        BoxingDispatcher(int opcode, Class<?> wrapperType, Class<?> primitiveType) {
                            this.opcode = opcode;
                            owner = Type.getInternalName(wrapperType);
                            descriptor = Type.getMethodDescriptor(Type.getType(wrapperType), Type.getType(primitiveType));
                            stackSize = StackSize.of(primitiveType);
                        }

                        /**
                         * Resolves a boxing dispatcher for the supplied primitive type.
                         *
                         * @param typeDefinition A description of a primitive type.
                         * @return An appropriate boxing dispatcher.
                         */
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
                                throw new IllegalArgumentException("Cannot box: " + typeDefinition);
                            }
                        }

                        /**
                         * Loads the value as a boxed version onto the stack.
                         *
                         * @param methodVisitor the method visitor for which to load the value.
                         * @param offset        The offset of the primitive value.
                         */
                        protected void loadBoxed(MethodVisitor methodVisitor, int offset) {
                            methodVisitor.visitVarInsn(opcode, offset);
                            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, owner, VALUE_OF, descriptor, false);
                        }

                        /**
                         * Returns the stack size of the primitive value.
                         *
                         * @return The stack size of the primitive value.
                         */
                        protected StackSize getStackSize() {
                            return stackSize;
                        }

                        @Override
                        public String toString() {
                            return "Advice.Dispatcher.OffsetMapping.Target.ForBoxedParameter.BoxingDispatcher." + name();
                        }
                    }
                }

                /**
                 * A target for an offset mapping of an array containing all (boxed) arguments of the instrumented method.
                 */
                class ForBoxedArguments implements Target {

                    /**
                     * The parameters of the instrumented method.
                     */
                    private final List<ParameterDescription.InDefinedShape> parameters;

                    /**
                     * Creates a mapping for a boxed array containing all arguments of the instrumented method.
                     *
                     * @param parameters The parameters of the instrumented method.
                     */
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
                                    if (parameter.getType().isPrimitive()) {
                                        ForBoxedParameter.BoxingDispatcher.of(parameter.getType()).loadBoxed(methodVisitor, parameter.getOffset());
                                    } else {
                                        methodVisitor.visitIntInsn(Opcodes.ALOAD, parameter.getOffset());
                                    }
                                    methodVisitor.visitInsn(Opcodes.AASTORE);
                                    stackSize = stackSize.maximum(parameter.getType().getStackSize());
                                }
                                return stackSize.getSize() + 2;
                            default:
                                throw new IllegalStateException("Unexpected opcode: " + opcode);
                        }
                    }

                    /**
                     * Londs an integer onto the operand stack.
                     *
                     * @param methodVisitor The method visitor for which the integer is loaded.
                     * @param value         The integer value to load onto the stack.
                     */
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
                        throw new IllegalStateException("Cannot incremement a boxed argument");
                    }

                    @Override
                    public boolean equals(Object object) {
                        if (this == object) return true;
                        if (object == null || getClass() != object.getClass()) return false;
                        ForBoxedArguments that = (ForBoxedArguments) object;
                        return parameters.equals(that.parameters);
                    }

                    @Override
                    public int hashCode() {
                        return parameters.hashCode();
                    }

                    @Override
                    public String toString() {
                        return "Advice.Dispatcher.OffsetMapping.Target.ForBoxedArguments{" +
                                "parameters=" + parameters +
                                '}';
                    }
                }

                /**
                 * Binds a null constant to the target parameter.
                 */
                enum ForNullConstant implements Target {

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
                            default:
                                throw new IllegalStateException("Unexpected opcode: " + opcode);
                        }
                        return NO_PADDING;
                    }

                    @Override
                    public int resolveIncrement(MethodVisitor methodVisitor, int increment) {
                        throw new IllegalStateException("Cannot increment a null constant");
                    }

                    @Override
                    public String toString() {
                        return "Advice.Dispatcher.OffsetMapping.Target.ForNullConstant." + name();
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
                    return "Advice.Dispatcher.OffsetMapping.ForParameter{" +
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
                        return "Advice.Dispatcher.OffsetMapping.ForParameter.Factory." + name();
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
                    return "Advice.Dispatcher.OffsetMapping.ForThisReference{" +
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
                        return "Advice.Dispatcher.OffsetMapping.ForThisReference.Factory." + name();
                    }
                }
            }

            /**
             * Maps the declaring type of the instrumented method.
             */
            enum ForInstrumentedType implements OffsetMapping {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public Target resolve(MethodDescription.InDefinedShape instrumentedMethod, Context context) {
                    return new Target.ForConstantPoolValue(Type.getType(instrumentedMethod.getDeclaringType().getDescriptor()));
                }

                @Override
                public String toString() {
                    return "Advice.Dispatcher.OffsetMapping.ForInstrumentedType." + name();
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
                 * {@code true} if this mapping is read-only.
                 */
                protected final boolean readOnly;

                /**
                 * Creates an offset mapping for a field.
                 *
                 * @param name       The name of the field.
                 * @param targetType The expected type that the field can be assigned to.
                 * @param readOnly   {@code true} if this mapping is read-only.
                 */
                protected ForField(String name, TypeDescription targetType, boolean readOnly) {
                    this.name = name;
                    this.targetType = targetType;
                    this.readOnly = readOnly;
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

                @Override
                public boolean equals(Object object) {
                    if (this == object) return true;
                    if (object == null || getClass() != object.getClass()) return false;
                    ForField forField = (ForField) object;
                    return name.equals(forField.name) && targetType.equals(forField.targetType) && readOnly == forField.readOnly;
                }

                @Override
                public int hashCode() {
                    int result = name.hashCode();
                    result = 31 * result + targetType.hashCode();
                    result = 31 * result + (readOnly ? 1 : 0);
                    return result;
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
                     * @param readOnly   {@code true} if the field is read-only.
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
                        return "Advice.Dispatcher.OffsetMapping.ForField.WithImplicitType{" +
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
                     * @param readOnly    {@code true} if the field is read-only.
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
                        return "Advice.Dispatcher.OffsetMapping.ForField.WithExplicitType{" +
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

                    /**
                     * The {@link FieldValue#readOnly()}} method.
                     */
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
                        return "Advice.Dispatcher.OffsetMapping.ForField.Factory." + name();
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
                                case Renderer.ForMethodName.SYMBOL:
                                    renderers.add(Renderer.ForMethodName.INSTANCE);
                                    break;
                                case Renderer.ForTypeName.SYMBOL:
                                    renderers.add(Renderer.ForTypeName.INSTANCE);
                                    break;
                                case Renderer.ForDescriptor.SYMBOL:
                                    renderers.add(Renderer.ForDescriptor.INSTANCE);
                                    break;
                                case Renderer.ForReturnTypeName.SYMBOL:
                                    renderers.add(Renderer.ForReturnTypeName.INSTANCE);
                                    break;
                                case Renderer.ForJavaSignature.SYMBOL:
                                    renderers.add(Renderer.ForJavaSignature.INSTANCE);
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
                    return "Advice.Dispatcher.OffsetMapping.ForOrigin{" +
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

                        /**
                         * The method name symbol.
                         */
                        public static final char SYMBOL = 'm';

                        @Override
                        public String apply(MethodDescription.InDefinedShape instrumentedMethod) {
                            return instrumentedMethod.getInternalName();
                        }

                        @Override
                        public String toString() {
                            return "Advice.Dispatcher.OffsetMapping.ForOrigin.Renderer.ForMethodName." + name();
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

                        /**
                         * The type name symbol.
                         */
                        public static final char SYMBOL = 't';

                        @Override
                        public String apply(MethodDescription.InDefinedShape instrumentedMethod) {
                            return instrumentedMethod.getDeclaringType().getName();
                        }

                        @Override
                        public String toString() {
                            return "Advice.Dispatcher.OffsetMapping.ForOrigin.Renderer.ForTypeName." + name();
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

                        /**
                         * The descriptor symbol.
                         */
                        public static final char SYMBOL = 'd';

                        @Override
                        public String apply(MethodDescription.InDefinedShape instrumentedMethod) {
                            return instrumentedMethod.getDescriptor();
                        }

                        @Override
                        public String toString() {
                            return "Advice.Dispatcher.OffsetMapping.ForOrigin.Renderer.ForDescriptor." + name();
                        }
                    }

                    /**
                     * A renderer for a method's Java signature in binary form.
                     */
                    enum ForJavaSignature implements Renderer {

                        /**
                         * The singleton instance.
                         */
                        INSTANCE;

                        /**
                         * The signature symbol.
                         */
                        public static final char SYMBOL = 's';

                        @Override
                        public String apply(MethodDescription.InDefinedShape instrumentedMethod) {
                            StringBuilder stringBuilder = new StringBuilder("(");
                            boolean comma = false;
                            for (TypeDescription typeDescription : instrumentedMethod.getParameters().asTypeList().asErasures()) {
                                if (comma) {
                                    stringBuilder.append(',');
                                } else {
                                    comma = true;
                                }
                                stringBuilder.append(typeDescription.getName());
                            }
                            return stringBuilder.append(')').toString();
                        }

                        @Override
                        public String toString() {
                            return "Advice.Dispatcher.OffsetMapping.ForOrigin.Renderer.ForJavaSignature." + name();
                        }
                    }

                    /**
                     * A renderer for a method's return type in binary form.
                     */
                    enum ForReturnTypeName implements Renderer {

                        /**
                         * The singleton instance.
                         */
                        INSTANCE;

                        /**
                         * The return type symbol.
                         */
                        public static final char SYMBOL = 'r';

                        @Override
                        public String apply(MethodDescription.InDefinedShape instrumentedMethod) {
                            return instrumentedMethod.getReturnType().asErasure().getName();
                        }

                        @Override
                        public String toString() {
                            return "Advice.Dispatcher.OffsetMapping.ForOrigin.Renderer.ForReturnTypeName." + name();
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
                            return "Advice.Dispatcher.OffsetMapping.ForOrigin.Renderer.ForStringRepresentation." + name();
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
                            return "Advice.Dispatcher.OffsetMapping.ForOrigin.Renderer.ForConstantValue{" +
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
                        } else if (parameterDescription.getType().asErasure().represents(Class.class)) {
                            return OffsetMapping.ForInstrumentedType.INSTANCE;
                        } else if (parameterDescription.getType().asErasure().isAssignableFrom(String.class)) {
                            return ForOrigin.parse(origin.loadSilent().value());
                        } else {
                            throw new IllegalStateException("Non-String type " + parameterDescription + " for origin annotation");
                        }
                    }

                    @Override
                    public String toString() {
                        return "Advice.Dispatcher.OffsetMapping.ForOrigin.Factory." + name();
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
                    return "Advice.Dispatcher.OffsetMapping.ForIgnored." + name();
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
                    return "Advice.Dispatcher.OffsetMapping.ForEnterValue." + name();
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
                        return "Advice.Dispatcher.OffsetMapping.ForEnterValue.Factory{" +
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
                    return "Advice.Dispatcher.OffsetMapping.ForReturnValue{" +
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
                        return "Advice.Dispatcher.OffsetMapping.ForReturnValue.Factory." + name();
                    }
                }
            }

            /**
             * An offset mapping for the method's (boxed) return value.
             */
            enum ForBoxedReturnValue implements OffsetMapping, Factory {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public Target resolve(MethodDescription.InDefinedShape instrumentedMethod, Context context) {
                    if (instrumentedMethod.getReturnType().represents(void.class)) {
                        return Target.ForNullConstant.INSTANCE;
                    } else if (instrumentedMethod.getReturnType().isPrimitive()) {
                        return Target.ForBoxedParameter.of(instrumentedMethod.getStackSize() + context.getPadding(), instrumentedMethod.getReturnType());
                    } else {
                        return new Target.ForReadOnlyParameter(instrumentedMethod.getStackSize() + context.getPadding());
                    }
                }

                @Override
                public OffsetMapping make(ParameterDescription.InDefinedShape parameterDescription) {
                    if (!parameterDescription.getDeclaredAnnotations().isAnnotationPresent(BoxedReturn.class)) {
                        return UNDEFINED;
                    } else if (parameterDescription.getType().represents(Object.class)) {
                        return this;
                    } else {
                        throw new IllegalStateException("Can only assign a boxed return value to an Object type");
                    }
                }

                @Override
                public String toString() {
                    return "Advice.Dispatcher.OffsetMapping.ForBoxedReturnValue." + name();
                }
            }

            /**
             * An offset mapping for an array containing the (boxed) method arguments.
             */
            enum ForBoxedArguments implements OffsetMapping, Factory {

                /**
                 * The singleton instance.
                 */
                INSTANCE;

                @Override
                public Target resolve(MethodDescription.InDefinedShape instrumentedMethod, Context context) {
                    return new Target.ForBoxedArguments(instrumentedMethod.getParameters());
                }

                @Override
                public OffsetMapping make(ParameterDescription.InDefinedShape parameterDescription) {
                    if (!parameterDescription.getDeclaredAnnotations().isAnnotationPresent(BoxedArguments.class)) {
                        return UNDEFINED;
                    } else if (parameterDescription.getType().represents(Object[].class)) {
                        return this;
                    } else {
                        throw new IllegalStateException("Can only assign an array of boxed arguments to an Object[] array");
                    }
                }

                @Override
                public String toString() {
                    return "Advice.Dispatcher.OffsetMapping.ForBoxedArguments." + name();
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
                    return "Advice.Dispatcher.OffsetMapping.ForThrowable." + name();
                }
            }

            /**
             * Represents an offset mapping for a user-defined value.
             *
             * @param <T> The mapped annotation type.
             */
            class ForUserValue<T extends Annotation> implements OffsetMapping {

                /**
                 * The target parameter that is bound.
                 */
                private final ParameterDescription.InDefinedShape target;

                /**
                 * The annotation value that triggered the binding.
                 */
                private final AnnotationDescription.Loadable<T> annotation;

                /**
                 * The dynamic value that is bound.
                 */
                private final DynamicValue<T> dynamicValue;

                /**
                 * Creates a new offset mapping for a user-defined value.
                 *
                 * @param target       The target parameter that is bound.
                 * @param annotation   The annotation value that triggered the binding.
                 * @param dynamicValue The dynamic value that is bound.
                 */
                protected ForUserValue(ParameterDescription.InDefinedShape target,
                                       AnnotationDescription.Loadable<T> annotation,
                                       DynamicValue<T> dynamicValue) {
                    this.target = target;
                    this.annotation = annotation;
                    this.dynamicValue = dynamicValue;
                }

                @Override
                public Target resolve(MethodDescription.InDefinedShape instrumentedMethod, Context context) {
                    Object value = dynamicValue.resolve(instrumentedMethod, target, annotation, context.isInitialized());
                    if (value == null) {
                        if (target.getType().isPrimitive()) {
                            throw new IllegalStateException("Cannot map null to primitive type of " + target);
                        }
                        return Target.ForNullConstant.INSTANCE;
                    } else if ((target.getType().asErasure().isAssignableFrom(String.class) && value instanceof String)
                            || (target.getType().isPrimitive() && target.getType().asErasure().isInstanceOrWrapper(value))) {
                        return new Target.ForConstantPoolValue(value);
                    } else if (target.getType().asErasure().isAssignableFrom(Class.class) && value instanceof Class) {
                        return new Target.ForConstantPoolValue(Type.getType((Class<?>) value));
                    } else if (target.getType().asErasure().isAssignableFrom(Class.class) && value instanceof TypeDescription) {
                        return new Target.ForConstantPoolValue(Type.getType(((TypeDescription) value).getDescriptor()));
                    } else {
                        throw new IllegalStateException("Cannot map " + value + " as constant value of " + target.getType());
                    }
                }

                @Override
                public boolean equals(Object object) {
                    if (this == object) return true;
                    if (object == null || getClass() != object.getClass()) return false;
                    ForUserValue that = (ForUserValue) object;
                    return target.equals(that.target)
                            && annotation.equals(that.annotation)
                            && dynamicValue.equals(that.dynamicValue);
                }

                @Override
                public int hashCode() {
                    int result = target.hashCode();
                    result = 31 * result + annotation.hashCode();
                    result = 31 * result + dynamicValue.hashCode();
                    return result;
                }

                @Override
                public String toString() {
                    return "Advice.Dispatcher.OffsetMapping.ForUserValue{" +
                            "target=" + target +
                            ", annotation=" + annotation +
                            ", dynamicValue=" + dynamicValue +
                            '}';
                }

                /**
                 * A factory for mapping a user-defined dynamic value.
                 *
                 * @param <S> The mapped annotation type.
                 */
                protected static class Factory<S extends Annotation> implements OffsetMapping.Factory {

                    /**
                     * The mapped annotation type.
                     */
                    private final Class<S> type;

                    /**
                     * The dynamic value instance used for resolving a binding.
                     */
                    private final DynamicValue<S> dynamicValue;

                    /**
                     * Creates a new factory for a user-defined dynamic value.
                     *
                     * @param type         The mapped annotation type.
                     * @param dynamicValue The dynamic value instance used for resolving a binding.
                     */
                    protected Factory(Class<S> type, DynamicValue<S> dynamicValue) {
                        this.type = type;
                        this.dynamicValue = dynamicValue;
                    }

                    /**
                     * Creates a new factory for mapping a user value.
                     *
                     * @param type         The mapped annotation type.
                     * @param dynamicValue The dynamic value instance used for resolving a binding.
                     * @return An appropriate factory for such a offset mapping.
                     */
                    @SuppressWarnings("unchecked")
                    protected static OffsetMapping.Factory of(Class<? extends Annotation> type, DynamicValue<?> dynamicValue) {
                        return new Factory(type, dynamicValue);
                    }

                    @Override
                    public OffsetMapping make(ParameterDescription.InDefinedShape parameterDescription) {
                        AnnotationDescription.Loadable<S> annotation = parameterDescription.getDeclaredAnnotations().ofType(type);
                        return annotation == null
                                ? UNDEFINED
                                : new ForUserValue<S>(parameterDescription, annotation, dynamicValue);
                    }

                    @Override
                    public boolean equals(Object object) {
                        if (this == object) return true;
                        if (object == null || getClass() != object.getClass()) return false;
                        Factory factory = (Factory) object;
                        return type.equals(factory.type) && dynamicValue.equals(factory.dynamicValue);
                    }

                    @Override
                    public int hashCode() {
                        int result = type.hashCode();
                        result = 31 * result + dynamicValue.hashCode();
                        return result;
                    }

                    @Override
                    public String toString() {
                        return "Advice.Dispatcher.OffsetMapping.ForUserValue.Factory{" +
                                "type=" + type +
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
                    return "Advice.Dispatcher.OffsetMapping.Illegal{" +
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
             * Binds this dispatcher for resolution to a specific method.
             *
             * @param instrumentedMethod The instrumented method.
             * @param methodVisitor      The method visitor for writing the instrumented method.
             * @param metaDataHandler    A meta data handler for writing to the instrumented method.
             * @param classReader        A class reader for parsing the class file containing the represented advice method.
             * @return A dispatcher that is bound to the instrumented method.
             */
            Bound bind(MethodDescription.InDefinedShape instrumentedMethod,
                       MethodVisitor methodVisitor,
                       MetaDataHandler.ForInstrumentedMethod metaDataHandler,
                       ClassReader classReader);

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
         * A bound resolution of an advice method.
         */
        interface Bound {

            /**
             * Prepares the advice method's exception handlers.
             */
            void prepare();

            /**
             * Writes the advice method's code.
             */
            void apply();
        }

        /**
         * An implementation for inactive devise that does not write any byte code.
         */
        enum Inactive implements Dispatcher.Unresolved, Resolved.ForMethodEnter, Resolved.ForMethodExit, Bound {

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
            public void prepare() {
                /* do nothing */
            }

            @Override
            public void apply() {
                /* do nothing */
            }

            @Override
            public Bound bind(MethodDescription.InDefinedShape instrumentedMethod,
                              MethodVisitor methodVisitor,
                              MetaDataHandler.ForInstrumentedMethod metaDataHandler,
                              ClassReader classReader) {
                return this;
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
                 * The suppression handler to use.
                 */
                protected final SuppressionHandler suppressionHandler;

                /**
                 * Creates a new resolved version of a dispatcher.
                 *
                 * @param adviceMethod  The represented advice method.
                 * @param factories     A list of factories to resolve for the parameters of the advice method.
                 * @param throwableType The type to handle by a suppession handler or {@link NoSuppression} to not handle any exceptions.
                 */
                protected Resolved(MethodDescription.InDefinedShape adviceMethod, List<OffsetMapping.Factory> factories, TypeDescription throwableType) {
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
                    suppressionHandler = throwableType.represents(NoSuppression.class)
                            ? SuppressionHandler.NoOp.INSTANCE
                            : new SuppressionHandler.Suppressing(throwableType);
                }

                @Override
                public boolean isAlive() {
                    return true;
                }

                @Override
                public Bound bind(MethodDescription.InDefinedShape instrumentedMethod,
                                  MethodVisitor methodVisitor,
                                  MetaDataHandler.ForInstrumentedMethod metaDataHandler,
                                  ClassReader classReader) {
                    return new CodeCopier(instrumentedMethod, methodVisitor, metaDataHandler, suppressionHandler.bind(), classReader);
                }

                /**
                 * Applies a resolution for a given instrumented method.
                 *
                 * @param methodVisitor      A method visitor for writing byte code to the instrumented method.
                 * @param metaDataHandler    A handler for translating meta data that is embedded into the instrumented method's byte code.
                 * @param instrumentedMethod A description of the instrumented method.
                 * @param suppressionHandler The bound suppression handler that is used for suppressing exceptions of this advice method.
                 * @return A method visitor for visiting the advice method's byte code.
                 */
                protected abstract MethodVisitor apply(MethodVisitor methodVisitor,
                                                       MetaDataHandler.ForInstrumentedMethod metaDataHandler,
                                                       MethodDescription.InDefinedShape instrumentedMethod,
                                                       SuppressionHandler.Bound suppressionHandler);

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
                 * A bound advice method that copies the code by first extracting the exception table and later appending the
                 * code of the method without copying any meta data.
                 */
                protected class CodeCopier extends ClassVisitor implements Bound {

                    /**
                     * The instrumented method.
                     */
                    private final MethodDescription.InDefinedShape instrumentedMethod;

                    /**
                     * The method visitor for writing the instrumented method.
                     */
                    private final MethodVisitor methodVisitor;

                    /**
                     * A meta data handler for writing to the instrumented method.
                     */
                    private final MetaDataHandler.ForInstrumentedMethod metaDataHandler;

                    /**
                     * A bound suppression handler that is used for suppressing exceptions of this advice method.
                     */
                    private final SuppressionHandler.Bound suppressionHandler;

                    /**
                     * A class reader for parsing the class file containing the represented advice method.
                     */
                    private final ClassReader classReader;

                    /**
                     * The labels that were found during parsing the method's exception handler in the order of their discovery.
                     */
                    private List<Label> labels;

                    /**
                     * Creates a new code copier.
                     *
                     * @param instrumentedMethod The instrumented method.
                     * @param methodVisitor      The method visitor for writing the instrumented method.
                     * @param metaDataHandler    A meta data handler for writing to the instrumented method.
                     * @param suppressionHandler A bound suppression handler that is used for suppressing exceptions of this advice method.
                     * @param classReader        A class reader for parsing the class file containing the represented advice method.
                     */
                    protected CodeCopier(MethodDescription.InDefinedShape instrumentedMethod,
                                         MethodVisitor methodVisitor,
                                         MetaDataHandler.ForInstrumentedMethod metaDataHandler,
                                         SuppressionHandler.Bound suppressionHandler,
                                         ClassReader classReader) {
                        super(Opcodes.ASM5);
                        this.instrumentedMethod = instrumentedMethod;
                        this.methodVisitor = methodVisitor;
                        this.metaDataHandler = metaDataHandler;
                        this.suppressionHandler = suppressionHandler;
                        this.classReader = classReader;
                        labels = new ArrayList<Label>();
                    }

                    @Override
                    public void prepare() {
                        classReader.accept(new ExceptionTableExtractor(), ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
                        suppressionHandler.onPrepare(methodVisitor);
                    }

                    @Override
                    public void apply() {
                        classReader.accept(this, ClassReader.SKIP_DEBUG | metaDataHandler.getReaderHint());
                    }

                    @Override
                    public MethodVisitor visitMethod(int modifiers, String internalName, String descriptor, String signature, String[] exception) {
                        return adviceMethod.getInternalName().equals(internalName) && adviceMethod.getDescriptor().equals(descriptor)
                                ? new ExceptionTabelSubstitutor(Active.Resolved.this.apply(methodVisitor, metaDataHandler, instrumentedMethod, suppressionHandler))
                                : IGNORE_METHOD;
                    }

                    @Override
                    public String toString() {
                        return "Advice.Dispatcher.Active.Resolved.CodeCopier{" +
                                "instrumentedMethod=" + instrumentedMethod +
                                ", methodVisitor=" + methodVisitor +
                                ", metaDataHandler=" + metaDataHandler +
                                ", suppressionHandler=" + suppressionHandler +
                                ", classReader=" + classReader +
                                ", labels=" + labels +
                                '}';
                    }

                    /**
                     * A class visitor that extracts the exception tables of the advice method.
                     */
                    protected class ExceptionTableExtractor extends ClassVisitor {

                        /**
                         * Creates a new exception table extractor.
                         */
                        protected ExceptionTableExtractor() {
                            super(Opcodes.ASM5);
                        }

                        @Override
                        public MethodVisitor visitMethod(int modifiers, String internalName, String descriptor, String signature, String[] exception) {
                            return adviceMethod.getInternalName().equals(internalName) && adviceMethod.getDescriptor().equals(descriptor)
                                    ? new ExceptionTableCollector(methodVisitor)
                                    : IGNORE_METHOD;
                        }

                        @Override
                        public String toString() {
                            return "Advice.Dispatcher.Active.Resolved.CodeCopier.ExceptionTableExtractor{" +
                                    "methodVisitor=" + methodVisitor +
                                    '}';
                        }
                    }

                    /**
                     * A visitor that only writes try-catch-finally blocks to the supplied method visitor. All labels of these tables are collected
                     * for substitution when revisiting the reminder of the method.
                     */
                    protected class ExceptionTableCollector extends MethodVisitor {

                        /**
                         * The method visitor for which the try-catch-finally blocks should be written.
                         */
                        private final MethodVisitor methodVisitor;

                        /**
                         * Creates a new exception table collector.
                         *
                         * @param methodVisitor The method visitor for which the try-catch-finally blocks should be written.
                         */
                        protected ExceptionTableCollector(MethodVisitor methodVisitor) {
                            super(Opcodes.ASM5);
                            this.methodVisitor = methodVisitor;
                        }

                        @Override
                        public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
                            methodVisitor.visitTryCatchBlock(start, end, handler, type);
                            labels.addAll(Arrays.asList(start, end, handler));
                        }

                        @Override
                        public AnnotationVisitor visitTryCatchAnnotation(int typeReference, TypePath typePath, String descriptor, boolean visible) {
                            return methodVisitor.visitTryCatchAnnotation(typeReference, typePath, descriptor, visible);
                        }

                        @Override
                        public String toString() {
                            return "Advice.Dispatcher.Active.Resolved.CodeCopier.ExceptionTableCollector{" +
                                    "methodVisitor=" + methodVisitor +
                                    '}';
                        }
                    }

                    /**
                     * A label substitutor allows to visit an advice method a second time after the exception handlers were already written.
                     * Doing so, this visitor substitutes all labels that were already created during the first visit to keep the mapping
                     * consistent.
                     */
                    protected class ExceptionTabelSubstitutor extends MethodVisitor {

                        /**
                         * A map containing resolved substitutions.
                         */
                        private final Map<Label, Label> substitutions;

                        /**
                         * The current index of the visited labels that are used for try-catch-finally blocks.
                         */
                        private int index;

                        /**
                         * Creates a label substitor.
                         *
                         * @param methodVisitor The method visitor for which to substitute labels.
                         */
                        protected ExceptionTabelSubstitutor(MethodVisitor methodVisitor) {
                            super(Opcodes.ASM5, methodVisitor);
                            substitutions = new IdentityHashMap<Label, Label>();
                        }

                        @Override
                        public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
                            substitutions.put(start, labels.get(index++));
                            substitutions.put(end, labels.get(index++));
                            substitutions.put(handler, labels.get(index++));
                        }

                        @Override
                        public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
                            return IGNORE_ANNOTATION;
                        }

                        @Override
                        public void visitLabel(Label label) {
                            super.visitLabel(resolve(label));
                        }

                        @Override
                        public void visitJumpInsn(int opcode, Label label) {
                            super.visitJumpInsn(opcode, resolve(label));
                        }

                        @Override
                        public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
                            super.visitTableSwitchInsn(min, max, dflt, resolve(labels));
                        }

                        @Override
                        public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
                            super.visitLookupSwitchInsn(resolve(dflt), keys, resolve(labels));
                        }

                        /**
                         * Resolves an array of labels.
                         *
                         * @param label The labels to resolved.
                         * @return An array containing the resolved arrays.
                         */
                        private Label[] resolve(Label[] label) {
                            Label[] resolved = new Label[label.length];
                            int index = 0;
                            for (Label aLabel : label) {
                                resolved[index++] = resolve(aLabel);
                            }
                            return resolved;
                        }

                        /**
                         * Resolves a single label if mapped or returns the original label.
                         *
                         * @param label The label to resolve.
                         * @return The resolved label.
                         */
                        private Label resolve(Label label) {
                            Label substitution = substitutions.get(label);
                            return substitution == null
                                    ? label
                                    : substitution;
                        }

                        @Override
                        public String toString() {
                            return "Advice.Dispatcher.Active.Resolved.CodeCopier.ExceptionTabelSubstitutor{" +
                                    "methodVisitor=" + methodVisitor +
                                    ", substitutions=" + substitutions +
                                    ", index=" + index +
                                    '}';
                        }
                    }
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
                     * @param adviceMethod  The represented advice method.
                     * @param userFactories A list of user-defined factories for offset mappings.
                     */
                    @SuppressWarnings("all") // In absence of @SafeVarargs for Java 6
                    protected ForMethodEnter(MethodDescription.InDefinedShape adviceMethod, List<? extends OffsetMapping.Factory> userFactories) {
                        super(adviceMethod,
                                CompoundList.of(Arrays.asList(OffsetMapping.ForParameter.Factory.INSTANCE,
                                        OffsetMapping.ForBoxedArguments.INSTANCE,
                                        OffsetMapping.ForThisReference.Factory.INSTANCE,
                                        OffsetMapping.ForField.Factory.INSTANCE,
                                        OffsetMapping.ForOrigin.Factory.INSTANCE,
                                        OffsetMapping.ForIgnored.INSTANCE,
                                        new OffsetMapping.Illegal(Thrown.class, Enter.class, Return.class, BoxedReturn.class)), userFactories),
                                adviceMethod.getDeclaredAnnotations().ofType(OnMethodEnter.class).getValue(SUPPRESS, TypeDescription.class));
                    }

                    @Override
                    public TypeDescription getEnterType() {
                        return adviceMethod.getReturnType().asErasure();
                    }

                    @Override
                    protected MethodVisitor apply(MethodVisitor methodVisitor,
                                                  MetaDataHandler.ForInstrumentedMethod metaDataHandler,
                                                  MethodDescription.InDefinedShape instrumentedMethod,
                                                  SuppressionHandler.Bound suppressionHandler) {
                        Map<Integer, OffsetMapping.Target> offsetMappings = new HashMap<Integer, OffsetMapping.Target>();
                        for (Map.Entry<Integer, OffsetMapping> entry : this.offsetMappings.entrySet()) {
                            offsetMappings.put(entry.getKey(), entry.getValue().resolve(instrumentedMethod, OffsetMapping.Context.ForMethodEntry.of(instrumentedMethod)));
                        }
                        return new CodeTranslationVisitor.ForMethodEnter(methodVisitor,
                                metaDataHandler.bindEntry(adviceMethod),
                                instrumentedMethod,
                                adviceMethod,
                                offsetMappings,
                                suppressionHandler);
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
                     * @param adviceMethod  The represented advice method.
                     * @param userFactories A list of user-defined factories for offset mappings.
                     * @param enterType     The type of the value supplied by the enter advice method or
                     *                      a description of {@code void} if no such value exists.
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
                                        OffsetMapping.ForIgnored.INSTANCE,
                                        new OffsetMapping.ForEnterValue.Factory(enterType),
                                        OffsetMapping.ForReturnValue.Factory.INSTANCE,
                                        OffsetMapping.ForBoxedReturnValue.INSTANCE,
                                        adviceMethod.getDeclaredAnnotations().ofType(OnMethodExit.class).loadSilent().onThrowable()
                                                ? OffsetMapping.ForThrowable.INSTANCE
                                                : new OffsetMapping.Illegal(Thrown.class)), userFactories),
                                adviceMethod.getDeclaredAnnotations().ofType(OnMethodExit.class).getValue(SUPPRESS, TypeDescription.class));
                        this.enterType = enterType;
                    }

                    /**
                     * Resolves exit advice that handles exceptions depending on the specification of the exit advice.
                     *
                     * @param adviceMethod  The advice method.
                     * @param userFactories A list of user-defined factories for offset mappings.
                     * @param enterType     The type of the value supplied by the enter advice method or
                     *                      a description of {@code void} if no such value exists.
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
                                                  MethodDescription.InDefinedShape instrumentedMethod,
                                                  SuppressionHandler.Bound suppressionHandler) {
                        Map<Integer, OffsetMapping.Target> offsetMappings = new HashMap<Integer, OffsetMapping.Target>();
                        for (Map.Entry<Integer, OffsetMapping> entry : this.offsetMappings.entrySet()) {
                            offsetMappings.put(entry.getKey(), entry.getValue().resolve(instrumentedMethod, OffsetMapping.Context.ForMethodExit.of(enterType)));
                        }
                        return new CodeTranslationVisitor.ForMethodExit(methodVisitor,
                                metaDataHandler.bindExit(adviceMethod),
                                instrumentedMethod,
                                adviceMethod,
                                offsetMappings,
                                suppressionHandler,
                                enterType.getStackSize().getSize() + getPadding().getSize());
                    }

                    /**
                     * Returns the additional padding this exit advice implies.
                     *
                     * @return The additional padding this exit advice implies.
                     */
                    protected abstract StackSize getPadding();

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
                         * @param adviceMethod  The represented advice method.
                         * @param userFactories A list of user-defined factories for offset mappings.
                         * @param enterType     The type of the value supplied by the enter advice method or
                         *                      a description of {@code void} if no such value exists.
                         */
                        protected WithExceptionHandler(MethodDescription.InDefinedShape adviceMethod,
                                                       List<? extends OffsetMapping.Factory> userFactories,
                                                       TypeDescription enterType) {
                            super(adviceMethod, userFactories, enterType);
                        }

                        @Override
                        protected StackSize getPadding() {
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
                         * @param adviceMethod  The represented advice method.
                         * @param userFactories A list of user-defined factories for offset mappings.
                         * @param enterType     The type of the value supplied by the enter advice method or
                         *                      a description of {@code void} if no such value exists.
                         */
                        protected WithoutExceptionHandler(MethodDescription.InDefinedShape adviceMethod,
                                                          List<? extends OffsetMapping.Factory> userFactories,
                                                          TypeDescription enterType) {
                            super(adviceMethod, userFactories, enterType);
                        }

                        @Override
                        protected StackSize getPadding() {
                            return StackSize.ZERO;
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
             * A suppression handler for optionally suppressing exceptions.
             */
            protected interface SuppressionHandler {

                /**
                 * Binds the supression handler for instrumenting a specific method.
                 *
                 * @return A bound version of the suppression handler.
                 */
                Bound bind();

                /**
                 * A bound version of a suppression handler that must not be reused.
                 */
                interface Bound {

                    /**
                     * Invoked to prepare the suppression handler, i.e. to write an exception handler entry if appropriate.
                     *
                     * @param methodVisitor The method visitor to apply the preparation to.
                     */
                    void onPrepare(MethodVisitor methodVisitor);

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
                }

                /**
                 * A non-operational suppression handler that does not suppress any method.
                 */
                enum NoOp implements SuppressionHandler, Bound {

                    /**
                     * The singleton instance.
                     */
                    INSTANCE;

                    @Override
                    public Bound bind() {
                        return this;
                    }

                    @Override
                    public void onPrepare(MethodVisitor methodVisitor) {
                            /* do nothing */
                    }

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
                        return "Advice.Dispatcher.Active.SuppressionHandler.NoOp." + name();
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
                     * Creates a new suppressing suppression handler.
                     *
                     * @param throwableType The suppressed throwable type.
                     */
                    protected Suppressing(TypeDescription throwableType) {
                        this.throwableType = throwableType;
                    }

                    @Override
                    public SuppressionHandler.Bound bind() {
                        return new Bound(throwableType);
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
                        return "Advice.Dispatcher.Active.SuppressionHandler.Suppressing{" +
                                "throwableType=" + throwableType +
                                '}';
                    }

                    /**
                     * An active, bound suppression handler.
                     */
                    protected static class Bound implements SuppressionHandler.Bound {

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
                         * Creates a new active, bound suppression handler.
                         *
                         * @param throwableType The suppressed throwable type.
                         */
                        protected Bound(TypeDescription throwableType) {
                            this.throwableType = throwableType;
                            startOfMethod = new Label();
                            endOfMethod = new Label();
                        }

                        @Override
                        public void onPrepare(MethodVisitor methodVisitor) {
                            methodVisitor.visitTryCatchBlock(startOfMethod, endOfMethod, endOfMethod, throwableType.getInternalName());
                        }

                        @Override
                        public void onStart(MethodVisitor methodVisitor, MetaDataHandler.ForAdvice metaDataHandler) {
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
                        public String toString() {
                            return "Advice.Dispatcher.Active.SuppressionHandler.Suppressing.Bound{" +
                                    "throwableType=" + throwableType +
                                    ", startOfMethod=" + startOfMethod +
                                    ", endOfMethod=" + endOfMethod +
                                    '}';
                        }

                    }
                }
            }

            /**
             * A visitor for translating an advice method's byte code for inlining into the instrumented method.
             */
            protected abstract static class CodeTranslationVisitor extends MethodVisitor implements ReturnValueProducer {

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
                private final SuppressionHandler.Bound suppressionHandler;

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
                 * @param suppressionHandler The suppression handler to use.
                 */
                protected CodeTranslationVisitor(MethodVisitor methodVisitor,
                                                 MetaDataHandler.ForAdvice metaDataHandler,
                                                 MethodDescription.InDefinedShape instrumentedMethod,
                                                 MethodDescription.InDefinedShape adviceMethod,
                                                 Map<Integer, Resolved.OffsetMapping.Target> offsetMappings,
                                                 SuppressionHandler.Bound suppressionHandler) {
                    super(Opcodes.ASM5, methodVisitor);
                    this.metaDataHandler = metaDataHandler;
                    this.instrumentedMethod = instrumentedMethod;
                    this.adviceMethod = adviceMethod;
                    this.offsetMappings = offsetMappings;
                    this.suppressionHandler = suppressionHandler;
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
                        metaDataHandler.recordPadding(target.resolveAccess(mv, opcode));
                    } else {
                        mv.visitVarInsn(opcode, adjust(offset + instrumentedMethod.getStackSize() - adviceMethod.getStackSize()));
                    }
                }

                @Override
                public void visitIincInsn(int offset, int increment) {
                    Resolved.OffsetMapping.Target target = offsetMappings.get(offset);
                    if (target != null) {
                        metaDataHandler.recordPadding(target.resolveIncrement(mv, increment));
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
                     * @param suppressionHandler The suppression handler to use.
                     */
                    protected ForMethodEnter(MethodVisitor methodVisitor,
                                             MetaDataHandler.ForAdvice metaDataHandler,
                                             MethodDescription.InDefinedShape instrumentedMethod,
                                             MethodDescription.InDefinedShape adviceMethod,
                                             Map<Integer, Resolved.OffsetMapping.Target> offsetMappings,
                                             SuppressionHandler.Bound suppressionHandler) {
                        super(methodVisitor, metaDataHandler, instrumentedMethod, adviceMethod, offsetMappings, suppressionHandler);
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
                     * @param suppressionHandler The suppression handler to use.
                     * @param padding            The padding after the instrumented method's arguments in the local variable array.
                     */
                    protected ForMethodExit(MethodVisitor methodVisitor,
                                            MetaDataHandler.ForAdvice metaDataHandler,
                                            MethodDescription.InDefinedShape instrumentedMethod,
                                            MethodDescription.InDefinedShape adviceMethod,
                                            Map<Integer, Resolved.OffsetMapping.Target> offsetMappings,
                                            SuppressionHandler.Bound suppressionHandler,
                                            int padding) {
                        super(methodVisitor,
                                metaDataHandler,
                                instrumentedMethod,
                                adviceMethod,
                                offsetMappings,
                                suppressionHandler);
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
         * type must be equal to the type declaring the instrumented method. If this property is set to {@code true}, the
         * annotated parameter can be any super type of the instrumented method's declaring type.
         *
         * @return {@code true} if this parameter is read-only.
         */
        boolean readOnly() default true;
    }

    /**
     * <p>
     * Indicates that the annotated parameter should be mapped to a field in the scope of the instrumented method.
     * </p>
     * <p>
     * <b>Important</b>: Parameters with this option must not be used when from a constructor in combination with
     * {@link OnMethodEnter} and a non-static field where the {@code this} reference is not available.
     * </p>
     * <p>
     * <b>Note</b>: As the mapping is virtual, Byte Buddy might be required to reserve more space on the operand stack than the
     * optimal value when accessing this parameter. This does not normally matter as the additional space requirement is minimal.
     * However, if the runtime performance of class creation is secondary, one can require ASM to recompute the optimal frames by
     * setting {@link ClassWriter#COMPUTE_MAXS}. This is however only relevant when writing to a non-static field.
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

        /**
         * Indicates if it is possible to write to this parameter. If this property is set to {@code false}, the annotated
         * type must be equal to the mapped field type. If this property is set to {@code true}, the  annotated parameter
         * can be any super type of the field type.
         *
         * @return {@code true} if this parameter is read-only.
         */
        boolean readOnly() default true;
    }

    /**
     * Indicates that the annotated parameter should be mapped to a string representation of the instrumented method or
     * to a constant representing the {@link Class} declaring the method.
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

    /**
     * <p>
     * Indicates that the annotated parameter should be mapped to a the return value where primitive types are boxed. For this
     * to be possible, the annotated parameter must be of type {@link Object}.
     * </p>
     * <p>
     * Note that accessing this parameter is merely virtual. A new array is created on every access. As a result, changes to the
     * array have no effect other than for the local copy and when accessing the array twice, the equality relation does not hold.
     * For example, for {@code @Advice.BoxedReturn Object foo}, the relation {@code foo == foo} does not necessarily hold for primitive
     * types. For avoiding additional allocations, the array needs to be stored in a separate local variable. The variable itself is
     * always read only.
     * </p>
     * <p>
     * <b>Note</b>: As the mapping is virtual, Byte Buddy might be required to reserve more space on the operand stack than the
     * optimal value when accessing this parameter. This does not normally matter as the additional space requirement is minimal.
     * However, if the runtime performance of class creation is secondary, one can require ASM to recompute the optimal frames by
     * setting {@link ClassWriter#COMPUTE_MAXS}.
     * </p>
     *
     * @see Advice
     * @see OnMethodEnter
     * @see OnMethodExit
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public @interface BoxedReturn {
        /* boxed */
    }

    /**
     * <p>
     * Indicates that the annotated parameter should be mapped to an array containing a (boxed) version of all arguments of the
     * method being instrumented. It is required that the annotated parameter is an array of type {@link Object}.
     * </p>
     * <p>
     * Note that accessing this parameter is merely virtual. A new array is created on every access. As a result, changes to the
     * array have no effect other than for the local copy and when accessing the array twice, the equality relation does not hold.
     * For example, for {@code @Advice.BoxedArguments Object[] foo}, the relation {@code foo == foo} does not hold. For avoiding
     * new allocations, the array needs to be stored in a separate local variable. The variable itself is always read only.
     * </p>
     * <p>
     * <b>Note</b>: As the mapping is virtual, Byte Buddy might be required to reserve more space on the operand stack than the
     * optimal value when accessing this parameter. This does not normally matter as the additional space requirement is minimal.
     * However, if the runtime performance of class creation is secondary, one can require ASM to recompute the optimal frames by
     * setting {@link ClassWriter#COMPUTE_MAXS}.
     * </p>
     *
     * @see Advice
     * @see OnMethodEnter
     * @see OnMethodExit
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public @interface BoxedArguments {
        /* boxed */
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
     * <p>
     * A dynamic value allows to bind parameters of an {@link Advice} method to a custom, constant value.
     * </p>
     * <p>The mapped value must be a constant value that can be embedded into a Java class file. This holds for all primitive types,
     * instances of {@link String} and for {@link Class} instances as well as their unloaded {@link TypeDescription} representations.
     * </p>
     *
     * @param <T> The type of the annotation this dynamic value requires to provide a mapping.
     * @see WithCustomMapping
     */
    public interface DynamicValue<T extends Annotation> {

        /**
         * Resolves a constant value that is mapped to a parameter that is annotated with a custom bound annotation.
         *
         * @param instrumentedMethod The instrumented method onto which this advice is applied.
         * @param target             The target parameter that is bound.
         * @param annotation         The annotation that triggered this binding.
         * @param initialized        {@code true} if the method is initialized when the value is bound, i.e. that the value is not
         *                           supplied to a constructor before the super constructor was invoked.
         * @return The constant pool value that is bound to the supplied parameter or {@code null} to assign this value.
         */
        Object resolve(MethodDescription.InDefinedShape instrumentedMethod,
                       ParameterDescription.InDefinedShape target,
                       AnnotationDescription.Loadable<T> annotation,
                       boolean initialized);

        /**
         * <p>
         * A {@link DynamicValue} implementation that always binds a fixed value.
         * </p>
         * <p>
         * The mapped value must be a constant value that can be embedded into a Java class file. This holds for all primitive types,
         * instances of {@link String} and for {@link Class} instances as well as their unloaded {@link TypeDescription} representations.
         * </p>
         */
        class ForFixedValue implements DynamicValue<Annotation> {

            /**
             * The fixed value to bind to the corresponding annotation.
             */
            private final Object value;

            /**
             * Creates a dynamic value for a fixed value.
             *
             * @param value The fixed value to bind to the corresponding annotation.
             */
            public ForFixedValue(Object value) {
                this.value = value;
            }

            @Override
            public Object resolve(MethodDescription.InDefinedShape instrumentedMethod,
                                  ParameterDescription.InDefinedShape target,
                                  AnnotationDescription.Loadable<Annotation> annotation,
                                  boolean initialized) {
                return value;
            }

            @Override
            public boolean equals(Object object) {
                if (this == object) return true;
                if (object == null || getClass() != object.getClass()) return false;
                ForFixedValue that = (ForFixedValue) object;
                return value != null ? value.equals(that.value) : that.value == null;
            }

            @Override
            public int hashCode() {
                return value != null ? value.hashCode() : 0;
            }

            @Override
            public String toString() {
                return "Advice.DynamicValue.ForFixedValue{" +
                        "value=" + value +
                        '}';
            }
        }
    }

    /**
     * A builder step for creating an {@link Advice} that uses custom mappings of annotations to constant pool values.
     */
    public static class WithCustomMapping {

        /**
         * A map containing dynamically computed constant pool values that are mapped by their triggering annotation type.
         */
        private final Map<Class<? extends Annotation>, DynamicValue<?>> dynamicValues;

        /**
         * Creates a new custom mapping builder step without including any custom mappings.
         */
        protected WithCustomMapping() {
            this(Collections.<Class<? extends Annotation>, DynamicValue<?>>emptyMap());
        }

        /**
         * Creates a new custom mapping builder step with the given custom mappings.
         *
         * @param dynamicValues A map containing dynamically computed constant pool values that are mapped by their triggering annotation type.
         */
        protected WithCustomMapping(Map<Class<? extends Annotation>, DynamicValue<?>> dynamicValues) {
            this.dynamicValues = dynamicValues;
        }

        /**
         * Binds an annotation type to dynamically computed value. Whenever the {@link Advice} component discovers the given annotation on
         * a parameter of an advice method, the dynamic value is asked to provide a value that is then assigned to the parameter in question.
         *
         * @param type         The annotation type that triggers the mapping.
         * @param dynamicValue The dynamic value that is computed for binding the parameter to a value.
         * @param <T>          The annotation type.
         * @return A new builder for an advice that considers the supplied annotation type during binding.
         */
        public <T extends Annotation> WithCustomMapping bind(Class<? extends T> type, DynamicValue<T> dynamicValue) {
            Map<Class<? extends Annotation>, DynamicValue<?>> dynamicValues = new HashMap<Class<? extends Annotation>, Advice.DynamicValue<?>>(this.dynamicValues);
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

        /**
         * Implements advice where every matched method is advised by the given type's advisory methods.
         *
         * @param typeDescription  A description of the type declaring the advice.
         * @param classFileLocator The class file locator for locating the advisory class's class file.
         * @return A method visitor wrapper representing the supplied advice.
         */
        public Advice to(TypeDescription typeDescription, ClassFileLocator classFileLocator) {
            List<Dispatcher.OffsetMapping.Factory> userFactories = new ArrayList<Dispatcher.OffsetMapping.Factory>(dynamicValues.size());
            for (Map.Entry<Class<? extends Annotation>, DynamicValue<?>> entry : dynamicValues.entrySet()) {
                userFactories.add(Dispatcher.OffsetMapping.ForUserValue.Factory.of(entry.getKey(), entry.getValue()));
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
