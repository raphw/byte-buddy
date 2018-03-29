package net.bytebuddy.dynamic;

import net.bytebuddy.description.type.TypeDescription;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class TransformerNoOpTest {

    @Test
    public void testTransformation() throws Exception {
        Object target = mock(Object.class);
        assertThat(Transformer.NoOp.INSTANCE.transform(mock(TypeDescription.class), target), is(target));
    }
}
