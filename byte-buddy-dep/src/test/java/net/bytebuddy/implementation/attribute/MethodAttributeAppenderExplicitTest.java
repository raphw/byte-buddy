package net.bytebuddy.implementation.attribute;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.objectweb.asm.Type;

import java.lang.annotation.Annotation;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class MethodAttributeAppenderExplicitTest extends AbstractMethodAttributeAppenderTest {

    private static final int PARAMETER_INDEX = 0;

    @Mock
    private MethodDescription methodDescription;

    @Mock
    private ParameterDescription parameterDescription;

    @Before
    @Override
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        super.setUp();
        when(methodDescription.getParameters()).thenReturn((ParameterList) new ParameterList.Explicit<ParameterDescription>(parameterDescription));
        when(annotationValueFilter.isRelevant(any(AnnotationDescription.class), any(MethodDescription.InDefinedShape.class))).thenReturn(true);
    }

    @Test
    public void testAnnotationAppenderNoRetention() throws Exception {
        new MethodAttributeAppender.Explicit(new AnnotationList.ForLoadedAnnotations(new Qux.Instance()))
                .apply(methodVisitor, methodDescription, annotationValueFilter);
        verifyNoMoreInteractions(methodVisitor);
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    public void testAnnotationAppenderRuntimeRetention() throws Exception {
        new MethodAttributeAppender.Explicit(new AnnotationList.ForLoadedAnnotations(new Baz.Instance()))
                .apply(methodVisitor, methodDescription, annotationValueFilter);
        verify(methodVisitor).visitAnnotation(Type.getDescriptor(Baz.class), true);
        verifyNoMoreInteractions(methodVisitor);
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    public void testAnnotationAppenderByteCodeRetention() throws Exception {
        new MethodAttributeAppender.Explicit(new AnnotationList.ForLoadedAnnotations(new QuxBaz.Instance()))
                .apply(methodVisitor, methodDescription, annotationValueFilter);
        verify(methodVisitor).visitAnnotation(Type.getDescriptor(QuxBaz.class), false);
        verifyNoMoreInteractions(methodVisitor);
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    public void testAnnotationAppenderForParameterNoRetention() throws Exception {
        new MethodAttributeAppender.Explicit(PARAMETER_INDEX, new AnnotationList.ForLoadedAnnotations(new Qux.Instance()))
                .apply(methodVisitor, methodDescription, annotationValueFilter);
        verifyNoMoreInteractions(methodVisitor);
        verify(methodDescription).getParameters();
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    public void testAnnotationAppenderForParameterRuntimeRetention() throws Exception {
        new MethodAttributeAppender.Explicit(PARAMETER_INDEX, new AnnotationList.ForLoadedAnnotations(new Baz.Instance()))
                .apply(methodVisitor, methodDescription, annotationValueFilter);
        verify(methodVisitor).visitParameterAnnotation(PARAMETER_INDEX, Type.getDescriptor(Baz.class), true);
        verifyNoMoreInteractions(methodVisitor);
        verify(methodDescription).getParameters();
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    public void testAnnotationAppenderForParameterByteCodeRetention() throws Exception {
        new MethodAttributeAppender.Explicit(PARAMETER_INDEX, new AnnotationList.ForLoadedAnnotations(new QuxBaz.Instance()))
                .apply(methodVisitor, methodDescription, annotationValueFilter);
        verify(methodVisitor).visitParameterAnnotation(PARAMETER_INDEX, Type.getDescriptor(QuxBaz.class), false);
        verifyNoMoreInteractions(methodVisitor);
        verify(methodDescription).getParameters();
        verifyNoMoreInteractions(methodDescription);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAnnotationAppenderNotEnoughParameters() throws Exception {
        new MethodAttributeAppender.Explicit(PARAMETER_INDEX + 1, new AnnotationList.ForLoadedAnnotations(new Baz.Instance()))
                .apply(methodVisitor, methodDescription, annotationValueFilter);
    }

    @Test
    public void testFactory() throws Exception {
        MethodAttributeAppender.Explicit methodAttributeAppender = new MethodAttributeAppender.Explicit(new AnnotationList.ForLoadedAnnotations(new Qux.Instance()));
        assertThat(methodAttributeAppender.make(instrumentedType), sameInstance((MethodAttributeAppender) methodAttributeAppender));
        verifyNoMoreInteractions(instrumentedType);
    }

    @Test
    public void testOfMethod() throws Exception {
        when(methodDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Baz.Instance()));
        when(parameterDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Baz.Instance()));
        MethodAttributeAppender methodAttributeAppender = MethodAttributeAppender.Explicit.of(methodDescription).make(instrumentedType);
        methodAttributeAppender.apply(methodVisitor, methodDescription, annotationValueFilter);
        verify(methodVisitor).visitAnnotation(Type.getDescriptor(Baz.class), true);
        verify(methodVisitor).visitParameterAnnotation(PARAMETER_INDEX, Type.getDescriptor(Baz.class), true);
        verifyNoMoreInteractions(methodVisitor);
        verify(methodDescription, times(2)).getParameters();
        verify(methodDescription).getDeclaredAnnotations();
        verifyNoMoreInteractions(methodDescription);
        verify(parameterDescription).getIndex();
        verify(parameterDescription).getDeclaredAnnotations();
        verifyNoMoreInteractions(parameterDescription);
        verifyNoMoreInteractions(instrumentedType);
    }

    @Test
    public void testOfMethodAnnotations() throws Exception {
        when(methodDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Baz.Instance()));
        when(parameterDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Baz.Instance()));
        MethodAttributeAppender methodAttributeAppender = MethodAttributeAppender.Explicit.ofMethodAnnotations(methodDescription).make(instrumentedType);
        methodAttributeAppender.apply(methodVisitor, methodDescription, annotationValueFilter);
        verify(methodVisitor).visitAnnotation(Type.getDescriptor(Baz.class), true);
        verify(methodVisitor, times(0)).visitParameterAnnotation(anyInt(), anyString(), anyBoolean());
        verifyNoMoreInteractions(methodVisitor);
        verify(methodDescription, times(0)).getParameters();
        verify(methodDescription).getDeclaredAnnotations();
        verifyNoMoreInteractions(methodDescription);
        verifyNoMoreInteractions(parameterDescription);
        verifyNoMoreInteractions(instrumentedType);
    }

    @Test
    public void testOfParameterAnnotations() throws Exception {
        when(methodDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Baz.Instance()));
        when(parameterDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.ForLoadedAnnotations(new Baz.Instance()));
        MethodAttributeAppender methodAttributeAppender = MethodAttributeAppender.Explicit.ofParameterAnnotations(methodDescription).make(instrumentedType);
        methodAttributeAppender.apply(methodVisitor, methodDescription, annotationValueFilter);
        verify(methodVisitor, times(0)).visitAnnotation(anyString(), anyBoolean());
        verify(methodVisitor).visitParameterAnnotation(PARAMETER_INDEX, Type.getDescriptor(Baz.class), true);
        verifyNoMoreInteractions(methodVisitor);
        verify(methodDescription, times(2)).getParameters();
        verify(methodDescription, times(0)).getDeclaredAnnotations();
        verifyNoMoreInteractions(methodDescription);
        verify(parameterDescription).getIndex();
        verify(parameterDescription).getDeclaredAnnotations();
        verifyNoMoreInteractions(parameterDescription);
        verifyNoMoreInteractions(instrumentedType);
    }

    public @interface SimpleAnnotation {

        String value();

        class Instance implements SimpleAnnotation {

            private final String value;

            public Instance(String value) {
                this.value = value;
            }

            public String value() {
                return value;
            }

            public Class<? extends Annotation> annotationType() {
                return SimpleAnnotation.class;
            }
        }
    }
}
