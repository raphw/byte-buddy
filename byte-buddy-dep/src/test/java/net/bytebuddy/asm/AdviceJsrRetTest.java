package net.bytebuddy.asm;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import org.junit.Test;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AdviceJsrRetTest {

    private static final String FOO = "foo", BAR = "bar";

    @Test
    public void testJsrRetByteCodes() throws Exception {
        Class<?> type = new ByteBuddy(ClassFileVersion.JAVA_V4)
                .subclass(Object.class)
                .defineMethod(FOO, String.class, Visibility.PUBLIC)
                .intercept(new JsrRetMethod())
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER_PERSISTENT)
                .getLoaded();
        assertThat(type.getDeclaredMethod(FOO).invoke(type.getConstructor().newInstance()), is((Object) FOO));
        Class<?> advised = new ByteBuddy()
                .redefine(type)
                .visit(Advice.to(JsrAdvice.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(advised.getDeclaredMethod(FOO).invoke(advised.getConstructor().newInstance()), is((Object) BAR));
    }

    @SuppressWarnings("all")
    private static class JsrAdvice {

        @Advice.OnMethodExit
        private static void exit(@Advice.Return(readOnly = false) String value) {
            value = BAR;
        }
    }

    private static class JsrRetMethod implements Implementation, ByteCodeAppender {

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
            Label target = new Label();
            methodVisitor.visitJumpInsn(Opcodes.JSR, target);
            methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
            methodVisitor.visitInsn(Opcodes.ARETURN);
            methodVisitor.visitLabel(target);
            methodVisitor.visitVarInsn(Opcodes.ASTORE, 2);
            methodVisitor.visitLdcInsn(FOO);
            methodVisitor.visitVarInsn(Opcodes.ASTORE, 1);
            methodVisitor.visitVarInsn(Opcodes.RET, 2);
            return new Size(1, 3);
        }
    }
}
