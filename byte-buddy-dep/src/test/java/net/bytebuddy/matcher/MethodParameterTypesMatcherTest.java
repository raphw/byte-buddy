package net.bytebuddy.matcher;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class MethodParameterTypesMatcherTest extends AbstractElementMatcherTest<MethodParameterTypesMatcher<?>> {

    @Mock
    private ElementMatcher<? super List<? extends TypeDescription>> parameterMatcher;

    @Mock
    private MethodDescription methodDescription;

    @Mock
    private TypeList typeList;

    @SuppressWarnings("unchecked")
    public MethodParameterTypesMatcherTest() {
        super((Class<MethodParameterTypesMatcher<?>>) (Object) MethodParameterTypesMatcher.class, "parameters");
    }

    @Before
    public void setUp() throws Exception {
        when(methodDescription.getParameterTypes()).thenReturn(typeList);
    }

    @Test
    public void testMatch() throws Exception {
        when(parameterMatcher.matches(typeList)).thenReturn(true);
        assertThat(new MethodParameterTypesMatcher<MethodDescription>(parameterMatcher).matches(methodDescription), is(true));
        verify(parameterMatcher).matches(typeList);
        verifyNoMoreInteractions(parameterMatcher);
    }

    @Test
    public void testNoMatch() throws Exception {
        when(parameterMatcher.matches(typeList)).thenReturn(false);
        assertThat(new MethodParameterTypesMatcher<MethodDescription>(parameterMatcher).matches(methodDescription), is(false));
        verify(parameterMatcher).matches(typeList);
        verifyNoMoreInteractions(parameterMatcher);
    }
}
