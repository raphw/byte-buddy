package net.bytebuddy.implementation.bind;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.Removal;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.implementation.bytecode.member.MethodReturn;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import static net.bytebuddy.test.utility.FieldByFieldComparison.hasPrototype;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class MethodDelegationBinderTerminationHandlerTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

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

    @Test
    public void testDropping() throws Exception {
        when(target.getReturnType()).thenReturn(genericSourceType);
        when(genericSourceType.getStackSize()).thenReturn(StackSize.SINGLE);
        StackManipulation stackManipulation = MethodDelegationBinder.TerminationHandler.Default.DROPPING.resolve(assigner, Assigner.Typing.STATIC, source, target);
        assertThat(stackManipulation, is((StackManipulation) Removal.SINGLE));
        verify(genericSourceType).getStackSize();
        verifyNoMoreInteractions(genericSourceType);
    }

    @Test
    public void testReturning() throws Exception {
        when(source.getReturnType()).thenReturn(genericSourceType);
        when(target.getReturnType()).thenReturn(genericTargetType);
        when(genericSourceType.asErasure()).thenReturn(sourceType);
        when(genericTargetType.asErasure()).thenReturn(targetType);
        when(assigner.assign(genericTargetType, genericSourceType, Assigner.Typing.STATIC)).thenReturn(stackManipulation);
        StackManipulation stackManipulation = MethodDelegationBinder.TerminationHandler.Default.RETURNING.resolve(assigner,
                Assigner.Typing.STATIC,
                source,
                target);
        assertThat(stackManipulation, hasPrototype((StackManipulation) new StackManipulation.Compound(this.stackManipulation, MethodReturn.REFERENCE)));
        verify(assigner).assign(genericTargetType, genericSourceType, Assigner.Typing.STATIC);
    }
}
