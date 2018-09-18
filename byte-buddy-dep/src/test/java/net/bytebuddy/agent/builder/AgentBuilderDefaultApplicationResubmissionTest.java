package net.bytebuddy.agent.builder;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.test.packaging.SimpleType;
import net.bytebuddy.test.utility.AgentAttachmentRule;
import net.bytebuddy.test.utility.IntegrationRule;
import net.bytebuddy.utility.JavaModule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.none;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class AgentBuilderDefaultApplicationResubmissionTest {

    private static final String FOO = "foo", BAR = "bar";

    private static final long TIMEOUT = 1L;

    @Rule
    public MethodRule agentAttachmentRule = new AgentAttachmentRule();

    @Rule
    public MethodRule integrationRule = new IntegrationRule();

    private ClassLoader classLoader;

    @Before
    public void setUp() throws Exception {
        classLoader = new ByteArrayClassLoader(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassFileLocator.ForClassLoader.readToNames(SimpleType.class));
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
                        public boolean isAlive() {
                            return true;
                        }

                        public Cancelable schedule(final Runnable job) {
                            return new Cancelable.ForFuture(scheduledExecutorService.scheduleWithFixedDelay(job, TIMEOUT, TIMEOUT, TimeUnit.SECONDS));
                        }
                    })
                    .type(ElementMatchers.is(SimpleType.class), ElementMatchers.is(classLoader)).transform(new SampleTransformer())
                    .installOnByteBuddyAgent();
            try {
                Class<?> type = classLoader.loadClass(SimpleType.class.getName());
                Thread.sleep(TimeUnit.SECONDS.toMillis(TIMEOUT * 3));
                assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) BAR));
            } finally {
                ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
            }
        } finally {
            scheduledExecutorService.shutdown();
        }
    }

    @Test
    public void testResubmissionCancelationNonOperational() throws Exception {
        AgentBuilder.RedefinitionStrategy.ResubmissionScheduler.Cancelable.NoOp.INSTANCE.cancel();
    }

    @Test
    public void testResubmissionCancelationForFuture() throws Exception {
        Future<?> future = mock(Future.class);
        new AgentBuilder.RedefinitionStrategy.ResubmissionScheduler.Cancelable.ForFuture(future).cancel();
        verify(future).cancel(true);
        verifyNoMoreInteractions(future);
    }

    private static class SampleTransformer implements AgentBuilder.Transformer {

        public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader, JavaModule module) {
            return builder.method(named(FOO)).intercept(FixedValue.value(BAR));
        }
    }
}
