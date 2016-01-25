package net.bytebuddy.agent.builder;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class AgentBuilderLambdaInstrumentationStrategyTest {

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
