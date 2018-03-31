package net.bytebuddy.agent.builder;

import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.utility.JavaModule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class AgentBuilderLocationStrategySimpleTest {

    @Test
    public void testLocation() throws Exception {
        ClassFileLocator classFileLocator = mock(ClassFileLocator.class);
        assertThat(new AgentBuilder.LocationStrategy.Simple(classFileLocator).classFileLocator(mock(ClassLoader.class), mock(JavaModule.class)), is(classFileLocator));
    }
}
