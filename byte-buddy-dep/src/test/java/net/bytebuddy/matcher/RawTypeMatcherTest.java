package net.bytebuddy.matcher;

import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class RawTypeMatcherTest extends AbstractElementMatcherTest<RawTypeMatcher<?>> {

    @Mock
    private TypeDefinition typeDefinition;

    @Mock
    private TypeDescription typeDescription;

    @Mock
    private ElementMatcher<TypeDescription> elementMatcher;

    @SuppressWarnings("unchecked")
    public RawTypeMatcherTest() {
        super((Class<? extends RawTypeMatcher<?>>) (Object) RawTypeMatcher.class, "rawType");
    }

    @Before
    public void setUp() throws Exception {
        when(typeDefinition.asErasure()).thenReturn(typeDescription);
    }

    @Test
    public void testMatch() throws Exception {
        when(elementMatcher.matches(typeDescription)).thenReturn(true);
        when(typeDefinition.getSort()).thenReturn(TypeDefinition.Sort.NON_GENERIC);
        assertThat(new RawTypeMatcher<TypeDefinition>(elementMatcher).matches(typeDefinition), is(true));
        verify(typeDefinition).getSort();
        verify(typeDefinition).asErasure();
        verifyNoMoreInteractions(typeDefinition);
        verify(elementMatcher).matches(typeDescription);
        verifyNoMoreInteractions(elementMatcher);
        verifyZeroInteractions(typeDescription);
    }

    @Test
    public void testNoMatchWildcard() throws Exception {
        when(typeDefinition.getSort()).thenReturn(TypeDefinition.Sort.WILDCARD);
        assertThat(new RawTypeMatcher<TypeDefinition>(elementMatcher).matches(typeDefinition), is(false));
        verify(typeDefinition).getSort();
        verifyNoMoreInteractions(typeDefinition);
        verifyZeroInteractions(elementMatcher);
        verifyZeroInteractions(typeDescription);
    }

    @Test
    public void testNoMatch() throws Exception {
        when(elementMatcher.matches(typeDescription)).thenReturn(false);
        when(typeDefinition.getSort()).thenReturn(TypeDefinition.Sort.NON_GENERIC);
        assertThat(new RawTypeMatcher<TypeDefinition>(elementMatcher).matches(typeDefinition), is(false));
        verify(typeDefinition).getSort();
        verify(typeDefinition).asErasure();
        verifyNoMoreInteractions(typeDefinition);
        verify(elementMatcher).matches(typeDescription);
        verifyNoMoreInteractions(elementMatcher);
        verifyZeroInteractions(typeDescription);
    }
}