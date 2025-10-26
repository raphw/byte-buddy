package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.stubbing.Answer;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Iterator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class AbstractAnnotationBinderTest<T extends Annotation> extends AbstractAnnotationTest<T> {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    protected AnnotationDescription.Loadable<T> annotationDescription;

    protected T annotation;

    @Mock
    protected MethodDescription.InDefinedShape source;

    @Mock
    protected MethodDescription.TypeToken sourceTypeToken;

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

    protected AbstractAnnotationBinderTest(Class<T> annotationType) {
        super(annotationType);
    }

    protected abstract TargetMethodAnnotationDrivenBinder.ParameterBinder<T> getSimpleBinder();

    @Test
    public void testHandledType() throws Exception {
        assertThat(getSimpleBinder().getHandledType(), CoreMatchers.<Class<?>>is(annotationType));
    }

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(sourceDeclaringType.asErasure()).thenReturn(sourceDeclaringType);
        when(targetDeclaringType.asErasure()).thenReturn(targetDeclaringType);
        when(source.getDeclaringType()).thenReturn(sourceDeclaringType);
        annotation = mock(annotationType);
        doReturn(annotationType).when(annotation).annotationType();
        annotationDescription = AnnotationDescription.ForLoadedAnnotation.of(annotation);
        when(assigner.assign(any(TypeDescription.Generic.class), any(TypeDescription.Generic.class), any(Assigner.Typing.class))).thenReturn(stackManipulation);
        when(implementationTarget.getInstrumentedType()).thenReturn(instrumentedType);
        when(implementationTarget.getOriginType()).thenReturn(instrumentedType);
        when(instrumentedType.asErasure()).thenReturn(instrumentedType);
        when(instrumentedType.iterator()).then(new Answer<Iterator<TypeDefinition>>() {
            public Iterator<TypeDefinition> answer(InvocationOnMock invocationOnMock) throws Throwable {
                return Collections.<TypeDefinition>singleton(instrumentedType).iterator();
            }
        });
        when(source.asTypeToken()).thenReturn(sourceTypeToken);
    }
}
