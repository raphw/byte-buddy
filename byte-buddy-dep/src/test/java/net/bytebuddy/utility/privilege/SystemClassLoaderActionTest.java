package net.bytebuddy.utility.privilege;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.security.AccessController;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class SystemClassLoaderActionTest {

    @Test
    public void testApplication() throws Exception {
        assertThat(SystemClassLoaderAction.apply(AccessController.getContext()), is(ClassLoader.getSystemClassLoader()));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(SystemClassLoaderAction.class).apply();
    }
}
