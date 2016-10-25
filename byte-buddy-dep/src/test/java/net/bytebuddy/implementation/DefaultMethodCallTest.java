package net.bytebuddy.implementation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultMethodCallTest {

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
        DynamicType.Loaded<?> loaded = new ByteBuddy()
                .subclass(Object.class)
                .implement(Class.forName(SINGLE_DEFAULT_METHOD))
                .intercept(DefaultMethodCall.unambiguousOnly())
                .make()
                .load(Class.forName(SINGLE_DEFAULT_METHOD).getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        Method method = loaded.getLoaded().getDeclaredMethod(FOO);
        Object instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(method.invoke(instance), is((Object) FOO));
    }

    @Test(expected = IllegalStateException.class)
    @JavaVersionRule.Enforce(8)
    public void testAmbiguousDefaultMethodThrowsException() throws Exception {
        new ByteBuddy()
                .subclass(Object.class)
                .implement(Class.forName(SINGLE_DEFAULT_METHOD), Class.forName(CONFLICTING_INTERFACE))
                .intercept(DefaultMethodCall.unambiguousOnly())
                .make();
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
        DynamicType.Loaded<?> loaded = new ByteBuddy()
                .subclass(Object.class)
                .implement(preferredInterface, secondInterface)
                .intercept(DefaultMethodCall.prioritize(preferredInterfaces))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        Method method = loaded.getLoaded().getDeclaredMethod(FOO);
        Object instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(method.invoke(instance), is(expectedResult));
    }

    @Test(expected = IllegalStateException.class)
    @JavaVersionRule.Enforce(8)
    public void testUnrelatedPreferredDefaultMethodThrowsException() throws Exception {
        new ByteBuddy()
                .subclass(Object.class)
                .implement(Class.forName(SINGLE_DEFAULT_METHOD), Class.forName(CONFLICTING_INTERFACE))
                .intercept(DefaultMethodCall.prioritize(Class.forName(NON_OVERRIDING_INTERFACE)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    @JavaVersionRule.Enforce(8)
    public void testNonDeclaredDefaultMethodThrowsException() throws Exception {
        new ByteBuddy()
                .subclass(Class.forName(SINGLE_DEFAULT_METHOD_CLASS))
                .method(not(isDeclaredBy(Object.class)))
                .intercept(DefaultMethodCall.unambiguousOnly())
                .make();
    }

    @Test(expected = IllegalStateException.class)
    @JavaVersionRule.Enforce(8)
    public void testNonDeclaredPreferredDefaultMethodThrowsException() throws Exception {
        new ByteBuddy()
                .subclass(Class.forName(SINGLE_DEFAULT_METHOD_CLASS))
                .method(not(isDeclaredBy(Object.class)))
                .intercept(DefaultMethodCall.prioritize(Class.forName(SINGLE_DEFAULT_METHOD)))
                .make();
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testDeclaredAndImplementedMethod() throws Exception {
        DynamicType.Loaded<?> loaded = new ByteBuddy()
                .subclass(Class.forName(SINGLE_DEFAULT_METHOD_CLASS))
                .implement(Class.forName(SINGLE_DEFAULT_METHOD))
                .method(not(isDeclaredBy(Object.class)))
                .intercept(DefaultMethodCall.unambiguousOnly())
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        Method method = loaded.getLoaded().getDeclaredMethod(FOO);
        Object instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(method.invoke(instance), is((Object) FOO));
    }

    @Test(expected = IllegalStateException.class)
    @JavaVersionRule.Enforce(8)
    public void testDeclaredAndImplementedAmbiguousMethodThrowsException() throws Exception {
        new ByteBuddy()
                .subclass(Class.forName(SINGLE_DEFAULT_METHOD_CLASS))
                .implement(Class.forName(SINGLE_DEFAULT_METHOD), Class.forName(CONFLICTING_INTERFACE))
                .method(not(isDeclaredBy(Object.class)))
                .intercept(DefaultMethodCall.unambiguousOnly())
                .make();
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testDeclaredAndImplementedAmbiguousMethodWithPreference() throws Exception {
        DynamicType.Loaded<?> loaded = new ByteBuddy()
                .subclass(Class.forName(SINGLE_DEFAULT_METHOD_CLASS))
                .implement(Class.forName(SINGLE_DEFAULT_METHOD), Class.forName(CONFLICTING_INTERFACE))
                .method(not(isDeclaredBy(Object.class)))
                .intercept(DefaultMethodCall.prioritize(Class.forName(SINGLE_DEFAULT_METHOD)))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        Method method = loaded.getLoaded().getDeclaredMethod(FOO);
        Object instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
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
                when(typeDescription.getSort()).thenReturn(TypeDefinition.Sort.NON_GENERIC);
                return Collections.singletonList(typeDescription);
            }
        }).apply();
        final TypeDescription removalType = mock(TypeDescription.class);
        final TypeDescription.Generic genericRemovalType = mock(TypeDescription.Generic.class);
        when(genericRemovalType.asGenericType()).thenReturn(genericRemovalType);
        when(genericRemovalType.asErasure()).thenReturn(removalType);
        ObjectPropertyAssertion.of(DefaultMethodCall.Appender.class).refine(new ObjectPropertyAssertion.Refinement<Implementation.Target>() {
            @Override
            public void apply(Implementation.Target mock) {
                TypeDescription typeDescription = mock(TypeDescription.class), otherType = mock(TypeDescription.class);
                TypeDescription.Generic otherGenericType = mock(TypeDescription.Generic.class);
                when(otherGenericType.asErasure()).thenReturn(otherType);
                when(otherGenericType.asGenericType()).thenReturn(otherGenericType);
                when(typeDescription.getInterfaces()).thenReturn(new TypeList.Generic.Explicit(genericRemovalType, otherGenericType));
                when(mock.getInstrumentedType()).thenReturn(typeDescription);
            }
        }).create(new ObjectPropertyAssertion.Creator<List<?>>() {
            @Override
            public List<?> create() {
                TypeDescription typeDescription = mock(TypeDescription.class);
                when(typeDescription.asErasure()).thenReturn(typeDescription);
                return Arrays.asList(removalType, typeDescription);
            }
        }).apply();
    }
}
