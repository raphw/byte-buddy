package net.bytebuddy.implementation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.FieldProxy;
import org.junit.Before;
import org.junit.Test;

import java.io.Serializable;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodDelegationFieldProxyTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux";

    @Before
    public void setUp() throws Exception {
        ExplicitStatic.foo = FOO;
    }

    @Test
    public void testExplicitFieldAccessSimplex() throws Exception {
        DynamicType.Loaded<Explicit> loaded = new ByteBuddy()
                .subclass(Explicit.class)
                .method(isDeclaredBy(Explicit.class))
                .intercept(MethodDelegation.withDefaultConfiguration().withBinders(FieldProxy.Binder.install(Get.class, Set.class)).to(Swap.class))
                .make()
                .load(Explicit.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        Explicit explicit = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(explicit.foo, is(FOO));
        explicit.swap();
        assertThat(explicit.foo, is(FOO + BAR));
    }

    @Test
    public void testExplicitFieldAccessDuplex() throws Exception {
        DynamicType.Loaded<Explicit> loaded = new ByteBuddy()
                .subclass(Explicit.class)
                .method(isDeclaredBy(Explicit.class))
                .intercept(MethodDelegation.withDefaultConfiguration().withBinders(FieldProxy.Binder.install(GetSet.class)).to(SwapDuplex.class))
                .make()
                .load(Explicit.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        Explicit explicit = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(explicit.foo, is(FOO));
        explicit.swap();
        assertThat(explicit.foo, is(FOO + BAR));
    }

    @Test
    public void testExplicitFieldAccessSerializableSimplex() throws Exception {
        DynamicType.Loaded<Explicit> loaded = new ByteBuddy()
                .subclass(Explicit.class)
                .method(isDeclaredBy(Explicit.class))
                .intercept(MethodDelegation.withDefaultConfiguration().withBinders(FieldProxy.Binder.install(Get.class, Set.class)).to(SwapSerializable.class))
                .make()
                .load(Explicit.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        Explicit explicit = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(explicit.foo, is(FOO));
        explicit.swap();
        assertThat(explicit.foo, is(FOO + BAR));
    }

    @Test
    public void testExplicitFieldAccessSerializableDuplex() throws Exception {
        DynamicType.Loaded<Explicit> loaded = new ByteBuddy()
                .subclass(Explicit.class)
                .method(isDeclaredBy(Explicit.class))
                .intercept(MethodDelegation.withDefaultConfiguration().withBinders(FieldProxy.Binder.install(GetSet.class)).to(SwapSerializableDuplex.class))
                .make()
                .load(Explicit.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        Explicit explicit = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(explicit.foo, is(FOO));
        explicit.swap();
        assertThat(explicit.foo, is(FOO + BAR));
    }

    @Test
    public void testExplicitFieldAccessStatic() throws Exception {
        DynamicType.Loaded<ExplicitStatic> loaded = new ByteBuddy()
                .subclass(ExplicitStatic.class)
                .method(isDeclaredBy(ExplicitStatic.class))
                .intercept(MethodDelegation.withDefaultConfiguration().withBinders(FieldProxy.Binder.install(Get.class, Set.class)).to(Swap.class))
                .make()
                .load(ExplicitStatic.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        ExplicitStatic explicit = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(ExplicitStatic.foo, is(FOO));
        explicit.swap();
        assertThat(ExplicitStatic.foo, is(FOO + BAR));
    }

    @Test
    public void testExplicitFieldAccessStaticDuplex() throws Exception {
        DynamicType.Loaded<ExplicitStatic> loaded = new ByteBuddy()
                .subclass(ExplicitStatic.class)
                .method(isDeclaredBy(ExplicitStatic.class))
                .intercept(MethodDelegation.withDefaultConfiguration().withBinders(FieldProxy.Binder.install(GetSet.class)).to(SwapDuplex.class))
                .make()
                .load(ExplicitStatic.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        ExplicitStatic explicit = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(ExplicitStatic.foo, is(FOO));
        explicit.swap();
        assertThat(ExplicitStatic.foo, is(FOO + BAR));
    }

    @Test
    public void testImplicitFieldGetterAccess() throws Exception {
        DynamicType.Loaded<ImplicitGetter> loaded = new ByteBuddy()
                .subclass(ImplicitGetter.class)
                .method(isDeclaredBy(ImplicitGetter.class))
                .intercept(MethodDelegation.withDefaultConfiguration().withBinders(FieldProxy.Binder.install(Get.class, Set.class)).to(GetInterceptor.class))
                .make()
                .load(ImplicitGetter.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        ImplicitGetter implicitGetter = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(implicitGetter.foo, is(FOO));
        assertThat(implicitGetter.getFoo(), is(FOO + BAR));
        assertThat(implicitGetter.foo, is(FOO + BAR));
    }

    @Test
    public void testImplicitFieldSetterAccess() throws Exception {
        DynamicType.Loaded<ImplicitSetter> loaded = new ByteBuddy()
                .subclass(ImplicitSetter.class)
                .method(isDeclaredBy(ImplicitSetter.class))
                .intercept(MethodDelegation.withDefaultConfiguration().withBinders(FieldProxy.Binder.install(Get.class, Set.class)).to(SetInterceptor.class))
                .make()
                .load(ImplicitSetter.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        ImplicitSetter implicitSetter = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(implicitSetter.foo, is(FOO));
        implicitSetter.setFoo(BAR);
        assertThat(implicitSetter.foo, is(FOO + BAR));
    }

    @Test
    public void testImplicitFieldGetterAccessDuplex() throws Exception {
        DynamicType.Loaded<ImplicitGetter> loaded = new ByteBuddy()
                .subclass(ImplicitGetter.class)
                .method(isDeclaredBy(ImplicitGetter.class))
                .intercept(MethodDelegation.withDefaultConfiguration().withBinders(FieldProxy.Binder.install(GetSet.class)).to(GetInterceptorDuplex.class))
                .make()
                .load(ImplicitGetter.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        ImplicitGetter implicitGetter = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(implicitGetter.foo, is(FOO));
        assertThat(implicitGetter.getFoo(), is(FOO + BAR));
        assertThat(implicitGetter.foo, is(FOO + BAR));
    }

    @Test
    public void testImplicitFieldSetterAccessDuplex() throws Exception {
        DynamicType.Loaded<ImplicitSetter> loaded = new ByteBuddy()
                .subclass(ImplicitSetter.class)
                .method(isDeclaredBy(ImplicitSetter.class))
                .intercept(MethodDelegation.withDefaultConfiguration().withBinders(FieldProxy.Binder.install(GetSet.class)).to(SetInterceptorDuplex.class))
                .make()
                .load(ImplicitSetter.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        ImplicitSetter implicitSetter = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(implicitSetter.foo, is(FOO));
        implicitSetter.setFoo(BAR);
        assertThat(implicitSetter.foo, is(FOO + BAR));
    }

    @Test
    public void testExplicitFieldAccessImplicitTarget() throws Exception {
        DynamicType.Loaded<ExplicitInherited> loaded = new ByteBuddy()
                .subclass(ExplicitInherited.class)
                .method(isDeclaredBy(ExplicitInherited.class))
                .intercept(MethodDelegation.withDefaultConfiguration().withBinders(FieldProxy.Binder.install(Get.class, Set.class)).to(Swap.class))
                .make()
                .load(ExplicitInherited.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        ExplicitInherited explicitInherited = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(((Explicit) explicitInherited).foo, is(FOO));
        assertThat(explicitInherited.foo, is(QUX));
        explicitInherited.swap();
        assertThat(((Explicit) explicitInherited).foo, is(FOO));
        assertThat(explicitInherited.foo, is(QUX + BAR));
    }

    @Test
    public void testExplicitFieldAccessExplicitTarget() throws Exception {
        DynamicType.Loaded<ExplicitInherited> loaded = new ByteBuddy()
                .subclass(ExplicitInherited.class)
                .method(isDeclaredBy(ExplicitInherited.class))
                .intercept(MethodDelegation.withDefaultConfiguration().withBinders(FieldProxy.Binder.install(Get.class, Set.class)).to(SwapInherited.class))
                .make()
                .load(ExplicitInherited.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        ExplicitInherited explicitInherited = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(((Explicit) explicitInherited).foo, is(FOO));
        assertThat(explicitInherited.foo, is(QUX));
        explicitInherited.swap();
        assertThat(((Explicit) explicitInherited).foo, is(FOO + BAR));
        assertThat(explicitInherited.foo, is(QUX));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFinalFieldSimplex() throws Exception {
        new ByteBuddy()
                .subclass(FinalField.class)
                .method(isDeclaredBy(FinalField.class))
                .intercept(MethodDelegation.withDefaultConfiguration().withBinders(FieldProxy.Binder.install(Get.class, Set.class)).to(Swap.class))
                .make();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testFinalFieldDuplex() throws Exception {
        DynamicType.Loaded<FinalField> loaded = new ByteBuddy()
                .subclass(FinalField.class)
                .method(isDeclaredBy(FinalField.class))
                .intercept(MethodDelegation.withDefaultConfiguration().withBinders(FieldProxy.Binder.install(GetSet.class)).to(SwapDuplex.class))
                .make()
                .load(FinalField.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        loaded.getLoaded().getDeclaredConstructor().newInstance().method();
    }

    @Test(expected = ClassCastException.class)
    public void testIncompatibleGetterTypeThrowsException() throws Exception {
        DynamicType.Loaded<Explicit> loaded = new ByteBuddy()
                .subclass(Explicit.class)
                .method(isDeclaredBy(Explicit.class))
                .intercept(MethodDelegation.withDefaultConfiguration().withBinders(FieldProxy.Binder.install(Get.class, Set.class)).to(GetterIncompatible.class))
                .make()
                .load(Explicit.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        Explicit explicit = loaded.getLoaded().getDeclaredConstructor().newInstance();
        explicit.swap();
    }

    @Test(expected = ClassCastException.class)
    public void testIncompatibleSetterTypeThrowsException() throws Exception {
        DynamicType.Loaded<Explicit> loaded = new ByteBuddy()
                .subclass(Explicit.class)
                .method(isDeclaredBy(Explicit.class))
                .intercept(MethodDelegation.withDefaultConfiguration().withBinders(FieldProxy.Binder.install(Get.class, Set.class)).to(SetterIncompatible.class))
                .make()
                .load(Explicit.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        Explicit explicit = loaded.getLoaded().getDeclaredConstructor().newInstance();
        explicit.swap();
    }

    @Test(expected = ClassCastException.class)
    public void testIncompatibleTypeThrowsExceptionGetDuplex() throws Exception {
        DynamicType.Loaded<Explicit> loaded = new ByteBuddy()
                .subclass(Explicit.class)
                .method(isDeclaredBy(Explicit.class))
                .intercept(MethodDelegation.withDefaultConfiguration().withBinders(FieldProxy.Binder.install(GetSet.class)).to(GetterIncompatibleDuplex.class))
                .make()
                .load(Explicit.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        Explicit explicit = loaded.getLoaded().getDeclaredConstructor().newInstance();
        explicit.swap();
    }

    @Test(expected = ClassCastException.class)
    public void testIncompatibleTypeThrowsExceptionSetDuplex() throws Exception {
        DynamicType.Loaded<Explicit> loaded = new ByteBuddy()
                .subclass(Explicit.class)
                .method(isDeclaredBy(Explicit.class))
                .intercept(MethodDelegation.withDefaultConfiguration().withBinders(FieldProxy.Binder.install(GetSet.class)).to(SetterIncompatibleDuplex.class))
                .make()
                .load(Explicit.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        Explicit explicit = loaded.getLoaded().getDeclaredConstructor().newInstance();
        explicit.swap();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetterTypeDoesNotDeclareCorrectMethodThrowsException() throws Exception {
        FieldProxy.Binder.install(Serializable.class, Set.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetterTypeDoesNotDeclareCorrectMethodThrowsException() throws Exception {
        FieldProxy.Binder.install(Get.class, Serializable.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetterTypeDoesInheritFromOtherTypeThrowsException() throws Exception {
        FieldProxy.Binder.install(GetInherited.class, Set.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetterTypeDoesInheritFromOtherTypeThrowsException() throws Exception {
        FieldProxy.Binder.install(Get.class, SetInherited.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetterTypeNonPublicThrowsException() throws Exception {
        FieldProxy.Binder.install(GetPrivate.class, Set.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetterTypeNonPublicThrowsException() throws Exception {
        FieldProxy.Binder.install(Get.class, SetPrivate.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetterTypeIncorrectSignatureThrowsException() throws Exception {
        FieldProxy.Binder.install(GetIncorrect.class, Set.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetterTypeIncorrectSignatureThrowsException() throws Exception {
        FieldProxy.Binder.install(Get.class, SetIncorrect.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetterTypeNotInterfaceThrowsException() throws Exception {
        FieldProxy.Binder.install(Object.class, Set.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetterTypeNotInterfaceThrowsException() throws Exception {
        FieldProxy.Binder.install(Get.class, Object.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDuplexTooManyMethodsThrowsException() throws Exception {
        FieldProxy.Binder.install(GetSetTooManyMethods.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDuplexNonPublicThrowsException() throws Exception {
        FieldProxy.Binder.install(GetSetNonPublic.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDuplexInheritedThrowsException() throws Exception {
        FieldProxy.Binder.install(GetSetInherited.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetterTypeIncorrectSignatureDuplexThrowsException() throws Exception {
        FieldProxy.Binder.install(GetSetSetIncorrect.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetterTypeIncorrectSignatureDuplexThrowsException() throws Exception {
        FieldProxy.Binder.install(GetSetGetIncorrect.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTypeNotInterfaceDuplexThrowsException() throws Exception {
        FieldProxy.Binder.install(Object.class);
    }

    public interface Get<T> {

        T get();
    }

    public interface Set<T> {

        void set(T value);
    }

    public interface GetInherited<T> extends Get<T> {
        /* empty */
    }

    public interface SetInherited<T> extends Set<T> {
        /* empty */
    }

    private interface GetPrivate<T> {

        T get();
    }

    private interface SetPrivate<T> {

        void set(T value);
    }

    public interface GetIncorrect<T> {

        T get(Object value);
    }

    public interface SetIncorrect<T> {

        Object set(T value);
    }

    public interface GetSet<T> {

        T get();

        void set(T value);
    }

    public interface GetSetGetIncorrect<T> {

        String get();

        void set(T value);
    }

    public interface GetSetSetIncorrect<T> {

        T get();

        void set(String value);
    }

    interface GetSetNonPublic<T> {

        T get();

        void set(T value);
    }

    public interface GetSetTooManyMethods<T> {

        T get();

        void set(String value);

        void set(T value);
    }

    public interface GetSetInherited<T> extends GetSet<T> {
        /* empty */
    }

    public static class Swap {

        public static void swap(@FieldProxy(FOO) Get<String> getter, @FieldProxy(FOO) Set<String> setter) {
            assertThat(getter, not(instanceOf(Serializable.class)));
            assertThat(setter, not(instanceOf(Serializable.class)));
            setter.set(getter.get() + BAR);
        }
    }

    public static class SwapDuplex {

        public static void swap(@FieldProxy(FOO) GetSet<String> accessor) {
            assertThat(accessor, not(instanceOf(Serializable.class)));
            accessor.set(accessor.get() + BAR);
        }
    }

    public static class Explicit {

        protected String foo = FOO;

        public void swap() {
            /* do nothing */
        }
    }

    public static class ExplicitInherited extends Explicit {

        protected String foo = QUX;

        public void swap() {
            /* do nothing */
        }
    }

    public static class SwapInherited {

        public static void swap(@FieldProxy(value = FOO, declaringType = Explicit.class) Get<String> getter,
                                @FieldProxy(value = FOO, declaringType = Explicit.class) Set<String> setter) {
            assertThat(getter, not(instanceOf(Serializable.class)));
            assertThat(setter, not(instanceOf(Serializable.class)));
            setter.set(getter.get() + BAR);
        }
    }

    public static class ExplicitStatic {

        protected static String foo;

        public void swap() {
            /* do nothing */
        }
    }

    public static class ImplicitGetter {

        protected String foo = FOO;

        public String getFoo() {
            return null;
        }


    }

    public static class GetInterceptor {

        public static String get(@FieldProxy Get<String> getter, @FieldProxy Set<String> setter) {
            setter.set(getter.get() + BAR);
            return getter.get();
        }
    }

    public static class SetInterceptor {

        public static void set(@Argument(0) String value, @FieldProxy Get<String> getter, @FieldProxy Set<String> setter) {
            setter.set(getter.get() + value);
        }
    }

    public static class GetInterceptorDuplex {

        public static String get(@FieldProxy GetSet<String> accessor) {
            accessor.set(accessor.get() + BAR);
            return accessor.get();
        }
    }

    public static class SetInterceptorDuplex {

        public static void set(@Argument(0) String value, @FieldProxy GetSet<String> accessor) {
            accessor.set(accessor.get() + value);
        }
    }

    public static class ImplicitSetter {

        protected String foo = FOO;

        public void setFoo(String value) {
            /* do nothing */
        }
    }

    public static class FinalField {

        protected final String foo = FOO;

        public void method() {
            /* do nothing */
        }
    }

    public static class SwapSerializable {

        public static void swap(@FieldProxy(value = FOO, serializableProxy = true) Get<String> getter,
                                @FieldProxy(value = FOO, serializableProxy = true) Set<String> setter) {
            assertThat(getter, instanceOf(Serializable.class));
            assertThat(setter, instanceOf(Serializable.class));
            setter.set(getter.get() + BAR);
        }
    }

    public static class SwapSerializableDuplex {

        public static void swap(@FieldProxy(value = FOO, serializableProxy = true) GetSet<String> accessor) {
            assertThat(accessor, instanceOf(Serializable.class));
            accessor.set(accessor.get() + BAR);
        }
    }

    public static class GetterIncompatible {

        public static void swap(@FieldProxy(FOO) Get<Integer> getter, @FieldProxy(FOO) Set<String> setter) {
            Integer value = getter.get();
        }
    }

    public static class SetterIncompatible {

        public static void swap(@FieldProxy(FOO) Get<String> getter, @FieldProxy(FOO) Set<Integer> setter) {
            setter.set(0);
        }
    }

    public static class GetterIncompatibleDuplex {

        public static void swap(@FieldProxy(FOO) GetSet<Integer> accessor) {
            Integer value = accessor.get();
        }
    }

    public static class SetterIncompatibleDuplex {

        public static void swap(@FieldProxy(FOO) GetSet<Integer> accessor) {
            accessor.set(0);
        }
    }
}
