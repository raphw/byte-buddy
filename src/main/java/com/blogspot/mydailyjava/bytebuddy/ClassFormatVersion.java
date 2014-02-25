package com.blogspot.mydailyjava.bytebuddy;

import org.objectweb.asm.Opcodes;

public class ClassFormatVersion {

    public static ClassFormatVersion forJavaVersion(int javaVersion) {
        switch (javaVersion) {
            case 1:
                return new ClassFormatVersion(Opcodes.V1_1);
            case 2:
                return new ClassFormatVersion(Opcodes.V1_2);
            case 3:
                return new ClassFormatVersion(Opcodes.V1_3);
            case 4:
                return new ClassFormatVersion(Opcodes.V1_4);
            case 5:
                return new ClassFormatVersion(Opcodes.V1_5);
            case 6:
                return new ClassFormatVersion(Opcodes.V1_6);
            case 7:
                return new ClassFormatVersion(Opcodes.V1_7);
            case 8:
                return new ClassFormatVersion(Opcodes.V1_7 + 1);
            default:
                throw new IllegalArgumentException("Unknown Java version: " + javaVersion);
        }
    }

    private final int versionNumber;

    public ClassFormatVersion(int versionNumber) {
        this.versionNumber = validateVersionNumber(versionNumber);
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    private static int validateVersionNumber(int versionNumber) {
        if (!(versionNumber > 0)) {
            throw new IllegalArgumentException("Class version " + versionNumber + " is not valid");
        }
        return versionNumber;
    }
}
