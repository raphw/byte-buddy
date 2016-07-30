package net.bytebuddy.dynamic;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ClassFileLocatorSimpleTest {

    private static final String FOO = "foo", BAR = "bar";

    private static final byte[] QUX = new byte[]{1, 2, 3};

    @Test
    public void testSuccessfulLocation() throws Exception {
        ClassFileLocator.Resolution resolution = ClassFileLocator.Simple.of(FOO, QUX).locate(FOO);
        assertThat(resolution.isResolved(), is(true));
        assertThat(resolution.resolve(), is(QUX));
    }

    @Test
    public void testInSuccessfulLocation() throws Exception {
        ClassFileLocator.Resolution resolution = ClassFileLocator.Simple.of(FOO, QUX).locate(BAR);
        assertThat(resolution.isResolved(), is(false));
    }

    @Test
    public void testClose() throws Exception {
        ClassFileLocator.Simple.of(FOO, QUX).close();
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ClassFileLocator.Simple.class).apply();
    }
}
