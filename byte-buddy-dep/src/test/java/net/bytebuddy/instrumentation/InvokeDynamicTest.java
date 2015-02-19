package net.bytebuddy.instrumentation;

import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import net.bytebuddy.test.utility.PrecompiledTypeClassLoader;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
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
            if (method.getName().equals(FOO)) {
                continue;
            }
            DynamicType.Loaded<Simple> dynamicType = instrument(Simple.class,
                    InvokeDynamic.bootstrap(method).withoutArguments(),
                    classLoader,
                    isDeclaredBy(Simple.class));
            assertThat(dynamicType.getLoaded().newInstance().foo(), is(FOO));
        }
    }

    @Test
    @JavaVersionRule.Enforce
    public void testBootstrapConstructor() throws Exception {
        for (Constructor<?> constructor : classLoader.loadClass(STANDARD_ARGUMENT_BOOTSTRAP).getDeclaredConstructors()) {
            DynamicType.Loaded<Simple> dynamicType = instrument(Simple.class,
                    InvokeDynamic.bootstrap(constructor).withoutArguments(),
                    classLoader,
                    isDeclaredBy(Simple.class));
            assertThat(dynamicType.getLoaded().newInstance().foo(), is(FOO));
        }
    }

    public static class Simple {

        public String foo() {
            return null;
        }
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(InvokeDynamic.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.Appender.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.WithImplicitTarget.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.WithImplicitArguments.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.Default.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.Default.Target.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.Target.Resolved.Simple.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.Target.ForMethodDescription.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.Default.NameProvider.ForExplicitName.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.Default.ReturnTypeProvider.ForExplicitType.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.ArgumentProvider.ForBooleanValue.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.ArgumentProvider.ForByteValue.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.ArgumentProvider.ForShortValue.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.ArgumentProvider.ForCharacterValue.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.ArgumentProvider.ForIntegerValue.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.ArgumentProvider.ForLongValue.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.ArgumentProvider.ForFloatValue.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.ArgumentProvider.ForDoubleValue.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.ArgumentProvider.ForExistingField.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.ArgumentProvider.ForInstanceField.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.ArgumentProvider.ForStaticField.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.ArgumentProvider.Resolved.Simple.class).apply();
        ObjectPropertyAssertion.of(InvokeDynamic.InvocationProvider.ArgumentProvider.ConstantPoolWrapper.WrappingArgumentProvider.class).apply();
    }
}
