package net.bytebuddy.matcher;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class MethodReturnTypeMatcherTest extends AbstractElementMatcherTest<MethodReturnTypeMatcher<?>> {

    @Mock
    private ElementMatcher<? super TypeDescription.Generic> typeMatcher;

    @Mock
    private TypeDescription.Generic returnType;

    @Mock
    private MethodDescription methodDescription;

    @SuppressWarnings("unchecked")
    public MethodReturnTypeMatcherTest() {
        super((Class<? extends MethodReturnTypeMatcher<?>>) (Object) MethodReturnTypeMatcher.class, "returns");
    }

    @Before
    public void setUp() throws Exception {
        when(methodDescription.getReturnType()).thenReturn(returnType);
    }

    @Test
    public void testMatch() throws Exception {
        when(typeMatcher.matches(returnType)).thenReturn(true);
        assertThat(new MethodReturnTypeMatcher<MethodDescription>(typeMatcher).matches(methodDescription), is(true));
        verify(typeMatcher).matches(returnType);
        verifyNoMoreInteractions(typeMatcher);
    }

    @Test
    public void testNoMatch() throws Exception {
        when(typeMatcher.matches(returnType)).thenReturn(false);
        assertThat(new MethodReturnTypeMatcher<MethodDescription>(typeMatcher).matches(methodDescription), is(false));
        verify(typeMatcher).matches(returnType);
        verifyNoMoreInteractions(typeMatcher);
    }
}
