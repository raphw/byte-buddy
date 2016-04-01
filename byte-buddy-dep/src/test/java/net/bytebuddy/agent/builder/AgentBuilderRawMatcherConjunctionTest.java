package net.bytebuddy.agent.builder;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.security.ProtectionDomain;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
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
    private ProtectionDomain protectionDomain;

    @Test
    public void testMatches() throws Exception {
        when(left.matches(typeDescription, classLoader, Foo.class, protectionDomain)).thenReturn(true);
        when(right.matches(typeDescription, classLoader, Foo.class, protectionDomain)).thenReturn(true);
        AgentBuilder.RawMatcher rawMatcher = new AgentBuilder.RawMatcher.Conjunction(left, right);
        assertThat(rawMatcher.matches(typeDescription, classLoader, Foo.class, protectionDomain), is(true));
        verify(left).matches(typeDescription, classLoader, Foo.class, protectionDomain);
        verifyNoMoreInteractions(left);
        verify(right).matches(typeDescription, classLoader, Foo.class, protectionDomain);
        verifyNoMoreInteractions(right);
    }

    @Test
    public void testNotMatchesLeft() throws Exception {
        when(left.matches(typeDescription, classLoader, Foo.class, protectionDomain)).thenReturn(true);
        when(right.matches(typeDescription, classLoader, Foo.class, protectionDomain)).thenReturn(false);
        AgentBuilder.RawMatcher rawMatcher = new AgentBuilder.RawMatcher.Conjunction(left, right);
        assertThat(rawMatcher.matches(typeDescription, classLoader, Foo.class, protectionDomain), is(false));
        verify(left).matches(typeDescription, classLoader, Foo.class, protectionDomain);
        verifyNoMoreInteractions(left);
        verify(right).matches(typeDescription, classLoader, Foo.class, protectionDomain);
        verifyNoMoreInteractions(right);
    }

    @Test
    public void testNotMatchesRight() throws Exception {
        when(left.matches(typeDescription, classLoader, Foo.class, protectionDomain)).thenReturn(false);
        when(right.matches(typeDescription, classLoader, Foo.class, protectionDomain)).thenReturn(true);
        AgentBuilder.RawMatcher rawMatcher = new AgentBuilder.RawMatcher.Conjunction(left, right);
        assertThat(rawMatcher.matches(typeDescription, classLoader, Foo.class, protectionDomain), is(false));
        verify(left).matches(typeDescription, classLoader, Foo.class, protectionDomain);
        verifyNoMoreInteractions(left);
        verifyZeroInteractions(right);
    }

    @Test
    public void testNotMatchesEither() throws Exception {
        when(left.matches(typeDescription, classLoader, Foo.class, protectionDomain)).thenReturn(false);
        when(right.matches(typeDescription, classLoader, Foo.class, protectionDomain)).thenReturn(false);
        AgentBuilder.RawMatcher rawMatcher = new AgentBuilder.RawMatcher.Conjunction(left, right);
        assertThat(rawMatcher.matches(typeDescription, classLoader, Foo.class, protectionDomain), is(false));
        verify(left).matches(typeDescription, classLoader, Foo.class, protectionDomain);
        verifyNoMoreInteractions(left);
        verifyZeroInteractions(right);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AgentBuilder.RawMatcher.Conjunction.class).apply();
    }

    private static class Foo {
        /* empty */
    }
}