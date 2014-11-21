package net.bytebuddy.matcher;

import net.bytebuddy.instrumentation.type.DeclaredInType;
import net.bytebuddy.instrumentation.type.TypeDescription;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class DeclaringTypeMatcherTest extends AbstractElementMatcherTest<DeclaringTypeMatcher<?>> {

    @Mock
    private ElementMatcher<? super TypeDescription> typeMatcher;

    @Mock
    private DeclaredInType declaredInType;

    @Mock
    private TypeDescription typeDescription;

    @SuppressWarnings("unchecked")
    public DeclaringTypeMatcherTest() {
        super((Class<DeclaringTypeMatcher<?>>) (Object) DeclaringTypeMatcher.class, "declaredBy");
    }

    @Test
    public void testMatch() throws Exception {
        when(declaredInType.getDeclaringType()).thenReturn(typeDescription);
        when(typeMatcher.matches(typeDescription)).thenReturn(true);
        assertThat(new DeclaringTypeMatcher<DeclaredInType>(typeMatcher).matches(declaredInType), is(true));
        verify(typeMatcher).matches(typeDescription);
        verifyNoMoreInteractions(typeMatcher);
        verify(declaredInType).getDeclaringType();
        verifyNoMoreInteractions(declaredInType);
    }

    @Test
    public void testNoMatch() throws Exception {
        when(declaredInType.getDeclaringType()).thenReturn(typeDescription);
        when(typeMatcher.matches(typeDescription)).thenReturn(false);
        assertThat(new DeclaringTypeMatcher<DeclaredInType>(typeMatcher).matches(declaredInType), is(false));
        verify(typeMatcher).matches(typeDescription);
        verifyNoMoreInteractions(typeMatcher);
        verify(declaredInType).getDeclaringType();
        verifyNoMoreInteractions(declaredInType);
    }

    @Test
    public void testNoMatchWhenNull() throws Exception {
        assertThat(new DeclaringTypeMatcher<DeclaredInType>(typeMatcher).matches(declaredInType), is(false));
        verifyZeroInteractions(typeMatcher);
        verify(declaredInType).getDeclaringType();
        verifyNoMoreInteractions(declaredInType);
    }
}
