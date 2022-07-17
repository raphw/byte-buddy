package net.bytebuddy.agent.builder;

import net.bytebuddy.description.method.MethodDescription;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class AgentBuilderDefaultNativeMethodStrategyTest {

    private static final String FOO = "foo", BAR = "bar";

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private MethodDescription methodDescription;

    @Mock
    private Instrumentation instrumentation;

    @Mock
    private ClassFileTransformer classFileTransformer;

    @Before
    public void setUp() throws Exception {
        when(methodDescription.getInternalName()).thenReturn(BAR);
    }

    @Test
    public void testDisabledStrategySuffixesNames() throws Exception {
        assertThat(AgentBuilder.Default.NativeMethodStrategy.Disabled.INSTANCE.resolve().transform(methodDescription), startsWith(BAR));
        assertThat(AgentBuilder.Default.NativeMethodStrategy.Disabled.INSTANCE.resolve().transform(methodDescription), not(BAR));
    }

    @Test
    public void testDisabledStrategyApply() throws Exception {
        AgentBuilder.Default.NativeMethodStrategy.Disabled.INSTANCE.apply(instrumentation, classFileTransformer);
        verifyNoMoreInteractions(instrumentation);
        verifyNoMoreInteractions(classFileTransformer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEnabledStrategyMustNotBeEmptyString() throws Exception {
        AgentBuilder.Default.NativeMethodStrategy.ForPrefix.of("");
    }

    @Test
    public void testEnabledStrategySuffixesNames() throws Exception {
        assertThat(new AgentBuilder.Default.NativeMethodStrategy.ForPrefix(FOO).resolve().transform(methodDescription), is(FOO + BAR));
    }

    @Test
    public void testEnabledStrategyApplySupported() throws Exception {
        when(instrumentation.isNativeMethodPrefixSupported()).thenReturn(true);
        new AgentBuilder.Default.NativeMethodStrategy.ForPrefix(FOO).apply(instrumentation, classFileTransformer);
        verify(instrumentation).isNativeMethodPrefixSupported();
        verify(instrumentation).setNativeMethodPrefix(classFileTransformer, FOO);
        verifyNoMoreInteractions(instrumentation);
        verifyNoMoreInteractions(classFileTransformer);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEnabledStrategyApplyNotSupported() throws Exception {
        when(instrumentation.isNativeMethodPrefixSupported()).thenReturn(false);
        new AgentBuilder.Default.NativeMethodStrategy.ForPrefix(FOO).apply(instrumentation, classFileTransformer);
    }
}
