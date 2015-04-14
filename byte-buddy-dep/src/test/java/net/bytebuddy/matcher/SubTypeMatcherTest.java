package net.bytebuddy.matcher;

import net.bytebuddy.description.type.TypeDescription;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class SubTypeMatcherTest extends AbstractElementMatcherTest<SubTypeMatcher<?>> {

    @Mock
    private TypeDescription typeDescription, otherType;

    @SuppressWarnings("unchecked")
    public SubTypeMatcherTest() {
        super((Class<? extends SubTypeMatcher<?>>) (Object) SubTypeMatcher.class, "isSubTypeOf");
    }

    @Test
    public void testMatch() throws Exception {
        when(otherType.isAssignableTo(typeDescription)).thenReturn(true);
        assertThat(new SubTypeMatcher<TypeDescription>(typeDescription).matches(otherType), is(true));
        verify(otherType).isAssignableTo(typeDescription);
        verifyNoMoreInteractions(otherType);
        verifyZeroInteractions(typeDescription);
    }

    @Test
    public void testNoMatch() throws Exception {
        when(otherType.isAssignableTo(typeDescription)).thenReturn(false);
        assertThat(new SubTypeMatcher<TypeDescription>(typeDescription).matches(otherType), is(false));
        verify(otherType).isAssignableTo(typeDescription);
        verifyNoMoreInteractions(otherType);
        verifyZeroInteractions(typeDescription);
    }
}
