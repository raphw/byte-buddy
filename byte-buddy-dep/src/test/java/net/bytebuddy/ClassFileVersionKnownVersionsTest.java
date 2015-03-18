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

    private final int byteCodeVersion;

    public ClassFileVersionKnownVersionsTest(int javaVersion, int byteCodeVersion) {
        this.javaVersion = javaVersion;
        this.byteCodeVersion = byteCodeVersion;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {1, Opcodes.V1_1},
                {2, Opcodes.V1_2},
                {3, Opcodes.V1_3},
                {4, Opcodes.V1_4},
                {5, Opcodes.V1_5},
                {6, Opcodes.V1_6},
                {7, Opcodes.V1_7},
                {8, Opcodes.V1_8},
        });
    }

    @Test
    public void testVersionIsAsExpected() throws Exception {
        assertThat(ClassFileVersion.forKnownJavaVersion(javaVersion).getVersionNumber(), is(byteCodeVersion));
    }
}
