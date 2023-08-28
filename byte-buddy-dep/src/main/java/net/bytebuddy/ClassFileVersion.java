/*
 * Copyright 2014 - Present Rafael Winterhalter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bytebuddy;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.build.AccessControllerPlugin;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.utility.OpenedClassReader;
import net.bytebuddy.utility.nullability.MaybeNull;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.security.PrivilegedAction;

/**
 * A wrapper object for representing a validated class file version in the format that is specified by the
 * <a href="http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html">JVMS</a>.
 */
public class ClassFileVersion implements Comparable<ClassFileVersion>, Serializable {

    /**
     * The class's serial version UID.
     */
    private static final long serialVersionUID = 1L;

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
     * The class file version of Java 12.
     */
    public static final ClassFileVersion JAVA_V12 = new ClassFileVersion(Opcodes.V12);

    /**
     * The class file version of Java 13.
     */
    public static final ClassFileVersion JAVA_V13 = new ClassFileVersion(Opcodes.V13);

    /**
     * The class file version of Java 14.
     */
    public static final ClassFileVersion JAVA_V14 = new ClassFileVersion(Opcodes.V14);

    /**
     * The class file version of Java 15.
     */
    public static final ClassFileVersion JAVA_V15 = new ClassFileVersion(Opcodes.V15);

    /**
     * The class file version of Java 16.
     */
    public static final ClassFileVersion JAVA_V16 = new ClassFileVersion(Opcodes.V16);

    /**
     * The class file version of Java 17.
     */
    public static final ClassFileVersion JAVA_V17 = new ClassFileVersion(Opcodes.V17);

    /**
     * The class file version of Java 18.
     */
    public static final ClassFileVersion JAVA_V18 = new ClassFileVersion(Opcodes.V18);

    /**
     * The class file version of Java 19.
     */
    public static final ClassFileVersion JAVA_V19 = new ClassFileVersion(Opcodes.V19);

    /**
     * The class file version of Java 20.
     */
    public static final ClassFileVersion JAVA_V20 = new ClassFileVersion(Opcodes.V20);

    /**
     * The class file version of Java 21.
     */
    public static final ClassFileVersion JAVA_V21 = new ClassFileVersion(Opcodes.V21);

    /**
     * A version locator for the executing JVM.
     */
    private static final VersionLocator VERSION_LOCATOR = doPrivileged(VersionLocator.Resolver.INSTANCE);

    /**
     * The version number that is represented by this class file version instance.
     */
    private final int versionNumber;

    /**
     * Creates a wrapper for a given minor-major release of the Java class file format.
     *
     * @param versionNumber The minor-major release number.
     */
    protected ClassFileVersion(int versionNumber) {
        this.versionNumber = versionNumber;
    }

    /**
     * A proxy for {@code java.security.AccessController#doPrivileged} that is activated if available.
     *
     * @param action The action to execute from a privileged context.
     * @param <T>    The type of the action's resolved value.
     * @return The action's resolved value.
     */
    @AccessControllerPlugin.Enhance
    private static <T> T doPrivileged(PrivilegedAction<T> action) {
        return action.run();
    }

    /**
     * Creates a wrapper for a given minor-major release of the Java class file format.
     *
     * @param versionNumber The minor-major release number.
     * @return A representation of the version number.
     */
    public static ClassFileVersion ofMinorMajor(int versionNumber) {
        ClassFileVersion classFileVersion = new ClassFileVersion(versionNumber);
        if (classFileVersion.getMajorVersion() > 0 && classFileVersion.getMajorVersion() <= BASE_VERSION) {
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
        } else if (javaVersionString.equals("1.13") || javaVersionString.equals("13")) {
            return JAVA_V13;
        } else if (javaVersionString.equals("1.14") || javaVersionString.equals("14")) {
            return JAVA_V14;
        } else if (javaVersionString.equals("1.15") || javaVersionString.equals("15")) {
            return JAVA_V15;
        } else if (javaVersionString.equals("1.16") || javaVersionString.equals("16")) {
            return JAVA_V16;
        } else if (javaVersionString.equals("1.17") || javaVersionString.equals("17")) {
            return JAVA_V17;
        } else if (javaVersionString.equals("1.18") || javaVersionString.equals("18")) {
            return JAVA_V18;
        } else if (javaVersionString.equals("1.19") || javaVersionString.equals("19")) {
            return JAVA_V19;
        } else if (javaVersionString.equals("1.20") || javaVersionString.equals("20")) {
            return JAVA_V20;
        } else if (javaVersionString.equals("1.21") || javaVersionString.equals("21")) {
            return JAVA_V21;
        } else {
            if (OpenedClassReader.EXPERIMENTAL) {
                try {
                    int version = Integer.parseInt(javaVersionString.startsWith("1.")
                            ? javaVersionString.substring(2)
                            : javaVersionString);
                    if (version > 0) {
                        return new ClassFileVersion(BASE_VERSION + version);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
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
            case 13:
                return JAVA_V13;
            case 14:
                return JAVA_V14;
            case 15:
                return JAVA_V15;
            case 16:
                return JAVA_V16;
            case 17:
                return JAVA_V17;
            case 18:
                return JAVA_V18;
            case 19:
                return JAVA_V19;
            case 20:
                return JAVA_V20;
            case 21:
                return JAVA_V21;
            default:
                if (OpenedClassReader.EXPERIMENTAL && javaVersion > 0) {
                    return new ClassFileVersion(BASE_VERSION + javaVersion);
                } else {
                    throw new IllegalArgumentException("Unknown Java version: " + javaVersion);
                }
        }
    }

    /**
     * Returns the latest officially supported Java version when experimental support is not enabled.
     *
     * @return The latest officially supported Java version.
     */
    public static ClassFileVersion latest() {
        return ClassFileVersion.JAVA_V21;
    }

    /**
     * Finds the highest class file version that is compatible to the current JVM version. Prior to Java 9, this is achieved
     * by parsing the {@code java.version} property which is provided by {@link java.lang.System#getProperty(String)}. If the system
     * property is not available, an {@link IllegalStateException} is thrown.
     *
     * @return The currently running Java process's class file version.
     */
    public static ClassFileVersion ofThisVm() {
        return VERSION_LOCATOR.resolve();
    }

    /**
     * Finds the highest class file version that is compatible to the current JVM version. Prior to Java 9, this is achieved
     * by parsing the {@code java.version} property which is provided by {@link java.lang.System#getProperty(String)}. If the system
     * property is not available, the {@code fallback} version is returned.
     *
     * @param fallback The version to fallback to if locating a class file version is not possible.
     * @return The currently running Java process's class file version or the fallback if locating this version is impossible.
     */
    @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Exception should not be rethrown but trigger a fallback.")
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
        return ofClassFile(classFileLocator.locate(typeDescription.getName()).resolve());
    }

    /**
     * Extracts a class' class version from a class file.
     *
     * @param binaryRepresentation The class file's binary representation.
     * @return The supplied class file's class file version.
     */
    public static ClassFileVersion ofClassFile(byte[] binaryRepresentation) {
        if (binaryRepresentation.length < 7) {
            throw new IllegalArgumentException("Supplied byte array is too short to be a class file with " + binaryRepresentation.length + " byte");
        }
        return ofMinorMajor(binaryRepresentation[4] << 24
                | binaryRepresentation[5] << 16
                | binaryRepresentation[6] << 8
                | binaryRepresentation[7]);
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
    public short getMajorVersion() {
        return (short) (versionNumber & 0xFFFF);
    }

    /**
     * Returns the minor version this instance represents.
     *
     * @return The minor version this instance represents.
     */
    public short getMinorVersion() {
        return (short) (versionNumber >>> 16);
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
     * Returns this class file version indicating a class using preview features.
     *
     * @return This class file version but indicating the use of preview features.
     */
    public ClassFileVersion asPreviewVersion() {
        return new ClassFileVersion(versionNumber | Opcodes.V_PREVIEW);
    }

    /**
     * Returns {@code true} if this class file version indicates the use of preview features.
     *
     * @return {@code true} if this class file version indicates the use of preview features.
     */
    public boolean isPreviewVersion() {
        return (versionNumber & Opcodes.V_PREVIEW) == Opcodes.V_PREVIEW;
    }

    /**
     * {@inheritDoc}
     */
    public int compareTo(ClassFileVersion other) {
        return Integer.signum(getMajorVersion() == other.getMajorVersion()
                ? getMinorVersion() - other.getMinorVersion()
                : getMajorVersion() - other.getMajorVersion());
    }

    @Override
    public int hashCode() {
        return versionNumber;
    }

    @Override
    public boolean equals(@MaybeNull Object other) {
        if (this == other) {
            return true;
        } else if (other == null || getClass() != other.getClass()) {
            return false;
        }
        return versionNumber == ((ClassFileVersion) other).versionNumber;
    }

    @Override
    public String toString() {
        return "Java " + getJavaVersion() + " (" + getMinorMajorVersion() + ")";
    }

    /**
     * A locator for the executing VM's Java version.
     */
    protected interface VersionLocator {

        /**
         * A suffix that might indicate an early access version of Java.
         */
        String EARLY_ACCESS = "-ea";

        /**
         * The property for reading the current VM's Java version.
         */
        String JAVA_VERSION = "java.version";

        /**
         * Locates the current VM's major version number.
         *
         * @return The current VM's major version number.
         */
        ClassFileVersion resolve();

        /**
         * A resolver for the current VM's class file version.
         */
        enum Resolver implements PrivilegedAction<VersionLocator> {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * {@inheritDoc}
             */
            @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Exception should not be rethrown but trigger a fallback.")
            public VersionLocator run() {
                try {
                    Class<?> type = Class.forName(Runtime.class.getName() + "$Version");
                    Method method;
                    try {
                        method = type.getMethod("feature");
                    } catch (NoSuchMethodException ignored) {
                        method = type.getMethod("major");
                    }
                    return new Resolved(ClassFileVersion.ofJavaVersion((Integer) method.invoke(Runtime.class.getMethod("version").invoke(null))));
                } catch (Throwable ignored) {
                    try {
                        String versionString = System.getProperty(JAVA_VERSION);
                        if (versionString == null) {
                            throw new IllegalStateException("Java version property is not set");
                        } else if (versionString.equals("0")) { // Used by Android, assume Java 6 defensively.
                            return new Resolved(ClassFileVersion.JAVA_V6);
                        }
                        if (versionString.endsWith(EARLY_ACCESS)) {
                            versionString = versionString.substring(0, versionString.length() - EARLY_ACCESS.length());
                        }
                        int[] versionIndex = {-1, 0, 0};
                        for (int index = 1; index < 3; index++) {
                            versionIndex[index] = versionString.indexOf('.', versionIndex[index - 1] + 1);
                            if (versionIndex[index] == -1) {
                                throw new IllegalStateException("This JVM's version string does not seem to be valid: " + versionString);
                            }
                        }
                        return new Resolved(ClassFileVersion.ofJavaVersion(Integer.parseInt(versionString.substring(versionIndex[1] + 1, versionIndex[2]))));
                    } catch (Throwable throwable) {
                        return new Unresolved(throwable.getMessage());
                    }
                }
            }
        }

        /**
         * A version locator for a resolved class file version.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class Resolved implements VersionLocator {

            /**
             * The resolved class file version.
             */
            private final ClassFileVersion classFileVersion;

            /**
             * Creates a new resolved version locator.
             *
             * @param classFileVersion The resolved class file version.
             */
            protected Resolved(ClassFileVersion classFileVersion) {
                this.classFileVersion = classFileVersion;
            }

            /**
             * {@inheritDoc}
             */
            public ClassFileVersion resolve() {
                return classFileVersion;
            }
        }

        /**
         * An unresolved version locator.
         */
        @HashCodeAndEqualsPlugin.Enhance
        class Unresolved implements VersionLocator {

            /**
             * The message of the exception that explains the resolution error.
             */
            private final String message;

            /**
             * Creates an unresolved version locator.
             *
             * @param message The message of the exception that explains the resolution error.
             */
            protected Unresolved(String message) {
                this.message = message;
            }

            /**
             * {@inheritDoc}
             */
            public ClassFileVersion resolve() {
                throw new IllegalStateException("Failed to resolve the class file version of the current VM: " + message);
            }
        }
    }
}
