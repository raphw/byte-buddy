package net.bytebuddy.matcher;

import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class TypeSymbolMatcherTest extends AbstractElementMatcherTest<TypeSymbolMatcher<?>> {

    private static final String FOO = "foo", BAR = "bar";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private GenericTypeDescription genericTypeDescription;

    @Mock
    private ElementMatcher<? super String> matcher;

    @SuppressWarnings("unchecked")
    public TypeSymbolMatcherTest() {
        super((Class<TypeSymbolMatcher<?>>) (Object) TypeSymbolMatcher.class, "isVariable");
    }

    @Test
    public void testMatch() throws Exception {
        when(genericTypeDescription.getSort()).thenReturn(GenericTypeDescription.Sort.VARIABLE);
        when(genericTypeDescription.getSymbol()).thenReturn(FOO);
        when(matcher.matches(FOO)).thenReturn(true);
        assertThat(new TypeSymbolMatcher<GenericTypeDescription>(matcher).matches(genericTypeDescription), is(true));
        verify(genericTypeDescription).getSort();
        verify(genericTypeDescription).getSymbol();
        verifyNoMoreInteractions(genericTypeDescription);
        verify(matcher).matches(FOO);
        verifyNoMoreInteractions(matcher);
    }

    @Test
    public void testNoMatchDifferentSymbol() throws Exception {
        when(genericTypeDescription.getSort()).thenReturn(GenericTypeDescription.Sort.VARIABLE);
        when(genericTypeDescription.getSymbol()).thenReturn(BAR);
        when(matcher.matches(FOO)).thenReturn(true);
        assertThat(new TypeSymbolMatcher<GenericTypeDescription>(matcher).matches(genericTypeDescription), is(false));
        verify(genericTypeDescription).getSort();
        verify(genericTypeDescription).getSymbol();
        verifyNoMoreInteractions(genericTypeDescription);
        verify(matcher).matches(BAR);
        verifyNoMoreInteractions(matcher);
    }

    @Test
    public void testNoMatchNonVariable() throws Exception {
        when(genericTypeDescription.getSort()).thenReturn(GenericTypeDescription.Sort.NON_GENERIC);
        assertThat(new TypeSymbolMatcher<GenericTypeDescription>(matcher).matches(genericTypeDescription), is(false));
        verify(genericTypeDescription).getSort();
        verifyNoMoreInteractions(genericTypeDescription);
        verifyZeroInteractions(matcher);
    }
}
