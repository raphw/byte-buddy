package net.bytebuddy.matcher;

import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class TypeSortMatcherTest extends AbstractElementMatcherTest<TypeSortMatcher<?>> {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private TypeDescription.Generic typeDescription;

    @Mock
    private ElementMatcher<TypeDefinition.Sort> matcher;

    @SuppressWarnings("unchecked")
    public TypeSortMatcherTest() {
        super((Class<TypeSortMatcher<?>>) (Object) TypeSortMatcher.class, "ofSort");
    }

    @Test
    public void testMatch() throws Exception {
        when(typeDescription.getSort()).thenReturn(TypeDefinition.Sort.NON_GENERIC);
        when(matcher.matches(TypeDefinition.Sort.NON_GENERIC)).thenReturn(true);
        assertThat(new TypeSortMatcher<TypeDescription.Generic>(matcher).matches(typeDescription), is(true));
        verify(typeDescription).getSort();
        verifyNoMoreInteractions(typeDescription);
        verify(matcher).matches(TypeDefinition.Sort.NON_GENERIC);
        verifyNoMoreInteractions(matcher);
    }

    @Test
    public void testNoMatch() throws Exception {
        when(typeDescription.getSort()).thenReturn(TypeDefinition.Sort.NON_GENERIC);
        when(matcher.matches(TypeDefinition.Sort.NON_GENERIC)).thenReturn(false);
        assertThat(new TypeSortMatcher<TypeDescription.Generic>(matcher).matches(typeDescription), is(false));
        verify(typeDescription).getSort();
        verifyNoMoreInteractions(typeDescription);
        verify(matcher).matches(TypeDefinition.Sort.NON_GENERIC);
        verifyNoMoreInteractions(matcher);
    }
}
