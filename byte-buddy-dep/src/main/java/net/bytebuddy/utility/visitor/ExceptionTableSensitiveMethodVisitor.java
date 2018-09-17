package net.bytebuddy.utility.visitor;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

/**
 * A {@link MethodVisitor} that adds a callback after visiting the exception table of a method.
 */
public abstract class ExceptionTableSensitiveMethodVisitor extends MethodVisitor {

    /**
     * {@code true} if the exception table callback was already triggered.
     */
    private boolean trigger;

    /**
     * Creates an exception table sensitive method visitor.
     *
     * @param api           The ASM API version.
     * @param methodVisitor The delegating method visitor.
     */
    protected ExceptionTableSensitiveMethodVisitor(int api, MethodVisitor methodVisitor) {
        super(api, methodVisitor);
        trigger = true;
    }

    /**
     * Considers if the end of the exception table was reached.
     */
    private void considerEndOfExceptionTable() {
        if (trigger) {
            trigger = false;
            onAfterExceptionTable();
        }
    }

    /**
     * Invoked after the exception table was visited. Typically, the exception table is visited by ASM at the beginning
     * of a method. It is however possible that a user adds exception table entries at a later point. Normally, this is
     * however not meaningful use of ASM.
     */
    protected abstract void onAfterExceptionTable();

    @Override
    public final void visitLabel(Label label) {
        considerEndOfExceptionTable();
        onVisitLabel(label);
    }

    /**
     * Visits a label.
     *
     * @param label The visited label.
     * @see MethodVisitor#visitLabel(Label)
     */
    protected void onVisitLabel(Label label) {
        super.visitLabel(label);
    }

    @Override
    public final void visitIntInsn(int opcode, int operand) {
        considerEndOfExceptionTable();
        onVisitIntInsn(opcode, operand);
    }

    /**
     * Visits an integer opcode.
     *
     * @param opcode  The visited opcode.
     * @param operand The visited operand.
     */
    protected void onVisitIntInsn(int opcode, int operand) {
        super.visitIntInsn(opcode, operand);
    }

    @Override
    public final void visitVarInsn(int opcode, int offset) {
        considerEndOfExceptionTable();
        onVisitVarInsn(opcode, offset);
    }

    /**
     * Visits an variable instruction.
     *
     * @param opcode The visited opcode.
     * @param offset The visited offset.
     */
    protected void onVisitVarInsn(int opcode, int offset) {
        super.visitVarInsn(opcode, offset);
    }

    @Override
    public final void visitTypeInsn(int opcode, String type) {
        considerEndOfExceptionTable();
        onVisitTypeInsn(opcode, type);
    }

    /**
     * Visits a type instruction.
     *
     * @param opcode The visited opcode.
     * @param type   The type name.
     */
    protected void onVisitTypeInsn(int opcode, String type) {
        super.visitTypeInsn(opcode, type);
    }

    @Override
    public final void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        considerEndOfExceptionTable();
        onVisitFieldInsn(opcode, owner, name, descriptor);
    }

    /**
     * Visits a field instruction.
     *
     * @param opcode     The visited opcode.
     * @param owner      The field's owner.
     * @param name       The field's name.
     * @param descriptor The field's descriptor.
     */
    protected void onVisitFieldInsn(int opcode, String owner, String name, String descriptor) {
        super.visitFieldInsn(opcode, owner, name, descriptor);
    }

    @Override
    @SuppressWarnings("deprecation")
    public final void visitMethodInsn(int opcode, String owner, String name, String descriptor) {
        considerEndOfExceptionTable();
        onVisitMethodInsn(opcode, owner, name, descriptor);
    }

    /**
     * Visits a method instruction.
     *
     * @param opcode     The visited opcode.
     * @param owner      The method's owner.
     * @param name       The method's internal name.
     * @param descriptor The method's descriptor.
     * @deprecated Use {@link ExceptionTableSensitiveMethodVisitor#onVisitMethodInsn(int, String, String, String, boolean)} instead.
     */
    @Deprecated
    @SuppressWarnings("deprecation")
    protected void onVisitMethodInsn(int opcode, String owner, String name, String descriptor) {
        considerEndOfExceptionTable();
        super.visitMethodInsn(opcode, owner, name, descriptor);
    }

    @Override
    public final void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        considerEndOfExceptionTable();
        onVisitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }

    /**
     * Visits a method instruction.
     *
     * @param opcode     The visited opcode.
     * @param owner      The method's owner.
     * @param name       The method's internal name.
     * @param descriptor The method's descriptor.
     * @param isInterface      {@code true} if the method belongs to an interface.
     */
    protected void onVisitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }

    @Override
    public final void visitInvokeDynamicInsn(String name, String descriptor, Handle handle, Object... argument) {
        considerEndOfExceptionTable();
        onVisitInvokeDynamicInsn(name, descriptor, handle, argument);
    }

    /**
     * Visits an invoke dynamic instruction.
     *
     * @param name       The name of the method.
     * @param descriptor The descriptor of the method.
     * @param handle     The bootstrap method handle.
     * @param argument   The bootstrap method arguments.
     */
    protected void onVisitInvokeDynamicInsn(String name, String descriptor, Handle handle, Object... argument) {
        super.visitInvokeDynamicInsn(name, descriptor, handle, argument);
    }

    @Override
    public final void visitJumpInsn(int opcode, Label label) {
        considerEndOfExceptionTable();
        onVisitJumpInsn(opcode, label);
    }

    /**
     * Visits a jump instruction.
     *
     * @param opcode The visited opcode.
     * @param label  The visited label.
     */
    protected void onVisitJumpInsn(int opcode, Label label) {
        super.visitJumpInsn(opcode, label);
    }

    @Override
    public final void visitLdcInsn(Object constant) {
        considerEndOfExceptionTable();
        onVisitLdcInsn(constant);
    }

    /**
     * Visits a constant pool access instruction.
     *
     * @param constant The constant pool value.
     */
    protected void onVisitLdcInsn(Object constant) {
        super.visitLdcInsn(constant);
    }

    @Override
    public final void visitIincInsn(int offset, int increment) {
        considerEndOfExceptionTable();
        onVisitIincInsn(offset, increment);
    }

    /**
     * Visits an increment instruction.
     *
     * @param offset    The offset of the accessed variable.
     * @param increment The value with which to increment.
     */
    protected void onVisitIincInsn(int offset, int increment) {
        super.visitIincInsn(offset, increment);
    }

    @Override
    public final void visitTableSwitchInsn(int minimum, int maximum, Label defaultTarget, Label... label) {
        considerEndOfExceptionTable();
        onVisitTableSwitchInsn(minimum, maximum, defaultTarget, label);
    }

    /**
     * Visits a table switch instruction.
     *
     * @param minimum           The minimum index.
     * @param maximum           The maximum index.
     * @param defaultTarget A label indicating the default value.
     * @param label         Labels indicating the jump targets.
     */
    protected void onVisitTableSwitchInsn(int minimum, int maximum, Label defaultTarget, Label... label) {
        super.visitTableSwitchInsn(minimum, maximum, defaultTarget, label);
    }

    @Override
    public final void visitLookupSwitchInsn(Label dflt, int[] key, Label[] label) {
        considerEndOfExceptionTable();
        onVisitLookupSwitchInsn(dflt, key, label);
    }

    /**
     * Visits a lookup switch instruction.
     *
     * @param defaultTarget The default option.
     * @param key          The key values.
     * @param label           The targets for each key.
     */
    protected void onVisitLookupSwitchInsn(Label defaultTarget, int[] key, Label[] label) {
        super.visitLookupSwitchInsn(defaultTarget, key, label);
    }

    @Override
    public final void visitMultiANewArrayInsn(String descriptor, int dimensions) {
        considerEndOfExceptionTable();
        onVisitMultiANewArrayInsn(descriptor, dimensions);
    }

    /**
     * Visits an instruction for creating a multidimensional array.
     *
     * @param descriptor The type descriptor of the array's component type.
     * @param dimensions The dimensions of the array.
     */
    protected void onVisitMultiANewArrayInsn(String descriptor, int dimensions) {
        super.visitMultiANewArrayInsn(descriptor, dimensions);
    }

    @Override
    public final void visitInsn(int opcode) {
        considerEndOfExceptionTable();
        onVisitInsn(opcode);
    }

    /**
     * Visits a simple instruction.
     *
     * @param opcode The opcode of the instruction.
     */
    protected void onVisitInsn(int opcode) {
        super.visitInsn(opcode);
    }
}
