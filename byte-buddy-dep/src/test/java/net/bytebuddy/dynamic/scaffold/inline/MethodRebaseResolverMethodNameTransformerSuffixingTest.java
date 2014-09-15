package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.utility.HashCodeEqualsTester;
import net.bytebuddy.utility.RandomString;
import org.junit.Test;

import java.util.Random;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class MethodRebaseResolverMethodNameTransformerSuffixingTest {

    private static final String FOO = "foo";

    @Test
    public void testTransformation() throws Exception {
        String transformed = new MethodRebaseResolver.MethodNameTransformer.Suffixing(new RandomString()).transform(FOO);
        assertThat(transformed, not(FOO));
        assertThat(transformed, startsWith(FOO));
    }

    @Test
    public void testHashCodeEquals() throws Exception {
        HashCodeEqualsTester.of(MethodRebaseResolver.MethodNameTransformer.Suffixing.class).refine(new HashCodeEqualsTester.Refinement() {
            @Override
            public void apply(Object mock) {
                if (RandomString.class.isAssignableFrom(mock.getClass())) {
                    when(((RandomString) mock).nextString()).thenReturn("" + new Random().nextInt());
                }
            }
        }).apply();
    }
}
