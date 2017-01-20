package net.bytebuddy.matcher;

import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class ErasureMatcherTest extends AbstractElementMatcherTest<ErasureMatcher<?>> {

    @Mock
    private TypeDefinition typeDefinition;

    @Mock
    private TypeDescription typeDescription;

    @Mock
    private ElementMatcher<TypeDescription> elementMatcher;

    @SuppressWarnings("unchecked")
    public ErasureMatcherTest() {
        super((Class<? extends ErasureMatcher<?>>) (Object) ErasureMatcher.class, "erasure");
    }

    @Before
    public void setUp() throws Exception {
        when(typeDefinition.asErasure()).thenReturn(typeDescription);
    }

    @Test
    public void testMatch() throws Exception {
        when(elementMatcher.matches(typeDescription)).thenReturn(true);
        when(typeDefinition.getSort()).thenReturn(TypeDefinition.Sort.NON_GENERIC);
        assertThat(new ErasureMatcher<TypeDefinition>(elementMatcher).matches(typeDefinition), is(true));
        verify(typeDefinition).asErasure();
        verifyNoMoreInteractions(typeDefinition);
        verify(elementMatcher).matches(typeDescription);
        verifyNoMoreInteractions(elementMatcher);
        verifyZeroInteractions(typeDescription);
    }

    @Test
    public void testNoMatch() throws Exception {
        when(elementMatcher.matches(typeDescription)).thenReturn(false);
        when(typeDefinition.getSort()).thenReturn(TypeDefinition.Sort.NON_GENERIC);
        assertThat(new ErasureMatcher<TypeDefinition>(elementMatcher).matches(typeDefinition), is(false));
        verify(typeDefinition).asErasure();
        verifyNoMoreInteractions(typeDefinition);
        verify(elementMatcher).matches(typeDescription);
        verifyNoMoreInteractions(elementMatcher);
        verifyZeroInteractions(typeDescription);
    }
}
