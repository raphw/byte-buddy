package net.bytebuddy.instrumentation;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import net.bytebuddy.instrumentation.method.bytecode.stack.Duplication;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.TypeCreation;
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
        return new InvokeDynamic(nonNull(methodDescription), Arrays.asList(nonNull(argument)));
    }

    private final MethodDescription bootstrapMethod;

    private final List<?> arguments;

    protected InvokeDynamic(MethodDescription bootstrapMethod, List<?> arguments) {
        if (!bootstrapMethod.isBootstrap()) {
            throw new IllegalArgumentException("Not a valid bootstrap method: " + bootstrapMethod);
        }
        this.bootstrapMethod = bootstrapMethod;
        this.arguments = arguments;
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
                MethodReturn.returning(instrumentedMethod.getReturnType())
        ).apply(methodVisitor, instrumentationContext);
        return new Size(size.getMaximalSize(), instrumentedMethod.getStackSize());
    }
}
