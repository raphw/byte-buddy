package net.bytebuddy;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;
import org.objectweb.asm.Opcodes;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ClassFileVersionTest {

    @Test
    public void testCurrentJavaVersionWasManuallyEvaluated() throws Exception {
        // This test is supposed to fail if ByteBuddy was not yet manually considered for
        // a new major release targeting Java.
        assertThat(ClassFileVersion.forCurrentJavaVersion().getMinorMajorVersion() <= Opcodes.V1_8, is(true));
    }

    @Test
    public void testExplicitConstructionOfUnknownVersion() throws Exception {
        assertThat(ClassFileVersion.ofMinorMajor(Opcodes.V1_8 + 1).getMinorMajorVersion(), is(Opcodes.V1_8 + 1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalVersion() throws Exception {
        ClassFileVersion.ofMinorMajor(ClassFileVersion.BASE_VERSION);
    }

    @Test
    public void testComparison() throws Exception {
        assertThat(new ClassFileVersion(Opcodes.V1_1).compareTo(new ClassFileVersion(Opcodes.V1_1)), is(0));
        assertThat(new ClassFileVersion(Opcodes.V1_1).compareTo(new ClassFileVersion(Opcodes.V1_2)), is(-1));
        assertThat(new ClassFileVersion(Opcodes.V1_2).compareTo(new ClassFileVersion(Opcodes.V1_1)), is(1));
        assertThat(new ClassFileVersion(Opcodes.V1_2).compareTo(new ClassFileVersion(Opcodes.V1_2)), is(0));
        assertThat(new ClassFileVersion(Opcodes.V1_3).compareTo(new ClassFileVersion(Opcodes.V1_2)), is(1));
        assertThat(new ClassFileVersion(Opcodes.V1_2).compareTo(new ClassFileVersion(Opcodes.V1_3)), is(-1));
    }

    @Test
    public void testVersionPropertyAction() throws Exception {
        assertThat(ClassFileVersion.VersionPropertyAction.INSTANCE.run(), is(System.getProperty("java.version")));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ClassFileVersion.class).apply();
        ObjectPropertyAssertion.of(ClassFileVersion.VersionPropertyAction.class).apply();
    }
}
