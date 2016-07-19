package net.bytebuddy.utility.privilege;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.security.AccessController;
import java.util.Arrays;
import java.util.Iterator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;

public class ProtectionDomainActionTest {

    @Test
    public void testApplication() throws Exception {
        assertThat(ProtectionDomainAction.apply(getClass(), AccessController.getContext()), is(getClass().getProtectionDomain()));
    }

    @Test
    public void testObjectProperties() throws Exception {
        final Iterator<Class<?>> iterator = Arrays.<Class<?>>asList(Object.class, String.class).iterator();
        ObjectPropertyAssertion.of(ProtectionDomainAction.class).create(new ObjectPropertyAssertion.Creator<Class<?>>() {
            @Override
            public Class<?> create() {
                return iterator.next();
            }
        }).apply();
    }
}
