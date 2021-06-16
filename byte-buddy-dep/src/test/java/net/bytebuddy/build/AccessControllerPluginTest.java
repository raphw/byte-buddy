package net.bytebuddy.build;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.PrivilegedAction;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class AccessControllerPluginTest {

    private static final String FOO = "foo", BAR = "bar";

    @Test
    public void testPluginEnhanceNoProperty() throws Exception {
        Class<?> type = new AccessControllerPlugin()
                .apply(new ByteBuddy().redefine(SimpleSample.class),
                        TypeDescription.ForLoadedType.of(SimpleSample.class),
                        ClassFileLocator.ForClassLoader.of(SimpleSample.class.getClassLoader()))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Field field = type.getDeclaredField("ACCESS_CONTROLLER");
        field.setAccessible(true);
        assertThat(field.getBoolean(null), is(true));
        Method method = type.getDeclaredMethod("doPrivileged", PrivilegedAction.class);
        method.setAccessible(true);
        assertThat(method.invoke(null, new TrivialPrivilegedAction(FOO)), is((Object) FOO));
    }

    @Test
    public void testPluginEnhanceProperty() throws Exception {
        Class<?> type = new AccessControllerPlugin(AccessControllerPluginTest.class.getName())
                .apply(new ByteBuddy().redefine(SimpleSample.class),
                        TypeDescription.ForLoadedType.of(SimpleSample.class),
                        ClassFileLocator.ForClassLoader.of(SimpleSample.class.getClassLoader()))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Field field = type.getDeclaredField("ACCESS_CONTROLLER");
        field.setAccessible(true);
        assertThat(field.getBoolean(null), is(true));
        Method method = type.getDeclaredMethod("doPrivileged", PrivilegedAction.class);
        method.setAccessible(true);
        assertThat(method.invoke(null, new TrivialPrivilegedAction(FOO)), is((Object) FOO));
    }

    @Test
    public void testPluginEnhancePropertyDisabled() throws Exception {
        System.setProperty(AccessControllerPluginTest.class.getName() + ".disabled", "false");
        Class<?> type = new AccessControllerPlugin(AccessControllerPluginTest.class.getName() + ".disabled")
                .apply(new ByteBuddy().redefine(SimpleSample.class),
                        TypeDescription.ForLoadedType.of(SimpleSample.class),
                        ClassFileLocator.ForClassLoader.of(SimpleSample.class.getClassLoader()))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Field field = type.getDeclaredField("ACCESS_CONTROLLER");
        field.setAccessible(true);
        assertThat(field.getBoolean(null), is(false));
        Method method = type.getDeclaredMethod("doPrivileged", PrivilegedAction.class);
        method.setAccessible(true);
        assertThat(method.invoke(null, new TrivialPrivilegedAction(FOO)), is((Object) BAR));
    }

    @Test
    public void testPluginConflictingField() throws Exception {
        Class<?> type = new AccessControllerPlugin()
                .apply(new ByteBuddy().redefine(ConflictingFieldSample.class),
                        TypeDescription.ForLoadedType.of(ConflictingFieldSample.class),
                        ClassFileLocator.ForClassLoader.of(ConflictingFieldSample.class.getClassLoader()))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Field field = type.getDeclaredField("ACCESS_CONTROLLER$");
        field.setAccessible(true);
        assertThat(field.getBoolean(null), is(true));
        Method method = type.getDeclaredMethod("doPrivileged", PrivilegedAction.class);
        method.setAccessible(true);
        assertThat(method.invoke(null, new TrivialPrivilegedAction(FOO)), is((Object) FOO));
    }

    @Test
    public void testPluginMappedType() throws Exception {
        Class<?> type = new AccessControllerPlugin()
                .apply(new ByteBuddy().redefine(MappingSample.class),
                        TypeDescription.ForLoadedType.of(MappingSample.class),
                        ClassFileLocator.ForClassLoader.of(MappingSample.class.getClassLoader()))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Field field = type.getDeclaredField("ACCESS_CONTROLLER");
        field.setAccessible(true);
        assertThat(field.getBoolean(null), is(true));
        Method context = type.getDeclaredMethod("getContext");
        context.setAccessible(true);
        Method method = type.getDeclaredMethod("doPrivileged", PrivilegedAction.class, Object.class);
        method.setAccessible(true);
        assertThat(method.invoke(null, new TrivialPrivilegedAction(FOO), context.invoke(null)), is((Object) FOO));
    }

    @Test(expected = IllegalStateException.class)
    public void testTargetMethodIsPublic() {
        new AccessControllerPlugin()
                .apply(new ByteBuddy().redefine(PublicMethodSample.class),
                        TypeDescription.ForLoadedType.of(PublicMethodSample.class),
                        ClassFileLocator.ForClassLoader.of(PublicMethodSample.class.getClassLoader()))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testUnknownMethod() {
        new AccessControllerPlugin()
                .apply(new ByteBuddy().redefine(UnknownMethodSample.class),
                        TypeDescription.ForLoadedType.of(UnknownMethodSample.class),
                        ClassFileLocator.ForClassLoader.of(UnknownMethodSample.class.getClassLoader()))
                .make();
    }

    public static class SimpleSample {

        @SuppressWarnings("unused")
        @AccessControllerPlugin.Enhance
        static Object doPrivileged(PrivilegedAction<?> action) {
            return BAR;
        }
    }

    @SuppressWarnings("unused")
    public static class MappingSample {

        static Object getContext() {
            return null;
        }

        @AccessControllerPlugin.Enhance
        static Object doPrivileged(PrivilegedAction<?> action, Object context) {
            return BAR;
        }
    }

    public static class PublicMethodSample {

        @SuppressWarnings("unused")
        @AccessControllerPlugin.Enhance
        public static Object doPrivileged(PrivilegedAction<?> action) {
            return BAR;
        }
    }

    public static class UnknownMethodSample {

        @SuppressWarnings("unused")
        @AccessControllerPlugin.Enhance
        public static Object doPrivileged() {
            return BAR;
        }
    }

    @SuppressWarnings("unused")
    public static class ConflictingFieldSample {

        private static final boolean ACCESS_CONTROLLER = false;

        @AccessControllerPlugin.Enhance
        static Object doPrivileged(PrivilegedAction<?> action) {
            return BAR;
        }
    }

    private static class TrivialPrivilegedAction implements PrivilegedAction<String> {

        private final String value;

        private TrivialPrivilegedAction(String value) {
            this.value = value;
        }

        public String run() {
            return value;
        }
    }
}
