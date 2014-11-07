package net.bytebuddy.instrumentation.method.bytecode.stack.collection;

import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.method.bytecode.stack.StackSize;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.utility.MockitoRule;
import net.bytebuddy.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ArrayFactoryObjectPropertiesTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private StackManipulation stackManipulation;

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ArrayFactory.class).refine(new ObjectPropertyAssertion.Refinement<TypeDescription>() {
            @Override
            public void apply(TypeDescription mock) {
                when(mock.getInternalName()).thenReturn("" + System.identityHashCode(mock));
                when(mock.getStackSize()).thenReturn(StackSize.ZERO);
            }
        }).ignoreFields("sizeDecrease", "arrayCreator").apply();
        ObjectPropertyAssertion.of(ArrayFactory.ArrayCreator.ForReferenceType.class).refine(new ObjectPropertyAssertion.Refinement<TypeDescription>() {
            @Override
            public void apply(TypeDescription mock) {
                when(mock.getInternalName()).thenReturn("" + System.identityHashCode(mock));
            }
        }).apply();
    }

    @Test
    public void testStackManipulationHashCodeEquals() throws Exception {
        assertThat(ArrayFactory.targeting(new TypeDescription.ForLoadedType(Object.class)).withValues(Arrays.asList(stackManipulation)).hashCode(),
                is(ArrayFactory.targeting(new TypeDescription.ForLoadedType(Object.class)).withValues(Arrays.asList(stackManipulation)).hashCode()));
        assertThat(ArrayFactory.targeting(new TypeDescription.ForLoadedType(Object.class)).withValues(Arrays.asList(stackManipulation)),
                is(ArrayFactory.targeting(new TypeDescription.ForLoadedType(Object.class)).withValues(Arrays.asList(stackManipulation))));
        assertThat(ArrayFactory.targeting(new TypeDescription.ForLoadedType(Object.class)).withValues(Arrays.asList(stackManipulation)).hashCode(),
                not(is(ArrayFactory.targeting(new TypeDescription.ForLoadedType(Object.class)).withValues(Arrays.asList(mock(StackManipulation.class))).hashCode())));
        assertThat(ArrayFactory.targeting(new TypeDescription.ForLoadedType(Object.class)).withValues(Arrays.asList(stackManipulation)),
                not(is(ArrayFactory.targeting(new TypeDescription.ForLoadedType(Object.class)).withValues(Arrays.asList(mock(StackManipulation.class))))));
    }
}
