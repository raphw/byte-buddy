package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.StubMethod;
import net.bytebuddy.test.utility.DebuggingWrapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collection;

import static net.bytebuddy.matcher.ElementMatchers.isTypeInitializer;

@RunWith(Parameterized.class)
public class TypeWriterInitializerRemapperTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {NoInitializer.class},
                {BranchingInitializer.class},
        });
    }

    private final Class<?> type;

    public TypeWriterInitializerRemapperTest(Class<?> type) {
        this.type = type;
    }

    @Test
    public void testNoInitializerWithEnabledContext() throws Exception {
        ClassLoader classLoader = new URLClassLoader(new URL[0], ClassLoadingStrategy.BOOTSTRAP_LOADER);
        Class.forName(new ByteBuddy()
                .redefine(type)
                .make()
                .load(classLoader)
                .getLoaded().getName(), true, classLoader);
    }

    @Test
    public void testNoInitializerWithDisabledContext() throws Exception {
        ClassLoader classLoader = new URLClassLoader(new URL[0], ClassLoadingStrategy.BOOTSTRAP_LOADER);
        Class.forName(new ByteBuddy()
                .with(Implementation.Context.Disabled.Factory.INSTANCE)
                .redefine(type)
                .make()
                .load(classLoader)
                .getLoaded().getName(), true, classLoader);
    }

    @Test
    public void testInitializerWithEnabledContext() throws Exception {
        ClassLoader classLoader = new URLClassLoader(new URL[0], ClassLoadingStrategy.BOOTSTRAP_LOADER);
        Class.forName(new ByteBuddy()
                .redefine(type)
                .invokable(isTypeInitializer()).intercept(StubMethod.INSTANCE)
                .make()
                .load(classLoader)
                .getLoaded().getName(), true, classLoader);
    }

    @Test
    public void testInitializerWithDisabledContext() throws Exception {
        ClassLoader classLoader = new URLClassLoader(new URL[0], ClassLoadingStrategy.BOOTSTRAP_LOADER);
        Class.forName(new ByteBuddy()
                .with(Implementation.Context.Disabled.Factory.INSTANCE)
                .redefine(type)
                .invokable(isTypeInitializer()).intercept(StubMethod.INSTANCE)
                .make()
                .load(classLoader)
                .getLoaded().getName(), true, classLoader);
    }

    private static class NoInitializer {
        /* empty */
    }

    private static class BranchingInitializer {

        static {
            int ignored = 0;
            {
                long v1 = 1L, v2 = 2L, v3 = 3L;
                if (ignored == 1) {
                    throw new AssertionError();
                } else if (ignored == 2) {
                    if (v1 + v2 + v3 == 0L) {
                        throw new AssertionError();
                    }
                }
            }
            long v4 = 4L, v5 = 5L, v6 = 6L, v7 = 7L;
            if (ignored == 3) {
                throw new AssertionError();
            } else if (ignored == 4) {
                if (v4 + v5 + v6 + v7 == 0L) {
                    throw new AssertionError();
                }
            }
            try {
                long v8 = 8L;
            } catch (Exception exception) {
                long v9 = 9L;
            }
        }
    }
}
