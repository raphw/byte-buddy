package net.bytebuddy.description.type;

import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TypeDescriptionGenericVisitorNoOpTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription.Generic typeDescription;

    @Test
    public void testVisitGenericArray() throws Exception {
        assertThat(TypeDescription.Generic.Visitor.NoOp.INSTANCE.onGenericArray(typeDescription), is(typeDescription));
    }

    @Test
    public void testVisitWildcard() throws Exception {
        assertThat(TypeDescription.Generic.Visitor.NoOp.INSTANCE.onWildcard(typeDescription), is(typeDescription));
    }

    @Test
    public void testVisitParameterized() throws Exception {
        assertThat(TypeDescription.Generic.Visitor.NoOp.INSTANCE.onParameterizedType(typeDescription), is(typeDescription));
    }

    @Test
    public void testVisitTypeVariable() throws Exception {
        assertThat(TypeDescription.Generic.Visitor.NoOp.INSTANCE.onTypeVariable(typeDescription), is(typeDescription));
    }

    @Test
    public void testVisitNonGenericType() throws Exception {
        assertThat(TypeDescription.Generic.Visitor.NoOp.INSTANCE.onNonGenericType(typeDescription), is(typeDescription));
    }
}
