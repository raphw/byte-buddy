package net.bytebuddy.instrumentation.method.bytecode.stack.constant;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.collection.ArrayFactory;
import net.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the creation of a {@link java.lang.reflect.Method} value which can be created from a given
 * set of constant pool values and can therefore be considered a constant in the broader meaing.
 */
public abstract class MethodConstant implements StackManipulation {

    /**
     * The internal name of the {@link Class} type.
     */
    private static final String CLASS_TYPE_INTERNAL_NAME = "java/lang/Class";

    /**
     * A description of the method to be loaded onto the stack.
     */
    protected final MethodDescription methodDescription;

    /**
     * Creates a new method constant.
     *
     * @param methodDescription The method description for which the {@link java.lang.reflect.Method} representation
     *                          should be created.
     */
    protected MethodConstant(MethodDescription methodDescription) {
        this.methodDescription = methodDescription;
    }

    /**
     * Creates a stack manipulation that loads a method constant onto the operand stack.
     *
     * @param methodDescription The method to be loaded onto the stack.
     * @return A stack manipulation that assigns a method constant for the given method description.
     */
    public static StackManipulation forMethod(MethodDescription methodDescription) {
        if (methodDescription.isConstructor()) {
            return new ForConstructor(methodDescription);
        } else {
            return new ForMethod(methodDescription);
        }
    }

    /**
     * Returns a list of type constant load operations for the given list of parameters.
     *
     * @param parameterTypes A list of all type descriptions that should be represented as type constant
     *                       load operations.
     * @return A corresponding list of type constant load operations.
     */
    private static List<StackManipulation> typeConstantsFor(List<TypeDescription> parameterTypes) {
        List<StackManipulation> typeConstants = new ArrayList<StackManipulation>(parameterTypes.size());
        for (TypeDescription parameterType : parameterTypes) {
            typeConstants.add(ClassConstant.of(parameterType));
        }
        return typeConstants;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
        Size argumentSize = prepare(methodVisitor)
                .aggregate(ArrayFactory.targeting(new TypeDescription.ForLoadedType(Class.class))
                        .withValues(typeConstantsFor(methodDescription.getParameterTypes()))
                        .apply(methodVisitor, instrumentationContext));
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                CLASS_TYPE_INTERNAL_NAME,
                getMethodName(),
                getDescriptor(),
                false);
        return new Size(1, argumentSize.getMaximalSize());
    }

    /**
     * Applies all preparation to the given method visitor.
     *
     * @param methodVisitor The method visitor to which the preparation is applied.
     * @return The size of this preparation.
     */
    protected abstract Size prepare(MethodVisitor methodVisitor);

    /**
     * Returns the name of the {@link java.lang.Class} method for creating this method constant.
     *
     * @return The descriptor for creating this method constant.
     */
    protected abstract String getMethodName();

    /**
     * Returns the descriptor of the {@link java.lang.Class} method for creating this method constant.
     *
     * @return The descriptor for creating this method constant.
     */
    protected abstract String getDescriptor();

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && methodDescription.equals(((MethodConstant) other).methodDescription);
    }

    @Override
    public int hashCode() {
        return methodDescription.hashCode();
    }

    @Override
    public String toString() {
        return "MethodConstant{methodDescription=" + methodDescription + '}';
    }

    /**
     * Creates a {@link net.bytebuddy.instrumentation.method.bytecode.stack.constant.MethodConstant} for loading
     * a {@link java.lang.reflect.Method} instance onto the operand stack.
     */
    private static class ForMethod extends MethodConstant {

        /**
         * The name of the {@link java.lang.Class#getDeclaredMethod(String, Class[])} method.
         */
        private static final String GET_DECLARED_METHOD_NAME = "getDeclaredMethod";

        /**
         * The descriptor of the {@link java.lang.Class#getDeclaredMethod(String, Class[])} method.
         */
        private static final String GET_DECLARED_METHOD_DESCRIPTOR =
                "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;";

        /**
         * Creates a new {@link net.bytebuddy.instrumentation.method.bytecode.stack.constant.MethodConstant} for
         * creating a {@link java.lang.reflect.Method} instance.
         *
         * @param methodDescription The method to be loaded onto the stack.
         */
        private ForMethod(MethodDescription methodDescription) {
            super(methodDescription);
        }

        @Override
        protected Size prepare(MethodVisitor methodVisitor) {
            methodVisitor.visitLdcInsn(Type.getType(methodDescription.getDeclaringType().getDescriptor()));
            methodVisitor.visitLdcInsn(methodDescription.getInternalName());
            return new Size(2, 2);
        }

        @Override
        protected String getMethodName() {
            return GET_DECLARED_METHOD_NAME;
        }

        @Override
        protected String getDescriptor() {
            return GET_DECLARED_METHOD_DESCRIPTOR;
        }
    }

    /**
     * Creates a {@link net.bytebuddy.instrumentation.method.bytecode.stack.constant.MethodConstant} for loading
     * a {@link java.lang.reflect.Constructor} instance onto the operand stack.
     */
    private static class ForConstructor extends MethodConstant {

        /**
         * The name of the {@link java.lang.Class#getDeclaredMethod(String, Class[])} method.
         */
        private static final String GET_DECLARED_CONSTRUCTOR_NAME = "getDeclaredConstructor";

        /**
         * The descriptor of the {@link java.lang.Class#getDeclaredMethod(String, Class[])} method.
         */
        private static final String GET_DECLARED_CONSTRUCTOR_DESCRIPTOR =
                "([Ljava/lang/Class;)Ljava/lang/reflect/Constructor;";

        /**
         * Creates a new {@link net.bytebuddy.instrumentation.method.bytecode.stack.constant.MethodConstant} for
         * creating a {@link java.lang.reflect.Constructor} instance.
         *
         * @param methodDescription The constructor to be loaded onto the stack.
         */
        private ForConstructor(MethodDescription methodDescription) {
            super(methodDescription);
        }

        @Override
        protected Size prepare(MethodVisitor methodVisitor) {
            methodVisitor.visitLdcInsn(Type.getType(methodDescription.getDeclaringType().getDescriptor()));
            return new Size(1, 1);
        }

        @Override
        protected String getMethodName() {
            return GET_DECLARED_CONSTRUCTOR_NAME;
        }

        @Override
        protected String getDescriptor() {
            return GET_DECLARED_CONSTRUCTOR_DESCRIPTOR;
        }
    }
}
