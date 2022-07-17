package net.bytebuddy.implementation.bind;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import java.util.Collections;

import static org.mockito.Mockito.when;

public class MethodDelegationBinderProcessorTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private MethodDescription source;

    @Mock
    private MethodDelegationBinder.AmbiguityResolver ambiguityResolver;

    @Mock
    private MethodDelegationBinder.MethodBinding methodBinding;

    @Mock
    private MethodDelegationBinder.Record record;

    @Mock
    private MethodDelegationBinder.TerminationHandler terminationHandler;

    @Mock
    private MethodDelegationBinder.MethodInvoker methodInvoker;

    @Mock
    private Implementation.Target implementationTarget;

    @Mock
    private MethodDelegationBinder.BindingResolver bindingResolver;

    @Mock
    private Assigner assigner;

    @Test(expected = IllegalArgumentException.class)
    public void testNoBindableTarget() throws Exception {
        when(methodBinding.isValid()).thenReturn(false);
        when(record.bind(implementationTarget, source, terminationHandler, methodInvoker, assigner)).thenReturn(methodBinding);
        new MethodDelegationBinder.Processor(Collections.singletonList(record), ambiguityResolver, bindingResolver)
                .bind(implementationTarget, source, terminationHandler, methodInvoker, assigner);
    }
}
