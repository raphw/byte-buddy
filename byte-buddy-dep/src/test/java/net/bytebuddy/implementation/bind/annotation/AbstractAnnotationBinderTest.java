package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeList;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public abstract class AbstractAnnotationBinderTest<T extends Annotation> {

    private final Class<T> annotationType;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    protected AnnotationDescription.Loadable<T> annotationDescription;

    protected T annotation;

    @Mock
    protected MethodDescription.InDeclaredForm source;

    @Mock
    protected ParameterDescription target;

    @Mock
    protected Implementation.Target implementationTarget;

    @Mock
    protected TypeDescription instrumentedType, sourceDeclaringType, targetDeclaringType;

    @Mock
    protected Assigner assigner;

    @Mock
    protected StackManipulation stackManipulation;

    @Mock
    protected ParameterList sourceParameterList;

    @Mock
    protected GenericTypeList sourceTypeList;

    @Mock
    protected TypeList rawSourceTypeList;

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
        when(sourceDeclaringType.asRawType()).thenReturn(sourceDeclaringType);
        when(targetDeclaringType.asRawType()).thenReturn(targetDeclaringType);
        when(source.getDeclaringType()).thenReturn(sourceDeclaringType);
        annotation = mock(annotationType);
        doReturn(annotationType).when(annotation).annotationType();
        annotationDescription = AnnotationDescription.ForLoadedAnnotation.of(annotation);
        when(source.getParameters()).thenReturn(sourceParameterList);
        when(sourceParameterList.asTypeList()).thenReturn(sourceTypeList);
        when(sourceTypeList.asRawTypes()).thenReturn(rawSourceTypeList);
        when(assigner.assign(any(TypeDescription.class), any(TypeDescription.class), anyBoolean())).thenReturn(stackManipulation);
        when(implementationTarget.getTypeDescription()).thenReturn(instrumentedType);
        when(implementationTarget.getOriginType()).thenReturn(instrumentedType);
        when(instrumentedType.asRawType()).thenReturn(instrumentedType);
        when(instrumentedType.iterator()).then(new Answer<Iterator<GenericTypeDescription>>() {
            @Override
            public Iterator<GenericTypeDescription> answer(InvocationOnMock invocationOnMock) throws Throwable {
                return Collections.<GenericTypeDescription>singleton(instrumentedType).iterator();
            }
        });
    }
}
