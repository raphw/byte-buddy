package net.bytebuddy.matcher;

import net.bytebuddy.instrumentation.type.TypeDescription;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class SubTypeMatcherTest extends AbstractElementMatcherTest<SubTypeMatcher2<?>> {

    @SuppressWarnings("unchecked")
    public SubTypeMatcherTest() {
        super((Class<? extends SubTypeMatcher2<?>>) (Object) SubTypeMatcher2.class, "isSubTypeOf");
    }

    @Mock
    private TypeDescription typeDescription, otherType;

    @Test
    public void testMatch() throws Exception {
        when(otherType.isAssignableTo(typeDescription)).thenReturn(true);
        assertThat(new SubTypeMatcher2<TypeDescription>(typeDescription).matches(otherType), is(true));
        verify(otherType).isAssignableTo(typeDescription);
        verifyNoMoreInteractions(otherType);
        verifyZeroInteractions(typeDescription);
    }

    @Test
    public void testNoMatch() throws Exception {
        when(otherType.isAssignableTo(typeDescription)).thenReturn(false);
        assertThat(new SubTypeMatcher2<TypeDescription>(typeDescription).matches(otherType), is(false));
        verify(otherType).isAssignableTo(typeDescription);
        verifyNoMoreInteractions(otherType);
        verifyZeroInteractions(typeDescription);
    }
}
