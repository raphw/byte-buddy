package net.bytebuddy.dynamic;

import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import net.bytebuddy.test.utility.AgentAttachmentRule;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AccessControlContext;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class ClassFileLocatorAgentBasedTest {

    @Rule
    public MethodRule agentAttachmentRule = new AgentAttachmentRule();

    public MethodRule javaVersionRule = new JavaVersionRule();

    @Test
    @AgentAttachmentRule.Enforce
    public void testStrategyCreation() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        assertThat(ClassReloadingStrategy.fromInstalledAgent(), notNullValue());
    }

    @Test
    @AgentAttachmentRule.Enforce
    public void testExtraction() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassFileLocator classFileLocator = ClassFileLocator.AgentBased.fromInstalledAgent(getClass().getClassLoader());
        ClassFileLocator.Resolution resolution = classFileLocator.locate(Foo.class.getName());
        assertThat(resolution.isResolved(), is(true));
        assertThat(resolution.resolve(), notNullValue(byte[].class));
    }

    @Test
    @AgentAttachmentRule.Enforce
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
        ClassFileLocator classFileLocator = ClassFileLocator.AgentBased.fromInstalledAgent(delegateClass.getClassLoader());
        ClassFileLocator.Resolution resolution = classFileLocator.locate(delegateClass.getName());
        assertThat(resolution.isResolved(), is(true));
        assertThat(resolution.resolve(), notNullValue(byte[].class));
    }

    @Test
    public void testExplicitLookup() throws Exception {
        ClassFileLocator.AgentBased.ClassLoadingDelegate fallback = mock(ClassFileLocator.AgentBased.ClassLoadingDelegate.class);
        ClassFileLocator.AgentBased.ClassLoadingDelegate classLoadingDelegate = new ClassFileLocator.AgentBased
                .ClassLoadingDelegate.Explicit(fallback, Collections.<Class<?>>singletonList(Object.class));
        assertEquals(Object.class, classLoadingDelegate.locate(Object.class.getName()));
        doReturn(String.class).when(fallback).locate(String.class.getName());
        assertEquals(String.class, classLoadingDelegate.locate(String.class.getName()));
        verify(fallback).locate(String.class.getName());
        ClassLoader classLoader = mock(ClassLoader.class);
        when(fallback.getClassLoader()).thenReturn(classLoader);
        assertThat(classLoadingDelegate.getClassLoader(), is(classLoader));
        verify(fallback).getClassLoader();
        verifyNoMoreInteractions(fallback);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ClassFileLocator.AgentBased.class).refine(new ObjectPropertyAssertion.Refinement<Instrumentation>() {
            @Override
            public void apply(Instrumentation mock) {
                when(mock.isRetransformClassesSupported()).thenReturn(true);
            }
        }).apply();
        ObjectPropertyAssertion.of(ClassFileLocator.AgentBased.ClassLoadingDelegate.Default.class).apply();
        ObjectPropertyAssertion.of(ClassFileLocator.AgentBased.ClassLoadingDelegate.ForDelegatingClassLoader.class)
                .create(new ObjectPropertyAssertion.Creator<AccessControlContext>() {
                    @Override
                    public AccessControlContext create() {
                        return new AccessControlContext(new ProtectionDomain[]{mock(ProtectionDomain.class)});
                    }
                }).apply();
        final Iterator<Field> iterator = Arrays.asList(Foo.class.getDeclaredFields()).iterator();
        ObjectPropertyAssertion.of(ClassFileLocator.AgentBased.ClassLoadingDelegate.ForDelegatingClassLoader.Dispatcher.Resolved.class)
                .create(new ObjectPropertyAssertion.Creator<Field>() {
                    @Override
                    public Field create() {
                        return iterator.next();
                    }
                })
                .apply();
        ObjectPropertyAssertion.of(ClassFileLocator.AgentBased.ClassLoadingDelegate.ForDelegatingClassLoader.Dispatcher.Unresolved.class).apply();
        final Iterator<Class<?>> otherIterator = Arrays.<Class<?>>asList(Integer.class, String.class, Object.class, Byte.class).iterator();
        ObjectPropertyAssertion.of(ClassFileLocator.AgentBased.ClassLoadingDelegate.Explicit.class).create(new ObjectPropertyAssertion.Creator<Collection<Class<?>>>() {
            @Override
            public Collection<Class<?>> create() {
                return Collections.<Class<?>>singletonList(otherIterator.next());
            }
        }).apply();
        ObjectPropertyAssertion.of(ClassFileLocator.AgentBased.ExtractionClassFileTransformer.class).applyBasic();
    }


    @Test(expected = IllegalArgumentException.class)
    public void testNonCompatible() throws Exception {
        new ClassFileLocator.AgentBased(mock(Instrumentation.class), getClass().getClassLoader());
    }

    private static class Foo {

        int foo, bar;

        void bar() {
        }
    }
}
