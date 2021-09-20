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

import java.util.*;

/**
 * A method visitor that introduces padding using a {@link Opcodes#NOP} instruction if two frames a visited consecutively.
 */
public class FramePaddingMethodVisitor extends MethodVisitor {

    /**
     * {@code true} if a visited frame requires padding.
     */
    private boolean padding;

    private final List<Label> padded;

    private final Map<Label, Label> mapped;

    /**
     * Creates a new frame padding method visitor.
     *
     * @param methodVisitor The delegate method visitor.
     */
    public FramePaddingMethodVisitor(MethodVisitor methodVisitor) {
        super(OpenedClassReader.ASM_API, methodVisitor);
        padding = false;
        padded = new ArrayList<Label>();
        mapped = new HashMap<Label, Label>();
    }

    @Override
    public void visitLabel(Label label) {
        if (padding) {
            padded.add(label);
        }
        super.visitLabel(label);
    }

    @Override
    public void visitFrame(int type, int localVariableLength, Object[] localVariable, int stackSize, Object[] stack) {
        if (padding) {
            super.visitInsn(Opcodes.NOP);
        } else {
            padding = true;
        }
        if (!mapped.isEmpty()) {
            patch(localVariable, localVariableLength);
            patch(stack, stackSize);
        }
        super.visitFrame(type, localVariableLength, localVariable, stackSize, stack);
    }

    private void patch(Object[] target, int length) {
        if (target != null) {
            for (int index = 0; index < length; index++) {
                if (target[index] instanceof Label) {
                    Label label = mapped.get((Label) target[index]);
                    if (label != null) {
                        target[index] = label;
                    }
                }
            }
        }
    }

    @Override
    public void visitInsn(int opcode) {
        resetPadding();
        super.visitInsn(opcode);
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        resetPadding();
        super.visitIntInsn(opcode, operand);
    }

    @Override
    public void visitVarInsn(int opcode, int offset) {
        resetPadding();
        super.visitVarInsn(opcode, offset);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        resetPadding();
        super.visitTypeInsn(opcode, type);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        resetPadding();
        super.visitFieldInsn(opcode, owner, name, descriptor);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor) {
        resetPadding();
        super.visitMethodInsn(opcode, owner, name, descriptor);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        resetPadding();
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor, Handle handle, Object... argument) {
        resetPadding();
        super.visitInvokeDynamicInsn(name, descriptor, handle, argument);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        resetPadding();
        super.visitJumpInsn(opcode, label);
    }

    @Override
    public void visitLdcInsn(Object value) {
        resetPadding();
        super.visitLdcInsn(value);
    }

    @Override
    public void visitIincInsn(int offset, int increment) {
        resetPadding();
        super.visitIincInsn(offset, increment);
    }

    @Override
    public void visitTableSwitchInsn(int minimum, int maximum, Label defaultLabel, Label... label) {
        resetPadding();
        super.visitTableSwitchInsn(minimum, maximum, defaultLabel, label);
    }

    @Override
    public void visitLookupSwitchInsn(Label defaultLabel, int[] key, Label[] label) {
        resetPadding();
        super.visitLookupSwitchInsn(defaultLabel, key, label);
    }

    @Override
    public void visitMultiANewArrayInsn(String descriptor, int dimensions) {
        resetPadding();
        super.visitMultiANewArrayInsn(descriptor, dimensions);
    }

    private void resetPadding() {
        if (padding) {
            padding = false;
            for (Label label : padded) {
                Label target = new Label();
                super.visitLabel(target);
                mapped.put(label, target);
            }
            padded.clear();
        }
    }
}
