package net.bytebuddy.dynamic.loading;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.instrumentation.FixedValue;
import net.bytebuddy.test.utility.ToolsJarRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.lang.instrument.Instrumentation;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.spy;

public class ClassReloadingStrategyTest {

    private static final String FOO = "foo", BAR = "bar";

    @Rule
    public MethodRule toolsJarRule = new ToolsJarRule();

    @Before
    public void setUp() throws Exception {
        assertThat(ByteBuddyAgent.installOnOpenJDK(), instanceOf(Instrumentation.class));
    }

    @Test
    @ToolsJarRule.Enforce
    public void testStrategyCreation() throws Exception {
        assertThat(ClassReloadingStrategy.fromInstalledAgent(), notNullValue());
    }

    @Test
    @ToolsJarRule.Enforce
    public void testFromAgentClassReloadingStrategy() throws Exception {
        Foo foo = new Foo();
        assertThat(foo.foo(), is(FOO));
        ClassReloadingStrategy classReloadingStrategy = ClassReloadingStrategy.fromInstalledAgent();
        new ByteBuddy()
                .redefine(Foo.class)
                .method(named(FOO))
                .intercept(FixedValue.value(BAR))
                .make()
                .load(Foo.class.getClassLoader(), classReloadingStrategy);
        assertThat(foo.foo(), is(BAR));
        classReloadingStrategy.reset(Foo.class);
        assertThat(foo.foo(), is(FOO));
    }

    @Test
    @ToolsJarRule.Enforce
    public void testRedefinitionReloadingStrategy() throws Exception {
        Instrumentation instrumentation = spy(ByteBuddyAgent.getInstrumentation());
        Foo foo = new Foo();
        assertThat(foo.foo(), is(FOO));
        ClassReloadingStrategy classReloadingStrategy = new ClassReloadingStrategy(instrumentation,
                ClassReloadingStrategy.Engine.REDEFINITION);
        new ByteBuddy()
                .redefine(Foo.class)
                .method(named(FOO))
                .intercept(FixedValue.value(BAR))
                .make()
                .load(Foo.class.getClassLoader(), classReloadingStrategy);
        assertThat(foo.foo(), is(BAR));
        classReloadingStrategy.reset(Foo.class);
        assertThat(foo.foo(), is(FOO));
    }

    @Test
    @ToolsJarRule.Enforce
    public void testRetransformationReloadingStrategy() throws Exception {
        Foo foo = new Foo();
        assertThat(foo.foo(), is(FOO));
        ClassReloadingStrategy classReloadingStrategy = new ClassReloadingStrategy(ByteBuddyAgent.getInstrumentation(),
                ClassReloadingStrategy.Engine.RETRANSFORMATION);
        new ByteBuddy()
                .redefine(Foo.class)
                .method(named(FOO))
                .intercept(FixedValue.value(BAR))
                .make()
                .load(Foo.class.getClassLoader(), classReloadingStrategy);
        assertThat(foo.foo(), is(BAR));
        classReloadingStrategy.reset(Foo.class);
        assertThat(foo.foo(), is(FOO));
    }

    @SuppressWarnings("unused")
    public static class Foo {

        /**
         * Padding to increase class file size.
         */
        private long a1, a2, a3, a4, a5, a6, a7, a8, a9, a10,
                b2, b3, b4, b5, b6, b7, b8, b9, b10,
                c1, c2, c3, c4, c5, c6, c7, c8, c9, c10,
                d1, d2, d3, d4, d5, d6, d7, d8, d9, d10;

        public String foo() {
            return FOO;
        }
    }
}
