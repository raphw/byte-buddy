package com.blogspot.mydailyjava.bytebuddy.utility;

public class MethodDescriptorIterator {

    private static final char OBJECT = 'L', ARRAY = '[',
            INT = 'I', BOOLEAN = 'Z', CHAR = 'C', BYTE = 'B', SHORT = 'S', LONG = 'J',
            DOUBLE = 'D', FLOAT = 'F';

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
                case OBJECT: {
                    int nextCursor = endOfObject(argumentTypesInternalForm, argumentIndex);
                    visitor.visitObject(argumentTypesInternalForm.substring(cursor, nextCursor), argumentIndex++);
                    cursor = nextCursor;
                    break;
                }
                case ARRAY: {
                    int nextCursor = endOfArray(argumentTypesInternalForm, argumentIndex);
                    visitor.visitArray(argumentTypesInternalForm.substring(cursor, nextCursor), argumentIndex++);
                    cursor = nextCursor;
                    break;
                }
                case DOUBLE:
                    visitor.visitDouble(argumentIndex += 2);
                    cursor++;
                    break;
                case LONG:
                    visitor.visitLong(argumentIndex += 2);
                    cursor++;
                    break;
                case FLOAT:
                    visitor.visitFloat(argumentIndex++);
                    cursor++;
                    break;
                case INT:
                    visitor.visitInt(argumentIndex++);
                    cursor++;
                    break;
                case BOOLEAN:
                    visitor.visitBoolean(argumentIndex++);
                    cursor++;
                    break;
                case BYTE:
                    visitor.visitByte(argumentIndex++);
                    cursor++;
                    break;
                case CHAR:
                    visitor.visitChar(argumentIndex++);
                    cursor++;
                    break;
                case SHORT:
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
}
