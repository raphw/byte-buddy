package net.bytebuddy.instrumentation.type.auxiliary;

import net.bytebuddy.instrumentation.Instrumentation;
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

    @Test(expected = IllegalStateException.class)
    public void testAccessIsIllegal() throws Exception {
        AuxiliaryType.MethodAccessorFactory.Illegal.INSTANCE.registerAccessorFor(specialMethodInvocation);
    }
}
