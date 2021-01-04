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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.utility.OpenedClassReader;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * A method visitor that traces the amount of used local variable slots.
 */
public class LocalVariableAwareMethodVisitor extends MethodVisitor {

    /**
     * The first offset that was observed to not be used.
     */
    private int freeOffset;

    /**
     * Creates a local variable aware method visitor.
     *
     * @param methodVisitor     The method visitor to delegate to.
     * @param methodDescription The method being visited.
     */
    public LocalVariableAwareMethodVisitor(MethodVisitor methodVisitor, MethodDescription methodDescription) {
        super(OpenedClassReader.ASM_API, methodVisitor);
        freeOffset = methodDescription.getStackSize();
    }

    @Override
    @SuppressFBWarnings(value = "SF_SWITCH_NO_DEFAULT", justification = "No action required on default option.")
    public void visitVarInsn(int opcode, int offset) {
        switch (opcode) {
            case Opcodes.ISTORE:
            case Opcodes.FSTORE:
            case Opcodes.ASTORE:
                freeOffset = Math.max(freeOffset, offset + 1);
                break;
            case Opcodes.LSTORE:
            case Opcodes.DSTORE:
                freeOffset = Math.max(freeOffset, offset + 2);
                break;
        }
        super.visitVarInsn(opcode, offset);
    }

    /**
     * Returns the first offset that was observed to be free.
     *
     * @return The first offset that was observed to be free.
     */
    public int getFreeOffset() {
        return freeOffset;
    }
}
