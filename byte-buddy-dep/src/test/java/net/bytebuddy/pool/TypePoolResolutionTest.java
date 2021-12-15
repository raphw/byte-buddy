package net.bytebuddy.pool;

import net.bytebuddy.description.type.TypeDescription;
import org.junit.Test;

import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
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

    @Test
    public void testIllegalResolution() throws Exception {
        assertThat(new TypePool.Resolution.Illegal(FOO).isResolved(), is(false));
        try {
            new TypePool.Resolution.Illegal(FOO).resolve();
            fail();
        } catch (TypePool.Resolution.NoSuchTypeException exception) {
            assertThat(exception.getName(), is(FOO));
        }
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
        assertThat(TypePool.Default.ArrayTypeResolution.of(resolution, 1), not(resolution));
        TypeDescription typeDescription = TypePool.Default.ArrayTypeResolution.of(resolution, 1).resolve();
        assertThat(typeDescription.isArray(), is(true));
    }
}
