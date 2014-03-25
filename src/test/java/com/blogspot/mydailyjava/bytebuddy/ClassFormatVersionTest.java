package com.blogspot.mydailyjava.bytebuddy;

import org.junit.Test;
import org.mockito.asm.Opcodes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertTrue;

public class ClassFormatVersionTest {

    @Test
    public void testCurrentJavaVersionWasManuallyEvaluated() throws Exception {
        // This test is supposed to fail if ByteBuddy was not yet manually considered for
        // a new major release targeting Java.
        assertTrue(ClassFormatVersion.forCurrentJavaVersion().getVersionNumber() <= Opcodes.V1_6 + 2);
    }

    @Test
    public void testExplicitConstructionOfUnknownVersion() throws Exception {
        assertThat(new ClassFormatVersion(Opcodes.V1_6 + 3).getVersionNumber(), is(Opcodes.V1_6 + 3));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalVersion() throws Exception {
        new ClassFormatVersion(0);
    }
}
