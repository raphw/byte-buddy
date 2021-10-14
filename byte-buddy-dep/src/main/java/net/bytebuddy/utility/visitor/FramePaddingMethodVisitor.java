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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A method visitor that introduces padding using a {@link Opcodes#NOP} instruction if two frames a visited consecutively.
 */
public class FramePaddingMethodVisitor extends MethodVisitor {

    /**
     * {@code true} if a visited frame requires padding.
     */
    private boolean padding;

    /**
     * Indicates that an injected {@link Opcodes#NOP} instruction is the last current instruction.
     */
    private boolean injected;

    /**
     * A list of labels currently discovered since the last byte code instruction.
     */
    private final List<Label> labels;

    /**
     * A mapping of labels to their replacements if used in a stack map frame.
     */
    private final Map<Label, Label> mapped;

    /**
     * Creates a new frame padding method visitor.
     *
     * @param methodVisitor The delegate method visitor.
     */
    public FramePaddingMethodVisitor(MethodVisitor methodVisitor) {
        super(OpenedClassReader.ASM_API, methodVisitor);
        padding = false;
        injected = false;
        labels = new ArrayList<Label>();
        mapped = new HashMap<Label, Label>();
    }

    @Override
    public void visitLabel(Label label) {
        labels.add(label);
        super.visitLabel(label);
    }

    @Override
    public void visitFrame(int type, int localVariableLength, Object[] localVariable, int stackSize, Object[] stack) {
        if (padding) {
            if (type == Opcodes.F_SAME) { // fixme: test me
                return;
            }
            injected = true;
            super.visitInsn(Opcodes.NOP);
        } else {
            padding = true;
        }
        if (!mapped.isEmpty()) {
            patch(localVariableLength, localVariable);
            patch(stackSize, stack);
        }
        super.visitFrame(type, localVariableLength, localVariable, stackSize, stack);
    }

    @Override
    public void visitInsn(int opcode) {
        reset();
        super.visitInsn(opcode);
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        reset();
        super.visitIntInsn(opcode, operand);
    }

    @Override
    public void visitVarInsn(int opcode, int offset) {
        reset();
        super.visitVarInsn(opcode, offset);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        reset();
        super.visitTypeInsn(opcode, type);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        reset();
        super.visitFieldInsn(opcode, owner, name, descriptor);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor) {
        reset();
        super.visitMethodInsn(opcode, owner, name, descriptor);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        reset();
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor, Handle handle, Object... argument) {
        reset();
        super.visitInvokeDynamicInsn(name, descriptor, handle, argument);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        reset();
        super.visitJumpInsn(opcode, label);
    }

    @Override
    public void visitLdcInsn(Object value) {
        reset();
        super.visitLdcInsn(value);
    }

    @Override
    public void visitIincInsn(int offset, int increment) {
        reset();
        super.visitIincInsn(offset, increment);
    }

    @Override
    public void visitTableSwitchInsn(int minimum, int maximum, Label defaultLabel, Label... label) {
        reset();
        super.visitTableSwitchInsn(minimum, maximum, defaultLabel, label);
    }

    @Override
    public void visitLookupSwitchInsn(Label defaultLabel, int[] key, Label[] label) {
        reset();
        super.visitLookupSwitchInsn(defaultLabel, key, label);
    }

    @Override
    public void visitMultiANewArrayInsn(String descriptor, int dimensions) {
        reset();
        super.visitMultiANewArrayInsn(descriptor, dimensions);
    }

    /**
     * Patches an array of stack map frame data.
     *
     * @param length The length of the array.
     * @param target The array containing the data.
     */
    private void patch(int length, Object[] target) {
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

    /**
     * Resets a padding if padding is currently active and maps
     * labels to synthetic substitutes, if necessary.
     */
    private void reset() {
        if (padding) {
            if (injected) {
                for (Label label : labels) {
                    Label target = new Label();
                    super.visitLabel(target);
                    mapped.put(label, target);
                }
                injected = false;
            }
            padding = false;
        }
        labels.clear();
    }
}
