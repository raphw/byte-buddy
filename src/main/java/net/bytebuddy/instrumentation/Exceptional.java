package net.bytebuddy.instrumentation;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import net.bytebuddy.instrumentation.method.bytecode.stack.Duplication;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.Throw;
import net.bytebuddy.instrumentation.method.bytecode.stack.TypeCreation;
import net.bytebuddy.instrumentation.method.bytecode.stack.constant.TextConstant;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodInvocation;
import net.bytebuddy.instrumentation.type.InstrumentedType;
import net.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;

import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.isConstructor;
import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.takesArguments;

/**
 * This instrumentation causes a {@link java.lang.Throwable} to be thrown when the instrumented method is invoked.
 * Be aware that the Java Virtual machine does not care about exception declarations and will throw any isThrowable
 * from any method even if the method does not declared a checked exception.
 */
public class Exceptional implements Instrumentation, ByteCodeAppender {

    private final TypeDescription throwableType;
    private final ConstructionDelegate constructionDelegate;

    /**
     * Creates a new instance of an instrumentation for throwing throwables.
     *
     * @param throwableType        The type of the exception to be thrown.
     * @param constructionDelegate A delegate that is responsible for calling the isThrowable's constructor.
     */
    public Exceptional(TypeDescription throwableType,
                       ConstructionDelegate constructionDelegate) {
        this.throwableType = throwableType;
        this.constructionDelegate = constructionDelegate;
    }

    /**
     * Creates an instrumentation that creates a new instance of the given isThrowable type on each method invocation
     * which is then thrown immediately. For this to be possible, the given type must define a default constructor
     * which is visible from the instrumented type.
     *
     * @param throwable The type of the isThrowable.
     * @return An instrumentation that will throw an instance of the isThrowable on each method invocation of the
     * instrumented methods.
     */
    public static Instrumentation throwing(Class<? extends Throwable> throwable) {
        TypeDescription exceptionType = new TypeDescription.ForLoadedType(throwable);
        return new Exceptional(exceptionType, new ConstructionDelegate.ForDefaultConstructor(exceptionType));
    }

    /**
     * Creates an instrumentation that creates a new instance of the given isThrowable type on each method invocation
     * which is then thrown immediately. For this to be possible, the given type must define a constructor that
     * takes a single {@link java.lang.String} as its argument.
     *
     * @param throwable The type of the isThrowable.
     * @param message   The string that is handed to the constructor. Usually an exception message.
     * @return An instrumentation that will throw an instance of the isThrowable on each method invocation of the
     * instrumented methods.
     */
    public static Instrumentation throwing(Class<? extends Throwable> throwable, String message) {
        TypeDescription exceptionType = new TypeDescription.ForLoadedType(throwable);
        return new Exceptional(exceptionType, new ConstructionDelegate.ForStringConstructor(exceptionType, message));
    }

    @Override
    public InstrumentedType prepare(InstrumentedType instrumentedType) {
        return instrumentedType;
    }

    @Override
    public ByteCodeAppender appender(TypeDescription instrumentedType) {
        return this;
    }

    @Override
    public boolean appendsCode() {
        return true;
    }

    @Override
    public Size apply(MethodVisitor methodVisitor,
                      Context instrumentationContext,
                      MethodDescription instrumentedMethod) {
        StackManipulation.Size stackSize = new StackManipulation.Compound(
                constructionDelegate.make(),
                Throw.INSTANCE
        ).apply(methodVisitor, instrumentationContext);
        return new Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && constructionDelegate.equals(((Exceptional) other).constructionDelegate)
                && throwableType.equals(((Exceptional) other).throwableType);
    }

    @Override
    public int hashCode() {
        return 31 * throwableType.hashCode() + constructionDelegate.hashCode();
    }

    @Override
    public String toString() {
        return "Exceptional{" +
                "throwableType=" + throwableType +
                ", constructionDelegate=" + constructionDelegate +
                '}';
    }

    /**
     * A construction delegate is responsible for calling a isThrowable's constructor.
     */
    public static interface ConstructionDelegate {

        /**
         * Creates a stack manipulation that creates pushes all constructor arguments onto the operand stack
         * and subsequently calls the constructor.
         *
         * @return A stack manipulation for constructing a isThrowable.
         */
        StackManipulation make();

        /**
         * A construction delegate that calls the default constructor.
         */
        static class ForDefaultConstructor implements ConstructionDelegate {

            private final TypeDescription targetType;
            private final MethodDescription targetConstructor;

            /**
             * Creates a new construction delegate that calls a default constructor.
             *
             * @param targetType The type of the isThrowable.
             */
            public ForDefaultConstructor(TypeDescription targetType) {
                this.targetType = targetType;
                this.targetConstructor = targetType.getDeclaredMethods()
                        .filter(isConstructor().and(takesArguments(0))).getOnly();
            }

            @Override
            public StackManipulation make() {
                return new StackManipulation.Compound(
                        TypeCreation.forType(targetType),
                        Duplication.SINGLE,
                        MethodInvocation.invoke(targetConstructor));
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && targetType.equals(((ForDefaultConstructor) other).targetType);
            }

            @Override
            public int hashCode() {
                return targetType.hashCode();
            }

            @Override
            public String toString() {
                return "Exceptional.ConstructionDelegate.ForDefaultConstructor{" +
                        "targetType=" + targetType +
                        ", targetConstructor=" + targetConstructor +
                        '}';
            }
        }

        /**
         * A construction delegate that calls a constructor that takes a single string as its argument.
         */
        static class ForStringConstructor implements ConstructionDelegate {

            private final TypeDescription targetType;
            private final MethodDescription targetConstructor;
            private final String message;

            /**
             * Creates a new construction delegate that calls a constructor by handing it the given string.
             *
             * @param targetType The type of the isThrowable.
             * @param message    The string that is handed to the constructor.
             */
            public ForStringConstructor(TypeDescription targetType, String message) {
                this.targetType = targetType;
                this.targetConstructor = targetType.getDeclaredMethods()
                        .filter(isConstructor().and(takesArguments(String.class))).getOnly();
                this.message = message;
            }

            @Override
            public StackManipulation make() {
                return new StackManipulation.Compound(
                        TypeCreation.forType(targetType),
                        Duplication.SINGLE,
                        new TextConstant(message),
                        MethodInvocation.invoke(targetConstructor));
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && message.equals(((ForStringConstructor) other).message)
                        && targetType.equals(((ForStringConstructor) other).targetType);
            }

            @Override
            public int hashCode() {
                return 31 * targetType.hashCode() + message.hashCode();
            }

            @Override
            public String toString() {
                return "Exceptional.ConstructionDelegate.ForStringConstructor{" +
                        "targetType=" + targetType +
                        ", targetConstructor=" + targetConstructor +
                        ", message='" + message + '\'' +
                        '}';
            }
        }
    }
}
