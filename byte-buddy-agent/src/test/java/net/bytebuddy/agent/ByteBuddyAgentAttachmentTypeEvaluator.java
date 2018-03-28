package net.bytebuddy.agent;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ByteBuddyAgentAttachmentTypeEvaluator {

    @Test
    public void testDisabled() throws Exception {
        assertThat(ByteBuddyAgent.AttachmentTypeEvaluator.Disabled.INSTANCE.requiresExternalAttachment("foo"), is(false));
    }
}
