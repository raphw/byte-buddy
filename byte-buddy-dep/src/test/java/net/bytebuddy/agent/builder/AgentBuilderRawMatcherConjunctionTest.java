package net.bytebuddy.agent.builder;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.utility.JavaModule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import java.security.ProtectionDomain;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class AgentBuilderRawMatcherConjunctionTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

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
        AgentBuilder.RawMatcher rawMatcher = new AgentBuilder.RawMatcher.Conjunction(left, right);
        assertThat(rawMatcher.matches(typeDescription, classLoader, module, Foo.class, protectionDomain), is(true));
        verify(left).matches(typeDescription, classLoader, module, Foo.class, protectionDomain);
        verifyNoMoreInteractions(left);
        verify(right).matches(typeDescription, classLoader, module, Foo.class, protectionDomain);
        verifyNoMoreInteractions(right);
    }

    @Test
    public void testNotMatchesLeft() throws Exception {
        when(left.matches(typeDescription, classLoader, module, Foo.class, protectionDomain)).thenReturn(true);
        when(right.matches(typeDescription, classLoader, module, Foo.class, protectionDomain)).thenReturn(false);
        AgentBuilder.RawMatcher rawMatcher = new AgentBuilder.RawMatcher.Conjunction(left, right);
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
        AgentBuilder.RawMatcher rawMatcher = new AgentBuilder.RawMatcher.Conjunction(left, right);
        assertThat(rawMatcher.matches(typeDescription, classLoader, module, Foo.class, protectionDomain), is(false));
        verify(left).matches(typeDescription, classLoader, module, Foo.class, protectionDomain);
        verifyNoMoreInteractions(left);
        verifyNoMoreInteractions(right);
    }

    @Test
    public void testNotMatchesEither() throws Exception {
        when(left.matches(typeDescription, classLoader, module, Foo.class, protectionDomain)).thenReturn(false);
        when(right.matches(typeDescription, classLoader, module, Foo.class, protectionDomain)).thenReturn(false);
        AgentBuilder.RawMatcher rawMatcher = new AgentBuilder.RawMatcher.Conjunction(left, right);
        assertThat(rawMatcher.matches(typeDescription, classLoader, module, Foo.class, protectionDomain), is(false));
        verify(left).matches(typeDescription, classLoader, module, Foo.class, protectionDomain);
        verifyNoMoreInteractions(left);
        verifyNoMoreInteractions(right);
    }

    private static class Foo {
        /* empty */
    }
}
