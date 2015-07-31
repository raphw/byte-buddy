package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.method.MethodDescription;
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

public class MethodGraphCompilerDefaultMergerStrictTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodDescription.Token left, right;

    @Before
    public void setUp() throws Exception {
        when(left.isIdenticalTo(left)).thenReturn(true);
    }

    @Test
    public void testIdentical() throws Exception {
        assertThat(MethodGraph.Compiler.Default.Merger.Strict.INSTANCE.merge(left, left), is(left));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNonIdentical() throws Exception {
        MethodGraph.Compiler.Default.Merger.Strict.INSTANCE.merge(left, right);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodGraph.Compiler.Default.Merger.Strict.class).apply();
    }
}
