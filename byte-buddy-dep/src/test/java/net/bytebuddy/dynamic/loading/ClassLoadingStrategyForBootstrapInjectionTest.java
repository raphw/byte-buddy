package net.bytebuddy.dynamic.loading;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.test.utility.AgentAttachmentRule;
import net.bytebuddy.test.utility.ClassReflectionInjectionAvailableRule;
import net.bytebuddy.utility.RandomString;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class ClassLoadingStrategyForBootstrapInjectionTest {

    private static final String FOO = "foo", BAR = "bar";

    @Rule
    public MethodRule agentAttachmentRule = new AgentAttachmentRule();

    @Rule
    public MethodRule classInjectionAvailableRule = new ClassReflectionInjectionAvailableRule();

    private File file;

    @Before
    public void setUp() throws Exception {
        file = File.createTempFile(FOO, BAR);
        assertThat(file.delete(), is(true));
        file = new File(file.getParentFile(), RandomString.make());
        assertThat(file.mkdir(), is(true));
    }

    @Test
    @AgentAttachmentRule.Enforce
    public void testBootstrapInjection() throws Exception {
        ClassLoadingStrategy<ClassLoader> bootstrapStrategy = new ClassLoadingStrategy.ForBootstrapInjection(ByteBuddyAgent.install(), file);
        String name = FOO + RandomString.make();
        DynamicType dynamicType = new ByteBuddy().subclass(Object.class).name(name).make();
        Map<TypeDescription, Class<?>> loaded = bootstrapStrategy.load(ClassLoadingStrategy.BOOTSTRAP_LOADER, Collections.singletonMap(dynamicType.getTypeDescription(), dynamicType.getBytes()));
        assertThat(loaded.size(), is(1));
        assertThat(loaded.get(dynamicType.getTypeDescription()).getName(), is(name));
        assertThat(loaded.get(dynamicType.getTypeDescription()).getClassLoader(), nullValue(ClassLoader.class));
    }

    @Test
    @AgentAttachmentRule.Enforce
    @ClassReflectionInjectionAvailableRule.Enforce
    public void testClassLoaderInjection() throws Exception {
        ClassLoadingStrategy<ClassLoader> bootstrapStrategy = new ClassLoadingStrategy.ForBootstrapInjection(ByteBuddyAgent.install(), file);
        String name = BAR + RandomString.make();
        ClassLoader classLoader = new URLClassLoader(new URL[0], null);
        DynamicType dynamicType = new ByteBuddy().subclass(Object.class).name(name).make();
        Map<TypeDescription, Class<?>> loaded = bootstrapStrategy.load(classLoader, Collections.singletonMap(dynamicType.getTypeDescription(), dynamicType.getBytes()));
        assertThat(loaded.size(), is(1));
        assertThat(loaded.get(dynamicType.getTypeDescription()).getName(), is(name));
        assertThat(loaded.get(dynamicType.getTypeDescription()).getClassLoader(), is(classLoader));
    }
}
