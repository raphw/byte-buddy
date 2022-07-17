package net.bytebuddy.matcher;


import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.type.TypeDescription;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

public class LatentMatcherForFieldTokenTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private FieldDescription.Token token;

    @Mock
    private FieldDescription.SignatureToken signatureToken, otherSignatureToken;

    @Mock
    private TypeDescription typeDescription;

    @Mock
    private FieldDescription fieldDescription;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(token.accept(any(TypeDescription.Generic.Visitor.class))).thenReturn(token);
    }

    @Test
    public void testMatch() throws Exception {
        when(fieldDescription.asSignatureToken()).thenReturn(signatureToken);
        when(token.asSignatureToken(typeDescription)).thenReturn(signatureToken);
        assertThat(new LatentMatcher.ForFieldToken(token).resolve(typeDescription).matches(fieldDescription), is(true));
    }

    @Test
    public void testNoMatch() throws Exception {
        when(fieldDescription.asSignatureToken()).thenReturn(otherSignatureToken);
        when(token.asSignatureToken(typeDescription)).thenReturn(signatureToken);
        assertThat(new LatentMatcher.ForFieldToken(token).resolve(typeDescription).matches(fieldDescription), is(false));
    }
}
