package net.bytebuddy.implementation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.bind.annotation.Morph;
import net.bytebuddy.test.utility.CallTraceable;
import net.bytebuddy.test.utility.JavaVersionRule;
import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.io.Serializable;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodDelegationMorphTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux";

    private static final int BAZ = 42;

    private static final String DEFAULT_INTERFACE = "net.bytebuddy.test.precompiled.MorphDefaultInterface";

    private static final String DEFAULT_INTERFACE_TARGET_EXPLICIT = "net.bytebuddy.test.precompiled.MorphDefaultDelegationTargetExplicit";

    private static final String DEFAULT_INTERFACE_TARGET_IMPLICIT = "net.bytebuddy.test.precompiled.MorphDefaultDelegationTargetImplicit";

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    @Test
    public void testMorph() throws Exception {
        DynamicType.Loaded<Foo> loaded = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(MethodDelegation.withDefaultConfiguration()
                        .withBinders(Morph.Binder.install(Morphing.class))
                        .to(new SimpleMorph(QUX)))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        Foo instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.foo(FOO), is(QUX + BAR));
    }

    @Test
    public void testMorphVoid() throws Exception {
        SimpleMorph simpleMorph = new SimpleMorph(QUX);
        DynamicType.Loaded<Bar> loaded = new ByteBuddy()
                .subclass(Bar.class)
                .method(isDeclaredBy(Bar.class))
                .intercept(MethodDelegation.withDefaultConfiguration()
                        .withBinders(Morph.Binder.install(Morphing.class))
                        .to(simpleMorph))
                .make()
                .load(Bar.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        Bar instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        instance.foo();
        instance.assertOnlyCall(FOO);
        simpleMorph.assertOnlyCall(BAR);
    }

    @Test
    public void testMorphPrimitive() throws Exception {
        DynamicType.Loaded<Qux> loaded = new ByteBuddy()
                .subclass(Qux.class)
                .method(isDeclaredBy(Qux.class))
                .intercept(MethodDelegation.withDefaultConfiguration()
                        .withBinders(Morph.Binder.install(Morphing.class))
                        .to(new PrimitiveMorph(BAZ)))
                .make()
                .load(Qux.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        Qux instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.foo(0), is(BAZ * 2L));
    }

    @Test
    public void testMorphSerializable() throws Exception {
        DynamicType.Loaded<Foo> loaded = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(MethodDelegation.withDefaultConfiguration()
                        .withBinders(Morph.Binder.install(Morphing.class))
                        .to(SimpleMorphSerializable.class))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        Foo instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.foo(FOO), is(QUX + BAR));
    }

    @Test(expected = IllegalStateException.class)
    public void testMorphIllegal() throws Exception {
        new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(MethodDelegation.withDefaultConfiguration()
                        .withBinders(Morph.Binder.install(Morphing.class))
                        .to(SimpleMorphIllegal.class))
                .make();
    }

    @Test(expected = ClassCastException.class)
    public void testMorphToIncompatibleTypeThrowsException() throws Exception {
        DynamicType.Loaded<Foo> loaded = new ByteBuddy()
                .subclass(Foo.class)
                .method(isDeclaredBy(Foo.class))
                .intercept(MethodDelegation.withDefaultConfiguration()
                        .withBinders(Morph.Binder.install(Morphing.class))
                        .to(new SimpleMorph(new Object())))
                .make()
                .load(Foo.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        Foo instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        instance.foo(QUX);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMorphTypeDoesNotDeclareCorrectMethodThrowsException() throws Exception {
        Morph.Binder.install(Serializable.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMorphTypeDoesInheritFromOtherTypeThrowsException() throws Exception {
        Morph.Binder.install(InheritingMorphingType.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMorphTypeIsNotPublicThrowsException() throws Exception {
        Morph.Binder.install(PackagePrivateMorphing.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPipeTypeDoesNotDeclareCorrectMethodSignatureThrowsException() throws Exception {
        Morph.Binder.install(WrongParametersMorphing.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPipeTypeIsNotInterfaceThrowsException() throws Exception {
        MethodDelegation.withDefaultConfiguration().withBinders(Morph.Binder.install(Object.class)).to(new SimpleMorph(QUX));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testDefaultMethodExplicit() throws Exception {
        DynamicType.Loaded<Object> loaded = new ByteBuddy()
                .subclass(Object.class)
                .implement(Class.forName(DEFAULT_INTERFACE))
                .intercept(MethodDelegation.withDefaultConfiguration()
                        .withBinders(Morph.Binder.install(Morphing.class))
                        .to(Class.forName(DEFAULT_INTERFACE_TARGET_EXPLICIT)))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        Object instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance.getClass().getDeclaredMethod(FOO, String.class)
                .invoke(instance, QUX), is((Object) (FOO + BAR)));
    }

    @Test
    @JavaVersionRule.Enforce(8)
    public void testDefaultMethodImplicit() throws Exception {
        DynamicType.Loaded<Object> loaded = new ByteBuddy()
                .subclass(Object.class)
                .implement(Class.forName(DEFAULT_INTERFACE))
                .intercept(MethodDelegation.withDefaultConfiguration()
                        .withBinders(Morph.Binder.install(Morphing.class))
                        .to(Class.forName(DEFAULT_INTERFACE_TARGET_IMPLICIT)))
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        Object instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
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
