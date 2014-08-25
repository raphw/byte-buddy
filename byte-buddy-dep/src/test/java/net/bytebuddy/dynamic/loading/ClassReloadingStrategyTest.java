package net.bytebuddy.dynamic.loading;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.instrumentation.MethodDelegation;
import net.bytebuddy.utility.ToolsJarRule;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.lang.instrument.Instrumentation;

import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.named;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class ClassReloadingStrategyTest {

    private static final String FOO = "foo", BAR = "bar";

    private static final String INSTRUMENTATION = "instrumentation";

    private static final Object STATIC_FIELD = null;

    @Rule
    public MethodRule toolsJarRule = new ToolsJarRule();

    @BeforeClass
    @ToolsJarRule.Enforce
    public static void setUpClass() throws Exception {
        assertThat(ByteBuddyAgent.installOnOpenJDK(), instanceOf(Instrumentation.class));
    }

    @Test
    @ToolsJarRule.Enforce
    public void testStrategyCreation() throws Exception {
        assertThat(ClassReloadingStrategy.fromInstalledAgent(), notNullValue());
    }

    @Test
    @ToolsJarRule.Enforce
    public void testRedefinition() throws Exception {
        Foo foo = new Foo();
        assertThat(foo.foo(), is(FOO));
        ClassReloadingStrategy classReloadingStrategy = ClassReloadingStrategy.fromInstalledAgent();
        new ByteBuddy()
                .redefine(Foo.class)
                .method(named(FOO))
                .intercept(MethodDelegation.to(Interceptor.class))
                .make()
                .load(Foo.class.getClassLoader(), classReloadingStrategy);
        assertThat(foo.foo(), is(BAR));
        /*classReloadingStrategy.reset(Foo.class);
        assertThat(foo.foo(), is(FOO));*/
    }

    public static class Foo {

        public String foo() {
            return FOO;
        }
    }

    public static class Interceptor {

        public static String intercept() throws Exception {
            return BAR;
        }
    }
}
