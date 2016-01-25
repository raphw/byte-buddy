package net.bytebuddy.agent.builder;

import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.TargetType;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import net.bytebuddy.dynamic.loading.PackageDefinitionStrategy;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.SuperMethodCall;
import net.bytebuddy.implementation.bind.annotation.Super;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.test.packaging.SimpleType;
import net.bytebuddy.test.utility.AgentAttachmentRule;
import net.bytebuddy.test.utility.ClassFileExtraction;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.*;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.function.Function;

import static net.bytebuddy.matcher.ElementMatchers.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class AgentBuilderDefaultApplicationTest {

    private static final ProtectionDomain DEFAULT_PROTECTION_DOMAIN = null;

    private static final String FOO = "foo", BAR = "bar", QUX = "qux";

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {AgentBuilder.BinaryLocator.Default.EXTENDED},
                {AgentBuilder.BinaryLocator.Default.FAST},
                {AgentBuilder.BinaryLocator.ClassLoading.INSTANCE}
        });
    }

    @Rule
    public MethodRule agentAttachmentRule = new AgentAttachmentRule();

    private ClassLoader classLoader;

    private final AgentBuilder.BinaryLocator binaryLocator;

    public AgentBuilderDefaultApplicationTest(AgentBuilder.BinaryLocator binaryLocator) {
        this.binaryLocator = binaryLocator;
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
                AccessController.getContext(),
                ByteArrayClassLoader.PersistenceHandler.MANIFEST,
                PackageDefinitionStrategy.NoOp.INSTANCE);
    }

    @Test
    @AgentAttachmentRule.Enforce
    public void testAgentWithoutSelfInitialization() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(binaryLocator)
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
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
                .with(binaryLocator)
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
    public void testAgentSelfInitializationAuxiliaryTypeEager() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(binaryLocator)
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
    public void testAgentSelfInitializationAuxiliaryTypeLazy() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(binaryLocator)
                .type(isAnnotatedWith(ShouldRebase.class), ElementMatchers.is(classLoader)).transform(new QuxBazTransformer())
                .installOnByteBuddyAgent();
        try {
            Class<?> type = classLoader.loadClass(QuxBaz.class.getName());
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
                .with(binaryLocator)
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
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
    @AgentAttachmentRule.Enforce(redefinesClasses = true)
    public void testRedefinition() throws Exception {
        // As documented, the class loading binary locator is not applicable for redefinitions.
        if (binaryLocator.equals(AgentBuilder.BinaryLocator.ClassLoading.INSTANCE)) {
            return;
        }
        // A redefinition reflects on loaded types which are eagerly validated types (Java 7- for redefinition).
        // This causes type equality for outer/inner classes to fail which is whz an external class is used.
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        assertThat(classLoader.loadClass(SimpleType.class.getName()).getName(), is(SimpleType.class.getName())); // ensure that class is loaded
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(binaryLocator)
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .type(isAnnotatedWith(ShouldRebase.class), ElementMatchers.is(classLoader)).transform(new FooTransformer())
                .installOnByteBuddyAgent();
        try {
            Class<?> type = classLoader.loadClass(SimpleType.class.getName());
            assertThat(type.getDeclaredMethod(FOO).invoke(type.newInstance()), is((Object) BAR));
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
        }
    }

    @Test
    @AgentAttachmentRule.Enforce(retransformsClasses = true)
    public void testRetransformation() throws Exception {
        // A redefinition reflects on loaded types which are eagerly validated types (Java 7- for redefinition).
        // This causes type equality for outer/inner classes to fail which is whz an external class is used.
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        assertThat(classLoader.loadClass(SimpleType.class.getName()).getName(), is(SimpleType.class.getName())); // ensure that class is loaded
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(binaryLocator)
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .type(isAnnotatedWith(ShouldRebase.class), ElementMatchers.is(classLoader)).transform(new FooTransformer())
                .installOnByteBuddyAgent();
        try {
            Class<?> type = classLoader.loadClass(SimpleType.class.getName());
            assertThat(type.getDeclaredMethod(FOO).invoke(type.newInstance()), is((Object) BAR));
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
        }
    }

    @Test
    @AgentAttachmentRule.Enforce
    public void testChainedAgent() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        AgentBuilder agentBuilder = new AgentBuilder.Default()
                .with(binaryLocator)
                .type(isAnnotatedWith(ShouldRebase.class), ElementMatchers.is(classLoader)).transform(new QuxTransformer());
        ClassFileTransformer firstTransformer = agentBuilder.installOnByteBuddyAgent();
        ClassFileTransformer secondTransformer = agentBuilder.installOnByteBuddyAgent();
        try {
            Class<?> type = classLoader.loadClass(Qux.class.getName());
            assertThat(type.getDeclaredMethod(FOO).invoke(type.newInstance()), is((Object) (FOO + BAR + BAR)));
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(firstTransformer);
            ByteBuddyAgent.getInstrumentation().removeTransformer(secondTransformer);
        }
    }

    @Test
    @AgentAttachmentRule.Enforce
    public void testSignatureTypesAreAvailableAfterLoad() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(binaryLocator)
                .type(isAnnotatedWith(ShouldRebase.class), ElementMatchers.is(classLoader)).transform(new ConstructorTransformer())
                .installOnByteBuddyAgent();
        try {
            Class<?> type = classLoader.loadClass(Foo.class.getName());
            assertThat(type.getDeclaredConstructors().length, is(2));
            assertThat(type.newInstance(), notNullValue(Object.class));
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
        }
    }

    @Test
    public void testNonCapturingLambda() throws Exception {
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(binaryLocator)
                .enableLambdaInstrumentation(true)
                .type(isSubTypeOf(Callable.class)).transform((builder, typeDescription) -> builder.method(named("call")).intercept(FixedValue.value(BAR)))
                .installOn(ByteBuddyAgent.install());
        try {
            Callable<String> r = () -> FOO;
            assertThat(r.call(), is(BAR));
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
            AgentBuilder.Default.releaseLambdaTransformer(classFileTransformer, ByteBuddyAgent.getInstrumentation());
        }
    }

    @Test
    public void testArgumentCapturingLambda() throws Exception {
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(binaryLocator)
                .enableLambdaInstrumentation(true)
                .type(isSubTypeOf(Callable.class)).transform((builder, typeDescription) -> builder.method(named("call")).intercept(FixedValue.value(BAR)))
                .installOn(ByteBuddyAgent.install());
        try {
            String foo = FOO;
            Callable<String> r = () -> foo;
            assertThat(r.call(), is(BAR));
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
            AgentBuilder.Default.releaseLambdaTransformer(classFileTransformer, ByteBuddyAgent.getInstrumentation());
        }
    }

    private String foo = FOO;

    @Test
    public void testInstanceCapturingLambda() throws Exception {
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(binaryLocator)
                .enableLambdaInstrumentation(true)
                .type(isSubTypeOf(Callable.class)).transform((builder, typeDescription) -> builder.method(named("call")).intercept(FixedValue.value(BAR)))
                .installOn(ByteBuddyAgent.install());
        try {
            Callable<String> r = () -> foo;
            assertThat(r.call(), is(BAR));
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
            AgentBuilder.Default.releaseLambdaTransformer(classFileTransformer, ByteBuddyAgent.getInstrumentation());
        }
    }

    @Test
    public void testNonCapturingLambdaWithArguments() throws Exception {
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(binaryLocator)
                .enableLambdaInstrumentation(true)
                .type(isSubTypeOf(Function.class)).transform((builder, typeDescription) -> builder.method(named("apply")).intercept(FixedValue.value(BAR)))
                .installOn(ByteBuddyAgent.install());
        try {
            Function<String, String> r = (bar) -> bar;
            assertThat(r.apply(FOO), is(BAR));
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
            AgentBuilder.Default.releaseLambdaTransformer(classFileTransformer, ByteBuddyAgent.getInstrumentation());
        }
    }

    @Test
    public void testCapturingLambdaWithArguments() throws Exception {
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(binaryLocator)
                .enableLambdaInstrumentation(true)
                .type(isSubTypeOf(Function.class)).transform((builder, typeDescription) -> builder.method(named("apply")).intercept(FixedValue.value(BAR)))
                .installOn(ByteBuddyAgent.install());
        try {
            String qux = QUX;
            Function<String, String> r = (bar) -> bar + qux;
            assertThat(r.apply(FOO), is(BAR));
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
            AgentBuilder.Default.releaseLambdaTransformer(classFileTransformer, ByteBuddyAgent.getInstrumentation());
        }
    }

    @Test
    public void testSerializableLambda() throws Exception {
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .with(binaryLocator)
                .enableLambdaInstrumentation(true)
                .type(isSubTypeOf(Callable.class)).transform((builder, typeDescription) -> builder.method(named("call")).intercept(FixedValue.value(BAR)))
                .installOn(ByteBuddyAgent.install());
        try {
            String foo = FOO;
            Callable<String> r = (Callable<String> & Serializable) () -> foo;
            assertThat(r.call(), is(BAR));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
            objectOutputStream.writeObject(r);
            objectOutputStream.close();
            ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(outputStream.toByteArray()));
            @SuppressWarnings("unchecked")
            Callable<String> deserialized = (Callable<String>) objectInputStream.readObject();
            assertThat(deserialized.call(), is(BAR));
            objectInputStream.close();
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
            AgentBuilder.Default.releaseLambdaTransformer(classFileTransformer, ByteBuddyAgent.getInstrumentation());
        }
    }

    @Test
    public void testFoo() throws Exception {
        Callable<String> r = (Callable<String> & Serializable) () -> "foo";
        r.call();

    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface ShouldRebase {
        /* empty */
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

    public static class QuxBazTransformer implements AgentBuilder.Transformer {

        @Override
        public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription) {
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
        public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription) {
            return builder.constructor(ElementMatchers.any()).intercept(SuperMethodCall.INSTANCE);
        }
    }
}
