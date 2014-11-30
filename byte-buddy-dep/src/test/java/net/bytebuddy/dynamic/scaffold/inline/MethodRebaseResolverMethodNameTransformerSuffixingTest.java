package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodRebaseResolverMethodNameTransformerSuffixingTest {

    private static final String FOO = "foo";

    @Test
    public void testTransformation() throws Exception {
        String transformed = new MethodRebaseResolver.MethodNameTransformer.Suffixing().transform(FOO);
        assertThat(transformed, not(FOO));
        assertThat(transformed, startsWith(FOO));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodRebaseResolver.MethodNameTransformer.Suffixing.class).apply();
    }
}
