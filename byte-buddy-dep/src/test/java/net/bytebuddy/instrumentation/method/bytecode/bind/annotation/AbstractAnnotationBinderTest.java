package net.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.assign.Assigner;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.instrumentation.type.TypeList;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.lang.annotation.Annotation;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class AbstractAnnotationBinderTest<T extends Annotation> {

    private final Class<T> annotationType;
    @Rule
    public TestRule mockitoRule = new MockitoRule(this);
    protected T annotation;
    @Mock
    protected MethodDescription source, target;
    @Mock
    protected TypeDescription instrumentedType;
    @Mock
    protected Assigner assigner;
    @Mock
    protected StackManipulation stackManipulation;
    @Mock
    protected TypeList sourceTypeList, targetTypeList;

    protected AbstractAnnotationBinderTest(Class<T> annotationType) {
        this.annotationType = annotationType;
    }

    @Before
    public void setUp() throws Exception {
        annotation = mock(annotationType);
        when(source.getParameterTypes()).thenReturn(sourceTypeList);
        when(target.getParameterTypes()).thenReturn(targetTypeList);
        when(assigner.assign(any(TypeDescription.class), any(TypeDescription.class), anyBoolean()))
                .thenReturn(stackManipulation);
    }
}
