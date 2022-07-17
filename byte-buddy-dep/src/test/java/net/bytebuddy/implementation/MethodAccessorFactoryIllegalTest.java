package net.bytebuddy.implementation;

import net.bytebuddy.description.field.FieldDescription;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

public class MethodAccessorFactoryIllegalTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private Implementation.SpecialMethodInvocation specialMethodInvocation;

    @Mock
    private FieldDescription fieldDescription;

    @Test(expected = IllegalStateException.class)
    public void testAccessorIsIllegal() throws Exception {
        MethodAccessorFactory.Illegal.INSTANCE.registerAccessorFor(specialMethodInvocation, MethodAccessorFactory.AccessType.DEFAULT);
    }

    @Test(expected = IllegalStateException.class)
    public void testGetterIsIllegal() throws Exception {
        MethodAccessorFactory.Illegal.INSTANCE.registerSetterFor(fieldDescription, MethodAccessorFactory.AccessType.DEFAULT);
    }

    @Test(expected = IllegalStateException.class)
    public void testSetterIsIllegal() throws Exception {
        MethodAccessorFactory.Illegal.INSTANCE.registerGetterFor(fieldDescription, MethodAccessorFactory.AccessType.DEFAULT);
    }
}
