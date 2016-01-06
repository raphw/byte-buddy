package net.bytebuddy.description.type;

import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class TypeVariableTokenTest {

    private static final String FOO = "foo";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription.Generic bound, visitedBound;

    @Mock
    private TypeDescription.Generic.Visitor<? extends TypeDescription.Generic> visitor;

    @Before
    public void setUp() throws Exception {
        when(bound.asGenericType()).thenReturn(bound);
        when(visitedBound.asGenericType()).thenReturn(visitedBound);
        when(bound.accept(visitor)).thenReturn(visitedBound);
    }

    @Test
    public void testProperties() throws Exception {
        assertThat(new TypeVariableToken(FOO, bound).getSymbol(), is(FOO));
        assertThat(new TypeVariableToken(FOO, bound).getBounds(), is(Collections.singletonList(bound)));
    }

    @Test
    public void testVisitor() throws Exception {
        assertThat(new TypeVariableToken(FOO, bound).accept(visitor), is(new TypeVariableToken(FOO, visitedBound)));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeVariableToken.class).apply();
    }
}
