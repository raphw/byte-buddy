package net.bytebuddy.instrumentation.method.bytecode.stack.collection;

import net.bytebuddy.instrumentation.method.bytecode.stack.StackManipulation;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.utility.HashCodeEqualsTester;
import net.bytebuddy.utility.MockitoRule;
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

public class ArrayFactoryHashCodeEquals {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private StackManipulation stackManipulation;

    @Test
    public void testHashCodeEquals() throws Exception {
        assertThat(ArrayFactory.targeting(new TypeDescription.ForLoadedType(Object.class)).hashCode(),
                is(ArrayFactory.targeting(new TypeDescription.ForLoadedType(Object.class)).hashCode()));
        assertThat(ArrayFactory.targeting(new TypeDescription.ForLoadedType(Object.class)),
                is(ArrayFactory.targeting(new TypeDescription.ForLoadedType(Object.class))));
        assertThat(ArrayFactory.targeting(new TypeDescription.ForLoadedType(Object.class)).hashCode(),
                not(is(ArrayFactory.targeting(new TypeDescription.ForLoadedType(String.class)).hashCode())));
        assertThat(ArrayFactory.targeting(new TypeDescription.ForLoadedType(Object.class)),
                not(is(ArrayFactory.targeting(new TypeDescription.ForLoadedType(String.class)))));
    }

    @Test
    public void testReferenceCreatorHashCodeEquals() throws Exception {
        HashCodeEqualsTester.of(ArrayFactory.ArrayCreator.Reference.class).refine(new HashCodeEqualsTester.Refinement<TypeDescription>() {
            @Override
            public void apply(TypeDescription mock) {
                when(mock.getInternalName()).thenReturn("" + System.identityHashCode(mock));
            }
        }).skipSynthetic().apply();
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
