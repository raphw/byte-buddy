package net.bytebuddy.matcher;


import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
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

public class LatentMatcherForMethodTokenTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodDescription.Token token, otherToken;

    @Mock
    private TypeDescription instrumentedType;

    @Mock
    private MethodDescription methodDescription;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(token.accept(any(TypeDescription.Generic.Visitor.class))).thenReturn(token);
    }

    @Test
    public void testMatch() throws Exception {
        when(methodDescription.asToken(ElementMatchers.is(instrumentedType))).thenReturn(token);
        assertThat(new LatentMatcher.ForMethodToken(token).resolve(instrumentedType).matches(methodDescription), is(true));
    }

    @Test
    public void testNoMatch() throws Exception {
        when(methodDescription.asToken(ElementMatchers.is(instrumentedType))).thenReturn(otherToken);
        assertThat(new LatentMatcher.ForMethodToken(token).resolve(instrumentedType).matches(methodDescription), is(false));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(LatentMatcher.ForMethodToken.class).apply();
    }
}
