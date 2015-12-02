package net.bytebuddy.pool;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class TypePoolDefaultPrecomputedTest {

    private TypePool typePool;

    @Before
    public void setUp() throws Exception {
        typePool = new TypePool.Default.Precomputed(new TypePool.CacheProvider.Simple(),
                ClassFileLocator.ForClassLoader.ofClassPath(),
                TypePool.Default.ReaderMode.FAST);
    }

    @Test
    public void testLoadableBootstrapLoaderClassPrecomputed() throws Exception {
        TypePool.Resolution resolution = typePool.describe(Object.class.getName());
        assertThat(resolution.isResolved(), is(true));
        assertThat(resolution.resolve(), is(TypeDescription.OBJECT));
    }

    @Test
    public void testLoadableBootstrapLoaderClassNonPrecomputed() throws Exception {
        TypePool.Resolution resolution = typePool.describe(Object.class.getName());
        assertThat(resolution.isResolved(), is(true));
        assertThat(resolution.resolve(), is(TypeDescription.OBJECT));
    }

    @Test
    public void testArrayClass() throws Exception {
        TypePool.Resolution resolution = typePool.describe(Object[].class.getName());
        assertThat(resolution.isResolved(), is(true));
        assertThat(resolution.resolve(), is((TypeDescription) new TypeDescription.ForLoadedType(Object[].class)));
    }

    @Test
    public void testPrimitiveClass() throws Exception {
        TypePool.Resolution resolution = typePool.describe(int.class.getName());
        assertThat(resolution.isResolved(), is(true));
        assertThat(resolution.resolve(), is((TypeDescription) new TypeDescription.ForLoadedType(int.class)));
    }

    @Test
    public void testClearRetainsFunctionality() throws Exception {
        TypePool.Resolution resolution = typePool.describe(Object.class.getName());
        assertThat(resolution.isResolved(), is(true));
        assertThat(resolution.resolve(), is(TypeDescription.OBJECT));
        typePool.clear();
        TypePool.Resolution otherResolution = typePool.describe(Object.class.getName());
        assertThat(otherResolution.isResolved(), is(true));
        assertThat(resolution.resolve(), is(TypeDescription.OBJECT));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypePool.Default.Precomputed.class).apply();
    }
}
