package net.bytebuddy.implementation;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeList;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultMethodCallTest extends AbstractImplementationTest {

    private static final String FOO = "foo", QUX = "qux";

    private static final String SINGLE_DEFAULT_METHOD = "net.bytebuddy.test.precompiled.SingleDefaultMethodInterface";

    private static final String SINGLE_DEFAULT_METHOD_CLASS = "net.bytebuddy.test.precompiled.SingleDefaultMethodClass";

    private static final String CONFLICTING_INTERFACE = "net.bytebuddy.test.precompiled.SingleDefaultMethodConflictingInterface";

    private static final String NON_OVERRIDING_INTERFACE = "net.bytebuddy.test.precompiled.SingleDefaultMethodNonOverridingInterface";

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Test
    @JavaVersionRule.Enforce(8)
    public void testUnambiguousDefaultMethod() throws Exception {
        DynamicType.Loaded<?> loaded = implement(Object.class,
                DefaultMethodCall.unambiguousOnly(),
                getClass().getClassLoader(),
                isMethod().and(not(isDeclaredBy(Object.class))),
                Class.forName(SINGLE_DEFAULT_METHOD));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        Method method = loaded.getLoaded().getDeclaredMethod(FOO);
        Object instance = loaded.getLoaded().newInstance();
        assertThat(method.invoke(instance), is((Object) FOO));
    }

    @Test(expected = IllegalStateException.class)
    @JavaVersionRule.Enforce(8)
    public void testAmbiguousDefaultMethodThrowsException() throws Exception {
        implement(Object.class,
                DefaultMethodCall.unambiguousOnly(),
                getClass().getClassLoader(),
                not(isDeclaredBy(Object.class)),
                Class.forName(SINGLE_DEFAULT_METHOD), Class.forName(CONFLICTING_INTERFACE));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testAmbiguousDefaultMethodWithExplicitPreference() throws Exception {
        Class<?> singleMethodInterface = Class.forName(SINGLE_DEFAULT_METHOD);
        Class<?> conflictingInterface = Class.forName(CONFLICTING_INTERFACE);
        assertConflictChoice(singleMethodInterface, conflictingInterface, FOO, singleMethodInterface);
        assertConflictChoice(singleMethodInterface, conflictingInterface, QUX, conflictingInterface);
        assertConflictChoice(singleMethodInterface, conflictingInterface, FOO, singleMethodInterface, conflictingInterface);
        assertConflictChoice(singleMethodInterface, conflictingInterface, QUX, conflictingInterface, singleMethodInterface);
        assertConflictChoice(singleMethodInterface, conflictingInterface, FOO, singleMethodInterface, Class.forName(NON_OVERRIDING_INTERFACE));
        assertConflictChoice(singleMethodInterface, conflictingInterface, FOO, Class.forName(NON_OVERRIDING_INTERFACE), singleMethodInterface);
    }

    private void assertConflictChoice(Class<?> preferredInterface,
                                      Class<?> secondInterface,
                                      Object expectedResult,
                                      Class<?>... preferredInterfaces) throws Exception {
        DynamicType.Loaded<?> loaded = implement(Object.class,
                DefaultMethodCall.prioritize(preferredInterfaces),
                getClass().getClassLoader(),
                isMethod().and(not(isDeclaredBy(Object.class))),
                preferredInterface, secondInterface);
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        Method method = loaded.getLoaded().getDeclaredMethod(FOO);
        Object instance = loaded.getLoaded().newInstance();
        assertThat(method.invoke(instance), is(expectedResult));
    }

    @Test(expected = IllegalStateException.class)
    @JavaVersionRule.Enforce(8)
    public void testUnrelatedPreferredDefaultMethodThrowsException() throws Exception {
        implement(Object.class,
                DefaultMethodCall.prioritize(Class.forName(NON_OVERRIDING_INTERFACE)),
                getClass().getClassLoader(),
                not(isDeclaredBy(Object.class)),
                Class.forName(SINGLE_DEFAULT_METHOD), Class.forName(CONFLICTING_INTERFACE));
    }

    @Test(expected = IllegalStateException.class)
    @JavaVersionRule.Enforce(8)
    public void testNonDeclaredDefaultMethodThrowsException() throws Exception {
        implement(Class.forName(SINGLE_DEFAULT_METHOD_CLASS),
                DefaultMethodCall.unambiguousOnly(),
                getClass().getClassLoader(),
                not(isDeclaredBy(Object.class)));
    }

    @Test(expected = IllegalStateException.class)
    @JavaVersionRule.Enforce(8)
    public void testNonDeclaredPreferredDefaultMethodThrowsException() throws Exception {
        implement(Class.forName(SINGLE_DEFAULT_METHOD_CLASS),
                DefaultMethodCall.prioritize(Class.forName(SINGLE_DEFAULT_METHOD)),
                getClass().getClassLoader(),
                not(isDeclaredBy(Object.class)));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testDeclaredAndImplementedMethod() throws Exception {
        DynamicType.Loaded<?> loaded = implement(Class.forName(SINGLE_DEFAULT_METHOD_CLASS),
                DefaultMethodCall.unambiguousOnly(),
                getClass().getClassLoader(),
                isMethod().and(not(isDeclaredBy(Object.class))),
                Class.forName(SINGLE_DEFAULT_METHOD));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        Method method = loaded.getLoaded().getDeclaredMethod(FOO);
        Object instance = loaded.getLoaded().newInstance();
        assertThat(method.invoke(instance), is((Object) FOO));
    }

    @Test(expected = IllegalStateException.class)
    @JavaVersionRule.Enforce(8)
    public void testDeclaredAndImplementedAmbiguousMethodThrowsException() throws Exception {
        implement(Class.forName(SINGLE_DEFAULT_METHOD_CLASS),
                DefaultMethodCall.unambiguousOnly(),
                getClass().getClassLoader(),
                not(isDeclaredBy(Object.class)),
                Class.forName(SINGLE_DEFAULT_METHOD), Class.forName(CONFLICTING_INTERFACE));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testDeclaredAndImplementedAmbiguousMethodWithPreference() throws Exception {
        DynamicType.Loaded<?> loaded = implement(Class.forName(SINGLE_DEFAULT_METHOD_CLASS),
                DefaultMethodCall.prioritize(Class.forName(SINGLE_DEFAULT_METHOD)),
                getClass().getClassLoader(),
                isMethod().and(not(isDeclaredBy(Object.class))),
                Class.forName(SINGLE_DEFAULT_METHOD), Class.forName(CONFLICTING_INTERFACE));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        Method method = loaded.getLoaded().getDeclaredMethod(FOO);
        Object instance = loaded.getLoaded().newInstance();
        assertThat(method.invoke(instance), is((Object) FOO));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(DefaultMethodCall.class).create(new ObjectPropertyAssertion.Creator<List<?>>() {
            @Override
            public List<?> create() {
                TypeDescription typeDescription = mock(TypeDescription.class);
                when(typeDescription.isInterface()).thenReturn(true);
                when(typeDescription.asErasure()).thenReturn(typeDescription);
                when(typeDescription.getSort()).thenReturn(GenericTypeDescription.Sort.NON_GENERIC);
                return Collections.singletonList(typeDescription);
            }
        }).apply();
        final TypeDescription removalType = mock(TypeDescription.class);
        ObjectPropertyAssertion.of(DefaultMethodCall.Appender.class).refine(new ObjectPropertyAssertion.Refinement<Implementation.Target>() {
            @Override
            public void apply(Implementation.Target mock) {
                TypeDescription typeDescription = mock(TypeDescription.class);
                when(typeDescription.getInterfaces()).thenReturn(new GenericTypeList.Explicit(Arrays.asList(removalType, mock(TypeDescription.class))));
                when(mock.getTypeDescription()).thenReturn(typeDescription);
            }
        }).create(new ObjectPropertyAssertion.Creator<List<?>>() {
            @Override
            public List<?> create() {
                return Arrays.asList(removalType, mock(TypeDescription.class));
            }
        }).apply();
    }
}
