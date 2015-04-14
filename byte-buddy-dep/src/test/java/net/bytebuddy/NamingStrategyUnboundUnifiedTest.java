package net.bytebuddy;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class NamingStrategyUnboundUnifiedTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private NamingStrategy namingStrategy;

    @Mock
    private TypeDescription typeDescription;

    @Test
    public void testSubclass() throws Exception {
        assertThat(new NamingStrategy.Unbound.Unified(namingStrategy).subclass(typeDescription), is(namingStrategy));
    }

    @Test
    public void testRedefine() throws Exception {
        assertThat(new NamingStrategy.Unbound.Unified(namingStrategy).redefine(typeDescription), is(namingStrategy));
    }

    @Test
    public void testRebase() throws Exception {
        assertThat(new NamingStrategy.Unbound.Unified(namingStrategy).rebase(typeDescription), is(namingStrategy));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(NamingStrategy.Unbound.Unified.class).apply();
    }
}
