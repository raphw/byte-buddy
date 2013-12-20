package com.blogspot.mydailyjava.bytebuddy.method.utility;

public final class TypeSymbol {

    public static final char REFERENCE = 'L';

    public static final char ARRAY = '[';

    public static final char BOOLEAN = 'Z';
    public static final char BYTE = 'B';
    public static final char SHORT = 'S';
    public static final char CHAR = 'C';
    public static final char INT = 'I';

    public static final char LONG = 'J';

    public static final char FLOAT = 'F';

    public static final char DOUBLE = 'D';

    private TypeSymbol() {
        throw new AssertionError();
    }
}
