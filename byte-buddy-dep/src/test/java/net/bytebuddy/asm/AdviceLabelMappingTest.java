package net.bytebuddy.asm;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import org.junit.Test;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static net.bytebuddy.matcher.ElementMatchers.named;

public class AdviceLabelMappingTest {

    @Test
    public void testLabelWithJumpMapping() throws Exception {
        new ByteBuddy()
                .subclass(Runnable.class)
                .visit(Advice.to(RunnableAdvice.class).on(named("run")))
                .defineMethod("run", void.class, Visibility.PUBLIC)
                .intercept(new Implementation.Simple(new ByteCodeAppender() {
                    @Override
                    public Size apply(MethodVisitor methodVisitor, Implementation.Context implementationContext, MethodDescription instrumentedMethod) {
                        Label label = new Label();
                        methodVisitor.visitFrame(Opcodes.F_SAME, 0, new Object[0], 0, null);
                        methodVisitor.visitLabel(label);
                        methodVisitor.visitTypeInsn(Opcodes.NEW, "java/util/ArrayList");
                        Label jump = new Label();
                        methodVisitor.visitJumpInsn(Opcodes.GOTO, jump);
                        methodVisitor.visitFrame(Opcodes.F_FULL, 0, new Object[0], 1, new Object[]{"java/lang/Throwable"});
                        methodVisitor.visitInsn(Opcodes.ATHROW);
                        methodVisitor.visitLabel(jump);
                        methodVisitor.visitFrame(Opcodes.F_FULL, 0, new Object[0], 1, new Object[]{label});
                        methodVisitor.visitInsn(Opcodes.DUP);
                        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V", false);
                        methodVisitor.visitVarInsn(Opcodes.ASTORE, 1);
                        methodVisitor.visitInsn(Opcodes.RETURN);
                        return new Size(2, 1);
                    }
                }))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded()
                .getConstructor()
                .newInstance()
                .run();
    }

    private static class RunnableAdvice {

        @Advice.OnMethodEnter(suppress = Throwable.class)
        private static void enter() {
            /* empty */
        }

        @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
        private static void exit() {
            /* empty */
        }
    }
}
