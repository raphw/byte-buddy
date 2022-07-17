package net.bytebuddy.agent.builder;

import net.bytebuddy.dynamic.loading.ClassInjector;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.test.utility.ClassJnaInjectionAvailableRule;
import net.bytebuddy.test.utility.ClassReflectionInjectionAvailableRule;
import net.bytebuddy.test.utility.ClassUnsafeInjectionAvailableRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import static net.bytebuddy.test.utility.FieldByFieldComparison.hasPrototype;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AgentBuilderInjectionStrategyTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Rule
    public MethodRule classInjectionAvailableRule = new ClassReflectionInjectionAvailableRule();

    @Rule
    public MethodRule classUnsafeInjectionAvailableRule = new ClassUnsafeInjectionAvailableRule();

    @Rule
    public MethodRule classJnaInjectionAvailableRule = new ClassJnaInjectionAvailableRule();

    @Mock
    private ClassLoader classLoader;

    @Mock
    private ProtectionDomain protectionDomain;

    @Test(expected = IllegalStateException.class)
    public void testDisabled() throws Exception {
        AgentBuilder.InjectionStrategy.Disabled.INSTANCE.resolve(classLoader, protectionDomain);
    }

    @Test
    @ClassReflectionInjectionAvailableRule.Enforce
    public void testUsingReflection() throws Exception {
        assertThat(AgentBuilder.InjectionStrategy.UsingReflection.INSTANCE.resolve(classLoader, protectionDomain),
                hasPrototype((ClassInjector) new ClassInjector.UsingReflection(classLoader, protectionDomain)));
    }

    @Test(expected = IllegalStateException.class)
    @ClassReflectionInjectionAvailableRule.Enforce
    public void testUsingReflectionCannotHandleBootstrapLoader() throws Exception {
        AgentBuilder.InjectionStrategy.UsingReflection.INSTANCE.resolve(null, protectionDomain);
    }

    @Test
    @ClassUnsafeInjectionAvailableRule.Enforce
    public void testUsingUnsafe() throws Exception {
        assertThat(AgentBuilder.InjectionStrategy.UsingUnsafe.INSTANCE.resolve(classLoader, protectionDomain),
                hasPrototype((ClassInjector) new ClassInjector.UsingUnsafe(classLoader, protectionDomain)));
    }

    @Test
    @ClassJnaInjectionAvailableRule.Enforce
    public void testUsingJna() throws Exception {
        assertThat(AgentBuilder.InjectionStrategy.UsingJna.INSTANCE.resolve(classLoader, protectionDomain),
                hasPrototype((ClassInjector) new ClassInjector.UsingJna(classLoader, protectionDomain)));
    }

    @Test
    public void testUsingUnsafeFactory() throws Exception {
        ClassInjector.UsingUnsafe.Factory factory = mock(ClassInjector.UsingUnsafe.Factory.class);
        ClassInjector classInjector = mock(ClassInjector.class);
        when(factory.make(classLoader, protectionDomain)).thenReturn(classInjector);
        assertThat(new AgentBuilder.InjectionStrategy.UsingUnsafe.OfFactory(factory).resolve(classLoader, protectionDomain), is(classInjector));
    }

    @Test
    public void testBootstrapInjectionBootClassLoader() throws Exception {
        assertThat(new AgentBuilder.InjectionStrategy.UsingInstrumentation(mock(Instrumentation.class), mock(File.class))
                .resolve(ClassLoadingStrategy.BOOTSTRAP_LOADER, protectionDomain), instanceOf(ClassInjector.UsingInstrumentation.class));
    }

    @Test
    @ClassReflectionInjectionAvailableRule.Enforce
    public void testBootstrapInjectionNonBootClassLoader() throws Exception {
        assertThat(new AgentBuilder.InjectionStrategy.UsingInstrumentation(mock(Instrumentation.class), mock(File.class))
                .resolve(classLoader, protectionDomain), instanceOf(ClassInjector.UsingReflection.class));
    }
}
