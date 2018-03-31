package net.bytebuddy.description.method;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodDescriptionSignatureTokenTest {

    private static final String FOO = "foo";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription returnType, parameterType;

    @Test
    public void testProperties() throws Exception {
        MethodDescription.SignatureToken token = new MethodDescription.SignatureToken(FOO, returnType, Collections.singletonList(parameterType));
        assertThat(token.getName(), is(FOO));
        assertThat(token.getReturnType(), is(returnType));
        assertThat(token.getParameterTypes(), is(Collections.singletonList(parameterType)));
    }

    @Test
    public void testTypeToken() throws Exception {
        assertThat(new MethodDescription.SignatureToken(FOO, returnType, Collections.singletonList(parameterType)).asTypeToken(),
                is(new MethodDescription.TypeToken(returnType, Collections.singletonList(parameterType))));
    }
}
