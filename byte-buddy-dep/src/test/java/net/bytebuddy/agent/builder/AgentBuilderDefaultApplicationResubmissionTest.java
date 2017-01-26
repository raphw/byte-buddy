package net.bytebuddy.agent.builder;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.loading.PackageDefinitionStrategy;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.test.utility.AgentAttachmentRule;
import net.bytebuddy.test.utility.ClassFileExtraction;
import net.bytebuddy.test.utility.IntegrationRule;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.utility.JavaModule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.none;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AgentBuilderDefaultApplicationResubmissionTest {

    private static final String FOO = "foo";

    private static final long TIMEOUT = 1L;

    @Rule
    public MethodRule agentAttachmentRule = new AgentAttachmentRule();

    @Rule
    public MethodRule integrationRule = new IntegrationRule();

    private ClassLoader classLoader;

    @Before
    public void setUp() throws Exception {
        classLoader = new ByteArrayClassLoader(ClassLoadingStrategy.BOOTSTRAP_LOADER,
                ClassFileExtraction.of(Foo.class),
                ClassLoadingStrategy.NO_PROTECTION_DOMAIN,
                ByteArrayClassLoader.PersistenceHandler.LATENT,
                PackageDefinitionStrategy.NoOp.INSTANCE);
    }

    @Test
    @AgentAttachmentRule.Enforce(retransformsClasses = true)
    @IntegrationRule.Enforce
    public void testResubmission() throws Exception {
        // A redefinition reflects on loaded types which are eagerly validated types (Java 7- for redefinition).
        // This causes type equality for outer/inner classes to fail which is why an external class is used.
        final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        try {
            assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
            ClassFileTransformer classFileTransformer = new AgentBuilder.Default(new ByteBuddy().with(TypeValidation.DISABLED))
                    .ignore(none())
                    .disableClassFormatChanges()
                    .with(AgentBuilder.LocationStrategy.NoOp.INSTANCE)
                    .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                    .withResubmission(new AgentBuilder.RedefinitionStrategy.ResubmissionScheduler() {
                        @Override
                        public void schedule(final Runnable job) {
                            scheduledExecutorService.scheduleWithFixedDelay(job, TIMEOUT, TIMEOUT, TimeUnit.SECONDS);
                        }
                    })
                    .type(ElementMatchers.is(Foo.class), ElementMatchers.is(classLoader)).transform(new FooTransformer())
                    .installOnByteBuddyAgent();
            try {
                Class<?> type = classLoader.loadClass(Foo.class.getName());
                Thread.sleep(TimeUnit.SECONDS.toMillis(TIMEOUT * 3));
                assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
            } finally {
                ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
            }
        } finally {
            scheduledExecutorService.shutdown();
        }
    }

    public static class Foo {

        public String foo() {
            return null;
        }
    }

    private static class FooTransformer implements AgentBuilder.Transformer {

        @Override
        public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader, JavaModule module) {
            return builder.method(named(FOO)).intercept(FixedValue.value(FOO));
        }
    }
}
