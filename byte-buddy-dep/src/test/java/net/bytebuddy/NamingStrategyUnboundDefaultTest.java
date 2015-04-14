package net.bytebuddy;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class NamingStrategyUnboundDefaultTest {

    private static final String FOO = "foo", BAR = "bar";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription typeDescription;

    @Before
    public void setUp() throws Exception {
        when(typeDescription.getName()).thenReturn(BAR);
    }

    @Test
    public void testSubclass() throws Exception {
        assertThat(new NamingStrategy.Unbound.Default(FOO).subclass(typeDescription), is((NamingStrategy) new NamingStrategy
                .SuffixingRandom(FOO, new NamingStrategy.SuffixingRandom.BaseNameResolver.ForGivenType(typeDescription))));
    }

    @Test
    public void testRedefine() throws Exception {
        assertThat(new NamingStrategy.Unbound.Default(FOO).redefine(typeDescription), is((NamingStrategy) new NamingStrategy.Fixed(BAR)));
    }

    @Test
    public void testRebase() throws Exception {
        assertThat(new NamingStrategy.Unbound.Default(FOO).rebase(typeDescription), is((NamingStrategy) new NamingStrategy.Fixed(BAR)));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(NamingStrategy.Unbound.Default.class).apply();
    }
}
