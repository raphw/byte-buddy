package net.bytebuddy.description.method;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class MethodDescriptionTypeSubstitutingVariableRetainingDelegatorTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodDescription.TypeSubstituting.VariableRetainingDelegator.class).apply();
    }
}