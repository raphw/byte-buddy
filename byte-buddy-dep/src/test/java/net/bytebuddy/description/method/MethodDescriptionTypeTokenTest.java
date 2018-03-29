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

public class MethodDescriptionTypeTokenTest {

    private static final String FOO = "foo";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription returnType, parameterType;

    @Test
    public void testProperties() throws Exception {
        MethodDescription.TypeToken token = new MethodDescription.TypeToken(returnType, Collections.singletonList(parameterType));
        assertThat(token.getReturnType(), is(returnType));
        assertThat(token.getParameterTypes(), is(Collections.singletonList(parameterType)));
    }
}
