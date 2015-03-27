package net.bytebuddy.instrumentation.method.bytecode.stack.member;

import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.ParameterList;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MethodVariableAccessOtherTest {

    @Test(expected = IllegalArgumentException.class)
    public void testVoidArgument() throws Exception {
        TypeDescription voidTypeDescription = mock(TypeDescription.class);
        when(voidTypeDescription.isPrimitive()).thenReturn(true);
        when(voidTypeDescription.represents(void.class)).thenReturn(true);
        MethodVariableAccess.forType(voidTypeDescription);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodVariableAccess.ArgumentLoadingStackManipulation.class).apply();
        ObjectPropertyAssertion.of(MethodVariableAccess.TypeCastingHandler.ForBridgeTarget.class).refine(new ObjectPropertyAssertion.Refinement<MethodDescription>() {
            @Override
            public void apply(MethodDescription mock) {
                when(mock.getParameters()).thenReturn(new ParameterList.Empty());
            }
        }).applyMutable();
    }
}
