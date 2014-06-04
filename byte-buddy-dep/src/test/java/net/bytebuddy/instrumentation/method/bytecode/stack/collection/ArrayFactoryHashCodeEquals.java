package net.bytebuddy.instrumentation.method.bytecode.stack.collection;

import net.bytebuddy.instrumentation.type.TypeDescription;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class ArrayFactoryHashCodeEquals {

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
}
