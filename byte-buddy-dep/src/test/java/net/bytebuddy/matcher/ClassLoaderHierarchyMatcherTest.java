package net.bytebuddy.matcher;

import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class ClassLoaderHierarchyMatcherTest extends AbstractElementMatcherTest<ClassLoaderHierarchyMatcher<?>> {

    @Mock
    private ElementMatcher<? super ClassLoader> classLoaderMatcher;

    @Mock
    private ClassLoader classLoader;

    @SuppressWarnings("unchecked")
    public ClassLoaderHierarchyMatcherTest() {
        super((Class<ClassLoaderHierarchyMatcher<?>>) (Object) ClassLoaderHierarchyMatcher.class, "hasChild");
    }

    @Test
    public void testMatchesChild() throws Exception {
        when(classLoaderMatcher.matches(classLoader)).thenReturn(true);
        assertThat(new ClassLoaderHierarchyMatcher<ClassLoader>(classLoaderMatcher).matches(classLoader), is(true));
        verify(classLoaderMatcher).matches(classLoader);
        verifyNoMoreInteractions(classLoaderMatcher);
    }

    @Test
    public void testMatchesParent() throws Exception {
        when(classLoaderMatcher.matches(null)).thenReturn(true);
        assertThat(new ClassLoaderHierarchyMatcher<ClassLoader>(classLoaderMatcher).matches(classLoader), is(true));
        verify(classLoaderMatcher).matches(classLoader);
        verify(classLoaderMatcher).matches(null);
        verifyNoMoreInteractions(classLoaderMatcher);
    }

    @Test
    public void testNoMatch() throws Exception {
        assertThat(new ClassLoaderHierarchyMatcher<ClassLoader>(classLoaderMatcher).matches(classLoader), is(false));
        verify(classLoaderMatcher).matches(classLoader);
        verify(classLoaderMatcher).matches(null);
        verifyNoMoreInteractions(classLoaderMatcher);
    }
}