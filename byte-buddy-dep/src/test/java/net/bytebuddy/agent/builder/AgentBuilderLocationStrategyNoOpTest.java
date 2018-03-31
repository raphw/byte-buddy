package net.bytebuddy.agent.builder;

import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.utility.JavaModule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class AgentBuilderLocationStrategyNoOpTest {

    @Test
    public void testApplication() throws Exception {
        assertThat(AgentBuilder.LocationStrategy.NoOp.INSTANCE.classFileLocator(mock(ClassLoader.class), mock(JavaModule.class)),
                is((ClassFileLocator) ClassFileLocator.NoOp.INSTANCE));
    }
}
