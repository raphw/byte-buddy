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

public class AgentBuilderRedefinitionListenableResubmissionOnErrorMatcherTest {

    private static final String FOO = "foo";

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private Throwable throwable;

    @Mock
    private ClassLoader classLoader;

    @Mock
    private JavaModule module;

    @Mock
    private AgentBuilder.RedefinitionListenable.ResubmissionOnErrorMatcher left;

    @Mock
    private AgentBuilder.RedefinitionListenable.ResubmissionOnErrorMatcher right;

    @Test
    public void testTrivialMatching() {
        assertThat(AgentBuilder.RedefinitionListenable.ResubmissionOnErrorMatcher.Trivial.MATCHING.matches(throwable,
                FOO,
                classLoader,
                module), is(true));
    }

    @Test
    public void testTrivialNonMatching() {
        assertThat(AgentBuilder.RedefinitionListenable.ResubmissionOnErrorMatcher.Trivial.NON_MATCHING.matches(throwable,
                FOO,
                classLoader,
                module), is(false));
    }

    @Test
    public void testForElementMatchers() {
        assertThat(new AgentBuilder.RedefinitionListenable.ResubmissionOnErrorMatcher.ForElementMatchers(ElementMatchers.is(throwable),
                ElementMatchers.<String>is(FOO),
                ElementMatchers.is(classLoader),
                ElementMatchers.is(module)).matches(throwable,
                FOO,
                classLoader,
                module), is(true));
    }

    @Test
    public void testConjunction() {
        when(left.matches(throwable, FOO, classLoader, module)).thenReturn(true);
        when(right.matches(throwable, FOO, classLoader, module)).thenReturn(true);

        assertThat(new AgentBuilder.RedefinitionListenable.ResubmissionOnErrorMatcher.Conjunction(left, right).matches(throwable,
                FOO,
                classLoader,
                module), is(true));

        verify(left).matches(throwable, FOO, classLoader, module);
        verifyNoMoreInteractions(left);
        verify(right).matches(throwable, FOO, classLoader, module);
        verifyNoMoreInteractions(right);
    }

    @Test
    public void testDisjunction() {
        when(left.matches(throwable, FOO, classLoader, module)).thenReturn(true);
        when(right.matches(throwable, FOO, classLoader, module)).thenReturn(true);

        assertThat(new AgentBuilder.RedefinitionListenable.ResubmissionOnErrorMatcher.Disjunction(left, right).matches(throwable,
                FOO,
                classLoader,
                module), is(true));

        verify(left).matches(throwable, FOO, classLoader, module);
        verifyNoMoreInteractions(left);
        verifyNoMoreInteractions(right);
    }
}
