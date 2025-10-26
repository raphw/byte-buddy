package net.bytebuddy.matcher;

import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class NegatingMatcherTest extends AbstractElementMatcherTest<NegatingMatcher<?>> {

    @Mock
    private ElementMatcher<? super Object> elementMatcher;

    @SuppressWarnings("unchecked")
    public NegatingMatcherTest() {
        super((Class<NegatingMatcher<?>>) (Object) NegatingMatcher.class, "not");
    }

    @Test
    public void testNegateToPositive() throws Exception {
        Object target = new Object();
        when(elementMatcher.matches(target)).thenReturn(true);
        assertThat(new NegatingMatcher<Object>(elementMatcher).matches(target), is(false));
        verify(elementMatcher).matches(target);
        verifyNoMoreInteractions(elementMatcher);
    }

    @Test
    public void testPositiveToNegative() throws Exception {
        Object target = new Object();
        when(elementMatcher.matches(target)).thenReturn(false);
        assertThat(new NegatingMatcher<Object>(elementMatcher).matches(target), is(true));
        verify(elementMatcher).matches(target);
        verifyNoMoreInteractions(elementMatcher);
    }
}
