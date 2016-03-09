package net.bytebuddy.asm;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
 * <b>Important</b>: Currently, it is required to make ASM recompute stack sizes and stack map frames by setting the {@link ClassWriter#COMPUTE_FRAMES}
 * flag. This constraint will be relaxed in a future version.
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
    public static AsmVisitorWrapper.ForDeclaredMethods.MethodVisitorWrapper to(Class<?> type) {
        return to(type, ClassFileLocator.ForClassLoader.of(type.getClassLoader()));
    }

    /**
     * Implements advice where every matched method is advised by the given type's advisory methods.
     *
     * @param type             The type declaring the advice.
     * @param classFileLocator The class file locator for locating the advisory class's class file.
     * @return A method visitor wrapper representing the supplied advice.
     */
    public static AsmVisitorWrapper.ForDeclaredMethods.MethodVisitorWrapper to(Class<?> type, ClassFileLocator classFileLocator) {
        return to(new TypeDescription.ForLoadedType(type), classFileLocator);
    }

    /**
     * Implements advice where every matched method is advised by the given type's advisory methods.
     *
     * @param typeDescription  A description of the type declaring the advice.
     * @param classFileLocator The class file locator for locating the advisory class's class file.
     * @return A method visitor wrapper representing the supplied advice.
     */
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
            this.classReader = new ClassReader(binaryRepresentation);
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
        public void visitMaxs(int maxStack, int maxLocals) {
            onMethodEnd();
            super.visitMaxs(maxStack, maxLocals);
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
                return dispatcher.apply(internalName, descriptor, mv, instrumentedMethod);
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
             * @param instrumentedMethod A description of the instrumented method.
             * @return A method visitor for reading the discovered method or {@code null} if the discovered method is of no interest.
             */
            MethodVisitor apply(String internalName, String descriptor, MethodVisitor methodVisitor, MethodDescription.InDefinedShape instrumentedMethod);

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
            public MethodVisitor apply(String internalName, String descriptor, MethodVisitor methodVisitor, MethodDescription.InDefinedShape instrumentedMethod) {
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
                public MethodVisitor apply(String internalName, String descriptor, MethodVisitor methodVisitor, MethodDescription.InDefinedShape instrumentedMethod) {
                    return adviseMethod.getInternalName().equals(internalName) && adviseMethod.getDescriptor().equals(descriptor)
                            ? apply(methodVisitor, instrumentedMethod)
                            : IGNORE_METHOD;
                }

                /**
                 * Applies a resolution for a given instrumented method.
                 *
                 * @param methodVisitor      A method visitor for writing byte code to the instrumented method.
                 * @param instrumentedMethod A description of the instrumented method.
                 * @return A method visitor for visiting the advise method's byte code.
                 */
                protected abstract MethodVisitor apply(MethodVisitor methodVisitor, MethodDescription.InDefinedShape instrumentedMethod);

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
                     * @param additionalSize     The additional size to consider before writing to any slot.
                     * @return A suitable target mapping.
                     */
                    Target resolve(MethodDescription.InDefinedShape instrumentedMethod, StackSize additionalSize);

                    /**
                     * A target offset of an offset mapping.
                     */
                    interface Target {

                        /**
                         * Checks if this target supports a given opcode.
                         *
                         * @param opcode The opcode to check for its legitimacy.
                         * @return {@code true} if this target supports the given opcode.
                         */
                        boolean supports(int opcode);

                        /**
                         * Returns the mapped offset.
                         *
                         * @return The mapped offset.
                         */
                        int getOffset();

                        /**
                         * A read-write target mapping.
                         */
                        class ReadWrite implements Target {

                            /**
                             * The mapped offset.
                             */
                            private final int offset;

                            /**
                             * Creates a new read-write target mapping.
                             *
                             * @param offset The mapped offset.
                             */
                            protected ReadWrite(int offset) {
                                this.offset = offset;
                            }

                            @Override
                            public boolean supports(int opcode) {
                                return true;
                            }

                            @Override
                            public int getOffset() {
                                return offset;
                            }

                            @Override
                            public boolean equals(Object object) {
                                if (this == object) return true;
                                if (object == null || getClass() != object.getClass()) return false;
                                ReadWrite readWrite = (ReadWrite) object;
                                return offset == readWrite.offset;
                            }

                            @Override
                            public int hashCode() {
                                return offset;
                            }

                            @Override
                            public String toString() {
                                return "Advice.Dispatcher.Active.Resolved.OffsetMapping.Target.ReadWrite{" +
                                        "offset=" + offset +
                                        '}';
                            }
                        }

                        /**
                         * A read-only target mapping.
                         */
                        class ReadOnly implements Target {

                            /**
                             * The mapped offset.
                             */
                            private final int offset;

                            /**
                             * Creates a new read-only target mapping.
                             * @param offset The mapped offset.
                             */
                            protected ReadOnly(int offset) {
                                this.offset = offset;
                            }

                            @Override
                            public boolean supports(int opcode) {
                                switch (opcode) {
                                    case Opcodes.ISTORE:
                                    case Opcodes.LSTORE:
                                    case Opcodes.FSTORE:
                                    case Opcodes.DSTORE:
                                    case Opcodes.ASTORE:
                                        return false;
                                    default:
                                        return true;
                                }
                            }

                            @Override
                            public int getOffset() {
                                return offset;
                            }

                            @Override
                            public boolean equals(Object object) {
                                if (this == object) return true;
                                if (object == null || getClass() != object.getClass()) return false;
                                ReadOnly readOnly = (ReadOnly) object;
                                return offset == readOnly.offset;
                            }

                            @Override
                            public int hashCode() {
                                return offset;
                            }

                            @Override
                            public String toString() {
                                return "Advice.Dispatcher.Active.Resolved.OffsetMapping.Target.ReadOnly{" +
                                        "offset=" + offset +
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
                        public Target resolve(MethodDescription.InDefinedShape instrumentedMethod, StackSize additionalSize) {
                            ParameterList<?> parameters = instrumentedMethod.getParameters();
                            if (parameters.size() <= index) {
                                throw new IllegalStateException(instrumentedMethod + " does not define an index " + index);
                            } else if (!readOnly && !parameters.get(index).getType().asErasure().equals(targetType)) {
                                throw new IllegalStateException("read-only " + targetType + " is not equal to type of " + parameters.get(index));
                            } else if (readOnly && !parameters.get(index).getType().asErasure().isAssignableTo(targetType)) {
                                throw new IllegalStateException(targetType + " is not assignable to " + parameters.get(index));
                            }
                            return readOnly
                                    ? new Target.ReadOnly(parameters.get(index).getOffset())
                                    : new Target.ReadWrite(parameters.get(index).getOffset());
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
                         * @param targetType The type that the advise method expects for the {@code this} reference.
                         */
                        protected ForThisReference(boolean readOnly, TypeDescription targetType) {
                            this.readOnly = readOnly;
                            this.targetType = targetType;
                        }

                        @Override
                        public Target resolve(MethodDescription.InDefinedShape instrumentedMethod, StackSize additionalSize) {
                            if (instrumentedMethod.isStatic()) {
                                throw new IllegalStateException("Cannot map this reference for static method " + instrumentedMethod);
                            } else if (!readOnly && !instrumentedMethod.getDeclaringType().equals(targetType)) {
                                throw new IllegalStateException("Declaring type of " + instrumentedMethod + " is not equal to read-only " + targetType);
                            } else if (readOnly && !instrumentedMethod.getDeclaringType().isAssignableTo(targetType)) {
                                throw new IllegalStateException("Declaring type of " + instrumentedMethod + " is not assignable to " + targetType);
                            }
                            return readOnly
                                    ? new Target.ReadOnly(THIS_REFERENCE)
                                    : new Target.ReadWrite(THIS_REFERENCE);
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
                        public Target resolve(MethodDescription.InDefinedShape instrumentedMethod, StackSize additionalSize) {
                            return readOnly
                                    ? new Target.ReadOnly(instrumentedMethod.getStackSize())
                                    : new Target.ReadWrite(instrumentedMethod.getStackSize());
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
                        public Target resolve(MethodDescription.InDefinedShape instrumentedMethod, StackSize additionalSize) {
                            if (!readOnly && !instrumentedMethod.getReturnType().asErasure().equals(targetType)) {
                                throw new IllegalStateException("read-only return type of " + instrumentedMethod + " is not equal to " + targetType);
                            } else if (readOnly && !instrumentedMethod.getReturnType().asErasure().isAssignableTo(targetType)) {
                                throw new IllegalStateException("Cannot assign return type of " + instrumentedMethod + " to " + targetType);
                            }
                            return readOnly
                                    ? new Target.ReadOnly(instrumentedMethod.getStackSize() + additionalSize.getSize())
                                    : new Target.ReadWrite(instrumentedMethod.getStackSize() + additionalSize.getSize());
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
                        public Target resolve(MethodDescription.InDefinedShape instrumentedMethod, StackSize additionalSize) {
                            return new Target.ReadWrite(instrumentedMethod.getStackSize() + additionalSize.getSize() + instrumentedMethod.getReturnType().getStackSize().getSize());
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
                     * Creates a new resolved dispatcher for implementing method enter advise.
                     *
                     * @param adviseMethod The represented advise method.
                     */
                    @SuppressWarnings("all") // In absence of @SafeVarargs for Java 6
                    protected ForMethodEnter(MethodDescription.InDefinedShape adviseMethod) {
                        super(adviseMethod,
                                OffsetMapping.ForParameter.Factory.INSTANCE,
                                OffsetMapping.ForThisReference.Factory.INSTANCE,
                                new OffsetMapping.Illegal(Thrown.class, Enter.class, Return.class));
                    }

                    @Override
                    public TypeDescription getEnterType() {
                        return adviseMethod.getReturnType().asErasure();
                    }

                    @Override
                    protected MethodVisitor apply(MethodVisitor methodVisitor, MethodDescription.InDefinedShape instrumentedMethod) {
                        Map<Integer, OffsetMapping.Target> offsetMappings = new HashMap<Integer, OffsetMapping.Target>();
                        for (Map.Entry<Integer, OffsetMapping> entry : this.offsetMappings.entrySet()) {
                            offsetMappings.put(entry.getKey(), entry.getValue().resolve(instrumentedMethod, StackSize.ZERO));
                        }
                        return new CodeTranslationVisitor.ReturnValueRetaining(methodVisitor, instrumentedMethod, adviseMethod, offsetMappings);
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
                     * The additional stack size to consider when accessing the local variable array.
                     */
                    private final StackSize additionalSize;

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
                                new OffsetMapping.ForEnterValue.Factory(enterType),
                                OffsetMapping.ForReturnValue.Factory.INSTANCE,
                                adviseMethod.getDeclaredAnnotations().ofType(OnMethodExit.class).loadSilent().onThrowable()
                                        ? OffsetMapping.ForThrowable.INSTANCE
                                        : new OffsetMapping.Illegal(Thrown.class));
                        additionalSize = enterType.getStackSize();
                    }

                    @Override
                    public boolean isSkipThrowable() {
                        return !adviseMethod.getDeclaredAnnotations().ofType(OnMethodExit.class).loadSilent().onThrowable();
                    }

                    @Override
                    protected MethodVisitor apply(MethodVisitor methodVisitor, MethodDescription.InDefinedShape instrumentedMethod) {
                        Map<Integer, OffsetMapping.Target> offsetMappings = new HashMap<Integer, OffsetMapping.Target>();
                        for (Map.Entry<Integer, OffsetMapping> entry : this.offsetMappings.entrySet()) {
                            offsetMappings.put(entry.getKey(), entry.getValue().resolve(instrumentedMethod, additionalSize));
                        }
                        return new CodeTranslationVisitor.ReturnValueDiscarding(methodVisitor, instrumentedMethod, adviseMethod, offsetMappings, additionalSize);
                    }

                    @Override
                    public boolean equals(Object other) {
                        return this == other || !(other == null || getClass() != other.getClass())
                                && super.equals(other)
                                && additionalSize == ((Resolved.ForMethodExit) other).additionalSize;
                    }

                    @Override
                    public int hashCode() {
                        int result = super.hashCode();
                        result = 31 * result + additionalSize.hashCode();
                        return result;
                    }

                    @Override
                    public String toString() {
                        return "Advice.Dispatcher.Active.Resolved.ForMethodExit{" +
                                "adviseMethod=" + adviseMethod +
                                ", offsetMappings=" + offsetMappings +
                                ", additionalSize=" + additionalSize +
                                '}';
                    }
                }
            }

            /**
             * A visitor for translating an advise method's byte code for inlining into the instrumented method.
             */
            protected abstract static class CodeTranslationVisitor extends MethodVisitor {

                /**
                 * Indicates that an annotation should not be read.
                 */
                private static final AnnotationVisitor IGNORE_ANNOTATION = null;

                /**
                 * The instrumented method.
                 */
                protected final MethodDescription.InDefinedShape instrumentedMethod;

                /**
                 * The advise method.
                 */
                protected final MethodDescription.InDefinedShape adviseMethod;

                private final Map<Integer, Resolved.OffsetMapping.Target> offsetMappings;

                /**
                 * A label indicating the end of the advise byte code.
                 */
                protected final Label endOfMethod;

                /**
                 * Creates a new code translation visitor.
                 *
                 * @param methodVisitor      A method visitor for writing the instrumented method's byte code.
                 * @param instrumentedMethod The instrumented method.
                 * @param adviseMethod       The advise method.
                 * @param offsetMappings     A mapping of offsets of the advise methods to their corresponding offsets in the instrumented method.
                 */
                protected CodeTranslationVisitor(MethodVisitor methodVisitor,
                                                 MethodDescription.InDefinedShape instrumentedMethod,
                                                 MethodDescription.InDefinedShape adviseMethod,
                                                 Map<Integer, Resolved.OffsetMapping.Target> offsetMappings) {
                    super(Opcodes.ASM5, methodVisitor);
                    this.instrumentedMethod = instrumentedMethod;
                    this.adviseMethod = adviseMethod;
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
                    Resolved.OffsetMapping.Target target = offsetMappings.get(offset);
                    if (target != null) {
                        if (!target.supports(opcode)) {
                            throw new IllegalStateException("Cannot write to read-only variable " + target);
                        }
                        offset = target.getOffset();
                    } else {
                        offset = adjust(offset + instrumentedMethod.getStackSize() - adviseMethod.getStackSize());
                    }
                    super.visitVarInsn(opcode, offset);
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
                 * A code translation visitor that retains the return value of the represented advise method.
                 */
                protected static class ReturnValueRetaining extends CodeTranslationVisitor {

                    /**
                     * Creates a new code translation visitor that retains the return value of the enter advise.
                     *
                     * @param methodVisitor      A method visitor for writing the instrumented method's byte code.
                     * @param instrumentedMethod The instrumented method.
                     * @param adviseMethod       The advise method.
                     * @param offsetMappings     A mapping of offsets of the advise methods to their corresponding offsets in the instrumented method.
                     */
                    protected ReturnValueRetaining(MethodVisitor methodVisitor,
                                                   MethodDescription.InDefinedShape instrumentedMethod,
                                                   MethodDescription.InDefinedShape adviseMethod,
                                                   Map<Integer, Resolved.OffsetMapping.Target> offsetMappings) {
                        super(methodVisitor, instrumentedMethod, adviseMethod, offsetMappings);
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
                    public String toString() {
                        return "Advice.Dispatcher.Active.CodeTranslationVisitor.ReturnValueRetaining{" +
                                "instrumentedMethod=" + instrumentedMethod +
                                ", adviseMethod=" + adviseMethod +
                                '}';
                    }
                }

                /**
                 * A code translation visitor that discards the return value of the represented advise method.
                 */
                protected static class ReturnValueDiscarding extends CodeTranslationVisitor {

                    /**
                     * An additional size of the local variable array to consider when writing or reading values.
                     */
                    private final StackSize additionalVariableSize;

                    /**
                     * Creates a new code translation visitor that retains the return value of the enter advise.
                     *
                     * @param methodVisitor          A method visitor for writing the instrumented method's byte code.
                     * @param instrumentedMethod     The instrumented method.
                     * @param adviseMethod           The advise method.
                     * @param offsetMappings         A mapping of offsets of the advise methods to their corresponding offsets in the instrumented method.
                     * @param additionalVariableSize An additional size of the local variable array to consider when writing or reading values.
                     */
                    protected ReturnValueDiscarding(MethodVisitor methodVisitor,
                                                    MethodDescription.InDefinedShape instrumentedMethod,
                                                    MethodDescription.InDefinedShape adviseMethod,
                                                    Map<Integer, Resolved.OffsetMapping.Target> offsetMappings,
                                                    StackSize additionalVariableSize) {
                        super(methodVisitor, instrumentedMethod, adviseMethod, offsetMappings);
                        this.additionalVariableSize = additionalVariableSize;
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
                        return offset + instrumentedMethod.getReturnType().getStackSize().getSize() + additionalVariableSize.getSize() + 1;
                    }

                    @Override
                    public String toString() {
                        return "Advice.Dispatcher.Active.CodeTranslationVisitor.ReturnValueDiscarding{" +
                                "instrumentedMethod=" + instrumentedMethod +
                                ", adviseMethod=" + adviseMethod +
                                ", additionalVariableSize=" + additionalVariableSize +
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
        /* empty */
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
     * Indicates that the annotated parameter should be mapped to the {@code this} reference of the instrumented method.
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
}
