package net.bytebuddy.matcher;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static net.bytebuddy.matcher.ElementMatchers.any;
import static net.bytebuddy.test.utility.FieldByFieldComparison.hasPrototype;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class LatentMatcherConjunctionTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private LatentMatcher<?> left, right;

    @Mock
    private ElementMatcher<?> leftMatcher, rightMatcher;

    @Mock
    private TypeDescription typeDescription;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(left.resolve(typeDescription)).thenReturn((ElementMatcher) leftMatcher);
        when(right.resolve(typeDescription)).thenReturn((ElementMatcher) rightMatcher);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testManifestation() throws Exception {
        assertThat(new LatentMatcher.Conjunction(left, right).resolve(typeDescription),
                hasPrototype((ElementMatcher) any().and((ElementMatcher) leftMatcher).and((ElementMatcher) rightMatcher)));
    }
}
