package net.bytebuddy.utility.visitor;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * A method visitor that maps the first available line number information, if available, to the beginning of the method.
 */
public class LineNumberPrependingMethodVisitor extends ExceptionTableSensitiveMethodVisitor {

    /**
     * A label indicating the start of the method.
     */
    private final Label startOfMethod;

    /**
     * {@code true} if the first line number was not yet discovered.
     */
    private boolean prependLineNumber;

    /**
     * Creates a new line number prepending method visitor.
     *
     * @param methodVisitor The method visitor to delegate to.
     */
    public LineNumberPrependingMethodVisitor(MethodVisitor methodVisitor) {
        super(Opcodes.ASM6, methodVisitor);
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
}
