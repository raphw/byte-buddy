package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.StubMethod;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

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
        Class<?> type = new ByteBuddy()
                .redefine(this.type)
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Class.forName(type.getName(), true, type.getClassLoader());
    }

    @Test
    public void testNoInitializerWithDisabledContext() throws Exception {
        Class<?> type = new ByteBuddy()
                .with(Implementation.Context.Disabled.Factory.INSTANCE)
                .redefine(this.type)
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Class.forName(type.getName(), true, type.getClassLoader());
    }

    @Test
    public void testInitializerWithEnabledContext() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(this.type)
                .invokable(isTypeInitializer()).intercept(StubMethod.INSTANCE)
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Class.forName(type.getName(), true, type.getClassLoader());
    }

    @Test
    public void testInitializerWithDisabledContext() throws Exception {
        Class<?> type = new ByteBuddy()
                .with(Implementation.Context.Disabled.Factory.INSTANCE)
                .redefine(this.type)
                .invokable(isTypeInitializer()).intercept(StubMethod.INSTANCE)
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Class.forName(type.getName(), true, type.getClassLoader());
    }

    public static class NoInitializer {
        /* empty */
    }

    public static class BranchingInitializer {

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
