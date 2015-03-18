package net.bytebuddy.instrumentation;

import net.bytebuddy.test.utility.MockitoRule;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verifyZeroInteractions;

public class InstrumentationSpecialMethodInvocationIllegalTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Instrumentation.Context instrumentationContext;

    @After
    public void tearDown() throws Exception {
        verifyZeroInteractions(methodVisitor);
        verifyZeroInteractions(instrumentationContext);
    }

    @Test
    public void testIsInvalid() throws Exception {
        assertThat(Instrumentation.SpecialMethodInvocation.Illegal.INSTANCE.isValid(), is(false));
    }

    @Test(expected = IllegalStateException.class)
    public void testMethodDescriptionIllegal() throws Exception {
        Instrumentation.SpecialMethodInvocation.Illegal.INSTANCE.getMethodDescription();
    }

    @Test(expected = IllegalStateException.class)
    public void testTypeDescriptionIllegal() throws Exception {
        Instrumentation.SpecialMethodInvocation.Illegal.INSTANCE.getTypeDescription();
    }

    @Test(expected = IllegalStateException.class)
    public void testApplicationIllegal() throws Exception {
        Instrumentation.SpecialMethodInvocation.Illegal.INSTANCE.apply(methodVisitor, instrumentationContext);
    }
}
