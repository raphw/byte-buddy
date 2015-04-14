package net.bytebuddy.pool;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TypePoolResolutionTest {

    private static final String FOO = "foo";

    @Test
    public void testSimpleResolution() throws Exception {
        TypeDescription typeDescription = mock(TypeDescription.class);
        assertThat(new TypePool.Resolution.Simple(typeDescription).isResolved(), is(true));
        assertThat(new TypePool.Resolution.Simple(typeDescription).resolve(), is(typeDescription));
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalResolution() throws Exception {
        assertThat(new TypePool.Resolution.Illegal(FOO).isResolved(), is(false));
        new TypePool.Resolution.Illegal(FOO).resolve();
        fail();
    }

    @Test
    public void testArrayResolutionZeroArity() throws Exception {
        TypePool.Resolution resolution = mock(TypePool.Resolution.class);
        assertThat(TypePool.Default.ArrayTypeResolution.of(resolution, 0), is(resolution));
    }

    @Test
    public void testArrayResolutionPositiveArity() throws Exception {
        TypePool.Resolution resolution = mock(TypePool.Resolution.class);
        when(resolution.isResolved()).thenReturn(true);
        when(resolution.resolve()).thenReturn(mock(TypeDescription.class));
        assertThat(TypePool.Default.ArrayTypeResolution.of(resolution, 1), not(is(resolution)));
        TypeDescription typeDescription = TypePool.Default.ArrayTypeResolution.of(resolution, 1).resolve();
        assertThat(typeDescription.isArray(), is(true));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypePool.Resolution.Simple.class).apply();
        ObjectPropertyAssertion.of(TypePool.Resolution.Illegal.class).apply();
        ObjectPropertyAssertion.of(TypePool.Default.ArrayTypeResolution.class).apply();
    }
}
