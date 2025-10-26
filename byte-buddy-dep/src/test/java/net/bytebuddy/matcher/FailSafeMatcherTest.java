package net.bytebuddy.matcher;

import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class FailSafeMatcherTest extends AbstractElementMatcherTest<FailSafeMatcher<?>> {

    @SuppressWarnings("unchecked")
    public FailSafeMatcherTest() {
        super((Class<FailSafeMatcher<?>>) (Object) FailSafeMatcher.class, "failSafe");
    }

    @Mock
    private ElementMatcher<Object> elementMatcher;

    @Mock
    private Object target;

    @Test
    public void testMatch() throws Exception {
        when(elementMatcher.matches(target)).thenReturn(true);
        assertThat(new FailSafeMatcher<Object>(elementMatcher, false).matches(target), is(true));
        verifyNoMoreInteractions(target);
        verify(elementMatcher).matches(target);
        verifyNoMoreInteractions(elementMatcher);
    }

    @Test
    public void testNoMatch() throws Exception {
        when(elementMatcher.matches(target)).thenReturn(false);
        assertThat(new FailSafeMatcher<Object>(elementMatcher, false).matches(target), is(false));
        verifyNoMoreInteractions(target);
        verify(elementMatcher).matches(target);
        verifyNoMoreInteractions(elementMatcher);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMatchOnFailure() throws Exception {
        when(elementMatcher.matches(target)).thenThrow(RuntimeException.class);
        assertThat(new FailSafeMatcher<Object>(elementMatcher, true).matches(target), is(true));
        verifyNoMoreInteractions(target);
        verify(elementMatcher).matches(target);
        verifyNoMoreInteractions(elementMatcher);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testNoMatchOnFailure() throws Exception {
        when(elementMatcher.matches(target)).thenThrow(RuntimeException.class);
        assertThat(new FailSafeMatcher<Object>(elementMatcher, false).matches(target), is(false));
        verifyNoMoreInteractions(target);
        verify(elementMatcher).matches(target);
        verifyNoMoreInteractions(elementMatcher);
    }
}
