package net.bytebuddy.dynamic.loading;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessControlContext;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Iterator;

import static org.mockito.Mockito.mock;

public class ByteArrayClassLoaderObjectPropertiesTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ByteArrayClassLoader.ClassDefinitionAction.class).apply();
        ObjectPropertyAssertion.of(ByteArrayClassLoader.PersistenceHandler.class).apply();
        ObjectPropertyAssertion.of(ByteArrayClassLoader.PersistenceHandler.UrlDefinitionAction.class).apply();
        ObjectPropertyAssertion.of(ByteArrayClassLoader.PersistenceHandler.UrlDefinitionAction.ByteArrayUrlStreamHandler.class).apply();
    }
}
