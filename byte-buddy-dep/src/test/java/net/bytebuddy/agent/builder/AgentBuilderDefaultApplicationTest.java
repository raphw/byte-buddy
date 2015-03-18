package net.bytebuddy.agent.builder;

import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.instrumentation.FixedValue;
import net.bytebuddy.instrumentation.MethodDelegation;
import net.bytebuddy.instrumentation.method.bytecode.bind.annotation.SuperCall;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.test.utility.ToolsJarRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class AgentBuilderDefaultApplicationTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux";

    @Rule
    public MethodRule toolsJarRule = new ToolsJarRule();

    @Before
    public void setUp() throws Exception {
        assertThat(ByteBuddyAgent.installOnOpenJDK(), instanceOf(Instrumentation.class));
    }

    @Test
    @ToolsJarRule.Enforce
    public void testAgentWithoutSelfInitialization() throws Exception {
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .disableSelfInitialization()
                .rebase(isAnnotatedWith(ShouldRebase.class)).transform(new FooTransformer())
                .installOnByteBuddyAgent();
        try {
            assertThat(new Foo().foo(), is(BAR));
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
        }
    }

    @Test
    @ToolsJarRule.Enforce
    public void testAgentWithoutSelfInitializationWithNativeMethodPrefix() throws Exception {
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .disableSelfInitialization()
                .withNativeMethodPrefix(QUX)
                .rebase(isAnnotatedWith(ShouldRebase.class)).transform(new FooTransformer())
                .installOnByteBuddyAgent();
        try {
            assertThat(new Baz().foo(), is(BAR));
            assertThat(Baz.class.getDeclaredMethod(QUX + FOO), notNullValue(Method.class));
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
        }
    }

    @Test
    @ToolsJarRule.Enforce
    public void testAgentSelfInitialization() throws Exception {
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .rebase(isAnnotatedWith(ShouldRebase.class)).transform(new BarTransformer())
                .installOnByteBuddyAgent();
        try {
            assertThat(new Bar().foo(), is(BAR));
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
        }
    }

    @Test
    @ToolsJarRule.Enforce
    public void testAgentSelfInitializationAuxiliaryTypes() throws Exception {
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .rebase(isAnnotatedWith(ShouldRebase.class)).transform(new QuxTransformer())
                .installOnByteBuddyAgent();
        try {
            assertThat(new Qux().foo(), is(FOO + BAR));
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    private static @interface ShouldRebase {

    }

    private static class FooTransformer implements AgentBuilder.Transformer {

        @Override
        public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription) {
            return builder.method(named(FOO)).intercept(FixedValue.value(BAR));
        }
    }

    @ShouldRebase
    private static class Foo {

        public String foo() {
            return FOO;
        }
    }

    @ShouldRebase
    private static class Baz {

        public String foo() {
            return FOO;
        }
    }

    public static class BarTransformer implements AgentBuilder.Transformer {

        @Override
        public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription) {
            return builder.method(named(FOO)).intercept(MethodDelegation.to(new BarTransformer.Interceptor()));
        }

        public static class Interceptor {

            public String intercept() {
                return BAR;
            }
        }
    }

    @ShouldRebase
    private static class Bar {

        public String foo() {
            return FOO;
        }
    }

    public static class QuxTransformer implements AgentBuilder.Transformer {

        @Override
        public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription) {
            return builder.method(named(FOO)).intercept(MethodDelegation.to(new QuxTransformer.Interceptor()));
        }

        public static class Interceptor {

            public String intercept(@SuperCall Callable<String> zuper) throws Exception {
                return zuper.call() + BAR;
            }
        }
    }

    @ShouldRebase
    private static class Qux {

        public String foo() {
            return FOO;
        }
    }
}
