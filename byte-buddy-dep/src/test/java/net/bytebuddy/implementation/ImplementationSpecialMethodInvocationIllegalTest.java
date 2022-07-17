package net.bytebuddy.implementation;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.objectweb.asm.MethodVisitor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class ImplementationSpecialMethodInvocationIllegalTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private Implementation.Context implementationContext;

    @After
    public void tearDown() throws Exception {
        verifyNoMoreInteractions(methodVisitor);
        verifyNoMoreInteractions(implementationContext);
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
