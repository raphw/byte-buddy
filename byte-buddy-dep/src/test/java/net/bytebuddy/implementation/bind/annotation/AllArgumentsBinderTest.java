package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.method.ParameterList;
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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class AllArgumentsBinderTest extends AbstractAnnotationBinderTest<AllArguments> {

    private static final String FOO = "foo";

    @Mock
    private TypeDescription.Generic firstSourceType, secondSourceType, genericInstrumentedType;

    @Mock
    private TypeDescription rawTargetType, rawComponentType;

    @Mock
    private TypeDescription.Generic targetType, componentType;

    public AllArgumentsBinderTest() {
        super(AllArguments.class);
    }

    @Override
    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        super.setUp();
        when(firstSourceType.getStackSize()).thenReturn(StackSize.SINGLE);
        when(secondSourceType.getStackSize()).thenReturn(StackSize.SINGLE);
        when(componentType.asErasure()).thenReturn(rawComponentType);
        when(targetType.getComponentType()).thenReturn(componentType);
        when(targetType.asErasure()).thenReturn(rawTargetType);
        when(firstSourceType.asGenericType()).thenReturn(firstSourceType);
        when(firstSourceType.accept(any(TypeDescription.Generic.Visitor.class))).thenReturn(firstSourceType);
        when(secondSourceType.asGenericType()).thenReturn(secondSourceType);
        when(secondSourceType.accept(any(TypeDescription.Generic.Visitor.class))).thenReturn(secondSourceType);
    }

    @Override
    protected TargetMethodAnnotationDrivenBinder.ParameterBinder<AllArguments> getSimpleBinder() {
        return AllArguments.Binder.INSTANCE;
    }

    @Test
    public void testLegalStrictBindingRuntimeType() throws Exception {
        when(target.getIndex()).thenReturn(1);
        testLegalStrictBinding(Assigner.Typing.STATIC);
    }

    @Test
    public void testLegalStrictBindingNoRuntimeType() throws Exception {
        when(target.getIndex()).thenReturn(1);
        RuntimeType runtimeType = mock(RuntimeType.class);
        doReturn(RuntimeType.class).when(runtimeType).annotationType();
        when(target.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(runtimeType));
        testLegalStrictBinding(Assigner.Typing.DYNAMIC);
    }

    private void testLegalStrictBinding(Assigner.Typing typing) throws Exception {
        when(annotation.value()).thenReturn(AllArguments.Assignment.STRICT);
        when(stackManipulation.isValid()).thenReturn(true);
        when(source.getParameters()).thenReturn(new ParameterList.Explicit.ForTypes(source, firstSourceType, secondSourceType));
        when(source.isStatic()).thenReturn(false);
        when(targetType.isArray()).thenReturn(true);
        when(targetType.getComponentType()).thenReturn(componentType);
        when(componentType.getStackSize()).thenReturn(StackSize.SINGLE);
        when(target.getType()).thenReturn(targetType);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = AllArguments.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner, typing);
        assertThat(parameterBinding.isValid(), is(true));
        verify(source, atLeast(1)).getParameters();
        verify(source, atLeast(1)).isStatic();
        verify(target, atLeast(1)).getType();
        verify(target, never()).getDeclaredAnnotations();
        verify(assigner).assign(firstSourceType, componentType, typing);
        verify(assigner).assign(secondSourceType, componentType, typing);
        verifyNoMoreInteractions(assigner);
    }

    @Test
    public void testIllegalBinding() throws Exception {
        when(target.getIndex()).thenReturn(1);
        when(annotation.value()).thenReturn(AllArguments.Assignment.STRICT);
        when(stackManipulation.isValid()).thenReturn(false);
        when(source.getParameters()).thenReturn(new ParameterList.Explicit.ForTypes(source, firstSourceType, secondSourceType));
        when(source.isStatic()).thenReturn(false);
        when(targetType.isArray()).thenReturn(true);
        when(targetType.getComponentType()).thenReturn(componentType);
        when(componentType.getStackSize()).thenReturn(StackSize.SINGLE);
        when(target.getType()).thenReturn(targetType);
        when(target.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = AllArguments.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner, Assigner.Typing.STATIC);
        assertThat(parameterBinding.isValid(), is(false));
        verify(source, atLeast(1)).getParameters();
        verify(source, atLeast(1)).isStatic();
        verify(target, atLeast(1)).getType();
        verify(target, never()).getDeclaredAnnotations();
        verify(assigner).assign(firstSourceType, componentType, Assigner.Typing.STATIC);
        verifyNoMoreInteractions(assigner);
    }

    @Test
    public void testLegalSlackBinding() throws Exception {
        when(target.getIndex()).thenReturn(1);
        when(annotation.value()).thenReturn(AllArguments.Assignment.SLACK);
        when(stackManipulation.isValid()).thenReturn(false);
        when(source.getParameters()).thenReturn(new ParameterList.Explicit.ForTypes(source, firstSourceType, secondSourceType));
        when(source.isStatic()).thenReturn(false);
        when(targetType.isArray()).thenReturn(true);
        when(targetType.getComponentType()).thenReturn(componentType);
        when(componentType.getStackSize()).thenReturn(StackSize.SINGLE);
        when(target.getType()).thenReturn(targetType);
        when(target.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        when(rawComponentType.getInternalName()).thenReturn(FOO);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = AllArguments.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner, Assigner.Typing.STATIC);
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
        verify(target, never()).getDeclaredAnnotations();
        verify(assigner).assign(firstSourceType, componentType, Assigner.Typing.STATIC);
        verify(assigner).assign(secondSourceType, componentType, Assigner.Typing.STATIC);
        verifyNoMoreInteractions(assigner);
    }

    @Test(expected = IllegalStateException.class)
    public void testNonArrayTypeBinding() throws Exception {
        when(target.getIndex()).thenReturn(0);
        TypeDescription.Generic targetType = mock(TypeDescription.Generic.class);
        TypeDescription rawTargetType = mock(TypeDescription.class);
        when(targetType.asErasure()).thenReturn(rawTargetType);
        when(targetType.isArray()).thenReturn(false);
        when(target.getType()).thenReturn(targetType);
        AllArguments.Binder.INSTANCE.bind(annotationDescription, source, target, implementationTarget, assigner, Assigner.Typing.STATIC);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AllArguments.Assignment.class).apply();
        ObjectPropertyAssertion.of(AllArguments.Binder.class).apply();
    }
}
