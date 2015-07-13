package net.bytebuddy.matcher;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.ParameterDescription;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class ParameterTokenMatcherTest extends AbstractElementMatcherTest<ParameterTokenMatcher<?>> {

    @Mock
    private ParameterDescription parameterDescription;

    @Mock
    private ParameterDescription.Token parameterToken;

    @Mock
    private ElementMatcher<? super ParameterDescription.Token> tokenMatcher;

    @SuppressWarnings("unchecked")
    public ParameterTokenMatcherTest() {
        super((Class<ParameterTokenMatcher<?>>) (Object) ParameterTokenMatcher.class, "representedBy");
    }

    @Before
    public void setUp() throws Exception {
        when(parameterDescription.asToken()).thenReturn(parameterToken);
    }

    @Test
    public void testMatch() throws Exception {
        when(tokenMatcher.matches(parameterToken)).thenReturn(true);
        when(parameterDescription.asToken()).thenReturn(parameterToken);
        assertThat(new ParameterTokenMatcher<ParameterDescription>(tokenMatcher).matches(parameterDescription), is(true));
        verify(tokenMatcher).matches(parameterToken);
        verifyNoMoreInteractions(tokenMatcher);
        verify(parameterDescription).asToken();
        verifyNoMoreInteractions(parameterDescription);
    }

    @Test
    public void testNoMatch() throws Exception {
        when(tokenMatcher.matches(parameterToken)).thenReturn(false);
        assertThat(new ParameterTokenMatcher<ParameterDescription>(tokenMatcher).matches(parameterDescription), is(false));
        verify(tokenMatcher).matches(parameterToken);
        verifyNoMoreInteractions(tokenMatcher);
        verify(parameterDescription).asToken();
        verifyNoMoreInteractions(parameterDescription);
    }
}
