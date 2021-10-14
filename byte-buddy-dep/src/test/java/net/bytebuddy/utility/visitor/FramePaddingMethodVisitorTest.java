package net.bytebuddy.utility.visitor;

import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.*;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;

import static org.mockito.Mockito.*;

@RunWith(Parameterized.class)
public class FramePaddingMethodVisitorTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"visitIntInsn", new Class<?>[]{int.class, int.class}, new Object[]{0, 0}},
                {"visitVarInsn", new Class<?>[]{int.class, int.class}, new Object[]{0, 0}},
                {"visitTypeInsn", new Class<?>[]{int.class, String.class}, new Object[]{0, ""}},
                {"visitFieldInsn", new Class<?>[]{int.class, String.class, String.class, String.class}, new Object[]{0, "", "", ""}},
                {"visitMethodInsn", new Class<?>[]{int.class, String.class, String.class, String.class, boolean.class}, new Object[]{0, "", "", "", false}},
                {"visitInvokeDynamicInsn", new Class<?>[]{String.class, String.class, Handle.class, Object[].class}, new Object[]{"", "", new Handle(0, "", "", "", false), new Object[0]}},
                {"visitJumpInsn", new Class<?>[]{int.class, Label.class}, new Object[]{0, new Label()}},
                {"visitLdcInsn", new Class<?>[]{Object.class}, new Object[]{new Object()}},
                {"visitIincInsn", new Class<?>[]{int.class, int.class}, new Object[]{0, 0}},
                {"visitTableSwitchInsn", new Class<?>[]{int.class, int.class, Label.class, Label[].class}, new Object[]{0, 0, new Label(), new Label[0]}},
                {"visitLookupSwitchInsn", new Class<?>[]{Label.class, int[].class, Label[].class}, new Object[]{new Label(), new int[0], new Label[0]}},
                {"visitMultiANewArrayInsn", new Class<?>[]{String.class, int.class}, new Object[]{"", 0}},
                {"visitInsn", new Class<?>[]{int.class}, new Object[]{1}},
        });
    }

    @Rule
    public MockitoRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodVisitor methodVisitor;

    private final String name;

    private final Class<?>[] type;

    private final Object[] argument;

    public FramePaddingMethodVisitorTest(String name, Class<?>[] type, Object[] argument) {
        this.name = name;
        this.type = type;
        this.argument = argument;
    }

    @Test
    public void testFramePadding() throws Exception {
        Method method = MethodVisitor.class.getDeclaredMethod(name, type);
        FramePaddingMethodVisitor visitor = new FramePaddingMethodVisitor(methodVisitor);
        visitor.visitFrame(0, 0, null, 0, null);
        visitor.visitFrame(0, 0, null, 0, null);
        method.invoke(visitor, argument);
        InOrder inOrder = inOrder(methodVisitor);
        inOrder.verify(methodVisitor).visitFrame(0, 0, null, 0, null);
        inOrder.verify(methodVisitor).visitInsn(Opcodes.NOP);
        inOrder.verify(methodVisitor).visitFrame(0, 0, null, 0, null);
        method.invoke(inOrder.verify(methodVisitor), argument);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testFramePaddingIgnoredOnDuplicateOfSameFrame() throws Exception {
        Method method = MethodVisitor.class.getDeclaredMethod(name, type);
        FramePaddingMethodVisitor visitor = new FramePaddingMethodVisitor(methodVisitor);
        visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        visitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        method.invoke(visitor, argument);
        InOrder inOrder = inOrder(methodVisitor);
        inOrder.verify(methodVisitor).visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        method.invoke(inOrder.verify(methodVisitor), argument);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testNoFramePadding() throws Exception {
        Method method = MethodVisitor.class.getDeclaredMethod(name, type);
        FramePaddingMethodVisitor visitor = new FramePaddingMethodVisitor(methodVisitor);
        visitor.visitFrame(0, 0, null, 0, null);
        method.invoke(visitor, argument);
        visitor.visitFrame(0, 0, null, 0, null);
        InOrder inOrder = inOrder(methodVisitor);
        inOrder.verify(methodVisitor).visitFrame(0, 0, null, 0, null);
        method.invoke(inOrder.verify(methodVisitor), argument);
        inOrder.verify(methodVisitor).visitFrame(0, 0, null, 0, null);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testFramePaddingWithLabelMapping() throws Exception {
        Label label = new Label();
        Method method = MethodVisitor.class.getDeclaredMethod(name, type);
        FramePaddingMethodVisitor visitor = new FramePaddingMethodVisitor(methodVisitor);
        visitor.visitLabel(label);
        visitor.visitFrame(0, 0, null, 0, null);
        visitor.visitFrame(0, 0, null, 0, null);
        method.invoke(visitor, argument);
        visitor.visitFrame(0, 1, new Object[]{label}, 1, new Object[]{label});
        InOrder inOrder = inOrder(methodVisitor);
        inOrder.verify(methodVisitor).visitLabel(label);
        inOrder.verify(methodVisitor).visitFrame(0, 0, null, 0, null);
        inOrder.verify(methodVisitor).visitInsn(Opcodes.NOP);
        inOrder.verify(methodVisitor).visitFrame(0, 0, null, 0, null);
        ArgumentCaptor<Label> captor = ArgumentCaptor.forClass(Label.class);
        inOrder.verify(methodVisitor).visitLabel(captor.capture());
        method.invoke(inOrder.verify(methodVisitor), argument);
        inOrder.verify(methodVisitor).visitFrame(0, 1, new Object[]{captor.getValue()}, 1, new Object[]{captor.getValue()});
        inOrder.verifyNoMoreInteractions();
    }
}
