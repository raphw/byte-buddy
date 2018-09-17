package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.matcher.AbstractFilterableListTest;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodGraphNodeListTest extends AbstractFilterableListTest<MethodGraph.Node, MethodGraph.NodeList, MethodGraph.Node> {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodDescription first, second;

    private MethodGraph.Node firstNode, secondNode;

    @Before
    public void setUp() throws Exception {
        firstNode = new MethodGraph.Node.Simple(first);
        secondNode = new MethodGraph.Node.Simple(second);
    }

    protected MethodGraph.Node getFirst() throws Exception {
        return firstNode;
    }

    protected MethodGraph.Node getSecond() throws Exception {
        return secondNode;
    }

    protected MethodGraph.NodeList asList(List<MethodGraph.Node> elements) {
        return new MethodGraph.NodeList(elements);
    }

    protected MethodGraph.Node asElement(MethodGraph.Node element) {
        return element;
    }

    @Test
    @SuppressWarnings("unused")
    public void testAsMethodList() throws Exception {
        assertThat(new MethodGraph.NodeList(Arrays.asList(new MethodGraph.Node.Simple(first), new MethodGraph.Node.Simple(second))).asMethodList(),
                is((MethodList) new MethodList.Explicit<MethodDescription>(first, second)));
    }
}
