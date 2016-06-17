package net.bytebuddy;

import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class ClassFileVersionKnownVersionsTest {

    private final int javaVersion;

    private final int minorMajorVersion;

    private final int majorVersion;

    private final int minorVersion;

    private final boolean atLeastJava5;

    private final boolean atLeastJava7;

    private final boolean atLeastJava8;

    public ClassFileVersionKnownVersionsTest(int javaVersion,
                                             int minorMajorVersion,
                                             int majorVersion,
                                             int minorVersion,
                                             boolean atLeastJava5,
                                             boolean atLeastJava7,
                                             boolean atLeastJava8) {
        this.javaVersion = javaVersion;
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
                {1, Opcodes.V1_1, 45, 3, false, false, false},
                {2, Opcodes.V1_2, 46, 0, false, false, false},
                {3, Opcodes.V1_3, 47, 0, false, false, false},
                {4, Opcodes.V1_4, 48, 0, false, false, false},
                {5, Opcodes.V1_5, 49, 0, true, false, false},
                {6, Opcodes.V1_6, 50, 0, true, false, false},
                {7, Opcodes.V1_7, 51, 0, true, true, false},
                {8, Opcodes.V1_8, 52, 0, true, true, true},
                {9, 53, 53, 0, true, true, true}
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
    public void testJavaVersion() throws Exception {
        assertThat(ClassFileVersion.ofJavaVersion(javaVersion).getJavaVersion(), is(javaVersion));
    }

    @Test
    public void testLessThanJava8() throws Exception {
        assertThat(ClassFileVersion.ofJavaVersion(javaVersion).isLessThan(ClassFileVersion.JAVA_V8), is(!atLeastJava8));
    }

    @Test
    public void testSimpleClassCreation() throws Exception {
        ClassFileVersion classFileVersion = ClassFileVersion.ofJavaVersion(javaVersion);
        if (ClassFileVersion.forCurrentJavaVersion().compareTo(classFileVersion) >= 0) {
            Class<?> type = new ByteBuddy(classFileVersion)
                    .subclass(Foo.class)
                    .make()
                    .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                    .getLoaded();
            assertThat(type.newInstance(), notNullValue(Object.class));
        }
    }

    public static class Foo {
        /* empty */
    }
}
