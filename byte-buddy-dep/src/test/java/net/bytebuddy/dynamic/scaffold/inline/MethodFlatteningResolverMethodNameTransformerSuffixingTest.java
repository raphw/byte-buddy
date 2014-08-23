package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.utility.RandomString;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodFlatteningResolverMethodNameTransformerSuffixingTest {

    private static final String FOO = "foo";

    @Test
    public void testTransformation() throws Exception {
        String transformed = new MethodFlatteningResolver.MethodNameTransformer.Suffixing(new RandomString()).transform(FOO);
        assertThat(transformed, not(FOO));
        assertThat(transformed, startsWith(FOO));
    }
}
