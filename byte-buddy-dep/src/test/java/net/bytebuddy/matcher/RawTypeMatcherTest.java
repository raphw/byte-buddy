package net.bytebuddy.matcher;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class RawTypeMatcherTest extends AbstractElementMatcherTest<RawTypeMatcher<?>> {

    @Mock
    private GenericTypeDescription genericTypeDescription;

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
        when(genericTypeDescription.asErasure()).thenReturn(typeDescription);
    }

    @Test
    public void testMatch() throws Exception {
        when(elementMatcher.matches(typeDescription)).thenReturn(true);
        when(genericTypeDescription.getSort()).thenReturn(GenericTypeDescription.Sort.NON_GENERIC);
        assertThat(new RawTypeMatcher<GenericTypeDescription>(elementMatcher).matches(genericTypeDescription), is(true));
        verify(genericTypeDescription).getSort();
        verify(genericTypeDescription).asErasure();
        verifyNoMoreInteractions(genericTypeDescription);
        verify(elementMatcher).matches(typeDescription);
        verifyNoMoreInteractions(elementMatcher);
        verifyZeroInteractions(typeDescription);
    }

    @Test
    public void testNoMatchWildcard() throws Exception {
        when(genericTypeDescription.getSort()).thenReturn(GenericTypeDescription.Sort.WILDCARD);
        assertThat(new RawTypeMatcher<GenericTypeDescription>(elementMatcher).matches(genericTypeDescription), is(false));
        verify(genericTypeDescription).getSort();
        verifyNoMoreInteractions(genericTypeDescription);
        verifyZeroInteractions(elementMatcher);
        verifyZeroInteractions(typeDescription);
    }

    @Test
    public void testNoMatch() throws Exception {
        when(elementMatcher.matches(typeDescription)).thenReturn(false);
        when(genericTypeDescription.getSort()).thenReturn(GenericTypeDescription.Sort.NON_GENERIC);
        assertThat(new RawTypeMatcher<GenericTypeDescription>(elementMatcher).matches(genericTypeDescription), is(false));
        verify(genericTypeDescription).getSort();
        verify(genericTypeDescription).asErasure();
        verifyNoMoreInteractions(genericTypeDescription);
        verify(elementMatcher).matches(typeDescription);
        verifyNoMoreInteractions(elementMatcher);
        verifyZeroInteractions(typeDescription);
    }
}