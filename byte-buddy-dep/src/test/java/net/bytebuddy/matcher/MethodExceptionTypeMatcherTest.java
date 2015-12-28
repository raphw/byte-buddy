package net.bytebuddy.matcher;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class MethodExceptionTypeMatcherTest extends AbstractElementMatcherTest<MethodExceptionTypeMatcher<?>> {

    @Mock
    private ElementMatcher<? super List<? extends TypeDescription.Generic>> exceptionMatcher;

    @Mock
    private MethodDescription methodDescription;

    @Mock
    private TypeList.Generic typeList;

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
        when(exceptionMatcher.matches(typeList)).thenReturn(true);
        assertThat(new MethodExceptionTypeMatcher<MethodDescription>(exceptionMatcher).matches(methodDescription), is(true));
        verify(exceptionMatcher).matches(typeList);
        verifyNoMoreInteractions(exceptionMatcher);
    }

    @Test
    public void testNoMatch() throws Exception {
        when(exceptionMatcher.matches(typeList)).thenReturn(false);
        assertThat(new MethodExceptionTypeMatcher<MethodDescription>(exceptionMatcher).matches(methodDescription), is(false));
        verify(exceptionMatcher).matches(typeList);
        verifyNoMoreInteractions(exceptionMatcher);
    }
}
