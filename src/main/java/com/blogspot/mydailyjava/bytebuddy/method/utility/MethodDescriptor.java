package com.blogspot.mydailyjava.bytebuddy.method.utility;

public class MethodDescriptor {

    public static final char OBJECT_REFERENCE_SYMBOL = 'L';
    public static final char ARRAY_REFERENCE_SYMBOL = '[';
    public static final char DOUBLE_SYMBOL = 'D';
    public static final char LONG_SYMBOL = 'J';
    public static final char BOOLEAN_SYMBOL = 'Z';
    public static final char BYTE_SYMBOL = 'B';
    public static final char SHORT_SYMBOL = 'S';
    public static final char CHAR_SYMBOL = 'C';
    public static final char INT_SYMBOL = 'I';
    public static final char FLOAT_SYMBOL = 'F';
    public static final char VOID_SYMBOL = 'V';

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

    public MethodDescriptor(String methodDescriptor) {
        this.argumentTypesInternalForm = methodDescriptor.substring(1, methodDescriptor.lastIndexOf(METHOD_ARGUMENT_END));
    }

    public <T extends Visitor> T apply(T visitor) {
        int argumentIndex = 0, cursor = 0;
        while (cursor != argumentTypesInternalForm.length()) {
            switch (argumentTypesInternalForm.charAt(cursor)) {
                case OBJECT_REFERENCE_SYMBOL: {
                    int nextCursor = endOfObject(argumentTypesInternalForm, argumentIndex);
                    visitor.visitObject(argumentTypesInternalForm.substring(cursor, nextCursor), argumentIndex++);
                    cursor = nextCursor;
                    break;
                }
                case ARRAY_REFERENCE_SYMBOL: {
                    int nextCursor = endOfArray(argumentTypesInternalForm, argumentIndex);
                    visitor.visitArray(argumentTypesInternalForm.substring(cursor, nextCursor), argumentIndex++);
                    cursor = nextCursor;
                    break;
                }
                case INT_SYMBOL:
                    visitor.visitInt(argumentIndex++);
                    cursor++;
                    break;
                case DOUBLE_SYMBOL:
                    visitor.visitDouble(argumentIndex += 2);
                    cursor++;
                    break;
                case LONG_SYMBOL:
                    visitor.visitLong(argumentIndex += 2);
                    cursor++;
                    break;
                case BOOLEAN_SYMBOL:
                    visitor.visitBoolean(argumentIndex++);
                    cursor++;
                    break;
                case BYTE_SYMBOL:
                    visitor.visitByte(argumentIndex++);
                    cursor++;
                    break;
                case CHAR_SYMBOL:
                    visitor.visitChar(argumentIndex++);
                    cursor++;
                    break;
                case FLOAT_SYMBOL:
                    visitor.visitFloat(argumentIndex++);
                    cursor++;
                    break;
                case SHORT_SYMBOL:
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
            case OBJECT_REFERENCE_SYMBOL:
                return endOfObject(value, index + 1);
            case ARRAY_REFERENCE_SYMBOL:
                return endOfArray(value, index + 1);
            /*
            case FLOAT_SYMBOL:
            case LONG_SYMBOL:
            case DOUBLE_SYMBOL:
            case INT_SYMBOL:
            case BOOLEAN_SYMBOL:
            case CHAR_SYMBOL:
            case SHORT_SYMBOL:
            case BYTE_SYMBOL:
            */
            default:
                return index + 1;
        }
    }
}
