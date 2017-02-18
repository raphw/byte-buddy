package net.bytebuddy.agent.builder;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

public class AgentBuilderLambdaInstrumentationStrategyTest {

    @Test
    public void testEnabled() throws Exception {
        assertThat(AgentBuilder.LambdaInstrumentationStrategy.of(true).isEnabled(), is(true));
        assertThat(AgentBuilder.LambdaInstrumentationStrategy.of(false).isEnabled(), is(false));
    }

    @Test
    public void testEnabledStrategyNeverThrowsException() throws Exception {
        ClassFileTransformer initialClassFileTransformer = mock(ClassFileTransformer.class);
        assertThat(LambdaFactory.register(initialClassFileTransformer,
                mock(AgentBuilder.Default.LambdaInstrumentationStrategy.LambdaInstanceFactory.class)), is(true));
        try {
            ByteBuddy byteBuddy = mock(ByteBuddy.class);
            Instrumentation instrumentation = mock(Instrumentation.class);
            ClassFileTransformer classFileTransformer = mock(ClassFileTransformer.class);
            try {
                AgentBuilder.Default.LambdaInstrumentationStrategy.ENABLED.apply(byteBuddy, instrumentation, classFileTransformer);
            } finally {
                assertThat(LambdaFactory.release(classFileTransformer), is(false));
            }
        } finally {
            assertThat(LambdaFactory.release(initialClassFileTransformer), is(true));
        }
    }

    @Test
    public void testDisabledStrategyIsNoOp() throws Exception {
        ByteBuddy byteBuddy = mock(ByteBuddy.class);
        Instrumentation instrumentation = mock(Instrumentation.class);
        ClassFileTransformer classFileTransformer = mock(ClassFileTransformer.class);
        AgentBuilder.Default.LambdaInstrumentationStrategy.DISABLED.apply(byteBuddy, instrumentation, classFileTransformer);
        verifyZeroInteractions(byteBuddy);
        verifyZeroInteractions(instrumentation);
        verifyZeroInteractions(classFileTransformer);
    }

    @Test
    public void testEnabledIsInstrumented() throws Exception {
        assertThat(AgentBuilder.LambdaInstrumentationStrategy.ENABLED.isInstrumented(Object.class), is(true));
        assertThat(AgentBuilder.LambdaInstrumentationStrategy.ENABLED.isInstrumented(null), is(true));
    }

    @Test
    public void testDisabledIsInstrumented() throws Exception {
        assertThat(AgentBuilder.LambdaInstrumentationStrategy.DISABLED.isInstrumented(Object.class), is(true));
        assertThat(AgentBuilder.LambdaInstrumentationStrategy.DISABLED.isInstrumented(null), is(true));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AgentBuilder.Default.LambdaInstrumentationStrategy.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.Default.LambdaInstrumentationStrategy.MetaFactoryRedirection.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.Default.LambdaInstrumentationStrategy.AlternativeMetaFactoryRedirection.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.Default.LambdaInstrumentationStrategy.LambdaInstanceFactory.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.Default.LambdaInstrumentationStrategy.LambdaInstanceFactory.BridgeMethodImplementation.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.Default.LambdaInstrumentationStrategy.LambdaInstanceFactory.BridgeMethodImplementation.Appender.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.Default.LambdaInstrumentationStrategy.LambdaInstanceFactory.ConstructorImplementation.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.Default.LambdaInstrumentationStrategy.LambdaInstanceFactory.ConstructorImplementation.Appender.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.Default.LambdaInstrumentationStrategy.LambdaInstanceFactory.FactoryImplementation.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.Default.LambdaInstrumentationStrategy.LambdaInstanceFactory.FactoryImplementation.Appender.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.Default.LambdaInstrumentationStrategy.LambdaInstanceFactory.FactoryImplementation.Appender.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.Default.LambdaInstrumentationStrategy.LambdaInstanceFactory.LambdaMethodImplementation.Appender.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.Default.LambdaInstrumentationStrategy.LambdaInstanceFactory.SerializationImplementation.class).apply();
    }
}
