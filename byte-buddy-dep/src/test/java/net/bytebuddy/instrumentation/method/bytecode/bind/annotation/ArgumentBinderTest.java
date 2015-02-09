package net.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import net.bytebuddy.instrumentation.attribute.annotation.AnnotationList;
import net.bytebuddy.instrumentation.method.bytecode.bind.ArgumentTypeResolver;
import net.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import org.junit.Test;
import org.mockito.Mock;

import java.lang.annotation.Annotation;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.*;

public class ArgumentBinderTest extends AbstractAnnotationBinderTest<Argument> {

    @Mock
    TypeDescription sourceType, targetType;
    @Mock
    private TypeList sourceParameters, targetParameters;

    public ArgumentBinderTest() {
        super(Argument.class);
    }

    @Override
    protected TargetMethodAnnotationDrivenBinder.ParameterBinder<Argument> getSimpleBinder() {
        return Argument.Binder.INSTANCE;
    }

    @Test
    public void testLegalBindingNoRuntimeTypeUnique() throws Exception {
        assertBinding(new Annotation[2][0], false, Argument.BindingMechanic.UNIQUE);
    }

    @Test
    public void testLegalBindingRuntimeTypeUnique() throws Exception {
        RuntimeType runtimeType = mock(RuntimeType.class);
        doReturn(RuntimeType.class).when(runtimeType).annotationType();
        assertBinding(new Annotation[][]{{}, {runtimeType}}, true, Argument.BindingMechanic.UNIQUE);
    }

    @Test
    public void testLegalBindingNoRuntimeTypeAnonymous() throws Exception {
        assertBinding(new Annotation[2][0], false, Argument.BindingMechanic.ANONYMOUS);
    }

    @Test
    public void testLegalBindingRuntimeTypeAnonymous() throws Exception {
        RuntimeType runtimeType = mock(RuntimeType.class);
        doReturn(RuntimeType.class).when(runtimeType).annotationType();
        assertBinding(new Annotation[][]{{}, {runtimeType}}, true, Argument.BindingMechanic.ANONYMOUS);
    }

    private void assertBinding(Annotation[][] annotations,
                               boolean dynamicallyTyped,
                               Argument.BindingMechanic bindingMechanic) throws Exception {
        final int sourceIndex = 2, targetIndex = 1;
        when(stackManipulation.isValid()).thenReturn(true);
        when(annotation.value()).thenReturn(sourceIndex);
        when(annotation.bindingMechanic()).thenReturn(bindingMechanic);
        when(sourceParameters.size()).thenReturn(sourceIndex + 1);
        when(sourceParameters.get(sourceIndex)).thenReturn(sourceType);
        when(source.getParameterTypes()).thenReturn(sourceParameters);
        when(source.isStatic()).thenReturn(false);
        when(targetParameters.size()).thenReturn(targetIndex + 1);
        when(targetParameters.get(targetIndex)).thenReturn(targetType);
        when(target.getParameterTypes()).thenReturn(targetParameters);
        when(target.getParameterAnnotations()).thenReturn(AnnotationList.ForLoadedAnnotation.asList(annotations));
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = Argument.Binder.INSTANCE
                .bind(annotationDescription, targetIndex, source, target, instrumentationTarget, assigner);
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
        verify(source, atLeast(1)).getParameterTypes();
        verify(target, atLeast(1)).getParameterTypes();
        verify(target, atLeast(1)).getParameterAnnotations();
        verify(assigner).assign(sourceType, targetType, dynamicallyTyped);
        verifyNoMoreInteractions(assigner);
    }

    @Test
    public void testIllegalBinding() throws Exception {
        final int sourceIndex = 0, targetIndex = 0;
        when(annotation.value()).thenReturn(sourceIndex);
        TypeList typeList = mock(TypeList.class);
        when(typeList.size()).thenReturn(0);
        when(source.getParameterTypes()).thenReturn(typeList);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = Argument.Binder.INSTANCE
                .bind(annotationDescription, targetIndex, source, target, instrumentationTarget, assigner);
        assertThat(parameterBinding.isValid(), is(false));
        verify(annotation, atLeast(1)).value();
        verify(source, atLeast(1)).getParameterTypes();
        verifyZeroInteractions(assigner);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeAnnotationValue() throws Exception {
        when(annotation.value()).thenReturn(-1);
        Argument.Binder.INSTANCE.bind(annotationDescription, 0, source, target, instrumentationTarget, assigner);
    }

    @Test
    public void testDefaultArgument() throws Exception {
        Argument argument = new Argument.NextUnboundAsDefaultsProvider.NextUnboundArgumentIterator.DefaultArgument(0);
        Argument loadedArgument = (Argument) Carrier.class.getDeclaredMethod("method", Void.class).getParameterAnnotations()[0][0];
        assertThat(argument, is(loadedArgument));
        assertThat(argument.hashCode(), is(loadedArgument.hashCode()));
        assertThat(argument.toString(), is(loadedArgument.toString()));
    }

    private static class Carrier {

        private void method(@Argument(0) Void parameter) {
            /* do nothing */
        }

    }
}
