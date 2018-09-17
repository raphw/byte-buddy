package net.bytebuddy.implementation;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.bytecode.*;
import net.bytebuddy.implementation.bytecode.constant.TextConstant;
import net.bytebuddy.implementation.bytecode.member.MethodInvocation;
import org.objectweb.asm.MethodVisitor;

import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

/**
 * This implementation causes a {@link java.lang.Throwable} to be thrown when the instrumented method is invoked.
 * Be aware that the Java Virtual machine does not care about exception declarations and will throw any
 * {@link java.lang.Throwable} from any method even if the method does not declared a checked exception.
 */
@HashCodeAndEqualsPlugin.Enhance
public class ExceptionMethod implements Implementation, ByteCodeAppender {

    /**
     * The construction delegation which is responsible for creating the exception to be thrown.
     */
    private final ConstructionDelegate constructionDelegate;

    /**
     * Creates a new instance of an implementation for throwing throwables.
     *
     * @param constructionDelegate A delegate that is responsible for calling the {@link Throwable}'s constructor.
     */
    public ExceptionMethod(ConstructionDelegate constructionDelegate) {
        this.constructionDelegate = constructionDelegate;
    }

    /**
     * Creates an implementation that creates a new instance of the given {@link Throwable} type on each method invocation
     * which is then thrown immediately. For this to be possible, the given type must define a default constructor
     * which is visible from the instrumented type.
     *
     * @param throwableType The type of the {@link Throwable}.
     * @return An implementation that will throw an instance of the {@link Throwable} on each method invocation of the
     * instrumented methods.
     */
    public static Implementation throwing(Class<? extends Throwable> throwableType) {
        return throwing(TypeDescription.ForLoadedType.of(throwableType));
    }

    /**
     * Creates an implementation that creates a new instance of the given {@link Throwable} type on each method invocation
     * which is then thrown immediately. For this to be possible, the given type must define a default constructor
     * which is visible from the instrumented type.
     *
     * @param throwableType The type of the {@link Throwable}.
     * @return An implementation that will throw an instance of the {@link Throwable} on each method invocation of the
     * instrumented methods.
     */
    public static Implementation throwing(TypeDescription throwableType) {
        if (!throwableType.isAssignableTo(Throwable.class)) {
            throw new IllegalArgumentException(throwableType + " does not extend throwable");
        }
        return new ExceptionMethod(new ConstructionDelegate.ForDefaultConstructor(throwableType));
    }

    /**
     * Creates an implementation that creates a new instance of the given {@link Throwable} type on each method
     * invocation which is then thrown immediately. For this to be possible, the given type must define a
     * constructor that takes a single {@link java.lang.String} as its argument.
     *
     * @param throwableType The type of the {@link Throwable}.
     * @param message       The string that is handed to the constructor. Usually an exception message.
     * @return An implementation that will throw an instance of the {@link Throwable} on each method invocation
     * of the instrumented methods.
     */
    public static Implementation throwing(Class<? extends Throwable> throwableType, String message) {
        return throwing(TypeDescription.ForLoadedType.of(throwableType), message);
    }

    /**
     * Creates an implementation that creates a new instance of the given {@link Throwable} type on each method
     * invocation which is then thrown immediately. For this to be possible, the given type must define a
     * constructor that takes a single {@link java.lang.String} as its argument.
     *
     * @param throwableType The type of the {@link Throwable}.
     * @param message       The string that is handed to the constructor. Usually an exception message.
     * @return An implementation that will throw an instance of the {@link Throwable} on each method invocation
     * of the instrumented methods.
     */
    public static Implementation throwing(TypeDescription throwableType, String message) {
        if (!throwableType.isAssignableTo(Throwable.class)) {
            throw new IllegalArgumentException(throwableType + " does not extend throwable");
        }
        return new ExceptionMethod(new ConstructionDelegate.ForStringConstructor(throwableType, message));
    }

    /**
     * {@inheritDoc}
     */
    public InstrumentedType prepare(InstrumentedType instrumentedType) {
        return instrumentedType;
    }

    /**
     * {@inheritDoc}
     */
    public ByteCodeAppender appender(Target implementationTarget) {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
        StackManipulation.Size stackSize = new StackManipulation.Compound(
                constructionDelegate.make(),
                Throw.INSTANCE
        ).apply(methodVisitor, implementationContext);
        return new Size(stackSize.getMaximalSize(), instrumentedMethod.getStackSize());
    }

    /**
     * A construction delegate is responsible for calling a {@link Throwable}'s constructor.
     */
    public interface ConstructionDelegate {

        /**
         * Creates a stack manipulation that creates pushes all constructor arguments onto the operand stack
         * and subsequently calls the constructor.
         *
         * @return A stack manipulation for constructing a {@link Throwable}.
         */
        StackManipulation make();

        /**
         * A construction delegate that calls the default constructor.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class ForDefaultConstructor implements ConstructionDelegate {

            /**
             * The type of the exception that is to be thrown.
             */
            private final TypeDescription throwableType;

            /**
             * The constructor that is used for creating the exception.
             */
            private final MethodDescription targetConstructor;

            /**
             * Creates a new construction delegate that calls a default constructor.
             *
             * @param throwableType The type of the {@link Throwable}.
             */
            public ForDefaultConstructor(TypeDescription throwableType) {
                this.throwableType = throwableType;
                this.targetConstructor = throwableType.getDeclaredMethods()
                        .filter(isConstructor().and(takesArguments(0))).getOnly();
            }

            /**
             * {@inheritDoc}
             */
            public StackManipulation make() {
                return new StackManipulation.Compound(
                        TypeCreation.of(throwableType),
                        Duplication.SINGLE,
                        MethodInvocation.invoke(targetConstructor));
            }
        }

        /**
         * A construction delegate that calls a constructor that takes a single string as its argument.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class ForStringConstructor implements ConstructionDelegate {

            /**
             * The type of the exception that is to be thrown.
             */
            private final TypeDescription throwableType;

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
             * @param throwableType The type of the {@link Throwable}.
             * @param message       The string that is handed to the constructor.
             */
            public ForStringConstructor(TypeDescription throwableType, String message) {
                this.throwableType = throwableType;
                this.targetConstructor = throwableType.getDeclaredMethods()
                        .filter(isConstructor().and(takesArguments(String.class))).getOnly();
                this.message = message;
            }

            /**
             * {@inheritDoc}
             */
            public StackManipulation make() {
                return new StackManipulation.Compound(
                        TypeCreation.of(throwableType),
                        Duplication.SINGLE,
                        new TextConstant(message),
                        MethodInvocation.invoke(targetConstructor));
            }
        }
    }
}
