package net.bytebuddy.utility;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.implementation.bytecode.StackSize;
import org.objectweb.asm.*;

import java.util.*;

public class StackAwareMethodVisitor extends MethodVisitor {

    private static final int[] SIZE;

    static {
        SIZE = new int[202];
        String encoded = "EFFFFFFFFGGFFFGGFFFEEFGFGFEEEEEEEEEEEEEEEEEEEEDEDEDDDDD"
                + "CDCDEEEEEEEEEEEEEEEEEEEEBABABBBBDCFFFGGGEDCDCDCDCDCDCDCDCD"
                + "CDCEEEEDDDDDDDCDCDCEFEFDDEEFFDEDEEEBDDBBDDDDDDCCCCCCCCEFED"
                + "DDCDCDEEEEEEEEEEFEEEEEEDDEEDDEE";
        for (int index = 0; index < SIZE.length; ++index) {
            SIZE[index] = encoded.charAt(index) - 'E';
        }
    }

    private List<StackSize> current;

    private final Map<Label, List<StackSize>> sizes;

    private int freeIndex;

    public StackAwareMethodVisitor(MethodVisitor methodVisitor, MethodDescription methodDescription) {
        super(Opcodes.ASM5, methodVisitor);
        current = new ArrayList<StackSize>();
        sizes = new HashMap<Label, List<StackSize>>();
        freeIndex = methodDescription.getStackSize();
    }

    private void adjustStack(int size) {
        if (size == 1) {
            current.add(StackSize.SINGLE);
        } else if (size == 2) {
            current.add(StackSize.DOUBLE);
        } else if (size > 0) {
            throw new IllegalStateException("Cannot push multiple values onto the operand stack: " + size);
        } else {
            while (size < 0) {
                size += current.remove(current.size() - 1).getSize();
            }
        }
    }

    public void drainStack() {
        if (!current.isEmpty()) {
            for (StackSize stackSize : current) {
                switch (stackSize) {
                    case SINGLE:
                        super.visitInsn(Opcodes.POP);
                        break;
                    case DOUBLE:
                        super.visitInsn(Opcodes.POP2);
                        break;
                    default:
                        throw new IllegalStateException("Unexpected stack size: " + stackSize);
                }
            }
        }
    }

    public void drainStack(int store, int load) {
        if (current.size() != 1) {
            super.visitVarInsn(store, freeIndex);
            for (StackSize stackSize : current.subList(0, current.size() - 1)) {
                switch (stackSize) {
                    case SINGLE:
                        super.visitInsn(Opcodes.POP);
                        break;
                    case DOUBLE:
                        super.visitInsn(Opcodes.POP2);
                        break;
                    default:
                        throw new IllegalStateException("Unexpected stack size: " + stackSize);
                }
            }
            super.visitVarInsn(load, freeIndex);
        }
    }

    @Override
    public void visitInsn(int opcode) {
        adjustStack(SIZE[opcode]);
        super.visitInsn(opcode);
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        adjustStack(SIZE[opcode]);
        super.visitIntInsn(opcode, operand);
    }

    @Override
    public void visitVarInsn(int opcode, int variable) {
        //TODO: RET
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
        }
        adjustStack(SIZE[opcode]);
        super.visitVarInsn(opcode, variable);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        adjustStack(SIZE[opcode]);
        super.visitTypeInsn(opcode, type);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        int baseline = (descriptor.charAt(0) == 'D' || descriptor.charAt(0) == 'J' ? 2 : 1);
        switch (opcode) {
            case Opcodes.GETFIELD:
                adjustStack(baseline - 1);
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
        adjustStack(-(baseline >> 2) + ((opcode == Opcodes.INVOKESTATIC) ? 1 : 0));
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
    public void visitIincInsn(int variable, int increment) {
        super.visitIincInsn(variable, increment); // TODO: Remove
    }

    @Override
    public void visitMultiANewArrayInsn(String descriptor, int dimension) {
        adjustStack(1 - dimension);
        super.visitMultiANewArrayInsn(descriptor, dimension);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        // TODO: JSR
        adjustStack(SIZE[opcode]);
        sizes.put(label, new ArrayList<StackSize>(current));
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
    public void visitTableSwitchInsn(int min, int max, Label defaultOption, Label... label) {
        adjustStack(-1);
        sizes.put(defaultOption, new ArrayList<StackSize>(current));
        for (Label aLabel : label) {
            sizes.put(aLabel, new ArrayList<StackSize>(current));
        }
        super.visitTableSwitchInsn(min, max, defaultOption, label);
    }

    @Override
    public void visitLookupSwitchInsn(Label defaultOption, int[] key, Label[] label) {
        adjustStack(-1);
        sizes.put(defaultOption, new ArrayList<StackSize>(current));
        for (Label aLabel : label) {
            sizes.put(aLabel, new ArrayList<StackSize>(current));
        }
        super.visitLookupSwitchInsn(defaultOption, key, label);
    }

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        sizes.put(handler, new ArrayList<StackSize>(Collections.singletonList(StackSize.SINGLE)));
        super.visitTryCatchBlock(start, end, handler, type);
    }
}
