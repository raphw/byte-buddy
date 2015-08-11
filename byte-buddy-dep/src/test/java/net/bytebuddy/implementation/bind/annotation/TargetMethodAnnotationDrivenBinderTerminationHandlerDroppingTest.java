package net.bytebuddy.implementation.bind.annotation;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.Removal;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
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
        when(targetType.asErasure()).thenReturn(targetType);
        when(targetType.getStackSize()).thenReturn(StackSize.SINGLE);
    }

    @Test
    public void testApplication() throws Exception {
        StackManipulation stackManipulation = TargetMethodAnnotationDrivenBinder.TerminationHandler.Dropping.INSTANCE.resolve(assigner, source, target);
        assertThat(stackManipulation, is((StackManipulation) Removal.SINGLE));
        verify(targetType).getStackSize();
        verify(targetType).asErasure();
        verifyNoMoreInteractions(targetType);
    }
}
