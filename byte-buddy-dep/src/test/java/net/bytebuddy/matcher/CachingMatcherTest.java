package net.bytebuddy.matcher;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class CachingMatcherTest extends AbstractElementMatcherTest<CachingMatcher<?>> {

    @Mock
    private Object target;

    @Mock
    private ElementMatcher<? super Object> matcher;

    private ConcurrentMap<Object, Boolean> map;

    @SuppressWarnings("unchecked")
    public CachingMatcherTest() {
        super((Class<CachingMatcher<?>>) (Object) CachingMatcher.class, "cached");
    }

    @Before
    public void setUp() throws Exception {
        map = new ConcurrentHashMap<Object, Boolean>();
        when(matcher.matches(target)).thenReturn(true);
    }

    @Test
    public void testMatchCachesNoEviction() throws Exception {
        ElementMatcher<Object> matcher = new CachingMatcher<Object>(this.matcher, map);
        assertThat(matcher.matches(target), is(true));
        assertThat(matcher.matches(target), is(true));
        verify(this.matcher).matches(target);
        verifyNoMoreInteractions(this.matcher);
        verifyNoMoreInteractions(target);
    }

    @Test
    public void testMatchCachesEviction() throws Exception {
        ElementMatcher<Object> matcher = new CachingMatcher.WithInlineEviction<Object>(this.matcher, map, 1);
        Object other = mock(Object.class);
        assertThat(matcher.matches(target), is(true));
        assertThat(matcher.matches(other), is(false));
        assertThat(matcher.matches(other), is(false));
        assertThat(matcher.matches(target), is(true));
        verify(this.matcher, times(2)).matches(target);
        verify(this.matcher).matches(other);
        verifyNoMoreInteractions(this.matcher);
        verifyNoMoreInteractions(target);
    }

    @Test
    public void testMatchNoCachesEviction() throws Exception {
        ElementMatcher<Object> matcher = new CachingMatcher.WithInlineEviction<Object>(this.matcher, map, 2);
        Object other = mock(Object.class);
        assertThat(matcher.matches(target), is(true));
        assertThat(matcher.matches(other), is(false));
        assertThat(matcher.matches(other), is(false));
        assertThat(matcher.matches(target), is(true));
        verify(this.matcher).matches(target);
        verify(this.matcher).matches(other);
        verifyNoMoreInteractions(this.matcher);
        verifyNoMoreInteractions(target);
    }
}
