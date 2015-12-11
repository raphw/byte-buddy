package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.implementation.bind.ArgumentTypeResolver;
import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

public class ArgumentBinderTest extends AbstractAnnotationBinderTest<Argument> {

    @Mock
    TypeDescription sourceType, targetType;

    public ArgumentBinderTest() {
        super(Argument.class);
    }

    @Override
    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        super.setUp();
        when(sourceType.asErasure()).thenReturn(sourceType);
        when(sourceType.accept(any(GenericTypeDescription.Visitor.class))).thenReturn(sourceType);
        when(targetType.asErasure()).thenReturn(targetType);
        when(targetType.accept(any(GenericTypeDescription.Visitor.class))).thenReturn(targetType);
    }

    @Override
    protected TargetMethodAnnotationDrivenBinder.ParameterBinder<Argument> getSimpleBinder() {
        return Argument.Binder.INSTANCE;
    }

    @Test
    public void testLegalBindingNoRuntimeTypeUnique() throws Exception {
        assertBinding(false, Argument.BindingMechanic.UNIQUE);
    }

    @Test
    public void testLegalBindingRuntimeTypeUnique() throws Exception {
        RuntimeType runtimeType = mock(RuntimeType.class);
        doReturn(RuntimeType.class).when(runtimeType).annotationType();
        assertBinding(true, Argument.BindingMechanic.UNIQUE, runtimeType);
    }

    @Test
    public void testLegalBindingNoRuntimeTypeAnonymous() throws Exception {
        assertBinding(false, Argument.BindingMechanic.ANONYMOUS);
    }

    @Test
    public void testLegalBindingRuntimeTypeAnonymous() throws Exception {
        RuntimeType runtimeType = mock(RuntimeType.class);
        doReturn(RuntimeType.class).when(runtimeType).annotationType();
        assertBinding(true, Argument.BindingMechanic.ANONYMOUS, runtimeType);
    }

    private void assertBinding(boolean dynamicallyTyped,
                               Argument.BindingMechanic bindingMechanic,
                               Annotation... annotations) throws Exception {
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
        when(target.getType()).thenReturn(targetType);
        when(target.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotation(annotations));
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = Argument.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner);
        assertThat(parameterBinding.isValid(), is(true));
        Object expectedToken = new ArgumentTypeResolver.ParameterIndexToken(sourceIndex);
        if (bindingMechanic == Argument.BindingMechanic.UNIQUE) {
            assertThat(parameterBinding.getIdentificationToken(), equalTo(expectedToken));
            assertThat(parameterBinding.getIdentificationToken().hashCode(), equalTo(expectedToken.hashCode()));
        } else {
            assertThat(parameterBinding.getIdentificationToken(), not(equalTo(expectedToken)));
            assertThat(parameterBinding.getIdentificationToken().hashCode(), not(equalTo(expectedToken.hashCode())));
        }
        verify(annotation, atLeast(1)).value();
        verify(source, atLeast(1)).getParameters();
        verify(target, atLeast(1)).getType();
        verify(target, atLeast(1)).getDeclaredAnnotations();
        verify(assigner).assign(sourceType, targetType, Assigner.Typing.of(dynamicallyTyped));
        verifyNoMoreInteractions(assigner);
    }

    @Test
    public void testIllegalBinding() throws Exception {
        final int sourceIndex = 0, targetIndex = 0;
        when(annotation.value()).thenReturn(sourceIndex);
        when(target.getIndex()).thenReturn(targetIndex);
        when(source.getParameters()).thenReturn(new ParameterList.Empty());
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = Argument.Binder.INSTANCE
                .bind(annotationDescription, source, target, implementationTarget, assigner);
        assertThat(parameterBinding.isValid(), is(false));
        verify(annotation, atLeast(1)).value();
        verify(source, atLeast(1)).getParameters();
        verifyZeroInteractions(assigner);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeAnnotationValue() throws Exception {
        when(annotation.value()).thenReturn(-1);
        Argument.Binder.INSTANCE.bind(annotationDescription, source, target, implementationTarget, assigner);
    }

    @Test
    public void testDefaultArgument() throws Exception {
        Argument argument = new Argument.NextUnboundAsDefaultsProvider.NextUnboundArgumentIterator.DefaultArgument(0);
        Argument loadedArgument = (Argument) Carrier.class.getDeclaredMethod("method", Void.class).getParameterAnnotations()[0][0];
        assertThat(argument, is(loadedArgument));
        assertThat(argument.hashCode(), is(loadedArgument.hashCode()));
        assertThat(argument.toString(), is(loadedArgument.toString()));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(Argument.Binder.class).apply();
        ObjectPropertyAssertion.of(Argument.BindingMechanic.class).apply();
        ObjectPropertyAssertion.of(Argument.NextUnboundAsDefaultsProvider.class).apply();
        ObjectPropertyAssertion.of(Argument.NextUnboundAsDefaultsProvider.NextUnboundArgumentIterator.class).applyBasic();

    }

    private static class Carrier {

        private void method(@Argument(0) Void parameter) {
            /* do nothing */
        }

    }
}
