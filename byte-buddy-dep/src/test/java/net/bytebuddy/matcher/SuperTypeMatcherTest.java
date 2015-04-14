package net.bytebuddy.matcher;

import net.bytebuddy.description.type.TypeDescription;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class SuperTypeMatcherTest extends AbstractElementMatcherTest<SuperTypeMatcher<?>> {

    @Mock
    private TypeDescription typeDescription, otherType;

    @SuppressWarnings("unchecked")
    public SuperTypeMatcherTest() {
        super((Class<? extends SuperTypeMatcher<?>>) (Object) SuperTypeMatcher.class, "isSuperTypeOf");
    }

    @Test
    public void testMatch() throws Exception {
        when(otherType.isAssignableFrom(typeDescription)).thenReturn(true);
        assertThat(new SuperTypeMatcher<TypeDescription>(typeDescription).matches(otherType), is(true));
        verify(otherType).isAssignableFrom(typeDescription);
        verifyNoMoreInteractions(otherType);
        verifyZeroInteractions(typeDescription);
    }

    @Test
    public void testNoMatch() throws Exception {
        when(otherType.isAssignableFrom(typeDescription)).thenReturn(false);
        assertThat(new SuperTypeMatcher<TypeDescription>(typeDescription).matches(otherType), is(false));
        verify(otherType).isAssignableFrom(typeDescription);
        verifyNoMoreInteractions(otherType);
        verifyZeroInteractions(typeDescription);
    }
}
