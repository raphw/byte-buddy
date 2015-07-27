package net.bytebuddy;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@RunWith(Parameterized.class)
public class ClassFileVersionKnownVersionsTest {

    private final int javaVersion;

    private final int minorMajorVersion;

    private final int majorVersion;

    private final int minorVersion;

    public ClassFileVersionKnownVersionsTest(int javaVersion, int minorMajorVersion, int majorVersion, int minorVersion) {
        this.javaVersion = javaVersion;
        this.minorMajorVersion = minorMajorVersion;
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {1, Opcodes.V1_1, 45, 3},
                {2, Opcodes.V1_2, 46, 0},
                {3, Opcodes.V1_3, 47, 0},
                {4, Opcodes.V1_4, 48, 0},
                {5, Opcodes.V1_5, 49, 0},
                {6, Opcodes.V1_6, 50, 0},
                {7, Opcodes.V1_7, 51, 0},
                {8, Opcodes.V1_8, 52, 0},
                {9, Opcodes.V1_8, 52, 0}
        });
    }

    @Test
    public void testVersion() throws Exception {
        assertThat(ClassFileVersion.forKnownJavaVersion(javaVersion).getVersion(), is(minorMajorVersion));
    }

    @Test
    public void testMinorVersion() throws Exception {
        assertThat(ClassFileVersion.forKnownJavaVersion(javaVersion).getMinorVersion(), is(minorVersion));
    }

    @Test
    public void testMajorVersion() throws Exception {
        assertThat(ClassFileVersion.forKnownJavaVersion(javaVersion).getMajorVersion(), is(majorVersion));
    }
}
