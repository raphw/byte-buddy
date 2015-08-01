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

public class TypeSortMatcherTest extends AbstractElementMatcherTest<TypeSortMatcher<?>> {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private GenericTypeDescription genericTypeDescription;

    @Mock
    private ElementMatcher<GenericTypeDescription.Sort> matcher;

    @SuppressWarnings("unchecked")
    public TypeSortMatcherTest() {
        super((Class<TypeSortMatcher<?>>) (Object) TypeSortMatcher.class, "ofSort");
    }

    @Test
    public void testMatch() throws Exception {
        when(genericTypeDescription.getSort()).thenReturn(GenericTypeDescription.Sort.NON_GENERIC);
        when(matcher.matches(GenericTypeDescription.Sort.NON_GENERIC)).thenReturn(true);
        assertThat(new TypeSortMatcher<GenericTypeDescription>(matcher).matches(genericTypeDescription), is(true));
        verify(genericTypeDescription).getSort();
        verifyNoMoreInteractions(genericTypeDescription);
        verify(matcher).matches(GenericTypeDescription.Sort.NON_GENERIC);
        verifyNoMoreInteractions(matcher);
    }

    @Test
    public void testNoMatch() throws Exception {
        when(genericTypeDescription.getSort()).thenReturn(GenericTypeDescription.Sort.NON_GENERIC);
        when(matcher.matches(GenericTypeDescription.Sort.NON_GENERIC)).thenReturn(false);
        assertThat(new TypeSortMatcher<GenericTypeDescription>(matcher).matches(genericTypeDescription), is(false));
        verify(genericTypeDescription).getSort();
        verifyNoMoreInteractions(genericTypeDescription);
        verify(matcher).matches(GenericTypeDescription.Sort.NON_GENERIC);
        verifyNoMoreInteractions(matcher);
    }
}
