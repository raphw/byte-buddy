package net.bytebuddy.agent.builder;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import java.lang.instrument.Instrumentation;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class AgentBuilderPatchModeHandlerTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private Instrumentation instrumentation;

    @Mock
    private ResettableClassFileTransformer.Substitutable classFileTransformer;

    @Mock
    private ResettableClassFileTransformer other;

    @Test
    public void testNoOp() {
        AgentBuilder.PatchMode.Handler.NoOp.INSTANCE.onBeforeRegistration(instrumentation);
        assertThat(AgentBuilder.PatchMode.Handler.NoOp.INSTANCE.onRegistration(classFileTransformer), is(true));
        AgentBuilder.PatchMode.Handler.NoOp.INSTANCE.onAfterRegistration(instrumentation);
        verifyNoMoreInteractions(classFileTransformer);
    }

    @Test
    public void testGap() {
        AgentBuilder.PatchMode.Handler handler = AgentBuilder.PatchMode.GAP.toHandler(classFileTransformer);
        when(classFileTransformer.reset(instrumentation, AgentBuilder.RedefinitionStrategy.DISABLED)).thenReturn(true);
        handler.onBeforeRegistration(instrumentation);
        assertThat(handler.onRegistration(classFileTransformer), is(true));
        handler.onAfterRegistration(instrumentation);
        verify(classFileTransformer).reset(instrumentation, AgentBuilder.RedefinitionStrategy.DISABLED);
        verifyNoMoreInteractions(classFileTransformer);
    }

    @Test
    public void testOverlap() {
        AgentBuilder.PatchMode.Handler handler = AgentBuilder.PatchMode.OVERLAP.toHandler(classFileTransformer);
        when(classFileTransformer.reset(instrumentation, AgentBuilder.RedefinitionStrategy.DISABLED)).thenReturn(true);
        handler.onBeforeRegistration(instrumentation);
        assertThat(handler.onRegistration(classFileTransformer), is(true));
        handler.onAfterRegistration(instrumentation);
        verify(classFileTransformer).reset(instrumentation, AgentBuilder.RedefinitionStrategy.DISABLED);
        verifyNoMoreInteractions(classFileTransformer);
    }

    @Test
    public void testSubstitute() {
        AgentBuilder.PatchMode.Handler handler = AgentBuilder.PatchMode.SUBSTITUTE.toHandler(classFileTransformer);
        handler.onBeforeRegistration(instrumentation);
        assertThat(handler.onRegistration(other), is(false));
        handler.onAfterRegistration(instrumentation);
        verify(classFileTransformer).substitute(other);
        verifyNoMoreInteractions(classFileTransformer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGapError() {
        AgentBuilder.PatchMode.Handler handler = AgentBuilder.PatchMode.GAP.toHandler(classFileTransformer);
        handler.onBeforeRegistration(instrumentation);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testOverlapError() {
        AgentBuilder.PatchMode.Handler handler = AgentBuilder.PatchMode.OVERLAP.toHandler(classFileTransformer);
        handler.onAfterRegistration(instrumentation);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubstituteError() {
        AgentBuilder.PatchMode.SUBSTITUTE.toHandler(other);
    }
}
