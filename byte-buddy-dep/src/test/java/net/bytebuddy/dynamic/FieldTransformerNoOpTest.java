package net.bytebuddy.dynamic;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class FieldTransformerNoOpTest {

    @Test
    public void testTransformation() throws Exception {
        FieldDescription fieldDescription = mock(FieldDescription.class);
        assertThat(FieldTransformer.NoOp.INSTANCE.transform(mock(TypeDescription.class), fieldDescription), is(fieldDescription));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(FieldTransformer.NoOp.class).apply();
    }
}
