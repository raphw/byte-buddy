package net.bytebuddy.pool;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class TypePoolEmptyTest {

    private static final String FOO = "foo";

    @Test
    public void testResolutionUnresolved() throws Exception {
        assertThat(TypePool.Empty.INSTANCE.describe(FOO).isResolved(), is(false));
    }

    @Test(expected = IllegalStateException.class)
    public void testResolutionThrowsException() throws Exception {
        TypePool.Empty.INSTANCE.describe(FOO).resolve();
    }

    @Test
    public void testClearNoEffect() throws Exception {
        TypePool.Empty.INSTANCE.clear();
    }
}
