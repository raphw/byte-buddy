package net.bytebuddy.agent;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.security.AccessController;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ByteBuddyAgentSystemClassLoaderActionTest {

    @Test
    public void testApplication() throws Exception {
        assertThat(ByteBuddyAgent.SystemClassLoaderAction.apply(AccessController.getContext()), is(ClassLoader.getSystemClassLoader()));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ByteBuddyAgent.SystemClassLoaderAction.class).apply();
    }
}
