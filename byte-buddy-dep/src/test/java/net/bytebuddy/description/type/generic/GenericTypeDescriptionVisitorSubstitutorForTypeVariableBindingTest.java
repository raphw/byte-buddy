package net.bytebuddy.description.type.generic;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class GenericTypeDescriptionVisitorSubstitutorForTypeVariableBindingTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(GenericTypeDescription.Visitor.Substitutor.ForTypeVariableBinding.class).apply();
    }
}