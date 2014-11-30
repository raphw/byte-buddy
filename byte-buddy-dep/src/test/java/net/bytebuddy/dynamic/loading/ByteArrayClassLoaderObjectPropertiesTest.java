package net.bytebuddy.dynamic.loading;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.security.ProtectionDomain;
import java.util.Collections;

import static org.mockito.Mockito.mock;

public class ByteArrayClassLoaderObjectPropertiesTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ByteArrayClassLoader.class).apply(new ByteArrayClassLoader(mock(ClassLoader.class),
                Collections.<String, byte[]>emptyMap(),
                mock(ProtectionDomain.class),
                mock(ByteArrayClassLoader.PersistenceHandler.class)));
        ObjectPropertyAssertion.of(ByteArrayClassLoader.ChildFirst.class).apply(new ByteArrayClassLoader.ChildFirst(mock(ClassLoader.class),
                Collections.<String, byte[]>emptyMap(),
                mock(ProtectionDomain.class),
                mock(ByteArrayClassLoader.PersistenceHandler.class)));
    }
}
