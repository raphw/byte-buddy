package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.utility.ObjectPropertyAssertion;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodRebaseResolverMethodNameTransformerPrefixingTest {

    private static final String FOO = "foo";

    @Test
    public void testTransformation() throws Exception {
        String transformed = new MethodRebaseResolver.MethodNameTransformer.Prefixing().transform(FOO);
        assertThat(transformed, not(FOO));
        assertThat(transformed, endsWith(FOO));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodRebaseResolver.MethodNameTransformer.Prefixing.class).apply();
    }
}
