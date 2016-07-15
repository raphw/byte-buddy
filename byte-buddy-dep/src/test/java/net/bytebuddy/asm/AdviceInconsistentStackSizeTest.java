package net.bytebuddy.asm;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.implementation.bytecode.StackSize;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.Collection;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@RunWith(Parameterized.class)
public class AdviceInconsistentStackSizeTest {

    private final Class<?> type;

    private final Object original, replaced;

    private final int opcode;

    public AdviceInconsistentStackSizeTest(Class<?> type, Object original, Object replaced, int opcode) {
        this.type = type;
        this.original = original;
        this.replaced = replaced;
        this.opcode = opcode;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {String.class, "foo", "bar", Opcodes.ARETURN},
                {boolean.class, 0, false, Opcodes.IRETURN},
                {byte.class, 0, (byte) 42, Opcodes.IRETURN},
                {short.class, 0, (short) 42, Opcodes.IRETURN},
                {char.class, 0, (char) 42, Opcodes.IRETURN},
                {int.class, 0, 42, Opcodes.IRETURN},
                {long.class, 0L, 42L, Opcodes.LRETURN},
                {float.class, 0f, 42f, Opcodes.FRETURN},
                {double.class, 0d, 42d, Opcodes.DRETURN},
                {void.class, null, null, Opcodes.RETURN},
        });
    }

    @Test
    public void testInconsistentStackSize() throws Exception {
        Class<?> atypical = new ByteBuddy()
                .subclass(Object.class)
                .defineMethod("foo", type, Visibility.PUBLIC)
                .intercept(new InconsistentSizeAppender())
                .make()
                .load(null, ClassLoadingStrategy.Default.WRAPPER_PERSISTENT)
                .getLoaded();
        Class<?> adviced = new ByteBuddy()
                .redefine(atypical)
                .visit(Advice.withCustomMapping().bind(Value.class, replaced).to(ExitAdvice.class).on(named("foo")))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(adviced.getDeclaredMethod("foo").invoke(adviced.newInstance()), is(replaced));
    }

    @SuppressWarnings("all")
    private static class ExitAdvice {

        @Advice.OnMethodExit
        private static void exit(@Advice.BoxedReturn(readOnly = false) Object returned, @Value Object value) {
            returned = value;
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    private @interface Value {
        /* empty */
    }

    private class InconsistentSizeAppender implements Implementation, ByteCodeAppender {

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
            if (original != null) {
                methodVisitor.visitLdcInsn(original);
            }
            methodVisitor.visitInsn(opcode);
            methodVisitor.visitFrame(Opcodes.F_SAME, 0, new Object[0], 0, new Object[0]);
            if (original != null) {
                methodVisitor.visitLdcInsn(original);
                methodVisitor.visitLdcInsn(original);
            }
            methodVisitor.visitInsn(opcode);
            return new Size(StackSize.of(type).getSize() * 2, instrumentedMethod.getStackSize());
        }
    }
}
