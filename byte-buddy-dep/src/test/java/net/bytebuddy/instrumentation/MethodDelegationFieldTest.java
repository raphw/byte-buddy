package net.bytebuddy.instrumentation;

import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.instrumentation.method.bytecode.bind.annotation.Argument;
import net.bytebuddy.instrumentation.method.bytecode.bind.annotation.Field;
import org.junit.Before;
import org.junit.Test;

import java.io.Serializable;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class MethodDelegationFieldTest extends AbstractInstrumentationTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux";

    @Before
    public void setUp() throws Exception {
        ExplicitStatic.foo = FOO;
    }

    @Test
    public void testExplicitFieldAccess() throws Exception {
        DynamicType.Loaded<Explicit> loaded = instrument(Explicit.class, MethodDelegation.to(Swap.class)
                .appendParameterBinder(Field.Binder.install(Get.class, Set.class)));
        Explicit explicit = loaded.getLoaded().newInstance();
        assertThat(explicit.foo, is(FOO));
        explicit.swap();
        assertThat(explicit.foo, is(FOO + BAR));
    }

    @Test
    public void testExplicitFieldAccessSerializable() throws Exception {
        DynamicType.Loaded<Explicit> loaded = instrument(Explicit.class, MethodDelegation.to(SwapSerializable.class)
                .appendParameterBinder(Field.Binder.install(Get.class, Set.class)));
        Explicit explicit = loaded.getLoaded().newInstance();
        assertThat(explicit.foo, is(FOO));
        explicit.swap();
        assertThat(explicit.foo, is(FOO + BAR));
    }

    @Test
    public void testExplicitFieldAccessStatic() throws Exception {
        DynamicType.Loaded<ExplicitStatic> loaded = instrument(ExplicitStatic.class, MethodDelegation.to(Swap.class)
                .appendParameterBinder(Field.Binder.install(Get.class, Set.class)));
        ExplicitStatic explicit = loaded.getLoaded().newInstance();
        assertThat(ExplicitStatic.foo, is(FOO));
        explicit.swap();
        assertThat(ExplicitStatic.foo, is(FOO + BAR));
    }

    @Test
    public void testImplicitFieldGetterAccess() throws Exception {
        DynamicType.Loaded<ImplicitGetter> loaded = instrument(ImplicitGetter.class, MethodDelegation.to(GetInterceptor.class)
                .appendParameterBinder(Field.Binder.install(Get.class, Set.class)));
        ImplicitGetter implicitGetter = loaded.getLoaded().newInstance();
        assertThat(implicitGetter.foo, is(FOO));
        assertThat(implicitGetter.getFoo(), is(FOO + BAR));
        assertThat(implicitGetter.foo, is(FOO + BAR));
    }

    @Test
    public void testImplicitFieldSetterAccess() throws Exception {
        DynamicType.Loaded<ImplicitSetter> loaded = instrument(ImplicitSetter.class, MethodDelegation.to(SetInterceptor.class)
                .appendParameterBinder(Field.Binder.install(Get.class, Set.class)));
        ImplicitSetter implicitSetter = loaded.getLoaded().newInstance();
        assertThat(implicitSetter.foo, is(FOO));
        implicitSetter.setFoo(BAR);
        assertThat(implicitSetter.foo, is(FOO + BAR));
    }

    @Test
    public void testExplicitFieldAccessImplicitTarget() throws Exception {
        DynamicType.Loaded<ExplicitInherited> loaded = instrument(ExplicitInherited.class, MethodDelegation.to(Swap.class)
                .appendParameterBinder(Field.Binder.install(Get.class, Set.class)));
        ExplicitInherited explicitInherited = loaded.getLoaded().newInstance();
        assertThat(((Explicit) explicitInherited).foo, is(FOO));
        assertThat(explicitInherited.foo, is(QUX));
        explicitInherited.swap();
        assertThat(((Explicit) explicitInherited).foo, is(FOO));
        assertThat(explicitInherited.foo, is(QUX + BAR));
    }

    @Test
    public void testExplicitFieldAccessExplicitTarget() throws Exception {
        DynamicType.Loaded<ExplicitInherited> loaded = instrument(ExplicitInherited.class, MethodDelegation.to(SwapInherited.class)
                .appendParameterBinder(Field.Binder.install(Get.class, Set.class)));
        ExplicitInherited explicitInherited = loaded.getLoaded().newInstance();
        assertThat(((Explicit) explicitInherited).foo, is(FOO));
        assertThat(explicitInherited.foo, is(QUX));
        explicitInherited.swap();
        assertThat(((Explicit) explicitInherited).foo, is(FOO + BAR));
        assertThat(explicitInherited.foo, is(QUX));
    }

    @Test(expected = ClassCastException.class)
    public void testIncompatibleGetterTypeThrowsException() throws Exception {
        DynamicType.Loaded<Explicit> loaded = instrument(Explicit.class, MethodDelegation.to(GetterIncompatible.class)
                .appendParameterBinder(Field.Binder.install(Get.class, Set.class)));
        Explicit explicit = loaded.getLoaded().newInstance();
        explicit.swap();
    }

    @Test(expected = ClassCastException.class)
    public void testIncompatibleSetterTypeThrowsException() throws Exception {
        DynamicType.Loaded<Explicit> loaded = instrument(Explicit.class, MethodDelegation.to(SetterIncompatible.class)
                .appendParameterBinder(Field.Binder.install(Get.class, Set.class)));
        Explicit explicit = loaded.getLoaded().newInstance();
        explicit.swap();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetterTypeDoesNotDeclareCorrectMethodThrowsException() throws Exception {
        MethodDelegation.to(GetterIncompatible.class).appendParameterBinder(Field.Binder.install(Serializable.class, Set.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetterTypeDoesNotDeclareCorrectMethodThrowsException() throws Exception {
        MethodDelegation.to(GetterIncompatible.class).appendParameterBinder(Field.Binder.install(Get.class, Serializable.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetterTypeDoesInheritFromOtherTypeThrowsException() throws Exception {
        MethodDelegation.to(GetterIncompatible.class).appendParameterBinder(Field.Binder.install(GetInherited.class, Set.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetterTypeDoesInheritFromOtherTypeThrowsException() throws Exception {
        MethodDelegation.to(GetterIncompatible.class).appendParameterBinder(Field.Binder.install(Get.class, SetInherited.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetterTypeNonPublicThrowsException() throws Exception {
        MethodDelegation.to(GetterIncompatible.class).appendParameterBinder(Field.Binder.install(GetPrivate.class, Set.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetterTypeNonPublicThrowsException() throws Exception {
        MethodDelegation.to(GetterIncompatible.class).appendParameterBinder(Field.Binder.install(Get.class, SetPrivate.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetterTypeIncorrectSignatureThrowsException() throws Exception {
        MethodDelegation.to(GetterIncompatible.class).appendParameterBinder(Field.Binder.install(GetIncorrect.class, Set.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetterTypeIncorrectSignatureThrowsException() throws Exception {
        MethodDelegation.to(GetterIncompatible.class).appendParameterBinder(Field.Binder.install(Get.class, SetIncorrect.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetterTypeNotInterfaceThrowsException() throws Exception {
        MethodDelegation.to(GetterIncompatible.class).appendParameterBinder(Field.Binder.install(Object.class, Set.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetterTypeNotInterfaceThrowsException() throws Exception {
        MethodDelegation.to(GetterIncompatible.class).appendParameterBinder(Field.Binder.install(Get.class, Object.class));
    }

    public static interface Get<T> {

        T get();
    }

    public static interface Set<T> {

        void set(T value);
    }

    public static interface GetInherited<T> extends Get<T> {
        /* empty */
    }

    public static interface SetInherited<T> extends Set<T> {
        /* empty */
    }

    private static interface GetPrivate<T> {

        T get();
    }

    private static interface SetPrivate<T> {

        void set(T value);
    }

    public static interface GetIncorrect<T> {

        T get(Object value);
    }

    public static interface SetIncorrect<T> {

        Object set(T value);
    }

    public static class Swap {

        public static void swap(@Field(FOO) Get<String> getter, @Field(FOO) Set<String> setter) {
            assertThat(getter, not(instanceOf(Serializable.class)));
            assertThat(setter, not(instanceOf(Serializable.class)));
            setter.set(getter.get() + BAR);
        }
    }

    public static class Explicit {

        public void swap() {
            /* do nothing */
        }

        protected String foo = FOO;


    }

    public static class ExplicitInherited extends Explicit {

        protected String foo = QUX;

        @Override
        public void swap() {
            /* do nothing */
        }
    }

    public static class SwapInherited {

        public static void swap(@Field(value = FOO, definingType = Explicit.class) Get<String> getter,
                                @Field(value = FOO, definingType = Explicit.class) Set<String> setter) {
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

        public String getFoo() {
            return null;
        }

        protected String foo = FOO;


    }

    public static class GetInterceptor {

        public static String get(@Field Get<String> getter, @Field Set<String> setter) {
            setter.set(getter.get() + BAR);
            return getter.get();
        }
    }

    public static class ImplicitSetter {

        public void setFoo(String value) {
            /* do nothing */
        }

        protected String foo = FOO;


    }

    public static class SetInterceptor {

        public static void set(@Argument(0) String value, @Field Get<String> getter, @Field Set<String> setter) {
            setter.set(getter.get() + value);
        }
    }

    public static class SwapSerializable {

        public static void swap(@Field(value = FOO, serializableProxy = true) Get<String> getter,
                                @Field(value = FOO, serializableProxy = true) Set<String> setter) {
            assertThat(getter, instanceOf(Serializable.class));
            assertThat(setter, instanceOf(Serializable.class));
            setter.set(getter.get() + BAR);
        }
    }

    public static class GetterIncompatible {

        public static void swap(@Field(FOO) Get<Integer> getter, @Field(FOO) Set<String> setter) {
            Integer value = getter.get();
        }
    }

    public static class SetterIncompatible {

        public static void swap(@Field(FOO) Get<String> getter, @Field(FOO) Set<Integer> setter) {
            setter.set(0);
        }
    }
}
