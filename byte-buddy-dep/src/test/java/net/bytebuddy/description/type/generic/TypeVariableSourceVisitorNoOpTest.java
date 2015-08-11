package net.bytebuddy.description.type.generic;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class TypeVariableSourceVisitorNoOpTest {

    @Test
    public void testVisitType() throws Exception {
        TypeDescription typeDescription = mock(TypeDescription.class);
        assertThat(TypeVariableSource.Visitor.NoOp.INSTANCE.onType(typeDescription), is((TypeVariableSource) typeDescription));
    }

    @Test
    public void testVisitMethod() throws Exception {
        MethodDescription methodDescription = mock(MethodDescription.class);
        assertThat(TypeVariableSource.Visitor.NoOp.INSTANCE.onMethod(methodDescription), is((TypeVariableSource) methodDescription));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeVariableSource.Visitor.NoOp.class).apply();
    }
}
