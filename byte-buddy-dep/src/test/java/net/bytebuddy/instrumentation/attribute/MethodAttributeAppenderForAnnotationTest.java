package net.bytebuddy.instrumentation.attribute;

import net.bytebuddy.instrumentation.method.ParameterList;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Test;
import org.mockito.asm.Type;

import java.lang.annotation.Annotation;

import static org.mockito.Mockito.*;

public class MethodAttributeAppenderForAnnotationTest extends AbstractMethodAttributeAppenderTest {

    private static final int PARAMETER_INDEX = 0;

    @Before
    public void setUp() throws Exception {
        ParameterList parameterList = mock(ParameterList.class);
        when(parameterList.size()).thenReturn(PARAMETER_INDEX + 1);
        when(methodDescription.getParameters()).thenReturn(parameterList);
    }

    @Test
    public void testAnnotationAppenderNoRetention() throws Exception {
        new MethodAttributeAppender.ForAnnotation(new Qux.Instance()).apply(methodVisitor, methodDescription);
        verifyZeroInteractions(methodVisitor);
        verifyZeroInteractions(methodDescription);
    }

    @Test
    public void testAnnotationAppenderRuntimeRetention() throws Exception {
        new MethodAttributeAppender.ForAnnotation(new Baz.Instance()).apply(methodVisitor, methodDescription);
        verify(methodVisitor).visitAnnotation(Type.getDescriptor(Baz.class), true);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(methodDescription);
    }

    @Test
    public void testAnnotationAppenderByteCodeRetention() throws Exception {
        new MethodAttributeAppender.ForAnnotation(new QuxBaz.Instance()).apply(methodVisitor, methodDescription);
        verify(methodVisitor).visitAnnotation(Type.getDescriptor(QuxBaz.class), false);
        verifyNoMoreInteractions(methodVisitor);
        verifyZeroInteractions(methodDescription);
    }

    @Test
    public void testAnnotationAppenderForParameterNoRetention() throws Exception {
        new MethodAttributeAppender.ForAnnotation(PARAMETER_INDEX, new Qux.Instance()).apply(methodVisitor, methodDescription);
        verifyZeroInteractions(methodVisitor);
        verify(methodDescription).getParameters();
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    public void testAnnotationAppenderForParameterRuntimeRetention() throws Exception {
        new MethodAttributeAppender.ForAnnotation(PARAMETER_INDEX, new Baz.Instance()).apply(methodVisitor, methodDescription);
        verify(methodVisitor).visitParameterAnnotation(PARAMETER_INDEX, Type.getDescriptor(Baz.class), true);
        verifyNoMoreInteractions(methodVisitor);
        verify(methodDescription).getParameters();
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    public void testAnnotationAppenderForParameterByteCodeRetention() throws Exception {
        new MethodAttributeAppender.ForAnnotation(PARAMETER_INDEX, new QuxBaz.Instance()).apply(methodVisitor, methodDescription);
        verify(methodVisitor).visitParameterAnnotation(PARAMETER_INDEX, Type.getDescriptor(QuxBaz.class), false);
        verifyNoMoreInteractions(methodVisitor);
        verify(methodDescription).getParameters();
        verifyNoMoreInteractions(methodDescription);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAnnotationAppenderNotEnoughParameters() throws Exception {
        new MethodAttributeAppender.ForAnnotation(PARAMETER_INDEX + 1, new Baz.Instance()).apply(methodVisitor, methodDescription);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodAttributeAppender.ForAnnotation.class).generate(new ObjectPropertyAssertion.Generator<Annotation>() {
            @Override
            public Class<? extends Annotation> generate() {
                return SimpleAnnotation.class;
            }
        }).refine(new ObjectPropertyAssertion.Refinement<SimpleAnnotation>() {
            @Override
            public void apply(SimpleAnnotation mock) {
                doReturn(SimpleAnnotation.class).when(mock).annotationType();
                when(mock.value()).thenReturn("annotation" + System.identityHashCode(mock));
            }
        }).apply();
        ObjectPropertyAssertion.of(MethodAttributeAppender.ForAnnotation.Target.OnMethodParameter.class).apply();
    }

    public static @interface SimpleAnnotation {

        String value();
    }
}
