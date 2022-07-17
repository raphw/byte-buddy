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
import static org.mockito.Mockito.when;

public class AgentBuilderRawMatcherInversionTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

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
}
