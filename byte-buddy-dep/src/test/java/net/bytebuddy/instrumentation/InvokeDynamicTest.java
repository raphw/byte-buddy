package net.bytebuddy.instrumentation;

import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.test.utility.PrecompiledTypeClassLoader;
import org.junit.Before;
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

    private static final String STANDARD_ARGUMENT_BOOTSTRAP = "net.bytebuddy.test.precompiled.StandardArgumentBootstrap";

    @Rule
    public MethodRule java7Rule = new JavaVersionRule(7);

    private ClassLoader classLoader;

    @Before
    public void setUp() throws Exception {
        classLoader = new PrecompiledTypeClassLoader(getClass().getClassLoader());
    }

    @Test
    @JavaVersionRule.Enforce
    public void testBootstrapMethod() throws Exception {
        for (Method method : classLoader.loadClass(STANDARD_ARGUMENT_BOOTSTRAP).getDeclaredMethods()) {
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
        for (Constructor<?> constructor : classLoader.loadClass(STANDARD_ARGUMENT_BOOTSTRAP).getDeclaredConstructors()) {
            DynamicType.Loaded<Simple> dynamicType = instrument(Simple.class, InvokeDynamic.bootstrap(constructor));
            assertThat(dynamicType.getLoaded().newInstance().foo(), is(FOO));
        }
    }

    public static class Simple {

        public String foo() {
            return null;
        }
    }
}
