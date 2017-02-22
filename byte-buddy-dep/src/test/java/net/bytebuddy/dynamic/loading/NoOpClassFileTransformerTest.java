package net.bytebuddy.dynamic.loading;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class NoOpClassFileTransformerTest {

    @Test
    public void testNoTransformation() throws Exception {
        assertThat(NoOpClassFileTransformer.INSTANCE.transform(mock(ClassLoader.class),
                "foo",
                null,
                null,
                new byte[0]), nullValue(byte[].class));
    }
}