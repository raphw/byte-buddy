package net.bytebuddy.implementation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Ownership;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.junit.Test;

import static net.bytebuddy.matcher.ElementMatchers.isEquals;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class EqualsMethodOtherTest {

    private static final String FOO = "foo", BAR = "bar";

    @Test(expected = NullPointerException.class)
    public void testNullableField() throws Exception {
        Class<?> type = new ByteBuddy()
                .subclass(Object.class)
                .defineField(FOO, Object.class, Visibility.PUBLIC)
                .method(isEquals())
                .intercept(EqualsMethod.isolated().withNonNullableFields(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        type.getDeclaredConstructor().newInstance().equals(type.getDeclaredConstructor().newInstance());
    }

    @Test
    public void testEqualToSelf() throws Exception {
        DynamicType.Loaded<?> loaded = new ByteBuddy()
                .subclass(Object.class)
                .defineField(FOO, Object.class, Visibility.PUBLIC)
                .method(isEquals())
                .intercept(EqualsMethod.isolated().withNonNullableFields(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(1));
        Object instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        assertThat(instance, is(instance));
    }

    @Test
    public void testEqualToSelfIdentity() throws Exception {
        DynamicType.Loaded<?> loaded = new ByteBuddy()
                .subclass(Object.class)
                .defineField(FOO, Object.class, Visibility.PUBLIC)
                .method(isEquals())
                .intercept(EqualsMethod.isolated())
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(1));
        Object instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        loaded.getLoaded().getDeclaredField(FOO).set(instance, new NonEqualsBase());
        assertThat(instance, is(instance));
    }

    @Test
    public void testIgnoredField() throws Exception {
        DynamicType.Loaded<?> loaded = new ByteBuddy()
                .subclass(Object.class)
                .defineField(FOO, Object.class, Visibility.PUBLIC)
                .method(isEquals())
                .intercept(EqualsMethod.isolated().withIgnoredFields(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(1));
        Object left = loaded.getLoaded().getDeclaredConstructor().newInstance(), right = loaded.getLoaded().getDeclaredConstructor().newInstance();
        left.getClass().getDeclaredField(FOO).set(left, FOO);
        left.getClass().getDeclaredField(FOO).set(left, BAR);
        assertThat(left, is(right));
    }

    @Test
    public void testSuperMethod() throws Exception {
        DynamicType.Loaded<?> loaded = new ByteBuddy()
                .subclass(EqualsBase.class)
                .defineField(FOO, Object.class, Visibility.PUBLIC)
                .method(isEquals())
                .intercept(EqualsMethod.requiringSuperClassEquality())
                .make()
                .load(EqualsBase.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(1));
        Object left = loaded.getLoaded().getDeclaredConstructor().newInstance(), right = loaded.getLoaded().getDeclaredConstructor().newInstance();
        left.getClass().getDeclaredField(FOO).set(left, FOO);
        right.getClass().getDeclaredField(FOO).set(right, FOO);
        assertThat(left, is(right));
    }

    @Test
    public void testSuperClass() throws Exception {
        DynamicType.Loaded<?> superClass = new ByteBuddy()
                .subclass(EqualsBase.class)
                .defineField(FOO, Object.class, Visibility.PUBLIC)
                .method(isEquals())
                .intercept(EqualsMethod.isolated())
                .make()
                .load(EqualsBase.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        DynamicType.Loaded<?> subClass = new ByteBuddy()
                .subclass(superClass.getLoaded())
                .make()
                .load(superClass.getLoaded().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        Object left = subClass.getLoaded().getDeclaredConstructor().newInstance(), right = subClass.getLoaded().getDeclaredConstructor().newInstance();
        superClass.getLoaded().getDeclaredField(FOO).set(left, FOO);
        superClass.getLoaded().getDeclaredField(FOO).set(right, FOO);
        assertThat(left, is(right));
    }

    @Test
    public void testSuperMethodNoMatch() throws Exception {
        DynamicType.Loaded<?> loaded = new ByteBuddy()
                .subclass(NonEqualsBase.class)
                .defineField(FOO, Object.class, Visibility.PUBLIC)
                .method(isEquals())
                .intercept(EqualsMethod.requiringSuperClassEquality())
                .make()
                .load(NonEqualsBase.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(1));
        Object left = loaded.getLoaded().getDeclaredConstructor().newInstance(), right = loaded.getLoaded().getDeclaredConstructor().newInstance();
        left.getClass().getDeclaredField(FOO).set(left, FOO);
        right.getClass().getDeclaredField(FOO).set(right, FOO);
        assertThat(left, not(right));
    }

    @Test
    public void testInstanceOf() throws Exception {
        DynamicType.Loaded<?> superClass = new ByteBuddy()
                .subclass(Object.class)
                .method(isEquals())
                .intercept(EqualsMethod.isolated().withSubclassEquality())
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER);
        DynamicType.Loaded<?> subClass = new ByteBuddy()
                .subclass(superClass.getLoaded())
                .make()
                .load(superClass.getLoaded().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(superClass.getLoaded().getDeclaredConstructor().newInstance(), is(subClass.getLoaded().getDeclaredConstructor().newInstance()));
        assertThat(subClass.getLoaded().getDeclaredConstructor().newInstance(), is(superClass.getLoaded().getDeclaredConstructor().newInstance()));
    }

    @Test(expected = IllegalStateException.class)
    public void testInterface() throws Exception {
        new ByteBuddy()
                .makeInterface()
                .method(isEquals())
                .intercept(EqualsMethod.isolated())
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testIncompatibleReturn() throws Exception {
        new ByteBuddy()
                .subclass(Object.class)
                .defineMethod(FOO, Object.class)
                .withParameters(Object.class)
                .intercept(EqualsMethod.isolated())
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testIncompatibleArgumentLength() throws Exception {
        new ByteBuddy()
                .subclass(Object.class)
                .defineMethod(FOO, boolean.class)
                .withParameters(Object.class, Object.class)
                .intercept(EqualsMethod.isolated())
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testIncompatibleArgumentType() throws Exception {
        new ByteBuddy()
                .subclass(Object.class)
                .defineMethod(FOO, boolean.class)
                .withParameters(int.class)
                .intercept(EqualsMethod.isolated())
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testStaticMethod() throws Exception {
        new ByteBuddy()
                .subclass(Object.class)
                .defineMethod(FOO, boolean.class, Ownership.STATIC)
                .withParameters(Object.class)
                .intercept(EqualsMethod.isolated())
                .make();
    }

    public static class EqualsBase {

        @Override
        public boolean equals(Object other) {
            return true;
        }
    }

    public static class NonEqualsBase {

        @Override
        public boolean equals(Object other) {
            return false;
        }
    }
}
