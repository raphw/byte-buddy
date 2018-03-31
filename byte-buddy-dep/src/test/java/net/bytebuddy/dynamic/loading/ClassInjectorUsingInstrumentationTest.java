package net.bytebuddy.dynamic.loading;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.test.utility.AgentAttachmentRule;
import net.bytebuddy.utility.RandomString;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.io.File;
import java.security.AccessControlContext;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class ClassInjectorUsingInstrumentationTest {

    private static final String FOO = "foo", BAR = "bar";

    @Rule
    public MethodRule agentAttachmentRule = new AgentAttachmentRule();

    private File folder;

    @Before
    public void setUp() throws Exception {
        File file = File.createTempFile(FOO, BAR);
        assertThat(file.delete(), is(true));
        folder = new File(file.getParentFile(), RandomString.make());
        assertThat(folder.mkdir(), is(true));
    }

    @Test
    @AgentAttachmentRule.Enforce
    public void testBootstrapInjection() throws Exception {
        ClassInjector classInjector = ClassInjector.UsingInstrumentation.of(folder,
                ClassInjector.UsingInstrumentation.Target.BOOTSTRAP,
                ByteBuddyAgent.install());
        String name = FOO + RandomString.make();
        DynamicType dynamicType = new ByteBuddy().subclass(Object.class).name(name).make();
        Map<TypeDescription, Class<?>> types = classInjector.inject(Collections.singletonMap(dynamicType.getTypeDescription(), dynamicType.getBytes()));
        assertThat(types.size(), is(1));
        assertThat(types.get(dynamicType.getTypeDescription()).getName(), is(name));
        assertThat(types.get(dynamicType.getTypeDescription()).getClassLoader(), nullValue(ClassLoader.class));
    }

    @Test
    @AgentAttachmentRule.Enforce
    public void testSystemInjection() throws Exception {
        ClassInjector classInjector = ClassInjector.UsingInstrumentation.of(folder,
                ClassInjector.UsingInstrumentation.Target.SYSTEM,
                ByteBuddyAgent.install());
        String name = BAR + RandomString.make();
        DynamicType dynamicType = new ByteBuddy().subclass(Object.class).name(name).make();
        Map<TypeDescription, Class<?>> types = classInjector.inject(Collections.singletonMap(dynamicType.getTypeDescription(), dynamicType.getBytes()));
        assertThat(types.size(), is(1));
        assertThat(types.get(dynamicType.getTypeDescription()).getName(), is(name));
        assertThat(types.get(dynamicType.getTypeDescription()).getClassLoader(), is(ClassLoader.getSystemClassLoader()));
    }
}
