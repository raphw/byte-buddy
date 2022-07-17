package net.bytebuddy.description.method;

import net.bytebuddy.description.type.TypeDescription;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class MethodDescriptionSignatureTokenTest {

    private static final String FOO = "foo", BAR = "bar";

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

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

    @Test
    public void testSignature() throws Exception {
        when(returnType.getDescriptor()).thenReturn(FOO);
        when(parameterType.getDescriptor()).thenReturn(BAR);
        assertThat(new MethodDescription.SignatureToken(FOO, returnType, Collections.singletonList(parameterType)).getDescriptor(), is("(bar)foo"));
    }
}
