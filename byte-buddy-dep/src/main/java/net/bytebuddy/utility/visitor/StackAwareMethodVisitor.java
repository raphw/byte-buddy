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
import net.bytebuddy.build.AccessControllerPlugin;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.utility.CompoundList;
import net.bytebuddy.utility.OpenedClassReader;
import net.bytebuddy.utility.nullability.MaybeNull;
import net.bytebuddy.utility.privilege.GetSystemPropertyAction;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * <p>
 * A method visitor that is aware of the current size of the operand stack at all times. Additionally, this method takes
 * care of maintaining an index for the next currently unused index of the local variable array.
 * </p>
 * <p>
 * <b>Important</b>: It is not always possible to apply this method visitor if it is applied to a class file compiled
 * for Java 5 or earlier, or if frames are computed by ASM and not passed to this visitor, if a method also contains
 * {@link Opcodes#GOTO} instructions. In the latter case, the stack is assumed empty after the instruction. If this
 * is a problem, stack adjustment can be disabled by setting {@link StackAwareMethodVisitor#UNADJUSTED_PROPERTY} to
 * {@code true}. With this setting, Byte Buddy does no longer attempt draining non-empty stacks and skips this visitor
 * in all cases. This might however lead to verification problems if stacks are left non-empty. As the latter happens
 * more common and since this visitor is applied defensively, using this wrapper is considered the more sensible default.
 * </p>
 */
public class StackAwareMethodVisitor extends MethodVisitor {

    /**
     * A property to disable stack adjustment. Stack adjustment is typically needed when instrumenting other
     * generated code that leaves excess values on the stack. This is also often the case when byte code
     * obfuscation is used.
     */
    public static final String UNADJUSTED_PROPERTY = "net.bytebuddy.unadjusted";

    /**
     * {@code true} if stack adjustment is disabled.
     */
    public static final boolean UNADJUSTED;

    /*
     * Reads the raw type property.
     */
    static {
        boolean disabled;
        try {
            disabled = Boolean.parseBoolean(doPrivileged(new GetSystemPropertyAction(UNADJUSTED_PROPERTY)));
        } catch (Exception ignored) {
            disabled = false;
        }
        UNADJUSTED = disabled;
    }

    /**
     * An array mapping any opcode to its size impact onto the operand stack. This mapping is taken from
     * {@link org.objectweb.asm.Frame} with the difference that the {@link Opcodes#JSR} instruction is
     * mapped to a size of {@code 0} as it does not impact the stack after returning from the instruction.
     */
    private static final int[] SIZE_CHANGE;

    /*
     * Computes a mapping of byte codes to their size impact onto the operand stack.
     */
    static {
        SIZE_CHANGE = new int[202];
        String encoded = "EFFFFFFFFGGFFFGGFFFEEFGFGFEEEEEEEEEEEEEEEEEEEEDEDEDDDDDCD" +
                "CDEEEEEEEEEEEEEEEEEEEEBABABBBBDCFFFGGGEDCDCDCDCDCDCDCDCDCDCEEEEDDD" +
                "DDDDCDCDCEFEFDDEEFFDEDEEEBDDBBDDDDDDCCCCCCCCEEEDDDCDCDEEEEEEEEEEFE" +
                "EEEEEDDEEDDEE";
        for (int index = 0; index < SIZE_CHANGE.length; index++) {
            SIZE_CHANGE[index] = encoded.charAt(index) - 'E';
        }
    }

    /**
     * A list of the current elements on the operand stack.
     */
    private List<StackSize> current;

    /**
     * A mapping of labels to the operand stack size that is expected at this label. Lists stored in this
     * map must not be mutated.
     */
    private final Map<Label, List<StackSize>> sizes;

    /**
     * The next index of the local variable array that is available.
     */
    private int freeIndex;

    /**
     * Creates a new stack aware method visitor.
     *
     * @param methodVisitor      The method visitor to delegate operations to.
     * @param instrumentedMethod The method description for which this method visitor is applied.
     */
    protected StackAwareMethodVisitor(MethodVisitor methodVisitor, MethodDescription instrumentedMethod) {
        super(OpenedClassReader.ASM_API, methodVisitor);
        current = new ArrayList<StackSize>();
        sizes = new HashMap<Label, List<StackSize>>();
        freeIndex = instrumentedMethod.getStackSize();
    }

    /**
     * Wraps the provided method visitor within a stack aware method visitor.
     *
     * @param methodVisitor      The method visitor to delegate operations to.
     * @param instrumentedMethod The method description for which this method visitor is applied.
     * @return An appropriate
     */
    public static MethodVisitor of(MethodVisitor methodVisitor, MethodDescription instrumentedMethod) {
        return UNADJUSTED
                ? methodVisitor
                : new StackAwareMethodVisitor(methodVisitor, instrumentedMethod);
    }

    /**
     * A proxy for {@code java.security.AccessController#doPrivileged} that is activated if available.
     *
     * @param action The action to execute from a privileged context.
     * @param <T>    The type of the action's resolved value.
     * @return The action's resolved value.
     */
    @AccessControllerPlugin.Enhance
    private static <T> T doPrivileged(PrivilegedAction<T> action) {
        return action.run();
    }

    /**
     * Adjusts the current state of the operand stack.
     *
     * @param delta The change of the current operation of the operand stack. Must not be larger than {@code 2}.
     */
    private void adjustStack(int delta) {
        adjustStack(delta, 0);
    }

    /**
     * Adjusts the current state of the operand stack.
     *
     * @param delta  The change of the current operation of the operand stack. Must not be larger than {@code 2}.
     * @param offset The offset of the value within the operand stack. Must be bigger then {@code 0} and smaller than
     *               the current stack size. Only permitted if the supplied {@code delta} is positive.
     */
    private void adjustStack(int delta, int offset) {
        if (delta > 2) {
            throw new IllegalStateException("Cannot push multiple values onto the operand stack: " + delta);
        } else if (delta > 0) {
            int position = current.size();
            // The operand stack can legally underflow while traversing dead code.
            while (offset > 0 && position > 0) {
                offset -= current.get(--position).getSize();
            }
            if (offset < 0) {
                throw new IllegalStateException("Unexpected offset underflow: " + offset);
            }
            current.add(position, StackSize.of(delta));
        } else if (offset != 0) {
            throw new IllegalStateException("Cannot specify non-zero offset " + offset + " for non-incrementing value: " + delta);
        } else {
            while (delta < 0) {
                // The operand stack can legally underflow while traversing dead code.
                if (current.isEmpty()) {
                    return;
                }
                delta += current.remove(current.size() - 1).getSize();
            }
            if (delta == 1) {
                current.add(StackSize.SINGLE);
            } else if (delta != 0) {
                throw new IllegalStateException("Unexpected remainder on the operand stack: " + delta);
            }
        }
    }

    /**
     * Pops all values currently on the stack.
     */
    public void drainStack() {
        doDrain(current);
    }

    /**
     * Drains the stack to only contain the top value. For this, the value on top of the stack is temporarily stored
     * in the local variable array until all values on the stack are popped off. Subsequently, the top value is pushed
     * back onto the operand stack.
     *
     * @param store The opcode used for storing the top value.
     * @param load  The opcode used for loading the top value.
     * @param size  The size of the value on top of the operand stack.
     * @return The minimal size of the local variable array that is required to perform the operation.
     */
    public int drainStack(int store, int load, StackSize size) {
        if (current.isEmpty()) {
            return 0;
        }
        int difference = current.get(current.size() - 1).getSize() - size.getSize();
        if (current.size() == 1 && difference == 0) {
            return 0;
        } else {
            super.visitVarInsn(store, freeIndex);
            if (difference == 1) {
                super.visitInsn(Opcodes.POP);
            } else if (difference != 0) {
                throw new IllegalStateException("Unexpected remainder on the operand stack: " + difference);
            }
            doDrain(current.subList(0, current.size() - 1));
            super.visitVarInsn(load, freeIndex);
            return freeIndex + size.getSize();
        }
    }

    /**
     * Drains all supplied elements of the operand stack.
     *
     * @param stackSizes The stack sizes of the elements to drain.
     */
    private void doDrain(List<StackSize> stackSizes) {
        ListIterator<StackSize> iterator = stackSizes.listIterator(stackSizes.size());
        while (iterator.hasPrevious()) {
            StackSize current = iterator.previous();
            switch (current) {
                case SINGLE:
                    super.visitInsn(Opcodes.POP);
                    break;
                case DOUBLE:
                    super.visitInsn(Opcodes.POP2);
                    break;
                default:
                    throw new IllegalStateException("Unexpected stack size: " + current);
            }
        }
    }

    /**
     * Explicitly registers a label to define a given stack state.
     *
     * @param label      The label to register a stack state for.
     * @param stackSizes The stack sizes to assume when reaching the supplied label.
     */
    public void register(Label label, List<StackSize> stackSizes) {
        sizes.put(label, stackSizes);
    }

    @Override
    public void visitInsn(int opcode) {
        switch (opcode) {
            case Opcodes.RETURN:
            case Opcodes.ARETURN:
            case Opcodes.IRETURN:
            case Opcodes.LRETURN:
            case Opcodes.FRETURN:
            case Opcodes.DRETURN:
            case Opcodes.ATHROW:
                current.clear();
                break;
            case Opcodes.DUP_X1:
            case Opcodes.DUP2_X1:
                adjustStack(SIZE_CHANGE[opcode], SIZE_CHANGE[opcode] + 1);
                break;
            case Opcodes.DUP_X2:
            case Opcodes.DUP2_X2:
                adjustStack(SIZE_CHANGE[opcode], SIZE_CHANGE[opcode] + 2);
                break;
            case Opcodes.D2I:
            case Opcodes.D2F:
            case Opcodes.L2F:
            case Opcodes.L2I:
                adjustStack(-2);
                adjustStack(1);
                break;
            case Opcodes.I2D:
            case Opcodes.I2L:
            case Opcodes.F2D:
            case Opcodes.F2L:
                adjustStack(-1);
                adjustStack(2);
                break;
            case Opcodes.LALOAD:
            case Opcodes.DALOAD:
                adjustStack(-2);
                adjustStack(+2);
                break;
            default:
                adjustStack(SIZE_CHANGE[opcode]);
        }
        super.visitInsn(opcode);
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        adjustStack(SIZE_CHANGE[opcode]);
        super.visitIntInsn(opcode, operand);
    }

    @Override
    @SuppressFBWarnings(value = "SF_SWITCH_NO_DEFAULT", justification = "No action required on default option.")
    public void visitVarInsn(int opcode, int variable) {
        switch (opcode) {
            case Opcodes.ASTORE:
            case Opcodes.ISTORE:
            case Opcodes.FSTORE:
                freeIndex = Math.max(freeIndex, variable + 1);
                break;
            case Opcodes.LSTORE:
            case Opcodes.DSTORE:
                freeIndex = Math.max(freeIndex, variable + 2);
                break;
            case Opcodes.RET:
                current.clear();
                break;
        }
        adjustStack(SIZE_CHANGE[opcode]);
        super.visitVarInsn(opcode, variable);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        adjustStack(SIZE_CHANGE[opcode]);
        super.visitTypeInsn(opcode, type);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        int baseline = Type.getType(descriptor).getSize();
        switch (opcode) {
            case Opcodes.GETFIELD:
                adjustStack(-1);
                adjustStack(baseline);
                break;
            case Opcodes.GETSTATIC:
                adjustStack(baseline);
                break;
            case Opcodes.PUTFIELD:
                adjustStack(-baseline - 1);
                break;
            case Opcodes.PUTSTATIC:
                adjustStack(-baseline);
                break;
            default:
                throw new IllegalStateException("Unexpected opcode: " + opcode);
        }
        super.visitFieldInsn(opcode, owner, name, descriptor);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        int baseline = Type.getArgumentsAndReturnSizes(descriptor);
        adjustStack(-(baseline >> 2) + (opcode == Opcodes.INVOKESTATIC ? 1 : 0));
        adjustStack(baseline & 0x03);
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrap, Object... bootstrapArguments) {
        int baseline = Type.getArgumentsAndReturnSizes(descriptor);
        adjustStack(-(baseline >> 2) + 1);
        adjustStack(baseline & 0x03);
        super.visitInvokeDynamicInsn(name, descriptor, bootstrap, bootstrapArguments);
    }

    @Override
    public void visitLdcInsn(Object value) {
        adjustStack((value instanceof Long || value instanceof Double) ? 2 : 1);
        super.visitLdcInsn(value);
    }

    @Override
    public void visitMultiANewArrayInsn(String descriptor, int dimension) {
        adjustStack(1 - dimension);
        super.visitMultiANewArrayInsn(descriptor, dimension);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        adjustStack(SIZE_CHANGE[opcode]);
        sizes.put(label, new ArrayList<StackSize>(opcode == Opcodes.JSR
                ? CompoundList.of(current, StackSize.SINGLE)
                : current));
        if (opcode == Opcodes.GOTO) {
            current.clear();
        }
        super.visitJumpInsn(opcode, label);
    }

    @Override
    public void visitLabel(Label label) {
        List<StackSize> current = sizes.get(label);
        if (current != null) {
            this.current = new ArrayList<StackSize>(current);
        }
        super.visitLabel(label);
    }

    @Override
    public void visitTableSwitchInsn(int minimum, int maximum, Label defaultOption, Label... option) {
        adjustStack(-1);
        List<StackSize> current = new ArrayList<StackSize>(this.current);
        sizes.put(defaultOption, current);
        for (Label label : option) {
            sizes.put(label, current);
        }
        super.visitTableSwitchInsn(minimum, maximum, defaultOption, option);
    }

    @Override
    public void visitLookupSwitchInsn(Label defaultOption, int[] key, Label[] option) {
        adjustStack(-1);
        List<StackSize> current = new ArrayList<StackSize>(this.current);
        sizes.put(defaultOption, current);
        for (Label label : option) {
            sizes.put(label, current);
        }
        super.visitLookupSwitchInsn(defaultOption, key, option);
    }

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, @MaybeNull String type) {
        sizes.put(handler, Collections.singletonList(StackSize.SINGLE));
        super.visitTryCatchBlock(start, end, handler, type);
    }

    @Override
    @SuppressFBWarnings(value = "RC_REF_COMPARISON_BAD_PRACTICE", justification = "ASM models frames by reference identity.")
    public void visitFrame(int type, int localVariableLength, @MaybeNull Object[] localVariable, int stackSize, @MaybeNull Object[] stack) {
        switch (type) {
            case Opcodes.F_SAME:
            case Opcodes.F_CHOP:
            case Opcodes.F_APPEND:
                current.clear();
                break;
            case Opcodes.F_SAME1:
                current.clear();
                if (stack[0] == Opcodes.LONG || stack[0] == Opcodes.DOUBLE) {
                    current.add(StackSize.DOUBLE);
                } else {
                    current.add(StackSize.SINGLE);
                }
                break;
            case Opcodes.F_NEW:
            case Opcodes.F_FULL:
                current.clear();
                for (int index = 0; index < stackSize; index++) {
                    if (stack[index] == Opcodes.LONG || stack[index] == Opcodes.DOUBLE) {
                        current.add(StackSize.DOUBLE);
                    } else {
                        current.add(StackSize.SINGLE);
                    }
                }
                break;
            default:
                throw new IllegalStateException("Unknown frame type: " + type);
        }
        super.visitFrame(type, localVariableLength, localVariable, stackSize, stack);
    }
}
