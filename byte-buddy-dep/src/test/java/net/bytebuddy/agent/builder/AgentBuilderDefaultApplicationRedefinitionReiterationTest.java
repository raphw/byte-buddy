package net.bytebuddy.agent.builder;

import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.dynamic.loading.ByteArrayClassLoader;
import net.bytebuddy.implementation.bytecode.Removal;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.test.utility.AgentAttachmentRule;
import net.bytebuddy.test.utility.ClassFileExtraction;
import net.bytebuddy.test.utility.JavaVersionRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.none;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AgentBuilderDefaultApplicationRedefinitionReiterationTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux";

    @Rule
    public MethodRule agentAttachmentRule = new AgentAttachmentRule();

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    private ClassLoader classLoader;

    @Before
    public void setUp() throws Exception {
        classLoader = new ByteArrayClassLoader.ChildFirst(getClass().getClassLoader(),
                ClassFileExtraction.of(Foo.class, Bar.class),
                ByteArrayClassLoader.PersistenceHandler.MANIFEST);
    }

    @Test
    @AgentAttachmentRule.Enforce(retransformsClasses = true)
    public void testAdviceWithoutLoadedClasses() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        ClassFileTransformer classFileTransformer = installInstrumentation();
        try {
            assertAdvice();
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
        }
    }

    @Test
    @AgentAttachmentRule.Enforce(retransformsClasses = true)
    public void testAdviceWithOneLoadedClass() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        classLoader.loadClass(Foo.class.getName());
        ClassFileTransformer classFileTransformer = installInstrumentation();
        try {
            assertAdvice();
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
        }
    }

    @Test
    @AgentAttachmentRule.Enforce(retransformsClasses = true)
    public void testAdviceWithTwoLoadedClasses() throws Exception {
        assertThat(ByteBuddyAgent.install(), instanceOf(Instrumentation.class));
        classLoader.loadClass(Foo.class.getName());
        classLoader.loadClass(Bar.class.getName());
        ClassFileTransformer classFileTransformer = installInstrumentation();
        try {
            assertAdvice();
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
        }
    }

    private void assertAdvice() throws Exception {
        Class<?> type = classLoader.loadClass(Foo.class.getName());
        assertThat(type.getDeclaredMethod("createBar").invoke(type.getDeclaredConstructor().newInstance()).toString(), is((Object) (QUX + FOO + BAR)));
    }

    private ClassFileTransformer installInstrumentation() {
        return new AgentBuilder.Default()
                .disableClassFormatChanges()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(AgentBuilder.RedefinitionStrategy.DiscoveryStrategy.Reiterating.INSTANCE)
                .ignore(none())
                .type(named(Foo.class.getName()), ElementMatchers.is(classLoader))
                .transform(new AgentBuilder.Transformer.ForAdvice()
                        .with(AgentBuilder.LocationStrategy.ForClassLoader.STRONG)
                        .include(FooAdvice.class.getClassLoader())
                        .with(Assigner.DEFAULT)
                        .withExceptionHandler(Removal.SINGLE)
                        .advice(named("createBar"), FooAdvice.class.getName()))
                .asDecorator()
                .type(ElementMatchers.named(Bar.class.getName()), ElementMatchers.is(classLoader))
                .transform(new AgentBuilder.Transformer.ForAdvice()
                        .with(AgentBuilder.LocationStrategy.ForClassLoader.STRONG)
                        .include(BarAdvice.class.getClassLoader())
                        .with(Assigner.DEFAULT)
                        .withExceptionHandler(Removal.SINGLE)
                        .advice(named("toString"), BarAdvice.class.getName()))
                .asDecorator()
                .installOnByteBuddyAgent();
    }

    public static class Foo {

        @SuppressWarnings("unused")
        public Bar createBar() throws Exception {
            return new Bar();
        }

    }

    public static class Bar {

        private String x = QUX;

        public void append(String x) {
            this.x += x;
        }

        @Override
        public String toString() {
            return x;
        }
    }

    private static class FooAdvice {

        @Advice.OnMethodExit
        private static void exit(@Advice.Return Bar value) {
            value.append(FOO);
        }
    }

    private static class BarAdvice {

        @Advice.OnMethodExit
        private static void exit(@Advice.Return(readOnly = false) String value) {
            value += BAR;
        }
    }
}
