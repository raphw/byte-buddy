package net.bytebuddy.agent;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ByteBuddyAgentAttachmentTypeEvaluator {

    @Test
    public void testDisabled() throws Exception {
        assertThat(ByteBuddyAgent.AttachmentTypeEvaluator.Disabled.INSTANCE.requiresExternalAttachment("foo"), is(false));
    }

    @Test
    public void testObjectProperties() throws Exception {
        final Iterator<Method> iterator = Arrays.asList(Object.class.getDeclaredMethods()).iterator();
        ObjectPropertyAssertion.of(ByteBuddyAgent.AttachmentTypeEvaluator.ForJava9CapableVm.class)
                .create(new ObjectPropertyAssertion.Creator<Method>() {
                    @Override
                    public Method create() {
                        return iterator.next();
                    }
                }).apply();
    }
}
