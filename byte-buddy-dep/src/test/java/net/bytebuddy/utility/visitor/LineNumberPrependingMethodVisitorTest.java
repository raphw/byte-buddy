package net.bytebuddy.utility.visitor;

import org.junit.Test;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

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
        verify(delegate).visitLineNumber(eq(LINE), not(eq(label)));
        verifyNoMoreInteractions(delegate);
    }
}
