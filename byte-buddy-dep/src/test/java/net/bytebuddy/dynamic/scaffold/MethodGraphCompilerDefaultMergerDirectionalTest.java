package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodGraphCompilerDefaultMergerDirectionalTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodDescription left, right;

    @Test
    public void testLeft() throws Exception {
        assertThat(MethodGraph.Compiler.Default.Merger.Directional.LEFT.merge(left, right), is(left));
    }

    @Test
    public void testRight() throws Exception {
        assertThat(MethodGraph.Compiler.Default.Merger.Directional.RIGHT.merge(left, right), is(right));
    }
}
