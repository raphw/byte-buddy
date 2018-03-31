package net.bytebuddy.implementation.attribute;

import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.TypePath;

import static org.mockito.Mockito.*;

public class AnnotationAppenderTargetTest {

    private static final String FOO = "foo";

    private static final int BAR = 42;

    private static final String TYPE_PATH = "*";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private FieldVisitor fieldVisitor;

    @Mock
    private ClassVisitor classVisitor;

    @Test
    public void testOnField() throws Exception {
        new AnnotationAppender.Target.OnField(fieldVisitor).visit(FOO, true);
        verify(fieldVisitor).visitAnnotation(FOO, true);
        verifyNoMoreInteractions(fieldVisitor);
    }

    @Test
    public void testOnType() throws Exception {
        new AnnotationAppender.Target.OnType(classVisitor).visit(FOO, true);
        verify(classVisitor).visitAnnotation(FOO, true);
        verifyNoMoreInteractions(classVisitor);
    }

    @Test
    public void testOnMethod() throws Exception {
        new AnnotationAppender.Target.OnMethod(methodVisitor).visit(FOO, true);
        verify(methodVisitor).visitAnnotation(FOO, true);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testOnMethodParameter() throws Exception {
        new AnnotationAppender.Target.OnMethodParameter(methodVisitor, 0).visit(FOO, true);
        verify(methodVisitor).visitParameterAnnotation(0, FOO, true);
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testTypeAnnotationOnField() throws Exception {
        new AnnotationAppender.Target.OnField(fieldVisitor).visit(FOO, true, BAR, TYPE_PATH);
        verify(fieldVisitor).visitTypeAnnotation(eq(BAR), any(TypePath.class), eq(FOO), eq(true));
        verifyNoMoreInteractions(fieldVisitor);
    }

    @Test
    public void testTypeAnnotationOnType() throws Exception {
        new AnnotationAppender.Target.OnType(classVisitor).visit(FOO, true, BAR, TYPE_PATH);
        verify(classVisitor).visitTypeAnnotation(eq(BAR), any(TypePath.class), eq(FOO), eq(true));
        verifyNoMoreInteractions(classVisitor);
    }

    @Test
    public void testTypeAnnotationOnMethod() throws Exception {
        new AnnotationAppender.Target.OnMethod(methodVisitor).visit(FOO, true, BAR, TYPE_PATH);
        verify(methodVisitor).visitTypeAnnotation(eq(BAR), any(TypePath.class), eq(FOO), eq(true));
        verifyNoMoreInteractions(methodVisitor);
    }

    @Test
    public void testTypeAnnotationOnMethodParameter() throws Exception {
        new AnnotationAppender.Target.OnMethodParameter(methodVisitor, 0).visit(FOO, true, BAR, TYPE_PATH);
        verify(methodVisitor).visitTypeAnnotation(eq(BAR), any(TypePath.class), eq(FOO), eq(true));
        verifyNoMoreInteractions(methodVisitor);
    }
}
