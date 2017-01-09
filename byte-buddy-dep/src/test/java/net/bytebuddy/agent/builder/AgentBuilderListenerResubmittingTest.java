package net.bytebuddy.agent.builder;

import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.junit.Test;

import java.net.URL;
import java.net.URLClassLoader;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class AgentBuilderListenerResubmittingTest {

    // TODO: Implement remainding tests

    @Test
    public void testBootstrapLoaderReference() throws Exception {
        AgentBuilder.Listener.Resubmitting.ClassLoaderReference classLoaderReference =
                new AgentBuilder.Listener.Resubmitting.ClassLoaderReference(ClassLoadingStrategy.BOOTSTRAP_LOADER);
        assertThat(classLoaderReference.isBootstrapLoader(), is(true));
        assertThat(classLoaderReference.hashCode(), is(0));
        assertThat(classLoaderReference.get(), nullValue(ClassLoader.class));
        AgentBuilder.Listener.Resubmitting.ClassLoaderReference other =
                new AgentBuilder.Listener.Resubmitting.ClassLoaderReference(new URLClassLoader(new URL[0]));
        System.gc();
        assertThat(other.get(), nullValue(ClassLoader.class));
        assertThat(classLoaderReference, not(is(other)));
        assertThat(classLoaderReference, is(new AgentBuilder.Listener.Resubmitting.ClassLoaderReference(ClassLoadingStrategy.BOOTSTRAP_LOADER)));
    }

    @Test
    public void testNonBootstrapReference() throws Exception {
        ClassLoader classLoader = new URLClassLoader(new URL[0]);
        AgentBuilder.Listener.Resubmitting.ClassLoaderReference classLoaderReference =
                new AgentBuilder.Listener.Resubmitting.ClassLoaderReference(classLoader);
        assertThat(classLoaderReference.isBootstrapLoader(), is(false));
        assertThat(classLoaderReference, is(new AgentBuilder.Listener.Resubmitting.ClassLoaderReference(classLoader)));
        assertThat(classLoaderReference.hashCode(), is(classLoader.hashCode()));
        assertThat(classLoaderReference.get(), is(classLoader));
        classLoader = null; // Make GC eligible.
        System.gc();
        assertThat(classLoaderReference.get(), nullValue(ClassLoader.class));
        assertThat(classLoaderReference, not(is(new AgentBuilder.Listener.Resubmitting.ClassLoaderReference(ClassLoadingStrategy.BOOTSTRAP_LOADER))));
        assertThat(classLoaderReference.isBootstrapLoader(), is(false));
    }
}