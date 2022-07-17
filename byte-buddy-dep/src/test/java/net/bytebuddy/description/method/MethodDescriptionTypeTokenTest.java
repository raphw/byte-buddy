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

public class MethodDescriptionTypeTokenTest {

    private static final String FOO = "foo";

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private TypeDescription returnType, parameterType;

    @Test
    public void testProperties() throws Exception {
        MethodDescription.TypeToken token = new MethodDescription.TypeToken(returnType, Collections.singletonList(parameterType));
        assertThat(token.getReturnType(), is(returnType));
        assertThat(token.getParameterTypes(), is(Collections.singletonList(parameterType)));
    }
}
