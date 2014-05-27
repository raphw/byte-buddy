package net.bytebuddy.instrumentation;

import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.utility.Java8Rule;
import net.bytebuddy.utility.PrecompiledTypeClassLoader;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.lang.reflect.Method;

import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.isDeclaredBy;
import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.not;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class DefaultMethodCallTest extends AbstractInstrumentationTest {

    private static final String FOO = "foo", QUX = "qux";

    private static final String SINGLE_DEFAULT_METHOD = "net.bytebuddy.test.precompiled.SingleDefaultMethodInterface";
    private static final String SINGLE_DEFAULT_METHOD_CLASS = "net.bytebuddy.test.precompiled.SingleDefaultMethodClass";
    private static final String CONFLICTING_INTERFACE = "net.bytebuddy.test.precompiled.SingleDefaultMethodConflictingInterface";
    private static final String NON_OVERRIDING_INTERFACE = "net.bytebuddy.test.precompiled.SingleDefaultMethodNonOverridingInterface";

    @Rule
    public MethodRule java8Rule = new Java8Rule();

    private ClassLoader classLoader;

    @Before
    public void setUp() throws Exception {
        classLoader = new PrecompiledTypeClassLoader(getClass().getClassLoader());
    }

    @Test
    @Java8Rule.Enforce
    public void testUnambiguousDefaultMethod() throws Exception {
        DynamicType.Loaded<?> loaded = instrument(Object.class,
                DefaultMethodCall.unambiguousOnly(),
                classLoader,
                not(isDeclaredBy(Object.class)),
                classLoader.loadClass(SINGLE_DEFAULT_METHOD));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        Method method = loaded.getLoaded().getDeclaredMethod(FOO);
        Object instance = loaded.getLoaded().newInstance();
        assertThat(method.invoke(instance), is((Object) FOO));
    }

    @Test(expected = IllegalArgumentException.class)
    @Java8Rule.Enforce
    public void testAmbiguousDefaultMethodThrowsException() throws Exception {
        instrument(Object.class,
                DefaultMethodCall.unambiguousOnly(),
                classLoader,
                not(isDeclaredBy(Object.class)),
                classLoader.loadClass(SINGLE_DEFAULT_METHOD), classLoader.loadClass(CONFLICTING_INTERFACE));
    }

    @Test
    @Java8Rule.Enforce
    public void testAmbiguousDefaultMethodWithExplicitPreference() throws Exception {
        Class<?> singleMethodInterface = classLoader.loadClass(SINGLE_DEFAULT_METHOD);
        Class<?> conflictingInterface = classLoader.loadClass(CONFLICTING_INTERFACE);
        assertConflictChoice(singleMethodInterface, conflictingInterface, FOO, singleMethodInterface);
        assertConflictChoice(singleMethodInterface, conflictingInterface, QUX, conflictingInterface);
        assertConflictChoice(singleMethodInterface, conflictingInterface, FOO, singleMethodInterface, conflictingInterface);
        assertConflictChoice(singleMethodInterface, conflictingInterface, QUX, conflictingInterface, singleMethodInterface);
        assertConflictChoice(singleMethodInterface, conflictingInterface, FOO, singleMethodInterface, classLoader.loadClass(NON_OVERRIDING_INTERFACE));
        assertConflictChoice(singleMethodInterface, conflictingInterface, FOO, classLoader.loadClass(NON_OVERRIDING_INTERFACE), singleMethodInterface);
    }

    private void assertConflictChoice(Class<?> preferredInterface,
                                      Class<?> secondInterface,
                                      Object expectedResult,
                                      Class<?>... preferredInterfaces) throws Exception {
        DynamicType.Loaded<?> loaded = instrument(Object.class,
                DefaultMethodCall.preferring(preferredInterfaces),
                classLoader,
                not(isDeclaredBy(Object.class)),
                preferredInterface, secondInterface);
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        Method method = loaded.getLoaded().getDeclaredMethod(FOO);
        Object instance = loaded.getLoaded().newInstance();
        assertThat(method.invoke(instance), is(expectedResult));
    }

    @Test(expected = IllegalArgumentException.class)
    @Java8Rule.Enforce
    public void testUnrelatedPreferredDefaultMethodThrowsException() throws Exception {
        instrument(Object.class,
                DefaultMethodCall.preferring(classLoader.loadClass(NON_OVERRIDING_INTERFACE)),
                classLoader,
                not(isDeclaredBy(Object.class)),
                classLoader.loadClass(SINGLE_DEFAULT_METHOD), classLoader.loadClass(CONFLICTING_INTERFACE));
    }

    @Test(expected = IllegalArgumentException.class)
    @Java8Rule.Enforce
    public void testNonDeclaredDefaultMethodThrowsException() throws Exception {
        instrument(classLoader.loadClass(SINGLE_DEFAULT_METHOD_CLASS),
                DefaultMethodCall.unambiguousOnly(),
                classLoader,
                not(isDeclaredBy(Object.class)));
    }

    @Test(expected = IllegalArgumentException.class)
    @Java8Rule.Enforce
    public void testNonDeclaredPreferredDefaultMethodThrowsException() throws Exception {
        instrument(classLoader.loadClass(SINGLE_DEFAULT_METHOD_CLASS),
                DefaultMethodCall.preferring(classLoader.loadClass(SINGLE_DEFAULT_METHOD)),
                classLoader,
                not(isDeclaredBy(Object.class)));
    }

    @Test
    @Java8Rule.Enforce
    public void testDeclaredAndImplementedMethod() throws Exception {
        DynamicType.Loaded<?> loaded = instrument(classLoader.loadClass(SINGLE_DEFAULT_METHOD_CLASS),
                DefaultMethodCall.unambiguousOnly(),
                classLoader,
                not(isDeclaredBy(Object.class)),
                classLoader.loadClass(SINGLE_DEFAULT_METHOD));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        Method method = loaded.getLoaded().getDeclaredMethod(FOO);
        Object instance = loaded.getLoaded().newInstance();
        assertThat(method.invoke(instance), is((Object) FOO));
    }

    @Test(expected = IllegalArgumentException.class)
    @Java8Rule.Enforce
    public void testDeclaredAndImplementedAmbiguousMethodThrowsException() throws Exception {
        instrument(classLoader.loadClass(SINGLE_DEFAULT_METHOD_CLASS),
                DefaultMethodCall.unambiguousOnly(),
                classLoader,
                not(isDeclaredBy(Object.class)),
                classLoader.loadClass(SINGLE_DEFAULT_METHOD), classLoader.loadClass(CONFLICTING_INTERFACE));
    }

    @Test
    @Java8Rule.Enforce
    public void testDeclaredAndImplementedAmbiguousMethodWithPreference() throws Exception {
        DynamicType.Loaded<?> loaded = instrument(classLoader.loadClass(SINGLE_DEFAULT_METHOD_CLASS),
                DefaultMethodCall.preferring(classLoader.loadClass(SINGLE_DEFAULT_METHOD)),
                classLoader,
                not(isDeclaredBy(Object.class)),
                classLoader.loadClass(SINGLE_DEFAULT_METHOD), classLoader.loadClass(CONFLICTING_INTERFACE));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        Method method = loaded.getLoaded().getDeclaredMethod(FOO);
        Object instance = loaded.getLoaded().newInstance();
        assertThat(method.invoke(instance), is((Object) FOO));
    }
}
