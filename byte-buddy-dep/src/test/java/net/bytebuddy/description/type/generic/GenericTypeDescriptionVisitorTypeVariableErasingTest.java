package net.bytebuddy.description.type.generic;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class GenericTypeDescriptionVisitorTypeVariableErasingTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(GenericTypeDescription.Visitor.TypeVariableErasing.class).apply();
        ObjectPropertyAssertion.of(GenericTypeDescription.Visitor.TypeVariableErasing.PartialErasureReviser.class).apply();
    }
}