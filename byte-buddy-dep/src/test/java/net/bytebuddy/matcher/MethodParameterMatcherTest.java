package net.bytebuddy.matcher;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class MethodParameterMatcherTest extends AbstractElementMatcherTest<MethodParametersMatcher<?>> {

    @Mock
    private ElementMatcher<? super ParameterList<?>> parameterListMatcher;

    @Mock
    private MethodDescription methodDescription;

    @Mock
    private ParameterList<?> parameterList;

    @SuppressWarnings("unchecked")
    public MethodParameterMatcherTest() {
        super((Class<MethodParametersMatcher<?>>) (Object) MethodParametersMatcher.class, "hasParameter");
    }

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(methodDescription.getParameters()).thenReturn((ParameterList) parameterList);
    }

    @Test
    public void testMatch() throws Exception {
        when(parameterListMatcher.matches(parameterList)).thenReturn(true);
        assertThat(new MethodParametersMatcher<MethodDescription>(parameterListMatcher).matches(methodDescription), is(true));
        verify(parameterListMatcher).matches(parameterList);
        verifyNoMoreInteractions(parameterListMatcher);
    }

    @Test
    public void testNoMatch() throws Exception {
        when(parameterListMatcher.matches(parameterList)).thenReturn(false);
        assertThat(new MethodParametersMatcher<MethodDescription>(parameterListMatcher).matches(methodDescription), is(false));
        verify(parameterListMatcher).matches(parameterList);
        verifyNoMoreInteractions(parameterListMatcher);
    }
}
