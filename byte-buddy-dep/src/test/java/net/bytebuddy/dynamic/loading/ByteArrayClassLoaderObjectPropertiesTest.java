package net.bytebuddy.dynamic.loading;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class ByteArrayClassLoaderObjectPropertiesTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ByteArrayClassLoader.class).applyMutable();
        ObjectPropertyAssertion.of(ByteArrayClassLoader.ChildFirst.class).applyMutable();
        ObjectPropertyAssertion.of(ByteArrayClassLoader.PersistenceHandler.class).apply();
    }
}
