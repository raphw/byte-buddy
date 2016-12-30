package net.bytebuddy.implementation.bind;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;
import org.objectweb.asm.MethodVisitor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        assertThat(MethodDelegationBinder.Compiled.Ignored.INSTANCE.bind(mock(Implementation.Target.class), mock(MethodDescription.class)).isValid(), is(false));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodDelegationBinder.Compiled.Ignored.class).apply();
        ObjectPropertyAssertion.of(MethodDelegationBinder.MethodInvoker.Simple.class).apply();
        ObjectPropertyAssertion.of(MethodDelegationBinder.MethodInvoker.Virtual.class).apply();
        ObjectPropertyAssertion.of(MethodDelegationBinder.ParameterBinding.Illegal.class).apply();
        ObjectPropertyAssertion.of(MethodDelegationBinder.ParameterBinding.Anonymous.class).ignoreFields("anonymousToken").apply();
        ObjectPropertyAssertion.of(MethodDelegationBinder.ParameterBinding.Unique.class).apply();
        ObjectPropertyAssertion.of(MethodDelegationBinder.MethodBinding.Illegal.class).apply();
        ObjectPropertyAssertion.of(MethodDelegationBinder.MethodBinding.Builder.class).create(new ObjectPropertyAssertion.Creator<MethodDescription>() {
            @Override
            public MethodDescription create() {
                MethodDescription methodDescription = mock(MethodDescription.class);
                when(methodDescription.getParameters()).thenReturn((ParameterList) new ParameterList.Empty<ParameterDescription>());
                return methodDescription;
            }
        }).applyBasic();
        ObjectPropertyAssertion.of(MethodDelegationBinder.MethodBinding.Builder.Build.class).create(new ObjectPropertyAssertion.Creator<Map<?, ?>>() {
            @Override
            public Map<?, ?> create() {
                return Collections.singletonMap(new Object(), new Object());
            }
        }).create(new ObjectPropertyAssertion.Creator<List<?>>() {
            @Override
            public List<?> create() {
                return Collections.singletonList(new Object());
            }
        }).apply();
        ObjectPropertyAssertion.of(MethodDelegationBinder.AmbiguityResolver.Resolution.class).apply();
        ObjectPropertyAssertion.of(MethodDelegationBinder.AmbiguityResolver.NoOp.class).apply();
        ObjectPropertyAssertion.of(MethodDelegationBinder.AmbiguityResolver.Chain.class).apply();
    }
}
