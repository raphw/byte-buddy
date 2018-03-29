package net.bytebuddy;

import org.junit.Test;
import org.objectweb.asm.Opcodes;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ClassFileVersionTest {

    @Test
    public void testExplicitConstructionOfUnknownVersion() throws Exception {
        assertThat(ClassFileVersion.ofMinorMajor(Opcodes.V10 + 2).getMinorMajorVersion(), is(Opcodes.V10 + 2));
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
        assertThat(ClassFileVersion.VersionLocator.ForLegacyVm.INSTANCE.run(), is(System.getProperty("java.version")));
    }

    @Test
    public void testVersionOfClass() throws Exception {
        assertThat(ClassFileVersion.of(Foo.class).compareTo(ClassFileVersion.ofThisVm()) < 1, is(true));
    }

    private static class Foo {
        /* empty */
    }
}
