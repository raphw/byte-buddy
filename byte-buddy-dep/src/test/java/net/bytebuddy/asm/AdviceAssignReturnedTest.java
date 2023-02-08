package net.bytebuddy.asm;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import static net.bytebuddy.matcher.ElementMatchers.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class AdviceAssignReturnedTest {

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";

    @Test
    public void testAssignReturnedToArgumentScalar() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping()
                        .with(new Advice.AssignReturned.Factory())
                        .to(ToArgumentScalar.class)
                        .on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getMethod(FOO, String.class).invoke(type.getConstructor().newInstance(), FOO), is((Object) BAR));
    }

    @Test
    public void testAssignReturnedToArgumentArray() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping()
                        .with(new Advice.AssignReturned.Factory())
                        .to(ToArgumentArray.class)
                        .on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getMethod(FOO, String.class).invoke(type.getConstructor().newInstance(), FOO), is((Object) BAR));
    }

    @Test
    public void testAssignReturnedToAllArgumentsScalar() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping()
                        .with(new Advice.AssignReturned.Factory())
                        .to(ToAllArgumentsScalar.class)
                        .on(named(BAZ)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getMethod(BAZ, String.class, String.class).invoke(type.getConstructor().newInstance(), FOO, BAR), is((Object) (BAR + FOO)));
    }

    @Test
    public void testAssignReturnedToAllArgumentsArray() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping()
                        .with(new Advice.AssignReturned.Factory())
                        .to(ToAllArgumentsArray.class)
                        .on(named(BAZ)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getMethod(BAZ, String.class, String.class).invoke(type.getConstructor().newInstance(), FOO, BAR), is((Object) (BAR + FOO)));
    }

    @Test
    public void testAssignThisToArgumentScalar() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping()
                        .with(new Advice.AssignReturned.Factory())
                        .to(ToThisScalar.class)
                        .on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getMethod(FOO, String.class).invoke(type.getConstructor().newInstance(), FOO), is((Object) FOO));
    }

    @Test
    public void testAssignThisToArgumentArray() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping()
                        .with(new Advice.AssignReturned.Factory())
                        .to(ToThisArray.class)
                        .on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getMethod(FOO, String.class).invoke(type.getConstructor().newInstance(), FOO), is((Object) FOO));
    }

    @Test
    public void testAssignReturnedToFieldScalar() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping()
                        .with(new Advice.AssignReturned.Factory())
                        .to(ToFieldScalar.class)
                        .on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getConstructor().newInstance();
        type.getField(FOO).set(instance, FOO);
        assertThat(type.getMethod(FOO, String.class).invoke(instance, FOO), is((Object) FOO));
        assertThat(type.getField(FOO).get(instance), is((Object) QUX));
    }

    @Test
    public void testAssignReturnedToFieldArray() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping()
                        .with(new Advice.AssignReturned.Factory())
                        .to(ToFieldArray.class)
                        .on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getConstructor().newInstance();
        type.getField(FOO).set(instance, FOO);
        assertThat(type.getMethod(FOO, String.class).invoke(instance, FOO), is((Object) FOO));
        assertThat(type.getField(FOO).get(instance), is((Object) QUX));
    }

    @Test
    public void testAssignReturnedToReturnedScalar() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping()
                        .with(new Advice.AssignReturned.Factory())
                        .to(ToReturnedScalar.class)
                        .on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getMethod(FOO, String.class).invoke(type.getConstructor().newInstance(), FOO), is((Object) BAR));
    }

    @Test
    public void testAssignReturnedToReturnedArray() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping()
                        .with(new Advice.AssignReturned.Factory())
                        .to(ToReturnedArray.class)
                        .on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getMethod(FOO, String.class).invoke(type.getConstructor().newInstance(), FOO), is((Object) BAR));
    }

    @Test
    public void testAssignReturnedVoid() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping()
                        .with(new Advice.AssignReturned.Factory())
                        .to(ToReturnedScalar.class)
                        .on(named(QUX)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getMethod(QUX, String.class).invoke(type.getConstructor().newInstance(), FOO), nullValue(Object.class));
    }

    @Test(expected = RuntimeException.class)
    public void testAssignReturnedToThrownScalar() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping()
                        .with(new Advice.AssignReturned.Factory())
                        .to(ToThrownScalar.class)
                        .on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        try {
            assertThat(type.getMethod(FOO, String.class).invoke(type.getConstructor().newInstance(), FOO), is((Object) BAR));
            fail();
        } catch (InvocationTargetException exception) {
            throw (Exception) exception.getTargetException();
        }
    }

    @Test
    public void testAssignReturnedToThrownSuppressScalar() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(SampleThrowing.class)
                .visit(Advice.withCustomMapping()
                        .with(new Advice.AssignReturned.Factory())
                        .to(ToThrownSuppressScalar.class)
                        .on(named(BAR)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getMethod(BAR).invoke(type.getConstructor().newInstance()), nullValue(Object.class));
    }

    @Test(expected = RuntimeException.class)
    public void testAssignReturnedToThrownArray() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping()
                        .with(new Advice.AssignReturned.Factory())
                        .to(ToThrownArray.class)
                        .on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        try {
            assertThat(type.getMethod(FOO, String.class).invoke(type.getConstructor().newInstance(), FOO), is((Object) BAR));
            fail();
        } catch (InvocationTargetException exception) {
            throw (Exception) exception.getTargetException();
        }
    }

    @Test
    public void testAssignReturnedToThrownSuppressedArray() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(SampleThrowing.class)
                .visit(Advice.withCustomMapping()
                        .with(new Advice.AssignReturned.Factory())
                        .to(ToThrownSuppressedArray.class)
                        .on(named(BAR)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getMethod(BAR).invoke(type.getConstructor().newInstance()), nullValue(Object.class));
    }

    @Test
    public void testAssignReturnedToArgumentArrayAsScalar() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(SampleArray.class)
                .visit(Advice.withCustomMapping()
                        .with(new Advice.AssignReturned.Factory())
                        .to(ToArgumentArrayAsScalar.class)
                        .on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getMethod(FOO, String[].class).invoke(type.getConstructor().newInstance(), (Object) new String[]{FOO}), is((Object) new String[]{BAR}));
    }

    @Test
    public void testAssignReturnedToThrownSuppression() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(SampleThrowing.class)
                .visit(Advice.withCustomMapping()
                        .with(new Advice.AssignReturned.Factory())
                        .to(ToThrownSuppress.class)
                        .on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getMethod(FOO, String.class).invoke(type.getConstructor().newInstance(), FOO), nullValue(Object.class));
    }

    @Test
    public void testAssignReturnedToAllArgumentsDynamicTyped() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping()
                        .with(new Advice.AssignReturned.Factory())
                        .to(ToAllArgumentsDynamic.class)
                        .on(named(BAZ)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getMethod(BAZ, String.class, String.class).invoke(type.getConstructor().newInstance(), FOO, BAR), is((Object) (BAR + FOO)));
    }

    @Test
    public void testAssignReturnedToFieldStaticFromNonStatic() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping()
                        .with(new Advice.AssignReturned.Factory())
                        .to(ToFieldStatic.class)
                        .on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        Object instance = type.getConstructor().newInstance();
        type.getField(BAR).set(null, FOO);
        assertThat(type.getMethod(FOO, String.class).invoke(instance, FOO), is((Object) FOO));
        assertThat(type.getField(BAR).get(null), is((Object) QUX));
    }

    @Test
    public void testAssignReturnedToFieldStaticFromStatic() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping()
                        .with(new Advice.AssignReturned.Factory())
                        .to(ToFieldStatic.class)
                        .on(named(BAR)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        type.getField(BAR).set(null, FOO);
        assertThat(type.getMethod(BAR, String.class).invoke(null, FOO), is((Object) FOO));
        assertThat(type.getField(BAR).get(null), is((Object) QUX));
    }

    @Test
    public void testAssignReturnedNoHandler() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping()
                        .with(new Advice.AssignReturned.Factory())
                        .to(ToNothing.class)
                        .on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getMethod(FOO, String.class).invoke(type.getConstructor().newInstance(), FOO), is((Object) FOO));
    }

    @Test
    public void testAssignReturnedWithSkip() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping()
                        .with(new Advice.AssignReturned.Factory())
                        .to(WithSkip.class)
                        .on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getMethod(FOO, String.class).invoke(type.getConstructor().newInstance(), FOO), is((Object) FOO));
    }

    @Test
    public void testAssignReturnedWithSkipDelegation() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping()
                        .with(new Advice.AssignReturned.Factory())
                        .to(WithSkipDelegation.class)
                        .on(named(FOO)))
                .make()
                .load(WithSkipDelegation.class.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded();
        assertThat(type.getMethod(FOO, String.class).invoke(type.getConstructor().newInstance(), FOO), is((Object) FOO));
    }

    @Test
    public void testAssignReturnedWithRepeat() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping()
                        .with(new Advice.AssignReturned.Factory())
                        .to(WithRepeat.class)
                        .on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getMethod(FOO, String.class).invoke(type.getConstructor().newInstance(), FOO), is((Object) FOO));
    }

    @Test
    public void testAssignReturnedWithRepeatDelegation() throws Exception {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping()
                        .with(new Advice.AssignReturned.Factory())
                        .to(WithRepeatDelegation.class)
                        .on(named(FOO)))
                .make()
                .load(WithRepeatDelegation.class.getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST)
                .getLoaded();
        assertThat(type.getMethod(FOO, String.class).invoke(type.getConstructor().newInstance(), FOO), is((Object) FOO));
    }

    @Test(expected = ClassCastException.class)
    public void testAssignIncompatible() throws Throwable {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping()
                        .with(new Advice.AssignReturned.Factory())
                        .to(WithIncompatibleAssignment.class)
                        .on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        try {
            type.getMethod(FOO, String.class).invoke(type.getConstructor().newInstance(), FOO);
        } catch (InvocationTargetException exception) {
            throw exception.getTargetException();
        }
    }

    @Test
    public void testAssignIncompatibleHandled() throws Throwable {
        Class<?> type = new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping()
                        .with(new Advice.AssignReturned.Factory().withSuppressed(ClassCastException.class))
                        .to(WithIncompatibleAssignment.class)
                        .on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(type.getMethod(FOO, String.class).invoke(type.getConstructor().newInstance(), FOO), is((Object) FOO));
    }

    @Test(expected = IllegalStateException.class)
    public void testArgumentTooFewParameters() {
        new ByteBuddy()
                .redefine(Object.class)
                .visit(Advice.withCustomMapping()
                        .with(new Advice.AssignReturned.Factory())
                        .to(ToArgumentScalar.class)
                        .on(isToString()))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testArgumentNotAssignable() throws Exception {
        new ByteBuddy()
                .redefine(SampleVoid.class)
                .visit(Advice.withCustomMapping()
                        .with(new Advice.AssignReturned.Factory())
                        .to(ToArgumentScalar.class)
                        .on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testReturnTypeNotAssignable() throws Exception {
        new ByteBuddy()
                .redefine(SampleVoid.class)
                .visit(Advice.withCustomMapping()
                        .with(new Advice.AssignReturned.Factory())
                        .to(ToReturnedScalar.class)
                        .on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testThrownNotAssignable() throws Exception {
        new ByteBuddy()
                .redefine(SampleThrowing.class)
                .visit(Advice.withCustomMapping()
                        .with(new Advice.AssignReturned.Factory())
                        .to(ToThrownNotAssignable.class)
                        .on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testThisStaticMethod() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping()
                        .with(new Advice.AssignReturned.Factory())
                        .to(ToThisScalar.class)
                        .on(named(BAR)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testThisConstructor() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping()
                        .with(new Advice.AssignReturned.Factory())
                        .to(ToThisScalar.class)
                        .on(isConstructor()))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testThisNotAssignable() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping()
                        .with(new Advice.AssignReturned.Factory())
                        .to(ToThisNotAssignable.class)
                        .on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testAllArgumentsNotAssignable() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping()
                        .with(new Advice.AssignReturned.Factory())
                        .to(ToAllArgumentsNotAssignable.class)
                        .on(named(BAZ)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testFieldValueNotAssignable() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping()
                        .with(new Advice.AssignReturned.Factory())
                        .to(ToFieldNotAssignable.class)
                        .on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testFieldValueNotFound() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping()
                        .with(new Advice.AssignReturned.Factory())
                        .to(ToFieldNotKnown.class)
                        .on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    public void testFieldValueStatic() throws Exception {
        new ByteBuddy()
                .redefine(Sample.class)
                .visit(Advice.withCustomMapping()
                        .with(new Advice.AssignReturned.Factory())
                        .to(ToFieldScalar.class)
                        .on(named(BAR)))
                .make();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHandledNotThrowable() {
        new Advice.AssignReturned.Factory().withSuppressed(TypeDescription.ForLoadedType.of(Object.class));
    }

    public static class Sample {

        public String foo;

        public static String bar;

        public String foo(String value) {
            return value;
        }

        public static String bar(String value) {
            return value;
        }

        public void qux(String value) {
            /* empty */
        }

        public String baz(String left, String right) {
            return left + right;
        }
    }

    public static class SampleArray {

        public String[] foo(String[] value) {
            return value;
        }
    }

    public static class SampleVoid {

        public Void foo;

        public Void foo(Void value) {
            return value;
        }
    }

    public static class SampleThrowing {

        public String foo(String value) {
            throw new RuntimeException();
        }

        public void bar() {
            throw new RuntimeException();
        }
    }

    public static class ToArgumentScalar {

        @Advice.OnMethodEnter
        @Advice.AssignReturned.ToArguments(@Advice.AssignReturned.ToArguments.ToArgument(0))
        public static String enter(@Advice.Argument(0) String argument) {
            if (!FOO.equals(argument)) {
                throw new AssertionError();
            }
            return BAR;
        }

        @Advice.OnMethodExit
        @Advice.AssignReturned.ToArguments(@Advice.AssignReturned.ToArguments.ToArgument(0))
        public static String exit(@Advice.Argument(0) String argument) {
            if (!BAR.equals(argument)) {
                throw new AssertionError();
            }
            return QUX;
        }
    }

    public static class ToArgumentArray {

        @Advice.OnMethodEnter
        @Advice.AssignReturned.ToArguments(@Advice.AssignReturned.ToArguments.ToArgument(value = 0, index = 0))
        public static String[] enter(@Advice.Argument(0) String argument) {
            if (!FOO.equals(argument)) {
                throw new AssertionError();
            }
            return new String[]{BAR};
        }

        @Advice.OnMethodExit
        @Advice.AssignReturned.ToArguments(@Advice.AssignReturned.ToArguments.ToArgument(value = 0, index = 0))
        public static String[] exit(@Advice.Argument(0) String argument) {
            if (!BAR.equals(argument)) {
                throw new AssertionError();
            }
            return new String[]{QUX};
        }
    }

    public static class ToAllArgumentsScalar {

        @Advice.OnMethodEnter
        @Advice.AssignReturned.AsScalar
        @Advice.AssignReturned.ToAllArguments
        public static String[] enter(@Advice.AllArguments String[] argument) {
            if (argument.length != 2 || !FOO.equals(argument[0]) || !BAR.equals(argument[1])) {
                throw new AssertionError();
            }
            return new String[]{BAR, FOO};
        }

        @Advice.OnMethodExit
        @Advice.AssignReturned.AsScalar
        @Advice.AssignReturned.ToAllArguments
        public static String[] exit(@Advice.AllArguments String[] argument) {
            if (argument.length != 2 || !BAR.equals(argument[0]) || !FOO.equals(argument[1])) {
                throw new AssertionError();
            }
            return new String[]{FOO, QUX};
        }
    }

    public static class ToAllArgumentsArray {

        @Advice.OnMethodEnter
        @Advice.AssignReturned.ToAllArguments(index = 0)
        public static String[][] enter(@Advice.AllArguments String[] argument) {
            if (argument.length != 2 || !FOO.equals(argument[0]) || !BAR.equals(argument[1])) {
                throw new AssertionError();
            }
            return new String[][]{{BAR, FOO}};
        }

        @Advice.OnMethodExit
        @Advice.AssignReturned.ToAllArguments(index = 0)
        public static String[][] exit(@Advice.AllArguments String[] argument) {
            if (argument.length != 2 || !BAR.equals(argument[0]) || !FOO.equals(argument[1])) {
                throw new AssertionError();
            }
            return new String[][]{{FOO, QUX}};
        }
    }

    public static class ToThisScalar {

        @Advice.OnMethodEnter
        @Advice.AssignReturned.ToThis
        public static Sample enter(@Advice.This Sample sample) {
            if (sample.foo != null) {
                throw new AssertionError();
            }
            Sample replacement = new Sample();
            replacement.foo = BAR;
            return replacement;
        }

        @Advice.OnMethodExit
        @Advice.AssignReturned.ToThis
        public static Sample exit(@Advice.This Sample sample) {
            if (!BAR.equals(sample.foo)) {
                throw new AssertionError();
            }
            Sample replacement = new Sample();
            replacement.foo = QUX;
            return replacement;
        }
    }

    public static class ToThisArray {

        @Advice.OnMethodEnter
        @Advice.AssignReturned.ToThis(index = 0)
        public static Sample[] enter(@Advice.This Sample sample) {
            if (sample.foo != null) {
                throw new AssertionError();
            }
            Sample replacement = new Sample();
            replacement.foo = BAR;
            return new Sample[]{replacement};
        }

        @Advice.OnMethodExit
        @Advice.AssignReturned.ToThis(index = 0)
        public static Sample[] exit(@Advice.This Sample sample) {
            if (!BAR.equals(sample.foo)) {
                throw new AssertionError();
            }
            Sample replacement = new Sample();
            replacement.foo = QUX;
            return new Sample[]{replacement};
        }
    }

    public static class ToFieldScalar {

        @Advice.OnMethodEnter
        @Advice.AssignReturned.ToFields(@Advice.AssignReturned.ToFields.ToField(FOO))
        public static String enter(@Advice.FieldValue(FOO) String field) {
            if (!FOO.equals(field)) {
                throw new AssertionError();
            }
            return BAR;
        }

        @Advice.OnMethodExit
        @Advice.AssignReturned.ToFields(@Advice.AssignReturned.ToFields.ToField(FOO))
        public static String exit(@Advice.FieldValue(FOO) String field) {
            if (!BAR.equals(field)) {
                throw new AssertionError();
            }
            return QUX;
        }
    }

    public static class ToFieldArray {

        @Advice.OnMethodEnter
        @Advice.AssignReturned.ToFields(@Advice.AssignReturned.ToFields.ToField(value = FOO, index = 0))
        public static String[] enter(@Advice.FieldValue(FOO) String field) {
            if (!FOO.equals(field)) {
                throw new AssertionError();
            }
            return new String[]{BAR};
        }

        @Advice.OnMethodExit
        @Advice.AssignReturned.ToFields(@Advice.AssignReturned.ToFields.ToField(value = FOO, index = 0))
        public static String[] exit(@Advice.FieldValue(FOO) String field) {
            if (!BAR.equals(field)) {
                throw new AssertionError();
            }
            return new String[]{QUX};
        }
    }

    public static class ToReturnedScalar {

        @Advice.OnMethodExit
        @Advice.AssignReturned.ToReturned
        public static String exit(@Advice.Argument(0) String argument) {
            if (!FOO.equals(argument)) {
                throw new AssertionError();
            }
            return BAR;
        }
    }

    public static class ToReturnedArray {

        @Advice.OnMethodExit
        @Advice.AssignReturned.ToReturned(index = 0)
        public static String[] exit(@Advice.Argument(0) String argument) {
            if (!FOO.equals(argument)) {
                throw new AssertionError();
            }
            return new String[]{BAR};
        }
    }

    public static class ToThrownScalar {

        @Advice.OnMethodExit(onThrowable = Throwable.class)
        @Advice.AssignReturned.ToThrown
        public static RuntimeException exit(@Advice.Argument(0) String argument) {
            if (!FOO.equals(argument)) {
                throw new AssertionError();
            }
            return new RuntimeException();
        }
    }

    public static class ToThrownSuppressScalar {

        @Advice.OnMethodExit(onThrowable = Throwable.class)
        @Advice.AssignReturned.ToThrown
        @Advice.AssignReturned.AsScalar(skipOnDefaultValue = false)
        public static RuntimeException exit() {
            return null;
        }
    }

    public static class ToThrownArray {

        @Advice.OnMethodExit(onThrowable = Throwable.class)
        @Advice.AssignReturned.ToThrown(index = 0)
        public static Throwable[] exit(@Advice.Argument(0) String argument) {
            if (!FOO.equals(argument)) {
                throw new AssertionError();
            }
            return new Throwable[]{new RuntimeException()};
        }
    }

    public static class ToThrownSuppressedArray {

        @Advice.OnMethodExit(onThrowable = Throwable.class)
        @Advice.AssignReturned.ToThrown(index = 0)
        public static Throwable[] exit() {
            return new Throwable[1];
        }
    }

    public static class ToArgumentArrayAsScalar {

        @Advice.OnMethodEnter
        @Advice.AssignReturned.AsScalar
        @Advice.AssignReturned.ToArguments(@Advice.AssignReturned.ToArguments.ToArgument(0))
        public static String[] enter(@Advice.Argument(0) String[] argument) {
            if (!Arrays.equals(new String[]{FOO}, argument)) {
                throw new AssertionError();
            }
            return new String[]{BAR};
        }

        @Advice.OnMethodExit
        @Advice.AssignReturned.AsScalar
        @Advice.AssignReturned.ToArguments(@Advice.AssignReturned.ToArguments.ToArgument(0))
        public static String[] exit(@Advice.Argument(0) String[] argument) {
            if (!Arrays.equals(new String[]{BAR}, argument)) {
                throw new AssertionError();
            }
            return new String[]{QUX};
        }
    }

    public static class ToThrownSuppress {

        @Advice.OnMethodExit(onThrowable = Throwable.class)
        @Advice.AssignReturned.ToThrown(index = 0)
        public static Throwable[] exit(@Advice.Thrown Throwable throwable) {
            if (!(throwable instanceof RuntimeException)) {
                throw new AssertionError();
            }
            return new Throwable[]{null};
        }
    }

    public static class ToThrownNotAssignable {

        @Advice.OnMethodExit(onThrowable = Throwable.class)
        @Advice.AssignReturned.ToThrown(index = 0)
        public static Object exit(@Advice.Thrown Throwable throwable) {
            if (!(throwable instanceof RuntimeException)) {
                throw new AssertionError();
            }
            return new Object();
        }
    }

    public static class ToThisNotAssignable {

        @Advice.OnMethodEnter
        @Advice.AssignReturned.ToThis
        public static Object enter(@Advice.This Sample sample) {
            if (sample.foo != null) {
                throw new AssertionError();
            }
            Sample replacement = new Sample();
            replacement.foo = BAR;
            return replacement;
        }
    }

    public static class ToAllArgumentsDynamic {

        @Advice.OnMethodEnter
        @Advice.AssignReturned.ToAllArguments(typing = Assigner.Typing.DYNAMIC)
        public static Object enter(@Advice.AllArguments String[] argument) {
            if (argument.length != 2 || !FOO.equals(argument[0]) || !BAR.equals(argument[1])) {
                throw new AssertionError();
            }
            return new String[]{BAR, FOO};
        }
    }

    public static class ToAllArgumentsNotAssignable {

        @Advice.OnMethodEnter
        @Advice.AssignReturned.ToAllArguments
        public static Object enter(@Advice.AllArguments String[] argument) {
            if (argument.length != 2 || !FOO.equals(argument[0]) || !BAR.equals(argument[1])) {
                throw new AssertionError();
            }
            return new String[]{BAR, FOO};
        }
    }

    public static class ToFieldNotAssignable {

        @Advice.OnMethodEnter
        @Advice.AssignReturned.ToFields(@Advice.AssignReturned.ToFields.ToField(FOO))
        public static Void enter() {
            throw new AssertionError();
        }
    }

    public static class ToFieldNotKnown {

        @Advice.OnMethodEnter
        @Advice.AssignReturned.ToFields(@Advice.AssignReturned.ToFields.ToField(QUX))
        public static String enter() {
            throw new AssertionError();
        }
    }

    public static class ToFieldStatic {

        @Advice.OnMethodEnter
        @Advice.AssignReturned.ToFields(@Advice.AssignReturned.ToFields.ToField(BAR))
        public static String enter(@Advice.FieldValue(BAR) String field) {
            if (!FOO.equals(field)) {
                throw new AssertionError();
            }
            return QUX;
        }
    }

    public static class ToNothing {

        @Advice.OnMethodEnter(suppress = Throwable.class)
        public static String enter(@Advice.Argument(0) String argument) {
            if (!FOO.equals(argument)) {
                throw new AssertionError();
            }
            return BAR;
        }

        @Advice.OnMethodExit(suppress = Throwable.class)
        public static String exit(@Advice.Argument(0) String argument) {
            if (!FOO.equals(argument)) {
                throw new AssertionError();
            }
            return QUX;
        }
    }

    public static class WithSkip {

        @Advice.OnMethodEnter(skipOn = String.class)
        @Advice.AssignReturned.ToArguments(@Advice.AssignReturned.ToArguments.ToArgument(0))
        public static String enter(@Advice.Argument(0) String argument) {
            if (!FOO.equals(argument)) {
                throw new AssertionError();
            }
            return null;
        }
    }

    public static class WithSkipDelegation {

        @Advice.OnMethodEnter(skipOn = String.class, inline = false)
        @Advice.AssignReturned.ToArguments(@Advice.AssignReturned.ToArguments.ToArgument(0))
        public static String enter(@Advice.Argument(0) String argument) {
            if (!FOO.equals(argument)) {
                throw new AssertionError();
            }
            return null;
        }
    }

    public static class WithRepeat {

        @Advice.OnMethodExit(repeatOn = String.class)
        @Advice.AssignReturned.ToArguments(@Advice.AssignReturned.ToArguments.ToArgument(0))
        public static String exit(@Advice.Argument(0) String argument) {
            if (!FOO.equals(argument)) {
                throw new AssertionError();
            }
            return null;
        }
    }

    public static class WithRepeatDelegation {

        @Advice.OnMethodExit(repeatOn = String.class, inline = false)
        @Advice.AssignReturned.ToArguments(@Advice.AssignReturned.ToArguments.ToArgument(0))
        public static String exit(@Advice.Argument(0) String argument) {
            if (!FOO.equals(argument)) {
                throw new AssertionError();
            }
            return null;
        }
    }

    public static class WithIncompatibleAssignment {

        @Advice.OnMethodEnter(suppress = ClassCastException.class)
        @Advice.AssignReturned.ToArguments(@Advice.AssignReturned.ToArguments.ToArgument(value = 0, typing = Assigner.Typing.DYNAMIC))
        public static Object enter(@Advice.Argument(0) String argument) {
            if (!FOO.equals(argument)) {
                throw new AssertionError();
            }
            return new Object();
        }
    }
}
