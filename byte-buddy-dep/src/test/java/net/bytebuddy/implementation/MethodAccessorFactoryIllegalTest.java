package net.bytebuddy.implementation;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodAccessorFactory;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

public class MethodAccessorFactoryIllegalTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

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

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodAccessorFactory.Illegal.class).apply();
    }
}
