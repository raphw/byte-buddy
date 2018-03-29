package net.bytebuddy.implementation.bind;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import org.junit.Test;
import org.objectweb.asm.MethodVisitor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class MethodDelegationBinderTest {

    @Test
    public void testIllegalBindingInInvalid() throws Exception {
        assertThat(MethodDelegationBinder.MethodBinding.Illegal.INSTANCE.isValid(), is(false));
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalBindingParameterIndexThrowsException() throws Exception {
        MethodDelegationBinder.MethodBinding.Illegal.INSTANCE.getTargetParameterIndex(mock(Object.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalBindingApplicationThrowsException() throws Exception {
        MethodDelegationBinder.MethodBinding.Illegal.INSTANCE.apply(mock(MethodVisitor.class), mock(Implementation.Context.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalBindingTargetThrowsException() throws Exception {
        MethodDelegationBinder.MethodBinding.Illegal.INSTANCE.getTarget();
    }

    @Test
    public void testIgnored() throws Exception {
        assertThat(MethodDelegationBinder.Record.Illegal.INSTANCE.bind(mock(Implementation.Target.class),
                mock(MethodDescription.class),
                mock(MethodDelegationBinder.TerminationHandler.class),
                mock(MethodDelegationBinder.MethodInvoker.class),
                mock(Assigner.class)).isValid(), is(false));
    }
}
