package net.bytebuddy.instrumentation.type.auxiliary;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.field.FieldDescription;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

public class AuxiliaryTypeMethodAccessorFactoryIllegalTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private Instrumentation.SpecialMethodInvocation specialMethodInvocation;

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
}
