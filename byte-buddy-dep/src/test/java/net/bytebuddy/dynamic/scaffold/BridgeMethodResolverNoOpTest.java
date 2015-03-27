package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodList;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class BridgeMethodResolverNoOpTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodDescription methodDescription;

    @Test
    public void testResolution() throws Exception {
        assertThat(BridgeMethodResolver.NoOp.INSTANCE.make(new MethodList.Empty()).resolve(methodDescription), is(methodDescription));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(BridgeMethodResolver.NoOp.class).apply();
    }
}
