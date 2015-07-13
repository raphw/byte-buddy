package net.bytebuddy.matcher;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verifyZeroInteractions;

public class RawTypeMatcherTest extends AbstractElementMatcherTest<RawTypeMatcher<?>> {

    @Mock
    private GenericTypeDescription genericTypeDescription;

    @Mock
    private TypeDescription typeDescriptions;

    @Mock
    private ElementMatcher<TypeDescription> elementMatcher;

    @SuppressWarnings("unchecked")
    public RawTypeMatcherTest() {
        super((Class<? extends RawTypeMatcher<?>>) (Object) RawTypeMatcher.class, "rawType");
    }

    @Before
    public void setUp() throws Exception {
        when(genericTypeDescription.asRawType()).thenReturn(typeDescriptions);
    }

    @Test
    public void testMatch() throws Exception {
        when(elementMatcher.matches(typeDescriptions)).thenReturn(true);
        when(genericTypeDescription.getSort()).thenReturn(GenericTypeDescription.Sort.NON_GENERIC);
        assertThat(new RawTypeMatcher<GenericTypeDescription>(elementMatcher).matches(genericTypeDescription), is(true));
        verify(genericTypeDescription).getSort();
        verify(genericTypeDescription).asRawType();
        verifyNoMoreInteractions(genericTypeDescription);
        verify(elementMatcher).matches(typeDescriptions);
        verifyNoMoreInteractions(elementMatcher);
        verifyZeroInteractions(typeDescriptions);
    }

    @Test
    public void testNoMatchWildcard() throws Exception {
        when(genericTypeDescription.getSort()).thenReturn(GenericTypeDescription.Sort.WILDCARD);
        assertThat(new RawTypeMatcher<GenericTypeDescription>(elementMatcher).matches(genericTypeDescription), is(false));
        verify(genericTypeDescription).getSort();
        verifyNoMoreInteractions(genericTypeDescription);
        verifyZeroInteractions(elementMatcher);
        verifyZeroInteractions(typeDescriptions);
    }

    @Test
    public void testNoMatch() throws Exception {
        when(elementMatcher.matches(typeDescriptions)).thenReturn(false);
        when(genericTypeDescription.getSort()).thenReturn(GenericTypeDescription.Sort.NON_GENERIC);
        assertThat(new RawTypeMatcher<GenericTypeDescription>(elementMatcher).matches(genericTypeDescription), is(false));
        verify(genericTypeDescription).getSort();
        verify(genericTypeDescription).asRawType();
        verifyNoMoreInteractions(genericTypeDescription);
        verify(elementMatcher).matches(typeDescriptions);
        verifyNoMoreInteractions(elementMatcher);
        verifyZeroInteractions(typeDescriptions);
    }
}