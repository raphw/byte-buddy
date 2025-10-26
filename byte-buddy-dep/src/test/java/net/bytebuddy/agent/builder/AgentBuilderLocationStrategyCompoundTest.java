package net.bytebuddy.agent.builder;

import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.utility.JavaModule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import static net.bytebuddy.test.utility.FieldByFieldComparison.hasPrototype;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class AgentBuilderLocationStrategyCompoundTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private AgentBuilder.LocationStrategy first, second;

    @Mock
    private ClassFileLocator firstLocator, secondLocator;

    @Mock
    private ClassLoader classLoader;

    @Mock
    private JavaModule module;

    @Test
    public void testApplication() throws Exception {
        AgentBuilder.LocationStrategy locationStrategy = new AgentBuilder.LocationStrategy.Compound(first, second);
        when(first.classFileLocator(classLoader, module)).thenReturn(firstLocator);
        when(second.classFileLocator(classLoader, module)).thenReturn(secondLocator);
        assertThat(locationStrategy.classFileLocator(classLoader, module), hasPrototype((ClassFileLocator) new ClassFileLocator.Compound(firstLocator, secondLocator)));
        verify(first).classFileLocator(classLoader, module);
        verifyNoMoreInteractions(first);
        verify(second).classFileLocator(classLoader, module);
        verifyNoMoreInteractions(second);
    }
}
