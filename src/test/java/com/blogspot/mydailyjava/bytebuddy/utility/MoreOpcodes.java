package com.blogspot.mydailyjava.bytebuddy.utility;

/**
 * A collection targeting opcodes that are not contained within {@link org.objectweb.asm.Opcodes}.
 */
public final class MoreOpcodes {

    public static final int ILOAD_0 = 26;
    public static final int ILOAD_1 = 27;
    public static final int ILOAD_2 = 28;
    public static final int ILOAD_3 = 29;

    public static final int LLOAD_0 = 30;
    public static final int LLOAD_1 = 31;
    public static final int LLOAD_2 = 32;
    public static final int LLOAD_3 = 33;

    public static final int FLOAD_0 = 34;
    public static final int FLOAD_1 = 35;
    public static final int FLOAD_2 = 36;
    public static final int FLOAD_3 = 37;

    public static final int DLOAD_0 = 38;
    public static final int DLOAD_1 = 39;
    public static final int DLOAD_2 = 40;
    public static final int DLOAD_3 = 41;

    public static final int ALOAD_0 = 42;
    public static final int ALOAD_1 = 43;
    public static final int ALOAD_2 = 44;
    public static final int ALOAD_3 = 45;

    private MoreOpcodes() {
        throw new AssertionError();
    }
}
