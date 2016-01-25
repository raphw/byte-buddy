package net.bytebuddy.agent.builder;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.loading.ClassInjector;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.Collections;
import java.util.concurrent.Callable;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

public class AgentBuilderLambdaInstrumentationStrategyTest implements Callable<Class<?>> {

    @Test
    public void testEnabledStrategyNeverThrowsException() throws Exception {
        ClassFileTransformer initialClassFileTransformer = mock(ClassFileTransformer.class);
        assertThat(LambdaFactory.register(initialClassFileTransformer, mock(AgentBuilder.Default.LambdaInstrumentationStrategy.LambdaInstanceFactory.class), this), is(true));
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

    @Test(expected = IllegalStateException.class)
    public void testDisabledStrategyCannotInject() throws Exception {
        AgentBuilder.Default.LambdaInstrumentationStrategy.DISABLED.call();
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

    @Override
    public Class<?> call() throws Exception {
        TypeDescription lambdaFactory = new TypeDescription.ForLoadedType(LambdaFactory.class);
        return ClassInjector.UsingReflection.ofSystemClassLoader()
                .inject(Collections.singletonMap(lambdaFactory, ClassFileLocator.ForClassLoader.read(LambdaFactory.class).resolve()))
                .get(lambdaFactory);
    }
}
