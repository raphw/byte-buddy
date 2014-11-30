package net.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.bytecode.stack.Removal;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import net.bytebuddy.instrumentation.method.bytecode.stack.assign.Assigner;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class TargetMethodAnnotationDrivenBinderTerminationHandlerDroppingTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private Assigner assigner;

    @Mock
    private MethodDescription source, target;

    @Mock
    private TypeDescription targetType;

    @Before
    public void setUp() throws Exception {
        when(target.getReturnType()).thenReturn(targetType);
        when(targetType.getStackSize()).thenReturn(StackSize.SINGLE);
    }

    @Test
    public void testApplication() throws Exception {
        StackManipulation stackManipulation = TargetMethodAnnotationDrivenBinder.TerminationHandler.Dropping.INSTANCE
                .resolve(assigner, source, target);
        assertThat(stackManipulation, is((StackManipulation) Removal.SINGLE));
        verify(targetType).getStackSize();
        verifyNoMoreInteractions(targetType);
    }
}
