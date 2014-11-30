package net.bytebuddy.pool;

import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import net.bytebuddy.utility.StreamDrainer;
import org.junit.Test;

import java.io.InputStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TypePoolSourceLocatorForClassLoaderTest {

    private static final String FOO = "foo";

    @Test
    public void testSuccessful() throws Exception {
        TypeDescription.BinaryRepresentation binaryRepresentation = TypePool.SourceLocator.ForClassLoader.ofSystemClassLoader()
                .locate(Object.class.getName());
        assertThat(binaryRepresentation.isValid(), is(true));
        InputStream inputStream = ClassLoader.getSystemResourceAsStream(Object.class.getName().replace('.', '/') + ".class");
        try {
            assertThat(binaryRepresentation.getData(), is(new StreamDrainer().drain(inputStream)));
        } finally {
            inputStream.close();
        }
    }

    @Test
    public void testNonSuccessful() throws Exception {
        assertThat(TypePool.SourceLocator.ForClassLoader.ofSystemClassLoader().locate(FOO).isValid(), is(false));
    }

    @Test(expected = IllegalStateException.class)
    public void testNonSuccessfulThrowsException() throws Exception {
        TypePool.SourceLocator.ForClassLoader.ofSystemClassLoader().locate(FOO).getData();
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypePool.SourceLocator.ForClassLoader.class).apply();
    }
}
