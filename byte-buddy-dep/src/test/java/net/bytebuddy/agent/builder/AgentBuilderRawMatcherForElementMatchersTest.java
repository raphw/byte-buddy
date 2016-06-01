package net.bytebuddy.agent.builder;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import net.bytebuddy.utility.JavaModule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.security.ProtectionDomain;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class AgentBuilderRawMatcherForElementMatchersTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private ElementMatcher<TypeDescription> typeMatcher;

    @Mock
    private ElementMatcher<ClassLoader> classLoaderMatcher;

    @Mock
    private ElementMatcher<JavaModule> moduleMatcher;

    @Mock
    private TypeDescription typeDescription;

    @Mock
    private ClassLoader classLoader;

    @Mock
    private JavaModule module;

    @Mock
    private ProtectionDomain protectionDomain;

    @Test
    public void testNoneMatches() throws Exception {
        assertThat(new AgentBuilder.RawMatcher.ForElementMatchers(typeMatcher, classLoaderMatcher, moduleMatcher)
                .matches(typeDescription, classLoader, module, Object.class, protectionDomain), is(false));
        verify(moduleMatcher).matches(module);
        verifyNoMoreInteractions(moduleMatcher);
        verifyNoMoreInteractions(classLoaderMatcher);
        verifyZeroInteractions(typeMatcher);
    }

    @Test
    public void testModuleMatches() throws Exception {
        when(moduleMatcher.matches(module)).thenReturn(true);
        assertThat(new AgentBuilder.RawMatcher.ForElementMatchers(typeMatcher, classLoaderMatcher, moduleMatcher)
                .matches(typeDescription, classLoader, module, Object.class, protectionDomain), is(false));
        verify(moduleMatcher).matches(module);
        verifyNoMoreInteractions(moduleMatcher);
        verify(classLoaderMatcher).matches(classLoader);
        verifyNoMoreInteractions(classLoaderMatcher);
        verifyZeroInteractions(typeMatcher);
    }

    @Test
    public void testClassLoaderMatches() throws Exception {
        when(classLoaderMatcher.matches(classLoader)).thenReturn(true);
        assertThat(new AgentBuilder.RawMatcher.ForElementMatchers(typeMatcher, classLoaderMatcher, moduleMatcher)
                .matches(typeDescription, classLoader, module, Object.class, protectionDomain), is(false));
        verify(moduleMatcher).matches(module);
        verifyNoMoreInteractions(moduleMatcher);
        verifyZeroInteractions(classLoaderMatcher);
        verifyNoMoreInteractions(typeMatcher);
    }

    @Test
    public void testModuleAndClassLoaderMatches() throws Exception {
        when(moduleMatcher.matches(module)).thenReturn(true);
        when(classLoaderMatcher.matches(classLoader)).thenReturn(true);
        assertThat(new AgentBuilder.RawMatcher.ForElementMatchers(typeMatcher, classLoaderMatcher, moduleMatcher)
                .matches(typeDescription, classLoader, module, Object.class, protectionDomain), is(false));
        verify(moduleMatcher).matches(module);
        verifyNoMoreInteractions(moduleMatcher);
        verify(classLoaderMatcher).matches(classLoader);
        verifyNoMoreInteractions(classLoaderMatcher);
        verify(typeMatcher).matches(typeDescription);
        verifyNoMoreInteractions(typeMatcher);
    }

    @Test
    public void testModuleAndTypeMatches() throws Exception {
        when(moduleMatcher.matches(module)).thenReturn(true);
        when(typeMatcher.matches(typeDescription)).thenReturn(true);
        assertThat(new AgentBuilder.RawMatcher.ForElementMatchers(typeMatcher, classLoaderMatcher, moduleMatcher)
                .matches(typeDescription, classLoader, module, Object.class, protectionDomain), is(false));
        verify(moduleMatcher).matches(module);
        verifyNoMoreInteractions(moduleMatcher);
        verify(classLoaderMatcher).matches(classLoader);
        verifyNoMoreInteractions(classLoaderMatcher);
        verifyZeroInteractions(typeMatcher);
    }

    @Test
    public void testClassLoaderAndTypeMatches() throws Exception {
        when(classLoaderMatcher.matches(classLoader)).thenReturn(true);
        when(typeMatcher.matches(typeDescription)).thenReturn(true);
        assertThat(new AgentBuilder.RawMatcher.ForElementMatchers(typeMatcher, classLoaderMatcher, moduleMatcher)
                .matches(typeDescription, classLoader, module, Object.class, protectionDomain), is(false));
        verify(moduleMatcher).matches(module);
        verifyNoMoreInteractions(moduleMatcher);
        verifyZeroInteractions(classLoaderMatcher);
        verifyZeroInteractions(typeMatcher);
    }

    @Test
    public void testTypeMatches() throws Exception {
        when(typeMatcher.matches(typeDescription)).thenReturn(true);
        assertThat(new AgentBuilder.RawMatcher.ForElementMatchers(typeMatcher, classLoaderMatcher, moduleMatcher)
                .matches(typeDescription, classLoader, module, Object.class, protectionDomain), is(false));
        verify(moduleMatcher).matches(module);
        verifyNoMoreInteractions(moduleMatcher);
        verifyZeroInteractions(classLoaderMatcher);
        verifyZeroInteractions(typeMatcher);
    }

    @Test
    public void testAllMatches() throws Exception {
        when(moduleMatcher.matches(module)).thenReturn(true);
        when(classLoaderMatcher.matches(classLoader)).thenReturn(true);
        when(typeMatcher.matches(typeDescription)).thenReturn(true);
        assertThat(new AgentBuilder.RawMatcher.ForElementMatchers(typeMatcher, classLoaderMatcher, moduleMatcher)
                .matches(typeDescription, classLoader, module, Object.class, protectionDomain), is(true));
        verify(classLoaderMatcher).matches(classLoader);
        verifyNoMoreInteractions(classLoaderMatcher);
        verify(typeMatcher).matches(typeDescription);
        verifyNoMoreInteractions(typeMatcher);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AgentBuilder.RawMatcher.ForElementMatchers.class).apply();
    }
}
