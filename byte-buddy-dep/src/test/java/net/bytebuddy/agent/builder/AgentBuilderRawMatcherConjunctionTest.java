package net.bytebuddy.agent.builder;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.utility.JavaModule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.security.ProtectionDomain;

import static net.bytebuddy.agent.builder.AgentBuilder.RawMatcher.Trivial.MATCHING;
import static net.bytebuddy.agent.builder.AgentBuilder.RawMatcher.Trivial.NON_MATCHING;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.*;
import static org.mockito.Mockito.*;

public class AgentBuilderRawMatcherConjunctionTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private AgentBuilder.RawMatcher left, right;

    @Mock
    private TypeDescription typeDescription;

    @Mock
    private ClassLoader classLoader;

    @Mock
    private JavaModule module;

    @Mock
    private ProtectionDomain protectionDomain;

    @Test
    public void testMatches() throws Exception {
        when(left.matches(typeDescription, classLoader, module, Foo.class, protectionDomain)).thenReturn(true);
        when(right.matches(typeDescription, classLoader, module, Foo.class, protectionDomain)).thenReturn(true);
        AgentBuilder.RawMatcher rawMatcher = AgentBuilder.RawMatcher.Composite.and(left, right);
        assertThat(rawMatcher.matches(typeDescription, classLoader, module, Foo.class, protectionDomain), is(true));
        verify(left).matches(typeDescription, classLoader, module, Foo.class, protectionDomain);
        verifyNoMoreInteractions(left);
        verify(right).matches(typeDescription, classLoader, module, Foo.class, protectionDomain);
        verifyNoMoreInteractions(right);
    }

    @Test
    public void testMatchesInlinedLeft() throws Exception {
        when(left.matches(typeDescription, classLoader, module, Foo.class, protectionDomain)).thenReturn(true);
        when(right.matches(typeDescription, classLoader, module, Foo.class, protectionDomain)).thenReturn(true);
        AgentBuilder.RawMatcher leaf = AgentBuilder.RawMatcher.Composite.and(left, right);
        AgentBuilder.RawMatcher rawMatcher = AgentBuilder.RawMatcher.Composite.and(leaf, MATCHING);
        assertThat(rawMatcher.matches(typeDescription, classLoader, module, Foo.class, protectionDomain), is(true));
        verify(left).matches(typeDescription, classLoader, module, Foo.class, protectionDomain);
        verifyNoMoreInteractions(left);
        verify(right).matches(typeDescription, classLoader, module, Foo.class, protectionDomain);
        verifyNoMoreInteractions(right);
    }

    @Test
    public void testNotMatchesInlinedLeft() throws Exception {
        when(left.matches(typeDescription, classLoader, module, Foo.class, protectionDomain)).thenReturn(true);
        when(right.matches(typeDescription, classLoader, module, Foo.class, protectionDomain)).thenReturn(true);
        AgentBuilder.RawMatcher leaf = AgentBuilder.RawMatcher.Composite.and(left, right);
        AgentBuilder.RawMatcher rawMatcher = AgentBuilder.RawMatcher.Composite.and(leaf, NON_MATCHING);
        assertThat(rawMatcher.matches(typeDescription, classLoader, module, Foo.class, protectionDomain), is(false));
        verify(left).matches(typeDescription, classLoader, module, Foo.class, protectionDomain);
        verifyNoMoreInteractions(left);
        verify(right).matches(typeDescription, classLoader, module, Foo.class, protectionDomain);
        verifyNoMoreInteractions(right);
    }

    @Test
    public void testMatchesInlinedRight() throws Exception {
        when(left.matches(typeDescription, classLoader, module, Foo.class, protectionDomain)).thenReturn(true);
        when(right.matches(typeDescription, classLoader, module, Foo.class, protectionDomain)).thenReturn(true);
        AgentBuilder.RawMatcher leaf = AgentBuilder.RawMatcher.Composite.and(left, right);
        AgentBuilder.RawMatcher rawMatcher = AgentBuilder.RawMatcher.Composite.and(MATCHING, leaf);
        assertThat(rawMatcher.matches(typeDescription, classLoader, module, Foo.class, protectionDomain), is(true));
        verify(left).matches(typeDescription, classLoader, module, Foo.class, protectionDomain);
        verifyNoMoreInteractions(left);
        verify(right).matches(typeDescription, classLoader, module, Foo.class, protectionDomain);
        verifyNoMoreInteractions(right);
    }

    @Test
    public void testNotMatchesInlinedRight() throws Exception {
        when(left.matches(typeDescription, classLoader, module, Foo.class, protectionDomain)).thenReturn(true);
        when(right.matches(typeDescription, classLoader, module, Foo.class, protectionDomain)).thenReturn(true);
        AgentBuilder.RawMatcher leaf = AgentBuilder.RawMatcher.Composite.and(left, right);
        AgentBuilder.RawMatcher rawMatcher = AgentBuilder.RawMatcher.Composite.and(NON_MATCHING, leaf);
        assertThat(rawMatcher.matches(typeDescription, classLoader, module, Foo.class, protectionDomain), is(false));
        verifyNoMoreInteractions(left);
        verifyNoMoreInteractions(right);
    }

    @Test
    public void testNotMatchesLeft() throws Exception {
        when(left.matches(typeDescription, classLoader, module, Foo.class, protectionDomain)).thenReturn(true);
        when(right.matches(typeDescription, classLoader, module, Foo.class, protectionDomain)).thenReturn(false);
        AgentBuilder.RawMatcher rawMatcher = AgentBuilder.RawMatcher.Composite.and(left, right);
        assertThat(rawMatcher.matches(typeDescription, classLoader, module, Foo.class, protectionDomain), is(false));
        verify(left).matches(typeDescription, classLoader, module, Foo.class, protectionDomain);
        verifyNoMoreInteractions(left);
        verify(right).matches(typeDescription, classLoader, module, Foo.class, protectionDomain);
        verifyNoMoreInteractions(right);
    }

    @Test
    public void testNotMatchesRight() throws Exception {
        when(left.matches(typeDescription, classLoader, module, Foo.class, protectionDomain)).thenReturn(false);
        when(right.matches(typeDescription, classLoader, module, Foo.class, protectionDomain)).thenReturn(true);
        AgentBuilder.RawMatcher rawMatcher = AgentBuilder.RawMatcher.Composite.and(left, right);
        assertThat(rawMatcher.matches(typeDescription, classLoader, module, Foo.class, protectionDomain), is(false));
        verify(left).matches(typeDescription, classLoader, module, Foo.class, protectionDomain);
        verifyNoMoreInteractions(left);
        verifyZeroInteractions(right);
    }

    @Test
    public void testNotMatchesEither() throws Exception {
        when(left.matches(typeDescription, classLoader, module, Foo.class, protectionDomain)).thenReturn(false);
        when(right.matches(typeDescription, classLoader, module, Foo.class, protectionDomain)).thenReturn(false);
        AgentBuilder.RawMatcher rawMatcher = AgentBuilder.RawMatcher.Composite.and(left, right);
        assertThat(rawMatcher.matches(typeDescription, classLoader, module, Foo.class, protectionDomain), is(false));
        verify(left).matches(typeDescription, classLoader, module, Foo.class, protectionDomain);
        verifyNoMoreInteractions(left);
        verifyZeroInteractions(right);
    }

    private static class Foo {
        /* empty */
    }
}
