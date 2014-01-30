package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.InstrumentedType;
import org.junit.Before;

import java.lang.annotation.Annotation;

import static org.mockito.Mockito.mock;

public abstract class AbstractAnnotationBinderTest<T extends Annotation> {

    private final Class<T> annotationType;

    protected AbstractAnnotationBinderTest(Class<T> annotationType) {
        this.annotationType = annotationType;
    }

    protected T annotation;
    protected MethodDescription source;
    protected MethodDescription target;
    protected InstrumentedType typeDescription;
    protected Assigner assigner;

    @Before
    public void setUp() throws Exception {
        annotation = mock(annotationType);
        source = mock(MethodDescription.class);
        target = mock(MethodDescription.class);
        typeDescription = mock(InstrumentedType.class);
        assigner = mock(Assigner.class);
    }
}
