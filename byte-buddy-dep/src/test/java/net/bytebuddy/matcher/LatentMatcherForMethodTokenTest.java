package net.bytebuddy.matcher;


import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class LatentMatcherForMethodTokenTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private MethodDescription.Token token;

    @Mock
    private MethodDescription.SignatureToken signatureToken, otherToken;

    @Mock
    private TypeDescription typeDescription;

    @Mock
    private MethodDescription methodDescription;

    @Test
    public void testMatch() throws Exception {
        when(methodDescription.asSignatureToken()).thenReturn(signatureToken);
        when(token.asSignatureToken(typeDescription)).thenReturn(signatureToken);
        assertThat(new LatentMatcher.ForMethodToken(token).resolve(typeDescription).matches(methodDescription), is(true));
    }

    @Test
    public void testNoMatch() throws Exception {
        when(methodDescription.asSignatureToken()).thenReturn(signatureToken);
        when(token.asSignatureToken(typeDescription)).thenReturn(otherToken);
        assertThat(new LatentMatcher.ForMethodToken(token).resolve(typeDescription).matches(methodDescription), is(false));
    }
}
