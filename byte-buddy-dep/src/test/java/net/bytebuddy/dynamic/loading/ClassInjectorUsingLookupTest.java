package net.bytebuddy.dynamic.loading;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.description.modifier.Ownership;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.test.utility.AgentAttachmentRule;
import net.bytebuddy.test.utility.JavaVersionRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ClassInjectorUsingLookupTest {

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    private Class<?> type;

    @Before
    public void setUp() throws Exception {
        type = new ByteBuddy()
                .subclass(Object.class)
                .name("net.bytebuddy.test.Foo")
                .defineMethod("lookup", Object.class, Ownership.STATIC, Visibility.PUBLIC)
                .intercept(MethodCall.invoke(Class.forName("java.lang.invoke.MethodHandles").getMethod("lookup")))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
    }

    @Test
    @JavaVersionRule.Enforce(9)
    public void testIsAvailable() {
        assertThat(ClassInjector.UsingLookup.isAvailable(), is(true));
    }

    @Test
    @JavaVersionRule.Enforce(9)
    public void testLookupType() throws Exception {
        assertThat(ClassInjector.UsingLookup.of(type.getMethod("lookup").invoke(null)).lookupType(), is((Object) type));
    }

    @Test
    @JavaVersionRule.Enforce(9)
    public void testLookupInjection() throws Exception {
        ClassInjector injector = ClassInjector.UsingLookup.of(type.getMethod("lookup").invoke(null));
        DynamicType dynamicType = new ByteBuddy()
                .subclass(Object.class)
                .name("net.bytebuddy.test.Bar")
                .make();
        assertThat(injector.inject(Collections.singletonMap(dynamicType.getTypeDescription(), dynamicType.getBytes()))
                .get(dynamicType.getTypeDescription()).getName(), is("net.bytebuddy.test.Bar"));
    }

    @Test
    @JavaVersionRule.Enforce(9)
    public void testLookupInjectionPropagate() throws Exception {
        ClassInjector injector = ClassInjector.UsingLookup.of(Class.forName("java.lang.invoke.MethodHandles").getMethod("lookup").invoke(null)).in(type);
        DynamicType dynamicType = new ByteBuddy()
                .subclass(Object.class)
                .name("net.bytebuddy.test.Bar")
                .make();
        assertThat(injector.inject(Collections.singletonMap(dynamicType.getTypeDescription(), dynamicType.getBytes()))
                .get(dynamicType.getTypeDescription()).getName(), is("net.bytebuddy.test.Bar"));
    }

    @Test
    @JavaVersionRule.Enforce(9)
    public void testAvailable() throws Exception {
        assertThat(ClassInjector.UsingLookup.isAvailable(), is(true));
        assertThat(ClassInjector.UsingLookup.of(type.getMethod("lookup").invoke(null)).isAlive(), is((Object) true));
    }
}
