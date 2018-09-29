package net.bytebuddy.dynamic;

import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.io.ByteArrayInputStream;
import java.io.Closeable;

import static net.bytebuddy.test.utility.FieldByFieldComparison.hasPrototype;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class ClassFileLocatorForClassLoaderWeaklyReferencedTest {

    private static final String FOOBAR = "foo/bar";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private ClosableClassLoader classLoader;

    @Test
    public void testCreation() throws Exception {
        assertThat(ClassFileLocator.ForClassLoader.WeaklyReferenced.of(classLoader), hasPrototype((ClassFileLocator) new ClassFileLocator.ForClassLoader.WeaklyReferenced(classLoader)));
        assertThat(ClassFileLocator.ForClassLoader.WeaklyReferenced.of(null), hasPrototype(ClassFileLocator.ForClassLoader.ofBootLoader()));
        assertThat(ClassFileLocator.ForClassLoader.WeaklyReferenced.of(ClassLoader.getSystemClassLoader()), hasPrototype(ClassFileLocator.ForClassLoader.ofSystemLoader()));
        assertThat(ClassFileLocator.ForClassLoader.WeaklyReferenced.of(ClassLoader.getSystemClassLoader().getParent()), hasPrototype(ClassFileLocator.ForClassLoader.ofPlatformLoader()));
    }

    @Test
    public void testLocatable() throws Exception {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[]{1, 2, 3});
        when(classLoader.getResourceAsStream(FOOBAR + ".class")).thenReturn(inputStream);
        ClassFileLocator.Resolution resolution = new ClassFileLocator.ForClassLoader.WeaklyReferenced(classLoader).locate(FOOBAR);
        assertThat(resolution.isResolved(), is(true));
        assertThat(resolution.resolve(), is(new byte[]{1, 2, 3}));
        verify(classLoader).getResourceAsStream(FOOBAR + ".class");
        verifyNoMoreInteractions(classLoader);
    }

    @Test(expected = IllegalStateException.class)
    public void testNonLocatable() throws Exception {
        ClassFileLocator.Resolution resolution = new ClassFileLocator.ForClassLoader.WeaklyReferenced(classLoader).locate(FOOBAR);
        assertThat(resolution.isResolved(), is(false));
        verify(classLoader).getResourceAsStream(FOOBAR + ".class");
        verifyNoMoreInteractions(classLoader);
        resolution.resolve();
        fail();
    }

    @Test
    public void testClose() throws Exception {
        ClassFileLocator.ForClassLoader.WeaklyReferenced.of(classLoader).close();
        verifyZeroInteractions(classLoader);
    }

    private abstract static class ClosableClassLoader extends ClassLoader implements Closeable {
        /* empty */
    }
}
