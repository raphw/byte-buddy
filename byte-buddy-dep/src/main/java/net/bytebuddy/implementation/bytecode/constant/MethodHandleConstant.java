package net.bytebuddy.implementation.bytecode.constant;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.utility.JavaInstance;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * A constant for a Java 7 {@code java.lang.invoke.MethodHandle}.
 */
public class MethodHandleConstant implements StackManipulation {

    /**
     * The size of a {@code java.lang.invoke.MethodHandle} on the operand stack.
     */
    private static final Size SIZE = StackSize.SINGLE.toIncreasingSize();

    /**
     * The ASM handle for the creating the given method handle.
     */
    private final Handle handle;

    /**
     * Creates a new method handle constant.
     *
     * @param handle The ASM handle for loading the handle onto the operand stack.
     */
    private MethodHandleConstant(Handle handle) {
        this.handle = handle;
    }

    /**
     * Creates a method handle for a method.
     *
     * @param methodDescription The method for which a method handle is to be put onto the operand stack.
     * @return A stack manipulation that represents the loading of the handle.
     */
    public static StackManipulation of(MethodDescription.InDefinedShape methodDescription) {
        return methodDescription.isTypeInitializer()
                ? Illegal.INSTANCE
                : new MethodHandleConstant(new Handle(tagFor(methodDescription),
                methodDescription.getDeclaringType().asErasure().getInternalName(),
                methodDescription.getInternalName(),
                methodDescription.getDescriptor()));
    }

    /**
     * Creates stack manipulation for loading the provided method handle onto the operand stack.
     *
     * @param methodHandle The method handle to load onto the operand stack.
     * @return A stack manipulation for loading the provided method handle onto the operand stack.
     */
    public static StackManipulation of(JavaInstance.MethodHandle methodHandle) {
        Type[] parameterType = new Type[methodHandle.getParameterTypes().size()];
        int index = 0;
        for (TypeDescription typeDescription : methodHandle.getParameterTypes()) {
            parameterType[index++] = Type.getType(typeDescription.getDescriptor());
        }
        return new MethodHandleConstant(new Handle(methodHandle.getHandleType().getIdentifier(),
                methodHandle.getOwnerType().getInternalName(),
                methodHandle.getName(),
                Type.getMethodDescriptor(Type.getType(methodHandle.getReturnType().getDescriptor()), parameterType)));
    }

    /**
     * Looks up the handle tag for the given method.
     *
     * @param methodDescription The method for which a method handle is to be put onto the operand stack.
     * @return The tag for the handle of this method.
     */
    private static int tagFor(MethodDescription methodDescription) {
        if (methodDescription.isConstructor()) {
            return Opcodes.H_NEWINVOKESPECIAL;
        } else if (methodDescription.isStatic()) {
            return Opcodes.H_INVOKESTATIC;
        } else if (methodDescription.isPrivate() || methodDescription.isDefaultMethod()) {
            return Opcodes.H_INVOKESPECIAL;
        } else if (methodDescription.isInterface()) {
            return Opcodes.H_INVOKEINTERFACE;
        } else {
            return Opcodes.H_INVOKEVIRTUAL;
        }
    }

    /**
     * Creates a method handle for a field getter.
     *
     * @param fieldDescription The field for which a get handle is to be put onto the operand stack.
     * @return A stack manipulation that represents the loading of the handle.
     */
    public static StackManipulation ofGetter(FieldDescription fieldDescription) {
        return of(fieldDescription, fieldDescription.isStatic() ? Opcodes.H_GETSTATIC : Opcodes.H_GETFIELD);
    }

    /**
     * Creates a method handle for a field putter.
     *
     * @param fieldDescription The field for which a put handle is to be put onto the operand stack.
     * @return A stack manipulation that represents the loading of the handle.
     */
    public static StackManipulation ofPutter(FieldDescription fieldDescription) {
        return of(fieldDescription, fieldDescription.isStatic() ? Opcodes.H_PUTSTATIC : Opcodes.H_PUTFIELD);
    }

    /**
     * Creates a method handle for a field.
     *
     * @param fieldDescription The field for which a handle is to be put onto the operand stack.
     * @param tag              The tag for this handle.
     * @return A stack manipulation that represents the loading of the handle.
     */
    private static StackManipulation of(FieldDescription fieldDescription, int tag) {
        return new MethodHandleConstant(new Handle(tag,
                fieldDescription.getDeclaringType().asErasure().getInternalName(),
                fieldDescription.getInternalName(),
                fieldDescription.getDescriptor()));
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext) {
        methodVisitor.visitLdcInsn(handle);
        return SIZE;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && handle.equals(((MethodHandleConstant) other).handle);
    }

    @Override
    public int hashCode() {
        return handle.hashCode();
    }

    @Override
    public String toString() {
        return "MethodHandleConstant{handle=" + handle + '}';
    }
}
