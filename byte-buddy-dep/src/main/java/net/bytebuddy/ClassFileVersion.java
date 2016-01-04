package net.bytebuddy;

import org.objectweb.asm.Opcodes;

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * A wrapper object for representing a validated class file version in the format that is specified by the
 * <a href="http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html">JVMS</a>.
 */
public class ClassFileVersion implements Comparable<ClassFileVersion> {

    /**
     * Returns the minimal version number that is legal.
     */
    protected static final int BASE_VERSION = 44;

    /**
     * The class file version of Java 1.
     */
    public static final ClassFileVersion JAVA_V1 = new ClassFileVersion(Opcodes.V1_1);

    /**
     * The class file version of Java 2.
     */
    public static final ClassFileVersion JAVA_V2 = new ClassFileVersion(Opcodes.V1_2);

    /**
     * The class file version of Java 3.
     */
    public static final ClassFileVersion JAVA_V3 = new ClassFileVersion(Opcodes.V1_3);

    /**
     * The class file version of Java 4.
     */
    public static final ClassFileVersion JAVA_V4 = new ClassFileVersion(Opcodes.V1_4);

    /**
     * The class file version of Java 5.
     */
    public static final ClassFileVersion JAVA_V5 = new ClassFileVersion(Opcodes.V1_5);

    /**
     * The class file version of Java 6.
     */
    public static final ClassFileVersion JAVA_V6 = new ClassFileVersion(Opcodes.V1_6);

    /**
     * The class file version of Java 7.
     */
    public static final ClassFileVersion JAVA_V7 = new ClassFileVersion(Opcodes.V1_7);

    /**
     * The class file version of Java 8.
     */
    public static final ClassFileVersion JAVA_V8 = new ClassFileVersion(Opcodes.V1_8);

    /**
     * The class file version of Java 9.
     */
    public static final ClassFileVersion JAVA_V9 = JAVA_V8;

    /**
     * The system property for this JVM's Java version.
     */
    private static final String JAVA_VERSION_PROPERTY = "java.version";

    /**
     * The version number that is represented by this class file version instance.
     */
    private final int versionNumber;

    /**
     * Creates a wrapper for a given minor-major release of the Java class file file.
     *
     * @param versionNumber The minor-major release number.
     */
    protected ClassFileVersion(int versionNumber) {
        this.versionNumber = versionNumber;
    }

    /**
     * Creates a wrapper for a given minor-major release of the Java class file file.
     *
     * @param versionNumber The minor-major release number.
     * @return A representation of the version number.
     */
    public static ClassFileVersion ofMinorMajor(int versionNumber) {
        ClassFileVersion classFileVersion = new ClassFileVersion(versionNumber);
        if (classFileVersion.getMajorVersion() <= BASE_VERSION) {
            throw new IllegalArgumentException("Class version " + versionNumber + " is not valid");
        }
        return classFileVersion;
    }

    /**
     * Creates a class file version for a given major release of Java. Currently, all versions reaching from
     * Java 1 to Java 8 are supported.
     *
     * @param javaVersion The Java version.
     * @return A wrapper for the given Java class file version.
     */
    public static ClassFileVersion ofJavaVersion(int javaVersion) {
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
            case 9:
                return JAVA_V9;
            default:
                throw new IllegalArgumentException("Unknown Java version: " + javaVersion);
        }
    }

    /**
     * Finds the highest class file version that is compatible to the current JVM version by parsing the java.version
     * property which is provided by {@link java.lang.System#getProperty(String)}.
     *
     * @return The currently running Java process's class file version.
     */
    public static ClassFileVersion forCurrentJavaVersion() {
        String versionString = AccessController.doPrivileged(VersionPropertyAction.INSTANCE);
        int[] versionIndex = {-1, 0, 0};
        for (int i = 1; i < 3; i++) {
            versionIndex[i] = versionString.indexOf('.', versionIndex[i - 1] + 1);
            if (versionIndex[i] == -1) {
                throw new IllegalStateException("This JVM's version string does not seem to be valid: " + versionString);
            }
        }
        return ClassFileVersion.ofJavaVersion(Integer.parseInt(versionString.substring(versionIndex[1] + 1, versionIndex[2])));
    }

    /**
     * Returns the minor-major release number of this class file version.
     *
     * @return The minor-major release number of this class file version.
     */
    public int getMinorMajorVersion() {
        return versionNumber;
    }

    /**
     * Returns the major version this instance represents.
     *
     * @return The major version this instance represents.
     */
    public int getMajorVersion() {
        return versionNumber & 0xFF;
    }

    /**
     * Returns the minor version this instance represents.
     *
     * @return The minor version this instance represents.
     */
    public int getMinorVersion() {
        return versionNumber >> 16;
    }

    /**
     * Returns the Java runtime version number of this class file version.
     *
     * @return The Java runtime version.
     */
    public int getJavaVersion() {
        return getMajorVersion() - BASE_VERSION;
    }

    /**
     * Checks if this class file version is at least of the provided version.
     *
     * @param classFileVersion The version to check against.
     * @return {@code true} if this version is at least of the given version.
     */
    public boolean isAtLeast(ClassFileVersion classFileVersion) {
        return compareTo(classFileVersion) > -1;
    }

    @Override
    public int compareTo(ClassFileVersion other) {
        return Integer.signum(getMajorVersion() == other.getMajorVersion()
                ? getMinorVersion() - other.getMinorVersion()
                : getMajorVersion() - other.getMajorVersion());
    }

    @Override
    public boolean equals(Object other) {
        return this == other || !(other == null || getClass() != other.getClass())
                && versionNumber == ((ClassFileVersion) other).versionNumber;
    }

    @Override
    public int hashCode() {
        return versionNumber;
    }

    @Override
    public String toString() {
        return "ClassFileVersion{versionNumber=" + versionNumber + '}';
    }

    protected enum VersionPropertyAction implements PrivilegedAction<String> {

        INSTANCE;

        @Override
        public String run() {
            return System.getProperty(JAVA_VERSION_PROPERTY);
        }

        @Override
        public String toString() {
            return "ClassFileVersion.VersionPropertyAction." + name();
        }
    }
}
