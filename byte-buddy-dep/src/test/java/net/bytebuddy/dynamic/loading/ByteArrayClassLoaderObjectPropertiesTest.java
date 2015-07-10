package net.bytebuddy.dynamic.loading;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class ByteArrayClassLoaderObjectPropertiesTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ByteArrayClassLoader.class).applyCustom();
        ObjectPropertyAssertion.of(ByteArrayClassLoader.ChildFirst.class).applyCustom();
        ObjectPropertyAssertion.of(ByteArrayClassLoader.PersistenceHandler.class).apply();
    }
}
