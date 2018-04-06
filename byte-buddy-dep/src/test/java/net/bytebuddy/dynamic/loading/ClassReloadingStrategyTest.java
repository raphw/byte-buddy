package net.bytebuddy.dynamic.loading;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.test.utility.AgentAttachmentRule;
import net.bytebuddy.test.utility.ClassFileExtraction;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.utility.RandomString;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.ArgumentCaptor;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.concurrent.Callable;

import static junit.framework.TestCase.assertEquals;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class ClassReloadingStrategyTest {

    private static final String FOO = "foo", BAR = "bar";

    private static final String LAMBDA_SAMPLE_FACTORY = "net.bytebuddy.test.precompiled.LambdaSampleFactory";

    @Rule
    public MethodRule agentAttachmentRule = new AgentAttachmentRule();

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Test
    @AgentAttachmentRule.Enforce(retransformsClasses = true)
    public void testStrategyCreation() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        assertThat(ClassReloadingStrategy.fromInstalledAgent(), notNullValue());
    }

    @Test
    @AgentAttachmentRule.Enforce(retransformsClasses = true)
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
        try {
            assertThat(foo.foo(), is(BAR));
        } finally {
            classReloadingStrategy.reset(Foo.class);
            assertThat(foo.foo(), is(FOO));
        }
    }

    @Test
    @AgentAttachmentRule.Enforce(retransformsClasses = true)
    @JavaVersionRule.Enforce(atMost = 10) // Wait for mechanism in sun.misc.Unsafe to define class.
    public void testFromAgentClassWithAuxiliaryReloadingStrategy() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        Foo foo = new Foo();
        assertThat(foo.foo(), is(FOO));
        ClassReloadingStrategy classReloadingStrategy = ClassReloadingStrategy.fromInstalledAgent();
        String randomName = FOO + RandomString.make();
        new ByteBuddy()
                .redefine(Foo.class)
                .method(named(FOO))
                .intercept(FixedValue.value(BAR))
                .make()
                .include(new ByteBuddy().subclass(Object.class).name(randomName).make())
                .load(Foo.class.getClassLoader(), classReloadingStrategy);
        try {
            assertThat(foo.foo(), is(BAR));
        } finally {
            classReloadingStrategy.reset(Foo.class);
            assertThat(foo.foo(), is(FOO));
        }
        assertThat(Class.forName(randomName), notNullValue(Class.class));
    }

    @Test
    @AgentAttachmentRule.Enforce(retransformsClasses = true)
    public void testClassRedefinitionRenamingWithStackMapFrames() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassReloadingStrategy classReloadingStrategy = ClassReloadingStrategy.fromInstalledAgent();
        Bar bar = new Bar();
        new ByteBuddy().redefine(Qux.class)
                .name(Bar.class.getName())
                .make()
                .load(Bar.class.getClassLoader(), classReloadingStrategy);
        try {
            assertThat(bar.foo(), is(BAR));
        } finally {
            classReloadingStrategy.reset(Bar.class);
            assertThat(bar.foo(), is(FOO));
        }
    }

    @Test
    @AgentAttachmentRule.Enforce(redefinesClasses = true)
    public void testRedefinitionReloadingStrategy() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        Foo foo = new Foo();
        assertThat(foo.foo(), is(FOO));
        ClassReloadingStrategy classReloadingStrategy = ClassReloadingStrategy.fromInstalledAgent(ClassReloadingStrategy.Strategy.REDEFINITION);
        new ByteBuddy()
                .redefine(Foo.class)
                .method(named(FOO))
                .intercept(FixedValue.value(BAR))
                .make()
                .load(Foo.class.getClassLoader(), classReloadingStrategy);
        try {
            assertThat(foo.foo(), is(BAR));
        } finally {
            classReloadingStrategy.reset(Foo.class);
            assertThat(foo.foo(), is(FOO));
        }
    }

    @Test
    @AgentAttachmentRule.Enforce(retransformsClasses = true)
    public void testRetransformationReloadingStrategy() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        Foo foo = new Foo();
        assertThat(foo.foo(), is(FOO));
        ClassReloadingStrategy classReloadingStrategy = ClassReloadingStrategy.fromInstalledAgent(ClassReloadingStrategy.Strategy.RETRANSFORMATION);
        new ByteBuddy()
                .redefine(Foo.class)
                .method(named(FOO))
                .intercept(FixedValue.value(BAR))
                .make()
                .load(Foo.class.getClassLoader(), classReloadingStrategy);
        try {
            assertThat(foo.foo(), is(BAR));
        } finally {
            classReloadingStrategy.reset(Foo.class);
            assertThat(foo.foo(), is(FOO));
        }
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
        new ClassReloadingStrategy(mock(Instrumentation.class), ClassReloadingStrategy.Strategy.REDEFINITION);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoRetransformation() throws Exception {
        new ClassReloadingStrategy(mock(Instrumentation.class), ClassReloadingStrategy.Strategy.RETRANSFORMATION);
    }

    @Test
    public void testResetNotSupported() throws Exception {
        Instrumentation instrumentation = mock(Instrumentation.class);
        when(instrumentation.isRetransformClassesSupported()).thenReturn(true);
        new ClassReloadingStrategy(instrumentation, ClassReloadingStrategy.Strategy.RETRANSFORMATION).reset();
    }

    @Test
    public void testEngineSelfReport() throws Exception {
        assertThat(ClassReloadingStrategy.Strategy.REDEFINITION.isRedefinition(), is(true));
        assertThat(ClassReloadingStrategy.Strategy.RETRANSFORMATION.isRedefinition(), is(false));
    }

    @Test
    @JavaVersionRule.Enforce(value = 8, atMost = 8)
    @AgentAttachmentRule.Enforce(retransformsClasses = true)
    public void testAnonymousType() throws Exception {
        ClassLoader classLoader = new ByteArrayClassLoader(ClassLoadingStrategy.BOOTSTRAP_LOADER,
                ClassFileExtraction.of(Class.forName(LAMBDA_SAMPLE_FACTORY)),
                ByteArrayClassLoader.PersistenceHandler.MANIFEST);
        Instrumentation instrumentation = ByteBuddyAgent.install();
        Class<?> factory = classLoader.loadClass(LAMBDA_SAMPLE_FACTORY);
        @SuppressWarnings("unchecked")
        Callable<String> instance = (Callable<String>) factory.getDeclaredMethod("nonCapturing").invoke(factory.getDeclaredConstructor().newInstance());
        // Anonymous types can only be reset to their original format, if a retransformation is applied.
        ClassReloadingStrategy classReloadingStrategy = new ClassReloadingStrategy(instrumentation,
                ClassReloadingStrategy.Strategy.RETRANSFORMATION).preregistered(instance.getClass());
        ClassFileLocator classFileLocator = ClassFileLocator.AgentBased.of(instrumentation, instance.getClass());
        try {
            assertThat(instance.call(), is(FOO));
            new ByteBuddy()
                    .redefine(instance.getClass(), classFileLocator)
                    .method(named("call"))
                    .intercept(FixedValue.value(BAR))
                    .make()
                    .load(instance.getClass().getClassLoader(), classReloadingStrategy);
            assertThat(instance.call(), is(BAR));
        } finally {
            classReloadingStrategy.reset(classFileLocator, instance.getClass());
            assertThat(instance.call(), is(FOO));
        }
    }

    @Test
    public void testResetEmptyNoEffectImplicitLocator() throws Exception {
        Instrumentation instrumentation = mock(Instrumentation.class);
        when(instrumentation.isRetransformClassesSupported()).thenReturn(true);
        ClassReloadingStrategy.of(instrumentation).reset();
        verify(instrumentation, times(2)).isRetransformClassesSupported();
        verifyNoMoreInteractions(instrumentation);
    }

    @Test
    public void testResetEmptyNoEffect() throws Exception {
        Instrumentation instrumentation = mock(Instrumentation.class);
        ClassFileLocator classFileLocator = mock(ClassFileLocator.class);
        when(instrumentation.isRetransformClassesSupported()).thenReturn(true);
        ClassReloadingStrategy.of(instrumentation).reset(classFileLocator);
        verify(instrumentation, times(2)).isRetransformClassesSupported();
        verifyNoMoreInteractions(instrumentation);
        verifyZeroInteractions(classFileLocator);
    }

    @Test
    public void testTransformerHandlesNullValue() throws Exception {
        assertThat(new ClassReloadingStrategy.Strategy.ClassRedefinitionTransformer(Collections.<Class<?>, ClassDefinition>emptyMap()).transform(mock(ClassLoader.class),
                FOO,
                Object.class,
                mock(ProtectionDomain.class),
                new byte[0]), nullValue(byte[].class));
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

        @SuppressWarnings("unused")
        public String foo() {
            Bar bar = new Bar();
            return Math.random() < 0
                    ? FOO
                    : FOO;
        }
    }

    public static class Qux {

        @SuppressWarnings("unused")
        public String foo() {
            Qux qux = new Qux();
            return Math.random() < 0
                    ? BAR
                    : BAR;
        }
    }
}
