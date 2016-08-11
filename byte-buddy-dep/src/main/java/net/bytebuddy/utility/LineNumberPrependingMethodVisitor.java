package net.bytebuddy.utility;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class LineNumberPrependingMethodVisitor extends ExceptionTableSensitiveMethodVisitor {

    private final Label startOfMethod;

    private boolean prependLineNumber;

    public LineNumberPrependingMethodVisitor(MethodVisitor methodVisitor) {
        super(Opcodes.ASM5, methodVisitor);
        startOfMethod = new Label();
        prependLineNumber = true;
    }

    @Override
    protected void onAfterExceptionTable() {
        super.visitLabel(startOfMethod);
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        if (prependLineNumber) {
            start = startOfMethod;
            prependLineNumber = false;
        }
        super.visitLineNumber(line, start);
    }

    @Override
    public String toString() {
        return "LineNumberPrependingMethodVisitor{" +
                "startOfMethod=" + startOfMethod +
                ", prependLineNumber=" + prependLineNumber +
                '}';
    }
}
