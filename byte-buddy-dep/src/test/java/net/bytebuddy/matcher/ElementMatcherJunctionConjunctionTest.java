package net.bytebuddy.matcher;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class ElementMatcherJunctionConjunctionTest extends AbstractElementMatcherTest<ElementMatcher.Junction.Conjunction<?>> {

    @Mock
    private ElementMatcher<? super Object> first, second;

    @SuppressWarnings("unchecked")
    public ElementMatcherJunctionConjunctionTest() {
        super((Class<? extends ElementMatcher.Junction.Conjunction<?>>) (Object) ElementMatcher.Junction.Conjunction.class, "");
    }

    @Test
    public void testApplicationBoth() throws Exception {
        Object target = new Object();
        when(first.matches(target)).thenReturn(true);
        when(second.matches(target)).thenReturn(true);
        assertThat(new ElementMatcher.Junction.Conjunction<Object>(first, second).matches(target), is(true));
        verify(first).matches(target);
        verifyNoMoreInteractions(first);
        verify(second).matches(target);
        verifyNoMoreInteractions(second);
    }

    @Test
    public void testApplicationFirstOnly() throws Exception {
        Object target = new Object();
        when(first.matches(target)).thenReturn(false);
        assertThat(new ElementMatcher.Junction.Conjunction<Object>(first, second).matches(target), is(false));
        verify(first).matches(target);
        verifyNoMoreInteractions(first);
        verifyZeroInteractions(second);
    }

    @Test
    public void testApplicationBothNegative() throws Exception {
        Object target = new Object();
        when(first.matches(target)).thenReturn(true);
        when(second.matches(target)).thenReturn(false);
        assertThat(new ElementMatcher.Junction.Conjunction<Object>(first, second).matches(target), is(false));
        verify(first).matches(target);
        verifyNoMoreInteractions(first);
        verify(second).matches(target);
        verifyNoMoreInteractions(second);
    }

    @Test
    public void testToString() {
        assertThat(new ElementMatcher.Junction.Conjunction<Object>(first, second).toString(), CoreMatchers.containsString(" and "));
    }

    @Test
    public void testInlineLeftMatches() {
        Object target = new Object();
        when(first.matches(target)).thenReturn(true);
        when(second.matches(target)).thenReturn(true);
        ElementMatcher.Junction.Conjunction<Object> leaf = new ElementMatcher.Junction.Conjunction<Object>(first, ElementMatchers.any());
        assertThat(new ElementMatcher.Junction.Conjunction<Object>(leaf, second).matches(target), is(true));
        verify(first).matches(target);
        verifyNoMoreInteractions(first);
        verify(second).matches(target);
        verifyNoMoreInteractions(second);
    }

    @Test
    public void testInlineRightMatches() {
        Object target = new Object();
        when(first.matches(target)).thenReturn(true);
        when(second.matches(target)).thenReturn(true);
        ElementMatcher.Junction.Conjunction<Object> leaf = new ElementMatcher.Junction.Conjunction<Object>(second, ElementMatchers.any());
        assertThat(new ElementMatcher.Junction.Conjunction<Object>(first, leaf).matches(target), is(true));
        verify(first).matches(target);
        verifyNoMoreInteractions(first);
        verify(second).matches(target);
        verifyNoMoreInteractions(second);
    }

    @Test
    public void testInlineLeftNotMatches() {
        Object target = new Object();
        when(first.matches(target)).thenReturn(true);
        when(second.matches(target)).thenReturn(false);
        ElementMatcher.Junction.Conjunction<Object> leaf = new ElementMatcher.Junction.Conjunction<Object>(first, ElementMatchers.any());
        assertThat(new ElementMatcher.Junction.Conjunction<Object>(leaf, second).matches(target), is(false));
        verify(first).matches(target);
        verifyNoMoreInteractions(first);
        verify(second).matches(target);
        verifyNoMoreInteractions(second);
    }

    @Test
    public void testInlineLeftChildNotMatches() {
        Object target = new Object();
        when(first.matches(target)).thenReturn(true);
        when(second.matches(target)).thenReturn(true);
        ElementMatcher.Junction.Conjunction<Object> leaf = new ElementMatcher.Junction.Conjunction<Object>(first, ElementMatchers.none());
        assertThat(new ElementMatcher.Junction.Conjunction<Object>(leaf, second).matches(target), is(false));
        verify(first).matches(target);
        verifyNoMoreInteractions(first);
        verifyNoMoreInteractions(second);
    }

    @Test
    public void testInlineRightNotMatches() {
        Object target = new Object();
        when(first.matches(target)).thenReturn(true);
        when(second.matches(target)).thenReturn(false);
        ElementMatcher.Junction.Conjunction<Object> leaf = new ElementMatcher.Junction.Conjunction<Object>(second, ElementMatchers.any());
        assertThat(new ElementMatcher.Junction.Conjunction<Object>(first, leaf).matches(target), is(false));
        verify(first).matches(target);
        verifyNoMoreInteractions(first);
        verify(second).matches(target);
        verifyNoMoreInteractions(second);
    }

    @Test
    public void testInlineRightChildNotMatches() {
        Object target = new Object();
        when(first.matches(target)).thenReturn(true);
        when(second.matches(target)).thenReturn(true);
        ElementMatcher.Junction.Conjunction<Object> leaf = new ElementMatcher.Junction.Conjunction<Object>(ElementMatchers.none(), second);
        assertThat(new ElementMatcher.Junction.Conjunction<Object>(first, leaf).matches(target), is(false));
        verify(first).matches(target);
        verifyNoMoreInteractions(first);
        verifyNoMoreInteractions(second);
    }
}
