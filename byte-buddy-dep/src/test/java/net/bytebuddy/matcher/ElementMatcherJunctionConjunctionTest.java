package net.bytebuddy.matcher;

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

    @Override
    protected String makeRegex(String startsWith) {
        return "^(.* and .*)$";
    }
}
