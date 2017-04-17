package net.bytebuddy.agent.builder;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.utility.JavaModule;
import org.junit.Test;

import java.security.ProtectionDomain;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class AgentBuilderRawMatcherTrivialTest {

    @Test
    public void testMatches() throws Exception {
        assertThat(AgentBuilder.RawMatcher.Trivial.MATCHING.matches(mock(TypeDescription.class),
                mock(ClassLoader.class),
                mock(JavaModule.class),
                Void.class,
                mock(ProtectionDomain.class)), is(true));
    }

    @Test
    public void testMatchesNot() throws Exception {
        assertThat(AgentBuilder.RawMatcher.Trivial.NON_MATCHING.matches(mock(TypeDescription.class),
                mock(ClassLoader.class),
                mock(JavaModule.class),
                Void.class,
                mock(ProtectionDomain.class)), is(false));
    }
}
