package net.bytebuddy.implementation.bytecode.constant;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.collection.ArrayFactory;
import net.bytebuddy.implementation.bytecode.member.FieldAccess;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the creation of a {@link java.lang.reflect.Method} value which can be created from a given
 * set of constant pool values and can therefore be considered a constant in the broader meaning.
 */
public abstract class MethodConstant implements StackManipulation {

    /**
     * The internal name of the {@link Class} type.
     */
    private static final String CLASS_TYPE_INTERNAL_NAME = "java/lang/Class";

    /**
     * A description of the method to be loaded onto the stack.
     */
    protected final MethodDescription.InDefinedShape methodDescription;

    /**
     * Creates a new method constant.
     *
     * @param methodDescription The method description for which the {@link java.lang.reflect.Method} representation
     *                          should be created.
     */
    protected MethodConstant(MethodDescription.InDefinedShape methodDescription) {
        this.methodDescription = methodDescription;
    }

    /**
     * Creates a stack manipulation that loads a method constant onto the operand stack.
     *
     * @param methodDescription The method to be loaded onto the stack.
     * @return A stack manipulation that assigns a method constant for the given method description.
     */
    public static CanCache forMethod(MethodDescription.InDefinedShape methodDescription) {
        if (methodDescription.isTypeInitializer()) {
            return CanCacheIllegal.INSTANCE;
        } else if (methodDescription.isConstructor()) {
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
    public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
        Size argumentSize = prepare(methodVisitor)
                .aggregate(ArrayFactory.forType(TypeDescription.CLASS)
                        .withValues(typeConstantsFor(methodDescription.getParameters().asTypeList().asErasures()))
                        .apply(methodVisitor, implementationContext));
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

    /**
     * Returns a cached version of this method constant as specified by
     * {@link net.bytebuddy.implementation.bytecode.constant.MethodConstant.Cached}.
     *
     * @return A cached version of this method constant.
     */
    public StackManipulation cached() {
        return new Cached(this);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && methodDescription.equals(((MethodConstant) other).methodDescription);
    }

    @Override
    public int hashCode() {
        return methodDescription.hashCode();
    }

    /**
     * Represents a method constant that cannot be represented by Java's reflection API.
     */
    protected enum CanCacheIllegal implements CanCache {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public StackManipulation cached() {
            return Illegal.INSTANCE;
        }

        @Override
        public boolean isValid() {
            return false;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
            return Illegal.INSTANCE.apply(methodVisitor, implementationContext);
        }

        @Override
        public String toString() {
            return "MethodConstant.CanCacheIllegal." + name();
        }
    }

    /**
     * Represents a {@link net.bytebuddy.implementation.bytecode.constant.MethodConstant} that is
     * directly loaded onto the operand stack without caching the value. Since the look-up of a Java method bares
     * some costs that sometimes need to be avoided, such a stack manipulation offers a convenience method for
     * defining this loading instruction as the retrieval of a field value that is initialized in the instrumented
     * type's type initializer.
     */
    public interface CanCache extends StackManipulation {

        /**
         * Returns this method constant as a cached version.
         *
         * @return A cached version of the method constant that is represented by this instance.
         */
        StackManipulation cached();
    }

    /**
     * Creates a {@link net.bytebuddy.implementation.bytecode.constant.MethodConstant} for loading
     * a {@link java.lang.reflect.Method} instance onto the operand stack.
     */
    protected static class ForMethod extends MethodConstant implements CanCache {

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
         * Creates a new {@link net.bytebuddy.implementation.bytecode.constant.MethodConstant} for
         * creating a {@link java.lang.reflect.Method} instance.
         *
         * @param methodDescription The method to be loaded onto the stack.
         */
        protected ForMethod(MethodDescription.InDefinedShape methodDescription) {
            super(methodDescription);
        }

        @Override
        protected Size prepare(MethodVisitor methodVisitor) {
            methodVisitor.visitLdcInsn(Type.getType(methodDescription.getDeclaringType().asErasure().getDescriptor()));
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

        @Override
        public String toString() {
            return "MethodConstant.ForMethod{methodDescription=" + methodDescription + '}';
        }
    }

    /**
     * Creates a {@link net.bytebuddy.implementation.bytecode.constant.MethodConstant} for loading
     * a {@link java.lang.reflect.Constructor} instance onto the operand stack.
     */
    protected static class ForConstructor extends MethodConstant implements CanCache {

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
         * Creates a new {@link net.bytebuddy.implementation.bytecode.constant.MethodConstant} for
         * creating a {@link java.lang.reflect.Constructor} instance.
         *
         * @param methodDescription The constructor to be loaded onto the stack.
         */
        protected ForConstructor(MethodDescription.InDefinedShape methodDescription) {
            super(methodDescription);
        }

        @Override
        protected Size prepare(MethodVisitor methodVisitor) {
            methodVisitor.visitLdcInsn(Type.getType(methodDescription.getDeclaringType().asErasure().getDescriptor()));
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

        @Override
        public String toString() {
            return "MethodConstant.ForConstructor{methodDescription=" + methodDescription + '}';
        }
    }

    /**
     * Represents a cached {@link net.bytebuddy.implementation.bytecode.constant.MethodConstant}.
     */
    protected static class Cached implements StackManipulation {

        /**
         * A description of the {@link java.lang.reflect.Method} type.
         */
        private static final TypeDescription METHOD_TYPE = new TypeDescription.ForLoadedType(Method.class);

        /**
         * The stack manipulation that is represented by this caching wrapper.
         */
        private final StackManipulation methodConstant;

        /**
         * Creates a new cached {@link net.bytebuddy.implementation.bytecode.constant.MethodConstant}.
         *
         * @param methodConstant The method constant to store in the field cache.
         */
        protected Cached(StackManipulation methodConstant) {
            this.methodConstant = methodConstant;
        }

        @Override
        public boolean isValid() {
            return methodConstant.isValid();
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
            return FieldAccess.forField(implementationContext.cache(methodConstant, METHOD_TYPE)).getter()
                    .apply(methodVisitor, implementationContext);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && methodConstant.equals(((Cached) other).methodConstant);
        }

        @Override
        public int hashCode() {
            return 31 * methodConstant.hashCode();
        }

        @Override
        public String toString() {
            return "MethodConstant.Cached{methodConstant=" + methodConstant + '}';
        }
    }
}
