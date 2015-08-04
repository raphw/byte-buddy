package net.bytebuddy.dynamic;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;

public class MethodTransformerRetainingTest {

    @Test
    public void testTransformation() throws Exception {
        MethodDescription methodDescription = mock(MethodDescription.class);
        assertThat(MethodTransformer.Retaining.INSTANCE.transform(methodDescription), is(methodDescription));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodTransformer.Retaining.class).apply();
    }
}