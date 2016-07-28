package net.bytebuddy.agent.builder;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.TargetType;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.loading.PackageDefinitionStrategy;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.implementation.bind.annotation.Super;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.test.packaging.SimpleOptionalType;
import net.bytebuddy.test.packaging.SimpleType;
import net.bytebuddy.test.utility.AgentAttachmentRule;
import net.bytebuddy.test.utility.ClassFileExtraction;
import net.bytebuddy.test.utility.IntegrationRule;
import net.bytebuddy.test.utility.JavaVersionRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import static net.bytebuddy.matcher.ElementMatchers.any;
import static net.bytebuddy.matcher.ElementMatchers.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class AgentBuilderDefaultApplicationTest {

    private static final ProtectionDomain DEFAULT_PROTECTION_DOMAIN = null;

    private static final String FOO = "foo", BAR = "bar", QUX = "qux";

    private static final String LAMBDA_SAMPLE_FACTORY = "net.bytebuddy.test.precompiled.LambdaSampleFactory";

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        // Travis runs out of memory if all of these tests are run. This property serves as a protection (on some profiles).
        if (Boolean.getBoolean("net.bytebuddy.test.travis")) {
            Logger.getLogger("net.bytebuddy").info("Running only a subset of type locator tests on Travis CI.");
            return Arrays.asList(new Object[][]{
                {AgentBuilder.TypeLocator.Default.EXTENDED},
                {AgentBuilder.TypeLocator.Eager.EXTENDED},
                {AgentBuilder.TypeLocator.ClassLoading.EXTENDED}
            });
        }
        return Arrays.asList(new Object[][]{
                {AgentBuilder.TypeLocator.Default.EXTENDED},
                {AgentBuilder.TypeLocator.Default.FAST},
                {AgentBuilder.TypeLocator.Eager.EXTENDED},
                {AgentBuilder.TypeLocator.Eager.FAST},
                {AgentBuilder.TypeLocator.ClassLoading.EXTENDED},
                {AgentBuilder.TypeLocator.ClassLoading.FAST}
        });
    }

    @Rule
    public MethodRule agentAttachmentRule = new AgentAttachmentRule();

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Rule
    public MethodRule integrationRule = new IntegrationRule();

    private ClassLoader classLoader;

    private final AgentBuilder.TypeLocator typeLocator;

    public AgentBuilderDefaultApplicationTest(AgentBuilder.TypeLocator typeLocator) {
        this.typeLocator = typeLocator;
    }

    @Before
    public void setUp() throws Exception {
        classLoader = new ByteArrayClassLoader.ChildFirst(getClass().getClassLoader(),
                ClassFileExtraction.of(Foo.class,
                        Bar.class,
                        Qux.class,
                        Baz.class,
                        QuxBaz.class,
                        SimpleType.class),
                DEFAULT_PROTECTION_DOMAIN,
                ByteArrayClassLoader.PersistenceHandler.MANIFEST,
                PackageDefinitionStrategy.NoOp.INSTANCE);
    }

    private ClassLoader lambdaSamples() throws Exception {
        return new ByteArrayClassLoader(null,
                ClassFileExtraction.of(Class.forName(LAMBDA_SAMPLE_FACTORY)),
                DEFAULT_PROTECTION_DOMAIN,
                ByteArrayClassLoader.PersistenceHandler.MANIFEST,
                PackageDefinitionStrategy.NoOp.INSTANCE);
    }

    @Test
    @AgentAttachmentRule.Enforce
    @IntegrationRule.Enforce
    public void testAgentWithoutSelfInitialization() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(typeLocator)
                .ignore(none())
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                .type(isAnnotatedWith(ShouldRebase.class), ElementMatchers.is(classLoader)).transform(new FooTransformer())
                .installOnByteBuddyAgent();
        try {
            Class<?> type = classLoader.loadClass(Foo.class.getName());
            assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) BAR));
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
        }
    }

    @Test
    @AgentAttachmentRule.Enforce
    @IntegrationRule.Enforce
    public void testAgentSelfInitialization() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(typeLocator)
                .ignore(none())
                .type(isAnnotatedWith(ShouldRebase.class), ElementMatchers.is(classLoader)).transform(new BarTransformer())
                .installOnByteBuddyAgent();
        try {
            Class<?> type = classLoader.loadClass(Bar.class.getName());
            assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) BAR));
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
        }
    }

    @Test
    @AgentAttachmentRule.Enforce
    @IntegrationRule.Enforce
    public void testAgentSelfInitializationAuxiliaryTypeEager() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(typeLocator)
                .ignore(none())
                .type(isAnnotatedWith(ShouldRebase.class), ElementMatchers.is(classLoader)).transform(new QuxTransformer())
                .installOnByteBuddyAgent();
        try {
            Class<?> type = classLoader.loadClass(Qux.class.getName());
            assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) (FOO + BAR)));
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
        }
    }

    @Test
    @AgentAttachmentRule.Enforce
    @IntegrationRule.Enforce
    public void testAgentSelfInitializationAuxiliaryTypeLazy() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(typeLocator)
                .ignore(none())
                .type(isAnnotatedWith(ShouldRebase.class), ElementMatchers.is(classLoader)).transform(new QuxBazTransformer())
                .installOnByteBuddyAgent();
        try {
            Class<?> type = classLoader.loadClass(QuxBaz.class.getName());
            assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) (FOO + BAR)));
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
        }
    }

    @Test
    @AgentAttachmentRule.Enforce
    @IntegrationRule.Enforce
    public void testAgentWithoutSelfInitializationWithNativeMethodPrefix() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(typeLocator)
                .ignore(none())
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                .enableNativeMethodPrefix(QUX)
                .type(isAnnotatedWith(ShouldRebase.class), ElementMatchers.is(classLoader)).transform(new FooTransformer())
                .installOnByteBuddyAgent();
        try {
            Class<?> type = classLoader.loadClass(Baz.class.getName());
            assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) BAR));
            assertThat(type.getDeclaredMethod(QUX + FOO), notNullValue(Method.class));
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
        }
    }

    @Test
    @AgentAttachmentRule.Enforce(redefinesClasses = true)
    @IntegrationRule.Enforce
    public void testRedefinition() throws Exception {
        // A redefinition reflects on loaded types which are eagerly validated types (Java 7- for redefinition).
        // This causes type equality for outer/inner classes to fail which is why an external class is used.
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        assertThat(classLoader.loadClass(SimpleType.class.getName()).getName(), is(SimpleType.class.getName())); // ensure that class is loaded
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(typeLocator)
                .ignore(none())
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .type(isAnnotatedWith(ShouldRebase.class), ElementMatchers.is(classLoader)).transform(new FooTransformer())
                .installOnByteBuddyAgent();
        try {
            Class<?> type = classLoader.loadClass(SimpleType.class.getName());
            assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) BAR));
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
        }
    }

    @Test
    @AgentAttachmentRule.Enforce(redefinesClasses = true)
    @IntegrationRule.Enforce
    public void testEmptyRedefinition() throws Exception {
        ByteBuddyAgent.getInstrumentation().removeTransformer(new AgentBuilder.Default()
                .with(typeLocator)
                .ignore(any())
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .installOnByteBuddyAgent());
    }

    @Test
    @AgentAttachmentRule.Enforce(redefinesClasses = true)
    @IntegrationRule.Enforce
    public void testChunkedRedefinition() throws Exception {
        // A redefinition reflects on loaded types which are eagerly validated types (Java 7- for redefinition).
        // This causes type equality for outer/inner classes to fail which is why an external class is used.
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        assertThat(classLoader.loadClass(SimpleType.class.getName()).getName(), is(SimpleType.class.getName())); // ensure that class is loaded
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(typeLocator)
                .ignore(none())
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION_CHUNKED)
                .type(isAnnotatedWith(ShouldRebase.class), ElementMatchers.is(classLoader)).transform(new FooTransformer())
                .installOnByteBuddyAgent();
        try {
            Class<?> type = classLoader.loadClass(SimpleType.class.getName());
            assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) BAR));
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
        }
    }

    @Test
    @AgentAttachmentRule.Enforce(redefinesClasses = true)
    @IntegrationRule.Enforce
    public void testEmptyChunkedRedefinition() throws Exception {
        ByteBuddyAgent.getInstrumentation().removeTransformer(new AgentBuilder.Default()
                .with(typeLocator)
                .ignore(any())
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION_CHUNKED)
                .installOnByteBuddyAgent());
    }

    @Test
    @AgentAttachmentRule.Enforce(redefinesClasses = true)
    @IntegrationRule.Enforce
    public void testRedefinitionWithPoolOnly() throws Exception {
        // A redefinition reflects on loaded types which are eagerly validated types (Java 7- for redefinition).
        // This causes type equality for outer/inner classes to fail which is why an external class is used.
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        assertThat(classLoader.loadClass(SimpleType.class.getName()).getName(), is(SimpleType.class.getName())); // ensure that class is loaded
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(typeLocator)
                .ignore(none())
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .with(AgentBuilder.DescriptionStrategy.Default.POOL_ONLY)
                .type(isAnnotatedWith(ShouldRebase.class), ElementMatchers.is(classLoader)).transform(new FooTransformer())
                .installOnByteBuddyAgent();
        try {
            Class<?> type = classLoader.loadClass(SimpleType.class.getName());
            assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) BAR));
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
        }
    }

    @Test
    @AgentAttachmentRule.Enforce(redefinesClasses = true)
    @IntegrationRule.Enforce
    public void testRedefinitionWithPoolFirst() throws Exception {
        // A redefinition reflects on loaded types which are eagerly validated types (Java 7- for redefinition).
        // This causes type equality for outer/inner classes to fail which is why an external class is used.
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        assertThat(classLoader.loadClass(SimpleType.class.getName()).getName(), is(SimpleType.class.getName())); // ensure that class is loaded
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(typeLocator)
                .ignore(none())
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .with(AgentBuilder.DescriptionStrategy.Default.POOL_FIRST)
                .type(isAnnotatedWith(ShouldRebase.class), ElementMatchers.is(classLoader)).transform(new FooTransformer())
                .installOnByteBuddyAgent();
        try {
            Class<?> type = classLoader.loadClass(SimpleType.class.getName());
            assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) BAR));
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
        }
    }

    @Test
    @AgentAttachmentRule.Enforce(redefinesClasses = true)
    @IntegrationRule.Enforce
    public void testRedefinitionWithPoolLastDeferred() throws Exception {
        // A redefinition reflects on loaded types which are eagerly validated types (Java 7- for redefinition).
        // This causes type equality for outer/inner classes to fail which is why an external class is used.
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        assertThat(classLoader.loadClass(SimpleType.class.getName()).getName(), is(SimpleType.class.getName())); // ensure that class is loaded
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(typeLocator)
                .ignore(none())
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .with(AgentBuilder.DescriptionStrategy.Default.POOL_LAST_DEFERRED)
                .type(isAnnotatedWith(ShouldRebase.class), ElementMatchers.is(classLoader)).transform(new FooTransformer())
                .installOnByteBuddyAgent();
        try {
            Class<?> type = classLoader.loadClass(SimpleType.class.getName());
            assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) BAR));
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
        }
    }

    @Test
    @AgentAttachmentRule.Enforce(retransformsClasses = true)
    @IntegrationRule.Enforce
    public void testRetransformation() throws Exception {
        // A redefinition reflects on loaded types which are eagerly validated types (Java 7- for redefinition).
        // This causes type equality for outer/inner classes to fail which is why an external class is used.
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        assertThat(classLoader.loadClass(SimpleType.class.getName()).getName(), is(SimpleType.class.getName())); // ensure that class is loaded
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(typeLocator)
                .ignore(none())
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .type(isAnnotatedWith(ShouldRebase.class), ElementMatchers.is(classLoader)).transform(new FooTransformer())
                .installOnByteBuddyAgent();
        try {
            Class<?> type = classLoader.loadClass(SimpleType.class.getName());
            assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) BAR));
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
        }
    }

    @Test
    @AgentAttachmentRule.Enforce(retransformsClasses = true)
    @IntegrationRule.Enforce
    public void testEmptyRetransformation() throws Exception {
        ByteBuddyAgent.getInstrumentation().removeTransformer(new AgentBuilder.Default()
                .with(typeLocator)
                .ignore(any())
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .installOnByteBuddyAgent());
    }

    @Test
    @AgentAttachmentRule.Enforce(retransformsClasses = true)
    @IntegrationRule.Enforce
    public void testChunkedRetransformation() throws Exception {
        // A redefinition reflects on loaded types which are eagerly validated types (Java 7- for redefinition).
        // This causes type equality for outer/inner classes to fail which is why an external class is used.
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        assertThat(classLoader.loadClass(SimpleType.class.getName()).getName(), is(SimpleType.class.getName())); // ensure that class is loaded
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(typeLocator)
                .ignore(none())
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION_CHUNKED)
                .type(isAnnotatedWith(ShouldRebase.class), ElementMatchers.is(classLoader)).transform(new FooTransformer())
                .installOnByteBuddyAgent();
        try {
            Class<?> type = classLoader.loadClass(SimpleType.class.getName());
            assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) BAR));
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
        }
    }

    @Test
    @AgentAttachmentRule.Enforce(retransformsClasses = true)
    @IntegrationRule.Enforce
    public void testChunkedEmptyRetransformation() throws Exception {
        ByteBuddyAgent.getInstrumentation().removeTransformer(new AgentBuilder.Default()
                .with(typeLocator)
                .ignore(any())
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION_CHUNKED)
                .installOnByteBuddyAgent());
    }

    @Test
    @AgentAttachmentRule.Enforce
    @IntegrationRule.Enforce
    public void testChainedAgent() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        AgentBuilder agentBuilder = new AgentBuilder.Default()
                .with(typeLocator)
                .ignore(none())
                .type(isAnnotatedWith(ShouldRebase.class), ElementMatchers.is(classLoader)).transform(new QuxTransformer());
        ClassFileTransformer firstTransformer = agentBuilder.installOnByteBuddyAgent();
        ClassFileTransformer secondTransformer = agentBuilder.installOnByteBuddyAgent();
        try {
            Class<?> type = classLoader.loadClass(Qux.class.getName());
            assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) (FOO + BAR + BAR)));
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(firstTransformer);
            ByteBuddyAgent.getInstrumentation().removeTransformer(secondTransformer);
        }
    }

    @Test
    @AgentAttachmentRule.Enforce
    @IntegrationRule.Enforce
    public void testSignatureTypesAreAvailableAfterLoad() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(typeLocator)
                .ignore(none())
                .type(isAnnotatedWith(ShouldRebase.class), ElementMatchers.is(classLoader)).transform(new ConstructorTransformer())
                .installOnByteBuddyAgent();
        try {
            Class<?> type = classLoader.loadClass(Foo.class.getName());
            assertThat(type.getDeclaredConstructors().length, is(2));
            assertThat(type.getConstructor().newInstance(), notNullValue(Object.class));
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
        }
    }

    @Test
    @AgentAttachmentRule.Enforce
    @IntegrationRule.Enforce
    public void testDecoration() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(typeLocator)
                .ignore(none())
                .type(ElementMatchers.is(Foo.class), ElementMatchers.is(classLoader)).transform(new BarAdviceTransformer())
                .type(ElementMatchers.is(Foo.class), ElementMatchers.is(classLoader)).transform(new QuxAdviceTransformer()).asDecorator()
                .installOnByteBuddyAgent();
        try {
            Class<?> type = classLoader.loadClass(Foo.class.getName());
            assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) (FOO + BAR + QUX)));
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
        }
    }

    @Test
    @AgentAttachmentRule.Enforce
    @IntegrationRule.Enforce
    public void testDecorationFallThrough() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(typeLocator)
                .ignore(none())
                .type(ElementMatchers.is(Foo.class), ElementMatchers.is(classLoader)).transform(new BarAdviceTransformer()).asDecorator()
                .type(ElementMatchers.is(Foo.class), ElementMatchers.is(classLoader)).transform(new QuxAdviceTransformer()).asDecorator()
                .installOnByteBuddyAgent();
        try {
            Class<?> type = classLoader.loadClass(Foo.class.getName());
            assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) (FOO + BAR + QUX)));
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
        }
    }

    @Test
    @AgentAttachmentRule.Enforce
    @IntegrationRule.Enforce
    public void testDecorationBlocked() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(typeLocator)
                .ignore(none())
                .type(ElementMatchers.is(Foo.class), ElementMatchers.is(classLoader)).transform(new BarAdviceTransformer()).asDecorator()
                .type(ElementMatchers.is(Foo.class), ElementMatchers.is(classLoader)).transform(new QuxAdviceTransformer())
                .installOnByteBuddyAgent();
        try {
            Class<?> type = classLoader.loadClass(Foo.class.getName());
            assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) (FOO + QUX)));
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
        }
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @AgentAttachmentRule.Enforce(redefinesClasses = true)
    @IntegrationRule.Enforce
    public void testNonCapturingLambda() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassLoader classLoader = lambdaSamples();
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(typeLocator)
                .ignore(none())
                .with(AgentBuilder.LambdaInstrumentationStrategy.ENABLED)
                .type(isSubTypeOf(Callable.class)).transform(new SingleMethodReplacer("call"))
                .installOn(ByteBuddyAgent.getInstrumentation());
        try {
            Class<?> sampleFactory = classLoader.loadClass(LAMBDA_SAMPLE_FACTORY);
            @SuppressWarnings("unchecked")
            Callable<String> instance = (Callable<String>) sampleFactory.getDeclaredMethod("nonCapturing").invoke(sampleFactory.getConstructor().newInstance());
            assertThat(instance.call(), is(BAR));
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
            AgentBuilder.LambdaInstrumentationStrategy.release(classFileTransformer, ByteBuddyAgent.getInstrumentation());
        }
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @AgentAttachmentRule.Enforce(redefinesClasses = true)
    @IntegrationRule.Enforce
    public void testNonCapturingLambdaIsConstant() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassLoader classLoader = lambdaSamples();
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(typeLocator)
                .ignore(none())
                .with(AgentBuilder.LambdaInstrumentationStrategy.ENABLED)
                .type(isSubTypeOf(Callable.class)).transform(new SingleMethodReplacer("call"))
                .installOn(ByteBuddyAgent.getInstrumentation());
        try {
            Class<?> sampleFactory = classLoader.loadClass(LAMBDA_SAMPLE_FACTORY);
            assertThat(sampleFactory.getDeclaredMethod("nonCapturing").invoke(sampleFactory.getConstructor().newInstance()),
                    sameInstance(sampleFactory.getDeclaredMethod("nonCapturing").invoke(sampleFactory.getConstructor().newInstance())));
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
            AgentBuilder.LambdaInstrumentationStrategy.release(classFileTransformer, ByteBuddyAgent.getInstrumentation());
        }
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @AgentAttachmentRule.Enforce(redefinesClasses = true)
    @IntegrationRule.Enforce
    public void testLambdaFactoryIsReset() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassLoader classLoader = lambdaSamples();
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(typeLocator)
                .ignore(none())
                .with(AgentBuilder.LambdaInstrumentationStrategy.ENABLED)
                .installOn(ByteBuddyAgent.getInstrumentation());
        ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
        AgentBuilder.LambdaInstrumentationStrategy.release(classFileTransformer, ByteBuddyAgent.getInstrumentation());
        Class<?> sampleFactory = classLoader.loadClass(LAMBDA_SAMPLE_FACTORY);
        @SuppressWarnings("unchecked")
        Callable<String> instance = (Callable<String>) sampleFactory.getDeclaredMethod("nonCapturing").invoke(sampleFactory.getConstructor().newInstance());
        assertThat(instance.call(), is(FOO));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @AgentAttachmentRule.Enforce(redefinesClasses = true)
    @IntegrationRule.Enforce
    public void testArgumentCapturingLambda() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassLoader classLoader = lambdaSamples();
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(typeLocator)
                .ignore(none())
                .with(AgentBuilder.LambdaInstrumentationStrategy.ENABLED)
                .type(isSubTypeOf(Callable.class)).transform(new SingleMethodReplacer("call"))
                .installOn(ByteBuddyAgent.getInstrumentation());
        try {
            Class<?> sampleFactory = classLoader.loadClass(LAMBDA_SAMPLE_FACTORY);
            @SuppressWarnings("unchecked")
            Callable<String> instance = (Callable<String>) sampleFactory.getDeclaredMethod("argumentCapturing", String.class).invoke(sampleFactory.getConstructor().newInstance(), FOO);
            assertThat(instance.call(), is(BAR));
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
            AgentBuilder.LambdaInstrumentationStrategy.release(classFileTransformer, ByteBuddyAgent.getInstrumentation());
        }
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @AgentAttachmentRule.Enforce(redefinesClasses = true)
    @IntegrationRule.Enforce
    public void testArgumentCapturingLambdaIsNotConstant() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassLoader classLoader = lambdaSamples();
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(typeLocator)
                .ignore(none())
                .with(AgentBuilder.LambdaInstrumentationStrategy.ENABLED)
                .type(isSubTypeOf(Callable.class)).transform(new SingleMethodReplacer("call"))
                .installOn(ByteBuddyAgent.getInstrumentation());
        try {
            Class<?> sampleFactory = classLoader.loadClass(LAMBDA_SAMPLE_FACTORY);
            assertThat(sampleFactory.getDeclaredMethod("argumentCapturing", String.class).invoke(sampleFactory.getConstructor().newInstance(), FOO),
                    not(sameInstance(sampleFactory.getDeclaredMethod("argumentCapturing", String.class).invoke(sampleFactory.getConstructor().newInstance(), FOO))));
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
            AgentBuilder.LambdaInstrumentationStrategy.release(classFileTransformer, ByteBuddyAgent.getInstrumentation());
        }
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @AgentAttachmentRule.Enforce(redefinesClasses = true)
    @IntegrationRule.Enforce
    public void testInstanceCapturingLambda() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassLoader classLoader = lambdaSamples();
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(typeLocator)
                .ignore(none())
                .with(AgentBuilder.LambdaInstrumentationStrategy.ENABLED)
                .type(isSubTypeOf(Callable.class)).transform(new SingleMethodReplacer("call"))
                .installOn(ByteBuddyAgent.getInstrumentation());
        try {
            Class<?> sampleFactory = classLoader.loadClass(LAMBDA_SAMPLE_FACTORY);
            @SuppressWarnings("unchecked")
            Callable<String> instance = (Callable<String>) sampleFactory.getDeclaredMethod("instanceCapturing").invoke(sampleFactory.getConstructor().newInstance());
            assertThat(instance.call(), is(BAR));
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
            AgentBuilder.LambdaInstrumentationStrategy.release(classFileTransformer, ByteBuddyAgent.getInstrumentation());
        }
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @AgentAttachmentRule.Enforce(redefinesClasses = true)
    @IntegrationRule.Enforce
    public void testNonCapturingLambdaWithArguments() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassLoader classLoader = lambdaSamples();
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(typeLocator)
                .ignore(none())
                .with(AgentBuilder.LambdaInstrumentationStrategy.ENABLED)
                .type(isSubTypeOf(Class.forName("java.util.function.Function"))).transform(new SingleMethodReplacer("apply"))
                .installOn(ByteBuddyAgent.getInstrumentation());
        try {
            Class<?> sampleFactory = classLoader.loadClass(LAMBDA_SAMPLE_FACTORY);
            Object instance = sampleFactory.getDeclaredMethod("nonCapturingWithArguments").invoke(sampleFactory.getConstructor().newInstance());
            assertThat(instance.getClass().getMethod("apply", Object.class).invoke(instance, FOO), is((Object) BAR));
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
            AgentBuilder.LambdaInstrumentationStrategy.release(classFileTransformer, ByteBuddyAgent.getInstrumentation());
        }
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @AgentAttachmentRule.Enforce(redefinesClasses = true)
    @IntegrationRule.Enforce
    public void testCapturingLambdaWithArguments() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassLoader classLoader = lambdaSamples();
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(typeLocator)
                .ignore(none())
                .with(AgentBuilder.LambdaInstrumentationStrategy.ENABLED)
                .type(isSubTypeOf(Class.forName("java.util.function.Function"))).transform(new SingleMethodReplacer("apply"))
                .installOn(ByteBuddyAgent.getInstrumentation());
        try {
            Class<?> sampleFactory = classLoader.loadClass(LAMBDA_SAMPLE_FACTORY);
            Object instance = sampleFactory.getDeclaredMethod("capturingWithArguments", String.class).invoke(sampleFactory.getConstructor().newInstance(), FOO);
            assertThat(instance.getClass().getMethod("apply", Object.class).invoke(instance, FOO), is((Object) BAR));
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
            AgentBuilder.LambdaInstrumentationStrategy.release(classFileTransformer, ByteBuddyAgent.getInstrumentation());
        }
    }

    @Test
    @JavaVersionRule.Enforce(8)
    @AgentAttachmentRule.Enforce(redefinesClasses = true)
    @IntegrationRule.Enforce
    public void testSerializableLambda() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassLoader classLoader = lambdaSamples();
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(typeLocator)
                .ignore(none())
                .with(AgentBuilder.LambdaInstrumentationStrategy.ENABLED)
                .installOn(ByteBuddyAgent.getInstrumentation());
        try {
            Class<?> sampleFactory = classLoader.loadClass(LAMBDA_SAMPLE_FACTORY);
            @SuppressWarnings("unchecked")
            Callable<String> instance = (Callable<String>) sampleFactory.getDeclaredMethod("serializable", String.class).invoke(sampleFactory.getConstructor().newInstance(), FOO);
            assertThat(instance.call(), is(FOO));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
            objectOutputStream.writeObject(instance);
            objectOutputStream.close();
            ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(outputStream.toByteArray()));
            @SuppressWarnings("unchecked")
            Callable<String> deserialized = (Callable<String>) objectInputStream.readObject();
            assertThat(deserialized.call(), is(FOO));
            objectInputStream.close();
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
            AgentBuilder.LambdaInstrumentationStrategy.release(classFileTransformer, ByteBuddyAgent.getInstrumentation());
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface ShouldRebase {
        /* empty */
    }

    private static class FooTransformer implements AgentBuilder.Transformer {

        @Override
        public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader) {
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
        public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader) {
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
        public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader) {
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

    public static class QuxBazTransformer implements AgentBuilder.Transformer {

        @Override
        public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader) {
            try {
                return builder.method(named(FOO)).intercept(MethodDelegation.to(new Interceptor()));
            } catch (Exception exception) {
                throw new AssertionError(exception);
            }
        }

        public static class Interceptor {

            // Interceptor cannot reference QuxBaz as the system class loader type does not equal the child-first type
            public String intercept(@Super(proxyType = TargetType.class) Object zuper) throws Exception {
                return zuper.getClass().getClassLoader().loadClass(QuxBaz.class.getName()).getDeclaredMethod("foo").invoke(zuper) + BAR;
            }
        }
    }

    @ShouldRebase
    public static class QuxBaz {

        public String foo() {
            return FOO;
        }
    }

    public static class ConstructorTransformer implements AgentBuilder.Transformer {

        @Override
        public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader) {
            return builder.constructor(ElementMatchers.any()).intercept(SuperMethodCall.INSTANCE);
        }
    }

    private static class SingleMethodReplacer implements AgentBuilder.Transformer {

        private final String methodName;

        public SingleMethodReplacer(String methodName) {
            this.methodName = methodName;
        }

        @Override
        public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader) {
            return builder.method(named(methodName)).intercept(FixedValue.value(BAR));
        }
    }

    public static class BarAdviceTransformer implements AgentBuilder.Transformer {

        @Override
        public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader) {
            return builder.visit(Advice.to(BarAdvice.class).on(named(FOO)));
        }
    }

    public static class QuxAdviceTransformer implements AgentBuilder.Transformer {

        @Override
        public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader) {
            return builder.visit(Advice.to(QuxAdvice.class).on(named(FOO)));
        }
    }

    private static class BarAdvice {

        @Advice.OnMethodExit
        private static void exit(@Advice.Return(readOnly = false) String value) {
            value += BAR;
        }
    }

    private static class QuxAdvice {

        @Advice.OnMethodExit
        private static void exit(@Advice.Return(readOnly = false) String value) {
            value += QUX;
        }
    }
}
