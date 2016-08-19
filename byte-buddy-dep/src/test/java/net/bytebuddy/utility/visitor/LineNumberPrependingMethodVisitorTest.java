package net.bytebuddy.utility.visitor;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import static org.hamcrest.CoreMatchers.not;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class LineNumberPrependingMethodVisitorTest {

    private static final int LINE = 42;

    @Test
    public void testPrepending() throws Exception {
        MethodVisitor delegate = mock(MethodVisitor.class);
        LineNumberPrependingMethodVisitor methodVisitor = new LineNumberPrependingMethodVisitor(delegate);
        methodVisitor.onAfterExceptionTable();
        Label label = new Label();
        methodVisitor.visitLineNumber(LINE, label);
        verify(delegate, times(2)).visitLabel(any(Label.class));
        verify(delegate).visitLineNumber(eq(LINE), argThat(not(label)));
        verifyNoMoreInteractions(delegate);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(LineNumberPrependingMethodVisitor.class).applyBasic();
    }
}
