package net.bytebuddy.implementation;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Ownership;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.junit.Test;

import static net.bytebuddy.matcher.ElementMatchers.isToString;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class ToStringMethodOtherTest {

    private static final String FOO = "foo", BAR = "bar";

    @Test
    public void testFullyQualifiedPrefix() throws Exception {
        DynamicType.Loaded<?> loaded = new ByteBuddy()
                .subclass(Object.class)
                .defineField(FOO, Object.class, Visibility.PUBLIC)
                .method(isToString())
                .intercept(ToStringMethod.prefixedByFullyQualifiedClassName())
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(1));
        Object instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        instance.getClass().getDeclaredField(FOO).set(instance, FOO);
        assertThat(instance.toString(), startsWith(instance.getClass().getName()));
    }

    @Test
    public void testCanonicalPrefix() throws Exception {
        DynamicType.Loaded<?> loaded = new ByteBuddy()
                .subclass(Object.class)
                .defineField(FOO, Object.class, Visibility.PUBLIC)
                .method(isToString())
                .intercept(ToStringMethod.prefixedByCanonicalClassName())
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(1));
        Object instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        instance.getClass().getDeclaredField(FOO).set(instance, FOO);
        assertThat(instance.toString(), startsWith(instance.getClass().getCanonicalName()));
    }

    @Test
    public void testSimplePrefix() throws Exception {
        DynamicType.Loaded<?> loaded = new ByteBuddy()
                .subclass(Object.class)
                .defineField(FOO, Object.class, Visibility.PUBLIC)
                .method(isToString())
                .intercept(ToStringMethod.prefixedBySimpleClassName())
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(1));
        Object instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        instance.getClass().getDeclaredField(FOO).set(instance, FOO);
        assertThat(instance.toString(), startsWith(instance.getClass().getSimpleName()));
    }

    @Test
    public void testIgnoredField() throws Exception {
        DynamicType.Loaded<?> loaded = new ByteBuddy()
                .subclass(Object.class)
                .defineField(FOO, Object.class, Visibility.PUBLIC)
                .method(isToString())
                .intercept(ToStringMethod.prefixedBy(FOO).withIgnoredFields(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(1));
        Object instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        instance.getClass().getDeclaredField(FOO).set(instance, FOO);
        assertThat(instance.toString(), is("foo{}"));
    }

    @Test
    public void testTokens() throws Exception {
        DynamicType.Loaded<?> loaded = new ByteBuddy()
                .subclass(Object.class)
                .defineField(FOO, Object.class, Visibility.PUBLIC)
                .defineField(BAR, Object.class, Visibility.PUBLIC)
                .method(isToString())
                .intercept(ToStringMethod.prefixedBy(FOO).withTokens("a", "b", "c", "d"))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER);
        assertThat(loaded.getLoadedAuxiliaryTypes().size(), is(0));
        assertThat(loaded.getLoaded().getDeclaredMethods().length, is(1));
        assertThat(loaded.getLoaded().getDeclaredFields().length, is(2));
        Object instance = loaded.getLoaded().getDeclaredConstructor().newInstance();
        instance.getClass().getDeclaredField(FOO).set(instance, FOO);
        instance.getClass().getDeclaredField(BAR).set(instance, BAR);
        assertThat(instance.toString(), is(FOO + "a" + FOO + "d" + FOO + "c" + BAR + "d" + BAR + "b"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullPrefix() {
        ToStringMethod.prefixedBy((String) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTokenStartNull() {
        ToStringMethod.prefixedBy(FOO).withTokens(null, "", "", "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTokenEndNull() {
        ToStringMethod.prefixedBy(FOO).withTokens("", null, "", "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTokenSeparatorNull() {
        ToStringMethod.prefixedBy(FOO).withTokens("", "", null, "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTokenDefinerNull() {
        ToStringMethod.prefixedBy(FOO).withTokens("", "", "", null);
    }

    @Test(expected = IllegalStateException.class)
    public void testNullPrefixResolved() {
        new ByteBuddy()
                .makeInterface()
                .method(isToString())
                .intercept(ToStringMethod.prefixedBy(mock(ToStringMethod.PrefixResolver.class)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testInterface() throws Exception {
        new ByteBuddy()
                .makeInterface()
                .method(isToString())
                .intercept(ToStringMethod.prefixedBy(FOO))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testIncompatibleReturn() throws Exception {
        new ByteBuddy()
                .subclass(Object.class)
                .defineMethod(FOO, Void.class)
                .intercept(ToStringMethod.prefixedBy(FOO))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testStaticMethod() throws Exception {
        new ByteBuddy()
                .subclass(Object.class)
                .defineMethod(FOO, String.class, Ownership.STATIC)
                .intercept(ToStringMethod.prefixedBy(FOO))
                .make();
    }
}
