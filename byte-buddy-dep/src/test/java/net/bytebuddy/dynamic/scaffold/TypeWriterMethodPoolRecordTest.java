package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.annotation.AnnotationValue;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.attribute.AnnotationValueFilter;
import net.bytebuddy.implementation.attribute.MethodAttributeAppender;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class TypeWriterMethodPoolRecordTest {

    private static final int MODIFIERS = 42, ONE = 1, TWO = 2, MULTIPLIER = 4;

    private static final String FOO = "foo", BAR = "bar", QUX = "qux", BAZ = "baz";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodAttributeAppender methodAttributeAppender;

    @Mock
    private ClassVisitor classVisitor;

    @Mock
    private Implementation.Context implementationContext;

    @Mock
    private MethodVisitor methodVisitor;

    @Mock
    private AnnotationVisitor annotationVisitor;

    @Mock
    private MethodDescription methodDescription, bridgeTarget;

    @Mock
    private TypeDescription superClass;

    @Mock
    private ByteCodeAppender byteCodeAppender, otherAppender;

    @Mock
    private TypeList.Generic exceptionTypes;

    @Mock
    private TypeList rawExceptionTypes;

    @Mock
    private ParameterDescription parameterDescription;

    @Mock
    private TypeWriter.MethodPool.Record delegate;

    @Mock
    private TypeDescription instrumentedType;

    @Mock
    private MethodDescription.TypeToken typeToken;

    @Mock
    private AnnotationValueFilter valueFilter;

    @Mock
    private AnnotationValueFilter.Factory annotationValueFilterFactory;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(methodDescription.getInternalName()).thenReturn(FOO);
        when(methodDescription.getDescriptor()).thenReturn(BAR);
        when(methodDescription.getGenericSignature()).thenReturn(QUX);
        when(methodDescription.getExceptionTypes()).thenReturn(exceptionTypes);
        when(methodDescription.getActualModifiers(anyBoolean(), any(Visibility.class))).thenReturn(MODIFIERS);
        when(exceptionTypes.asErasures()).thenReturn(rawExceptionTypes);
        when(rawExceptionTypes.toInternalNames()).thenReturn(new String[]{BAZ});
        when(classVisitor.visitMethod(MODIFIERS, FOO, BAR, QUX, new String[]{BAZ})).thenReturn(methodVisitor);
        when(methodDescription.getParameters()).thenReturn((ParameterList) new ParameterList.Explicit<ParameterDescription>(parameterDescription));
        when(parameterDescription.getName()).thenReturn(FOO);
        when(parameterDescription.getModifiers()).thenReturn(MODIFIERS);
        when(methodVisitor.visitAnnotationDefault()).thenReturn(annotationVisitor);
        when(byteCodeAppender.apply(methodVisitor, implementationContext, methodDescription))
                .thenReturn(new ByteCodeAppender.Size(ONE, TWO));
        when(otherAppender.apply(methodVisitor, implementationContext, methodDescription))
                .thenReturn(new ByteCodeAppender.Size(ONE * MULTIPLIER, TWO * MULTIPLIER));
        when(annotationValueFilterFactory.on(methodDescription)).thenReturn(valueFilter);
        when(methodDescription.getVisibility()).thenReturn(Visibility.PUBLIC);
    }

    @Test
    public void testSkippedMethod() throws Exception {
        assertThat(TypeWriter.MethodPool.Record.ForNonDefinedMethod.INSTANCE.getSort(), is(TypeWriter.MethodPool.Record.Sort.SKIPPED));
        TypeWriter.MethodPool.Record.ForNonDefinedMethod.INSTANCE.apply(classVisitor, implementationContext, annotationValueFilterFactory);
        verifyZeroInteractions(classVisitor);
        verifyZeroInteractions(implementationContext);
        verifyZeroInteractions(methodAttributeAppender);
    }

    @Test(expected = IllegalStateException.class)
    public void testSkippedMethodCannotGetImplementedMethod() throws Exception {
        TypeWriter.MethodPool.Record.ForNonDefinedMethod.INSTANCE.getMethod();
    }

    @Test(expected = IllegalStateException.class)
    public void testSkippedMethodCannotBePrepended() throws Exception {
        TypeWriter.MethodPool.Record.ForNonDefinedMethod.INSTANCE.prepend(byteCodeAppender);
    }

    @Test(expected = IllegalStateException.class)
    public void testSkippedMethodCannotApplyBody() throws Exception {
        TypeWriter.MethodPool.Record.ForNonDefinedMethod.INSTANCE.applyBody(methodVisitor, implementationContext, annotationValueFilterFactory);
    }

    @Test(expected = IllegalStateException.class)
    public void testSkippedMethodCannotApplyHead() throws Exception {
        TypeWriter.MethodPool.Record.ForNonDefinedMethod.INSTANCE.applyHead(methodVisitor);
    }

    @Test
    public void testDefinedMethod() throws Exception {
        TypeWriter.MethodPool.Record record = new TypeWriter.MethodPool.Record.ForDefinedMethod.WithoutBody(methodDescription,
                methodAttributeAppender,
                Visibility.PUBLIC);
        assertThat(record.getSort(), is(TypeWriter.MethodPool.Record.Sort.DEFINED));
        record.apply(classVisitor, implementationContext, annotationValueFilterFactory);
        verify(classVisitor).visitMethod(MODIFIERS, FOO, BAR, QUX, new String[]{BAZ});
        verifyNoMoreInteractions(classVisitor);
        verify(methodVisitor).visitEnd();
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(implementationContext);
        verify(methodAttributeAppender).apply(methodVisitor, methodDescription, valueFilter);
        verifyNoMoreInteractions(methodAttributeAppender);
    }

    @Test
    public void testDefinedMethodHeadOnly() throws Exception {
        when(parameterDescription.hasModifiers()).thenReturn(true);
        when(parameterDescription.isNamed()).thenReturn(true);
        TypeWriter.MethodPool.Record record = new TypeWriter.MethodPool.Record.ForDefinedMethod.WithoutBody(methodDescription,
                methodAttributeAppender,
                Visibility.PUBLIC);
        record.applyHead(methodVisitor);
        verifyZeroInteractions(methodVisitor);
        verifyZeroInteractions(implementationContext);
        verifyZeroInteractions(methodAttributeAppender);
    }

    @Test
    public void testDefinedMethodBodyOnly() throws Exception {
        when(parameterDescription.hasModifiers()).thenReturn(true);
        when(parameterDescription.isNamed()).thenReturn(true);
        TypeWriter.MethodPool.Record record = new TypeWriter.MethodPool.Record.ForDefinedMethod.WithoutBody(methodDescription,
                methodAttributeAppender,
                Visibility.PUBLIC);
        record.applyBody(methodVisitor, implementationContext, annotationValueFilterFactory);
        verifyZeroInteractions(methodVisitor);
        verifyZeroInteractions(implementationContext);
        verify(methodAttributeAppender).apply(methodVisitor, methodDescription, valueFilter);
    }

    @Test
    public void testDefinedMethodWithParameters() throws Exception {
        when(parameterDescription.hasModifiers()).thenReturn(true);
        when(parameterDescription.isNamed()).thenReturn(true);
        TypeWriter.MethodPool.Record record = new TypeWriter.MethodPool.Record.ForDefinedMethod.WithoutBody(methodDescription,
                methodAttributeAppender,
                Visibility.PUBLIC);
        assertThat(record.getSort(), is(TypeWriter.MethodPool.Record.Sort.DEFINED));
        record.apply(classVisitor, implementationContext, annotationValueFilterFactory);
        verify(classVisitor).visitMethod(MODIFIERS, FOO, BAR, QUX, new String[]{BAZ});
        verifyNoMoreInteractions(classVisitor);
        verify(methodVisitor).visitParameter(FOO, MODIFIERS);
        verify(methodVisitor).visitEnd();
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(implementationContext);
        verify(methodAttributeAppender).apply(methodVisitor, methodDescription, valueFilter);
        verifyNoMoreInteractions(methodAttributeAppender);
    }

    @Test(expected = IllegalStateException.class)
    public void testDefinedMethodPrepended() throws Exception {
        new TypeWriter.MethodPool.Record.ForDefinedMethod.WithoutBody(methodDescription,
                methodAttributeAppender,
                Visibility.PUBLIC).prepend(otherAppender);
    }

    @Test
    public void testDefaultValueMethod() throws Exception {
        when(methodDescription.getReturnType()).thenReturn(new TypeDescription.Generic.OfNonGenericType.ForLoadedType(String.class));
        when(methodDescription.isDefaultValue(AnnotationValue.ForConstant.of(FOO))).thenReturn(true);
        TypeWriter.MethodPool.Record record = new TypeWriter.MethodPool.Record.ForDefinedMethod.WithAnnotationDefaultValue(methodDescription,
                AnnotationValue.ForConstant.of(FOO),
                methodAttributeAppender);
        assertThat(record.getSort(), is(TypeWriter.MethodPool.Record.Sort.DEFINED));
        record.apply(classVisitor, implementationContext, annotationValueFilterFactory);
        verify(classVisitor).visitMethod(MODIFIERS, FOO, BAR, QUX, new String[]{BAZ});
        verifyNoMoreInteractions(classVisitor);
        verify(methodVisitor).visitAnnotationDefault();
        verify(methodVisitor).visitEnd();
        verifyNoMoreInteractions(methodVisitor);
        verify(annotationVisitor).visit(null, FOO);
        verify(annotationVisitor).visitEnd();
        verifyNoMoreInteractions(annotationVisitor);
        verifyZeroInteractions(implementationContext);
        verify(methodAttributeAppender).apply(methodVisitor, methodDescription, valueFilter);
        verifyNoMoreInteractions(methodAttributeAppender);
    }

    @Test
    public void testDefaultValueMethodHeadOnly() throws Exception {
        when(methodDescription.getReturnType()).thenReturn(new TypeDescription.Generic.OfNonGenericType.ForLoadedType(String.class));
        when(parameterDescription.hasModifiers()).thenReturn(true);
        when(parameterDescription.isNamed()).thenReturn(true);
        when(methodDescription.isDefaultValue(AnnotationValue.ForConstant.of(FOO))).thenReturn(true);
        TypeWriter.MethodPool.Record record = new TypeWriter.MethodPool.Record.ForDefinedMethod.WithAnnotationDefaultValue(methodDescription,
                AnnotationValue.ForConstant.of(FOO),
                methodAttributeAppender);
        record.applyHead(methodVisitor);
        verify(methodVisitor).visitAnnotationDefault();
        verifyNoMoreInteractions(methodVisitor);
        verify(annotationVisitor).visit(null, FOO);
        verify(annotationVisitor).visitEnd();
        verifyNoMoreInteractions(annotationVisitor);
        verifyZeroInteractions(implementationContext);
        verifyZeroInteractions(methodAttributeAppender);
    }

    @Test
    public void testDefaultValueMethodBodyOnly() throws Exception {
        when(parameterDescription.hasModifiers()).thenReturn(true);
        when(parameterDescription.isNamed()).thenReturn(true);
        TypeWriter.MethodPool.Record record = new TypeWriter.MethodPool.Record.ForDefinedMethod.WithAnnotationDefaultValue(methodDescription,
                AnnotationValue.ForConstant.of(FOO),
                methodAttributeAppender);
        record.applyBody(methodVisitor, implementationContext, annotationValueFilterFactory);
        verifyZeroInteractions(methodVisitor);
        verifyZeroInteractions(implementationContext);
        verify(methodAttributeAppender).apply(methodVisitor, methodDescription, valueFilter);
        verifyNoMoreInteractions(methodAttributeAppender);
    }

    @Test
    public void testDefaultValueMethodWithParameters() throws Exception {
        when(parameterDescription.hasModifiers()).thenReturn(true);
        when(parameterDescription.isNamed()).thenReturn(true);
        when(methodDescription.getReturnType()).thenReturn(new TypeDescription.Generic.OfNonGenericType.ForLoadedType(String.class));
        when(methodDescription.isDefaultValue(AnnotationValue.ForConstant.of(FOO))).thenReturn(true);
        TypeWriter.MethodPool.Record record = new TypeWriter.MethodPool.Record.ForDefinedMethod.WithAnnotationDefaultValue(methodDescription,
                AnnotationValue.ForConstant.of(FOO),
                methodAttributeAppender);
        assertThat(record.getSort(), is(TypeWriter.MethodPool.Record.Sort.DEFINED));
        record.apply(classVisitor, implementationContext, annotationValueFilterFactory);
        verify(classVisitor).visitMethod(MODIFIERS, FOO, BAR, QUX, new String[]{BAZ});
        verifyNoMoreInteractions(classVisitor);
        verify(methodVisitor).visitParameter(FOO, MODIFIERS);
        verify(methodVisitor).visitAnnotationDefault();
        verify(methodVisitor).visitEnd();
        verifyNoMoreInteractions(methodVisitor);
        verify(annotationVisitor).visit(null, FOO);
        verify(annotationVisitor).visitEnd();
        verifyNoMoreInteractions(annotationVisitor);
        verifyZeroInteractions(implementationContext);
        verify(methodAttributeAppender).apply(methodVisitor, methodDescription, valueFilter);
        verifyNoMoreInteractions(methodAttributeAppender);
    }

    @Test(expected = IllegalStateException.class)
    public void testDefaultValueMethodPrepended() throws Exception {
        new TypeWriter.MethodPool.Record.ForDefinedMethod.WithAnnotationDefaultValue(methodDescription,
                AnnotationValue.ForConstant.of(FOO),
                methodAttributeAppender).prepend(otherAppender);
    }

    @Test(expected = IllegalStateException.class)
    public void testNoDefaultValue() throws Exception {
        when(methodDescription.isDefaultValue(AnnotationValue.ForConstant.of(FOO))).thenReturn(false);
        new TypeWriter.MethodPool.Record.ForDefinedMethod.WithAnnotationDefaultValue(methodDescription,
                AnnotationValue.ForConstant.of(FOO),
                methodAttributeAppender).apply(classVisitor, implementationContext, annotationValueFilterFactory);
    }

    @Test
    public void testImplementedMethod() throws Exception {
        TypeWriter.MethodPool.Record record = new TypeWriter.MethodPool.Record.ForDefinedMethod.WithBody(methodDescription,
                byteCodeAppender,
                methodAttributeAppender,
                Visibility.PUBLIC);
        assertThat(record.getSort(), is(TypeWriter.MethodPool.Record.Sort.IMPLEMENTED));
        record.apply(classVisitor, implementationContext, annotationValueFilterFactory);
        verify(classVisitor).visitMethod(MODIFIERS, FOO, BAR, QUX, new String[]{BAZ});
        verifyNoMoreInteractions(classVisitor);
        verify(methodVisitor).visitCode();
        verify(methodVisitor).visitMaxs(ONE, TWO);
        verify(methodVisitor).visitEnd();
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(implementationContext);
        verify(methodAttributeAppender).apply(methodVisitor, methodDescription, valueFilter);
        verifyNoMoreInteractions(methodAttributeAppender);
        verify(byteCodeAppender).apply(methodVisitor, implementationContext, methodDescription);
        verifyNoMoreInteractions(byteCodeAppender);
    }

    @Test
    public void testImplementedMethodHeadOnly() throws Exception {
        when(parameterDescription.hasModifiers()).thenReturn(true);
        when(parameterDescription.isNamed()).thenReturn(true);
        TypeWriter.MethodPool.Record record = new TypeWriter.MethodPool.Record.ForDefinedMethod.WithBody(methodDescription,
                byteCodeAppender,
                methodAttributeAppender,
                Visibility.PUBLIC);
        record.applyHead(methodVisitor);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(implementationContext);
        verifyZeroInteractions(methodAttributeAppender);
        verifyZeroInteractions(byteCodeAppender);
    }

    @Test
    public void testImplementedMethodBodyOnly() throws Exception {
        when(parameterDescription.hasModifiers()).thenReturn(true);
        when(parameterDescription.isNamed()).thenReturn(true);
        TypeWriter.MethodPool.Record record = new TypeWriter.MethodPool.Record.ForDefinedMethod.WithBody(methodDescription,
                byteCodeAppender,
                methodAttributeAppender,
                Visibility.PUBLIC);
        record.applyBody(methodVisitor, implementationContext, annotationValueFilterFactory);
        verify(methodVisitor).visitCode();
        verify(methodVisitor).visitMaxs(ONE, TWO);
        verifyZeroInteractions(implementationContext);
        verify(methodAttributeAppender).apply(methodVisitor, methodDescription, valueFilter);
        verifyNoMoreInteractions(methodAttributeAppender);
        verify(byteCodeAppender).apply(methodVisitor, implementationContext, methodDescription);
        verifyNoMoreInteractions(byteCodeAppender);
    }

    @Test
    public void testImplementedMethodWithParameters() throws Exception {
        when(parameterDescription.hasModifiers()).thenReturn(true);
        when(parameterDescription.isNamed()).thenReturn(true);
        TypeWriter.MethodPool.Record record = new TypeWriter.MethodPool.Record.ForDefinedMethod.WithBody(methodDescription,
                byteCodeAppender,
                methodAttributeAppender,
                Visibility.PUBLIC);
        assertThat(record.getSort(), is(TypeWriter.MethodPool.Record.Sort.IMPLEMENTED));
        record.apply(classVisitor, implementationContext, annotationValueFilterFactory);
        verify(classVisitor).visitMethod(MODIFIERS, FOO, BAR, QUX, new String[]{BAZ});
        verifyNoMoreInteractions(classVisitor);
        verify(methodVisitor).visitParameter(FOO, MODIFIERS);
        verify(methodVisitor).visitCode();
        verify(methodVisitor).visitMaxs(ONE, TWO);
        verify(methodVisitor).visitEnd();
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(implementationContext);
        verify(methodAttributeAppender).apply(methodVisitor, methodDescription, valueFilter);
        verifyNoMoreInteractions(methodAttributeAppender);
        verify(byteCodeAppender).apply(methodVisitor, implementationContext, methodDescription);
        verifyNoMoreInteractions(byteCodeAppender);
    }

    @Test
    public void testImplementedMethodPrepended() throws Exception {
        TypeWriter.MethodPool.Record record = new TypeWriter.MethodPool.Record.ForDefinedMethod.WithBody(methodDescription,
                byteCodeAppender,
                methodAttributeAppender,
                Visibility.PUBLIC)
                .prepend(otherAppender);
        assertThat(record.getSort(), is(TypeWriter.MethodPool.Record.Sort.IMPLEMENTED));
        record.apply(classVisitor, implementationContext, annotationValueFilterFactory);
        verify(classVisitor).visitMethod(MODIFIERS, FOO, BAR, QUX, new String[]{BAZ});
        verifyNoMoreInteractions(classVisitor);
        verify(methodVisitor).visitCode();
        verify(methodVisitor).visitMaxs(ONE * MULTIPLIER, TWO * MULTIPLIER);
        verify(methodVisitor).visitEnd();
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(implementationContext);
        verify(methodAttributeAppender).apply(methodVisitor, methodDescription, valueFilter);
        verifyNoMoreInteractions(methodAttributeAppender);
        verify(byteCodeAppender).apply(methodVisitor, implementationContext, methodDescription);
        verifyNoMoreInteractions(byteCodeAppender);
        verify(otherAppender).apply(methodVisitor, implementationContext, methodDescription);
        verifyNoMoreInteractions(otherAppender);
    }

    @Test
    public void testVisibilityBridgeProperties() throws Exception {
        TypeWriter.MethodPool.Record record = new TypeWriter.MethodPool.Record.ForDefinedMethod.OfVisibilityBridge(methodDescription,
                bridgeTarget,
                superClass,
                methodAttributeAppender);
        assertThat(record.getSort(), is(TypeWriter.MethodPool.Record.Sort.IMPLEMENTED));
    }

    @Test
    public void testVisibilityBridgePrepending() throws Exception {
        assertThat(new TypeWriter.MethodPool.Record.ForDefinedMethod.OfVisibilityBridge(methodDescription,
                bridgeTarget,
                superClass,
                methodAttributeAppender).prepend(byteCodeAppender), instanceOf(TypeWriter.MethodPool.Record.ForDefinedMethod.WithBody.class));
    }

    @Test
    public void testAccessorBridgeProperties() throws Exception {
        when(delegate.getSort()).thenReturn(TypeWriter.MethodPool.Record.Sort.IMPLEMENTED);
        TypeWriter.MethodPool.Record record = new TypeWriter.MethodPool.Record.AccessBridgeWrapper(delegate,
                instrumentedType,
                bridgeTarget,
                Collections.singleton(typeToken),
                methodAttributeAppender);
        assertThat(record.getSort(), is(TypeWriter.MethodPool.Record.Sort.IMPLEMENTED));
    }

    @Test
    public void testAccessorBridgeBodyApplication() throws Exception {
        new TypeWriter.MethodPool.Record.AccessBridgeWrapper(delegate,
                instrumentedType,
                bridgeTarget,
                Collections.singleton(typeToken),
                methodAttributeAppender).applyBody(methodVisitor, implementationContext, annotationValueFilterFactory);
        verify(delegate).applyBody(methodVisitor, implementationContext, annotationValueFilterFactory);
        verifyNoMoreInteractions(delegate);
        verifyZeroInteractions(methodVisitor);
    }

    @Test
    public void testAccessorBridgeHeadApplication() throws Exception {
        new TypeWriter.MethodPool.Record.AccessBridgeWrapper(delegate,
                instrumentedType,
                bridgeTarget,
                Collections.singleton(typeToken),
                methodAttributeAppender).applyHead(methodVisitor);
        verify(delegate).applyHead(methodVisitor);
        verifyNoMoreInteractions(delegate);
        verifyZeroInteractions(methodVisitor);
    }

    @Test
    public void testAccessorBridgePrepending() throws Exception {
        assertThat(new TypeWriter.MethodPool.Record.AccessBridgeWrapper(delegate,
                instrumentedType,
                bridgeTarget,
                Collections.singleton(typeToken),
                methodAttributeAppender).prepend(byteCodeAppender), instanceOf(TypeWriter.MethodPool.Record.AccessBridgeWrapper.class));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeWriter.MethodPool.Record.ForDefinedMethod.WithBody.class).refine(new ObjectPropertyAssertion.Refinement<MethodDescription>() {
            @Override
            public void apply(MethodDescription mock) {
                when(mock.getVisibility()).thenReturn(Visibility.PUBLIC);
            }
        }).apply();
        ObjectPropertyAssertion.of(TypeWriter.MethodPool.Record.ForDefinedMethod.OfVisibilityBridge.class).apply();
        ObjectPropertyAssertion.of(TypeWriter.MethodPool.Record.ForDefinedMethod.WithoutBody.class).apply();
        ObjectPropertyAssertion.of(TypeWriter.MethodPool.Record.ForDefinedMethod.WithAnnotationDefaultValue.class).apply();
        ObjectPropertyAssertion.of(TypeWriter.MethodPool.Record.ForNonDefinedMethod.class).apply();
        ObjectPropertyAssertion.of(TypeWriter.MethodPool.Record.AccessBridgeWrapper.class).apply();
    }
}
