package net.bytebuddy.asm;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.Arrays;
import java.util.Collection;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class AdviceExchangedDuplicationTest {

    private static final String FOO = "foo";

    private static final int NUMERIC_VALUE = 42;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(
                new Object[][]{
                        {Opcodes.DUP_X1, int.class, int.class, NUMERIC_VALUE, NUMERIC_VALUE},
                        {Opcodes.DUP_X2, int.class, long.class, NUMERIC_VALUE, (long) NUMERIC_VALUE},
                        {Opcodes.DUP2_X1, long.class, int.class, (long) NUMERIC_VALUE, NUMERIC_VALUE},
                        {Opcodes.DUP2_X2, long.class, long.class, (long) NUMERIC_VALUE, (long) NUMERIC_VALUE}
                }
        );
    }

    private final int duplication;

    private final Class<?> valueType, ignoredValueType;

    private final Object value, ignoredValue;

    public AdviceExchangedDuplicationTest(int duplication, Class<?> valueType, Class<?> ignoredValueType, Object value, Object ignoredValue) {
        this.duplication = duplication;
        this.valueType = valueType;
        this.ignoredValueType = ignoredValueType;
        this.value = value;
        this.ignoredValue = ignoredValue;
    }

    @Test
    public void testAdvice() throws Exception {
        Class<?> type = new ByteBuddy()
                .subclass(Object.class)
                .defineMethod(FOO, valueType, Visibility.PUBLIC)
                .intercept(new DuplicationImplementation())
                .make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER_PERSISTENT)
                .getLoaded();
        Class<?> redefined = new ByteBuddy()
                .redefine(type)
                .visit(Advice.to(AdviceExchangedDuplicationTest.class).on(named(FOO)))
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        assertThat(redefined.getDeclaredMethod(FOO).invoke(redefined.getDeclaredConstructor().newInstance()), is(value));
    }

    @Advice.OnMethodExit
    @SuppressWarnings("unused")
    private static void exit() {
        /* empty */
    }

    private class DuplicationImplementation implements Implementation, ByteCodeAppender {

        public ByteCodeAppender appender(Target implementationTarget) {
            return this;
        }

        public InstrumentedType prepare(InstrumentedType instrumentedType) {
            return instrumentedType;
        }

        public Size apply(MethodVisitor methodVisitor, Context implementationContext, MethodDescription instrumentedMethod) {
            methodVisitor.visitLdcInsn(ignoredValue);
            methodVisitor.visitLdcInsn(value);
            methodVisitor.visitInsn(duplication);
            methodVisitor.visitInsn(Type.getType(valueType).getSize() == 2 ? Opcodes.POP2 : Opcodes.POP);
            methodVisitor.visitInsn(Type.getType(ignoredValueType).getSize() == 2 ? Opcodes.POP2 : Opcodes.POP);
            methodVisitor.visitInsn(Type.getType(valueType).getOpcode(Opcodes.IRETURN));
            return new Size(Type.getType(valueType).getSize() * 2 + Type.getType(ignoredValueType).getSize(), instrumentedMethod.getStackSize());
        }
    }
}
