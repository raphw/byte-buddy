package net.bytebuddy;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;
import org.objectweb.asm.Opcodes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertTrue;

public class ClassFileVersionTest {

    @Test
    public void testCurrentJavaVersionWasManuallyEvaluated() throws Exception {
        // This test is supposed to fail if ByteBuddy was not yet manually considered for
        // a new major release targeting Java.
        assertTrue(ClassFileVersion.forCurrentJavaVersion().getVersion() <= Opcodes.V1_8);
    }

    @Test
    public void testExplicitConstructionOfUnknownVersion() throws Exception {
        assertThat(new ClassFileVersion(Opcodes.V1_8 + 1).getVersion(), is(Opcodes.V1_8 + 1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalVersion() throws Exception {
        new ClassFileVersion(0);
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
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ClassFileVersion.class).apply();
    }
}
