package net.bytebuddy.matcher;

import net.bytebuddy.description.ByteCodeElement;
import net.bytebuddy.description.type.TypeDescription;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class AccessibilityMatcherTest extends AbstractElementMatcherTest<AccessibilityMatcher<?>> {

    @Mock
    private TypeDescription typeDescription;

    @Mock
    private ByteCodeElement byteCodeElement;

    @SuppressWarnings("unchecked")
    public AccessibilityMatcherTest() {
        super((Class<? extends AccessibilityMatcher<?>>) (Object) AccessibilityMatcher.class, "isAccessibleTo");
    }

    @Test
    public void testMatch() throws Exception {
        when(byteCodeElement.isAccessibleTo(typeDescription)).thenReturn(true);
        assertThat(new AccessibilityMatcher<ByteCodeElement>(typeDescription).matches(byteCodeElement), is(true));
        verify(byteCodeElement).isAccessibleTo(typeDescription);
        verifyNoMoreInteractions(byteCodeElement);
        verifyZeroInteractions(typeDescription);
    }

    @Test
    public void testNoMatch() throws Exception {
        when(byteCodeElement.isAccessibleTo(typeDescription)).thenReturn(false);
        assertThat(new AccessibilityMatcher<ByteCodeElement>(typeDescription).matches(byteCodeElement), is(false));
        verify(byteCodeElement).isAccessibleTo(typeDescription);
        verifyNoMoreInteractions(byteCodeElement);
        verifyZeroInteractions(typeDescription);
    }
}
