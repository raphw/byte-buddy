package com.blogspot.mydailyjava.bytebuddy.asm.method;

public class MethodReturnType {

    private static final char VOID_TYPE = 'V', ARGUMENTS_DELIMITER = ')';

    public final String returnTypeName;

    public MethodReturnType(String desc) {
        this.returnTypeName = desc.substring(desc.lastIndexOf(ARGUMENTS_DELIMITER));
    }

    public String getReturnTypeName() {
        return returnTypeName;
    }

    public boolean isReturnValue() {
        return returnTypeName.length() == 1 && returnTypeName.charAt(0) == VOID_TYPE;
    }
}
