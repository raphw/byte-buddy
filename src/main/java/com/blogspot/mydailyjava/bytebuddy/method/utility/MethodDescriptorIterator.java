package com.blogspot.mydailyjava.bytebuddy.method.utility;

public class MethodDescriptorIterator {

    private static final char SEPARATOR_TYPE = ';', METHOD_ARGUMENT_END = ')';

    public static interface Visitor {

        void visitObject(String descriptor, int localVariableIndex);

        void visitArray(String descriptor, int localVariableIndex);

        void visitDouble(int localVariableIndex);

        void visitFloat(int localVariableIndex);

        void visitLong(int localVariableIndex);

        void visitInt(int localVariableIndex);

        void visitChar(int localVariableIndex);

        void visitShort(int localVariableIndex);

        void visitByte(int localVariableIndex);

        void visitBoolean(int localVariableIndex);
    }

    private final String argumentTypesInternalForm;

    public MethodDescriptorIterator(String methodDescriptor) {
        this.argumentTypesInternalForm = methodDescriptor.substring(1, methodDescriptor.lastIndexOf(METHOD_ARGUMENT_END));
    }

    public <T extends Visitor> T apply(T visitor) {
        int argumentIndex = 0, cursor = 0;
        while (cursor != argumentTypesInternalForm.length()) {
            switch (argumentTypesInternalForm.charAt(cursor)) {
                case TypeSymbol.REFERENCE: {
                    int nextCursor = endOfObject(argumentTypesInternalForm, argumentIndex);
                    visitor.visitObject(argumentTypesInternalForm.substring(cursor, nextCursor), argumentIndex++);
                    cursor = nextCursor;
                    break;
                }
                case TypeSymbol.ARRAY: {
                    int nextCursor = endOfArray(argumentTypesInternalForm, argumentIndex);
                    visitor.visitArray(argumentTypesInternalForm.substring(cursor, nextCursor), argumentIndex++);
                    cursor = nextCursor;
                    break;
                }
                case TypeSymbol.DOUBLE:
                    visitor.visitDouble(argumentIndex += 2);
                    cursor++;
                    break;
                case TypeSymbol.LONG:
                    visitor.visitLong(argumentIndex += 2);
                    cursor++;
                    break;
                case TypeSymbol.FLOAT:
                    visitor.visitFloat(argumentIndex++);
                    cursor++;
                    break;
                case TypeSymbol.INT:
                    visitor.visitInt(argumentIndex++);
                    cursor++;
                    break;
                case TypeSymbol.BOOLEAN:
                    visitor.visitBoolean(argumentIndex++);
                    cursor++;
                    break;
                case TypeSymbol.BYTE:
                    visitor.visitByte(argumentIndex++);
                    cursor++;
                    break;
                case TypeSymbol.CHAR:
                    visitor.visitChar(argumentIndex++);
                    cursor++;
                    break;
                case TypeSymbol.SHORT:
                    visitor.visitShort(argumentIndex++);
                    cursor++;
                    break;
                default:
                    throw new AssertionError();
            }
        }
        return visitor;
    }

    private static int endOfObject(String value, int index) {
        return value.indexOf(SEPARATOR_TYPE, index + 1) + 1;
    }

    private static int endOfArray(String value, int index) {
        switch (value.charAt(index + 1)) {
            case TypeSymbol.REFERENCE:
                return endOfObject(value, index + 1);
            case TypeSymbol.ARRAY:
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
}
