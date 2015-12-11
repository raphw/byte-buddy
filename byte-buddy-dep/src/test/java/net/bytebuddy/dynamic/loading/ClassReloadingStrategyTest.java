package net.bytebuddy.dynamic.loading;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.test.utility.AgentAttachmentRule;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.ArgumentCaptor;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.util.Collections;

import static junit.framework.TestCase.assertEquals;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class ClassReloadingStrategyTest {

    private static final String FOO = "foo", BAR = "bar";

    @Rule
    public MethodRule agentAttachmentRule = new AgentAttachmentRule();

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Test
    @AgentAttachmentRule.Enforce(redefinesClasses = true)
    public void testStrategyCreation() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        assertThat(ClassReloadingStrategy.fromInstalledAgent(), notNullValue());
    }

    @Test
    @AgentAttachmentRule.Enforce(redefinesClasses = true)
    public void testFromAgentClassReloadingStrategy() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        Foo foo = new Foo();
        assertThat(foo.foo(), is(FOO));
        ClassReloadingStrategy classReloadingStrategy = ClassReloadingStrategy.fromInstalledAgent();
        new ByteBuddy()
                .redefine(Foo.class)
                .method(named(FOO))
                .intercept(FixedValue.value(BAR))
                .make()
                .load(Foo.class.getClassLoader(), classReloadingStrategy);
        assertThat(foo.foo(), is(BAR));
        classReloadingStrategy.reset(Foo.class);
        assertThat(foo.foo(), is(FOO));
    }

    @Test
    @AgentAttachmentRule.Enforce(redefinesClasses = true)
    public void testClassRedefinitionRenamingWithStackMapFrames() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassReloadingStrategy classReloadingStrategy = ClassReloadingStrategy.fromInstalledAgent();
        Bar bar = new Bar();
        new ByteBuddy().redefine(Qux.class)
                .name(Bar.class.getName())
                .make()
                .load(Bar.class.getClassLoader(), classReloadingStrategy);
        assertThat(bar.foo(), is(BAR));
        classReloadingStrategy.reset(Bar.class);
        assertThat(bar.foo(), is(FOO));
    }

    @Test
    @AgentAttachmentRule.Enforce(redefinesClasses = true)
    public void testRedefinitionReloadingStrategy() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        Foo foo = new Foo();
        assertThat(foo.foo(), is(FOO));
        ClassReloadingStrategy classReloadingStrategy = new ClassReloadingStrategy(ByteBuddyAgent.getInstrumentation(),
                ClassReloadingStrategy.Engine.REDEFINITION);
        new ByteBuddy()
                .redefine(Foo.class)
                .method(named(FOO))
                .intercept(FixedValue.value(BAR))
                .make()
                .load(Foo.class.getClassLoader(), classReloadingStrategy);
        assertThat(foo.foo(), is(BAR));
        classReloadingStrategy.reset(Foo.class);
        assertThat(foo.foo(), is(FOO));
    }

    @Test
    @AgentAttachmentRule.Enforce(retransformsClasses = true, redefinesClasses = true)
    public void testRetransformationReloadingStrategy() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        Foo foo = new Foo();
        assertThat(foo.foo(), is(FOO));
        ClassReloadingStrategy classReloadingStrategy = new ClassReloadingStrategy(ByteBuddyAgent.getInstrumentation(),
                ClassReloadingStrategy.Engine.RETRANSFORMATION);
        new ByteBuddy()
                .redefine(Foo.class)
                .method(named(FOO))
                .intercept(FixedValue.value(BAR))
                .make()
                .load(Foo.class.getClassLoader(), classReloadingStrategy);
        assertThat(foo.foo(), is(BAR));
        classReloadingStrategy.reset(Foo.class);
        assertThat(foo.foo(), is(FOO));
    }

    @Test
    public void testPreregisteredType() throws Exception {
        Instrumentation instrumentation = mock(Instrumentation.class);
        ClassLoader classLoader = mock(ClassLoader.class);
        when(instrumentation.isRedefineClassesSupported()).thenReturn(true);
        when(instrumentation.getInitiatedClasses(classLoader)).thenReturn(new Class<?>[0]);
        ClassReloadingStrategy classReloadingStrategy = ClassReloadingStrategy.of(instrumentation).preregistered(Object.class);
        ArgumentCaptor<ClassDefinition> classDefinition = ArgumentCaptor.forClass(ClassDefinition.class);
        classReloadingStrategy.load(classLoader, Collections.singletonMap(TypeDescription.OBJECT, new byte[]{1, 2, 3}));
        verify(instrumentation).redefineClasses(classDefinition.capture());
        assertEquals(Object.class, classDefinition.getValue().getDefinitionClass());
        assertThat(classDefinition.getValue().getDefinitionClassFile(), is(new byte[]{1, 2, 3}));
    }

    @Test
    public void testRetransformationDiscovery() throws Exception {
        Instrumentation instrumentation = mock(Instrumentation.class);
        when(instrumentation.isRetransformClassesSupported()).thenReturn(true);
        assertThat(ClassReloadingStrategy.of(instrumentation), notNullValue(ClassReloadingStrategy.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNonCompatible() throws Exception {
        ClassReloadingStrategy.of(mock(Instrumentation.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoRedefinition() throws Exception {
        new ClassReloadingStrategy(mock(Instrumentation.class), ClassReloadingStrategy.Engine.REDEFINITION);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoRetransformation() throws Exception {
        new ClassReloadingStrategy(mock(Instrumentation.class), ClassReloadingStrategy.Engine.RETRANSFORMATION);
    }

    @Test//(expected = IllegalStateException.class)
    public void testResetNotSupported() throws Exception {
        Instrumentation instrumentation = mock(Instrumentation.class);
        when(instrumentation.isRetransformClassesSupported()).thenReturn(true);
        new ClassReloadingStrategy(instrumentation, ClassReloadingStrategy.Engine.RETRANSFORMATION).reset();
    }

    @Test
    public void testEngineSelfReport() throws Exception {
        assertThat(ClassReloadingStrategy.Engine.REDEFINITION.isRedefinition(), is(true));
        assertThat(ClassReloadingStrategy.Engine.RETRANSFORMATION.isRedefinition(), is(false));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @AgentAttachmentRule.Enforce(retransformsClasses = true, redefinesClasses = true)
    public void testAnonymousType() throws Exception {
        ((Runnable) Class.forName("net.bytebuddy.test.precompiled.AnonymousClassLoaderTest").newInstance()).run();
    }

    @Test
    public void testResetEmptyNoEffectImplicitLocator() throws Exception {
        Instrumentation instrumentation = mock(Instrumentation.class);
        when(instrumentation.isRedefineClassesSupported()).thenReturn(true);
        ClassReloadingStrategy.of(instrumentation).reset();
        verify(instrumentation, times(2)).isRedefineClassesSupported();
        verifyNoMoreInteractions(instrumentation);
    }

    @Test
    public void testResetEmptyNoEffect() throws Exception {
        Instrumentation instrumentation = mock(Instrumentation.class);
        ClassFileLocator classFileLocator = mock(ClassFileLocator.class);
        when(instrumentation.isRedefineClassesSupported()).thenReturn(true);
        ClassReloadingStrategy.of(instrumentation).reset(classFileLocator);
        verify(instrumentation, times(2)).isRedefineClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verifyZeroInteractions(classFileLocator);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ClassReloadingStrategy.class).refine(new ObjectPropertyAssertion.Refinement<Instrumentation>() {
            @Override
            public void apply(Instrumentation mock) {
                when(mock.isRedefineClassesSupported()).thenReturn(true);
                when(mock.isRetransformClassesSupported()).thenReturn(true);
            }
        }).apply();
        ObjectPropertyAssertion.of(ClassReloadingStrategy.BootstrapInjection.Enabled.class).apply();
        ObjectPropertyAssertion.of(ClassReloadingStrategy.Engine.class).apply();
        ObjectPropertyAssertion.of(ClassReloadingStrategy.Engine.ClassRedefinitionTransformer.class).applyBasic();
        ObjectPropertyAssertion.of(ClassReloadingStrategy.BootstrapInjection.Enabled.class).apply();
        ObjectPropertyAssertion.of(ClassReloadingStrategy.BootstrapInjection.Disabled.class).apply();
    }

    @SuppressWarnings("unused")
    public static class Foo {

        /**
         * Padding to increase class file size.
         */
        private long a1, a2, a3, a4, a5, a6, a7, a8, a9, a10,
                b2, b3, b4, b5, b6, b7, b8, b9, b10,
                c1, c2, c3, c4, c5, c6, c7, c8, c9, c10,
                d1, d2, d3, d4, d5, d6, d7, d8, d9, d10;

        public String foo() {
            return FOO;
        }
    }

    public static class Bar {

        public String foo() {
            Bar bar = new Bar();
            return Math.random() < 0
                    ? FOO
                    : FOO;
        }
    }

    public static class Qux {

        public String foo() {
            Qux qux = new Qux();
            return Math.random() < 0
                    ? BAR
                    : BAR;
        }
    }
}
