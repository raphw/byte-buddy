package net.bytebuddy.matcher;

import net.bytebuddy.description.DeclaredByType;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class DeclaringTypeMatcherTest extends AbstractElementMatcherTest<DeclaringTypeMatcher<?>> {

    @Mock
    private ElementMatcher<? super GenericTypeDescription> typeMatcher;

    @Mock
    private DeclaredByType declaredByType;

    @Mock
    private TypeDescription typeDescription;

    @SuppressWarnings("unchecked")
    public DeclaringTypeMatcherTest() {
        super((Class<DeclaringTypeMatcher<?>>) (Object) DeclaringTypeMatcher.class, "declaredBy");
    }

    @Test
    public void testMatch() throws Exception {
        when(declaredByType.getDeclaringType()).thenReturn(typeDescription);
        when(typeMatcher.matches(typeDescription)).thenReturn(true);
        assertThat(new DeclaringTypeMatcher<DeclaredByType>(typeMatcher).matches(declaredByType), is(true));
        verify(typeMatcher).matches(typeDescription);
        verifyNoMoreInteractions(typeMatcher);
        verify(declaredByType).getDeclaringType();
        verifyNoMoreInteractions(declaredByType);
    }

    @Test
    public void testNoMatch() throws Exception {
        when(declaredByType.getDeclaringType()).thenReturn(typeDescription);
        when(typeMatcher.matches(typeDescription)).thenReturn(false);
        assertThat(new DeclaringTypeMatcher<DeclaredByType>(typeMatcher).matches(declaredByType), is(false));
        verify(typeMatcher).matches(typeDescription);
        verifyNoMoreInteractions(typeMatcher);
        verify(declaredByType).getDeclaringType();
        verifyNoMoreInteractions(declaredByType);
    }

    @Test
    public void testNoMatchWhenNull() throws Exception {
        assertThat(new DeclaringTypeMatcher<DeclaredByType>(typeMatcher).matches(declaredByType), is(false));
        verifyZeroInteractions(typeMatcher);
        verify(declaredByType).getDeclaringType();
        verifyNoMoreInteractions(declaredByType);
    }
}
