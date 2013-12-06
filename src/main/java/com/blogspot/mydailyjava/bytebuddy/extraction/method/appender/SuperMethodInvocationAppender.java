package com.blogspot.mydailyjava.bytebuddy.extraction.method.appender;

import com.blogspot.mydailyjava.bytebuddy.extraction.context.ClassContext;
import com.blogspot.mydailyjava.bytebuddy.extraction.context.MethodContext;
import com.blogspot.mydailyjava.bytebuddy.utility.MethodDescriptorIterator;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class SuperMethodInvocationAppender implements ByteCodeAppender {
    
    private static class ArgumentOnOperandStackLoader implements MethodDescriptorIterator.Visitor {
        
        private final MethodVisitor methodVisitor;
        
        public ArgumentOnOperandStackLoader(MethodVisitor methodVisitor) {
             this.methodVisitor = methodVisitor;
         }
     
         @Override
         public void visitObject(String descriptor, int localVariableIndex) {
             methodVisitor.visitVarInsn(Opcodes.ALOAD, localVariableIndex + 1);
         }
     
         @Override
         public void visitArray(String descriptor, int localVariableIndex) {
             methodVisitor.visitVarInsn(Opcodes.AALOAD, localVariableIndex+ 1);
         }
     
         @Override
         public void visitDouble(int localVariableIndex) {
             methodVisitor.visitVarInsn(Opcodes.DLOAD, localVariableIndex+ 1);
         }
     
         @Override
         public void visitFloat(int localVariableIndex) {
             methodVisitor.visitVarInsn(Opcodes.FLOAD, localVariableIndex+ 1);
         }
     
         @Override
         public void visitLong(int localVariableIndex) {
             methodVisitor.visitVarInsn(Opcodes.LLOAD, localVariableIndex+ 1);
         }
     
         @Override
         public void visitInt(int localVariableIndex) {
             methodVisitor.visitVarInsn(Opcodes.ILOAD, localVariableIndex+ 1);
         }
     
         @Override
         public void visitChar(int localVariableIndex) {
             methodVisitor.visitVarInsn(Opcodes.ILOAD, localVariableIndex+ 1);
         }
     
         @Override
         public void visitShort(int localVariableIndex) {
             methodVisitor.visitVarInsn(Opcodes.ILOAD, localVariableIndex+ 1);
         }
     
         @Override
         public void visitByte(int localVariableIndex) {
             methodVisitor.visitVarInsn(Opcodes.ILOAD, localVariableIndex+ 1);
         }
     
         @Override
         public void visitBoolean(int localVariableIndex) {
             methodVisitor.visitVarInsn(Opcodes.ILOAD, localVariableIndex+ 1);
         }
        
    }

    @Override
    public Size apply(MethodVisitor methodVisitor, ClassContext classContext, MethodContext methodContext) {
        methodVisitor.visitIntInsn(Opcodes.ALOAD, 0);
        new MethodDescriptorIterator(methodContext.getDescriptor()).apply(new ArgumentOnOperandStackLoader(methodVisitor));
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, classContext.getSuperName(), methodContext.getName(), methodContext.getDescriptor());
        methodVisitor.visitInsn(methodContext.getType().getReturnType().getOpcode(Opcodes.ARETURN));
        return new Size(methodContext.getType().getArgumentsAndReturnSizes(), 0);
    }
}
