package net.bytebuddy.instrumentation.method.bytecode.stack.constant;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.field.FieldDescription;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import net.bytebuddy.instrumentation.type.TypeDescription;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * A constant for a Java 7 {@code java.lang.invoke.MethodHandle}.
 */
public class MethodHandleConstant implements StackManipulation {

    /**
     * The type description for a method handle. This way, we avoid a compile time dependency to this
     * type which allows us to run Byte Buddy in Java 6.
     */
    private static final String METHOD_HANDLE_TYPE_INTERNAL_NAME = "java/lang/invoke/MethodHandle";
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
     * Checks if a type represents the {@code java.lang.invoke.MethodHandle} type.
     *
     * @param typeDescription The type to be checked.
     * @return {@code true} if the given type represents a {@code java.lang.invoke.MethodHandle}.
     */
    public static boolean isRepresentedBy(TypeDescription typeDescription) {
        return typeDescription.getInternalName().equals(METHOD_HANDLE_TYPE_INTERNAL_NAME);
    }

    /**
     * Creates a method handle for a method.
     *
     * @param methodDescription The method for which a method handle is to be put onto the operand stack.
     * @return A stack manipulation that represents the loading of the handle.
     */
    public static StackManipulation of(MethodDescription methodDescription) {
        return new MethodHandleConstant(new Handle(tagFor(methodDescription),
                methodDescription.getDeclaringType().getInternalName(),
                methodDescription.getInternalName(),
                methodDescription.getDescriptor()));
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
        } else if (methodDescription.isStatic() || methodDescription.isTypeInitializer()) {
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
                fieldDescription.getDeclaringType().getInternalName(),
                fieldDescription.getInternalName(),
                fieldDescription.getDescriptor()));
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Size apply(MethodVisitor methodVisitor, Instrumentation.Context instrumentationContext) {
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
