package net.bytebuddy.dynamic.loading;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Ownership;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.test.utility.ClassFileExtraction;
import net.bytebuddy.test.utility.JavaVersionRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.rules.TestRule;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;

import static net.bytebuddy.test.utility.FieldByFieldComparison.hasPrototype;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class ClassInjectorUsingLookupTest {

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    private Class<?> type;

    private ClassInjector.UsingLookup injector;

    @Before
    public void setUp() throws Exception {
        Method lookup = Class.forName("java.lang.invoke.MethodHandles").getMethod("lookup");
        type = new ByteBuddy()
                .subclass(Object.class)
                .name("net.bytebuddy.test.Foo")
                .defineMethod("lookup", Object.class, Ownership.STATIC, Visibility.PUBLIC)
                .intercept(MethodCall.invoke(lookup))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        injector = ClassInjector.UsingLookup.of(type.getDeclaredMethod("lookup").invoke(null));
    }

    @Test
    @JavaVersionRule.Enforce(9)
    public void testIsAvailable() {
        assertThat(ClassInjector.UsingLookup.isAvailable(), is(true));
    }

    @Test
    @JavaVersionRule.Enforce(9)
    public void testLookupType() throws Exception {
        assertThat(injector.lookupType(), is((Object) type));
    }

    @Test
    @JavaVersionRule.Enforce(9)
    public void testLookupInjection() throws Exception {
        DynamicType dynamicType = new ByteBuddy()
                .subclass(Object.class)
                .name("net.bytebuddy.test.Bar")
                .make();
        assertThat(injector.inject(Collections.singletonMap(dynamicType.getTypeDescription(), dynamicType.getBytes()))
                .get(dynamicType.getTypeDescription()).getName(), is("net.bytebuddy.test.Bar"));
    }
}
