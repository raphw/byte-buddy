package net.bytebuddy.implementation.bind;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MethodDelegationBinderTest {

    @Test
    public void testObjectProperties() throws Exception {
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
                when(methodDescription.getParameters()).thenReturn(new ParameterList.Empty());
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
