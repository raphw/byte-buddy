package net.bytebuddy.matcher;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class CollectionItemMatcherTest extends AbstractElementMatcherTest<CollectionItemMatcher<?>> {

    private Iterable<Object> iterable;

    private Object first, second;

    @Mock
    private ElementMatcher<Object> elementMatcher;

    @SuppressWarnings("unchecked")
    public CollectionItemMatcherTest() {
        super((Class<CollectionItemMatcher<?>>) (Object) CollectionItemMatcher.class, "whereOne");
    }

    @Before
    public void setUp() throws Exception {
        first = new Object();
        second = new Object();
        iterable = Arrays.asList(first, second);
    }

    @Test
    public void testMatchFirst() throws Exception {
        when(elementMatcher.matches(first)).thenReturn(true);
        assertThat(new CollectionItemMatcher<Object>(elementMatcher).matches(iterable), is(true));
        verify(elementMatcher).matches(first);
        verifyNoMoreInteractions(elementMatcher);
    }

    @Test
    public void testMatchSecond() throws Exception {
        when(elementMatcher.matches(first)).thenReturn(false);
        when(elementMatcher.matches(second)).thenReturn(true);
        assertThat(new CollectionItemMatcher<Object>(elementMatcher).matches(iterable), is(true));
        verify(elementMatcher).matches(first);
        verify(elementMatcher).matches(second);
        verifyNoMoreInteractions(elementMatcher);
    }

    @Test
    public void testNoMatch() throws Exception {
        when(elementMatcher.matches(first)).thenReturn(false);
        when(elementMatcher.matches(second)).thenReturn(false);
        assertThat(new CollectionItemMatcher<Object>(elementMatcher).matches(iterable), is(false));
        verify(elementMatcher).matches(first);
        verify(elementMatcher).matches(second);
        verifyNoMoreInteractions(elementMatcher);
    }
}
