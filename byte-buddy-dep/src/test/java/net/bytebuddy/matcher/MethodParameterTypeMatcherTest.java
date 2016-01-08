package net.bytebuddy.matcher;

import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class MethodParameterTypeMatcherTest extends AbstractElementMatcherTest<MethodParameterTypeMatcher<?>> {

    @Mock
    private ElementMatcher<? super TypeDescription.Generic> parameterMatcher;

    @Mock
    private TypeDescription.Generic typeDescription;

    @Mock
    private ParameterDescription parameterDescription;

    @SuppressWarnings("unchecked")
    public MethodParameterTypeMatcherTest() {
        super((Class<MethodParameterTypeMatcher<?>>) (Object) MethodParameterTypeMatcher.class, "hasType");
    }

    @Before
    public void setUp() throws Exception {
        when(parameterDescription.getType()).thenReturn(typeDescription);
    }

    @Test
    public void testMatch() throws Exception {
        when(parameterMatcher.matches(typeDescription)).thenReturn(true);
        assertThat(new MethodParameterTypeMatcher<ParameterDescription>(parameterMatcher).matches(parameterDescription), is(true));
        verify(parameterMatcher).matches(typeDescription);
        verifyNoMoreInteractions(parameterMatcher);
    }

    @Test
    public void testNoMatch() throws Exception {
        when(parameterMatcher.matches(typeDescription)).thenReturn(false);
        assertThat(new MethodParameterTypeMatcher<ParameterDescription>(parameterMatcher).matches(parameterDescription), is(false));
        verify(parameterMatcher).matches(typeDescription);
        verifyNoMoreInteractions(parameterMatcher);
    }
}
