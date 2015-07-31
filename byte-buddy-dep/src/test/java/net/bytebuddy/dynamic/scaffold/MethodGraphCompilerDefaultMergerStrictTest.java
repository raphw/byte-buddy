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
    private MethodDescription left, right;

    @Mock
    private MethodDescription.Token leftToken, rightToken;

    @Before
    public void setUp() throws Exception {
        when(left.asToken()).thenReturn(leftToken);
        when(right.asToken()).thenReturn(rightToken);
    }

    @Test
    public void testIdentical() throws Exception {
        when(leftToken.isIdenticalTo(rightToken)).thenReturn(true);
        assertThat(MethodGraph.Compiler.Default.Merger.Strict.INSTANCE.merge(left, right), is(left));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNonIdentical() throws Exception {
        when(leftToken.isIdenticalTo(rightToken)).thenReturn(false);
        MethodGraph.Compiler.Default.Merger.Strict.INSTANCE.merge(left, right);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodGraph.Compiler.Default.Merger.Strict.class).apply();
    }
}
