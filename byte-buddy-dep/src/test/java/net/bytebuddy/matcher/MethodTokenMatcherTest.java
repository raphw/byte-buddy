package net.bytebuddy.matcher;

import net.bytebuddy.description.method.MethodDescription;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class MethodTokenMatcherTest extends AbstractElementMatcherTest<MethodTokenMatcher<?>> {

    @Mock
    private MethodDescription methodDescription;

    @Mock
    private MethodDescription.Token methodToken;

    @Mock
    private ElementMatcher<? super MethodDescription.Token> tokenMatcher;

    @SuppressWarnings("unchecked")
    public MethodTokenMatcherTest() {
        super((Class<MethodTokenMatcher<?>>) (Object) MethodTokenMatcher.class, "representedBy");
    }

    @Before
    public void setUp() throws Exception {
        when(methodDescription.asToken()).thenReturn(methodToken);
    }

    @Test
    public void testMatch() throws Exception {
        when(tokenMatcher.matches(methodToken)).thenReturn(true);
        when(methodDescription.asToken()).thenReturn(methodToken);
        assertThat(new MethodTokenMatcher<MethodDescription>(tokenMatcher).matches(methodDescription), is(true));
        verify(tokenMatcher).matches(methodToken);
        verifyNoMoreInteractions(tokenMatcher);
        verify(methodDescription).asToken();
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    public void testNoMatch() throws Exception {
        when(tokenMatcher.matches(methodToken)).thenReturn(false);
        assertThat(new MethodTokenMatcher<MethodDescription>(tokenMatcher).matches(methodDescription), is(false));
        verify(tokenMatcher).matches(methodToken);
        verifyNoMoreInteractions(tokenMatcher);
        verify(methodDescription).asToken();
        verifyNoMoreInteractions(methodDescription);
    }
}
