package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.method.MethodDescription;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodGraphCompilerDefaultMergerDirectionalTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

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
