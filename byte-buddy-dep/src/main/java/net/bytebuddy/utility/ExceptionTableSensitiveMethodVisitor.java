package net.bytebuddy.utility;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public abstract class ExceptionTableSensitiveMethodVisitor extends MethodVisitor {

    private boolean trigger;

    protected ExceptionTableSensitiveMethodVisitor(int api, MethodVisitor methodVisitor) {
        super(api, methodVisitor);
        trigger = true;
    }

    private void considerEndOfExceptionTable() {
        if (trigger) {
            trigger = false;
            onFirstCodeInstruction();
        }
    }

    protected abstract void onFirstCodeInstruction();

    @Override
    public final void visitIntInsn(int opcode, int operand) {
        considerEndOfExceptionTable();
        onVisitIntInsn(opcode, operand);
    }

    protected void onVisitIntInsn(int opcode, int operand) {
        super.visitIntInsn(opcode, operand);
    }

    @Override
    public final void visitVarInsn(int opcode, int var) {
        considerEndOfExceptionTable();
        onVisitVarInsn(opcode, var);
    }

    protected void onVisitVarInsn(int opcode, int var) {
        super.visitVarInsn(opcode, var);
    }

    @Override
    public final void visitTypeInsn(int opcode, String type) {
        considerEndOfExceptionTable();
        onVisitTypeInsn(opcode, type);
    }

    protected void onVisitTypeInsn(int opcode, String type) {
        super.visitTypeInsn(opcode, type);
    }

    @Override
    public final void visitFieldInsn(int opcode, String owner, String name, String desc) {
        considerEndOfExceptionTable();
        onVisitFieldInsn(opcode, owner, name, desc);
    }

    protected void onVisitFieldInsn(int opcode, String owner, String name, String desc) {
        super.visitFieldInsn(opcode, owner, name, desc);
    }

    @Override
    @SuppressWarnings("deprecation")
    public final void visitMethodInsn(int opcode, String owner, String name, String desc) {
        considerEndOfExceptionTable();
        onVisitMethodInsn(opcode, owner, name, desc);
    }

    @Deprecated
    @SuppressWarnings("deprecation")
    protected void onVisitMethodInsn(int opcode, String owner, String name, String desc) {
        considerEndOfExceptionTable();
        super.visitMethodInsn(opcode, owner, name, desc);
    }

    @Override
    public final void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        considerEndOfExceptionTable();
        onVisitMethodInsn(opcode, owner, name, desc, itf);
    }

    protected void onVisitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        super.visitMethodInsn(opcode, owner, name, desc, itf);
    }

    @Override
    public final void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
        considerEndOfExceptionTable();
        onVisitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
    }

    protected void onVisitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
        super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
    }

    @Override
    public final void visitJumpInsn(int opcode, Label label) {
        considerEndOfExceptionTable();
        onVisitJumpInsn(opcode, label);
    }

    protected void onVisitJumpInsn(int opcode, Label label) {
        super.visitJumpInsn(opcode, label);
    }

    @Override
    public final void visitLdcInsn(Object cst) {
        considerEndOfExceptionTable();
        onVisitLdcInsn(cst);
    }

    protected void onVisitLdcInsn(Object cst) {
        super.visitLdcInsn(cst);
    }

    @Override
    public final void visitIincInsn(int var, int increment) {
        considerEndOfExceptionTable();
        onVisitIincInsn(var, increment);
    }

    protected void onVisitIincInsn(int var, int increment) {
        super.visitIincInsn(var, increment);
    }

    @Override
    public final void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        considerEndOfExceptionTable();
        onVisitTableSwitchInsn(min, max, dflt, labels);
    }

    protected void onVisitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        super.visitTableSwitchInsn(min, max, dflt, labels);
    }

    @Override
    public final void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        considerEndOfExceptionTable();
        onVisitLookupSwitchInsn(dflt, keys, labels);
    }

    protected void onVisitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        super.visitLookupSwitchInsn(dflt, keys, labels);
    }

    @Override
    public final void visitMultiANewArrayInsn(String desc, int dims) {
        considerEndOfExceptionTable();
        onVisitMultiANewArrayInsn(desc, dims);
    }

    protected void onVisitMultiANewArrayInsn(String desc, int dims) {
        super.visitMultiANewArrayInsn(desc, dims);
    }

    @Override
    public final void visitInsn(int opcode) {
        considerEndOfExceptionTable();
        onVisitInsn(opcode);
    }

    protected void onVisitInsn(int opcode) {
        super.visitInsn(opcode);
    }
}
