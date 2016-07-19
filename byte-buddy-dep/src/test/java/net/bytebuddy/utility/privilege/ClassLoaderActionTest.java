package net.bytebuddy.utility.privilege;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.security.AccessController;
import java.util.Arrays;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ClassLoaderActionTest {

    @Test
    public void testApplication() throws Exception {
        assertThat(ClassLoaderAction.apply(getClass(), AccessController.getContext()), is(getClass().getClassLoader()));
    }

    @Test
    public void testObjectProperties() throws Exception {
        final Iterator<Class<?>> iterator = Arrays.<Class<?>>asList(Object.class, String.class).iterator();
        ObjectPropertyAssertion.of(ClassLoaderAction.class).create(new ObjectPropertyAssertion.Creator<Class<?>>() {
            @Override
            public Class<?> create() {
                return iterator.next();
            }
        }).apply();
    }
}