package net.bytebuddy.utility.privilege;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.security.AccessController;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;

public class ParentClassLoaderActionTest {

    @Test
    public void testApplication() throws Exception {
        assertThat(ParentClassLoaderAction.apply(getClass().getClassLoader(), AccessController.getContext()), is(getClass().getClassLoader().getParent()));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ParentClassLoaderAction.class).apply();
    }
}
