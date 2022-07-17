package net.bytebuddy.agent.builder;

import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class AgentBuilderRedefinitionListenableResubmissionImmediateMatcherTest {

    private static final String FOO = "foo";

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private ClassLoader classLoader;

    @Mock
    private JavaModule module;

    @Mock
    private AgentBuilder.RedefinitionListenable.ResubmissionImmediateMatcher left;

    @Mock
    private AgentBuilder.RedefinitionListenable.ResubmissionImmediateMatcher right;

    @Test
    public void testTrivialMatching() {
        assertThat(AgentBuilder.RedefinitionListenable.ResubmissionImmediateMatcher.Trivial.MATCHING.matches(FOO,
                classLoader,
                module), is(true));
    }

    @Test
    public void testTrivialNonMatching() {
        assertThat(AgentBuilder.RedefinitionListenable.ResubmissionImmediateMatcher.Trivial.NON_MATCHING.matches(FOO,
                classLoader,
                module), is(false));
    }

    @Test
    public void testForElementMatchers() {
        assertThat(new AgentBuilder.RedefinitionListenable.ResubmissionImmediateMatcher.ForElementMatchers(ElementMatchers.<String>is(FOO),
                ElementMatchers.is(classLoader),
                ElementMatchers.is(module)).matches(FOO,
                classLoader,
                module), is(true));
    }

    @Test
    public void testConjunction() {
        when(left.matches(FOO, classLoader, module)).thenReturn(true);
        when(right.matches(FOO, classLoader, module)).thenReturn(true);

        assertThat(new AgentBuilder.RedefinitionListenable.ResubmissionImmediateMatcher.Conjunction(left, right).matches(FOO,
                classLoader,
                module), is(true));

        verify(left).matches(FOO, classLoader, module);
        verifyNoMoreInteractions(left);
        verify(right).matches(FOO, classLoader, module);
        verifyNoMoreInteractions(right);
    }

    @Test
    public void testDisjunction() {
        when(left.matches(FOO, classLoader, module)).thenReturn(true);
        when(right.matches(FOO, classLoader, module)).thenReturn(true);

        assertThat(new AgentBuilder.RedefinitionListenable.ResubmissionImmediateMatcher.Disjunction(left, right).matches(FOO,
                classLoader,
                module), is(true));

        verify(left).matches(FOO, classLoader, module);
        verifyNoMoreInteractions(left);
        verifyNoMoreInteractions(right);
    }
}
