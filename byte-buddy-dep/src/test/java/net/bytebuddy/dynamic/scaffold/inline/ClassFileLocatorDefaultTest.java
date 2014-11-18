package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.utility.StreamDrainer;
import org.junit.Test;

import java.io.InputStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ClassFileLocatorDefaultTest {

    @Test
    public void testClassFileLocator() throws Exception {
        TypeDescription.BinaryRepresentation binaryRepresentation =
                ClassFileLocator.Default.CLASS_PATH.classFileFor(new TypeDescription.ForLoadedType(Object.class));
        assertThat(binaryRepresentation, notNullValue());
        assertThat(binaryRepresentation.isValid(), is(true));
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(Object.class.getName().replace('.', '/') + ".class");
        try {
            assertThat(binaryRepresentation.getData(), is(new StreamDrainer().drain(inputStream)));
        } finally {
            inputStream.close();
        }
    }

    @Test
    public void testAttachedLocator() throws Exception {
        TypeDescription typeDescription = mock(TypeDescription.class);
        TypeDescription.BinaryRepresentation binaryRepresentation = mock(TypeDescription.BinaryRepresentation.class);
        when(typeDescription.toBinary()).thenReturn(binaryRepresentation);
        when(typeDescription.getInternalName()).thenReturn(getClass().getName().replace('.', '/'));
        assertThat(ClassFileLocator.Default.ATTACHED.classFileFor(typeDescription), notNullValue());
    }
}
