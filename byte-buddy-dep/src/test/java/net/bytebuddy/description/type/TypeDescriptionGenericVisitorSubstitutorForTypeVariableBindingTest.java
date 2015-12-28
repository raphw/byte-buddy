package net.bytebuddy.description.type;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class TypeDescriptionGenericVisitorSubstitutorForTypeVariableBindingTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeDescription.Generic.Visitor.Substitutor.ForTypeVariableBinding.class).apply();
    }
}