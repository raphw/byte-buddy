package net.bytebuddy;

import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.instrumentation.FixedValue;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.test.utility.PrecompiledTypeClassLoader;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.lang.reflect.Method;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ByteBuddyDefaultMethodDiscoveryTest {

    private static final String FOO = "foo", BAR = "bar";

    private static final String DEFAULT_METHOD_INTERFACE = "net.bytebuddy.test.precompiled.SingleDefaultMethodInterface";

    private static final Object STATIC_FIELD = null;

    private static final String INTERFACE_STATIC_FIELD_NAME = "FOO";

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    private ClassLoader classLoader;

    @Before
    public void setUp() throws Exception {
        classLoader = new PrecompiledTypeClassLoader(getClass().getClassLoader());
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testDefaultMethodNonOverridden() throws Exception {
        Class<?> interfaceType = classLoader.loadClass(DEFAULT_METHOD_INTERFACE);
        Object interfaceMarker = interfaceType.getDeclaredField(INTERFACE_STATIC_FIELD_NAME).get(STATIC_FIELD);
        Method interfaceMethod = interfaceType.getDeclaredMethod(FOO);
        Class<?> dynamicType = new ByteBuddy()
                .subclass(interfaceType)
                .make()
                .load(classLoader, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(dynamicType.getDeclaredFields().length, is(0));
        assertThat(dynamicType.getDeclaredMethods().length, is(0));
        assertThat(interfaceMethod.invoke(dynamicType.newInstance()), is(interfaceMarker));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testDefaultMethodOverridden() throws Exception {
        Class<?> interfaceType = classLoader.loadClass(DEFAULT_METHOD_INTERFACE);
        Method interfaceMethod = interfaceType.getDeclaredMethod(FOO);
        Class<?> dynamicType = new ByteBuddy()
                .subclass(interfaceType)
                .method(isDeclaredBy(interfaceType)).intercept(FixedValue.value(BAR))
                .make()
                .load(classLoader, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(dynamicType.getDeclaredFields().length, is(0));
        assertThat(dynamicType.getDeclaredMethods().length, is(1));
        assertThat(interfaceMethod.invoke(dynamicType.newInstance()), is((Object) BAR));
    }
}
