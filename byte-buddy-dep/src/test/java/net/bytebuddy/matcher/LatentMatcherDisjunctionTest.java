package net.bytebuddy.matcher;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static net.bytebuddy.matcher.ElementMatchers.none;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class LatentMatcherDisjunctionTest {

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
        assertThat(new LatentMatcher.Disjunction(left, right).resolve(typeDescription),
                is((ElementMatcher) none().or((ElementMatcher) leftMatcher).or((ElementMatcher) rightMatcher)));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(LatentMatcher.Disjunction.class).apply();
    }
}
