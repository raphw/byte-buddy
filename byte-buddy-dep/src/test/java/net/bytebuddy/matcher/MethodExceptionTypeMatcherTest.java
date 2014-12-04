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

public class MethodExceptionTypeMatcherTest extends AbstractElementMatcherTest<MethodExceptionTypeMatcher<?>> {

    @Mock
    private ElementMatcher<? super List<? extends TypeDescription>> parameterMatcher;
    @Mock
    private MethodDescription methodDescription;
    @Mock
    private TypeList typeList;

    @SuppressWarnings("unchecked")
    public MethodExceptionTypeMatcherTest() {
        super((Class<MethodExceptionTypeMatcher<?>>) (Object) MethodExceptionTypeMatcher.class, "exceptions");
    }

    @Before
    public void setUp() throws Exception {
        when(methodDescription.getExceptionTypes()).thenReturn(typeList);
    }

    @Test
    public void testMatch() throws Exception {
        when(parameterMatcher.matches(typeList)).thenReturn(true);
        assertThat(new MethodExceptionTypeMatcher<MethodDescription>(parameterMatcher).matches(methodDescription), is(true));
        verify(parameterMatcher).matches(typeList);
        verifyNoMoreInteractions(parameterMatcher);
    }

    @Test
    public void testNoMatch() throws Exception {
        when(parameterMatcher.matches(typeList)).thenReturn(false);
        assertThat(new MethodExceptionTypeMatcher<MethodDescription>(parameterMatcher).matches(methodDescription), is(false));
        verify(parameterMatcher).matches(typeList);
        verifyNoMoreInteractions(parameterMatcher);
    }
}
