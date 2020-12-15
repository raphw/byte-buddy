package net.bytebuddy;

import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class ClassFileVersionKnownVersionsTest {

    private final int javaVersion;

    private final int derivedVersion;

    private final Collection<String> javaVersionStrings;

    private final int minorMajorVersion;

    private final short majorVersion;

    private final short minorVersion;

    private final boolean atLeastJava5;

    private final boolean atLeastJava7;

    private final boolean atLeastJava8;

    public ClassFileVersionKnownVersionsTest(int javaVersion,
                                             int derivedVersion,
                                             Collection<String> javaVersionStrings,
                                             int minorMajorVersion,
                                             short majorVersion,
                                             short minorVersion,
                                             boolean atLeastJava5,
                                             boolean atLeastJava7,
                                             boolean atLeastJava8) {
        this.javaVersion = javaVersion;
        this.derivedVersion = derivedVersion;
        this.javaVersionStrings = javaVersionStrings;
        this.minorMajorVersion = minorMajorVersion;
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.atLeastJava5 = atLeastJava5;
        this.atLeastJava7 = atLeastJava7;
        this.atLeastJava8 = atLeastJava8;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {1, 1, Collections.singleton("1.1"), Opcodes.V1_1, (short) 45, (short) 3, false, false, false},
                {2, 2, Collections.singleton("1.2"), Opcodes.V1_2, (short) 46, (short) 0, false, false, false},
                {3, 3, Collections.singleton("1.3"), Opcodes.V1_3, (short) 47, (short) 0, false, false, false},
                {4, 4, Collections.singleton("1.4"), Opcodes.V1_4, (short) 48, (short) 0, false, false, false},
                {5, 5, Arrays.asList("1.5", "5"), Opcodes.V1_5, (short) 49, (short) 0, true, false, false},
                {6, 6, Arrays.asList("1.6", "6"), Opcodes.V1_6, (short) 50, (short) 0, true, false, false},
                {7, 7, Arrays.asList("1.7", "7"), Opcodes.V1_7, (short) 51, (short) 0, true, true, false},
                {8, 8, Arrays.asList("1.8", "8"), Opcodes.V1_8, (short) 52, (short) 0, true, true, true},
                {9, 9, Arrays.asList("1.9", "9"), Opcodes.V9, (short) 53, (short) 0, true, true, true},
                {10, 10, Arrays.asList("1.10", "10"), Opcodes.V10, (short) 54, (short) 0, true, true, true},
                {11, 11, Arrays.asList("1.11", "11"), Opcodes.V11, (short) 55, (short) 0, true, true, true},
                {12, 12, Arrays.asList("1.12", "12"), Opcodes.V12, (short) 56, (short) 0, true, true, true},
                {13, 13, Arrays.asList("1.13", "13"), Opcodes.V13, (short) 57, (short) 0, true, true, true},
                {14, 14, Arrays.asList("1.14", "14"), Opcodes.V14, (short) 58, (short) 0, true, true, true},
                {15, 15, Arrays.asList("1.15", "15"), Opcodes.V15, (short) 59, (short) 0, true, true, true},
                {16, 16, Arrays.asList("1.16", "16"), Opcodes.V16, (short) 60, (short) 0, true, true, true},
                {17, 17, Arrays.asList("1.17", "17"), Opcodes.V16 + 1, (short) 61, (short) 0, true, true, true}
        });
    }

    @Test
    public void testVersion() throws Exception {
        assertThat(ClassFileVersion.ofJavaVersion(javaVersion).getMinorMajorVersion(), is(minorMajorVersion));
    }

    @Test
    public void testMinorVersion() throws Exception {
        assertThat(ClassFileVersion.ofJavaVersion(javaVersion).getMinorVersion(), is(minorVersion));
    }

    @Test
    public void testMajorVersion() throws Exception {
        assertThat(ClassFileVersion.ofJavaVersion(javaVersion).getMajorVersion(), is(majorVersion));
    }

    @Test
    public void testAtLeastJava5() throws Exception {
        assertThat(ClassFileVersion.ofJavaVersion(javaVersion).isAtLeast(ClassFileVersion.JAVA_V5), is(atLeastJava5));
    }

    @Test
    public void testAtLeastJava7() throws Exception {
        assertThat(ClassFileVersion.ofJavaVersion(javaVersion).isAtLeast(ClassFileVersion.JAVA_V7), is(atLeastJava7));
    }

    @Test
    public void testAtLeastJava8() throws Exception {
        assertThat(ClassFileVersion.ofJavaVersion(javaVersion).isAtLeast(ClassFileVersion.JAVA_V8), is(atLeastJava8));
    }

    @Test
    public void testLessThanJava8() throws Exception {
        assertThat(ClassFileVersion.ofJavaVersion(javaVersion).isLessThan(ClassFileVersion.JAVA_V8), is(!atLeastJava8));
    }

    @Test
    public void testAtMostJava8() throws Exception {
        assertThat(ClassFileVersion.ofJavaVersion(javaVersion).isAtMost(ClassFileVersion.JAVA_V8), is(!atLeastJava8 || javaVersion == 8));
    }

    @Test
    public void testGreaterThanJava8() throws Exception {
        assertThat(ClassFileVersion.ofJavaVersion(javaVersion).isGreaterThan(ClassFileVersion.JAVA_V8), is(atLeastJava8 && javaVersion != 8));
    }

    @Test
    public void testJavaVersion() throws Exception {
        assertThat(ClassFileVersion.ofJavaVersion(javaVersion).getJavaVersion(), is(derivedVersion));
    }

    @Test
    public void testJavaVersionString() throws Exception {
        for (String javaVersionString : javaVersionStrings) {
            assertThat(ClassFileVersion.ofJavaVersionString(javaVersionString).getJavaVersion(), is(derivedVersion));
        }
    }

    @Test
    public void testSimpleClassCreation() throws Exception {
        ClassFileVersion classFileVersion = ClassFileVersion.ofJavaVersion(javaVersion);
        if (ClassFileVersion.ofThisVm().compareTo(classFileVersion) >= 0) {
            Class<?> type = new ByteBuddy(classFileVersion)
                    .subclass(Foo.class)
                    .make()
                    .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                    .getLoaded();
            assertThat(type.getDeclaredConstructor().newInstance(), notNullValue(Object.class));
        }
    }

    @Test
    public void testNotPreview() throws Exception {
        assertThat(ClassFileVersion.ofJavaVersion(javaVersion).isPreviewVersion(), is(false));
    }

    @Test
    public void testPreview() throws Exception {
        ClassFileVersion classFileVersion = ClassFileVersion.ofJavaVersion(javaVersion).asPreviewVersion();
        assertThat(classFileVersion.getJavaVersion(), is(javaVersion));
        assertThat(classFileVersion.isPreviewVersion(), is(true));
    }

    @Test
    public void testToString() {
        assertThat(ClassFileVersion.ofJavaVersion(javaVersion).toString(), is("Java " + derivedVersion + " (" + minorMajorVersion + ")"));
    }

    public static class Foo {
        /* empty */
    }
}
