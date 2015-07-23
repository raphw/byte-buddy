package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodRebaseResolverResolutionPreservedTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodDescription.inDefinedShape methodDescription;

    @Test
    public void testPreservation() throws Exception {
        MethodRebaseResolver.Resolution resolution = new MethodRebaseResolver.Resolution.Preserved(methodDescription);
        assertThat(resolution.isRebased(), is(false));
        assertThat(resolution.getResolvedMethod(), is(methodDescription));
        assertThat(resolution.getAdditionalArguments(), is((StackManipulation) StackManipulation.LegalTrivial.INSTANCE));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodRebaseResolver.Resolution.Preserved.class).apply();
    }
}
