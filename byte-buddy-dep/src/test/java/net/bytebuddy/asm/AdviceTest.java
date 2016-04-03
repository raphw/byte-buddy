package net.bytebuddy.asm;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.test.utility.DebuggingWrapper;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;
import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Iterator;

import static junit.framework.TestCase.fail;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AdviceTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";

    private static final String ENTER = "enter", EXIT = "exit", INSIDE = "inside", THROWABLE = "throwable", COUNT = "count";

    private static final int VALUE = 42;

    @Test
    public void testTrivialAdvice() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(TrivialAdvice.class).on(named(FOO)))
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testAdviceWithImplicitArgument() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(ArgumentAdvice.class).on(named(BAR)))
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(BAR, String.class).invoke(type.newInstance(), BAR), is((Object) BAR));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testAdviceWithExplicitArgument() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(ArgumentAdviceExplicit.class).on(named(QUX)))
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(QUX, String.class, String.class).invoke(type.newInstance(), FOO, BAR), is((Object) (FOO + BAR)));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testAdviceWithThisReference() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(ThisReferenceAdvice.class).on(named(FOO)))
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testAdviceWithEntranceValue() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(EntranceValueAdvice.class).on(named(FOO)))
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testAdviceWithReturnValue() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(ReturnValueAdvice.class).on(named(FOO)))
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 0));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testAdviceNotSkipExceptionImplicit() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(TrivialAdvice.class).on(named(FOO + BAR)))
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        try {
            type.getDeclaredMethod(FOO + BAR).invoke(type.newInstance());
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getCause(), instanceOf(RuntimeException.class));
        }
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testAdviceSkipExceptionImplicit() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(TrivialAdviceSkipException.class).on(named(FOO + BAR)))
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        try {
            type.getDeclaredMethod(FOO + BAR).invoke(type.newInstance());
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getCause(), instanceOf(RuntimeException.class));
        }
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 0));
    }

    @Test
    public void testAdviceNotSkipExceptionExplicit() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(TrivialAdvice.class).on(named(BAR + BAZ)))
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        try {
            type.getDeclaredMethod(BAR + BAZ).invoke(type.newInstance());
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getCause(), instanceOf(NullPointerException.class));
        }
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testAdviceSkipExceptionExplicit() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(TrivialAdviceSkipException.class).on(named(BAR + BAZ)))
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        try {
            type.getDeclaredMethod(BAR + BAZ).invoke(type.newInstance());
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getCause(), instanceOf(NullPointerException.class));
        }
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 0));
    }

    @Test
    public void testAdviceSkipExceptionDoesNotSkipNonException() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(TrivialAdviceSkipException.class).on(named(FOO)))
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testObsoleteReturnValue() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(ObsoleteReturnValueAdvice.class).on(named(FOO)))
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 0));
    }

    @Test
    public void testUnusedReturnValue() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(UnusedReturnValueAdvice.class).on(named(FOO)))
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testVariableMappingAdviceLarger() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(AdviceWithVariableValues.class).on(named(BAR)))
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(BAR, String.class).invoke(type.newInstance(), FOO + BAR + QUX + BAZ), is((Object) (FOO + BAR + QUX + BAZ)));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testVariableMappingInstrumentedLarger() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(AdviceWithVariableValues.class).on(named(QUX + BAZ)))
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(QUX + BAZ).invoke(type.newInstance()), is((Object) (FOO + BAR + QUX + BAZ)));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testExceptionWhenNotThrown() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(ThrowableAdvice.class).on(named(FOO)))
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.newInstance()), is((Object) (FOO)));
        assertThat(type.getDeclaredField(THROWABLE).get(null), nullValue(Object.class));
    }

    @Test
    public void testExceptionWhenThrown() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(ThrowableAdvice.class).on(named(FOO + BAR)))
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        try {
            type.getDeclaredMethod(FOO + BAR).invoke(type.newInstance());
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getCause(), instanceOf(RuntimeException.class));
        }
        assertThat(type.getDeclaredField(THROWABLE).get(null), instanceOf(RuntimeException.class));
    }

    @Test
    public void testAdviceThrowOnEnter() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(TracableSample.class)
                .visit(Advice.to(ThrowOnEnter.class).on(named(FOO)))
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        try {
            type.getDeclaredMethod(FOO).invoke(type.newInstance());
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getCause(), instanceOf(RuntimeException.class));
        }
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(INSIDE).get(null), is((Object) 0));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 0));
    }

    @Test
    public void testAdviceThrowOnExit() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(TracableSample.class)
                .visit(Advice.to(ThrowOnExit.class).on(named(FOO)))
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        try {
            type.getDeclaredMethod(FOO).invoke(type.newInstance());
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getCause(), instanceOf(RuntimeException.class));
        }
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(INSIDE).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testAdviceThrowSuppressed() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(TracableSample.class)
                .visit(Advice.to(ThrowSuppressed.class).on(named(FOO)))
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        type.getDeclaredMethod(FOO).invoke(type.newInstance());
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(INSIDE).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testAdviceThrowNotSuppressedOnEnter() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(TracableSample.class)
                .visit(Advice.to(ThrowNotSuppressedOnEnter.class).on(named(FOO)))
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        try {
            type.getDeclaredMethod(FOO).invoke(type.newInstance());
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getCause(), instanceOf(Exception.class));
        }
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(INSIDE).get(null), is((Object) 0));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 0));
    }

    @Test
    public void testAdviceThrowNotSuppressedOnExit() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(TracableSample.class)
                .visit(Advice.to(ThrowNotSuppressedOnExit.class).on(named(FOO)))
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        try {
            type.getDeclaredMethod(FOO).invoke(type.newInstance());
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getCause(), instanceOf(Exception.class));
        }
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(INSIDE).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testThisValueSubstitution() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Box.class)
                .visit(Advice.to(ThisSubstitutionAdvice.class).on(named(FOO)))
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor(String.class).newInstance(FOO)), is((Object) BAR));
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalThisValueSubstitution() throws Exception {
        new ByteBuddy()
                .redefine(Box.class)
                .visit(Advice.to(IllegalThisSubstitutionAdvice.class).on(named(FOO)))
                .make();
    }

    @Test
    public void testParameterValueSubstitution() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Box.class)
                .visit(Advice.to(ParameterSubstitutionAdvice.class).on(named(BAR)))
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(BAR, String.class).invoke(null, FOO), is((Object) BAR));
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalParameterValueSubstitution() throws Exception {
        new ByteBuddy()
                .redefine(Box.class)
                .visit(Advice.to(IllegalParameterSubstitutionAdvice.class).on(named(BAR)))
                .make();
    }

    @Test
    public void testReturnValueSubstitution() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(ReturnSubstitutionAdvice.class).on(named(BAR)))
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(BAR, String.class).invoke(type.newInstance(), FOO), is((Object) BAR));
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalReturnValueSubstitution() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(IllegalReturnSubstitutionAdvice.class).on(named(FOO)))
                .make();
    }

    @Test
    public void testEnterValueSubstitution() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(EnterSubstitutionAdvice.class).on(named(FOO)))
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.newInstance()), is((Object) FOO));
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalEnterValueSubstitution() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(IllegalEnterSubstitutionAdvice.class).on(named(BAR)))
                .make();
    }

    @Test
    public void testFieldAdviceImplicit() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(FieldSample.class)
                .visit(Advice.to(FieldAdviceImplicit.class).on(named(FOO)))
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testFieldAdviceExplicit() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(FieldSample.class)
                .visit(Advice.to(FieldAdviceExplicit.class).on(named(FOO)))
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testOriginAdvice() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(OriginAdvice.class).on(named(FOO)))
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testOriginCustomAdvice() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(OriginCustomAdvice.class).on(named(FOO)))
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.newInstance()), is((Object) FOO));
        assertThat(type.getDeclaredField(ENTER).get(null), is((Object) 1));
        assertThat(type.getDeclaredField(EXIT).get(null), is((Object) 1));
    }

    @Test
    public void testFrameAdvice() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(FrameSample.class)
                .visit(Advice.to(FrameAdvice.class).on(named(FOO)))
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO, String.class).invoke(type.newInstance(), FOO), is((Object) FOO));
        assertThat(type.getField(COUNT).getInt(null), is((Object) 2));
    }

    @Test
    public void testFrameAdviceStaticMethod() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(FrameSample.class)
                .visit(Advice.to(FrameAdvice.class).on(named(BAR)))
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(BAR, String.class).invoke(null, FOO), is((Object) FOO));
        assertThat(type.getField(COUNT).getInt(null), is((Object) 2));
    }

    @Test
    public void testFrameAdviceExpanded() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(FrameSample.class)
                .visit(Advice.to(FrameAdvice.class).on(named(FOO)).readerFlags(ClassReader.EXPAND_FRAMES))
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO, String.class).invoke(type.newInstance(), FOO), is((Object) FOO));
        assertThat(type.getField(COUNT).getInt(null), is((Object) 2));
    }

    @Test
    public void testFrameAdviceStaticMethodExpanded() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(FrameSample.class)
                .visit(Advice.to(FrameAdvice.class).on(named(BAR)).readerFlags(ClassReader.EXPAND_FRAMES))
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(BAR, String.class).invoke(null, FOO), is((Object) FOO));
        assertThat(type.getField(COUNT).getInt(null), is((Object) 2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAdviceWithoutAnnotations() throws Exception {
        Advice.to(Object.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDuplicateAdvice() throws Exception {
        Advice.to(IllegalStateException.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testIOExceptionOnRead() throws Exception {
        ClassFileLocator classFileLocator = mock(ClassFileLocator.class);
        when(classFileLocator.locate(TrivialAdvice.class.getName())).thenThrow(new IOException());
        Advice.to(TrivialAdvice.class, classFileLocator);
    }

    @Test(expected = IllegalStateException.class)
    public void testNonStaticAdvice() throws Exception {
        Advice.to(NonStaticAdvice.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testAmbiguousAdvice() throws Exception {
        Advice.to(AmbiguousAdvice.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testCannotBindEnterToEnter() throws Exception {
        Advice.to(EnterToEnterAdvice.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testCannotBindEnterToReturn() throws Exception {
        Advice.to(EnterToReturnAdvice.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testAdviceWithNonExistentArgument() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(IllegalArgumentAdvice.class).on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAdviceWithNonAssignableParameterImplicit() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(IllegalArgumentAdvice.class).on(named(BAR)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAdviceWithNonAssignableParameter() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(IllegalArgumentWritableAdvice.class).on(named(BAR)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAdviceWithNonEqualParameter() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(IllegalArgumentReadOnlyAdvice.class).on(named(BAR)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAdviceThisReferenceNonExistent() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(ThisReferenceAdvice.class).on(named(BAZ)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAdviceWithNonAssignableThisReference() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(IllegalThisReferenceAdvice.class).on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAdviceWithNonEqualThisReference() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(IllegalThisReferenceWritableAdvice.class).on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAdviceWithNonAssignableReturnValue() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(NonAssignableReturnAdvice.class).on(named(FOO + QUX)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAdviceWithNonAssignableReturnValueWritable() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.to(NonEqualReturnWritableAdvice.class).on(named(FOO + QUX)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAdviceAbstractMethod() throws Exception {
        new ByteBuddy()
                .redefine(AbstractMethod.class)
                .visit(Advice.to(TrivialAdvice.class).on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAdviceWithNonAssignableEnterValue() throws Exception {
        Advice.to(NonAssignableEnterAdvice.class);
    }

    @Test(expected = IllegalStateException.class)
    public void test() throws Exception {
        Advice.to(IllegalThrowableRequestAdvice.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalThrowableType() throws Exception {
        Advice.to(IllegalThrowableTypeAdvice.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testFieldIllegalExplicit() throws Exception {
        new ByteBuddy()
                .redefine(FieldSample.class)
                .visit(Advice.to(FieldAdviceIllegalExplicit.class).on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testFieldNonExistent() throws Exception {
        new ByteBuddy()
                .redefine(FieldSample.class)
                .visit(Advice.to(FieldAdviceNonExistent.class).on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testFieldNonAssignable() throws Exception {
        new ByteBuddy()
                .redefine(FieldSample.class)
                .visit(Advice.to(FieldAdviceNonAssignable.class).on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testFieldWrite() throws Exception {
        new ByteBuddy()
                .redefine(FieldSample.class)
                .visit(Advice.to(FieldAdviceWrite.class).on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalOriginType() throws Exception {
        Advice.to(IllegalOriginType.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalOriginPattern() throws Exception {
        Advice.to(IllegalOriginPattern.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalOriginPatternEnd() throws Exception {
        Advice.to(IllegalOriginPatternEnd.class);
    }

    @Test
    public void testCannotInstantiateSuppressionMarker() throws Exception {
        Class<?> type = Class.forName(Advice.class.getName() + "$NoSuppression");
        assertThat(Modifier.isPrivate(type.getModifiers()), is(true));
        try {
            Constructor<?> constructor = type.getDeclaredConstructor();
            assertThat(Modifier.isPrivate(constructor.getModifiers()), is(true));
            constructor.setAccessible(true);
            constructor.newInstance();
            fail();
        } catch (InvocationTargetException exception) {
            assertThat(exception.getCause(), instanceOf(UnsupportedOperationException.class));
        }
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(Advice.class).apply();
        ObjectPropertyAssertion.of(Advice.FrameTranslator.class).applyBasic();
        ObjectPropertyAssertion.of(Advice.FrameTranslator.Bound.class).applyBasic();
        ObjectPropertyAssertion.of(Advice.AdviceVisitor.CodeCopier.class).applyBasic();
        ObjectPropertyAssertion.of(Advice.Dispatcher.Inactive.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.Active.Resolved.OffsetMapping.Target.ForReadOnlyParameter.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.Active.Resolved.OffsetMapping.Target.ForParameter.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.Active.Resolved.OffsetMapping.Target.ForField.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.Active.Resolved.OffsetMapping.Target.ForConstantPoolValue.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.Active.Resolved.OffsetMapping.Target.ForDefaultValue.class).apply();
        final int[] value = new int[1];
        ObjectPropertyAssertion.of(Advice.Dispatcher.Active.Resolved.OffsetMapping.ForParameter.class).refine(new ObjectPropertyAssertion.Refinement<Advice.Argument>() {
            @Override
            public void apply(Advice.Argument mock) {
                when(mock.value()).thenReturn(value[0]++);
            }
        }).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.Active.Resolved.OffsetMapping.ForParameter.Factory.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.Active.Resolved.OffsetMapping.ForThisReference.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.Active.Resolved.OffsetMapping.ForThisReference.Factory.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.Active.Resolved.OffsetMapping.ForField.WithImplicitType.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.Active.Resolved.OffsetMapping.ForField.WithExplicitType.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.Active.Resolved.OffsetMapping.ForField.Factory.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.Active.Resolved.OffsetMapping.ForOrigin.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.Active.Resolved.OffsetMapping.ForOrigin.Factory.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.Active.Resolved.OffsetMapping.ForOrigin.Renderer.ForConstantValue.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.Active.Resolved.OffsetMapping.ForOrigin.Renderer.ForDescriptor.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.Active.Resolved.OffsetMapping.ForOrigin.Renderer.ForMethodName.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.Active.Resolved.OffsetMapping.ForOrigin.Renderer.ForStringRepresentation.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.Active.Resolved.OffsetMapping.ForOrigin.Renderer.ForTypeName.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.Active.Resolved.OffsetMapping.ForIgnored.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.Active.Resolved.OffsetMapping.ForReturnValue.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.Active.Resolved.OffsetMapping.ForReturnValue.Factory.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.Active.Resolved.OffsetMapping.ForThrowable.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.Active.Resolved.OffsetMapping.ForEnterValue.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.Active.Resolved.OffsetMapping.ForEnterValue.Factory.class).apply();
        final Iterator<Class<?>> types = Arrays.<Class<?>>asList(Object.class, String.class).iterator();
        ObjectPropertyAssertion.of(Advice.Dispatcher.Active.Resolved.OffsetMapping.Illegal.class).create(new ObjectPropertyAssertion.Creator<Class<?>>() {
            @Override
            public Class<?> create() {
                return types.next();
            }
        }).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.Active.Resolved.ForMethodEnter.class).refine(new ObjectPropertyAssertion.Refinement<MethodDescription.InDefinedShape>() {
            @Override
            public void apply(MethodDescription.InDefinedShape mock) {
                when(mock.getParameters()).thenReturn(new ParameterList.Empty<ParameterDescription.InDefinedShape>());
            }
        }).apply();
        final Iterator<StackSize> iterator = Arrays.asList(StackSize.values()).iterator();
        ObjectPropertyAssertion.of(Advice.Dispatcher.Active.Resolved.ForMethodExit.class).refine(new ObjectPropertyAssertion.Refinement<MethodDescription.InDefinedShape>() {
            @Override
            public void apply(MethodDescription.InDefinedShape mock) {
                when(mock.getParameters()).thenReturn(new ParameterList.Empty<ParameterDescription.InDefinedShape>());
                try {
                    when(mock.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(TrivialAdvice.class.getDeclaredMethod(EXIT).getDeclaredAnnotations()));
                } catch (Exception exception) {
                    throw new AssertionError(exception);
                }
            }
        }).refine(new ObjectPropertyAssertion.Refinement<TypeDescription>() {
            @Override
            public void apply(TypeDescription mock) {
                when(mock.getStackSize()).thenReturn(iterator.next());
            }
        }).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.Active.CodeTranslationVisitor.SuppressionHandler.NoOp.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.Active.CodeTranslationVisitor.SuppressionHandler.Suppressing.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.Active.CodeTranslationVisitor.ForMethodEnter.class).applyBasic();
        ObjectPropertyAssertion.of(Advice.Dispatcher.Active.CodeTranslationVisitor.ForMethodExit.class).refine(new ObjectPropertyAssertion.Refinement<MethodDescription.InDefinedShape>() {
            @Override
            public void apply(MethodDescription.InDefinedShape mock) {
                when(mock.getReturnType()).thenReturn(TypeDescription.Generic.VOID);
            }
        }).applyBasic();
    }

    @SuppressWarnings("unused")
    public static class Sample {

        private Object object;

        public static int enter, exit;

        public static Throwable throwable;

        public String foo() {
            return FOO;
        }

        public String foobar() {
            throw new RuntimeException();
        }

        public String bar(String argument) {
            return argument;
        }

        public String qux(String first, String second) {
            return first + second;
        }

        public static String baz() {
            return FOO;
        }

        public String quxbaz() {
            String foo = FOO, bar = BAR, qux = QUX, baz = BAZ;
            return foo + bar + qux + baz;
        }

        public void fooqux() {
            /* do nothing */
        }

        public void barbaz() {
            object.getClass(); // implicit null pointer
        }
    }

    public abstract static class AbstractMethod {

        public abstract void foo();
    }

    public static class TracableSample {

        public static int enter, exit, inside;

        public void foo() {
            inside++;
        }
    }

    @SuppressWarnings("unused")
    public static class TrivialAdvice {

        @Advice.OnMethodEnter
        private static void enter() {
            Sample.enter++;
        }

        @Advice.OnMethodExit
        private static void exit() {
            Sample.exit++;
        }
    }

    @SuppressWarnings("unused")
    public static class AdviceWithVariableValues {

        @Advice.OnMethodEnter
        private static int enter() {
            int foo = VALUE, bar = VALUE * 2;
            Sample.enter++;
            return foo + bar;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Enter int enter, @Advice.Return String value) {
            int foo = VALUE, bar = VALUE * 2;
            if (foo + bar != enter || !value.equals(FOO + BAR + QUX + BAZ)) {
                throw new AssertionError();
            }
            Sample.exit++;
        }
    }

    @SuppressWarnings("unused")
    public static class TrivialAdviceSkipException {

        @Advice.OnMethodEnter
        private static void enter() {
            Sample.enter++;
        }

        @Advice.OnMethodExit(onThrowable = false)
        private static void exit() {
            Sample.exit++;
        }
    }

    @SuppressWarnings("unused")
    public static class ArgumentAdvice {

        public static int enter, exit;

        @Advice.OnMethodEnter
        private static void enter(String argument) {
            if (!argument.equals(BAR)) {
                throw new AssertionError();
            }
            Sample.enter++;
        }

        @Advice.OnMethodExit
        private static void exit(String argument) {
            if (!argument.equals(BAR)) {
                throw new AssertionError();
            }
            Sample.exit++;
        }
    }

    @SuppressWarnings("unused")
    public static class ArgumentAdviceExplicit {

        @Advice.OnMethodEnter
        private static void enter(@Advice.Argument(1) String argument) {
            if (!argument.equals(BAR)) {
                throw new AssertionError();
            }
            Sample.enter++;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Argument(1) String argument) {
            if (!argument.equals(BAR)) {
                throw new AssertionError();
            }
            Sample.exit++;
        }
    }

    @SuppressWarnings("unused")
    public static class ThisReferenceAdvice {

        @Advice.OnMethodEnter
        private static void enter(@Advice.This Sample thiz) {
            if (thiz == null) {
                throw new AssertionError();
            }
            Sample.enter++;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.This Sample thiz) {
            if (thiz == null) {
                throw new AssertionError();
            }
            Sample.exit++;
        }
    }

    @SuppressWarnings("unused")
    public static class EntranceValueAdvice {

        @Advice.OnMethodEnter
        private static int enter() {
            Sample.enter++;
            return VALUE;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Enter int value) {
            if (value != VALUE) {
                throw new AssertionError();
            }
            Sample.exit++;
        }
    }

    @SuppressWarnings("unused")
    public static class ReturnValueAdvice {

        @Advice.OnMethodExit
        private static void exit(@Advice.Return String value) {
            if (!value.equals(FOO)) {
                throw new AssertionError();
            }
            Sample.exit++;
        }
    }

    @SuppressWarnings("unused")
    public static class ObsoleteReturnValueAdvice {

        @Advice.OnMethodEnter
        private static int enter() {
            Sample.enter++;
            return VALUE;
        }
    }

    @SuppressWarnings("unused")
    public static class UnusedReturnValueAdvice {

        @Advice.OnMethodEnter
        private static int enter() {
            Sample.enter++;
            return VALUE;
        }

        @Advice.OnMethodExit
        private static void exit() {
            Sample.exit++;
        }
    }

    @SuppressWarnings("unused")
    public static class ThrowableAdvice {

        @Advice.OnMethodExit
        private static void exit(@Advice.Thrown Throwable throwable) {
            Sample.throwable = throwable;
        }
    }

    @SuppressWarnings("unused")
    public static class ThrowOnEnter {

        @Advice.OnMethodEnter
        private static void enter() {
            TracableSample.enter++;
            throw new RuntimeException();
        }

        @Advice.OnMethodExit
        private static void exit() {
            TracableSample.exit++;
        }
    }

    @SuppressWarnings("unused")
    public static class ThrowOnExit {

        @Advice.OnMethodEnter
        private static void enter() {
            TracableSample.enter++;
        }

        @Advice.OnMethodExit
        private static void exit() {
            TracableSample.exit++;
            throw new RuntimeException();
        }
    }

    @SuppressWarnings("unused")
    public static class ThrowSuppressed {

        @Advice.OnMethodEnter(suppress = RuntimeException.class)
        private static void enter() {
            TracableSample.enter++;
            throw new RuntimeException();
        }

        @Advice.OnMethodExit(suppress = RuntimeException.class)
        private static void exit() {
            TracableSample.exit++;
            throw new RuntimeException();
        }
    }

    public static class ThrowNotSuppressedOnEnter {

        @Advice.OnMethodEnter(suppress = RuntimeException.class)
        private static void enter() throws Exception {
            TracableSample.enter++;
            throw new Exception();
        }
    }

    public static class ThrowNotSuppressedOnExit {

        @Advice.OnMethodEnter(suppress = RuntimeException.class)
        private static void enter() {
            TracableSample.enter++;
            throw new RuntimeException();
        }

        @Advice.OnMethodExit(suppress = RuntimeException.class)
        private static void exit() throws Exception {
            TracableSample.exit++;
            throw new Exception();
        }
    }

    @SuppressWarnings("unused")
    public static class ThisSubstitutionAdvice {

        @Advice.OnMethodEnter
        @SuppressWarnings("all")
        private static void enter(@Advice.This(readOnly = false) Box box) {
            box = new Box(BAR);
        }
    }

    @SuppressWarnings("unused")
    public static class IllegalThisSubstitutionAdvice {

        @Advice.OnMethodEnter
        @SuppressWarnings("all")
        private static void enter(@Advice.This Box box) {
            box = new Box(BAR);
        }
    }

    @SuppressWarnings("unused")
    public static class ParameterSubstitutionAdvice {

        @Advice.OnMethodEnter
        @SuppressWarnings("all")
        private static void enter(@Advice.Argument(value = 0, readOnly = false) String value) {
            value = BAR;
        }
    }

    @SuppressWarnings("unused")
    public static class IllegalParameterSubstitutionAdvice {

        @Advice.OnMethodEnter
        @SuppressWarnings("all")
        private static void enter(@Advice.Argument(0) String value) {
            value = BAR;
        }
    }

    @SuppressWarnings("unused")
    public static class EnterSubstitutionAdvice {

        @Advice.OnMethodEnter
        private static String enter() {
            return FOO;
        }

        @Advice.OnMethodExit
        @SuppressWarnings("all")
        private static void exit(@Advice.Enter(readOnly = false) String value) {
            value = BAR;
            if (!value.equals(BAR)) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class IllegalEnterSubstitutionAdvice {

        @Advice.OnMethodEnter
        private static String enter() {
            return FOO;
        }

        @Advice.OnMethodExit
        @SuppressWarnings("all")
        private static void exit(@Advice.Enter String value) {
            value = BAR;
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class ReturnSubstitutionAdvice {

        @Advice.OnMethodExit
        @SuppressWarnings("all")
        private static void exit(@Advice.Return(readOnly = false) String value) {
            value = BAR;
            if (!value.equals(BAR)) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class IllegalReturnSubstitutionAdvice {

        @Advice.OnMethodExit
        @SuppressWarnings("all")
        private static void exit(@Advice.Return String value) {
            value = BAR;
            throw new AssertionError();
        }
    }

    public static class Box {

        public final String value;

        public Box(String value) {
            this.value = value;
        }

        public String foo() {
            return value;
        }

        public static String bar(String value) {
            return value;
        }
    }

    public static class FieldSample {

        public static int enter, exit;

        private String foo = FOO;

        public String foo() {
            return foo;
        }

        public static String bar() {
            return BAR;
        }
    }

    public static class FieldAdviceImplicit {

        @Advice.OnMethodEnter
        private static void enter(@Advice.FieldValue("foo") String foo) {
            FieldSample.enter++;
            if (!foo.equals(FOO)) {
                throw new AssertionError();
            }
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.FieldValue("foo") String foo) {
            FieldSample.exit++;
            if (!foo.equals(FOO)) {
                throw new AssertionError();
            }
        }
    }

    public static class FieldAdviceExplicit {

        @Advice.OnMethodEnter
        private static void enter(@Advice.FieldValue(value = "foo", declaringType = FieldSample.class) String foo) {
            FieldSample.enter++;
            if (!foo.equals(FOO)) {
                throw new AssertionError();
            }
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.FieldValue(value = "foo", declaringType = FieldSample.class) String foo) {
            FieldSample.exit++;
            if (!foo.equals(FOO)) {
                throw new AssertionError();
            }
        }
    }

    @SuppressWarnings("unused")
    public static class OriginAdvice {

        @Advice.OnMethodEnter
        private static void enter(@Advice.Origin String origin) throws Exception {
            if (!origin.equals(Sample.class.getDeclaredMethod(FOO).toString())) {
                throw new AssertionError();
            }
            Sample.enter++;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Origin String origin) throws Exception {
            if (!origin.equals(Sample.class.getDeclaredMethod(FOO).toString())) {
                throw new AssertionError();
            }
            Sample.exit++;
        }
    }

    @SuppressWarnings("unused")
    public static class OriginCustomAdvice {

        @Advice.OnMethodEnter
        private static void enter(@Advice.Origin("#t #m #d") String origin) throws Exception {
            if (!origin.equals(Sample.class.getName() + " " + FOO + " ()L" + String.class.getName().replace('.', '/') + ";")) {
                System.out.println(origin);
                throw new AssertionError();
            }
            Sample.enter++;
        }

        @Advice.OnMethodExit
        private static void exit(@Advice.Origin("\\#\\#\\\\#m") String origin) throws Exception {
            if (!origin.equals("##\\" + FOO)) {
                System.out.println(origin);
                throw new AssertionError();
            }
            Sample.exit++;
        }
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
            return value;
        }
    }

    @SuppressWarnings("unused")
    public static class FrameAdvice {

        @Advice.OnMethodEnter(suppress = RuntimeException.class)
        @Advice.OnMethodExit(suppress = RuntimeException.class)
        private static String advice(@Advice.Ignored int ignored, @Advice.Argument(0) String value) {
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

    public static class FieldAdviceIllegalExplicit {

        @Advice.OnMethodEnter
        private static void enter(@Advice.FieldValue(value = "bar", declaringType = Void.class) String bar) {
            throw new AssertionError();
        }
    }

    public static class FieldAdviceNonExistent {

        @Advice.OnMethodEnter
        private static void enter(@Advice.FieldValue("bar") String bar) {
            throw new AssertionError();
        }
    }

    public static class FieldAdviceNonAssignable {

        @Advice.OnMethodEnter
        private static void enter(@Advice.FieldValue("foo") Void foo) {
            throw new AssertionError();
        }
    }

    public static class FieldAdviceWrite {

        @Advice.OnMethodEnter
        private static void enter(@Advice.FieldValue("foo") String foo) {
            foo = BAR;
        }
    }

    @SuppressWarnings("unused")
    public static class IllegalArgumentAdvice {

        @Advice.OnMethodEnter
        private static void enter(Void argument) {
            throw new AssertionError();
        }
    }

    public static class IllegalArgumentReadOnlyAdvice {

        @SuppressWarnings("unused")
        @Advice.OnMethodEnter
        private static void enter(@Advice.Argument(0) Void argument) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class IllegalArgumentWritableAdvice {

        @Advice.OnMethodEnter
        private static void enter(@Advice.Argument(value = 0, readOnly = false) Object argument) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class DuplicateAdvice {

        @Advice.OnMethodEnter
        private static void enter1() {
            throw new AssertionError();
        }

        @Advice.OnMethodEnter
        private static void enter2() {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class NonStaticAdvice {

        @Advice.OnMethodEnter
        private void enter() {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class IllegalThisReferenceAdvice {

        @Advice.OnMethodExit
        private static void enter(@Advice.This Void thiz) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class IllegalThisReferenceWritableAdvice {

        @Advice.OnMethodExit
        private static void enter(@Advice.This(readOnly = false) Object thiz) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class AmbiguousAdvice {

        @Advice.OnMethodEnter
        private static void enter(@Advice.Argument(0) @Advice.This Object thiz) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class EnterToEnterAdvice {

        @Advice.OnMethodEnter
        private static void enter(@Advice.Enter Object value) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class EnterToReturnAdvice {

        @Advice.OnMethodEnter
        private static void enter(@Advice.Return Object value) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class NonAssignableEnterAdvice {

        @Advice.OnMethodExit
        private static void exit(@Advice.Enter Object value) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class NonEqualEnterAdvice {

        @Advice.OnMethodExit
        private static void exit(@Advice.Enter(readOnly = false) Object value) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class NonAssignableReturnAdvice {

        @Advice.OnMethodExit
        private static void exit(@Advice.Return Void value) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class NonEqualReturnWritableAdvice {

        @Advice.OnMethodExit
        private static void exit(@Advice.Return(readOnly = false) Object value) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class IllegalThrowableRequestAdvice {

        @Advice.OnMethodExit(onThrowable = false)
        private static void exit(@Advice.Thrown Throwable value) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class IllegalThrowableTypeAdvice {

        @Advice.OnMethodExit
        private static void exit(@Advice.Thrown Object value) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class IllegalOriginType {

        @Advice.OnMethodExit
        private static void exit(@Advice.Origin Void value) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class IllegalOriginPattern {

        @Advice.OnMethodExit
        private static void exit(@Advice.Origin("#x") String value) {
            throw new AssertionError();
        }
    }

    @SuppressWarnings("unused")
    public static class IllegalOriginPatternEnd {

        @Advice.OnMethodExit
        private static void exit(@Advice.Origin("#") String value) {
            throw new AssertionError();
        }
    }
}
