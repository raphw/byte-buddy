package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.CoreMatchers.theInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class MethodRebaseResolverMethodNameTransformerSuffixingTest {

    private static final String FOO = "foo";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodDescription methodDescription;

    @Test
    public void testTransformation() throws Exception {
        when(methodDescription.getInternalName()).thenReturn(FOO);
        String transformed = new MethodRebaseResolver.MethodNameTransformer.Suffixing().transform(methodDescription);
        assertThat(transformed, not(FOO));
        assertThat(transformed, startsWith(FOO));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodRebaseResolver.MethodNameTransformer.Suffixing.class).apply();
    }
}
