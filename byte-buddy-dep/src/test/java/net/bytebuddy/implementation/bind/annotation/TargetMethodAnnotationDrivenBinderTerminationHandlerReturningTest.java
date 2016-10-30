package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import net.bytebuddy.test.utility.MockitoRule;
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
    private TypeDescription.Generic genericSourceType, genericTargetType;

    @Mock
    private StackManipulation stackManipulation;

    @Mock
    private AnnotationList annotationList;

    @Before
    public void setUp() throws Exception {
        when(source.getReturnType()).thenReturn(genericSourceType);
        when(target.getReturnType()).thenReturn(genericTargetType);
        when(genericSourceType.asErasure()).thenReturn(sourceType);
        when(genericTargetType.asErasure()).thenReturn(targetType);
        when(assigner.assign(eq(genericTargetType), eq(genericSourceType), any(Assigner.Typing.class))).thenReturn(stackManipulation);
        when(target.getDeclaredAnnotations()).thenReturn(annotationList);
    }

    @Test
    public void testApplication() throws Exception {
        StackManipulation stackManipulation = TargetMethodAnnotationDrivenBinder.TerminationHandler.RETURNING
                .resolve(assigner, source, target);
        assertThat(stackManipulation, is((StackManipulation) new StackManipulation.Compound(this.stackManipulation, MethodReturn.REFERENCE)));
        verify(annotationList).isAnnotationPresent(RuntimeType.class);
    }
}
