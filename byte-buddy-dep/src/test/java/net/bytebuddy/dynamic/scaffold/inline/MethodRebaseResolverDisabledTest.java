package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodRebaseResolverDisabledTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodDescription.InDefinedShape methodDescription;

    @Test
    public void testResolutionPreservesMethod() throws Exception {
        MethodRebaseResolver.Resolution resolution = MethodRebaseResolver.Disabled.INSTANCE.resolve(methodDescription);
        assertThat(resolution.isRebased(), is(false));
        assertThat(resolution.getResolvedMethod(), is(methodDescription));
    }

    @Test
    public void testNoAuxiliaryTypes() throws Exception {
        assertThat(MethodRebaseResolver.Disabled.INSTANCE.getAuxiliaryTypes().size(), is(0));
    }

    @Test
    public void testNoRebaseableMethods() throws Exception {
        assertThat(MethodRebaseResolver.Disabled.INSTANCE.asTokenMap().size(), is(0));
    }
}
