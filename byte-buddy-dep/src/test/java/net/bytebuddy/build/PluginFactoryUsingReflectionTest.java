package net.bytebuddy.build;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class PluginFactoryUsingReflectionTest {

    private static final String FOO = "foo";

    @Test
    public void testDefaultConstructor() {
        assertThat(new Plugin.Factory.UsingReflection(SimplePlugin.class).make(), instanceOf(SimplePlugin.class));
    }

    @Test
    public void testDefaultConstructorIgnoreArgument() {
        assertThat(new Plugin.Factory.UsingReflection(SimplePluginTwoConstructors.class).make(), instanceOf(SimplePluginTwoConstructors.class));
    }

    @Test
    public void testArgumentConstructor() {
        assertThat(new Plugin.Factory.UsingReflection(SimplePluginArgumentConstructor.class)
                .with(Plugin.Factory.UsingReflection.ArgumentResolver.ForType.of(Void.class, null))
                .make(), instanceOf(SimplePluginArgumentConstructor.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testArgumentConstructorNoResolver() {
        new Plugin.Factory.UsingReflection(SimplePluginArgumentConstructor.class).make();
    }

    @Test(expected = IllegalStateException.class)
    public void testConstructorAmbiguous() {
        new Plugin.Factory.UsingReflection(SimplePluginTwoConstructors.class)
                .with(Plugin.Factory.UsingReflection.ArgumentResolver.ForType.of(Void.class, null))
                .make();
    }

    @Test
    public void testArgumentConstructorPrirorityLeft() {
        assertThat(new Plugin.Factory.UsingReflection(SimplePluginPreferredConstructorLeft.class)
                .with(Plugin.Factory.UsingReflection.ArgumentResolver.ForType.of(Void.class, null))
                .make(), instanceOf(SimplePluginPreferredConstructorLeft.class));
    }

    @Test
    public void testArgumentConstructorPrirorityRight() {
        assertThat(new Plugin.Factory.UsingReflection(SimplePluginPreferredConstructorRight.class)
                .with(Plugin.Factory.UsingReflection.ArgumentResolver.ForType.of(Void.class, null))
                .make(), instanceOf(SimplePluginPreferredConstructorRight.class));
    }

    @Test
    public void testArgumentResolverType() {
        Plugin.Factory.UsingReflection.ArgumentResolver argumentResolver = Plugin.Factory.UsingReflection.ArgumentResolver.ForType.of(Void.class, null);
        Plugin.Factory.UsingReflection.ArgumentResolver.Resolution resolution = argumentResolver.resolve(-1, Void.class);
        assertThat(resolution.isResolved(), is(true));
        assertThat(resolution.getArgument(), nullValue());
        assertThat(argumentResolver.resolve(-1, Object.class).isResolved(), is(false));
    }

    @Test
    public void testArgumentResolverIndex() {
        Plugin.Factory.UsingReflection.ArgumentResolver argumentResolver = new Plugin.Factory.UsingReflection.ArgumentResolver.ForIndex(0, null);
        Plugin.Factory.UsingReflection.ArgumentResolver.Resolution resolution = argumentResolver.resolve(0, Object.class);
        assertThat(resolution.isResolved(), is(true));
        assertThat(resolution.getArgument(), nullValue());
        assertThat(argumentResolver.resolve(-1, Object.class).isResolved(), is(false));
    }

    @Test
    public void testArgumentResolverIndexWithDynamicTypeNotString() {
        Plugin.Factory.UsingReflection.ArgumentResolver argumentResolver = new Plugin.Factory.UsingReflection.ArgumentResolver.ForIndex.WithDynamicType(0, "42");
        Plugin.Factory.UsingReflection.ArgumentResolver.Resolution resolution = argumentResolver.resolve(0, Long.class);
        assertThat(resolution.isResolved(), is(true));
        assertThat(resolution.getArgument(), is((Object) 42L));
        assertThat(argumentResolver.resolve(-1, Long.class).isResolved(), is(false));
        assertThat(argumentResolver.resolve(0, Void.class).isResolved(), is(false));
    }

    @Test
    public void testArgumentResolverIndexWithDynamicTypeString() {
        Plugin.Factory.UsingReflection.ArgumentResolver argumentResolver = new Plugin.Factory.UsingReflection.ArgumentResolver.ForIndex.WithDynamicType(0, FOO);
        Plugin.Factory.UsingReflection.ArgumentResolver.Resolution resolution = argumentResolver.resolve(0, String.class);
        assertThat(resolution.isResolved(), is(true));
        assertThat(resolution.getArgument(), is((Object) FOO));
        assertThat(argumentResolver.resolve(-1, String.class).isResolved(), is(false));
        assertThat(argumentResolver.resolve(0, Void.class).isResolved(), is(false));
    }

    @Test(expected = IllegalStateException.class)
    public void testArgumentResolverUnresolvedResolution() {
        Plugin.Factory.UsingReflection.ArgumentResolver.Resolution.Unresolved.INSTANCE.getArgument();
    }

    public static class SimplePlugin implements Plugin {

        public boolean matches(TypeDescription target) {
            throw new AssertionError();
        }

        public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassFileLocator classFileLocator) {
            throw new AssertionError();
        }

        public void close() {
            throw new AssertionError();
        }
    }

    public static class SimplePluginArgumentConstructor implements Plugin {

        public SimplePluginArgumentConstructor(Void unused) {
            /* empty */
        }

        public boolean matches(TypeDescription target) {
            throw new AssertionError();
        }

        public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassFileLocator classFileLocator) {
            throw new AssertionError();
        }

        public void close() {
            throw new AssertionError();
        }
    }

    public static class SimplePluginTwoConstructors implements Plugin {

        public SimplePluginTwoConstructors() {
            /* empty */
        }

        public SimplePluginTwoConstructors(Void unused) {
            /* empty */
        }

        public boolean matches(TypeDescription target) {
            throw new AssertionError();
        }

        public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassFileLocator classFileLocator) {
            throw new AssertionError();
        }

        public void close() {
            throw new AssertionError();
        }
    }

    public static class SimplePluginPreferredConstructorLeft implements Plugin {

        @Factory.UsingReflection.Priority(1)
        public SimplePluginPreferredConstructorLeft() {
            /* empty */
        }

        public SimplePluginPreferredConstructorLeft(Void unused) {
            /* empty */
        }

        public boolean matches(TypeDescription target) {
            throw new AssertionError();
        }

        public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassFileLocator classFileLocator) {
            throw new AssertionError();
        }

        public void close() {
            throw new AssertionError();
        }
    }

    public static class SimplePluginPreferredConstructorRight implements Plugin {

        public SimplePluginPreferredConstructorRight() {
            /* empty */
        }

        @Factory.UsingReflection.Priority(1)
        public SimplePluginPreferredConstructorRight(Void unused) {
            /* empty */
        }

        public boolean matches(TypeDescription target) {
            throw new AssertionError();
        }

        public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassFileLocator classFileLocator) {
            throw new AssertionError();
        }

        public void close() {
            throw new AssertionError();
        }
    }
}
