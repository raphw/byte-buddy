package net.bytebuddy.build.gradle;

import net.bytebuddy.test.utility.MockitoRule;
import org.gradle.api.GradleException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class TransformationTest {

    private static final String FOO = "foo";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private File file, explicit, other;

    @Test
    public void testPlugin() throws Exception {
        Transformation transformation = new Transformation();
        transformation.setPlugin(FOO);
        assertThat(transformation.getPlugin(), is(FOO));
    }

    @Test(expected = GradleException.class)
    public void testEmptyPlugin() throws Exception {
        new Transformation().getPlugin();
    }

    @Test(expected = GradleException.class)
    public void testUnnamedPlugin() throws Exception {
        Transformation transformation = new Transformation();
        transformation.setPlugin("");
        transformation.getPlugin();
    }

    @Test
    public void testRawPlugin() throws Exception {
        assertThat(new Transformation().getRawPlugin(), nullValue(String.class));
    }

    @Test
    public void testExplicitClassPath() throws Exception {
        Transformation transformation = new Transformation();
        transformation.setClassPath(Collections.singleton(file));
        Iterator<? extends File> iterator = transformation.getClassPath(explicit, Collections.singleton(other)).iterator();
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(file));
        assertThat(iterator.hasNext(), is(false));
    }

    @Test
    public void testImplicitClassPath() throws Exception {
        Transformation transformation = new Transformation();
        Iterator<? extends File> iterator = transformation.getClassPath(explicit, Collections.singleton(other)).iterator();
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(explicit));
        assertThat(iterator.hasNext(), is(true));
        assertThat(iterator.next(), is(other));
        assertThat(iterator.hasNext(), is(false));
    }
}
