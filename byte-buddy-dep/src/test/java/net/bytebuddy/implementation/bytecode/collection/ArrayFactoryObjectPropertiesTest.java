package net.bytebuddy.implementation.bytecode.collection;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.implementation.bytecode.StackSize;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class ArrayFactoryObjectPropertiesTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private StackManipulation stackManipulation;

    @Test(expected = IllegalArgumentException.class)
    public void testVoidIsIllegal() throws Exception {
        ArrayFactory.forType(new TypeDescription.ForLoadedType(void.class));
    }

    @Test
    public void testIllegalArrayStackManipulation() throws Exception {
        assertThat(ArrayFactory.forType(new TypeDescription.ForLoadedType(Object.class))
                .new ArrayStackManipulation(Collections.<StackManipulation>singletonList(StackManipulation.Illegal.INSTANCE))
                .isValid(), is(false));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ArrayFactory.class).refine(new ObjectPropertyAssertion.Refinement<TypeDescription>() {
            @Override
            public void apply(TypeDescription mock) {
                when(mock.getInternalName()).thenReturn("" + System.identityHashCode(mock));
                when(mock.getStackSize()).thenReturn(StackSize.ZERO);
            }
        }).ignoreFields("sizeDecrease", "arrayCreator").apply();
        ObjectPropertyAssertion.of(ArrayFactory.ArrayCreator.ForPrimitiveType.class).apply();
        ObjectPropertyAssertion.of(ArrayFactory.ArrayCreator.ForReferenceType.class).refine(new ObjectPropertyAssertion.Refinement<TypeDescription>() {
            @Override
            public void apply(TypeDescription mock) {
                when(mock.getInternalName()).thenReturn("" + System.identityHashCode(mock));
            }
        }).apply();
        ObjectPropertyAssertion.of(ArrayFactory.ArrayStackManipulation.class).apply();
    }
}
