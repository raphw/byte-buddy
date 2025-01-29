package net.bytebuddy.implementation.bytecode.member;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.constant.JavaConstantValue;
import net.bytebuddy.utility.JavaConstant;
import org.objectweb.asm.MethodVisitor;

import java.util.List;

/**
 * A stack manipulation for an invokedynamic invocation.
 */
@HashCodeAndEqualsPlugin.Enhance
public class Invokedynamic extends StackManipulation.AbstractBase {

    /**
     * The name of the invokedynamic expression.
     */
    private final String name;

    /**
     * The type that is requested for the target.
     */
    private final JavaConstant.MethodType type;

    /**
     * A handle to the bootstrap method.
     */
    private final JavaConstant.MethodHandle bootstrap;

    /**
     * The constant arguments to the bootstrap method.
     */
    private final List<? extends JavaConstant> arguments;

    /**
     * Creates a stack manipulation for a bootstrap method.
     *
     * @param name      The name of the invokedynamic expression.
     * @param type      The type that is requested for the target.
     * @param bootstrap A handle to the bootstrap method.
     * @param arguments The constant arguments to the bootstrap method.
     */
    public Invokedynamic(String name,
                         JavaConstant.MethodType type,
                         JavaConstant.MethodHandle bootstrap,
                         List<? extends JavaConstant> arguments) {
        this.name = name;
        this.type = type;
        this.bootstrap = bootstrap;
        this.arguments = arguments;
    }

    /**
     * {@inheritDoc}
     */
    public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
        Object[] argument = new Object[arguments.size()];
        for (int index = 0; index < arguments.size(); index++) {
            argument[index] = arguments.get(index).accept(JavaConstantValue.Visitor.INSTANCE);
        }
        methodVisitor.visitInvokeDynamicInsn(name,
                type.getDescriptor(),
                JavaConstantValue.Visitor.INSTANCE.onMethodHandle(bootstrap),
                argument);
        return new Size(
                type.getReturnType().getStackSize().getSize() - type.getParameterTypes().getStackSize(),
                type.getReturnType().getStackSize().getSize());
    }
}
