package net.bytebuddy.build;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.PrivilegedAction;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

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
        assertThat(field.getBoolean(null), equalTo(true));
        Method method = type.getDeclaredMethod("doPrivileged", PrivilegedAction.class);
        method.setAccessible(true);
        assertThat(method.invoke(null, new TrivialPrivilegedAction(FOO)), equalTo((Object) FOO));
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
        assertThat(field.getBoolean(null), equalTo(true));
        Method method = type.getDeclaredMethod("doPrivileged", PrivilegedAction.class);
        method.setAccessible(true);
        assertThat(method.invoke(null, new TrivialPrivilegedAction(FOO)), equalTo((Object) FOO));
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
        assertThat(field.getBoolean(null), equalTo(false));
        Method method = type.getDeclaredMethod("doPrivileged", PrivilegedAction.class);
        method.setAccessible(true);
        assertThat(method.invoke(null, new TrivialPrivilegedAction(FOO)), equalTo((Object) BAR));
    }

    public static class SimpleSample {

        @SuppressWarnings("unused")
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
