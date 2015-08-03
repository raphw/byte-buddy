package net.bytebuddy.implementation;

import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.bind.annotation.Morph;
import net.bytebuddy.test.utility.CallTraceable;
import net.bytebuddy.test.utility.JavaVersionRule;
import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.io.Serializable;

import static net.bytebuddy.matcher.ElementMatchers.*;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodDelegationMorphTest extends AbstractImplementationTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux";

    private static final int BAZ = 42;

    private static final String DEFAULT_INTERFACE = "net.bytebuddy.test.precompiled.MorphDefaultInterface";

    private static final String DEFAULT_INTERFACE_TARGET_EXPLICIT = "net.bytebuddy.test.precompiled.MorphDefaultDelegationTargetExplicit";

    private static final String DEFAULT_INTERFACE_TARGET_IMPLICIT = "net.bytebuddy.test.precompiled.MorphDefaultDelegationTargetImplicit";

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Test
    public void testMorph() throws Exception {
        DynamicType.Loaded<Foo> loaded = implement(Foo.class, MethodDelegation.to(new SimpleMorph(QUX))
                .appendParameterBinder(Morph.Binder.install(Morphing.class)));
        Foo instance = loaded.getLoaded().newInstance();
        assertThat(instance.foo(FOO), is(QUX + BAR));
    }

    @Test
    public void testMorphVoid() throws Exception {
        SimpleMorph simpleMorph = new SimpleMorph(QUX);
        DynamicType.Loaded<Bar> loaded = implement(Bar.class, MethodDelegation.to(simpleMorph)
                .appendParameterBinder(Morph.Binder.install(Morphing.class)));
        Bar instance = loaded.getLoaded().newInstance();
        instance.foo();
        instance.assertOnlyCall(FOO);
        simpleMorph.assertOnlyCall(BAR);
    }

    @Test
    public void testMorphPrimitive() throws Exception {
        DynamicType.Loaded<Qux> loaded = implement(Qux.class, MethodDelegation.to(new PrimitiveMorph(BAZ))
                .appendParameterBinder(Morph.Binder.install(Morphing.class)));
        Qux instance = loaded.getLoaded().newInstance();
        assertThat(instance.foo(0), is(BAZ * 2L));
    }

    @Test
    public void testMorphSerializable() throws Exception {
        DynamicType.Loaded<Foo> loaded = implement(Foo.class, MethodDelegation.to(SimpleMorphSerializable.class)
                .appendParameterBinder(Morph.Binder.install(Morphing.class)));
        Foo instance = loaded.getLoaded().newInstance();
        assertThat(instance.foo(FOO), is(QUX + BAR));
    }

    @Test(expected = IllegalStateException.class)
    public void testMorphIllegal() throws Exception {
        implement(Foo.class, MethodDelegation.to(SimpleMorphIllegal.class)
                .appendParameterBinder(Morph.Binder.install(Morphing.class)));
    }

    @Test(expected = ClassCastException.class)
    public void testMorphToIncompatibleTypeThrowsException() throws Exception {
        DynamicType.Loaded<Foo> loaded = implement(Foo.class, MethodDelegation.to(new SimpleMorph(new Object()))
                .appendParameterBinder(Morph.Binder.install(Morphing.class)));
        Foo instance = loaded.getLoaded().newInstance();
        instance.foo(QUX);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMorphTypeDoesNotDeclareCorrectMethodThrowsException() throws Exception {
        MethodDelegation.to(new SimpleMorph(QUX)).defineParameterBinder(Morph.Binder.install(Serializable.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMorphTypeDoesInheritFromOtherTypeThrowsException() throws Exception {
        MethodDelegation.to(new SimpleMorph(QUX)).defineParameterBinder(Morph.Binder.install(InheritingMorphingType.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMorphTypeIsNotPublicThrowsException() throws Exception {
        MethodDelegation.to(new SimpleMorph(QUX)).defineParameterBinder(Morph.Binder.install(PackagePrivateMorphing.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPipeTypeDoesNotDeclareCorrectMethodSignatureThrowsException() throws Exception {
        MethodDelegation.to(new SimpleMorph(QUX)).defineParameterBinder(Morph.Binder.install(WrongParametersMorphing.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPipeTypeIsNotInterfaceThrowsException() throws Exception {
        MethodDelegation.to(new SimpleMorph(QUX)).defineParameterBinder(Morph.Binder.install(Object.class));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testDefaultMethodExplicit() throws Exception {
        DynamicType.Loaded<?> loaded = implement(Object.class,
                MethodDelegation.to(Class.forName(DEFAULT_INTERFACE_TARGET_EXPLICIT))
                        .appendParameterBinder(Morph.Binder.install(Morphing.class)),
                getClass().getClassLoader(),
                isMethod().and(not(isDeclaredBy(Object.class))),
                Class.forName(DEFAULT_INTERFACE));
        Object instance = loaded.getLoaded().newInstance();
        assertThat(instance.getClass().getDeclaredMethod(FOO, String.class)
                .invoke(instance, QUX), is((Object) (FOO + BAR)));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testDefaultMethodImplicit() throws Exception {
        DynamicType.Loaded<?> loaded = implement(Object.class,
                MethodDelegation.to(Class.forName(DEFAULT_INTERFACE_TARGET_IMPLICIT))
                        .appendParameterBinder(Morph.Binder.install(Morphing.class)),
                getClass().getClassLoader(),
                isMethod().and(not(isDeclaredBy(Object.class))),
                Class.forName(DEFAULT_INTERFACE));
        Object instance = loaded.getLoaded().newInstance();
        assertThat(instance.getClass().getDeclaredMethod(FOO, String.class)
                .invoke(instance, QUX), is((Object) (FOO + BAR)));
    }

    public interface Morphing<T> {

        T morph(Object... arguments);
    }

    public interface InheritingMorphingType<T> extends Morphing<T> {
        /* empty */
    }

    @SuppressWarnings("unused")
    private interface PackagePrivateMorphing<T> {

        T morph(Object... arguments);
    }

    @SuppressWarnings("unused")
    private interface WrongParametersMorphing<T> {

        T morph(Object arguments);
    }

    public static class Foo {

        public String foo(String foo) {
            return foo + BAR;
        }
    }

    public static class Bar extends CallTraceable {

        public void foo() {
            register(FOO);
        }
    }

    public static class Qux {

        public long foo(int argument) {
            return argument * 2L;
        }
    }

    public static class SimpleMorph extends CallTraceable {

        private final Object[] arguments;

        public SimpleMorph(Object... arguments) {
            this.arguments = arguments;
        }

        public String intercept(@Morph Morphing<String> morphing) {
            register(BAR);
            assertThat(morphing, CoreMatchers.not(instanceOf(Serializable.class)));
            return morphing.morph(arguments);
        }
    }

    public static class PrimitiveMorph {

        private final int argument;

        public PrimitiveMorph(int argument) {
            this.argument = argument;
        }

        public Long intercept(@Morph Morphing<Long> morphing) {
            assertThat(morphing, CoreMatchers.not(instanceOf(Serializable.class)));
            return morphing.morph(argument);
        }
    }

    public static class SimpleMorphSerializable {

        public static String intercept(@Morph(serializableProxy = true) Morphing<String> morphing) {
            assertThat(morphing, instanceOf(Serializable.class));
            return morphing.morph(QUX);
        }
    }

    @SuppressWarnings("unused")
    public static class SimpleMorphIllegal {

        public static String intercept(@Morph Void morphing) {
            return null;
        }
    }
}
