package net.bytebuddy.agent.builder;

import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.test.c.NativeSample;
import net.bytebuddy.test.utility.AgentAttachmentRule;
import net.bytebuddy.test.utility.IntegrationRule;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.test.utility.NativeSampleRule;
import net.bytebuddy.utility.JavaModule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.none;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AgentBuilderDefaultNativeApplicationTest {

    private static final String FOO = "foo";

    @Rule
    public MethodRule agentAttachmentRule = new AgentAttachmentRule();

    @Rule
    public MethodRule integrationRule = new IntegrationRule();

    @Rule
    public MethodRule nativeSampleRule = new NativeSampleRule();

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    private ClassLoader classLoader;

    @Before
    public void setUp() throws Exception {
        classLoader = new ByteArrayClassLoader.ChildFirst(getClass().getClassLoader(),
                ClassFileLocator.ForClassLoader.readToNames(NativeSample.class),
                ByteArrayClassLoader.PersistenceHandler.MANIFEST);
    }

    @Test
    @AgentAttachmentRule.Enforce
    @IntegrationRule.Enforce
    @NativeSampleRule.Enforce
    public void testNativeMethodPrefix() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .enableNativeMethodPrefix("bar")
                .ignore(none())
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                .type(ElementMatchers.is(NativeSample.class), ElementMatchers.is(classLoader)).transform(new FooTransformer())
                .installOnByteBuddyAgent();
        try {
            Class<?> type = classLoader.loadClass(NativeSample.class.getName());
            assertThat(type.getDeclaredMethod(FOO, int.class, int.class).invoke(type.getDeclaredConstructor().newInstance(), 42, 1), is((Object) 84));
        } finally {
            assertThat(ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer), is(true));
        }
    }

    @Test
    @AgentAttachmentRule.Enforce
    @IntegrationRule.Enforce
    @NativeSampleRule.Enforce
    @JavaVersionRule.Enforce(atMost = 12, j9 = false)
    public void testNativeMethodPrefixRetransformation() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        Class<?> type = classLoader.loadClass(NativeSample.class.getName());
        assertThat(type.getDeclaredMethod(FOO, int.class, int.class).invoke(type.getDeclaredConstructor().newInstance(), 42, 1), is((Object) 42));
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .enableNativeMethodPrefix("bar")
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .ignore(none())
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                .type(ElementMatchers.is(NativeSample.class), ElementMatchers.is(classLoader)).transform(new FooTransformer())
                .installOnByteBuddyAgent();
        try {
            assertThat(type.getDeclaredMethod(FOO, int.class, int.class).invoke(type.getDeclaredConstructor().newInstance(), 42, 1), is((Object) 84));
        } finally {
            assertThat(ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer), is(true));
        }
    }

    private static class FooTransformer implements AgentBuilder.Transformer {

        public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
                                                TypeDescription typeDescription,
                                                ClassLoader classLoader,
                                                JavaModule module,
                                                ProtectionDomain protectionDomain) {
            return builder.method(named(FOO)).intercept(MethodCall.invokeSuper().withArgument(0).with(2));
        }
    }
}
