package net.bytebuddy.dynamic;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class MethodTransformerNoOpTest {

    @Test
    public void testTransformation() throws Exception {
        MethodDescription methodDescription = mock(MethodDescription.class);
        assertThat(MethodTransformer.NoOp.INSTANCE.transform(mock(TypeDescription.class), methodDescription), is(methodDescription));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodTransformer.NoOp.class).apply();
    }
}