package net.bytebuddy.agent.builder;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import java.lang.instrument.Instrumentation;

import static org.mockito.Mockito.when;

public class AgentBuilderPatchModeHandlerTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private Instrumentation instrumentation;

    @Mock
    private ResettableClassFileTransformer classFileTransformer;

    @Test
    public void testNoOp() {
        AgentBuilder.PatchMode.Handler.NoOp.INSTANCE.onBeforeRegistration(instrumentation);
        AgentBuilder.PatchMode.Handler.NoOp.INSTANCE.onAfterRegistration(instrumentation);
    }

    @Test
    public void testGap() {
        AgentBuilder.PatchMode.Handler handler = new AgentBuilder.PatchMode.Handler.ForPatchWithGap(classFileTransformer);
        when(classFileTransformer.reset(instrumentation, AgentBuilder.RedefinitionStrategy.DISABLED)).thenReturn(true);
        handler.onBeforeRegistration(instrumentation);
        handler.onAfterRegistration(instrumentation);
    }

    @Test
    public void testOverlap() {
        AgentBuilder.PatchMode.Handler handler = new AgentBuilder.PatchMode.Handler.ForPatchWithOverlap(classFileTransformer);
        when(classFileTransformer.reset(instrumentation, AgentBuilder.RedefinitionStrategy.DISABLED)).thenReturn(true);
        handler.onBeforeRegistration(instrumentation);
        handler.onAfterRegistration(instrumentation);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGapError() {
        AgentBuilder.PatchMode.Handler handler = new AgentBuilder.PatchMode.Handler.ForPatchWithGap(classFileTransformer);
        handler.onBeforeRegistration(instrumentation);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testOverlapError() {
        AgentBuilder.PatchMode.Handler handler = new AgentBuilder.PatchMode.Handler.ForPatchWithOverlap(classFileTransformer);
        handler.onAfterRegistration(instrumentation);
    }
}
