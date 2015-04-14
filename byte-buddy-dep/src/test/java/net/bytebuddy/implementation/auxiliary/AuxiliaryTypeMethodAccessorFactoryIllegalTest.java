package net.bytebuddy.implementation.auxiliary;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

public class AuxiliaryTypeMethodAccessorFactoryIllegalTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private Implementation.SpecialMethodInvocation specialMethodInvocation;

    @Mock
    private FieldDescription fieldDescription;

    @Test(expected = IllegalStateException.class)
    public void testAccessorIsIllegal() throws Exception {
        AuxiliaryType.MethodAccessorFactory.Illegal.INSTANCE.registerAccessorFor(specialMethodInvocation);
    }

    @Test(expected = IllegalStateException.class)
    public void testGetterIsIllegal() throws Exception {
        AuxiliaryType.MethodAccessorFactory.Illegal.INSTANCE.registerSetterFor(fieldDescription);
    }

    @Test(expected = IllegalStateException.class)
    public void testSetterIsIllegal() throws Exception {
        AuxiliaryType.MethodAccessorFactory.Illegal.INSTANCE.registerGetterFor(fieldDescription);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AuxiliaryType.MethodAccessorFactory.Illegal.class).apply();
    }
}
