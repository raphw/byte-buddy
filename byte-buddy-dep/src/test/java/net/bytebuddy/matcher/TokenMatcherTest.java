package net.bytebuddy.matcher;

import net.bytebuddy.description.ByteCodeElement;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class TokenMatcherTest extends AbstractElementMatcherTest<TokenMatcher<?, ?>> {

    @Mock
    private ByteCodeElement.Token<?> token, otherToken;

    @Mock
    private ElementMatcher<ByteCodeElement.Token<?>> matcher;

    @Mock
    private ByteCodeElement.TypeDependant<?, ?> typeDependant;

    @SuppressWarnings("unchecked")
    public TokenMatcherTest() {
        super((Class<? extends TokenMatcher<?, ?>>) (Object) TokenMatcher.class, "hasToken");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMatch() throws Exception {
        when(matcher.matches(token)).thenReturn(true);
        when(typeDependant.asToken()).thenReturn((ByteCodeElement.Token) token);
        assertThat(new TokenMatcher(matcher).matches(typeDependant), is(true));
        verify(typeDependant).asToken();
        verifyNoMoreInteractions(typeDependant);
        verify(matcher).matches(token);
        verifyNoMoreInteractions(matcher);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testNoMatch() throws Exception {
        when(matcher.matches(token)).thenReturn(true);
        when(typeDependant.asToken()).thenReturn((ByteCodeElement.Token) otherToken);
        assertThat(new TokenMatcher(matcher).matches(typeDependant), is(false));
        verify(typeDependant).asToken();
        verifyNoMoreInteractions(typeDependant);
        verify(matcher).matches(otherToken);
        verifyNoMoreInteractions(matcher);
    }
}
