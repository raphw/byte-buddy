package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodGraphNodeSimpleTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodDescription methodDescription;

    @Test
    public void testRepresentative() throws Exception {
        assertThat(new MethodGraph.Node.Simple(methodDescription).getRepresentative(), is(methodDescription));
    }

    @Test
    public void testBridgesEmpty() throws Exception {
        assertThat(new MethodGraph.Node.Simple(methodDescription).getMethodTypes().size(), is(0));
    }

    @Test
    public void testSort() throws Exception {
        assertThat(new MethodGraph.Node.Simple(methodDescription).getSort(), is(MethodGraph.Node.Sort.RESOLVED));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodGraph.Node.Simple.class).apply();
    }
}
