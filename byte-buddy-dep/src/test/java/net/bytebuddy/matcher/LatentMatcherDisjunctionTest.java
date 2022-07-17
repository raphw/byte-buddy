package net.bytebuddy.matcher;

import net.bytebuddy.description.type.TypeDescription;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import static net.bytebuddy.matcher.ElementMatchers.none;
import static net.bytebuddy.test.utility.FieldByFieldComparison.hasPrototype;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class LatentMatcherDisjunctionTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private LatentMatcher<?> left, right;

    @Mock
    private ElementMatcher<?> leftMatcher, rightMatcher;

    @Mock
    private TypeDescription typeDescription;

    @Before
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void setUp() throws Exception {
        when(left.resolve(typeDescription)).thenReturn((ElementMatcher) leftMatcher);
        when(right.resolve(typeDescription)).thenReturn((ElementMatcher) rightMatcher);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void testManifestation() throws Exception {
        assertThat(new LatentMatcher.Disjunction(left, right).resolve(typeDescription),
                hasPrototype((ElementMatcher) none().or((ElementMatcher) leftMatcher).or((ElementMatcher) rightMatcher)));
    }
}
