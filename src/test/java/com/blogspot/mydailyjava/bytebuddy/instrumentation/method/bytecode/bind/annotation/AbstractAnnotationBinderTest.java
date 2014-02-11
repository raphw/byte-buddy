package com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.Assigner;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.assign.Assignment;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.InstrumentedType0;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeList;
import org.junit.Before;

import java.lang.annotation.Annotation;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class AbstractAnnotationBinderTest<T extends Annotation> {

    private final Class<T> annotationType;

    protected AbstractAnnotationBinderTest(Class<T> annotationType) {
        this.annotationType = annotationType;
    }

    protected T annotation;
    protected MethodDescription source;
    protected MethodDescription target;
    protected InstrumentedType0 typeDescription;
    protected Assigner assigner;
    protected Assignment assignment;
    protected TypeList sourceTypeList;
    protected TypeList targetTypeList;

    @Before
    public void setUp() throws Exception {
        annotation = mock(annotationType);
        source = mock(MethodDescription.class);
        target = mock(MethodDescription.class);
        typeDescription = mock(InstrumentedType0.class);
        assigner = mock(Assigner.class);
        assignment = mock(Assignment.class);
        sourceTypeList = mock(TypeList.class);
        targetTypeList = mock(TypeList.class);
        when(source.getParameterTypes()).thenReturn(sourceTypeList);
        when(target.getParameterTypes()).thenReturn(targetTypeList);
        when(assigner.assign(any(TypeDescription.class), any(TypeDescription.class), anyBoolean())).thenReturn(assignment);
    }
}
