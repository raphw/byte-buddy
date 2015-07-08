package net.bytebuddy.description.modifier;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class ModifierConstributorObjectPropertiesTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(EnumerationState.class).apply();
        ObjectPropertyAssertion.of(FieldManifestation.class).apply();
        ObjectPropertyAssertion.of(MethodArguments.class).apply();
        ObjectPropertyAssertion.of(MethodManifestation.class).apply();
        ObjectPropertyAssertion.of(Ownership.class).apply();
        ObjectPropertyAssertion.of(ParameterManifestation.class).apply();
        ObjectPropertyAssertion.of(ProvisioningState.class).apply();
        ObjectPropertyAssertion.of(SynchronizationState.class).apply();
        ObjectPropertyAssertion.of(SyntheticState.class).apply();
        ObjectPropertyAssertion.of(TypeManifestation.class).apply();
        ObjectPropertyAssertion.of(Visibility.class).apply();
    }
}
