package net.bytebuddy.agent;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class ByteBuddyAgentTest {

    @Test
    public void testAgentInstallation() throws Exception {
        assertThat(ByteBuddyAgent.installOnOpenJDK(), notNullValue());
    }
}
