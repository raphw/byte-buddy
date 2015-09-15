package net.bytebuddy.implementation.attribute;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import net.bytebuddy.utility.RandomString;
import org.junit.Before;
import org.junit.Test;
import org.mockito.asm.Type;

import java.lang.annotation.Annotation;

import static org.mockito.Mockito.*;

public class MethodAttributeAppenderForAnnotationTest extends AbstractMethodAttributeAppenderTest {

    private static final int PARAMETER_INDEX = 0;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        ParameterList<?> parameterList = mock(ParameterList.class);
        when(parameterList.size()).thenReturn(PARAMETER_INDEX + 1);
        when(methodDescription.getParameters()).thenReturn((ParameterList) parameterList);
        when(valueFilter.isRelevant(any(AnnotationDescription.class), any(MethodDescription.InDefinedShape.class))).thenReturn(true);
    }

    @Test
    public void testAnnotationAppenderNoRetention() throws Exception {
        new MethodAttributeAppender.ForAnnotation(new AnnotationList.ForLoadedAnnotation(new Qux.Instance()), valueFilter)
                .apply(methodVisitor, methodDescription);
        verifyZeroInteractions(methodVisitor);
        verifyZeroInteractions(methodDescription);
    }

    @Test
    public void testAnnotationAppenderRuntimeRetention() throws Exception {
        new MethodAttributeAppender.ForAnnotation(new AnnotationList.ForLoadedAnnotation(new Baz.Instance()), valueFilter)
                .apply(methodVisitor, methodDescription);
        verify(methodVisitor).visitAnnotation(Type.getDescriptor(Baz.class), true);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(methodDescription);
    }

    @Test
    public void testAnnotationAppenderByteCodeRetention() throws Exception {
        new MethodAttributeAppender.ForAnnotation(new AnnotationList.ForLoadedAnnotation(new QuxBaz.Instance()), valueFilter)
                .apply(methodVisitor, methodDescription);
        verify(methodVisitor).visitAnnotation(Type.getDescriptor(QuxBaz.class), false);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(methodDescription);
    }

    @Test
    public void testAnnotationAppenderForParameterNoRetention() throws Exception {
        new MethodAttributeAppender.ForAnnotation(PARAMETER_INDEX, new AnnotationList.ForLoadedAnnotation(new Qux.Instance()), valueFilter)
                .apply(methodVisitor, methodDescription);
        verifyZeroInteractions(methodVisitor);
        verify(methodDescription).getParameters();
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    public void testAnnotationAppenderForParameterRuntimeRetention() throws Exception {
        new MethodAttributeAppender.ForAnnotation(PARAMETER_INDEX, new AnnotationList.ForLoadedAnnotation(new Baz.Instance()), valueFilter)
                .apply(methodVisitor, methodDescription);
        verify(methodVisitor).visitParameterAnnotation(PARAMETER_INDEX, Type.getDescriptor(Baz.class), true);
        verifyNoMoreInteractions(methodVisitor);
        verify(methodDescription).getParameters();
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    public void testAnnotationAppenderForParameterByteCodeRetention() throws Exception {
        new MethodAttributeAppender.ForAnnotation(PARAMETER_INDEX, new AnnotationList.ForLoadedAnnotation(new QuxBaz.Instance()), valueFilter)
                .apply(methodVisitor, methodDescription);
        verify(methodVisitor).visitParameterAnnotation(PARAMETER_INDEX, Type.getDescriptor(QuxBaz.class), false);
        verifyNoMoreInteractions(methodVisitor);
        verify(methodDescription).getParameters();
        verifyNoMoreInteractions(methodDescription);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAnnotationAppenderNotEnoughParameters() throws Exception {
        new MethodAttributeAppender.ForAnnotation(PARAMETER_INDEX + 1, new AnnotationList.ForLoadedAnnotation(new Baz.Instance()), valueFilter)
                .apply(methodVisitor, methodDescription);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodAttributeAppender.ForAnnotation.class).create(new ObjectPropertyAssertion.Creator<Annotation>() {
            @Override
            public Annotation create() {
                return new SimpleAnnotation.Instance(RandomString.make());
            }
        }).apply();
        ObjectPropertyAssertion.of(MethodAttributeAppender.ForAnnotation.Target.OnMethod.class).apply();
        ObjectPropertyAssertion.of(MethodAttributeAppender.ForAnnotation.Target.OnMethodParameter.class).apply();
    }

    public @interface SimpleAnnotation {

        String value();

        class Instance implements SimpleAnnotation {

            private final String value;

            public Instance(String value) {
                this.value = value;
            }

            @Override
            public String value() {
                return value;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return SimpleAnnotation.class;
            }
        }
    }
}
