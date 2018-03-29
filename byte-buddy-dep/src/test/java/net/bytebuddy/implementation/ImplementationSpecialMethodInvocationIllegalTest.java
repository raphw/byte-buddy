package net.bytebuddy.implementation;

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

public class ImplementationSpecialMethodInvocationIllegalTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Implementation.Context implementationContext;

    @After
    public void tearDown() throws Exception {
        verifyZeroInteractions(methodVisitor);
        verifyZeroInteractions(implementationContext);
    }

    @Test
    public void testIsInvalid() throws Exception {
        assertThat(Implementation.SpecialMethodInvocation.Illegal.INSTANCE.isValid(), is(false));
    }

    @Test(expected = IllegalStateException.class)
    public void testMethodDescriptionIllegal() throws Exception {
        Implementation.SpecialMethodInvocation.Illegal.INSTANCE.getMethodDescription();
    }

    @Test(expected = IllegalStateException.class)
    public void testTypeDescriptionIllegal() throws Exception {
        Implementation.SpecialMethodInvocation.Illegal.INSTANCE.getTypeDescription();
    }

    @Test(expected = IllegalStateException.class)
    public void testApplicationIllegal() throws Exception {
        Implementation.SpecialMethodInvocation.Illegal.INSTANCE.apply(methodVisitor, implementationContext);
    }
}
