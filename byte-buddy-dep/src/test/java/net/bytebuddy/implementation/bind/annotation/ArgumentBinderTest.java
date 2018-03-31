package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bind.ArgumentTypeResolver;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class ArgumentBinderTest extends AbstractAnnotationBinderTest<Argument> {

    @Mock
    private TypeDescription sourceType, targetType;

    @Mock
    private TypeDescription.Generic genericSourceType, genericTargetType;

    public ArgumentBinderTest() {
        super(Argument.class);
    }

    @Override
    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        super.setUp();
        when(genericSourceType.asErasure()).thenReturn(sourceType);
        when(sourceType.asGenericType()).thenReturn(genericSourceType);
        when(genericSourceType.accept(any(TypeDescription.Generic.Visitor.class))).thenReturn(genericSourceType);
        when(genericTargetType.asErasure()).thenReturn(targetType);
        when(targetType.asGenericType()).thenReturn(genericTargetType);
        when(genericTargetType.accept(any(TypeDescription.Generic.Visitor.class))).thenReturn(targetType);
    }

    @Override
    protected TargetMethodAnnotationDrivenBinder.ParameterBinder<Argument> getSimpleBinder() {
        return Argument.Binder.INSTANCE;
    }

    @Test
    public void testLegalBindingNoRuntimeTypeUnique() throws Exception {
        assertBinding(Assigner.Typing.STATIC, Argument.BindingMechanic.UNIQUE);
    }

    @Test
    public void testLegalBindingRuntimeTypeUnique() throws Exception {
        assertBinding(Assigner.Typing.DYNAMIC, Argument.BindingMechanic.UNIQUE);
    }

    @Test
    public void testLegalBindingNoRuntimeTypeAnonymous() throws Exception {
        assertBinding(Assigner.Typing.STATIC, Argument.BindingMechanic.ANONYMOUS);
    }

    @Test
    public void testLegalBindingRuntimeTypeAnonymous() throws Exception {
        assertBinding(Assigner.Typing.DYNAMIC, Argument.BindingMechanic.ANONYMOUS);
    }

    private void assertBinding(Assigner.Typing typing, Argument.BindingMechanic bindingMechanic) throws Exception {
        final int sourceIndex = 2;
        when(stackManipulation.isValid()).thenReturn(true);
        when(annotation.value()).thenReturn(sourceIndex);
        when(annotation.bindingMechanic()).thenReturn(bindingMechanic);
        List<TypeDescription> typeDescriptions = new ArrayList<TypeDescription>(sourceIndex + 1);
        for (int i = 0; i < sourceIndex; i++) {
            TypeDescription typeDescription = mock(TypeDescription.class);
            when(typeDescription.getStackSize()).thenReturn(StackSize.ZERO);
            typeDescriptions.add(i, typeDescription);
        }
        when(sourceType.getStackSize()).thenReturn(StackSize.ZERO);
        typeDescriptions.add(sourceIndex, sourceType);
        when(source.getParameters()).thenReturn(new ParameterList.Explicit.ForTypes(source, typeDescriptions));
        when(source.isStatic()).thenReturn(false);
        when(target.getType()).thenReturn(genericTargetType);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = Argument.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner, typing);
        assertThat(parameterBinding.isValid(), is(true));
        Object expectedToken = new ArgumentTypeResolver.ParameterIndexToken(sourceIndex);
        if (bindingMechanic == Argument.BindingMechanic.UNIQUE) {
            assertThat(parameterBinding.getIdentificationToken(), is(expectedToken));
            assertThat(parameterBinding.getIdentificationToken().hashCode(), is(expectedToken.hashCode()));
        } else {
            assertThat(parameterBinding.getIdentificationToken(), not(expectedToken));
            assertThat(parameterBinding.getIdentificationToken().hashCode(), not(expectedToken.hashCode()));
        }
        verify(annotation, atLeast(1)).value();
        verify(source, atLeast(1)).getParameters();
        verify(target, atLeast(1)).getType();
        verify(target, never()).getDeclaredAnnotations();
        verify(assigner).assign(genericSourceType, genericTargetType, typing);
        verifyNoMoreInteractions(assigner);
    }

    @Test
    public void testIllegalBinding() throws Exception {
        final int sourceIndex = 0, targetIndex = 0;
        when(annotation.value()).thenReturn(sourceIndex);
        when(target.getIndex()).thenReturn(targetIndex);
        when(source.getParameters()).thenReturn(new ParameterList.Empty<ParameterDescription.InDefinedShape>());
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = Argument.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner, Assigner.Typing.STATIC);
        assertThat(parameterBinding.isValid(), is(false));
        verify(annotation, atLeast(1)).value();
        verify(source, atLeast(1)).getParameters();
        verifyZeroInteractions(assigner);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeAnnotationValue() throws Exception {
        when(annotation.value()).thenReturn(-1);
        Argument.Binder.INSTANCE.bind(annotationDescription, source, target, implementationTarget, assigner, Assigner.Typing.STATIC);
    }

    @SuppressWarnings("unused")
    private static class Carrier {

        private void method(@Argument(0) Void parameter) {
            /* do nothing */
        }
    }
}
