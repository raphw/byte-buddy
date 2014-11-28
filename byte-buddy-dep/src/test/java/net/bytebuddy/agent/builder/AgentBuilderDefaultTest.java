package net.bytebuddy.agent.builder;

import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.instrumentation.FixedValue;
import net.bytebuddy.instrumentation.MethodDelegation;
import net.bytebuddy.utility.ToolsJarRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;

import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AgentBuilderDefaultTest {

    private static final String FOO = "foo", BAR = "bar";

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
                .registerWithByteBuddyAgent();
        try {
            assertThat(new Foo().foo(), is(BAR));
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
        }
    }

    @Test
    @ToolsJarRule.Enforce
    public void testAgentSelfInitialization() throws Exception {
        ClassFileTransformer classFileTransformer = new AgentBuilder.Default()
                .rebase(isAnnotatedWith(ShouldRebase.class)).transform(new BarTransformer())
                .registerWithByteBuddyAgent();
        try {
            assertThat(new Bar().foo(), is(BAR));
        } finally {
            ByteBuddyAgent.getInstrumentation().removeTransformer(classFileTransformer);
        }
    }

    private static class FooTransformer implements AgentBuilder.Transformer {

        @Override
        public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder) {
            return builder.method(named(FOO)).intercept(FixedValue.value(BAR));
        }
    }

    @ShouldRebase
    private static class Foo {

        public String foo() {
            return FOO;
        }
    }

    public static class BarTransformer implements AgentBuilder.Transformer {

        @Override
        public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder) {
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

    @Retention(RetentionPolicy.RUNTIME)
    private static @interface ShouldRebase {
    }
}
