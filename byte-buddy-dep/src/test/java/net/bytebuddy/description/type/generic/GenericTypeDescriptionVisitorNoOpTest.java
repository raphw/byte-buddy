package net.bytebuddy.description.type.generic;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class GenericTypeDescriptionVisitorNoOpTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription typeDescription;

    @Test
    public void testVisitGenericArray() throws Exception {
        assertThat(GenericTypeDescription.Visitor.NoOp.INSTANCE.onGenericArray(typeDescription), is((GenericTypeDescription) typeDescription));
    }

    @Test
    public void testVisitWildcard() throws Exception {
        assertThat(GenericTypeDescription.Visitor.NoOp.INSTANCE.onWildcard(typeDescription), is((GenericTypeDescription) typeDescription));
    }

    @Test
    public void testVisitParameterized() throws Exception {
        assertThat(GenericTypeDescription.Visitor.NoOp.INSTANCE.onParameterizedType(typeDescription), is((GenericTypeDescription) typeDescription));
    }

    @Test
    public void testVisitTypeVariable() throws Exception {
        assertThat(GenericTypeDescription.Visitor.NoOp.INSTANCE.onTypeVariable(typeDescription), is((GenericTypeDescription) typeDescription));
    }

    @Test
    public void testVisitNonGenericType() throws Exception {
        assertThat(GenericTypeDescription.Visitor.NoOp.INSTANCE.onNonGenericType(typeDescription), is((GenericTypeDescription) typeDescription));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(GenericTypeDescription.Visitor.NoOp.class).apply();
    }
}
