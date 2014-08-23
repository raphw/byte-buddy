package net.bytebuddy.agent;

import net.bytebuddy.utility.OpenJDKRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class ByteBuddyAgentTest {

    @Rule
    public MethodRule hotSpotRule = new OpenJDKRule();

    @Test
    @OpenJDKRule.Enforce
    public void testAgentInstallation() throws Exception {
        assertThat(ByteBuddyAgent.installOnOpenJDK(), notNullValue());
    }
}
