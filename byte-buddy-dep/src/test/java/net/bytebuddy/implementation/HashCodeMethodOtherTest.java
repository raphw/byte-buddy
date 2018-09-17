package net.bytebuddy.implementation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Ownership;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.junit.Test;

import static net.bytebuddy.matcher.ElementMatchers.isHashCode;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class HashCodeMethodOtherTest {

    private static final String FOO = "foo";

    @Test(expected = NullPointerException.class)
    public void testNullableField() throws Exception {
        new ByteBuddy()
                .subclass(Object.class)
                .defineField(FOO, Object.class, Visibility.PUBLIC)
                .method(isHashCode())
                .intercept(HashCodeMethod.usingDefaultOffset().withNonNullableFields(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .getDeclaredConstructor()
                .newInstance()
                .hashCode();
    }

    @Test
    public void testIgnoredField() throws Exception {
        DynamicType.Loaded<?> loaded = new ByteBuddy()
                .subclass(Object.class)
                .defineField(FOO, Object.class, Visibility.PUBLIC)
                .method(isHashCode())
                .intercept(HashCodeMethod.usingOffset(0).withIgnoredFields(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(1));
        Object instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        instance.getClass().getDeclaredField(FOO).set(instance, FOO);
        assertThat(instance.hashCode(), is(0));
    }

    @Test
    public void testSuperMethod() throws Exception {
        DynamicType.Loaded<?> loaded = new ByteBuddy()
                .subclass(HashCodeBase.class)
                .defineField(FOO, Object.class, Visibility.PUBLIC)
                .method(isHashCode())
                .intercept(HashCodeMethod.usingSuperClassOffset())
                .make()
                .load(HashCodeBase.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(1));
        Object instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        instance.getClass().getDeclaredField(FOO).set(instance, FOO);
        assertThat(instance.hashCode(), is(42 * 31 + FOO.hashCode()));
    }

    @Test
    public void testMultiplier() throws Exception {
        DynamicType.Loaded<?> loaded = new ByteBuddy()
                .subclass(Object.class)
                .defineField(FOO, Object.class, Visibility.PUBLIC)
                .method(isHashCode())
                .intercept(HashCodeMethod.usingOffset(0).withMultiplier(1))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(1));
        Object instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        instance.getClass().getDeclaredField(FOO).set(instance, FOO);
        assertThat(instance.hashCode(), is(101574));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testZeroMultiplier() {
        HashCodeMethod.usingDefaultOffset().withMultiplier(0);
    }

    @Test(expected = IllegalStateException.class)
    public void testInterface() throws Exception {
        new ByteBuddy()
                .makeInterface()
                .method(isHashCode())
                .intercept(HashCodeMethod.usingDefaultOffset())
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testIncompatibleReturn() throws Exception {
        new ByteBuddy()
                .subclass(Object.class)
                .defineMethod(FOO, Object.class)
                .intercept(HashCodeMethod.usingDefaultOffset())
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testStaticMethod() throws Exception {
        new ByteBuddy()
                .subclass(Object.class)
                .defineMethod(FOO, int.class, Ownership.STATIC)
                .intercept(HashCodeMethod.usingDefaultOffset())
                .make();
    }

    public static class HashCodeBase {

        public int hashCode() {
            return 42;
        }
    }
}
