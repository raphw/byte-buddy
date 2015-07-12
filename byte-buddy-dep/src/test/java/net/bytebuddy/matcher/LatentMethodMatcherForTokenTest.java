package net.bytebuddy.matcher;


import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class LatentMethodMatcherForTokenTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodDescription.Token methodToken, otherToken;

    @Mock
    private TypeDescription instrumentedType;

    @Mock
    private MethodDescription methodDescription;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(methodToken.accept(any(GenericTypeDescription.Visitor.class))).thenReturn(methodToken);
    }

    @Test
    public void testMatch() throws Exception {
        when(methodDescription.asToken()).thenReturn(methodToken);
        assertThat(new LatentMethodMatcher.ForToken(methodToken).resolve(instrumentedType).matches(methodDescription), is(true));
    }

    @Test
    public void testNoMatch() throws Exception {
        when(methodDescription.asToken()).thenReturn(otherToken);
        assertThat(new LatentMethodMatcher.ForToken(methodToken).resolve(instrumentedType).matches(methodDescription), is(false));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(LatentMethodMatcher.ForToken.class).apply();
    }
}
