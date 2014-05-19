package net.bytebuddy;

import net.bytebuddy.dynamic.ClassLoadingStrategy;
import net.bytebuddy.instrumentation.FixedValue;
import net.bytebuddy.utility.Java8Rule;
import net.bytebuddy.utility.PrecompiledTypeClassLoader;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.lang.reflect.Method;

import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.isDeclaredBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class Java8DefaultMethodTest {

    private static final String FOO = "foo", BAR = "bar";

    private static final String DEFAULT_METHOD_INTERFACE = "net.bytebuddy.test.precompiled.SingleDefaultMethodInterface";

    private static final Object STATIC_FIELD = null;
    private static final String INTERFACE_STATIC_FIELD_NAME = "FOO";

    @Rule
    public MethodRule java8Rule = new Java8Rule();

    private ClassLoader classLoader;
    private Object interfaceMarker;
    private Class<?> interfaceType;
    private Method interfaceMethod;

    @Before
    public void setUp() throws Exception {
        classLoader = new PrecompiledTypeClassLoader(getClass().getClassLoader());
        interfaceType = classLoader.loadClass(DEFAULT_METHOD_INTERFACE);
        interfaceMarker = interfaceType.getDeclaredField(INTERFACE_STATIC_FIELD_NAME).get(STATIC_FIELD);
        interfaceMethod = interfaceType.getDeclaredMethod(FOO);
    }

    @Test
    @Java8Rule.Enforce
    public void testDefaultMethodNonOverridden() throws Exception {
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
    @Java8Rule.Enforce
    public void testDefaultMethodOverridden() throws Exception {
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
