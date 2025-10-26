package net.bytebuddy.matcher;

import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ElementMatcherJunctionConjunctionTest extends AbstractElementMatcherTest<ElementMatcher.Junction.Conjunction<?>> {

    @Mock
    private ElementMatcher<? super Object> first, second;

    @SuppressWarnings("unchecked")
    public ElementMatcherJunctionConjunctionTest() {
        super((Class<? extends ElementMatcher.Junction.Conjunction<?>>) (Object) ElementMatcher.Junction.Conjunction.class, "");
    }

    @Test
    @SuppressWarnings("unchecked")
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
    @SuppressWarnings("unchecked")
    public void testApplicationFirstOnly() throws Exception {
        Object target = new Object();
        when(first.matches(target)).thenReturn(false);
        assertThat(new ElementMatcher.Junction.Conjunction<Object>(first, second).matches(target), is(false));
        verify(first).matches(target);
        verifyNoMoreInteractions(first);
        verifyNoMoreInteractions(second);
    }

    @Test
    @SuppressWarnings("unchecked")
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
    @Override
    @SuppressWarnings("unchecked")
    public void testStringRepresentation() throws Exception {
        assertThat(new ElementMatcher.Junction.Conjunction<Object>(first, second).toString(), containsString(" and "));
    }
}
