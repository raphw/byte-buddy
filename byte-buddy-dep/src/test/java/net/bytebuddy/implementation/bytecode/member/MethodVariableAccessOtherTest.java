package net.bytebuddy.implementation.bytecode.member;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.type.TypeDescription;
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
        MethodVariableAccess.of(voidTypeDescription);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodVariableAccess.class).apply();
        ObjectPropertyAssertion.of(MethodVariableAccess.OffsetLoading.class).apply();
        ObjectPropertyAssertion.of(MethodVariableAccess.MethodLoading.class).apply();
        ObjectPropertyAssertion.of(MethodVariableAccess.MethodLoading.TypeCastingHandler.ForBridgeTarget.class)
                .refine(new ObjectPropertyAssertion.Refinement<MethodDescription>() {
                    @Override
                    public void apply(MethodDescription mock) {
                        when(mock.getParameters()).thenReturn((ParameterList) new ParameterList.Empty<ParameterDescription>());
                    }
                }).applyBasic();
        ObjectPropertyAssertion.of(MethodVariableAccess.MethodLoading.TypeCastingHandler.NoOp.class).apply();
    }
}
