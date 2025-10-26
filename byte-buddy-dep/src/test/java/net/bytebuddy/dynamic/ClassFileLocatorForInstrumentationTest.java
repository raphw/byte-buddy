package net.bytebuddy.dynamic;

import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import net.bytebuddy.test.utility.AgentAttachmentRule;
import net.bytebuddy.test.utility.JavaVersionRule;
import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class ClassFileLocatorForInstrumentationTest {

    private static final String FOO = "foo";

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
    @AgentAttachmentRule.Enforce(retransformsClasses = true)
    public void testExtraction() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassFileLocator classFileLocator = ClassFileLocator.ForInstrumentation.fromInstalledAgent(getClass().getClassLoader());
        ClassFileLocator.Resolution resolution = classFileLocator.locate(Foo.class.getName());
        assertThat(resolution.isResolved(), is(true));
        assertThat(resolution.resolve(), notNullValue(byte[].class));
    }

    @Test
    @AgentAttachmentRule.Enforce(retransformsClasses = true)
    @JavaVersionRule.Enforce(value = 8, atMost = 8)
    public void testExtractionOfInflatedMethodAccessor() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        Method bar = Foo.class.getDeclaredMethod("bar");
        for (int i = 0; i < 20; i++) {
            bar.invoke(new Foo());
        }
        Field field = Method.class.getDeclaredField("methodAccessor");
        field.setAccessible(true);
        Object methodAccessor = field.get(bar);
        Field delegate = methodAccessor.getClass().getDeclaredField("delegate");
        delegate.setAccessible(true);
        Class<?> delegateClass = delegate.get(methodAccessor).getClass();
        ClassFileLocator classFileLocator = ClassFileLocator.ForInstrumentation.fromInstalledAgent(delegateClass.getClassLoader());
        ClassFileLocator.Resolution resolution = classFileLocator.locate(delegateClass.getName());
        assertThat(resolution.isResolved(), is(true));
        assertThat(resolution.resolve(), notNullValue(byte[].class));
    }

    @Test
    public void testExplicitLookupBootstrapClassLoader() throws Exception {
        ClassFileLocator.ForInstrumentation.ClassLoadingDelegate classLoadingDelegate = ClassFileLocator.ForInstrumentation.ClassLoadingDelegate.Explicit.of(Object.class);
        assertThat(classLoadingDelegate.getClassLoader(), nullValue(ClassLoader.class));
        assertThat(classLoadingDelegate.locate(Object.class.getName()), CoreMatchers.<Class<?>>is(Object.class));
        assertThat(classLoadingDelegate.locate(String.class.getName()), CoreMatchers.<Class<?>>is(String.class));
    }

    @Test(expected = ClassNotFoundException.class)
    public void testExplicitLookupBootstrapClassLoaderDoesNotFindSystemClass() throws Exception {
        ClassFileLocator.ForInstrumentation.ClassLoadingDelegate classLoadingDelegate = ClassFileLocator.ForInstrumentation.ClassLoadingDelegate.Explicit.of(Object.class);
        classLoadingDelegate.locate(Foo.class.getName());
    }

    @Test
    public void testExplicitLookup() throws Exception {
        ClassFileLocator.ForInstrumentation.ClassLoadingDelegate classLoadingDelegate = ClassFileLocator.ForInstrumentation.ClassLoadingDelegate.Explicit.of(Foo.class);
        assertThat(classLoadingDelegate.getClassLoader(), is(Foo.class.getClassLoader()));
        assertThat(classLoadingDelegate.locate(Foo.class.getName()), CoreMatchers.<Class<?>>is(Foo.class));
        assertThat(classLoadingDelegate.locate(Object.class.getName()), CoreMatchers.<Class<?>>is(Object.class));
    }

    @Test
    public void testExtractingTransformerHandlesNullValue() throws Exception {
        assertThat(new ClassFileLocator.ForInstrumentation.ExtractionClassFileTransformer(mock(ClassLoader.class), FOO).transform(mock(ClassLoader.class),
                FOO,
                Object.class,
                mock(ProtectionDomain.class),
                new byte[0]), nullValue(byte[].class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNonCompatible() throws Exception {
        new ClassFileLocator.ForInstrumentation(mock(Instrumentation.class), getClass().getClassLoader());
    }

    private static class Foo {

        int foo, bar;

        void bar() {
        }
    }
}
