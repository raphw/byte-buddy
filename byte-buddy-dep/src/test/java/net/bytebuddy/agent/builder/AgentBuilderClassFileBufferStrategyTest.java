package net.bytebuddy.agent.builder;

import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.utility.JavaModule;
import org.junit.Test;

import java.security.ProtectionDomain;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;

public class AgentBuilderClassFileBufferStrategyTest {

    @Test
    public void testRetainingClassFileBufferStrategy() throws Exception {
        ClassFileLocator classFileLocator = AgentBuilder.ClassFileBufferStrategy.Default.RETAINING.resolve("foo",
                new byte[]{123},
                mock(ClassLoader.class),
                mock(JavaModule.class),
                mock(ProtectionDomain.class));

        assertThat(classFileLocator.locate("foo").isResolved(), is(true));
        assertThat(classFileLocator.locate("bar").isResolved(), is(false));
    }

    @Test
    public void testDiscardingClassFileBufferStrategy() throws Exception {
        ClassFileLocator classFileLocator = AgentBuilder.ClassFileBufferStrategy.Default.DISCARDING.resolve("foo",
                new byte[]{123},
                mock(ClassLoader.class),
                mock(JavaModule.class),
                mock(ProtectionDomain.class));

        assertThat(classFileLocator.locate("foo").isResolved(), is(false));
        assertThat(classFileLocator.locate("bar").isResolved(), is(false));
    }
}
