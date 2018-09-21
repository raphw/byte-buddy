package net.bytebuddy;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.build.CachedReturnPlugin;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.utility.OpenedClassReader;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * A wrapper object for representing a validated class file version in the format that is specified by the
 * <a href="http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html">JVMS</a>.
 */
@HashCodeAndEqualsPlugin.Enhance
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
    public static final ClassFileVersion JAVA_V9 = new ClassFileVersion(Opcodes.V9);

    /**
     * The class file version of Java 10.
     */
    public static final ClassFileVersion JAVA_V10 = new ClassFileVersion(Opcodes.V10);

    /**
     * The class file version of Java 11.
     */
    public static final ClassFileVersion JAVA_V11 = new ClassFileVersion(Opcodes.V11);

    /**
     * The class file version of Java 12 (preliminary).
     */
    public static final ClassFileVersion JAVA_V12 = new ClassFileVersion(Opcodes.V11);

    /**
     * A version locator for the executing JVM.
     */
    private static final VersionLocator VERSION_LOCATOR = AccessController.doPrivileged(VersionLocator.CreationAction.INSTANCE);

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
     * Returns the Java class file by its representation by a version string in accordance to the formats known to <i>javac</i>.
     *
     * @param javaVersionString The Java version string.
     * @return The appropriate class file version.
     */
    public static ClassFileVersion ofJavaVersionString(String javaVersionString) {
        if (javaVersionString.equals("1.1")) {
            return JAVA_V1;
        } else if (javaVersionString.equals("1.2")) {
            return JAVA_V2;
        } else if (javaVersionString.equals("1.3")) {
            return JAVA_V3;
        } else if (javaVersionString.equals("1.4")) {
            return JAVA_V4;
        } else if (javaVersionString.equals("1.5") || javaVersionString.equals("5")) {
            return JAVA_V5;
        } else if (javaVersionString.equals("1.6") || javaVersionString.equals("6")) {
            return JAVA_V6;
        } else if (javaVersionString.equals("1.7") || javaVersionString.equals("7")) {
            return JAVA_V7;
        } else if (javaVersionString.equals("1.8") || javaVersionString.equals("8")) {
            return JAVA_V8;
        } else if (javaVersionString.equals("1.9") || javaVersionString.equals("9")) {
            return JAVA_V9;
        } else if (javaVersionString.equals("1.10") || javaVersionString.equals("10")) {
            return JAVA_V10;
        } else if (javaVersionString.equals("1.11") || javaVersionString.equals("11")) {
            return JAVA_V11;
        } else if (javaVersionString.equals("1.12") || javaVersionString.equals("12")) {
            return JAVA_V12;
        } else {
            throw new IllegalArgumentException("Unknown Java version string: " + javaVersionString);
        }
    }


    /**
     * Creates a class file version for a given major release of Java. Currently, all versions reaching from
     * Java 1 to Java 9 are supported.
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
            case 10:
                return JAVA_V10;
            case 11:
                return JAVA_V11;
            case 12:
                return JAVA_V12;
            default:
                throw new IllegalArgumentException("Unknown Java version: " + javaVersion);
        }
    }

    /**
     * Finds the highest class file version that is compatible to the current JVM version. Prior to Java 9, this is achieved
     * by parsing the {@code java.version} property which is provided by {@link java.lang.System#getProperty(String)}. If the system
     * property is not available, an {@link IllegalStateException} is thrown.
     *
     * @return The currently running Java process's class file version.
     */
    @CachedReturnPlugin.Enhance
    public static ClassFileVersion ofThisVm() {
        return VERSION_LOCATOR.locate();
    }

    /**
     * Finds the highest class file version that is compatible to the current JVM version. Prior to Java 9, this is achieved
     * by parsing the {@code java.version} property which is provided by {@link java.lang.System#getProperty(String)}. If the system
     * property is not available, the {@code fallback} version is returned.
     *
     * @param fallback The version to fallback to if locating a class file version is not possible.
     * @return The currently running Java process's class file version or the fallback if locating this version is impossible.
     */
    @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Exception should not be rethrown but trigger a fallback")
    public static ClassFileVersion ofThisVm(ClassFileVersion fallback) {
        try {
            return ofThisVm();
        } catch (Exception ignored) {
            return fallback;
        }
    }

    /**
     * Extracts a class' class version. The class' byte code is located by querying the {@link ClassLoader} of the class.
     *
     * @param type The type for which to locate a class file version.
     * @return The type's class file version.
     * @throws IOException If an error occurs while reading the class file.
     */
    public static ClassFileVersion of(Class<?> type) throws IOException {
        return of(type, ClassFileLocator.ForClassLoader.of(type.getClassLoader()));
    }

    /**
     * Extracts a class' class version.
     *
     * @param type             The type for which to locate a class file version.
     * @param classFileLocator The class file locator to query for a class file.
     * @return The type's class file version.
     * @throws IOException If an error occurs while reading the class file.
     */
    public static ClassFileVersion of(Class<?> type, ClassFileLocator classFileLocator) throws IOException {
        return of(TypeDescription.ForLoadedType.of(type), classFileLocator);
    }

    /**
     * Extracts a class' class version.
     *
     * @param typeDescription  The type for which to locate a class file version.
     * @param classFileLocator The class file locator to query for a class file.
     * @return The type's class file version.
     * @throws IOException If an error occurs while reading the class file.
     */
    public static ClassFileVersion of(TypeDescription typeDescription, ClassFileLocator classFileLocator) throws IOException {
        ClassReader classReader = OpenedClassReader.of(classFileLocator.locate(typeDescription.getName()).resolve());
        VersionExtractor versionExtractor = new VersionExtractor();
        classReader.accept(versionExtractor, ClassReader.SKIP_CODE);
        return ClassFileVersion.ofMinorMajor(versionExtractor.getClassFileVersionNumber());
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
     * Checks if this class file version is at least as new as the provided version.
     *
     * @param classFileVersion The version to check against.
     * @return {@code true} if this version is at least of the given version.
     */
    public boolean isAtLeast(ClassFileVersion classFileVersion) {
        return compareTo(classFileVersion) > -1;
    }

    /**
     * Checks if this class file version is newer than the provided version.
     *
     * @param classFileVersion The version to check against.
     * @return {@code true} if this version is newer than the provided version.
     */
    public boolean isGreaterThan(ClassFileVersion classFileVersion) {
        return compareTo(classFileVersion) > 0;
    }

    /**
     * Checks if this class file version is at most as new as the provided version.
     *
     * @param classFileVersion The version to check against.
     * @return {@code true} if this version is as most as new as the provided version.
     */
    public boolean isAtMost(ClassFileVersion classFileVersion) {
        return compareTo(classFileVersion) < 1;
    }

    /**
     * Checks if this class file version is older than the provided version.
     *
     * @param classFileVersion The version to check against.
     * @return {@code true} if this version is older than the provided version.
     */
    public boolean isLessThan(ClassFileVersion classFileVersion) {
        return compareTo(classFileVersion) < 0;
    }

    /**
     * {@inheritDoc}
     */
    public int compareTo(ClassFileVersion other) {
        return Integer.signum(getMajorVersion() == other.getMajorVersion()
                ? getMinorVersion() - other.getMinorVersion()
                : getMajorVersion() - other.getMajorVersion());
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "Java " + getJavaVersion();
    }

    /**
     * A locator for the executing VM's Java version.
     */
    protected interface VersionLocator {

        /**
         * Locates the current VM's major version number.
         *
         * @return The current VM's major version number.
         */
        ClassFileVersion locate();

        /**
         * A creation action for a version locator.
         */
        enum CreationAction implements PrivilegedAction<VersionLocator> {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * {@inheritDoc}
             */
            @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Exception should not be rethrown but trigger a fallback")
            public VersionLocator run() {
                try {
                    return new VersionLocator.ForJava9CapableVm(Runtime.class.getMethod("version"),
                            Class.forName("java.lang.Runtime$Version").getMethod("major"));
                } catch (Exception ignored) {
                    return VersionLocator.ForLegacyVm.INSTANCE;
                }
            }
        }

        /**
         * A version locator for a JVM of at least version 9.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class ForJava9CapableVm implements VersionLocator {

            /**
             * Indicates that a reflective method call invokes a static method.
             */
            private static final Object STATIC_METHOD = null;

            /**
             * The {@code java.lang.Runtime#version()} method.
             */
            private final Method current;

            /**
             * The {@code java.lang.Runtime.Version#major()} method.
             */
            private final Method major;

            /**
             * Creates a new version locator for a Java 9 capable VM.
             *
             * @param current The {@code java.lang.Runtime#version()} method.
             * @param major   The {@code java.lang.Runtime.Version#major()} method.
             */
            protected ForJava9CapableVm(Method current, Method major) {
                this.current = current;
                this.major = major;
            }

            /**
             * {@inheritDoc}
             */
            public ClassFileVersion locate() {
                try {
                    return ClassFileVersion.ofJavaVersion((Integer) major.invoke(current.invoke(STATIC_METHOD)));
                } catch (IllegalAccessException exception) {
                    throw new IllegalStateException("Could not access VM version lookup", exception);
                } catch (InvocationTargetException exception) {
                    throw new IllegalStateException("Could not look up VM version", exception.getCause());
                }
            }
        }

        /**
         * A version locator for a JVM that does not provide the {@code java.lang.Runtime.Version} class.
         */
        enum ForLegacyVm implements VersionLocator, PrivilegedAction<String> {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * The system property for this JVM's Java version.
             */
            private static final String JAVA_VERSION_PROPERTY = "java.version";

            /**
             * {@inheritDoc}
             */
            public ClassFileVersion locate() {
                String versionString = AccessController.doPrivileged(this);
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
             * {@inheritDoc}
             */
            public String run() {
                return System.getProperty(JAVA_VERSION_PROPERTY);
            }
        }
    }

    /**
     * A simple visitor that extracts the class file version of a class file.
     */
    protected static class VersionExtractor extends ClassVisitor {

        /**
         * The class file version extracted from a class.
         */
        private int classFileVersionNumber;

        /**
         * Creates a new extractor.
         */
        protected VersionExtractor() {
            super(OpenedClassReader.ASM_API);
        }

        @Override
        public void visit(int classFileVersionNumber, int modifier, String internalName, String signature, String superTypeName, String[] interfaceName) {
            this.classFileVersionNumber = classFileVersionNumber;
        }

        /**
         * Returns the class file version number found in a class file.
         *
         * @return The class file version number found in a class file.
         */
        protected int getClassFileVersionNumber() {
            return classFileVersionNumber;
        }
    }
}
