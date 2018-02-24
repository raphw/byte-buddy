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
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AdviceInconsistentFrameTest {

    private static final String FOO = "foo", BAR  = "bar";

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

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
                .visit(Advice.to(TrivialAdvice.class).on(named(FOO)))
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
                .visit(Advice.to(TrivialAdvice.class).on(named(FOO)))
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
                .visit(Advice.to(TrivialAdvice.class).on(named(FOO)))
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
                .visit(Advice.to(TrivialAdvice.class).on(named(FOO)))
                .make();
    }

    @SuppressWarnings("all")
    private static class TrivialAdvice {

        @Advice.OnMethodEnter
        private static void exit() {
            /* empty */
        }
    }

    private static class TooShortMethod implements Implementation, ByteCodeAppender {

        @Override
        public ByteCodeAppender appender(Target implementationTarget) {
            return this;
        }

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
            methodVisitor.visitFrame(Opcodes.F_FULL, 0, new Object[0], 0, new Object[0]);
            methodVisitor.visitLdcInsn(BAR);
            methodVisitor.visitInsn(Opcodes.ARETURN);
            return new Size(1, 2);
        }
    }

    private static class DropImplicitMethod implements Implementation, ByteCodeAppender {

        @Override
        public ByteCodeAppender appender(Target implementationTarget) {
            return this;
        }

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
            methodVisitor.visitFrame(Opcodes.F_CHOP, 1, new Object[0], 0, null);
            methodVisitor.visitLdcInsn(BAR);
            methodVisitor.visitInsn(Opcodes.ARETURN);
            return new Size(1, 2);
        }
    }

    private static class InconsistentThisReferenceMethod implements Implementation, ByteCodeAppender {

        @Override
        public ByteCodeAppender appender(Target implementationTarget) {
            return this;
        }

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
            methodVisitor.visitFrame(Opcodes.F_FULL, 1, new Object[] {TypeDescription.OBJECT.getInternalName()}, 0, new Object[0]);
            methodVisitor.visitLdcInsn(BAR);
            methodVisitor.visitInsn(Opcodes.ARETURN);
            return new Size(1, 2);
        }
    }

    private static class InconsistentParameterReferenceMethod implements Implementation, ByteCodeAppender {

        @Override
        public ByteCodeAppender appender(Target implementationTarget) {
            return this;
        }

        @Override
        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType;
        }

        @Override
        public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
            methodVisitor.visitFrame(Opcodes.F_FULL, 1, new Object[] {TypeDescription.OBJECT.getInternalName()}, 0, new Object[0]);
            methodVisitor.visitLdcInsn(BAR);
            methodVisitor.visitInsn(Opcodes.ARETURN);
            return new Size(1, 2);
        }
    }
}
