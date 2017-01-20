package net.bytebuddy.matcher;

import net.bytebuddy.description.method.MethodDescription;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class SignatureTokenMatcherTest extends AbstractElementMatcherTest<SignatureTokenMatcher<?>> {

    @Mock
    private MethodDescription.SignatureToken token;

    @Mock
    private MethodDescription methodDescription;

    @Mock
    private ElementMatcher<? super MethodDescription.SignatureToken> matcher;

    @SuppressWarnings("unchecked")
    public SignatureTokenMatcherTest() {
        super((Class<SignatureTokenMatcher<?>>) (Object) SignatureTokenMatcher.class, "signature");
    }

    @Test
    public void testMatche() throws Exception {
        when(methodDescription.asSignatureToken()).thenReturn(token);
        when(matcher.matches(token)).thenReturn(true);
        assertThat(new SignatureTokenMatcher<MethodDescription>(matcher).matches(methodDescription), is(true));
        verify(matcher).matches(token);
        verifyNoMoreInteractions(matcher);
    }

    @Test
    public void testNoMatch() throws Exception {
        when(methodDescription.asSignatureToken()).thenReturn(token);
        when(matcher.matches(token)).thenReturn(false);
        assertThat(new SignatureTokenMatcher<MethodDescription>(matcher).matches(methodDescription), is(false));
        verify(matcher).matches(token);
        verifyNoMoreInteractions(matcher);
    }
}
