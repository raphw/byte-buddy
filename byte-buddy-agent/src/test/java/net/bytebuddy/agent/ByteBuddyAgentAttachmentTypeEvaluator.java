package net.bytebuddy.agent;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;

public class ByteBuddyAgentAttachmentTypeEvaluator {

    @Test
    public void testDisabled() throws Exception {
        assertThat(ByteBuddyAgent.AttachmentTypeEvaluator.Disabled.INSTANCE.requiresExternalAttachment("foo"), is(false));
    }
}
