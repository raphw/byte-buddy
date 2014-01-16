package com.blogspot.mydailyjava.bytebuddy;

public class ClassVersion {

    private final int versionNumber;

    public ClassVersion(int versionNumber) {
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
