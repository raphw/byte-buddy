package net.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import net.bytebuddy.instrumentation.attribute.annotation.AnnotationList;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.assign.Assigner;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TargetMethodAnnotationDrivenBinderTerminationHandlerReturningTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private Assigner assigner;

    @Mock
    private MethodDescription source, target;

    @Mock
    private TypeDescription sourceType, targetType;

    @Mock
    private StackManipulation stackManipulation;

    @Mock
    private AnnotationList annotationList;

    @Before
    public void setUp() throws Exception {
        when(source.getReturnType()).thenReturn(sourceType);
        when(target.getReturnType()).thenReturn(targetType);
        when(assigner.assign(eq(targetType), eq(sourceType), any(boolean.class))).thenReturn(stackManipulation);
        when(target.getDeclaredAnnotations()).thenReturn(annotationList);
    }

    @Test
    public void testApplication() throws Exception {
        StackManipulation stackManipulation = TargetMethodAnnotationDrivenBinder.TerminationHandler.Returning.INSTANCE
                .resolve(assigner, source, target);
        assertThat(stackManipulation, is(this.stackManipulation));
        verify(annotationList).isAnnotationPresent(RuntimeType.class);
    }
}
