package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.ParameterDescription;
import net.bytebuddy.instrumentation.method.ParameterList;
import net.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class TypeWriterMethodPoolEntryTest {

    private static final int MODIFIERS = 42, ONE = 1, TWO = 2, MULTIPLIER = 4;

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodAttributeAppender methodAttributeAppender;

    @Mock
    private ClassVisitor classVisitor;

    @Mock
    private Instrumentation.Context instrumentationContext;

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private AnnotationVisitor annotationVisitor;

    @Mock
    private MethodDescription methodDescription;

    @Mock
    private ByteCodeAppender byteCodeAppender, otherAppender;

    @Mock
    private TypeList exceptionTypes;

    @Mock
    private ParameterDescription parameterDescription;

    @Before
    public void setUp() throws Exception {
        when(methodDescription.getInternalName()).thenReturn(FOO);
        when(methodDescription.getDescriptor()).thenReturn(BAR);
        when(methodDescription.getGenericSignature()).thenReturn(QUX);
        when(methodDescription.getExceptionTypes()).thenReturn(exceptionTypes);
        when(methodDescription.getAdjustedModifiers(anyBoolean())).thenReturn(MODIFIERS);
        when(exceptionTypes.toInternalNames()).thenReturn(new String[]{BAZ});
        when(classVisitor.visitMethod(MODIFIERS, FOO, BAR, QUX, new String[]{BAZ})).thenReturn(methodVisitor);
        when(methodDescription.getParameters())
                .thenReturn(new ParameterList.Explicit(Collections.singletonList(parameterDescription)));
        when(parameterDescription.getName()).thenReturn(FOO);
        when(parameterDescription.getModifiers()).thenReturn(MODIFIERS);
        when(methodVisitor.visitAnnotationDefault()).thenReturn(annotationVisitor);
        when(byteCodeAppender.apply(methodVisitor, instrumentationContext, methodDescription))
                .thenReturn(new ByteCodeAppender.Size(ONE, TWO));
        when(otherAppender.apply(methodVisitor, instrumentationContext, methodDescription))
                .thenReturn(new ByteCodeAppender.Size(ONE * MULTIPLIER, TWO * MULTIPLIER));
    }

    @Test
    public void testSkippedMethod() throws Exception {
        assertThat(TypeWriter.MethodPool.Entry.ForSkippedMethod.INSTANCE.getSort(), is(TypeWriter.MethodPool.Entry.Sort.SKIP));
        TypeWriter.MethodPool.Entry.ForSkippedMethod.INSTANCE.apply(classVisitor, instrumentationContext, methodDescription);
        verifyZeroInteractions(classVisitor);
        verifyZeroInteractions(instrumentationContext);
        verifyZeroInteractions(methodAttributeAppender);
    }

    @Test(expected = IllegalStateException.class)
    public void testSkippedMethodCannotBePrepended() throws Exception {
        TypeWriter.MethodPool.Entry.ForSkippedMethod.INSTANCE.prepend(byteCodeAppender);
    }

    @Test(expected = IllegalStateException.class)
    public void testSkippedMethodCannotBeAppliedBody() throws Exception {
        TypeWriter.MethodPool.Entry.ForSkippedMethod.INSTANCE.applyBody(methodVisitor, instrumentationContext, methodDescription);
    }

    @Test(expected = IllegalStateException.class)
    public void testSkippedMethodCannotBeAppliedHead() throws Exception {
        TypeWriter.MethodPool.Entry.ForSkippedMethod.INSTANCE.applyHead(methodVisitor, methodDescription);
    }

    @Test
    public void testDefinedMethod() throws Exception {
        TypeWriter.MethodPool.Entry entry = new TypeWriter.MethodPool.Entry.ForAbstractMethod(methodAttributeAppender);
        assertThat(entry.getSort(), is(TypeWriter.MethodPool.Entry.Sort.DEFINE));
        entry.apply(classVisitor, instrumentationContext, methodDescription);
        verify(classVisitor).visitMethod(MODIFIERS, FOO, BAR, QUX, new String[]{BAZ});
        verifyNoMoreInteractions(classVisitor);
        verify(methodVisitor).visitEnd();
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(instrumentationContext);
        verify(methodAttributeAppender).apply(methodVisitor, methodDescription);
        verifyNoMoreInteractions(methodAttributeAppender);
    }

    @Test
    public void testDefinedMethodHeadOnly() throws Exception {
        when(parameterDescription.hasModifiers()).thenReturn(true);
        when(parameterDescription.isNamed()).thenReturn(true);
        TypeWriter.MethodPool.Entry entry = new TypeWriter.MethodPool.Entry.ForAbstractMethod(methodAttributeAppender);
        entry.applyHead(methodVisitor, methodDescription);
        verifyZeroInteractions(methodVisitor);
        verifyZeroInteractions(instrumentationContext);
        verifyZeroInteractions(methodAttributeAppender);
    }

    @Test
    public void testDefinedMethodBodyOnly() throws Exception {
        when(parameterDescription.hasModifiers()).thenReturn(true);
        when(parameterDescription.isNamed()).thenReturn(true);
        TypeWriter.MethodPool.Entry entry = new TypeWriter.MethodPool.Entry.ForAbstractMethod(methodAttributeAppender);
        entry.applyBody(methodVisitor, instrumentationContext, methodDescription);
        verifyZeroInteractions(methodVisitor);
        verifyZeroInteractions(instrumentationContext);
        verify(methodAttributeAppender).apply(methodVisitor, methodDescription);
    }

    @Test
    public void testDefinedMethodWithParameters() throws Exception {
        when(parameterDescription.hasModifiers()).thenReturn(true);
        when(parameterDescription.isNamed()).thenReturn(true);
        TypeWriter.MethodPool.Entry entry = new TypeWriter.MethodPool.Entry.ForAbstractMethod(methodAttributeAppender);
        assertThat(entry.getSort(), is(TypeWriter.MethodPool.Entry.Sort.DEFINE));
        entry.apply(classVisitor, instrumentationContext, methodDescription);
        verify(classVisitor).visitMethod(MODIFIERS, FOO, BAR, QUX, new String[]{BAZ});
        verifyNoMoreInteractions(classVisitor);
        verify(methodVisitor).visitParameter(FOO, MODIFIERS);
        verify(methodVisitor).visitEnd();
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(instrumentationContext);
        verify(methodAttributeAppender).apply(methodVisitor, methodDescription);
        verifyNoMoreInteractions(methodAttributeAppender);
    }

    @Test(expected = IllegalStateException.class)
    public void testDefinedMethodPrepended() throws Exception {
        new TypeWriter.MethodPool.Entry.ForAbstractMethod(methodAttributeAppender).prepend(otherAppender);
    }

    @Test
    public void testDefaultValueMethod() throws Exception {
        when(methodDescription.getReturnType()).thenReturn(TypeDescription.STRING);
        TypeWriter.MethodPool.Entry entry = new TypeWriter.MethodPool.Entry.ForAnnotationDefaultValue(FOO, methodAttributeAppender);
        assertThat(entry.getSort(), is(TypeWriter.MethodPool.Entry.Sort.DEFINE));
        entry.apply(classVisitor, instrumentationContext, methodDescription);
        verify(classVisitor).visitMethod(MODIFIERS, FOO, BAR, QUX, new String[]{BAZ});
        verifyNoMoreInteractions(classVisitor);
        verify(methodVisitor).visitAnnotationDefault();
        verify(methodVisitor).visitEnd();
        verifyNoMoreInteractions(methodVisitor);
        verify(annotationVisitor).visit(null, FOO);
        verify(annotationVisitor).visitEnd();
        verifyNoMoreInteractions(annotationVisitor);
        verifyZeroInteractions(instrumentationContext);
        verify(methodAttributeAppender).apply(methodVisitor, methodDescription);
        verifyNoMoreInteractions(methodAttributeAppender);
    }

    @Test
    public void testDefaultValueMethodHeadOnly() throws Exception {
        when(methodDescription.getReturnType()).thenReturn(TypeDescription.STRING);
        when(parameterDescription.hasModifiers()).thenReturn(true);
        when(parameterDescription.isNamed()).thenReturn(true);
        TypeWriter.MethodPool.Entry entry = new TypeWriter.MethodPool.Entry.ForAnnotationDefaultValue(FOO, methodAttributeAppender);
        entry.applyHead(methodVisitor, methodDescription);
        verify(methodVisitor).visitAnnotationDefault();
        verifyNoMoreInteractions(methodVisitor);
        verify(annotationVisitor).visit(null, FOO);
        verify(annotationVisitor).visitEnd();
        verifyNoMoreInteractions(annotationVisitor);
        verifyZeroInteractions(instrumentationContext);
        verifyZeroInteractions(methodAttributeAppender);
    }

    @Test
    public void testDefaultValueMethodBodyOnly() throws Exception {
        when(parameterDescription.hasModifiers()).thenReturn(true);
        when(parameterDescription.isNamed()).thenReturn(true);
        TypeWriter.MethodPool.Entry entry = new TypeWriter.MethodPool.Entry.ForAnnotationDefaultValue(FOO, methodAttributeAppender);
        entry.applyBody(methodVisitor, instrumentationContext, methodDescription);
        verifyZeroInteractions(methodVisitor);
        verifyZeroInteractions(instrumentationContext);
        verify(methodAttributeAppender).apply(methodVisitor, methodDescription);
        verifyNoMoreInteractions(methodAttributeAppender);
    }

    @Test
    public void testDefaultValueMethodWithParameters() throws Exception {
        when(parameterDescription.hasModifiers()).thenReturn(true);
        when(parameterDescription.isNamed()).thenReturn(true);
        when(methodDescription.getReturnType()).thenReturn(TypeDescription.STRING);
        TypeWriter.MethodPool.Entry entry = new TypeWriter.MethodPool.Entry.ForAnnotationDefaultValue(FOO, methodAttributeAppender);
        assertThat(entry.getSort(), is(TypeWriter.MethodPool.Entry.Sort.DEFINE));
        entry.apply(classVisitor, instrumentationContext, methodDescription);
        verify(classVisitor).visitMethod(MODIFIERS, FOO, BAR, QUX, new String[]{BAZ});
        verifyNoMoreInteractions(classVisitor);
        verify(methodVisitor).visitParameter(FOO, MODIFIERS);
        verify(methodVisitor).visitAnnotationDefault();
        verify(methodVisitor).visitEnd();
        verifyNoMoreInteractions(methodVisitor);
        verify(annotationVisitor).visit(null, FOO);
        verify(annotationVisitor).visitEnd();
        verifyNoMoreInteractions(annotationVisitor);
        verifyZeroInteractions(instrumentationContext);
        verify(methodAttributeAppender).apply(methodVisitor, methodDescription);
        verifyNoMoreInteractions(methodAttributeAppender);
    }

    @Test(expected = IllegalStateException.class)
    public void testDefaultValueMethodPrepended() throws Exception {
        new TypeWriter.MethodPool.Entry.ForAnnotationDefaultValue(FOO, methodAttributeAppender).prepend(otherAppender);
    }

    @Test
    public void testImplementedMethod() throws Exception {
        TypeWriter.MethodPool.Entry entry = new TypeWriter.MethodPool.Entry
                .ForImplementation(byteCodeAppender, methodAttributeAppender);
        assertThat(entry.getSort(), is(TypeWriter.MethodPool.Entry.Sort.IMPLEMENT));
        entry.apply(classVisitor, instrumentationContext, methodDescription);
        verify(classVisitor).visitMethod(MODIFIERS, FOO, BAR, QUX, new String[]{BAZ});
        verifyNoMoreInteractions(classVisitor);
        verify(methodVisitor).visitCode();
        verify(methodVisitor).visitMaxs(ONE, TWO);
        verify(methodVisitor).visitEnd();
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(instrumentationContext);
        verify(methodAttributeAppender).apply(methodVisitor, methodDescription);
        verifyNoMoreInteractions(methodAttributeAppender);
        verify(byteCodeAppender).apply(methodVisitor, instrumentationContext, methodDescription);
        verifyNoMoreInteractions(byteCodeAppender);
    }

    @Test
    public void testImplementedMethodHeadOnly() throws Exception {
        when(parameterDescription.hasModifiers()).thenReturn(true);
        when(parameterDescription.isNamed()).thenReturn(true);
        TypeWriter.MethodPool.Entry entry = new TypeWriter.MethodPool.Entry
                .ForImplementation(byteCodeAppender, methodAttributeAppender);
        entry.applyHead(methodVisitor, methodDescription);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(instrumentationContext);
        verifyZeroInteractions(methodAttributeAppender);
        verifyZeroInteractions(byteCodeAppender);
    }

    @Test
    public void testImplementedMethodBodyOnly() throws Exception {
        when(parameterDescription.hasModifiers()).thenReturn(true);
        when(parameterDescription.isNamed()).thenReturn(true);
        TypeWriter.MethodPool.Entry entry = new TypeWriter.MethodPool.Entry
                .ForImplementation(byteCodeAppender, methodAttributeAppender);
        entry.applyBody(methodVisitor, instrumentationContext, methodDescription);
        verify(methodVisitor).visitCode();
        verify(methodVisitor).visitMaxs(ONE, TWO);
        verifyZeroInteractions(instrumentationContext);
        verify(methodAttributeAppender).apply(methodVisitor, methodDescription);
        verifyNoMoreInteractions(methodAttributeAppender);
        verify(byteCodeAppender).apply(methodVisitor, instrumentationContext, methodDescription);
        verifyNoMoreInteractions(byteCodeAppender);
    }

    @Test
    public void testImplementedMethodWithParameters() throws Exception {
        when(parameterDescription.hasModifiers()).thenReturn(true);
        when(parameterDescription.isNamed()).thenReturn(true);
        TypeWriter.MethodPool.Entry entry = new TypeWriter.MethodPool.Entry
                .ForImplementation(byteCodeAppender, methodAttributeAppender);
        assertThat(entry.getSort(), is(TypeWriter.MethodPool.Entry.Sort.IMPLEMENT));
        entry.apply(classVisitor, instrumentationContext, methodDescription);
        verify(classVisitor).visitMethod(MODIFIERS, FOO, BAR, QUX, new String[]{BAZ});
        verifyNoMoreInteractions(classVisitor);
        verify(methodVisitor).visitParameter(FOO, MODIFIERS);
        verify(methodVisitor).visitCode();
        verify(methodVisitor).visitMaxs(ONE, TWO);
        verify(methodVisitor).visitEnd();
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(instrumentationContext);
        verify(methodAttributeAppender).apply(methodVisitor, methodDescription);
        verifyNoMoreInteractions(methodAttributeAppender);
        verify(byteCodeAppender).apply(methodVisitor, instrumentationContext, methodDescription);
        verifyNoMoreInteractions(byteCodeAppender);
    }

    @Test
    public void testImplementedMethodPrepended() throws Exception {
        TypeWriter.MethodPool.Entry entry = new TypeWriter.MethodPool.Entry
                .ForImplementation(byteCodeAppender, methodAttributeAppender)
                .prepend(otherAppender);
        assertThat(entry.getSort(), is(TypeWriter.MethodPool.Entry.Sort.IMPLEMENT));
        entry.apply(classVisitor, instrumentationContext, methodDescription);
        verify(classVisitor).visitMethod(MODIFIERS, FOO, BAR, QUX, new String[]{BAZ});
        verifyNoMoreInteractions(classVisitor);
        verify(methodVisitor).visitCode();
        verify(methodVisitor).visitMaxs(ONE * MULTIPLIER, TWO * MULTIPLIER);
        verify(methodVisitor).visitEnd();
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(instrumentationContext);
        verify(methodAttributeAppender).apply(methodVisitor, methodDescription);
        verifyNoMoreInteractions(methodAttributeAppender);
        verify(byteCodeAppender).apply(methodVisitor, instrumentationContext, methodDescription);
        verifyNoMoreInteractions(byteCodeAppender);
        verify(otherAppender).apply(methodVisitor, instrumentationContext, methodDescription);
        verifyNoMoreInteractions(otherAppender);
    }
}
