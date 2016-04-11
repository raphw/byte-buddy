package net.bytebuddy.utility;

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
    public final void visitVarInsn(int opcode, int var) {
        considerEndOfExceptionTable();
        onVisitVarInsn(opcode, var);
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
    public final void visitFieldInsn(int opcode, String owner, String name, String desc) {
        considerEndOfExceptionTable();
        onVisitFieldInsn(opcode, owner, name, desc);
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
    public final void visitMethodInsn(int opcode, String owner, String name, String desc) {
        considerEndOfExceptionTable();
        onVisitMethodInsn(opcode, owner, name, desc);
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
    public final void visitMethodInsn(int opcode, String owner, String name, String desc, boolean iFace) {
        considerEndOfExceptionTable();
        onVisitMethodInsn(opcode, owner, name, desc, iFace);
    }

    /**
     * Visits a method instruction.
     *
     * @param opcode     The visited opcode.
     * @param owner      The method's owner.
     * @param name       The method's internal name.
     * @param descriptor The method's descriptor.
     * @param iFace      {@code true} if the method belongs to an interface.
     */
    protected void onVisitMethodInsn(int opcode, String owner, String name, String descriptor, boolean iFace) {
        super.visitMethodInsn(opcode, owner, name, descriptor, iFace);
    }

    @Override
    public final void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
        considerEndOfExceptionTable();
        onVisitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
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
    public final void visitLdcInsn(Object cst) {
        considerEndOfExceptionTable();
        onVisitLdcInsn(cst);
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
    public final void visitIincInsn(int var, int increment) {
        considerEndOfExceptionTable();
        onVisitIincInsn(var, increment);
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
    public final void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        considerEndOfExceptionTable();
        onVisitTableSwitchInsn(min, max, dflt, labels);
    }

    /**
     * Visits a table switch instruction.
     *
     * @param min           The minimum index.
     * @param max           The maximum index.
     * @param defaultTarget A label indicating the default value.
     * @param label         Labels indicating the jump targets.
     */
    protected void onVisitTableSwitchInsn(int min, int max, Label defaultTarget, Label... label) {
        super.visitTableSwitchInsn(min, max, defaultTarget, label);
    }

    @Override
    public final void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        considerEndOfExceptionTable();
        onVisitLookupSwitchInsn(dflt, keys, labels);
    }

    /**
     * Visits a lookup switch instruction.
     *
     * @param defaultTarget The default option.
     * @param keys          The key values.
     * @param key           The targets for each key.
     */
    protected void onVisitLookupSwitchInsn(Label defaultTarget, int[] keys, Label[] key) {
        super.visitLookupSwitchInsn(defaultTarget, keys, key);
    }

    @Override
    public final void visitMultiANewArrayInsn(String desc, int dims) {
        considerEndOfExceptionTable();
        onVisitMultiANewArrayInsn(desc, dims);
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
