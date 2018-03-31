package net.bytebuddy.agent.builder;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.utility.JavaModule;
import org.junit.Test;

import java.security.ProtectionDomain;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class AgentBuilderRawMatcherForLoadStateTest {

    @Test
    public void testLoadedOnLoaded() throws Exception {
        assertThat(AgentBuilder.RawMatcher.ForLoadState.LOADED.matches(mock(TypeDescription.class),
                mock(ClassLoader.class),
                mock(JavaModule.class),
                Object.class,
                mock(ProtectionDomain.class)), is(true));
    }

    @Test
    public void testLoadedOnUnloaded() throws Exception {
        assertThat(AgentBuilder.RawMatcher.ForLoadState.LOADED.matches(mock(TypeDescription.class),
                mock(ClassLoader.class),
                mock(JavaModule.class),
                null,
                mock(ProtectionDomain.class)), is(false));
    }

    @Test
    public void testUnloadedOnLoaded() throws Exception {
        assertThat(AgentBuilder.RawMatcher.ForLoadState.UNLOADED.matches(mock(TypeDescription.class),
                mock(ClassLoader.class),
                mock(JavaModule.class),
                Object.class,
                mock(ProtectionDomain.class)), is(false));
    }

    @Test
    public void testUnloadedOnUnloaded() throws Exception {
        assertThat(AgentBuilder.RawMatcher.ForLoadState.UNLOADED.matches(mock(TypeDescription.class),
                mock(ClassLoader.class),
                mock(JavaModule.class),
                null,
                mock(ProtectionDomain.class)), is(true));
    }
}
