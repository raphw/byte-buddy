package net.bytebuddy.utility;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

public abstract class ExceptionTableSensitiveMethodVisitor extends MethodVisitor {

    private boolean trigger;

    protected ExceptionTableSensitiveMethodVisitor(int api, MethodVisitor methodVisitor) {
        super(api, methodVisitor);
    }

    private void considerEndExceptionTable() {
        if (trigger) {
            trigger = false;
            onMethodStart();
        }
    }

    protected abstract void onMethodStart();

    @Override
    public final void visitIntInsn(int opcode, int operand) {
        considerEndExceptionTable();
        onVisitIntInsn(opcode, operand);
    }

    protected void onVisitIntInsn(int opcode, int operand) {
        super.visitIntInsn(opcode, operand);
    }

    @Override
    public final void visitVarInsn(int opcode, int var) {
        considerEndExceptionTable();
        onVisitVarInsn(opcode, var);
    }

    protected void onVisitVarInsn(int opcode, int var) {
        super.visitVarInsn(opcode, var);
    }

    @Override
    public final void visitTypeInsn(int opcode, String type) {
        considerEndExceptionTable();
        onVisitTypeInsn(opcode, type);
    }

    protected void onVisitTypeInsn(int opcode, String type) {
        super.visitTypeInsn(opcode, type);
    }

    @Override
    public final void visitFieldInsn(int opcode, String owner, String name, String desc) {
        considerEndExceptionTable();
        onVisitFieldInsn(opcode, owner, name, desc);
    }

    protected void onVisitFieldInsn(int opcode, String owner, String name, String desc) {
        super.visitFieldInsn(opcode, owner, name, desc);
    }

    @Override
    @SuppressWarnings("deprecation")
    public final void visitMethodInsn(int opcode, String owner, String name, String desc) {
        considerEndExceptionTable();
        onVisitMethodInsn(opcode, owner, name, desc);
    }

    @Deprecated
    @SuppressWarnings("deprecation")
    protected void onVisitMethodInsn(int opcode, String owner, String name, String desc) {
        considerEndExceptionTable();
        super.visitMethodInsn(opcode, owner, name, desc);
    }

    @Override
    public final void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        considerEndExceptionTable();
        onVisitMethodInsn(opcode, owner, name, desc, itf);
    }

    protected void onVisitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        super.visitMethodInsn(opcode, owner, name, desc, itf);
    }

    @Override
    public final void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
        considerEndExceptionTable();
        onVisitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
    }

    protected void onVisitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
        super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
    }

    @Override
    public final void visitJumpInsn(int opcode, Label label) {
        considerEndExceptionTable();
        onVisitJumpInsn(opcode, label);
    }

    protected void onVisitJumpInsn(int opcode, Label label) {
        super.visitJumpInsn(opcode, label);
    }

    @Override
    public final void visitLabel(Label label) {
        considerEndExceptionTable();
        onVisitLabel(label);
    }

    protected void onVisitLabel(Label label) {
        super.visitLabel(label);
    }

    @Override
    public final void visitLdcInsn(Object cst) {
        considerEndExceptionTable();
        onVisitLdcInsn(cst);
    }

    protected void onVisitLdcInsn(Object cst) {
        super.visitLdcInsn(cst);
    }

    @Override
    public final void visitIincInsn(int var, int increment) {
        considerEndExceptionTable();
        onVisitIincInsn(var, increment);
    }

    protected void onVisitIincInsn(int var, int increment) {
        super.visitIincInsn(var, increment);
    }

    @Override
    public final void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        considerEndExceptionTable();
        onVisitTableSwitchInsn(min, max, dflt, labels);
    }

    protected void onVisitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        super.visitTableSwitchInsn(min, max, dflt, labels);
    }

    @Override
    public final void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        considerEndExceptionTable();
        onVisitLookupSwitchInsn(dflt, keys, labels);
    }

    protected void onVisitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        super.visitLookupSwitchInsn(dflt, keys, labels);
    }

    @Override
    public final void visitMultiANewArrayInsn(String desc, int dims) {
        considerEndExceptionTable();
        onVisitMultiANewArrayInsn(desc, dims);
    }

    protected void onVisitMultiANewArrayInsn(String desc, int dims) {
        super.visitMultiANewArrayInsn(desc, dims);
    }

    @Override
    public final void visitInsn(int opcode) {
        considerEndExceptionTable();
        onVisitInsn(opcode);
    }

    protected void onVisitInsn(int opcode) {
        super.visitInsn(opcode);
    }
}
