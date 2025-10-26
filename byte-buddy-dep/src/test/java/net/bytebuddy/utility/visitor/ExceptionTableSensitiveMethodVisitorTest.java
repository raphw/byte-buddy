package net.bytebuddy.utility.visitor;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(Parameterized.class)
public class ExceptionTableSensitiveMethodVisitorTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {Opcodes.ASM6, "visitLabel", new Class<?>[]{Label.class}, new Object[]{new Label()}},
                {Opcodes.ASM6, "visitFrame", new Class<?>[]{int.class, int.class, Object[].class, int.class, Object[].class}, new Object[]{0, 0, new Object[0], 0, new Object[0]}},
                {Opcodes.ASM6, "visitIntInsn", new Class<?>[]{int.class, int.class}, new Object[]{0, 0}},
                {Opcodes.ASM6, "visitVarInsn", new Class<?>[]{int.class, int.class}, new Object[]{0, 0}},
                {Opcodes.ASM6, "visitTypeInsn", new Class<?>[]{int.class, String.class}, new Object[]{0, ""}},
                {Opcodes.ASM6, "visitFieldInsn", new Class<?>[]{int.class, String.class, String.class, String.class}, new Object[]{0, "", "", ""}},
                {Opcodes.ASM4, "visitMethodInsn", new Class<?>[]{int.class, String.class, String.class, String.class}, new Object[]{0, "", "", ""}},
                {Opcodes.ASM6, "visitMethodInsn", new Class<?>[]{int.class, String.class, String.class, String.class, boolean.class}, new Object[]{0, "", "", "", false}},
                {Opcodes.ASM6, "visitInvokeDynamicInsn", new Class<?>[]{String.class, String.class, Handle.class, Object[].class}, new Object[]{"", "", new Handle(0, "", "", "", false), new Object[0]}},
                {Opcodes.ASM6, "visitJumpInsn", new Class<?>[]{int.class, Label.class}, new Object[]{0, new Label()}},
                {Opcodes.ASM6, "visitLdcInsn", new Class<?>[]{Object.class}, new Object[]{new Object()}},
                {Opcodes.ASM6, "visitIincInsn", new Class<?>[]{int.class, int.class}, new Object[]{0, 0}},
                {Opcodes.ASM6, "visitTableSwitchInsn", new Class<?>[]{int.class, int.class, Label.class, Label[].class}, new Object[]{0, 0, new Label(), new Label[0]}},
                {Opcodes.ASM6, "visitLookupSwitchInsn", new Class<?>[]{Label.class, int[].class, Label[].class}, new Object[]{new Label(), new int[0], new Label[0]}},
                {Opcodes.ASM6, "visitMultiANewArrayInsn", new Class<?>[]{String.class, int.class}, new Object[]{"", 0}},
                {Opcodes.ASM6, "visitInsn", new Class<?>[]{int.class}, new Object[]{0}},
        });
    }

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private MethodVisitor methodVisitor;

    private final int api;

    private final String name;

    private final Class<?>[] type;

    private final Object[] argument;

    public ExceptionTableSensitiveMethodVisitorTest(int api, String name, Class<?>[] type, Object[] argument) {
        this.api = api;
        this.name = name;
        this.type = type;
        this.argument = argument;
    }

    @Test
    public void testCallback() throws Exception {
        Method method = MethodVisitor.class.getDeclaredMethod(name, type);
        PseudoVisitor pseudoVisitor = new PseudoVisitor(api, methodVisitor);
        method.invoke(pseudoVisitor, argument);
        method.invoke(pseudoVisitor, argument);
        method.invoke(verify(methodVisitor, times(2)), argument);
        verifyNoMoreInteractions(methodVisitor);
        pseudoVisitor.check();
    }

    private static class PseudoVisitor extends ExceptionTableSensitiveMethodVisitor {

        private boolean called;

        private PseudoVisitor(int api, MethodVisitor methodVisitor) {
            super(api, methodVisitor);
        }

        @Override
        protected void onAfterExceptionTable() {
            if (called) {
                throw new AssertionError();
            }
            called = true;
            verifyNoMoreInteractions(mv);
        }

        @Override
        @SuppressWarnings("deprecation") // avoid redirection implementation for redirection to work.
        protected void onVisitMethodInsn(int opcode, String owner, String name, String descriptor) {
            mv.visitMethodInsn(opcode, owner, name, descriptor);
        }

        protected void check() {
            if (!called) {
                throw new AssertionError();
            }
        }
    }
}