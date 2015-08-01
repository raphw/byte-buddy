package net.bytebuddy.matcher;

import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class TypeVariableMatcherTest extends AbstractElementMatcherTest<TypeVariableMatcher<?>> {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private GenericTypeDescription genericTypeDescription;

    @Mock
    private ElementMatcher<? super NamedElement> matcher;

    @SuppressWarnings("unchecked")
    public TypeVariableMatcherTest() {
        super((Class<TypeVariableMatcher<?>>) (Object) TypeVariableMatcher.class, "isVariable");
    }

    @Test
    public void testMatch() throws Exception {
        when(genericTypeDescription.getSort()).thenReturn(GenericTypeDescription.Sort.VARIABLE);
        when(matcher.matches(genericTypeDescription)).thenReturn(true);
        assertThat(new TypeVariableMatcher<GenericTypeDescription>(matcher).matches(genericTypeDescription), is(true));
        verify(genericTypeDescription).getSort();
        verifyNoMoreInteractions(genericTypeDescription);
        verify(matcher).matches(genericTypeDescription);
        verifyNoMoreInteractions(matcher);
    }

    @Test
    public void testNoMatchDifferentSymbol() throws Exception {
        when(genericTypeDescription.getSort()).thenReturn(GenericTypeDescription.Sort.VARIABLE);
        when(matcher.matches(genericTypeDescription)).thenReturn(false);
        assertThat(new TypeVariableMatcher<GenericTypeDescription>(matcher).matches(genericTypeDescription), is(false));
        verify(genericTypeDescription).getSort();
        verifyNoMoreInteractions(genericTypeDescription);
        verify(matcher).matches(genericTypeDescription);
        verifyNoMoreInteractions(matcher);
    }

    @Test
    public void testNoMatchNonVariable() throws Exception {
        when(genericTypeDescription.getSort()).thenReturn(GenericTypeDescription.Sort.NON_GENERIC);
        assertThat(new TypeVariableMatcher<GenericTypeDescription>(matcher).matches(genericTypeDescription), is(false));
        verify(genericTypeDescription).getSort();
        verifyNoMoreInteractions(genericTypeDescription);
        verifyZeroInteractions(matcher);
    }
}
