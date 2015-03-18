package net.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.attribute.annotation.AnnotationDescription;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.ParameterDescription;
import net.bytebuddy.instrumentation.method.ParameterList;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.assign.Assigner;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.lang.annotation.Annotation;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.*;

public abstract class AbstractAnnotationBinderTest<T extends Annotation> {

    private final Class<T> annotationType;
    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    protected AnnotationDescription.Loadable<T> annotationDescription;

    protected T annotation;

    @Mock
    protected MethodDescription source;
    @Mock
    protected ParameterDescription target;

    @Mock
    protected Instrumentation.Target instrumentationTarget;
    @Mock
    protected TypeDescription instrumentedType;
    @Mock
    protected Assigner assigner;
    @Mock
    protected StackManipulation stackManipulation;
    @Mock
    protected ParameterList sourceParameterList;

    @Mock
    protected TypeList sourceTypeList;

    protected AbstractAnnotationBinderTest(Class<T> annotationType) {
        this.annotationType = annotationType;
    }

    protected abstract TargetMethodAnnotationDrivenBinder.ParameterBinder<T> getSimpleBinder();

    @Test
    public void testHandledType() throws Exception {
        assertEquals(annotationType, getSimpleBinder().getHandledType());
    }

    @Before
    public void setUp() throws Exception {
        annotation = mock(annotationType);
        doReturn(annotationType).when(annotation).annotationType();
        annotationDescription = AnnotationDescription.ForLoadedAnnotation.of(annotation);
        when(source.getParameters()).thenReturn(sourceParameterList);
        when(sourceParameterList.asTypeList()).thenReturn(sourceTypeList);
        when(assigner.assign(any(TypeDescription.class), any(TypeDescription.class), anyBoolean()))
                .thenReturn(stackManipulation);
        when(instrumentationTarget.getTypeDescription()).thenReturn(instrumentedType);
        when(instrumentationTarget.getOriginType()).thenReturn(instrumentedType);
    }
}
