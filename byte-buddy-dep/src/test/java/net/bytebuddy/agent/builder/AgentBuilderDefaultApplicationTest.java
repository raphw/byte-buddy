package net.bytebuddy.agent.builder;

import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.TargetType;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import net.bytebuddy.dynamic.loading.ClassInjector;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.implementation.bind.annotation.Super;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bytecode.Removal;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.test.packaging.SimpleType;
import net.bytebuddy.test.utility.AgentAttachmentRule;
import net.bytebuddy.test.utility.ClassReflectionInjectionAvailableRule;
import net.bytebuddy.test.utility.IntegrationRule;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.utility.JavaModule;
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
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Callable;

import static net.bytebuddy.matcher.ElementMatchers.any;
import static net.bytebuddy.matcher.ElementMatchers.isSubTypeOf;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.none;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class AgentBuilderDefaultApplicationTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux";

    private static final String LAMBDA_SAMPLE_FACTORY = "net.bytebuddy.test.precompiled.v8.LambdaSampleFactory";

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {AgentBuilder.PoolStrategy.Default.EXTENDED},
                {AgentBuilder.PoolStrategy.Default.FAST},
                {AgentBuilder.PoolStrategy.Eager.EXTENDED},
                {AgentBuilder.PoolStrategy.Eager.FAST},
                {AgentBuilder.PoolStrategy.ClassLoading.EXTENDED},
                {AgentBuilder.PoolStrategy.ClassLoading.FAST}
        });
    }

    @Rule
    public MethodRule agentAttachmentRule = new AgentAttachmentRule();

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Rule
    public MethodRule integrationRule = new IntegrationRule();

    @Rule
    public MethodRule classUnsafeInjectionAvailableRule = new ClassReflectionInjectionAvailableRule();

    private ClassLoader classLoader;

    private final AgentBuilder.PoolStrategy poolStrategy;

    public AgentBuilderDefaultApplicationTest(AgentBuilder.PoolStrategy poolStrategy) {
        this.poolStrategy = poolStrategy;
    }

    @Before
    public void setUp() throws Exception {
        classLoader = new ByteArrayClassLoader.ChildFirst(getClass().getClassLoader(),
                ClassFileLocator.ForClassLoader.readToNames(Foo.class,
                        Bar.class,
                        Qux.class,
                        Baz.class,
                        QuxBaz.class,
                        SimpleType.class),
                ByteArrayClassLoader.PersistenceHandler.MANIFEST);
    }

    private ClassLoader lambdaSamples() throws Exception {
        return new ByteArrayClassLoader(ClassLoadingStrategy.BOOTSTRAP_LOADER,
                ClassFileLocator.ForClassLoader.readToNames(Class.forName(LAMBDA_SAMPLE_FACTORY)),
                ByteArrayClassLoader.PersistenceHandler.MANIFEST);
    }

    @Test
    @AgentAttachmentRule.Enforce
    @IntegrationRule.Enforce
    public void testAgentWithoutSelfInitialization() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(poolStrategy)
                .ignore(none())
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                .type(ElementMatchers.is(Foo.class), ElementMatchers.is(classLoader)).transform(new FooTransformer())
                .installOnByteBuddyAgent();
        try {
            Class<?> type = classLoader.loadClass(Foo.class.getName());
            assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) BAR));
        } finally {
            assertThat(ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer), is(true));
        }
    }

    @Test
    @AgentAttachmentRule.Enforce
    @IntegrationRule.Enforce
    public void testAgentSelfInitialization() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(poolStrategy)
                .ignore(none())
                .type(ElementMatchers.is(Bar.class), ElementMatchers.is(classLoader)).transform(new BarTransformer())
                .installOnByteBuddyAgent();
        try {
            Class<?> type = classLoader.loadClass(Bar.class.getName());
            assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) BAR));
        } finally {
            assertThat(ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer), is(true));
        }
    }

    @Test
    @AgentAttachmentRule.Enforce
    @IntegrationRule.Enforce
    @ClassReflectionInjectionAvailableRule.Enforce
    public void testAgentSelfInitializationAuxiliaryTypeEager() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(poolStrategy)
                .ignore(none())
                .type(ElementMatchers.is(Qux.class), ElementMatchers.is(classLoader)).transform(new QuxTransformer())
                .installOnByteBuddyAgent();
        try {
            Class<?> type = classLoader.loadClass(Qux.class.getName());
            assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) (FOO + BAR)));
        } finally {
            assertThat(ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer), is(true));
        }
    }

    @Test
    @AgentAttachmentRule.Enforce
    @IntegrationRule.Enforce
    @ClassReflectionInjectionAvailableRule.Enforce
    public void testAgentSelfInitializationAuxiliaryTypeLazy() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(poolStrategy)
                .ignore(none())
                .type(ElementMatchers.is(QuxBaz.class), ElementMatchers.is(classLoader)).transform(new QuxBazTransformer())
                .installOnByteBuddyAgent();
        try {
            Class<?> type = classLoader.loadClass(QuxBaz.class.getName());
            assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) (FOO + BAR)));
        } finally {
            assertThat(ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer), is(true));
        }
    }

    @Test
    @AgentAttachmentRule.Enforce
    @IntegrationRule.Enforce
    public void testAgentWithoutSelfInitializationWithNativeMethodPrefix() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(poolStrategy)
                .ignore(none())
                .enableNativeMethodPrefix(QUX)
                .type(ElementMatchers.is(Baz.class), ElementMatchers.is(classLoader)).transform(new FooTransformer())
                .installOnByteBuddyAgent();
        try {
            Class<?> type = classLoader.loadClass(Baz.class.getName());
            assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) BAR));
            assertThat(type.getDeclaredMethod(QUX + FOO), notNullValue(Method.class));
        } finally {
            assertThat(ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer), is(true));
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
                .with(poolStrategy)
                .ignore(none())
                .disableClassFormatChanges()
                .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .type(ElementMatchers.is(SimpleType.class), ElementMatchers.is(classLoader)).transform(new FooTransformer())
                .installOnByteBuddyAgent();
        try {
            Class<?> type = classLoader.loadClass(SimpleType.class.getName());
            assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) BAR));
        } finally {
            assertThat(ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer), is(true));
        }
    }

    @Test
    @AgentAttachmentRule.Enforce(redefinesClasses = true)
    @IntegrationRule.Enforce
    public void testRedefinitionWithReset() throws Exception {
        // A redefinition reflects on loaded types which are eagerly validated types (Java 7- for redefinition).
        // This causes type equality for outer/inner classes to fail which is why an external class is used.
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        assertThat(classLoader.loadClass(SimpleType.class.getName()).getName(), is(SimpleType.class.getName())); // ensure that class is loaded
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(poolStrategy)
                .ignore(none())
                .disableClassFormatChanges()
                .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .type(ElementMatchers.is(SimpleType.class), ElementMatchers.is(classLoader)).transform(new FooTransformer())
                .installOnByteBuddyAgent();
        try {
            Class<?> type = classLoader.loadClass(SimpleType.class.getName());
            assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) BAR));
        } finally {
            assertThat(classFileTransformer.reset(ByteBuddyAgent.getInstrumentation(), AgentBuilder.RedefinitionStrategy.REDEFINITION), is(true));
        }
        Class<?> type = classLoader.loadClass(SimpleType.class.getName());
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
    }

    @Test
    @AgentAttachmentRule.Enforce(redefinesClasses = true)
    @IntegrationRule.Enforce
    public void testEmptyRedefinition() throws Exception {
        ByteBuddyAgent.getInstrumentation().removeTransformer(new AgentBuilder.Default()
                .with(poolStrategy)
                .ignore(any())
                .disableClassFormatChanges()
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
                .with(poolStrategy)
                .ignore(none())
                .disableClassFormatChanges()
                .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .with(AgentBuilder.RedefinitionStrategy.BatchAllocator.ForFixedSize.ofSize(1))
                .type(ElementMatchers.is(SimpleType.class), ElementMatchers.is(classLoader)).transform(new FooTransformer())
                .installOnByteBuddyAgent();
        try {
            Class<?> type = classLoader.loadClass(SimpleType.class.getName());
            assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) BAR));
        } finally {
            assertThat(ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer), is(true));
        }
    }

    @Test
    @AgentAttachmentRule.Enforce(redefinesClasses = true)
    @IntegrationRule.Enforce
    public void testEmptyChunkedRedefinition() throws Exception {
        ByteBuddyAgent.getInstrumentation().removeTransformer(new AgentBuilder.Default()
                .with(poolStrategy)
                .ignore(any())
                .disableClassFormatChanges()
                .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .with(AgentBuilder.RedefinitionStrategy.BatchAllocator.ForFixedSize.ofSize(1))
                .installOnByteBuddyAgent());
    }

    @Test
    @AgentAttachmentRule.Enforce(redefinesClasses = true)
    @IntegrationRule.Enforce
    public void testRedefinitionWithExplicitTypes() throws Exception {
        // A redefinition reflects on loaded types which are eagerly validated types (Java 7- for redefinition).
        // This causes type equality for outer/inner classes to fail which is why an external class is used.
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        assertThat(classLoader.loadClass(SimpleType.class.getName()).getName(), is(SimpleType.class.getName())); // ensure that class is loaded
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(poolStrategy)
                .ignore(none())
                .disableClassFormatChanges()
                .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .redefineOnly(classLoader.loadClass(SimpleType.class.getName()))
                .type(ElementMatchers.is(SimpleType.class), ElementMatchers.is(classLoader)).transform(new FooTransformer())
                .installOnByteBuddyAgent();
        try {
            Class<?> type = classLoader.loadClass(SimpleType.class.getName());
            assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) BAR));
        } finally {
            assertThat(ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer), is(true));
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
                .with(poolStrategy)
                .ignore(none())
                .disableClassFormatChanges()
                .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .type(ElementMatchers.is(SimpleType.class), ElementMatchers.is(classLoader)).transform(new FooTransformer())
                .installOnByteBuddyAgent();
        try {
            Class<?> type = classLoader.loadClass(SimpleType.class.getName());
            assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) BAR));
        } finally {
            assertThat(ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer), is(true));
        }
    }

    @Test
    @AgentAttachmentRule.Enforce(retransformsClasses = true)
    @IntegrationRule.Enforce
    public void testRetransformationWithReset() throws Exception {
        // A redefinition reflects on loaded types which are eagerly validated types (Java 7- for redefinition).
        // This causes type equality for outer/inner classes to fail which is why an external class is used.
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        assertThat(classLoader.loadClass(SimpleType.class.getName()).getName(), is(SimpleType.class.getName())); // ensure that class is loaded
        ResettableClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(poolStrategy)
                .ignore(none())
                .disableClassFormatChanges()
                .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .type(ElementMatchers.is(SimpleType.class), ElementMatchers.is(classLoader)).transform(new FooTransformer())
                .installOnByteBuddyAgent();
        try {
            Class<?> type = classLoader.loadClass(SimpleType.class.getName());
            assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) BAR));
        } finally {
            assertThat(classFileTransformer.reset(ByteBuddyAgent.getInstrumentation(), AgentBuilder.RedefinitionStrategy.RETRANSFORMATION), is(true));
        }
        Class<?> type = classLoader.loadClass(SimpleType.class.getName());
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) FOO));
    }

    @Test
    @AgentAttachmentRule.Enforce(retransformsClasses = true)
    @IntegrationRule.Enforce
    public void testEmptyRetransformation() throws Exception {
        ByteBuddyAgent.getInstrumentation().removeTransformer(new AgentBuilder.Default()
                .with(poolStrategy)
                .ignore(any())
                .disableClassFormatChanges()
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
                .with(poolStrategy)
                .ignore(none())
                .disableClassFormatChanges()
                .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(AgentBuilder.RedefinitionStrategy.BatchAllocator.ForFixedSize.ofSize(1))
                .type(ElementMatchers.is(SimpleType.class), ElementMatchers.is(classLoader)).transform(new FooTransformer())
                .installOnByteBuddyAgent();
        try {
            Class<?> type = classLoader.loadClass(SimpleType.class.getName());
            assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) BAR));
        } finally {
            assertThat(ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer), is(true));
        }
    }

    @Test
    @AgentAttachmentRule.Enforce(retransformsClasses = true)
    @IntegrationRule.Enforce
    public void testChunkedEmptyRetransformation() throws Exception {
        ByteBuddyAgent.getInstrumentation().removeTransformer(new AgentBuilder.Default()
                .with(poolStrategy)
                .ignore(any())
                .disableClassFormatChanges()
                .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(AgentBuilder.RedefinitionStrategy.BatchAllocator.ForFixedSize.ofSize(1))
                .installOnByteBuddyAgent());
    }

    @Test
    @AgentAttachmentRule.Enforce(retransformsClasses = true)
    @IntegrationRule.Enforce
    public void testRetransformationWithExplicitTypes() throws Exception {
        // A redefinition reflects on loaded types which are eagerly validated types (Java 7- for redefinition).
        // This causes type equality for outer/inner classes to fail which is why an external class is used.
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        assertThat(classLoader.loadClass(SimpleType.class.getName()).getName(), is(SimpleType.class.getName())); // ensure that class is loaded
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(poolStrategy)
                .ignore(none())
                .disableClassFormatChanges()
                .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .redefineOnly(classLoader.loadClass(SimpleType.class.getName()))
                .type(ElementMatchers.is(SimpleType.class), ElementMatchers.is(classLoader)).transform(new FooTransformer())
                .installOnByteBuddyAgent();
        try {
            Class<?> type = classLoader.loadClass(SimpleType.class.getName());
            assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) BAR));
        } finally {
            assertThat(ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer), is(true));
        }
    }

    @Test
    @AgentAttachmentRule.Enforce
    @IntegrationRule.Enforce
    @ClassReflectionInjectionAvailableRule.Enforce
    public void testChainedAgent() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        AgentBuilder agentBuilder = new AgentBuilder.Default()
                .with(poolStrategy)
                .ignore(none())
                .type(ElementMatchers.is(Qux.class), ElementMatchers.is(classLoader)).transform(new QuxTransformer());
        ClassFileTransformer firstTransformer = agentBuilder.installOnByteBuddyAgent();
        ClassFileTransformer secondTransformer = agentBuilder.installOnByteBuddyAgent();
        try {
            Class<?> type = classLoader.loadClass(Qux.class.getName());
            assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) (FOO + BAR + BAR)));
        } finally {
            assertThat(ByteBuddyAgent.getInstrumentation().removeTransformer(firstTransformer), is(true));
            assertThat(ByteBuddyAgent.getInstrumentation().removeTransformer(secondTransformer), is(true));
        }
    }

    @Test
    @AgentAttachmentRule.Enforce
    @IntegrationRule.Enforce
    @ClassReflectionInjectionAvailableRule.Enforce
    public void testSignatureTypesAreAvailableAfterLoad() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(poolStrategy)
                .ignore(none())
                .type(ElementMatchers.is(Foo.class), ElementMatchers.is(classLoader)).transform(new ConstructorTransformer())
                .installOnByteBuddyAgent();
        try {
            Class<?> type = classLoader.loadClass(Foo.class.getName());
            assertThat(type.getDeclaredConstructors().length, is(2));
            assertThat(type.getDeclaredConstructor().newInstance(), notNullValue(Object.class));
        } finally {
            assertThat(ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer), is(true));
        }
    }

    @Test
    @AgentAttachmentRule.Enforce
    @IntegrationRule.Enforce
    public void testDecoration() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(poolStrategy)
                .ignore(none())
                .type(ElementMatchers.is(Foo.class), ElementMatchers.is(classLoader)).transform(new QuxAdviceTransformer())
                .type(ElementMatchers.is(Foo.class), ElementMatchers.is(classLoader)).transform(new BarAdviceTransformer())
                .installOnByteBuddyAgent();
        try {
            Class<?> type = classLoader.loadClass(Foo.class.getName());
            assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) (FOO + BAR + QUX)));
        } finally {
            assertThat(ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer), is(true));
        }
    }

    @Test
    @AgentAttachmentRule.Enforce
    @IntegrationRule.Enforce
    public void testDecorationFallThrough() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(poolStrategy)
                .ignore(none())
                .type(ElementMatchers.is(Foo.class), ElementMatchers.is(classLoader)).transform(new QuxAdviceTransformer())
                .type(ElementMatchers.is(Foo.class), ElementMatchers.is(classLoader)).transform(new BarAdviceTransformer())
                .installOnByteBuddyAgent();
        try {
            Class<?> type = classLoader.loadClass(Foo.class.getName());
            assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) (FOO + BAR + QUX)));
        } finally {
            assertThat(ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer), is(true));
        }
    }

    @Test
    @AgentAttachmentRule.Enforce
    @IntegrationRule.Enforce
    public void testDecorationBlocked() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(poolStrategy)
                .ignore(none())
                .type(ElementMatchers.is(Foo.class), ElementMatchers.is(classLoader)).transform(new QuxAdviceTransformer()).asTerminalTransformation()
                .type(ElementMatchers.is(Foo.class), ElementMatchers.is(classLoader)).transform(new BarAdviceTransformer())
                .installOnByteBuddyAgent();
        try {
            Class<?> type = classLoader.loadClass(Foo.class.getName());
            assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) (FOO + QUX)));
        } finally {
            assertThat(ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer), is(true));
        }
    }

    @Test
    @JavaVersionRule.Enforce(value = 8, atMost = 23, j9 = false)
    @AgentAttachmentRule.Enforce
    @IntegrationRule.Enforce
    public void testNonCapturingLambda() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassLoader classLoader = lambdaSamples();
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(poolStrategy)
                .ignore(none())
                .with(AgentBuilder.LambdaInstrumentationStrategy.ENABLED)
                .type(isSubTypeOf(Callable.class)).transform(new SingleMethodReplacer("call"))
                .installOn(ByteBuddyAgent.getInstrumentation());
        try {
            Class<?> sampleFactory = classLoader.loadClass(LAMBDA_SAMPLE_FACTORY);
            @SuppressWarnings("unchecked")
            Callable<String> instance = (Callable<String>) sampleFactory.getDeclaredMethod("nonCapturing").invoke(sampleFactory.getDeclaredConstructor().newInstance());
            assertThat(instance.call(), is(BAR));
        } finally {
            assertThat(ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer), is(true));
            AgentBuilder.LambdaInstrumentationStrategy.release(classFileTransformer, ByteBuddyAgent.getInstrumentation());
        }
    }

    @Test
    @JavaVersionRule.Enforce(value = 8, atMost = 23, j9 = false)
    @AgentAttachmentRule.Enforce
    @IntegrationRule.Enforce
    public void testNonCapturingLambdaIsConstant() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassLoader classLoader = lambdaSamples();
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(poolStrategy)
                .ignore(none())
                .with(AgentBuilder.LambdaInstrumentationStrategy.ENABLED)
                .type(isSubTypeOf(Callable.class)).transform(new SingleMethodReplacer("call"))
                .installOn(ByteBuddyAgent.getInstrumentation());
        try {
            Class<?> sampleFactory = classLoader.loadClass(LAMBDA_SAMPLE_FACTORY);
            assertThat(sampleFactory.getDeclaredMethod("nonCapturing").invoke(sampleFactory.getDeclaredConstructor().newInstance()),
                    sameInstance(sampleFactory.getDeclaredMethod("nonCapturing").invoke(sampleFactory.getDeclaredConstructor().newInstance())));
        } finally {
            assertThat(ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer), is(true));
            AgentBuilder.LambdaInstrumentationStrategy.release(classFileTransformer, ByteBuddyAgent.getInstrumentation());
        }
    }

    @Test
    @JavaVersionRule.Enforce(value = 8, atMost = 23, j9 = false)
    @AgentAttachmentRule.Enforce
    @IntegrationRule.Enforce
    public void testLambdaFactoryIsReset() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassLoader classLoader = lambdaSamples();
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(poolStrategy)
                .ignore(none())
                .with(AgentBuilder.LambdaInstrumentationStrategy.ENABLED)
                .installOn(ByteBuddyAgent.getInstrumentation());
        ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
        AgentBuilder.LambdaInstrumentationStrategy.release(classFileTransformer, ByteBuddyAgent.getInstrumentation());
        Class<?> sampleFactory = classLoader.loadClass(LAMBDA_SAMPLE_FACTORY);
        @SuppressWarnings("unchecked")
        Callable<String> instance = (Callable<String>) sampleFactory.getDeclaredMethod("nonCapturing").invoke(sampleFactory.getDeclaredConstructor().newInstance());
        assertThat(instance.call(), is(FOO));
    }

    @Test
    @JavaVersionRule.Enforce(value = 8, atMost = 23, j9 = false)
    @AgentAttachmentRule.Enforce
    @IntegrationRule.Enforce
    public void testArgumentCapturingLambda() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassLoader classLoader = lambdaSamples();
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(poolStrategy)
                .ignore(none())
                .with(AgentBuilder.LambdaInstrumentationStrategy.ENABLED)
                .type(isSubTypeOf(Callable.class)).transform(new SingleMethodReplacer("call"))
                .installOn(ByteBuddyAgent.getInstrumentation());
        try {
            Class<?> sampleFactory = classLoader.loadClass(LAMBDA_SAMPLE_FACTORY);
            @SuppressWarnings("unchecked")
            Callable<String> instance = (Callable<String>) sampleFactory.getDeclaredMethod("argumentCapturing", String.class).invoke(sampleFactory.getDeclaredConstructor().newInstance(), FOO);
            assertThat(instance.call(), is(BAR));
        } finally {
            assertThat(ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer), is(true));
            AgentBuilder.LambdaInstrumentationStrategy.release(classFileTransformer, ByteBuddyAgent.getInstrumentation());
        }
    }

    @Test
    @JavaVersionRule.Enforce(value = 8, atMost = 23, j9 = false)
    @AgentAttachmentRule.Enforce
    @IntegrationRule.Enforce
    public void testArgumentCapturingLambdaIsNotConstant() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassLoader classLoader = lambdaSamples();
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(poolStrategy)
                .ignore(none())
                .with(AgentBuilder.LambdaInstrumentationStrategy.ENABLED)
                .type(isSubTypeOf(Callable.class)).transform(new SingleMethodReplacer("call"))
                .installOn(ByteBuddyAgent.getInstrumentation());
        try {
            Class<?> sampleFactory = classLoader.loadClass(LAMBDA_SAMPLE_FACTORY);
            assertThat(sampleFactory.getDeclaredMethod("argumentCapturing", String.class).invoke(sampleFactory.getDeclaredConstructor().newInstance(), FOO),
                    not(sameInstance(sampleFactory.getDeclaredMethod("argumentCapturing", String.class).invoke(sampleFactory.getDeclaredConstructor().newInstance(), FOO))));
        } finally {
            assertThat(ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer), is(true));
            AgentBuilder.LambdaInstrumentationStrategy.release(classFileTransformer, ByteBuddyAgent.getInstrumentation());
        }
    }

    @Test
    @JavaVersionRule.Enforce(value = 8, atMost = 23, j9 = false)
    @AgentAttachmentRule.Enforce
    @IntegrationRule.Enforce
    public void testInstanceCapturingLambda() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassLoader classLoader = lambdaSamples();
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(poolStrategy)
                .ignore(none())
                .with(AgentBuilder.LambdaInstrumentationStrategy.ENABLED)
                .type(isSubTypeOf(Callable.class)).transform(new SingleMethodReplacer("call"))
                .installOn(ByteBuddyAgent.getInstrumentation());
        try {
            Class<?> sampleFactory = classLoader.loadClass(LAMBDA_SAMPLE_FACTORY);
            @SuppressWarnings("unchecked")
            Callable<String> instance = (Callable<String>) sampleFactory.getDeclaredMethod("instanceCapturing").invoke(sampleFactory.getDeclaredConstructor().newInstance());
            assertThat(instance.call(), is(BAR));
        } finally {
            assertThat(ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer), is(true));
            AgentBuilder.LambdaInstrumentationStrategy.release(classFileTransformer, ByteBuddyAgent.getInstrumentation());
        }
    }

    @Test
    @JavaVersionRule.Enforce(value = 8, atMost = 23, j9 = false)
    @AgentAttachmentRule.Enforce
    @IntegrationRule.Enforce
    public void testNonCapturingLambdaWithArguments() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassLoader classLoader = lambdaSamples();
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(poolStrategy)
                .ignore(none())
                .with(AgentBuilder.LambdaInstrumentationStrategy.ENABLED)
                .type(isSubTypeOf(Class.forName("java.util.function.Function"))).transform(new SingleMethodReplacer("apply"))
                .installOn(ByteBuddyAgent.getInstrumentation());
        try {
            Class<?> sampleFactory = classLoader.loadClass(LAMBDA_SAMPLE_FACTORY);
            Object instance = sampleFactory.getDeclaredMethod("nonCapturingWithArguments").invoke(sampleFactory.getDeclaredConstructor().newInstance());
            assertThat(instance.getClass().getMethod("apply", Object.class).invoke(instance, FOO), is((Object) BAR));
        } finally {
            assertThat(ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer), is(true));
            AgentBuilder.LambdaInstrumentationStrategy.release(classFileTransformer, ByteBuddyAgent.getInstrumentation());
        }
    }

    @Test
    @JavaVersionRule.Enforce(value = 8, atMost = 23, j9 = false)
    @AgentAttachmentRule.Enforce
    @IntegrationRule.Enforce
    public void testCapturingLambdaWithArguments() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassLoader classLoader = lambdaSamples();
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(poolStrategy)
                .ignore(none())
                .with(AgentBuilder.LambdaInstrumentationStrategy.ENABLED)
                .type(isSubTypeOf(Class.forName("java.util.function.Function"))).transform(new SingleMethodReplacer("apply"))
                .installOn(ByteBuddyAgent.getInstrumentation());
        try {
            Class<?> sampleFactory = classLoader.loadClass(LAMBDA_SAMPLE_FACTORY);
            Object instance = sampleFactory.getDeclaredMethod("capturingWithArguments", String.class).invoke(sampleFactory.getDeclaredConstructor().newInstance(), FOO);
            assertThat(instance.getClass().getMethod("apply", Object.class).invoke(instance, FOO), is((Object) BAR));
        } finally {
            assertThat(ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer), is(true));
            AgentBuilder.LambdaInstrumentationStrategy.release(classFileTransformer, ByteBuddyAgent.getInstrumentation());
        }
    }

    @Test
    @JavaVersionRule.Enforce(value = 8, atMost = 23, j9 = false)
    @AgentAttachmentRule.Enforce
    @IntegrationRule.Enforce
    public void testSerializableLambda() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassLoader classLoader = lambdaSamples();
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(poolStrategy)
                .ignore(none())
                .with(AgentBuilder.LambdaInstrumentationStrategy.ENABLED)
                .installOn(ByteBuddyAgent.getInstrumentation());
        try {
            Class<?> sampleFactory = classLoader.loadClass(LAMBDA_SAMPLE_FACTORY);
            @SuppressWarnings("unchecked")
            Callable<String> instance = (Callable<String>) sampleFactory.getDeclaredMethod("serializable", String.class).invoke(sampleFactory.getDeclaredConstructor().newInstance(), FOO);
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
            assertThat(ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer), is(true));
            AgentBuilder.LambdaInstrumentationStrategy.release(classFileTransformer, ByteBuddyAgent.getInstrumentation());
        }
    }

    @Test
    @JavaVersionRule.Enforce(value = 8, atMost = 23, j9 = false)
    @AgentAttachmentRule.Enforce
    @IntegrationRule.Enforce
    public void testReturnTypeTransformingLambda() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassLoader classLoader = lambdaSamples();
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(poolStrategy)
                .ignore(none())
                .with(AgentBuilder.LambdaInstrumentationStrategy.ENABLED)
                .type(isSubTypeOf(Callable.class)).transform(new SingleMethodReplacer("call"))
                .installOn(ByteBuddyAgent.getInstrumentation());
        try {
            Class<?> sampleFactory = classLoader.loadClass(LAMBDA_SAMPLE_FACTORY);
            Runnable instance = (Runnable) sampleFactory.getDeclaredMethod("returnTypeTransforming").invoke(sampleFactory.getDeclaredConstructor().newInstance());
            instance.run();
        } finally {
            assertThat(ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer), is(true));
            AgentBuilder.LambdaInstrumentationStrategy.release(classFileTransformer, ByteBuddyAgent.getInstrumentation());
        }
    }

    @Test
    @JavaVersionRule.Enforce(value = 8, atMost = 23, j9 = false)
    @AgentAttachmentRule.Enforce
    @IntegrationRule.Enforce
    public void testInstanceReturningLambda() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassLoader classLoader = lambdaSamples();
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(poolStrategy)
                .ignore(none())
                .with(AgentBuilder.LambdaInstrumentationStrategy.ENABLED)
                .type(isSubTypeOf(Callable.class)).transform(new SingleMethodReplacer("call"))
                .installOn(ByteBuddyAgent.getInstrumentation());
        try {
            Class<?> sampleFactory = classLoader.loadClass(LAMBDA_SAMPLE_FACTORY);
            Callable<?> instance = (Callable<?>) sampleFactory.getDeclaredMethod("instanceReturning").invoke(sampleFactory.getDeclaredConstructor().newInstance());
            assertThat(instance.call(), notNullValue(Object.class));
        } finally {
            assertThat(ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer), is(true));
            AgentBuilder.LambdaInstrumentationStrategy.release(classFileTransformer, ByteBuddyAgent.getInstrumentation());
        }
    }

    @Test
    @IntegrationRule.Enforce
    public void testAdviceTransformer() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(poolStrategy)
                .ignore(none())
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                .type(ElementMatchers.is(Foo.class), ElementMatchers.is(classLoader)).transform(new AgentBuilder.Transformer.ForAdvice()
                        .with(poolStrategy)
                        .with(AgentBuilder.LocationStrategy.ForClassLoader.STRONG)
                        .include(BarAdvice.class.getClassLoader())
                        .with(Assigner.DEFAULT)
                        .withExceptionHandler(new Advice.ExceptionHandler.Simple(Removal.SINGLE))
                        .advice(named(FOO), BarAdvice.class.getName()))
                .installOnByteBuddyAgent();
        try {
            Class<?> type = classLoader.loadClass(Foo.class.getName());
            assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) (FOO + BAR)));
        } finally {
            assertThat(ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer), is(true));
        }
    }

    @Test
    @IntegrationRule.Enforce
    public void testAdviceTransformerWithInjection() throws Exception {
        Instrumentation instrumentation = ByteBuddyAgent.install();
        assertThat(instrumentation, instanceOf(Instrumentation.class));
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(poolStrategy)
                .with(new AgentBuilder.InjectionStrategy.UsingUnsafe.OfFactory(ClassInjector.UsingUnsafe.Factory.resolve(instrumentation)))
                .ignore(none())
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                .type(ElementMatchers.is(Foo.class), ElementMatchers.is(classLoader)).transform(new AgentBuilder.Transformer.ForAdvice()
                        .with(poolStrategy)
                        .with(AgentBuilder.LocationStrategy.ForClassLoader.STRONG)
                        .include(BazAdvice.class.getClassLoader())
                        .with(Assigner.DEFAULT)
                        .withExceptionHandler(new Advice.ExceptionHandler.Simple(Removal.SINGLE))
                        .advice(named(FOO), BazAdvice.class.getName())
                        .auxiliary(BazAdviceAuxiliary.class.getName()))
                .installOnByteBuddyAgent();
        try {
            Class<?> type = classLoader.loadClass(Foo.class.getName());
            assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) (FOO + BAR)));
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
            return builder.method(named(FOO)).intercept(FixedValue.value(BAR));
        }
    }

    public static class Foo {

        public String foo() {
            return FOO;
        }
    }

    public static class Baz {

        public String foo() {
            return FOO;
        }
    }

    public static class BarTransformer implements AgentBuilder.Transformer {

        public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
                                                TypeDescription typeDescription,
                                                ClassLoader classLoader,
                                                JavaModule module,
                                                ProtectionDomain protectionDomain) {
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

    public static class Bar {

        public String foo() {
            return FOO;
        }
    }

    public static class QuxTransformer implements AgentBuilder.Transformer {

        public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
                                                TypeDescription typeDescription,
                                                ClassLoader classLoader,
                                                JavaModule module,
                                                ProtectionDomain protectionDomain) {
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

    public static class Qux {

        public String foo() {
            return FOO;
        }
    }

    public static class QuxBazTransformer implements AgentBuilder.Transformer {

        public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
                                                TypeDescription typeDescription,
                                                ClassLoader classLoader,
                                                JavaModule module,
                                                ProtectionDomain protectionDomain) {
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

    public static class QuxBaz {

        public String foo() {
            return FOO;
        }
    }

    public static class ConstructorTransformer implements AgentBuilder.Transformer {

        public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
                                                TypeDescription typeDescription,
                                                ClassLoader classLoader,
                                                JavaModule module,
                                                ProtectionDomain protectionDomain) {
            return builder.constructor(ElementMatchers.any()).intercept(SuperMethodCall.INSTANCE);
        }
    }

    private static class SingleMethodReplacer implements AgentBuilder.Transformer {

        private final String methodName;

        public SingleMethodReplacer(String methodName) {
            this.methodName = methodName;
        }

        public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
                                                TypeDescription typeDescription,
                                                ClassLoader classLoader,
                                                JavaModule module,
                                                ProtectionDomain protectionDomain) {
            return builder.method(named(methodName)).intercept(FixedValue.value(BAR));
        }
    }

    public static class BarAdviceTransformer implements AgentBuilder.Transformer {

        public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
                                                TypeDescription typeDescription,
                                                ClassLoader classLoader,
                                                JavaModule module,
                                                ProtectionDomain protectionDomain) {
            return builder.visit(Advice.to(BarAdvice.class).on(named(FOO)));
        }
    }

    public static class QuxAdviceTransformer implements AgentBuilder.Transformer {

        public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder,
                                                TypeDescription typeDescription,
                                                ClassLoader classLoader,
                                                JavaModule module,
                                                ProtectionDomain protectionDomain) {
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

    private static class BazAdvice {

        @Advice.OnMethodExit
        private static void exit(@Advice.Return(readOnly = false) String value) {
            value += BazAdviceAuxiliary.value();
        }
    }

    public static class BazAdviceAuxiliary {

        public static String value() {
            return BAR;
        }
    }
}
