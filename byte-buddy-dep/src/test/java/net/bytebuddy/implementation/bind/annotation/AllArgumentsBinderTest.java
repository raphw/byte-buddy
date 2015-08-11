package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.annotation.Annotation;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
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
        when(targetType.asErasure()).thenReturn(targetType);
    }

    @Override
    protected TargetMethodAnnotationDrivenBinder.ParameterBinder<AllArguments> getSimpleBinder() {
        return AllArguments.Binder.INSTANCE;
    }

    @Test
    public void testLegalStrictBindingRuntimeType() throws Exception {
        when(target.getIndex()).thenReturn(1);
        testLegalStrictBinding(false);
    }

    @Test
    public void testLegalStrictBindingNoRuntimeType() throws Exception {
        when(target.getIndex()).thenReturn(1);
        RuntimeType runtimeType = mock(RuntimeType.class);
        doReturn(RuntimeType.class).when(runtimeType).annotationType();
        when(target.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotation(runtimeType));
        testLegalStrictBinding(true, runtimeType);
    }

    private void testLegalStrictBinding(boolean dynamicallyTyped, Annotation... targetAnnotation) throws Exception {
        when(annotation.value()).thenReturn(AllArguments.Assignment.STRICT);
        when(stackManipulation.isValid()).thenReturn(true);
        when(rawSourceTypeList.iterator()).thenReturn(Arrays.asList(firstSourceType, secondSourceType).iterator());
        when(source.isStatic()).thenReturn(false);
        when(targetType.isArray()).thenReturn(true);
        when(targetType.getComponentType()).thenReturn(componentType);
        when(componentType.getStackSize()).thenReturn(StackSize.SINGLE);
        when(target.getType()).thenReturn(targetType);
        when(target.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotation(targetAnnotation));
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = AllArguments.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner);
        assertThat(parameterBinding.isValid(), is(true));
        verify(source, atLeast(1)).getParameters();
        verify(source, atLeast(1)).isStatic();
        verify(target, atLeast(1)).getType();
        verify(target, atLeast(1)).getDeclaredAnnotations();
        verify(assigner).assign(firstSourceType, componentType, Assigner.Typing.of(dynamicallyTyped));
        verify(assigner).assign(secondSourceType, componentType, Assigner.Typing.of(dynamicallyTyped));
        verifyNoMoreInteractions(assigner);
    }

    @Test
    public void testIllegalBinding() throws Exception {
        when(target.getIndex()).thenReturn(1);
        when(annotation.value()).thenReturn(AllArguments.Assignment.STRICT);
        when(stackManipulation.isValid()).thenReturn(false);
        when(rawSourceTypeList.iterator()).thenReturn(Arrays.asList(firstSourceType, secondSourceType).iterator());
        when(source.isStatic()).thenReturn(false);
        when(targetType.isArray()).thenReturn(true);
        when(targetType.getComponentType()).thenReturn(componentType);
        when(componentType.getStackSize()).thenReturn(StackSize.SINGLE);
        when(target.getType()).thenReturn(targetType);
        when(target.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = AllArguments.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner);
        assertThat(parameterBinding.isValid(), is(false));
        verify(source, atLeast(1)).getParameters();
        verify(source, atLeast(1)).isStatic();
        verify(target, atLeast(1)).getType();
        verify(target, atLeast(1)).getDeclaredAnnotations();
        verify(assigner).assign(firstSourceType, componentType, Assigner.Typing.STATIC);
        verifyNoMoreInteractions(assigner);
    }

    @Test
    public void testLegalSlackBinding() throws Exception {
        when(target.getIndex()).thenReturn(1);
        when(annotation.value()).thenReturn(AllArguments.Assignment.SLACK);
        when(stackManipulation.isValid()).thenReturn(false);
        when(rawSourceTypeList.iterator()).thenReturn(Arrays.asList(firstSourceType, secondSourceType).iterator());
        when(source.isStatic()).thenReturn(false);
        when(targetType.isArray()).thenReturn(true);
        when(targetType.getComponentType()).thenReturn(componentType);
        when(componentType.getStackSize()).thenReturn(StackSize.SINGLE);
        when(target.getType()).thenReturn(targetType);
        when(target.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        when(componentType.getInternalName()).thenReturn(FOO);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = AllArguments.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner);
        MethodVisitor methodVisitor = mock(MethodVisitor.class);
        Implementation.Context implementationContext = mock(Implementation.Context.class);
        StackManipulation.Size size = parameterBinding.apply(methodVisitor, implementationContext);
        assertThat(size.getSizeImpact(), is(1));
        assertThat(size.getMaximalSize(), is(1));
        verify(methodVisitor).visitInsn(Opcodes.ICONST_0);
        verify(methodVisitor).visitTypeInsn(Opcodes.ANEWARRAY, FOO);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(implementationContext);
        assertThat(parameterBinding.isValid(), is(true));
        verify(source, atLeast(1)).getParameters();
        verify(source, atLeast(1)).isStatic();
        verify(target, atLeast(1)).getType();
        verify(target, atLeast(1)).getDeclaredAnnotations();
        verify(assigner).assign(firstSourceType, componentType, Assigner.Typing.STATIC);
        verify(assigner).assign(secondSourceType, componentType, Assigner.Typing.STATIC);
        verifyNoMoreInteractions(assigner);
    }

    @Test(expected = IllegalStateException.class)
    public void testNonArrayTypeBinding() throws Exception {
        when(target.getIndex()).thenReturn(0);
        TypeDescription targetType = mock(TypeDescription.class);
        when(targetType.isArray()).thenReturn(false);
        when(target.getType()).thenReturn(targetType);
        when(targetType.asErasure()).thenReturn(targetType);
        AllArguments.Binder.INSTANCE.bind(annotationDescription, source, target, implementationTarget, assigner);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AllArguments.Assignment.class).apply();
        ObjectPropertyAssertion.of(AllArguments.Binder.class).apply();
    }
}
