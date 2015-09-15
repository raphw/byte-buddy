package net.bytebuddy.dynamic.loading;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.net.URL;
import java.security.AccessControlContext;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Iterator;

import static org.mockito.Mockito.mock;

public class ByteArrayClassLoaderObjectPropertiesTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ByteArrayClassLoader.class).create(new ObjectPropertyAssertion.Creator<AccessControlContext>() {
            @Override
            public AccessControlContext create() {
                return new AccessControlContext(new ProtectionDomain[]{mock(ProtectionDomain.class)});
            }
        }).applyBasic();
        ObjectPropertyAssertion.of(ByteArrayClassLoader.ChildFirst.class).create(new ObjectPropertyAssertion.Creator<AccessControlContext>() {
            @Override
            public AccessControlContext create() {
                return new AccessControlContext(new ProtectionDomain[]{mock(ProtectionDomain.class)});
            }
        }).applyBasic();
        ObjectPropertyAssertion.of(ByteArrayClassLoader.PersistenceHandler.class).apply();
        ObjectPropertyAssertion.of(ByteArrayClassLoader.PersistenceHandler.UrlDefinitionAction.class).apply();
        ObjectPropertyAssertion.of(ByteArrayClassLoader.PersistenceHandler.UrlDefinitionAction.ByteArrayUrlStreamHandler.class).apply();
        final Iterator<URL> urls = Arrays.asList(new URL("http://foo"), new URL("http://bar")).iterator();
        ObjectPropertyAssertion.of(ByteArrayClassLoader.PersistenceHandler.UrlDefinitionAction.ByteArrayUrlStreamHandler.ByteArrayUrlConnection.class)
                .create(new ObjectPropertyAssertion.Creator<URL>() {
                    @Override
                    public URL create() {
                        return urls.next();
                    }
                }).applyBasic();
        ObjectPropertyAssertion.of(ByteArrayClassLoader.ClassLoaderCreationAction.class).create(new ObjectPropertyAssertion.Creator<AccessControlContext>() {
            @Override
            public AccessControlContext create() {
                return new AccessControlContext(new ProtectionDomain[]{mock(ProtectionDomain.class)});
            }
        }).apply();
    }
}
