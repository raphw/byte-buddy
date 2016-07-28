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

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static net.bytebuddy.dynamic.loading.ClassInjector.DEFAULT_PROTECTION_DOMAIN;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.none;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@RunWith(Parameterized.class)
public class AgentBuilderDefaultApplicationRedefineTest {

    private static final String FOO = "foo", BAR = "bar";

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        List<Object[]> list = new ArrayList<Object[]>();
        for (AgentBuilder.DescriptionStrategy descriptionStrategy : AgentBuilder.DescriptionStrategy.Default.values()) {
            list.add(new Object[]{descriptionStrategy});
        }
        return list;
    }

    private final AgentBuilder.DescriptionStrategy descriptionStrategy;

    public AgentBuilderDefaultApplicationRedefineTest(AgentBuilder.DescriptionStrategy descriptionStrategy) {
        this.descriptionStrategy = descriptionStrategy;
    }

    @Rule
    public MethodRule agentAttachmentRule = new AgentAttachmentRule();

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Rule
    public MethodRule integrationRule = new IntegrationRule();

    private ClassLoader simpleTypeLoader, optionalTypeLoader;

    @Before
    public void setUp() throws Exception {
        simpleTypeLoader = new ByteArrayClassLoader(ClassLoadingStrategy.BOOTSTRAP_LOADER,
                ClassFileExtraction.of(SimpleType.class),
                DEFAULT_PROTECTION_DOMAIN,
                ByteArrayClassLoader.PersistenceHandler.MANIFEST,
                PackageDefinitionStrategy.NoOp.INSTANCE);
        optionalTypeLoader = new ByteArrayClassLoader(ClassLoadingStrategy.BOOTSTRAP_LOADER,
                ClassFileExtraction.of(SimpleOptionalType.class),
                DEFAULT_PROTECTION_DOMAIN,
                ByteArrayClassLoader.PersistenceHandler.MANIFEST,
                PackageDefinitionStrategy.NoOp.INSTANCE);
    }

    @Test
    @AgentAttachmentRule.Enforce(redefinesClasses = true)
    @IntegrationRule.Enforce
    public void testRedefinition() throws Exception {
        // A redefinition reflects on loaded types which are eagerly validated types (Java 7- for redefinition).
        // This causes type equality for outer/inner classes to fail which is why an external class is used.
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        assertThat(simpleTypeLoader.loadClass(SimpleType.class.getName()).getName(), is(SimpleType.class.getName())); // ensure that class is loaded
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .ignore(none())
                .disableClassFormatChanges()
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .with(descriptionStrategy)
                .type(ElementMatchers.is(SimpleType.class), ElementMatchers.is(simpleTypeLoader)).transform(new FooTransformer())
                .installOnByteBuddyAgent();
        try {
            Class<?> type = simpleTypeLoader.loadClass(SimpleType.class.getName());
            assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) BAR));
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
        }
    }

    @Test
    @AgentAttachmentRule.Enforce(redefinesClasses = true)
    @IntegrationRule.Enforce
    public void testRedefinitionOptionalType() throws Exception {
        // A redefinition reflects on loaded types which are eagerly validated types (Java 7- for redefinition).
        // This causes type equality for outer/inner classes to fail which is why an external class is used.
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        assertThat(optionalTypeLoader.loadClass(SimpleOptionalType.class.getName()).getName(), is(SimpleOptionalType.class.getName())); // ensure that class is loaded
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(new ByteBuddy().with(TypeValidation.DISABLED))
                .ignore(none())
                .disableClassFormatChanges()
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .with(descriptionStrategy)
                .type(ElementMatchers.is(SimpleOptionalType.class), ElementMatchers.is(optionalTypeLoader)).transform(new FooTransformer())
                .installOnByteBuddyAgent();
        try {
            Class<?> type = optionalTypeLoader.loadClass(SimpleOptionalType.class.getName());
            // The hybrid strategy cannot transform optional types.
            assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()),
                    is((Object) (descriptionStrategy == AgentBuilder.DescriptionStrategy.Default.HYBRID ? FOO : BAR)));
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
        assertThat(simpleTypeLoader.loadClass(SimpleType.class.getName()).getName(), is(SimpleType.class.getName())); // ensure that class is loaded
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .ignore(none())
                .disableClassFormatChanges()
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .with(descriptionStrategy)
                .type(ElementMatchers.is(SimpleType.class), ElementMatchers.is(simpleTypeLoader)).transform(new FooTransformer())
                .installOnByteBuddyAgent();
        try {
            Class<?> type = simpleTypeLoader.loadClass(SimpleType.class.getName());
            assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) BAR));
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
        }
    }

    @Test
    @AgentAttachmentRule.Enforce(retransformsClasses = true)
    @IntegrationRule.Enforce
    public void testRetransformationOptionalType() throws Exception {
        // A redefinition reflects on loaded types which are eagerly validated types (Java 7- for redefinition).
        // This causes type equality for outer/inner classes to fail which is why an external class is used.
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        assertThat(optionalTypeLoader.loadClass(SimpleOptionalType.class.getName()).getName(), is(SimpleOptionalType.class.getName())); // ensure that class is loaded
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default(new ByteBuddy().with(TypeValidation.DISABLED))
                .ignore(none())
                .disableClassFormatChanges()
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION)
                .with(descriptionStrategy)
                .type(ElementMatchers.is(SimpleOptionalType.class), ElementMatchers.is(optionalTypeLoader)).transform(new FooTransformer())
                .installOnByteBuddyAgent();
        try {
            Class<?> type = optionalTypeLoader.loadClass(SimpleOptionalType.class.getName());
            // The hybrid strategy cannot transform optional types.
            assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) (descriptionStrategy == AgentBuilder.DescriptionStrategy.Default.HYBRID
                    ? FOO
                    : BAR)));
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
        }
    }

    private static class FooTransformer implements AgentBuilder.Transformer {

        @Override
        public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader) {
            return builder.method(named(FOO)).intercept(FixedValue.value(BAR));
        }
    }
}
