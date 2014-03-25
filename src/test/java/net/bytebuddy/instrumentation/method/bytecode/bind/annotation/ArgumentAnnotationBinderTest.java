package net.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import net.bytebuddy.instrumentation.method.bytecode.bind.MethodDelegationBinder;
import net.bytebuddy.instrumentation.method.bytecode.bind.MostSpecificTypeResolver;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import org.junit.Test;
import org.mockito.Mock;

import java.lang.annotation.Annotation;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class ArgumentAnnotationBinderTest extends AbstractAnnotationBinderTest<Argument> {

    @Mock
    private TypeList sourceParameters, targetParameters;
    @Mock
    TypeDescription sourceType, targetType;

    public ArgumentAnnotationBinderTest() {
        super(Argument.class);
    }

    @Test
    public void testAnnotationType() throws Exception {
        assertEquals(Argument.class, Argument.Binder.INSTANCE.getHandledType());
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
                               boolean considerRuntimeType,
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
        when(target.getParameterAnnotations()).thenReturn(annotations);
        MethodDelegationBinder.ParameterBinding<?> parameterBinding = Argument.Binder.INSTANCE
                .bind(annotation, targetIndex, source, target, instrumentedType, assigner);
        assertThat(parameterBinding.isValid(), is(true));
        Object expectedToken = new MostSpecificTypeResolver.ParameterIndexToken(sourceIndex);
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
        verify(assigner).assign(sourceType, targetType, considerRuntimeType);
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
                .bind(annotation, targetIndex, source, target, instrumentedType, assigner);
        assertThat(parameterBinding.isValid(), is(false));
        verify(annotation, atLeast(1)).value();
        verify(source, atLeast(1)).getParameterTypes();
        verifyZeroInteractions(assigner);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeAnnotationValue() throws Exception {
        when(annotation.value()).thenReturn(-1);
        Argument.Binder.INSTANCE.bind(annotation, 0, source, target, instrumentedType, assigner);
    }
}
