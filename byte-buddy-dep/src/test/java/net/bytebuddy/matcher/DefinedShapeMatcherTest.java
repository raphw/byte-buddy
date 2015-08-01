package net.bytebuddy.matcher;

import net.bytebuddy.description.ByteCodeElement;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class DefinedShapeMatcherTest extends AbstractElementMatcherTest<DefinedShapeMatcher<?, ?>> {

    @Mock
    private ByteCodeElement.TypeDependant<?, ?> dependant, resolvedDependant, otherResolvedDependant;

    @Mock
    private ElementMatcher<ByteCodeElement.TypeDependant<?, ?>> matcher;

    @SuppressWarnings("unchecked")
    public DefinedShapeMatcherTest() {
        super((Class<? extends DefinedShapeMatcher<?, ?>>) (Object) DefinedShapeMatcher.class, "isDefinedAs");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMatch() throws Exception {
        when(matcher.matches(resolvedDependant)).thenReturn(true);
        when(dependant.asDefined()).thenReturn((ByteCodeElement.TypeDependant) resolvedDependant);
        assertThat(new DefinedShapeMatcher(matcher).matches(dependant), is(true));
        verify(dependant).asDefined();
        verifyNoMoreInteractions(dependant);
        verify(matcher).matches(resolvedDependant);
        verifyNoMoreInteractions(matcher);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testNoMatch() throws Exception {
        when(matcher.matches(resolvedDependant)).thenReturn(true);
        when(dependant.asDefined()).thenReturn((ByteCodeElement.TypeDependant) otherResolvedDependant);
        assertThat(new DefinedShapeMatcher(matcher).matches(dependant), is(false));
        verify(dependant).asDefined();
        verifyNoMoreInteractions(dependant);
        verify(matcher).matches(otherResolvedDependant);
        verifyNoMoreInteractions(matcher);
    }
}
