package net.bytebuddy.agent.builder;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import net.bytebuddy.utility.JavaModule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.security.ProtectionDomain;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class AgentBuilderRawMatcherInversionTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private AgentBuilder.RawMatcher rawMatcher;

    @Mock
    private TypeDescription typeDescription;

    @Mock
    private ClassLoader classLoader;

    @Mock
    private JavaModule module;

    @Mock
    private ProtectionDomain protectionDomain;

    @Test
    public void testInversionTrue() throws Exception {
        when(rawMatcher.matches(typeDescription, classLoader, module, Object.class, protectionDomain)).thenReturn(true);
        assertThat(new AgentBuilder.RawMatcher.Inversion(rawMatcher).matches(typeDescription, classLoader, module, Object.class, protectionDomain), is(false));
    }

    @Test
    public void testInversionFalse() throws Exception {
        when(rawMatcher.matches(typeDescription, classLoader, module, Object.class, protectionDomain)).thenReturn(false);
        assertThat(new AgentBuilder.RawMatcher.Inversion(rawMatcher).matches(typeDescription, classLoader, module, Object.class, protectionDomain), is(true));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AgentBuilder.RawMatcher.Inversion.class).apply();
    }
}
