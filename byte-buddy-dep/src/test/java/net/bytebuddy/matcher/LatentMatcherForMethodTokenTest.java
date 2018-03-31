package net.bytebuddy.matcher;


import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class LatentMatcherForMethodTokenTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

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
