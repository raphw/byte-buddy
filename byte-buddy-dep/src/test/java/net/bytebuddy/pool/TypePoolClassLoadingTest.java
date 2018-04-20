package net.bytebuddy.pool;

import net.bytebuddy.description.type.TypeDescription;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class TypePoolClassLoadingTest {

    private final TypePool typePool;

    public TypePoolClassLoadingTest(TypePool typePool) {
        this.typePool = typePool;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {TypePool.ClassLoading.ofBootPath()},
                {TypePool.ClassLoading.ofClassPath()},
                {TypePool.ClassLoading.of(TypePoolClassLoadingTest.class.getClassLoader())}
        });
    }

    @Test
    public void testLoadableBootstrapLoaderClass() throws Exception {
        TypePool.Resolution resolution = typePool.describe(Object.class.getName());
        assertThat(resolution.isResolved(), is(true));
        assertThat(resolution.resolve(), is(TypeDescription.OBJECT));
    }

    @Test
    public void testArrayClass() throws Exception {
        TypePool.Resolution resolution = typePool.describe(Object[].class.getName());
        assertThat(resolution.isResolved(), is(true));
        assertThat(resolution.resolve(), is((TypeDescription) TypeDescription.ForLoadedType.of(Object[].class)));
    }

    @Test
    public void testPrimitiveClass() throws Exception {
        TypePool.Resolution resolution = typePool.describe(int.class.getName());
        assertThat(resolution.isResolved(), is(true));
        assertThat(resolution.resolve(), is((TypeDescription) TypeDescription.ForLoadedType.of(int.class)));
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
}
