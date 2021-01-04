/*
 * Copyright 2014 - Present Rafael Winterhalter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bytebuddy.utility.visitor;

import net.bytebuddy.utility.OpenedClassReader;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * A method visitor that introduces padding using a {@link Opcodes#NOP} instruction if two frames a visited consecutively.
 */
public class FramePaddingMethodVisitor extends MethodVisitor {

    /**
     * {@code true} if a visited frame requires padding.
     */
    private boolean padding;

    /**
     * Creates a new frame padding method visitor.
     *
     * @param methodVisitor The delegate method visitor.
     */
    public FramePaddingMethodVisitor(MethodVisitor methodVisitor) {
        super(OpenedClassReader.ASM_API, methodVisitor);
        padding = false;
    }

    @Override
    public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
        if (padding) {
            super.visitInsn(Opcodes.NOP);
        } else {
            padding = true;
        }
        super.visitFrame(type, numLocal, local, numStack, stack);
    }

    @Override
    public void visitInsn(int opcode) {
        padding = false;
        super.visitInsn(opcode);
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        padding = false;
        super.visitIntInsn(opcode, operand);
    }

    @Override
    public void visitVarInsn(int opcode, int offset) {
        padding = false;
        super.visitVarInsn(opcode, offset);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        padding = false;
        super.visitTypeInsn(opcode, type);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        padding = false;
        super.visitFieldInsn(opcode, owner, name, descriptor);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor) {
        padding = false;
        super.visitMethodInsn(opcode, owner, name, descriptor);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        padding = false;
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor, Handle handle, Object... argument) {
        padding = false;
        super.visitInvokeDynamicInsn(name, descriptor, handle, argument);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        padding = false;
        super.visitJumpInsn(opcode, label);
    }

    @Override
    public void visitLdcInsn(Object value) {
        padding = false;
        super.visitLdcInsn(value);
    }

    @Override
    public void visitIincInsn(int offset, int increment) {
        padding = false;
        super.visitIincInsn(offset, increment);
    }



    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... label) {
        padding = false;
        super.visitTableSwitchInsn(min, max, dflt, label);
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] key, Label[] label) {
        padding = false;
        super.visitLookupSwitchInsn(dflt, key, label);
    }

    @Override
    public void visitMultiANewArrayInsn(String descriptor, int dimensions) {
        padding = false;
        super.visitMultiANewArrayInsn(descriptor, dimensions);
    }
}
