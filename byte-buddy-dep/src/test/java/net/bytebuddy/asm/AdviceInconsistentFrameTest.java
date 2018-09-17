package net.bytebuddy.asm;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.modifier.Ownership;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.test.utility.JavaVersionRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.Collection;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class AdviceInconsistentFrameTest {

    private static final String FOO = "foo", BAR = "bar";

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {TrivialAdvice.class, DiscardingAdvice.class, CopyingAdvice.class, RetainingAdvice.class},
                {DelegatingTrivialAdvice.class, DelegatingDiscardingAdvice.class, DelegatingCopyingAdvice.class, DelegatingRetainingAdvice.class}
        });
    }

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    private final Class<?> trivial, discarding, copying, retaining;

    public AdviceInconsistentFrameTest(Class<?> trivial, Class<?> discarding, Class<?> copying, Class<?> retaining) {
        this.trivial = trivial;
        this.discarding = discarding;
        this.copying = copying;
        this.retaining = retaining;
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testFrameTooShortTrivial() throws Exception {
        Class<?> type = new ByteBuddy()
                .subclass(Object.class)
                .defineMethod(FOO, String.class, Visibility.PUBLIC)
                .intercept(new TooShortMethod())
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER_PERSISTENT)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) BAR));
        new ByteBuddy()
                .redefine(type)
                .visit(Advice.to(trivial).on(named(FOO)))
                .make();
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testFrameDropImplicitTrivial() throws Exception {
        Class<?> type = new ByteBuddy()
                .subclass(Object.class)
                .defineMethod(FOO, String.class, Visibility.PUBLIC)
                .intercept(new DropImplicitMethod())
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER_PERSISTENT)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) BAR));
        new ByteBuddy()
                .redefine(type)
                .visit(Advice.to(trivial).on(named(FOO)))
                .make();
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testFrameInconsistentThisParameterTrivial() throws Exception {
        Class<?> type = new ByteBuddy()
                .subclass(Object.class)
                .defineMethod(FOO, String.class, Visibility.PUBLIC)
                .intercept(new InconsistentThisReferenceMethod())
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER_PERSISTENT)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) BAR));
        new ByteBuddy()
                .redefine(type)
                .visit(Advice.to(trivial).on(named(FOO)))
                .make();
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testFrameInconsistentParameterTrivial() throws Exception {
        Class<?> type = new ByteBuddy()
                .subclass(Object.class)
                .defineMethod(FOO, String.class, Visibility.PUBLIC, Ownership.STATIC)
                .withParameters(Void.class)
                .intercept(new InconsistentParameterReferenceMethod())
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER_PERSISTENT)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO, Void.class).invoke(null, (Object) null), is((Object) BAR));
        new ByteBuddy()
                .redefine(type)
                .visit(Advice.to(trivial).on(named(FOO)))
                .make();
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testFrameTooShortTrivialDiscarding() throws Exception {
        Class<?> type = new ByteBuddy()
                .subclass(Object.class)
                .defineMethod(FOO, String.class, Visibility.PUBLIC)
                .intercept(new TooShortMethod())
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER_PERSISTENT)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) BAR));
        new ByteBuddy()
                .redefine(type)
                .visit(Advice.to(discarding).on(named(FOO)))
                .make();
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testFrameDropImplicitTrivialDiscarding() throws Exception {
        Class<?> type = new ByteBuddy()
                .subclass(Object.class)
                .defineMethod(FOO, String.class, Visibility.PUBLIC)
                .intercept(new DropImplicitMethod())
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER_PERSISTENT)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) BAR));
        new ByteBuddy()
                .redefine(type)
                .visit(Advice.to(discarding).on(named(FOO)))
                .make();
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testFrameInconsistentThisParameterTrivialDiscarding() throws Exception {
        Class<?> type = new ByteBuddy()
                .subclass(Object.class)
                .defineMethod(FOO, String.class, Visibility.PUBLIC)
                .intercept(new InconsistentThisReferenceMethod())
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER_PERSISTENT)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) BAR));
        new ByteBuddy()
                .redefine(type)
                .visit(Advice.to(discarding).on(named(FOO)))
                .make();
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testFrameInconsistentParameterTrivialDiscarding() throws Exception {
        Class<?> type = new ByteBuddy()
                .subclass(Object.class)
                .defineMethod(FOO, String.class, Visibility.PUBLIC, Ownership.STATIC)
                .withParameters(Void.class)
                .intercept(new InconsistentParameterReferenceMethod())
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER_PERSISTENT)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO, Void.class).invoke(null, (Object) null), is((Object) BAR));
        new ByteBuddy()
                .redefine(type)
                .visit(Advice.to(discarding).on(named(FOO)))
                .make();
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testFrameTooShortTrivialCopying() throws Exception {
        Class<?> type = new ByteBuddy()
                .subclass(Object.class)
                .defineMethod(FOO, String.class, Visibility.PUBLIC)
                .intercept(new TooShortMethod())
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER_PERSISTENT)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) BAR));
        new ByteBuddy()
                .redefine(type)
                .visit(Advice.to(copying).on(named(FOO)))
                .make();
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testFrameDropImplicitTrivialCopying() throws Exception {
        Class<?> type = new ByteBuddy()
                .subclass(Object.class)
                .defineMethod(FOO, String.class, Visibility.PUBLIC)
                .intercept(new DropImplicitMethod())
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER_PERSISTENT)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) BAR));
        new ByteBuddy()
                .redefine(type)
                .visit(Advice.to(copying).on(named(FOO)))
                .make();
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testFrameInconsistentThisParameterTrivialCopying() throws Exception {
        Class<?> type = new ByteBuddy()
                .subclass(Object.class)
                .defineMethod(FOO, String.class, Visibility.PUBLIC)
                .intercept(new InconsistentThisReferenceMethod())
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER_PERSISTENT)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) BAR));
        new ByteBuddy()
                .redefine(type)
                .visit(Advice.to(copying).on(named(FOO)))
                .make();
    }

    @Test
    @JavaVersionRule.Enforce(7)
    public void testFrameInconsistentParameterTrivialCopying() throws Exception {
        Class<?> type = new ByteBuddy()
                .subclass(Object.class)
                .defineMethod(FOO, String.class, Visibility.PUBLIC, Ownership.STATIC)
                .withParameters(Void.class)
                .intercept(new InconsistentParameterReferenceMethod())
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER_PERSISTENT)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO, Void.class).invoke(null, (Object) null), is((Object) BAR));
        new ByteBuddy()
                .redefine(type)
                .visit(Advice.to(copying).on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    @JavaVersionRule.Enforce(7)
    public void testFrameTooShort() throws Exception {
        Class<?> type = new ByteBuddy()
                .subclass(Object.class)
                .defineMethod(FOO, String.class, Visibility.PUBLIC)
                .intercept(new TooShortMethod())
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER_PERSISTENT)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) BAR));
        new ByteBuddy()
                .redefine(type)
                .visit(Advice.to(retaining).on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    @JavaVersionRule.Enforce(7)
    public void testFrameDropImplicit() throws Exception {
        Class<?> type = new ByteBuddy()
                .subclass(Object.class)
                .defineMethod(FOO, String.class, Visibility.PUBLIC)
                .intercept(new DropImplicitMethod())
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER_PERSISTENT)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) BAR));
        new ByteBuddy()
                .redefine(type)
                .visit(Advice.to(retaining).on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    @JavaVersionRule.Enforce(7)
    public void testFrameInconsistentThisParameter() throws Exception {
        Class<?> type = new ByteBuddy()
                .subclass(Object.class)
                .defineMethod(FOO, String.class, Visibility.PUBLIC)
                .intercept(new InconsistentThisReferenceMethod())
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER_PERSISTENT)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getDeclaredConstructor().newInstance()), is((Object) BAR));
        new ByteBuddy()
                .redefine(type)
                .visit(Advice.to(retaining).on(named(FOO)))
                .make();
    }

    @Test(expected = IllegalStateException.class)
    @JavaVersionRule.Enforce(7)
    public void testFrameInconsistentParameter() throws Exception {
        Class<?> type = new ByteBuddy()
                .subclass(Object.class)
                .defineMethod(FOO, String.class, Visibility.PUBLIC, Ownership.STATIC)
                .withParameters(Void.class)
                .intercept(new InconsistentParameterReferenceMethod())
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER_PERSISTENT)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO, Void.class).invoke(null, (Object) null), is((Object) BAR));
        new ByteBuddy()
                .redefine(type)
                .visit(Advice.to(retaining).on(named(FOO)))
                .make();
    }

    @SuppressWarnings("all")
    public static class TrivialAdvice {

        @Advice.OnMethodEnter
        public static void enter() {
            /* empty */
        }
    }

    @SuppressWarnings("all")
    public static class DiscardingAdvice {

        @Advice.OnMethodEnter
        public static boolean enter() {
            return false;
        }
    }

    @SuppressWarnings("all")
    public static class CopyingAdvice {

        @Advice.OnMethodEnter
        public static boolean enter() {
            return false;
        }

        @Advice.OnMethodExit
        public static void exit() {
            /* do nothing */
        }
    }

    @SuppressWarnings("all")
    public static class RetainingAdvice {

        @Advice.OnMethodEnter
        public static boolean enter() {
            return false;
        }

        @Advice.OnMethodExit(backupArguments = false)
        public static void exit() {
            /* do nothing */
        }
    }

    @SuppressWarnings("all")
    public static class DelegatingTrivialAdvice {

        @Advice.OnMethodEnter(inline = false)
        public static void enter() {
            /* empty */
        }
    }

    @SuppressWarnings("all")
    public static class DelegatingDiscardingAdvice {

        @Advice.OnMethodEnter(inline = false)
        public static boolean enter() {
            return false;
        }
    }

    @SuppressWarnings("all")
    public static class DelegatingCopyingAdvice {

        @Advice.OnMethodEnter(inline = false)
        public static boolean enter() {
            return false;
        }

        @Advice.OnMethodExit(inline = false)
        public static void exit() {
            /* do nothing */
        }
    }

    @SuppressWarnings("all")
    public static class DelegatingRetainingAdvice {

        @Advice.OnMethodEnter(inline = false)
        public static boolean enter() {
            return false;
        }

        @Advice.OnMethodExit(backupArguments = false, inline = false)
        public static void exit() {
            /* do nothing */
        }
    }

    public static class TooShortMethod implements Implementation, ByteCodeAppender {

        public ByteCodeAppender appender(Target implementationTarget) {
            return this;
        }

        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType;
        }

        public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
            methodVisitor.visitFrame(Opcodes.F_FULL, 0, new Object[0], 0, new Object[0]);
            methodVisitor.visitLdcInsn(BAR);
            methodVisitor.visitInsn(Opcodes.ARETURN);
            return new Size(1, 2);
        }
    }

    public static class DropImplicitMethod implements Implementation, ByteCodeAppender {

        public ByteCodeAppender appender(Target implementationTarget) {
            return this;
        }

        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType;
        }

        public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
            methodVisitor.visitFrame(Opcodes.F_CHOP, 1, new Object[0], 0, null);
            methodVisitor.visitLdcInsn(BAR);
            methodVisitor.visitInsn(Opcodes.ARETURN);
            return new Size(1, 2);
        }
    }

    public static class InconsistentThisReferenceMethod implements Implementation, ByteCodeAppender {

        public ByteCodeAppender appender(Target implementationTarget) {
            return this;
        }

        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType;
        }

        public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
            methodVisitor.visitFrame(Opcodes.F_FULL, 1, new Object[]{TypeDescription.OBJECT.getInternalName()}, 0, new Object[0]);
            methodVisitor.visitLdcInsn(BAR);
            methodVisitor.visitInsn(Opcodes.ARETURN);
            return new Size(1, 2);
        }
    }

    public static class InconsistentParameterReferenceMethod implements Implementation, ByteCodeAppender {

        public ByteCodeAppender appender(Target implementationTarget) {
            return this;
        }

        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType;
        }

        public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
            methodVisitor.visitFrame(Opcodes.F_FULL, 1, new Object[]{TypeDescription.OBJECT.getInternalName()}, 0, new Object[0]);
            methodVisitor.visitLdcInsn(BAR);
            methodVisitor.visitInsn(Opcodes.ARETURN);
            return new Size(1, 2);
        }
    }
}
