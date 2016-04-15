package net.bytebuddy.dynamic;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.hamcrest.MatcherAssert;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ClassFileLocatorResolutionTest {

    private static final String FOO = "foo";

    private static final byte[] DATA = new byte[]{1, 2, 3};

    @Test
    public void testIllegal() throws Exception {
        MatcherAssert.assertThat(new ClassFileLocator.Resolution.Illegal(FOO).isResolved(), is(false));
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalThrowsException() throws Exception {
        new ClassFileLocator.Resolution.Illegal(FOO).resolve();
    }

    @Test
    public void testExplicit() throws Exception {
        assertThat(new ClassFileLocator.Resolution.Explicit(DATA).isResolved(), is(true));
    }

    @Test
    public void testExplicitGetData() throws Exception {
        assertThat(new ClassFileLocator.Resolution.Explicit(DATA).resolve(), is(DATA));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ClassFileLocator.Resolution.Explicit.class).apply();
        ObjectPropertyAssertion.of(ClassFileLocator.Resolution.Illegal.class).apply();
    }
}
