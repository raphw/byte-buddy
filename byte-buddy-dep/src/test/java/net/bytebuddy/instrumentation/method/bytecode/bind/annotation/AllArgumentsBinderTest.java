package net.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import net.bytebuddy.instrumentation.type.TypeDescription;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.annotation.Annotation;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class AllArgumentsBinderTest extends AbstractAnnotationBinderTest<AllArguments> {

    private static final String FOO = "foo";

    @Mock
    private TypeDescription firstSourceType, secondSourceType;
    @Mock
    private TypeDescription targetType, componentType;

    public AllArgumentsBinderTest() {
        super(AllArguments.class);
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        when(firstSourceType.getStackSize()).thenReturn(StackSize.SINGLE);
        when(secondSourceType.getStackSize()).thenReturn(StackSize.SINGLE);
    }

    @Test
    public void testAnnotationType() throws Exception {
        assertEquals(AllArguments.class, AllArguments.Binder.INSTANCE.getHandledType());
    }

    @Test
    public void testLegalStrictBindingRuntimeType() throws Exception {
        testLegalStrictBinding(new Annotation[2][0], false);
    }

    @Test
    public void testLegalStrictBindingNoRuntimeType() throws Exception {
        RuntimeType runtimeType = mock(RuntimeType.class);
        doReturn(RuntimeType.class).when(runtimeType).annotationType();
        when(target.getParameterAnnotations()).thenReturn(new Annotation[][]{{}, {runtimeType}});
        testLegalStrictBinding(new Annotation[][]{{}, {runtimeType}}, true);
    }

    private void testLegalStrictBinding(Annotation[][] targetAnnotations, boolean considerRuntimeType) throws Exception {
        when(annotation.value()).thenReturn(AllArguments.Assignment.STRICT);
        when(stackManipulation.isValid()).thenReturn(true);
        when(sourceTypeList.iterator()).thenReturn(Arrays.asList(firstSourceType, secondSourceType).iterator());
        when(source.isStatic()).thenReturn(false);
        when(targetType.isArray()).thenReturn(true);
        when(targetType.getComponentType()).thenReturn(componentType);
        when(componentType.getStackSize()).thenReturn(StackSize.SINGLE);
        when(targetTypeList.get(1)).thenReturn(targetType);
        when(targetTypeList.size()).thenReturn(2);
        when(target.getParameterAnnotations()).thenReturn(targetAnnotations);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = AllArguments.Binder.INSTANCE
                .bind(annotation, 1, source, target, instrumentationTarget, assigner);
        assertThat(parameterBinding.isValid(), is(true));
        verify(source, atLeast(1)).getParameterTypes();
        verify(source, atLeast(1)).isStatic();
        verify(target, atLeast(1)).getParameterTypes();
        verify(target, atLeast(1)).getParameterAnnotations();
        verify(assigner).assign(firstSourceType, componentType, considerRuntimeType);
        verify(assigner).assign(secondSourceType, componentType, considerRuntimeType);
        verifyNoMoreInteractions(assigner);
    }

    @Test
    public void testIllegalBinding() throws Exception {
        when(annotation.value()).thenReturn(AllArguments.Assignment.STRICT);
        when(stackManipulation.isValid()).thenReturn(false);
        when(sourceTypeList.iterator()).thenReturn(Arrays.asList(firstSourceType, secondSourceType).iterator());
        when(source.isStatic()).thenReturn(false);
        when(targetType.isArray()).thenReturn(true);
        when(targetType.getComponentType()).thenReturn(componentType);
        when(componentType.getStackSize()).thenReturn(StackSize.SINGLE);
        when(targetTypeList.get(1)).thenReturn(targetType);
        when(targetTypeList.size()).thenReturn(2);
        when(target.getParameterAnnotations()).thenReturn(new Annotation[2][0]);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = AllArguments.Binder.INSTANCE
                .bind(annotation, 1, source, target, instrumentationTarget, assigner);
        assertThat(parameterBinding.isValid(), is(false));
        verify(source, atLeast(1)).getParameterTypes();
        verify(source, atLeast(1)).isStatic();
        verify(target, atLeast(1)).getParameterTypes();
        verify(target, atLeast(1)).getParameterAnnotations();
        verify(assigner).assign(firstSourceType, componentType, false);
        verifyNoMoreInteractions(assigner);
    }

    @Test
    public void testLegalSlackBinding() throws Exception {
        when(annotation.value()).thenReturn(AllArguments.Assignment.SLACK);
        when(stackManipulation.isValid()).thenReturn(false);
        when(sourceTypeList.iterator()).thenReturn(Arrays.asList(firstSourceType, secondSourceType).iterator());
        when(source.isStatic()).thenReturn(false);
        when(targetType.isArray()).thenReturn(true);
        when(targetType.getComponentType()).thenReturn(componentType);
        when(componentType.getStackSize()).thenReturn(StackSize.SINGLE);
        when(targetTypeList.get(1)).thenReturn(targetType);
        when(targetTypeList.size()).thenReturn(2);
        when(target.getParameterAnnotations()).thenReturn(new Annotation[2][0]);
        when(componentType.getInternalName()).thenReturn(FOO);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = AllArguments.Binder.INSTANCE
                .bind(annotation, 1, source, target, instrumentationTarget, assigner);
        MethodVisitor methodVisitor = mock(MethodVisitor.class);
        Instrumentation.Context instrumentationContext = mock(Instrumentation.Context.class);
        StackManipulation.Size size = parameterBinding.apply(methodVisitor, instrumentationContext);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));
        verify(methodVisitor).visitInsn(Opcodes.ICONST_0);
        verify(methodVisitor).visitTypeInsn(Opcodes.ANEWARRAY, FOO);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(instrumentationContext);
        assertThat(parameterBinding.isValid(), is(true));
        verify(source, atLeast(1)).getParameterTypes();
        verify(source, atLeast(1)).isStatic();
        verify(target, atLeast(1)).getParameterTypes();
        verify(target, atLeast(1)).getParameterAnnotations();
        verify(assigner).assign(firstSourceType, componentType, false);
        verify(assigner).assign(secondSourceType, componentType, false);
        verifyNoMoreInteractions(assigner);
    }

    @Test(expected = IllegalStateException.class)
    public void testNonArrayTypeBinding() throws Exception {
        TypeDescription targetType = mock(TypeDescription.class);
        when(targetType.isArray()).thenReturn(false);
        when(targetTypeList.get(0)).thenReturn(targetType);
        AllArguments.Binder.INSTANCE.bind(annotation, 0, source, target, instrumentationTarget, assigner);
    }
}
