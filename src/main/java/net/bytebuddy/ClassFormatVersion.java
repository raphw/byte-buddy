package net.bytebuddy;

import org.objectweb.asm.Opcodes;

/**
 * A wrapper object for representing a validated class format version as specified by the
 * <a href="http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html">JVMS</a>.
 */
public class ClassFormatVersion {

    private static final String JAVA_VERSION_PROPERTY = "java.version";

    /**
     * The byte code version of Java 1.
     */
    public static final ClassFormatVersion JAVA_V1 = new ClassFormatVersion(Opcodes.V1_1);

    /**
     * The byte code version of Java 2.
     */
    public static final ClassFormatVersion JAVA_V2 = new ClassFormatVersion(Opcodes.V1_2);

    /**
     * The byte code version of Java 3.
     */
    public static final ClassFormatVersion JAVA_V3 = new ClassFormatVersion(Opcodes.V1_3);

    /**
     * The byte code version of Java 4.
     */
    public static final ClassFormatVersion JAVA_V4 = new ClassFormatVersion(Opcodes.V1_4);

    /**
     * The byte code version of Java 5.
     */
    public static final ClassFormatVersion JAVA_V5 = new ClassFormatVersion(Opcodes.V1_5);

    /**
     * The byte code version of Java 6.
     */
    public static final ClassFormatVersion JAVA_V6 = new ClassFormatVersion(Opcodes.V1_6);

    /**
     * The byte code version of Java 7.
     */
    public static final ClassFormatVersion JAVA_V7 = new ClassFormatVersion(Opcodes.V1_7);

    /**
     * The byte code version of Java 8.
     */
    public static final ClassFormatVersion JAVA_V8 = new ClassFormatVersion(Opcodes.V1_7 + 1);

    /**
     * Creates a ClassFormatVersion for a given major release of Java. Currently, all versions reaching from
     * Java 1 to Java 8 are supported.
     *
     * @param javaVersion The Java version.
     * @return A wrapper for the given Java version.
     */
    public static ClassFormatVersion forKnownJavaVersion(int javaVersion) {
        switch (javaVersion) {
            case 1:
                return JAVA_V1;
            case 2:
                return JAVA_V2;
            case 3:
                return JAVA_V3;
            case 4:
                return JAVA_V4;
            case 5:
                return JAVA_V5;
            case 6:
                return JAVA_V6;
            case 7:
                return JAVA_V7;
            case 8:
                return JAVA_V8;
            default:
                throw new IllegalArgumentException("Unknown Java version: " + javaVersion);
        }
    }

    private final int versionNumber;

    /**
     * Finds the highest class format version compatible to the current JVM version by parsing the java.version property
     * provided by {@link java.lang.System#getProperty(String)}.
     *
     * @return The currently running Java process's byte code format version.
     */
    public static ClassFormatVersion forCurrentJavaVersion() {
        String versionString = System.getProperty(JAVA_VERSION_PROPERTY);
        int[] versionIndex = {-1, 0, 0};
        for (int i = 1; i < 3; i++) {
            versionIndex[i] = versionString.indexOf('.', versionIndex[i - 1] + 1);
            if (versionIndex[i] == -1) {
                throw new IllegalStateException("This JVM's version string does not seem to be valid: " + versionString);
            }
        }
        return ClassFormatVersion.forKnownJavaVersion(Integer.parseInt(versionString.substring(versionIndex[1] + 1, versionIndex[2])));
    }

    /**
     * Creates a wrapper for a given minor-major release of the Java class file format and validates the
     * integrity of the version number.
     *
     * @param versionNumber The minor-major release number.
     */
    public ClassFormatVersion(int versionNumber) {
        this.versionNumber = validateVersionNumber(versionNumber);
    }

    /**
     * Returns the minor-major release number of this class format version.
     *
     * @return The minor-major release number of this class format version.
     */
    public int getVersionNumber() {
        return versionNumber;
    }

    private static int validateVersionNumber(int versionNumber) {
        if (!(versionNumber > 0)) {
            throw new IllegalArgumentException("Class version " + versionNumber + " is not valid");
        }
        return versionNumber;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && versionNumber == ((ClassFormatVersion) other).versionNumber;
    }

    @Override
    public int hashCode() {
        return versionNumber;
    }

    @Override
    public String toString() {
        return "ClassFormatVersion{versionNumber=" + versionNumber + '}';
    }
}
