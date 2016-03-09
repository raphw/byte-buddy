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
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;
import org.objectweb.asm.ClassWriter;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
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

    private static final String ENTER = "enter", EXIT = "exit", INSIDE = "inside", THROWABLE = "throwable";

    private static final int VALUE = 42;

    @Test
    public void testTrivialAdvice() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(new AsmVisitorWrapper.ForDeclaredMethods().writerFlags(ClassWriter.COMPUTE_FRAMES).method(named(FOO), Advice.to(TrivialAdvice.class)))
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
                .visit(new AsmVisitorWrapper.ForDeclaredMethods().writerFlags(ClassWriter.COMPUTE_FRAMES).method(named(BAR), Advice.to(ArgumentAdvice.class)))
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
                .visit(new AsmVisitorWrapper.ForDeclaredMethods().writerFlags(ClassWriter.COMPUTE_FRAMES).method(named(QUX), Advice.to(ArgumentAdviceExplicit.class)))
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
                .visit(new AsmVisitorWrapper.ForDeclaredMethods().writerFlags(ClassWriter.COMPUTE_FRAMES).method(named(FOO), Advice.to(ThisReferenceAdvice.class)))
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
                .visit(new AsmVisitorWrapper.ForDeclaredMethods().writerFlags(ClassWriter.COMPUTE_FRAMES).method(named(FOO), Advice.to(EntranceValueAdvice.class)))
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
                .visit(new AsmVisitorWrapper.ForDeclaredMethods().writerFlags(ClassWriter.COMPUTE_FRAMES).method(named(FOO), Advice.to(ReturnValueAdvice.class)))
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
                .visit(new AsmVisitorWrapper.ForDeclaredMethods().writerFlags(ClassWriter.COMPUTE_FRAMES).method(named(FOO + BAR), Advice.to(TrivialAdvice.class)))
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
                .visit(new AsmVisitorWrapper.ForDeclaredMethods().writerFlags(ClassWriter.COMPUTE_FRAMES).method(named(FOO + BAR), Advice.to(TrivialAdviceSkipException.class)))
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
                .visit(new AsmVisitorWrapper.ForDeclaredMethods().writerFlags(ClassWriter.COMPUTE_FRAMES).method(named(BAR + BAZ), Advice.to(TrivialAdvice.class)))
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
                .visit(new AsmVisitorWrapper.ForDeclaredMethods().writerFlags(ClassWriter.COMPUTE_FRAMES).method(named(BAR + BAZ), Advice.to(TrivialAdviceSkipException.class)))
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
                .visit(new AsmVisitorWrapper.ForDeclaredMethods().writerFlags(ClassWriter.COMPUTE_FRAMES).method(named(FOO), Advice.to(TrivialAdviceSkipException.class)))
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
                .visit(new AsmVisitorWrapper.ForDeclaredMethods().writerFlags(ClassWriter.COMPUTE_FRAMES).method(named(FOO), Advice.to(ObsoleteReturnValueAdvice.class)))
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
                .visit(new AsmVisitorWrapper.ForDeclaredMethods().writerFlags(ClassWriter.COMPUTE_FRAMES).method(named(FOO), Advice.to(UnusedReturnValueAdvice.class)))
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
                .visit(new AsmVisitorWrapper.ForDeclaredMethods().writerFlags(ClassWriter.COMPUTE_FRAMES).method(named(BAR), Advice.to(AdviceWithVariableValues.class)))
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
                .visit(new AsmVisitorWrapper.ForDeclaredMethods().writerFlags(ClassWriter.COMPUTE_FRAMES).method(named(QUX + BAZ), Advice.to(AdviceWithVariableValues.class)))
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
                .visit(new AsmVisitorWrapper.ForDeclaredMethods().writerFlags(ClassWriter.COMPUTE_FRAMES).method(named(FOO), Advice.to(ThrowableAdvice.class)))
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
                .visit(new AsmVisitorWrapper.ForDeclaredMethods().writerFlags(ClassWriter.COMPUTE_FRAMES).method(named(FOO + BAR), Advice.to(ThrowableAdvice.class)))
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
                .visit(new AsmVisitorWrapper.ForDeclaredMethods().writerFlags(ClassWriter.COMPUTE_FRAMES).method(named(FOO), Advice.to(ThrowOnEnter.class)))
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
                .visit(new AsmVisitorWrapper.ForDeclaredMethods().writerFlags(ClassWriter.COMPUTE_FRAMES).method(named(FOO), Advice.to(ThrowOnExit.class)))
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
    public void testThisValueSubstitution() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Box.class)
                .visit(new AsmVisitorWrapper.ForDeclaredMethods().writerFlags(ClassWriter.COMPUTE_FRAMES).method(named(FOO), Advice.to(ThisSubstitutionAdvice.class)))
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor(String.class).newInstance(FOO)), is((Object) BAR));
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalThisValueSubstitution() throws Exception {
        new ByteBuddy()
                .redefine(Box.class)
                .visit(new AsmVisitorWrapper.ForDeclaredMethods().writerFlags(ClassWriter.COMPUTE_FRAMES).method(named(FOO), Advice.to(IllegalThisSubstitutionAdvice.class)))
                .make();
    }

    @Test
    public void testParameterValueSubstitution() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Box.class)
                .visit(new AsmVisitorWrapper.ForDeclaredMethods().writerFlags(ClassWriter.COMPUTE_FRAMES).method(named(BAR), Advice.to(ParameterSubstitutionAdvice.class)))
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(BAR, String.class).invoke(null, FOO), is((Object) BAR));
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalParameterValueSubstitution() throws Exception {
        new ByteBuddy()
                .redefine(Box.class)
                .visit(new AsmVisitorWrapper.ForDeclaredMethods().writerFlags(ClassWriter.COMPUTE_FRAMES).method(named(BAR), Advice.to(IllegalParameterSubstitutionAdvice.class)))
                .make();
    }

    @Test
    public void testReturnValueSubstitution() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(new AsmVisitorWrapper.ForDeclaredMethods().writerFlags(ClassWriter.COMPUTE_FRAMES).method(named(BAR), Advice.to(ReturnSubstitutionAdvice.class)))
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(BAR, String.class).invoke(type.newInstance(), FOO), is((Object) BAR));
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalReturnValueSubstitution() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(new AsmVisitorWrapper.ForDeclaredMethods().writerFlags(ClassWriter.COMPUTE_FRAMES).method(named(FOO), Advice.to(IllegalReturnSubstitutionAdvice.class)))
                .make();
    }

    @Test
    public void testEnterValueSubstitution() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(new AsmVisitorWrapper.ForDeclaredMethods().writerFlags(ClassWriter.COMPUTE_FRAMES).method(named(FOO), Advice.to(EnterSubstitutionAdvice.class)))
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.newInstance()), is((Object) FOO));
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalEnterValueSubstitution() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(new AsmVisitorWrapper.ForDeclaredMethods().writerFlags(ClassWriter.COMPUTE_FRAMES).method(named(BAR), Advice.to(IllegalEnterSubstitutionAdvice.class)))
                .make();
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
                .visit(new AsmVisitorWrapper.ForDeclaredMethods().writerFlags(ClassWriter.COMPUTE_FRAMES).method(named(FOO), Advice.to(IllegalArgumentAdvice.class)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAdviceWithNonAssignableParameterImplicit() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(new AsmVisitorWrapper.ForDeclaredMethods().writerFlags(ClassWriter.COMPUTE_FRAMES).method(named(BAR), Advice.to(IllegalArgumentAdvice.class)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAdviceWithNonAssignableParameter() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(new AsmVisitorWrapper.ForDeclaredMethods().writerFlags(ClassWriter.COMPUTE_FRAMES).method(named(BAR), Advice.to(IllegalArgumentWritableAdvice.class)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAdviceWithNonEqualParameter() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(new AsmVisitorWrapper.ForDeclaredMethods().writerFlags(ClassWriter.COMPUTE_FRAMES).method(named(BAR), Advice.to(IllegalArgumentReadOnlyAdvice.class)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAdviceThisReferenceNonExistent() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(new AsmVisitorWrapper.ForDeclaredMethods().writerFlags(ClassWriter.COMPUTE_FRAMES).method(named(BAZ), Advice.to(ThisReferenceAdvice.class)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAdviceWithNonAssignableThisReference() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(new AsmVisitorWrapper.ForDeclaredMethods().writerFlags(ClassWriter.COMPUTE_FRAMES).method(named(FOO), Advice.to(IllegalThisReferenceAdvice.class)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAdviceWithNonEqualThisReference() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(new AsmVisitorWrapper.ForDeclaredMethods().writerFlags(ClassWriter.COMPUTE_FRAMES).method(named(FOO), Advice.to(IllegalThisReferenceWritableAdvice.class)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAdviceWithNonAssignableReturnValue() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(new AsmVisitorWrapper.ForDeclaredMethods().writerFlags(ClassWriter.COMPUTE_FRAMES).method(named(FOO + QUX), Advice.to(NonAssignableReturnAdvice.class)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAdviceWithNonAssignableReturnValueWritable() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(new AsmVisitorWrapper.ForDeclaredMethods().writerFlags(ClassWriter.COMPUTE_FRAMES).method(named(FOO + QUX), Advice.to(NonEqualReturnWritableAdvice.class)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAdviceAbstractMethod() throws Exception {
        new ByteBuddy()
                .redefine(AbstractMethod.class)
                .visit(new AsmVisitorWrapper.ForDeclaredMethods().writerFlags(ClassWriter.COMPUTE_FRAMES).method(named(FOO), Advice.to(TrivialAdvice.class)))
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

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(Advice.class).apply();
        ObjectPropertyAssertion.of(Advice.AdviceVisitor.CodeCopier.class).applyBasic();
        ObjectPropertyAssertion.of(Advice.Dispatcher.Inactive.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.Active.Resolved.OffsetMapping.Target.ReadOnly.class).apply();
        ObjectPropertyAssertion.of(Advice.Dispatcher.Active.Resolved.OffsetMapping.Target.ReadWrite.class).apply();
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
        ObjectPropertyAssertion.of(Advice.Dispatcher.Active.CodeTranslationVisitor.ReturnValueDiscarding.class).applyBasic();
        ObjectPropertyAssertion.of(Advice.Dispatcher.Active.CodeTranslationVisitor.ReturnValueRetaining.class).applyBasic();
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
}
