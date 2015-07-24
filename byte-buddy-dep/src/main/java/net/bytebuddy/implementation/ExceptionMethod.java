package net.bytebuddy.implementation;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.bytecode.*;
import net.bytebuddy.implementation.bytecode.constant.TextConstant;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import org.objectweb.asm.MethodVisitor;

import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;
import static net.bytebuddy.utility.ByteBuddyCommons.nonNull;

/**
 * This implementation causes a {@link java.lang.Throwable} to be thrown when the instrumented method is invoked.
 * Be aware that the Java Virtual machine does not care about exception declarations and will throw any
 * {@link java.lang.Throwable} from any method even if the method does not declared a checked exception.
 */
public class ExceptionMethod implements Implementation, ByteCodeAppender {

    /**
     * The type of the exception that is thrown.
     */
    private final TypeDescription throwableType;

    /**
     * The construction delegation which is responsible for creating the exception to be thrown.
     */
    private final ConstructionDelegate constructionDelegate;

    /**
     * Creates a new instance of an implementation for throwing throwables.
     *
     * @param throwableType        The type of the exception to be thrown.
     * @param constructionDelegate A delegate that is responsible for calling the isThrowable's constructor.
     */
    public ExceptionMethod(TypeDescription throwableType, ConstructionDelegate constructionDelegate) {
        this.throwableType = throwableType;
        this.constructionDelegate = constructionDelegate;
    }

    /**
     * Creates an implementation that creates a new instance of the given isThrowable type on each method invocation
     * which is then thrown immediately. For this to be possible, the given type must define a default constructor
     * which is visible from the instrumented type.
     *
     * @param exceptionType The type of the isThrowable.
     * @return An implementation that will throw an instance of the isThrowable on each method invocation of the
     * instrumented methods.
     */
    public static Implementation throwing(Class<? extends Throwable> exceptionType) {
        return throwing(new TypeDescription.ForLoadedType(nonNull(exceptionType)));
    }

    /**
     * Creates an implementation that creates a new instance of the given isThrowable type on each method invocation
     * which is then thrown immediately. For this to be possible, the given type must define a default constructor
     * which is visible from the instrumented type.
     *
     * @param exceptionType The type of the isThrowable.
     * @return An implementation that will throw an instance of the isThrowable on each method invocation of the
     * instrumented methods.
     */
    public static Implementation throwing(TypeDescription exceptionType) {
        if (!exceptionType.isAssignableTo(Throwable.class)) {
            throw new IllegalArgumentException(exceptionType + " does not extend throwable");
        }
        return new ExceptionMethod(nonNull(exceptionType), new ConstructionDelegate.ForDefaultConstructor(exceptionType));
    }

    /**
     * Creates an implementation that creates a new instance of the given isThrowable type on each method invocation
     * which is then thrown immediately. For this to be possible, the given type must define a constructor that
     * takes a single {@link java.lang.String} as its argument.
     *
     * @param exceptionType The type of the isThrowable.
     * @param message       The string that is handed to the constructor. Usually an exception message.
     * @return An implementation that will throw an instance of the isThrowable on each method invocation of the
     * instrumented methods.
     */
    public static Implementation throwing(Class<? extends Throwable> exceptionType, String message) {
        return throwing(new TypeDescription.ForLoadedType(nonNull(exceptionType)), message);
    }

    /**
     * Creates an implementation that creates a new instance of the given isThrowable type on each method invocation
     * which is then thrown immediately. For this to be possible, the given type must define a constructor that
     * takes a single {@link java.lang.String} as its argument.
     *
     * @param exceptionType The type of the isThrowable.
     * @param message       The string that is handed to the constructor. Usually an exception message.
     * @return An implementation that will throw an instance of the isThrowable on each method invocation of the
     * instrumented methods.
     */
    public static Implementation throwing(TypeDescription exceptionType, String message) {
        if (!exceptionType.isAssignableTo(Throwable.class)) {
            throw new IllegalArgumentException(exceptionType + " does not extend throwable");
        }
        return new ExceptionMethod(nonNull(exceptionType), new ConstructionDelegate.ForStringConstructor(exceptionType, nonNull(message)));
    }

    @Override
    public InstrumentedType prepare(InstrumentedType instrumentedType) {
        return instrumentedType;
    }

    @Override
    public ByteCodeAppender appender(Target implementationTarget) {
        return this;
    }

    @Override
    public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
        StackManipulation.Size stackSize = new StackManipulation.Compound(
                constructionDelegate.make(),
                Throw.INSTANCE
        ).apply(methodVisitor, implementationContext);
        return new Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && constructionDelegate.equals(((ExceptionMethod) other).constructionDelegate)
                && throwableType.equals(((ExceptionMethod) other).throwableType);
    }

    @Override
    public int hashCode() {
        return 31 * throwableType.hashCode() + constructionDelegate.hashCode();
    }

    @Override
    public String toString() {
        return "ExceptionMethod{" +
                "throwableType=" + throwableType +
                ", constructionDelegate=" + constructionDelegate +
                '}';
    }

    /**
     * A construction delegate is responsible for calling a Throwable's constructor.
     */
    public interface ConstructionDelegate {

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
        class ForDefaultConstructor implements ConstructionDelegate {

            /**
             * The type of the exception that is to be thrown.
             */
            private final TypeDescription exceptionType;

            /**
             * The constructor that is used for creating the exception.
             */
            private final MethodDescription targetConstructor;

            /**
             * Creates a new construction delegate that calls a default constructor.
             *
             * @param exceptionType The type of the isThrowable.
             */
            public ForDefaultConstructor(TypeDescription exceptionType) {
                this.exceptionType = exceptionType;
                this.targetConstructor = exceptionType.getDeclaredMethods()
                        .filter(isConstructor().and(takesArguments(0))).getOnly();
            }

            @Override
            public StackManipulation make() {
                return new StackManipulation.Compound(
                        TypeCreation.forType(exceptionType),
                        Duplication.SINGLE,
                        MethodInvocation.invoke(targetConstructor));
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && exceptionType.equals(((ForDefaultConstructor) other).exceptionType);
            }

            @Override
            public int hashCode() {
                return exceptionType.hashCode();
            }

            @Override
            public String toString() {
                return "ExceptionMethod.ConstructionDelegate.ForDefaultConstructor{" +
                        "exceptionType=" + exceptionType +
                        ", targetConstructor=" + targetConstructor +
                        '}';
            }
        }

        /**
         * A construction delegate that calls a constructor that takes a single string as its argument.
         */
        class ForStringConstructor implements ConstructionDelegate {

            /**
             * The type of the exception that is to be thrown.
             */
            private final TypeDescription exceptionType;

            /**
             * The constructor that is used for creating the exception.
             */
            private final MethodDescription targetConstructor;

            /**
             * The {@link java.lang.String} that is to be passed to the exception's constructor.
             */
            private final String message;

            /**
             * Creates a new construction delegate that calls a constructor by handing it the given string.
             *
             * @param exceptionType The type of the isThrowable.
             * @param message       The string that is handed to the constructor.
             */
            public ForStringConstructor(TypeDescription exceptionType, String message) {
                this.exceptionType = exceptionType;
                this.targetConstructor = exceptionType.getDeclaredMethods()
                        .filter(isConstructor().and(takesArguments(String.class))).getOnly();
                this.message = message;
            }

            @Override
            public StackManipulation make() {
                return new StackManipulation.Compound(
                        TypeCreation.forType(exceptionType),
                        Duplication.SINGLE,
                        new TextConstant(message),
                        MethodInvocation.invoke(targetConstructor));
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && message.equals(((ForStringConstructor) other).message)
                        && exceptionType.equals(((ForStringConstructor) other).exceptionType);
            }

            @Override
            public int hashCode() {
                return 31 * exceptionType.hashCode() + message.hashCode();
            }

            @Override
            public String toString() {
                return "ExceptionMethod.ConstructionDelegate.ForStringConstructor{" +
                        "exceptionType=" + exceptionType +
                        ", targetConstructor=" + targetConstructor +
                        ", message='" + message + '\'' +
                        '}';
            }
        }
    }
}
