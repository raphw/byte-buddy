package net.bytebuddy.matcher;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class CollectionOneToOneMatcherTest extends AbstractElementMatcherTest<CollectionOneToOneMatcher<?>> {

    private Iterable<Object> iterable;

    private Object first, second;

    @Mock
    private ElementMatcher<Object> firstMatcher, secondMatcher;

    @SuppressWarnings("unchecked")
    public CollectionOneToOneMatcherTest() {
        super((Class<CollectionOneToOneMatcher<?>>) (Object) CollectionOneToOneMatcher.class, "containing");
    }

    @Before
    public void setUp() throws Exception {
        first = new Object();
        second = new Object();
        iterable = Arrays.asList(first, second);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMatch() throws Exception {
        when(firstMatcher.matches(first)).thenReturn(true);
        when(secondMatcher.matches(second)).thenReturn(true);
        assertThat(new CollectionOneToOneMatcher<Object>(Arrays.asList(firstMatcher, secondMatcher)).matches(iterable), is(true));
        verify(firstMatcher).matches(first);
        verifyNoMoreInteractions(firstMatcher);
        verify(secondMatcher).matches(second);
        verifyNoMoreInteractions(secondMatcher);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testNoMatchFirst() throws Exception {
        when(firstMatcher.matches(first)).thenReturn(false);
        assertThat(new CollectionOneToOneMatcher<Object>(Arrays.asList(firstMatcher, secondMatcher)).matches(iterable), is(false));
        verify(firstMatcher).matches(first);
        verifyNoMoreInteractions(firstMatcher);
        verifyZeroInteractions(secondMatcher);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testNoMatchSecond() throws Exception {
        when(firstMatcher.matches(first)).thenReturn(true);
        when(secondMatcher.matches(second)).thenReturn(false);
        assertThat(new CollectionOneToOneMatcher<Object>(Arrays.asList(firstMatcher, secondMatcher)).matches(iterable), is(false));
        verify(firstMatcher).matches(first);
        verifyNoMoreInteractions(firstMatcher);
        verify(secondMatcher).matches(second);
        verifyNoMoreInteractions(secondMatcher);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testNoMatchSize() throws Exception {
        assertThat(new CollectionOneToOneMatcher<Object>(Arrays.asList(firstMatcher, secondMatcher)).matches(Collections.singletonList(firstMatcher)), is(false));
        verifyZeroInteractions(firstMatcher);
        verifyZeroInteractions(secondMatcher);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testStringRepresentation() throws Exception {
        assertThat(new CollectionOneToOneMatcher<Object>(Arrays.asList(firstMatcher, secondMatcher)).toString(), is(startsWith + "(" + firstMatcher + ", " + secondMatcher + ")"));
    }
}
