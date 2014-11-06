package net.bytebuddy.instrumentation.attribute;

import net.bytebuddy.instrumentation.type.TypeList;
import net.bytebuddy.utility.HashCodeEqualsTester;
import org.junit.Before;
import org.junit.Test;
import org.mockito.asm.Type;

import java.lang.annotation.Annotation;

import static org.mockito.Mockito.*;

public class MethodAttributeAppenderForAnnotationTest extends AbstractMethodAttributeAppenderTest {

    private static final int PARAMETER_INDEX = 0;

    @Before
    public void setUp() throws Exception {
        TypeList typeList = mock(TypeList.class);
        when(typeList.size()).thenReturn(PARAMETER_INDEX + 1);
        when(methodDescription.getParameterTypes()).thenReturn(typeList);
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
        verify(methodDescription).getParameterTypes();
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    public void testAnnotationAppenderForParameterRuntimeRetention() throws Exception {
        new MethodAttributeAppender.ForAnnotation(PARAMETER_INDEX, new Baz.Instance()).apply(methodVisitor, methodDescription);
        verify(methodVisitor).visitParameterAnnotation(PARAMETER_INDEX, Type.getDescriptor(Baz.class), true);
        verifyNoMoreInteractions(methodVisitor);
        verify(methodDescription).getParameterTypes();
        verifyNoMoreInteractions(methodDescription);
    }

    @Test
    public void testAnnotationAppenderForParameterByteCodeRetention() throws Exception {
        new MethodAttributeAppender.ForAnnotation(PARAMETER_INDEX, new QuxBaz.Instance()).apply(methodVisitor, methodDescription);
        verify(methodVisitor).visitParameterAnnotation(PARAMETER_INDEX, Type.getDescriptor(QuxBaz.class), false);
        verifyNoMoreInteractions(methodVisitor);
        verify(methodDescription).getParameterTypes();
        verifyNoMoreInteractions(methodDescription);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAnnotationAppenderNotEnoughParameters() throws Exception {
        new MethodAttributeAppender.ForAnnotation(PARAMETER_INDEX + 1, new Baz.Instance()).apply(methodVisitor, methodDescription);
    }

    @Test
    public void testHashCodeEquals() throws Exception {
        HashCodeEqualsTester.of(MethodAttributeAppender.ForAnnotation.class).generate(new HashCodeEqualsTester.Generator<Annotation>() {
            @Override
            public Class<? extends Annotation> generate() {
                return SimpleAnnotation.class;
            }
        }).refine(new HashCodeEqualsTester.Refinement<SimpleAnnotation>() {
            @Override
            public void apply(SimpleAnnotation mock) {
                doReturn(SimpleAnnotation.class).when(mock).annotationType();
                when(mock.value()).thenReturn("annotation" + System.identityHashCode(mock));
            }
        }).apply();
    }

    public static @interface SimpleAnnotation {

        String value();
    }
}
