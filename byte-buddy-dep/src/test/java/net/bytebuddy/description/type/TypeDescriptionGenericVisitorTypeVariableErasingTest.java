package net.bytebuddy.description.type;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class TypeDescriptionGenericVisitorTypeVariableErasingTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeDescription.Generic.Visitor.TypeVariableErasing.class).apply();
        ObjectPropertyAssertion.of(TypeDescription.Generic.Visitor.TypeVariableErasing.PartialErasureReviser.class).apply();
    }
}