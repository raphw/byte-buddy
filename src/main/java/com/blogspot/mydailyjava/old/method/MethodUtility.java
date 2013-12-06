package com.blogspot.mydailyjava.old.method;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class MethodUtility {

    public static final String CONSTRUCTOR_METHOD_NAME = "<init>";

    private static final char OBJECT = 'L', ARRAY = '[',
            INT = 'I', BOOLEAN = 'Z', CHAR = 'C', BYTE = 'B', SHORT = 'S',
            LONG = 'J', DOUBLE = 'D', FLOAT = 'F';

    private static final char SEPARATOR_TYPE = ';', SEPARATOR_NAME = '/';

    public static int loadArgumentsOnStack(MethodVisitor methodVisitor, String desc) {
        desc = desc.substring(1, desc.length() - 2);
        int variableIndex = 1, descPointer = 0;
        while (descPointer != desc.length()) {
            switch (desc.charAt(descPointer)) {
                case OBJECT:
                    methodVisitor.visitVarInsn(Opcodes.ALOAD, variableIndex++);
                    descPointer = endOfObject(desc, descPointer);
                    break;
                case ARRAY:
                    methodVisitor.visitVarInsn(Opcodes.ALOAD, variableIndex++);
                    descPointer = endOfArray(desc, descPointer);
                    break;
                case DOUBLE:
                    methodVisitor.visitVarInsn(Opcodes.DLOAD, variableIndex);
                    variableIndex += 2;
                    descPointer++;
                    break;
                case LONG:
                    methodVisitor.visitVarInsn(Opcodes.LLOAD, variableIndex);
                    variableIndex += 2;
                    descPointer++;
                    break;
                case FLOAT:
                    methodVisitor.visitVarInsn(Opcodes.FLOAD, variableIndex++);
                    descPointer++;
                    break;
                /*
                case INT:
                case BOOLEAN:
                case CHAR:
                case SHORT:
                case BYTE:
                */
                default:
                    methodVisitor.visitVarInsn(Opcodes.ILOAD, variableIndex++);
                    descPointer++;
            }
        }
        return variableIndex - 1;
    }

    public static int loadThisAndArgumentsOnStack(MethodVisitor methodVisitor, String desc) {
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        return loadArgumentsOnStack(methodVisitor, desc) + 1;
    }

    private static int endOfObject(String value, int index) {
        return value.indexOf(SEPARATOR_TYPE, index + 1) + 1;
    }

    private static int endOfArray(String value, int index) {
        switch (value.charAt(index + 1)) {
            case OBJECT:
                return endOfObject(value, index + 1);
            case ARRAY:
                return endOfArray(value, index + 1);
            /*
            case FLOAT:
            case LONG:
            case DOUBLE:
            case INT:
            case BOOLEAN:
            case CHAR:
            case SHORT:
            case BYTE:
            */
            default:
                return index + 1;
        }
    }

    public static boolean isOverridable(int access) {
        return access != Opcodes.ACC_PRIVATE;
    }

    private MethodUtility() {
        throw new AssertionError();
    }
}
