package net.bytebuddy.instrumentation.type;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.ClassLoadingStrategy;
import net.bytebuddy.utility.ObjectPropertyAssertion;
import net.bytebuddy.utility.StreamDrainer;
import org.junit.Test;

import java.io.InputStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TypeDescriptionBinaryRepresentationTest {

    private static final byte[] DATA = new byte[]{1, 2, 3};

    @Test
    public void testIllegal() throws Exception {
        assertThat(TypeDescription.BinaryRepresentation.Illegal.INSTANCE.isValid(), is(false));
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalThrowsException() throws Exception {
        TypeDescription.BinaryRepresentation.Illegal.INSTANCE.getData();
    }

    @Test
    public void testExplicit() throws Exception {
        assertThat(new TypeDescription.BinaryRepresentation.Explicit(DATA).isValid(), is(true));
    }

    @Test
    public void testExplicitGetData() throws Exception {
        assertThat(new TypeDescription.BinaryRepresentation.Explicit(DATA).getData(), is(DATA));
    }

    @Test
    public void testReadTypeBootstrapClassLoader() throws Exception {
        TypeDescription.BinaryRepresentation binaryRepresentation = TypeDescription.BinaryRepresentation.Explicit.of(Object.class);
        assertThat(binaryRepresentation.isValid(), is(true));
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(Object.class.getName().replace('.', '/') + ".class");
        try {
            assertThat(binaryRepresentation.getData(), is(new StreamDrainer().drain(inputStream)));
        } finally {
            inputStream.close();
        }
    }

    @Test
    public void testReadTypeNonBootstrapClassLoader() throws Exception {
        TypeDescription.BinaryRepresentation binaryRepresentation = TypeDescription.BinaryRepresentation.Explicit.of(Foo.class);
        assertThat(binaryRepresentation.isValid(), is(true));
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(Foo.class.getName().replace('.', '/') + ".class");
        try {
            assertThat(binaryRepresentation.getData(), is(new StreamDrainer().drain(inputStream)));
        } finally {
            inputStream.close();
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testReadTypeIllegal() throws Exception {
        Class<?> nonClassFileType = new ByteBuddy().subclass(Object.class).make()
                .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER).getLoaded();
        TypeDescription.BinaryRepresentation binaryRepresentation = TypeDescription.BinaryRepresentation.Explicit.of(nonClassFileType);
        assertThat(binaryRepresentation.isValid(), is(false));
        binaryRepresentation.getData();
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeDescription.BinaryRepresentation.Explicit.class).apply();
    }

    private static class Foo {
    }
}
