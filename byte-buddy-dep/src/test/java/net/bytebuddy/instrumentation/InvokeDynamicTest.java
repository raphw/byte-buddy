package net.bytebuddy.instrumentation;

import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.test.precompiled.StandardArgumentBootstrap;
import net.bytebuddy.test.utility.JavaVersionRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.rules.TestRule;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class InvokeDynamicTest extends AbstractInstrumentationTest {

    private static final String FOO = "foo";

    @Rule
    public MethodRule java7Rule = new JavaVersionRule(7);

    @Test
    @JavaVersionRule.Enforce
    public void testBootstrapMethod() throws Exception {
        for (Method method : StandardArgumentBootstrap.class.getDeclaredMethods()) {
            if(method.getName().equals(FOO)) {
                continue;
            }
            DynamicType.Loaded<Simple> dynamicType = instrument(Simple.class, InvokeDynamic.bootstrap(method));
            assertThat(dynamicType.getLoaded().newInstance().foo(), is(FOO));
        }
    }

    @Test
    @JavaVersionRule.Enforce
    public void testBootstrapConstructor() throws Exception {
        for (Constructor<?> constructor : StandardArgumentBootstrap.class.getDeclaredConstructors()) {
            DynamicType.Loaded<Simple> dynamicType = instrument(Simple.class, InvokeDynamic.bootstrap(constructor));
            assertThat(dynamicType.getLoaded().newInstance().foo(), is(FOO));
        }
    }

    public static class Simple {

        public String foo() {
            return null;
        }
    }

    // TODO: Test illegal bootstrap methods and bootstrap with arguments - Implement argument loader similar to MethodCall instrumentation?
}