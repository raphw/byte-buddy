package net.bytebuddy.asm;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.util.Arrays;
import java.util.Collection;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class AdviceFrameTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", COUNT = "count";

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {FrameAdvice.class, 2},
                {FrameAdviceWithoutThrowable.class, 2},
                {FrameAdviceWithSuppression.class, 2},
                {FrameAdviceEntryOnly.class, 1},
                {FrameAdviceEntryOnlyWithSuppression.class, 1},
                {FrameAdviceExitOnly.class, 1},
                {FrameAdviceExitOnlyWithSuppression.class, 1},
                {FrameAdviceExitOnlyWithSuppressionAndNonExceptionHandling.class, 1},
                {FrameReturnAdvice.class, 2}
        });
    }

    private final Class<?> advice;

    private final int count;

    public AdviceFrameTest(Class<?> advice, int count) {
        this.advice = advice;
        this.count = count;
    }

    @Test
    public void testFrameAdvice() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(FrameSample.class)
                .visit(Advice.to(advice).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO, String.class).invoke(type.getConstructor().newInstance(), FOO), is((Object) FOO));
        assertThat(type.getField(COUNT).getInt(null), is((Object) count));
    }

    @Test
    public void testFrameAdviceStaticMethod() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(FrameSample.class)
                .visit(Advice.to(advice).on(named(BAR)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(BAR, String.class).invoke(null, FOO), is((Object) FOO));
        assertThat(type.getField(COUNT).getInt(null), is((Object) count));
    }

    @Test
    public void testFrameAdviceExpanded() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(FrameSample.class)
                .visit(Advice.to(advice).on(named(FOO)).readerFlags(ClassReader.EXPAND_FRAMES))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO, String.class).invoke(type.getConstructor().newInstance(), FOO), is((Object) FOO));
        assertThat(type.getField(COUNT).getInt(null), is((Object) count));
    }

    @Test
    public void testFrameAdviceStaticMethodExpanded() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(FrameSample.class)
                .visit(Advice.to(advice).on(named(BAR)).readerFlags(ClassReader.EXPAND_FRAMES))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(BAR, String.class).invoke(null, FOO), is((Object) FOO));
        assertThat(type.getField(COUNT).getInt(null), is((Object) count));
    }

    @Test
    public void testFrameAdviceComputedMaxima() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(FrameSample.class)
                .visit(Advice.to(advice).on(named(FOO)).writerFlags(ClassWriter.COMPUTE_MAXS))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO, String.class).invoke(type.getConstructor().newInstance(), FOO), is((Object) FOO));
        assertThat(type.getField(COUNT).getInt(null), is((Object) count));
    }

    @Test
    public void testFrameAdviceStaticMethodComputedMaxima() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(FrameSample.class)
                .visit(Advice.to(advice).on(named(BAR)).writerFlags(ClassWriter.COMPUTE_MAXS))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(BAR, String.class).invoke(null, FOO), is((Object) FOO));
        assertThat(type.getField(COUNT).getInt(null), is((Object) count));
    }

    @Test
    public void testFrameAdviceComputedFrames() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(FrameSample.class)
                .visit(Advice.to(advice).on(named(FOO)).writerFlags(ClassWriter.COMPUTE_FRAMES))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO, String.class).invoke(type.getConstructor().newInstance(), FOO), is((Object) FOO));
        assertThat(type.getField(COUNT).getInt(null), is((Object) count));
    }

    @Test
    public void testFrameAdviceStaticMethodComputedFrames() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(FrameSample.class)
                .visit(Advice.to(advice).on(named(BAR)).writerFlags(ClassWriter.COMPUTE_FRAMES))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(BAR, String.class).invoke(null, FOO), is((Object) FOO));
        assertThat(type.getField(COUNT).getInt(null), is((Object) count));
    }

    @SuppressWarnings("all")
    public static class FrameSample {

        public static int count;

        public String foo(String value) {
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
            return value;
        }

        public static String bar(String value) {
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
            long v4 = 4L, v5 = 5L, v6 = 6L, v7 = 4L;
            if (ignored == 3) {
                throw new AssertionError();
            } else if (ignored == 4) {
                if (v4 + v5 + v6 + v7 == 0L) {
                    throw new AssertionError();
                }
            }
            try {
                long v8 = 1L;
            } catch (Exception exception) {
                long v9 = 1L;
            }
            return value;
        }
    }

    @SuppressWarnings("unused")
    public static class FrameAdvice {

        @Advice.OnMethodEnter
        @Advice.OnMethodExit(onThrowable = Exception.class)
        private static String advice(@Advice.Ignored int ignored, @Advice.Argument(0) String value) {
            int v0 = 1;
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
            long v4 = 1L, v5 = 2L, v6 = 3L, v7 = 4L;
            if (ignored == 3) {
                throw new AssertionError();
            } else if (ignored == 4) {
                if (v4 + v5 + v6 + v7 == 0L) {
                    throw new AssertionError();
                }
            }
            try {
                long v8 = 1L;
            } catch (Exception exception) {
                long v9 = 1L;
            }
            FrameSample.count++;
            return value;
        }
    }

    @SuppressWarnings("unused")
    public static class FrameAdviceWithoutThrowable {

        @Advice.OnMethodEnter
        @Advice.OnMethodExit
        private static String advice(@Advice.Ignored int ignored, @Advice.Argument(0) String value) {
            int v0 = 1;
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
            long v4 = 1L, v5 = 2L, v6 = 3L, v7 = 4L;
            if (ignored == 3) {
                throw new AssertionError();
            } else if (ignored == 4) {
                if (v4 + v5 + v6 + v7 == 0L) {
                    throw new AssertionError();
                }
            }
            try {
                long v8 = 1L;
            } catch (Exception exception) {
                long v9 = 1L;
            }
            FrameSample.count++;
            return value;
        }
    }

    @SuppressWarnings("unused")
    public static class FrameAdviceWithSuppression {

        @Advice.OnMethodEnter(suppress = Exception.class)
        @Advice.OnMethodExit(suppress = Exception.class, onThrowable = Exception.class)
        private static String advice(@Advice.Ignored int ignored, @Advice.Argument(0) String value) {
            int v0 = 1;
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
            long v4 = 1L, v5 = 2L, v6 = 3L, v7 = 4L;
            if (ignored == 3) {
                throw new AssertionError();
            } else if (ignored == 4) {
                if (v4 + v5 + v6 + v7 == 0L) {
                    throw new AssertionError();
                }
            }
            try {
                long v8 = 1L;
            } catch (Exception exception) {
                long v9 = 1L;
            }
            FrameSample.count++;
            return value;
        }
    }

    @SuppressWarnings("unused")
    public static class FrameAdviceEntryOnly {

        @Advice.OnMethodEnter
        private static String advice(@Advice.Ignored int ignored, @Advice.Argument(0) String value) {
            int v0 = 1;
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
            long v4 = 1L, v5 = 2L, v6 = 3L, v7 = 4L;
            if (ignored == 3) {
                throw new AssertionError();
            } else if (ignored == 4) {
                if (v4 + v5 + v6 + v7 == 0L) {
                    throw new AssertionError();
                }
            }
            try {
                long v8 = 1L;
            } catch (Exception exception) {
                long v9 = 1L;
            }
            FrameSample.count++;
            return value;
        }
    }

    @SuppressWarnings("unused")
    public static class FrameAdviceEntryOnlyWithSuppression {

        @Advice.OnMethodEnter(suppress = Exception.class)
        private static String advice(@Advice.Ignored int ignored, @Advice.Argument(0) String value) {
            int v0 = 1;
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
            long v4 = 1L, v5 = 2L, v6 = 3L, v7 = 4L;
            if (ignored == 3) {
                throw new AssertionError();
            } else if (ignored == 4) {
                if (v4 + v5 + v6 + v7 == 0L) {
                    throw new AssertionError();
                }
            }
            try {
                long v8 = 1L;
            } catch (Exception exception) {
                long v9 = 1L;
            }
            FrameSample.count++;
            return value;
        }
    }

    @SuppressWarnings("unused")
    public static class FrameAdviceExitOnly {

        @Advice.OnMethodExit(onThrowable = Exception.class)
        private static String advice(@Advice.Ignored int ignored, @Advice.Argument(0) String value) {
            int v0 = 1;
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
            long v4 = 1L, v5 = 2L, v6 = 3L, v7 = 4L;
            if (ignored == 3) {
                throw new AssertionError();
            } else if (ignored == 4) {
                if (v4 + v5 + v6 + v7 == 0L) {
                    throw new AssertionError();
                }
            }
            try {
                long v8 = 1L;
            } catch (Exception exception) {
                long v9 = 1L;
            }
            FrameSample.count++;
            return value;
        }
    }

    @SuppressWarnings("unused")
    public static class FrameAdviceExitOnlyWithSuppression {

        @Advice.OnMethodExit(suppress = Exception.class, onThrowable = Exception.class)
        private static String advice(@Advice.Ignored int ignored, @Advice.Argument(0) String value) {
            int v0 = 1;
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
            long v4 = 1L, v5 = 2L, v6 = 3L, v7 = 4L;
            if (ignored == 3) {
                throw new AssertionError();
            } else if (ignored == 4) {
                if (v4 + v5 + v6 + v7 == 0L) {
                    throw new AssertionError();
                }
            }
            try {
                long v8 = 1L;
            } catch (Exception exception) {
                long v9 = 1L;
            }
            FrameSample.count++;
            return value;
        }
    }

    @SuppressWarnings("unused")
    public static class FrameAdviceExitOnlyWithSuppressionAndNonExceptionHandling {

        @Advice.OnMethodExit(suppress = Exception.class, onThrowable = Exception.class)
        private static String advice(@Advice.Ignored int ignored, @Advice.Argument(0) String value) {
            int v0 = 1;
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
            long v4 = 1L, v5 = 2L, v6 = 3L, v7 = 4L;
            if (ignored == 3) {
                throw new AssertionError();
            } else if (ignored == 4) {
                if (v4 + v5 + v6 + v7 == 0L) {
                    throw new AssertionError();
                }
            }
            try {
                long v8 = 1L;
            } catch (Exception exception) {
                long v9 = 1L;
            }
            FrameSample.count++;
            return value;
        }
    }

    @SuppressWarnings("all")
    public static class FrameReturnAdvice {

        @Advice.OnMethodEnter(suppress = RuntimeException.class)
        @Advice.OnMethodExit(suppress = RuntimeException.class)
        private static String advice() {
            try {
                int ignored = 0;
                if (ignored != 0) {
                    return BAR;
                }
            } catch (Exception e) {
                int ignored = 0;
                if (ignored != 0) {
                    return QUX;
                }
            }
            FrameSample.count++;
            return FOO;
        }
    }
}
