package net.bytebuddy.agent.builder;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class ResettableClassFileTransformerWithDelegationTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private ResettableClassFileTransformer delegate;

    @Mock
    private Instrumentation instrumentation;

    @Test
    public void testResetViaDelegate() throws Exception {
        Sample sample = new Sample(delegate);
        when(delegate.reset(instrumentation,
                sample,
                AgentBuilder.RedefinitionStrategy.DISABLED,
                AgentBuilder.RedefinitionStrategy.DiscoveryStrategy.SinglePass.INSTANCE,
                AgentBuilder.RedefinitionStrategy.BatchAllocator.ForTotal.INSTANCE,
                AgentBuilder.RedefinitionStrategy.Listener.NoOp.INSTANCE)).thenReturn(true);
        assertThat(sample.reset(instrumentation, AgentBuilder.RedefinitionStrategy.DISABLED), is(true));
        verify(delegate).reset(instrumentation,
                sample,
                AgentBuilder.RedefinitionStrategy.DISABLED,
                AgentBuilder.RedefinitionStrategy.DiscoveryStrategy.SinglePass.INSTANCE,
                AgentBuilder.RedefinitionStrategy.BatchAllocator.ForTotal.INSTANCE,
                AgentBuilder.RedefinitionStrategy.Listener.NoOp.INSTANCE);
        verifyNoMoreInteractions(delegate);
    }

    private static class Sample extends ResettableClassFileTransformer.WithDelegation {

        private Sample(ResettableClassFileTransformer classFileTransformer) {
            super(classFileTransformer);
        }

        @Override
        public byte[] transform(ClassLoader loader,
                                String className,
                                Class<?> classBeingRedefined,
                                ProtectionDomain protectionDomain,
                                byte[] classfileBuffer) {
            throw new AssertionError();
        }
    }
}
