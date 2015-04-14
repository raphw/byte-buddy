package net.bytebuddy.agent.builder;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.security.ProtectionDomain;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class AgentBuilderRawMatcherForElementMatcherPairTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private ElementMatcher<TypeDescription> typeMatcher;

    @Mock
    private ElementMatcher<ClassLoader> classLoaderMatcher;

    @Mock
    private TypeDescription typeDescription;

    @Mock
    private ClassLoader classLoader;

    @Mock
    private ProtectionDomain protectionDomain;

    @Test
    public void testNoneMatches() throws Exception {
        assertThat(new AgentBuilder.RawMatcher.ForElementMatcherPair(typeMatcher, classLoaderMatcher)
                .matches(typeDescription, classLoader, Object.class, protectionDomain), is(false));
        verify(classLoaderMatcher).matches(classLoader);
        verifyNoMoreInteractions(classLoaderMatcher);
        verifyZeroInteractions(typeMatcher);
    }

    @Test
    public void testClassLoaderMatches() throws Exception {
        when(classLoaderMatcher.matches(classLoader)).thenReturn(true);
        assertThat(new AgentBuilder.RawMatcher.ForElementMatcherPair(typeMatcher, classLoaderMatcher)
                .matches(typeDescription, classLoader, Object.class, protectionDomain), is(false));
        verify(classLoaderMatcher).matches(classLoader);
        verifyNoMoreInteractions(classLoaderMatcher);
        verify(typeMatcher).matches(typeDescription);
        verifyNoMoreInteractions(typeMatcher);
    }

    @Test
    public void testTypeMatches() throws Exception {
        when(typeMatcher.matches(typeDescription)).thenReturn(true);
        assertThat(new AgentBuilder.RawMatcher.ForElementMatcherPair(typeMatcher, classLoaderMatcher)
                .matches(typeDescription, classLoader, Object.class, protectionDomain), is(false));
        verify(classLoaderMatcher).matches(classLoader);
        verifyNoMoreInteractions(classLoaderMatcher);
        verifyZeroInteractions(typeMatcher);
    }

    @Test
    public void testBothMatches() throws Exception {
        when(classLoaderMatcher.matches(classLoader)).thenReturn(true);
        when(typeMatcher.matches(typeDescription)).thenReturn(true);
        assertThat(new AgentBuilder.RawMatcher.ForElementMatcherPair(typeMatcher, classLoaderMatcher)
                .matches(typeDescription, classLoader, Object.class, protectionDomain), is(true));
        verify(classLoaderMatcher).matches(classLoader);
        verifyNoMoreInteractions(classLoaderMatcher);
        verify(typeMatcher).matches(typeDescription);
        verifyNoMoreInteractions(typeMatcher);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AgentBuilder.RawMatcher.ForElementMatcherPair.class).apply();
    }
}
