package net.bytebuddy.agent.builder;

import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import net.bytebuddy.dynamic.loading.PackageDefinitionStrategy;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.test.utility.AgentAttachmentRule;
import net.bytebuddy.test.utility.ClassFileExtraction;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.ProtectionDomain;
import java.util.concurrent.Callable;

import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class AgentBuilderDefaultApplicationTest {

    private static final ProtectionDomain DEFAULT_PROTECTION_DOMAIN = null;

    private static final String FOO = "foo", BAR = "bar", QUX = "qux";

    @Rule
    public MethodRule agentAttachmentRule = new AgentAttachmentRule();

    private ClassLoader classLoader;

    @Before
    public void setUp() throws Exception {
        // Need to add all enclosing classes. Otherwise, eagerly validated types (Java 7- for redefinition)
        // fail to validate type equality for outer/inner classes.
        classLoader = new ByteArrayClassLoader.ChildFirst(getClass().getClassLoader(),
                ClassFileExtraction.of(Foo.class,
                        Bar.class,
                        Qux.class,
                        Baz.class,
                        getClass(),
                        ShouldRebase.class,
                        FooTransformer.class,
                        BarTransformer.class,
                        BarTransformer.Interceptor.class,
                        QuxTransformer.class),
                DEFAULT_PROTECTION_DOMAIN,
                AccessController.getContext(),
                ByteArrayClassLoader.PersistenceHandler.MANIFEST,
                PackageDefinitionStrategy.NoOp.INSTANCE);
    }

    @Test
    @AgentAttachmentRule.Enforce
    public void testAgentWithoutSelfInitialization() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .withInitialization(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                .type(isAnnotatedWith(ShouldRebase.class), ElementMatchers.is(classLoader)).transform(new FooTransformer())
                .installOnByteBuddyAgent();
        try {
            Class<?> type = classLoader.loadClass(Foo.class.getName());
            assertThat(type.getDeclaredMethod(FOO).invoke(type.newInstance()), is((Object) BAR));
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
        }
    }

    @Test
    @AgentAttachmentRule.Enforce
    public void testAgentSelfInitialization() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .type(isAnnotatedWith(ShouldRebase.class), ElementMatchers.is(classLoader)).transform(new BarTransformer())
                .installOnByteBuddyAgent();
        try {
            Class<?> type = classLoader.loadClass(Bar.class.getName());
            assertThat(type.getDeclaredMethod(FOO).invoke(type.newInstance()), is((Object) BAR));
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
        }
    }

    @Test
    @AgentAttachmentRule.Enforce
    public void testAgentSelfInitializationAuxiliaryTypes() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .type(isAnnotatedWith(ShouldRebase.class), ElementMatchers.is(classLoader)).transform(new QuxTransformer())
                .installOnByteBuddyAgent();
        try {
            Class<?> type = classLoader.loadClass(Qux.class.getName());
            assertThat(type.getDeclaredMethod(FOO).invoke(type.newInstance()), is((Object) (FOO + BAR)));
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
        }
    }

    @Test
    @AgentAttachmentRule.Enforce
    public void testAgentWithoutSelfInitializationWithNativeMethodPrefix() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .withInitialization(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                .withNativeMethodPrefix(QUX)
                .type(isAnnotatedWith(ShouldRebase.class), ElementMatchers.is(classLoader)).transform(new FooTransformer())
                .installOnByteBuddyAgent();
        try {
            Class<?> type = classLoader.loadClass(Baz.class.getName());
            assertThat(type.getDeclaredMethod(FOO).invoke(type.newInstance()), is((Object) BAR));
            assertThat(type.getDeclaredMethod(QUX + FOO), notNullValue(Method.class));
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
        }
    }

    @Test
    @AgentAttachmentRule.Enforce
    public void testRedefinition() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        assertThat(classLoader.loadClass(Foo.class.getName()).getName(), is(Foo.class.getName())); // ensure that class is loaded
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .withInitialization(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                .withTypeStrategy(AgentBuilder.TypeStrategy.REDEFINE)
                .withRedefinitionStrategy(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .type(isAnnotatedWith(ShouldRebase.class), ElementMatchers.is(classLoader)).transform(new FooTransformer())
                .installOnByteBuddyAgent();
        try {
            Class<?> type = classLoader.loadClass(Foo.class.getName());
            assertThat(type.getDeclaredMethod(FOO).invoke(type.newInstance()), is((Object) BAR));
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
        }
    }

    @Test
    @AgentAttachmentRule.Enforce
    public void testRetransformation() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        assertThat(classLoader.loadClass(Foo.class.getName()).getName(), is(Foo.class.getName())); // ensure that class is loaded
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .withInitialization(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                .withTypeStrategy(AgentBuilder.TypeStrategy.REDEFINE)
                .withRedefinitionStrategy(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .type(isAnnotatedWith(ShouldRebase.class), ElementMatchers.is(classLoader)).transform(new FooTransformer())
                .installOnByteBuddyAgent();
        try {
            Class<?> type = classLoader.loadClass(Foo.class.getName());
            assertThat(type.getDeclaredMethod(FOO).invoke(type.newInstance()), is((Object) BAR));
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface ShouldRebase {

    }

    private static class FooTransformer implements AgentBuilder.Transformer {

        @Override
        public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription) {
            return builder.method(named(FOO)).intercept(FixedValue.value(BAR));
        }
    }

    @ShouldRebase
    public static class Foo {

        public String foo() {
            return FOO;
        }
    }

    @ShouldRebase
    public static class Baz {

        public String foo() {
            return FOO;
        }
    }

    public static class BarTransformer implements AgentBuilder.Transformer {

        @Override
        public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription) {
            try {
                return builder.method(named(FOO)).intercept(MethodDelegation.to(new Interceptor()));
            } catch (Exception exception) {
                throw new AssertionError(exception);
            }
        }

        public static class Interceptor {

            public String intercept() {
                return BAR;
            }
        }
    }

    @ShouldRebase
    public static class Bar {

        public String foo() {
            return FOO;
        }
    }

    public static class QuxTransformer implements AgentBuilder.Transformer {

        @Override
        public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription) {
            try {
                return builder.method(named(FOO)).intercept(MethodDelegation.to(new Interceptor()));
            } catch (Exception exception) {
                throw new AssertionError(exception);
            }
        }

        public static class Interceptor {

            public String intercept(@SuperCall Callable<String> zuper) throws Exception {
                return zuper.call() + BAR;
            }
        }
    }

    @ShouldRebase
    public static class Qux {

        public String foo() {
            return FOO;
        }
    }
}
