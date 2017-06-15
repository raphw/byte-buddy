package net.bytebuddy.agent.builder;

import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.test.utility.AgentAttachmentRule;
import net.bytebuddy.test.utility.ClassFileExtraction;
import net.bytebuddy.test.utility.IntegrationRule;
import net.bytebuddy.utility.JavaModule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.none;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class AgentBuilderDefaultApplicationSuperTypeLoadingTest {

    private static final String FOO = "foo", BAR = "bar";

    @Rule
    public MethodRule agentAttachmentRule = new AgentAttachmentRule();

    @Rule
    public MethodRule integrationRule = new IntegrationRule();

    private ClassLoader classLoader;

    private ExecutorService executorService;

    @Before
    public void setUp() throws Exception {
        classLoader = new ByteArrayClassLoader(ClassLoadingStrategy.BOOTSTRAP_LOADER,
                ClassFileExtraction.of(Foo.class, Bar.class, AgentBuilderDefaultApplicationSuperTypeLoadingTest.class),
                ByteArrayClassLoader.PersistenceHandler.MANIFEST);
        executorService = Executors.newCachedThreadPool();
    }

    @After
    public void tearDown() throws Exception {
        executorService.shutdownNow();
    }

    @Test
    @AgentAttachmentRule.Enforce
    @IntegrationRule.Enforce
    public void testSynchronousSuperTypeLoading() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(AgentBuilder.DescriptionStrategy.Default.POOL_ONLY.withSuperTypeLoading())
                .ignore(none())
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                .type(ElementMatchers.isSubTypeOf(Foo.class), ElementMatchers.is(classLoader)).transform(new ConstantTransformer())
                .installOnByteBuddyAgent();
        try {
            Class<?> type = classLoader.loadClass(Bar.class.getName());
            assertThat(type.getDeclaredMethod(BAR).invoke(type.getDeclaredConstructor().newInstance()), is((Object) BAR));
            assertThat(type.getSuperclass().getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), nullValue(Object.class));
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
        }
    }

    @Test
    @AgentAttachmentRule.Enforce
    @IntegrationRule.Enforce
    public void testAsynchronousSuperTypeLoading() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(AgentBuilder.DescriptionStrategy.Default.POOL_ONLY.withSuperTypeLoading(executorService))
                .ignore(none())
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                .type(ElementMatchers.isSubTypeOf(Foo.class), ElementMatchers.is(classLoader)).transform(new ConstantTransformer())
                .installOnByteBuddyAgent();
        try {
            Class<?> type = classLoader.loadClass(Bar.class.getName());
            assertThat(type.getDeclaredMethod(BAR).invoke(type.getDeclaredConstructor().newInstance()), is((Object) BAR));
            assertThat(type.getSuperclass().getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
        }
    }

    public static class Foo {

        public String foo() {
            return null;
        }
    }

    public static class Bar extends Foo {

        public String bar() {
            return null;
        }
    }

    private static class ConstantTransformer implements AgentBuilder.Transformer {

        @Override
        public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
                                                TypeDescription typeDescription,
                                                ClassLoader classLoader,
                                                JavaModule module) {
            return builder
                    .method(isDeclaredBy(typeDescription).and(named(FOO))).intercept(FixedValue.value(FOO))
                    .method(isDeclaredBy(typeDescription).and(named(BAR))).intercept(FixedValue.value(BAR));
        }
    }
}
