package net.bytebuddy.instrumentation;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import net.bytebuddy.instrumentation.method.bytecode.stack.Removal;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodInvocation;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodReturn;
import net.bytebuddy.instrumentation.method.bytecode.stack.member.MethodVariableAccess;
import net.bytebuddy.instrumentation.type.InstrumentedType;
import org.objectweb.asm.MethodVisitor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static net.bytebuddy.utility.ByteBuddyCommons.nonNull;

public class InvokeDynamic implements Instrumentation, ByteCodeAppender {

    public static InvokeDynamic bootstrap(Method method, Object... argument) {
        return bootstrap(new MethodDescription.ForLoadedMethod(nonNull(method)), argument);
    }

    public static InvokeDynamic bootstrap(Constructor<?> constructor, Object... argument) {
        return bootstrap(new MethodDescription.ForLoadedConstructor(nonNull(constructor)), argument);
    }

    public static InvokeDynamic bootstrap(MethodDescription methodDescription, Object... argument) {
        return new InvokeDynamic(nonNull(methodDescription),
                Arrays.asList(nonNull(argument)),
                TerminationHandler.ForMethodReturn.INSTANCE);
    }

    private final MethodDescription bootstrapMethod;

    private final List<?> arguments;

    private final TerminationHandler terminationHandler;

    protected InvokeDynamic(MethodDescription bootstrapMethod,
                            List<?> arguments,
                            TerminationHandler terminationHandler) {
        if (!bootstrapMethod.isBootstrap()) {
            throw new IllegalArgumentException("Not a valid bootstrap method: " + bootstrapMethod);
        }
        this.bootstrapMethod = bootstrapMethod;
        this.arguments = arguments;
        this.terminationHandler = terminationHandler;
    }

    public Instrumentation andThen(Instrumentation instrumentation) {
        return new Instrumentation.Compound(new InvokeDynamic(bootstrapMethod, arguments,
                TerminationHandler.ForChainedInvocation.INSTANCE), nonNull(instrumentation));
    }

    @Override
    public InstrumentedType prepare(InstrumentedType instrumentedType) {
        return instrumentedType;
    }

    @Override
    public ByteCodeAppender appender(Target instrumentationTarget) {
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
        StackManipulation.Size size = new StackManipulation.Compound(
                MethodVariableAccess.loadArguments(instrumentedMethod), // TODO: How to treat this?
                MethodInvocation.invoke(bootstrapMethod)
                        .dynamic(instrumentedMethod.getInternalName(),
                                instrumentedMethod.getReturnType(),
                                instrumentedMethod.getParameterTypes(),
                                arguments),
                terminationHandler.resolve(instrumentedMethod)
        ).apply(methodVisitor, instrumentationContext);
        return new Size(size.getMaximalSize(), instrumentedMethod.getStackSize());
    }

    /**
     * A termination handler is responsible to handle the return value of a method that is invoked via a
     * {@link net.bytebuddy.instrumentation.MethodCall}.
     */
    protected static interface TerminationHandler {

        /**
         * Returns a stack manipulation that handles the method return.
         *
         * @param interceptedMethod The method being intercepted.
         * @return A stack manipulation that handles the method return.
         */
        StackManipulation resolve(MethodDescription interceptedMethod);

        /**
         * Returns the return value if the method call from the intercepted method.
         */
        static enum ForMethodReturn implements TerminationHandler {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public StackManipulation resolve(MethodDescription interceptedMethod) {
                return MethodReturn.returning(interceptedMethod.getReturnType());
            }
        }

        /**
         * Drops the return value of the called method from the operand stack without returning from the intercepted
         * method.
         */
        static enum ForChainedInvocation implements TerminationHandler {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public StackManipulation resolve(MethodDescription interceptedMethod) {
                return Removal.pop(interceptedMethod.isConstructor()
                        ? interceptedMethod.getDeclaringType()
                        : interceptedMethod.getReturnType());
            }
        }
    }
}
