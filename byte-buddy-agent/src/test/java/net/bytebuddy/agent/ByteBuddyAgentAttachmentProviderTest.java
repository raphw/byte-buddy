package net.bytebuddy.agent;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.io.File;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class ByteBuddyAgentAttachmentProviderTest {

    @Test
    public void testSimpleAccessor() throws Exception {
        File file = mock(File.class);
        ByteBuddyAgent.AttachmentProvider.Accessor accessor =
                new ByteBuddyAgent.AttachmentProvider.Accessor.Simple.WithExternalAttachment(Void.class, Collections.singletonList(file));
        assertThat(accessor.isAvailable(), is(true));
        assertThat(accessor.getVirtualMachineType(), CoreMatchers.<Class<?>>is(Void.class));
        assertThat(accessor.getExternalAttachment().getVirtualMachineType(), is(Void.class.getName()));
        assertThat(accessor.getExternalAttachment().getClassPath(), is(Collections.singletonList(file)));
    }

    @Test(expected = IllegalStateException.class)
    public void testSimpleAccessorWithoutExternalAttachment() throws Exception {
        new ByteBuddyAgent.AttachmentProvider.Accessor.Simple.WithoutExternalAttachment(Void.class).getExternalAttachment();
    }

    @Test
    public void testUnavailableAccessor() throws Exception {
        assertThat(ByteBuddyAgent.AttachmentProvider.Accessor.Unavailable.INSTANCE.isAvailable(), is(false));
    }

    @Test(expected = IllegalStateException.class)
    public void testUnavailableAccessorThrowsExceptionForType() throws Exception {
        ByteBuddyAgent.AttachmentProvider.Accessor.Unavailable.INSTANCE.getVirtualMachineType();
    }

    @Test(expected = IllegalStateException.class)
    public void testUnavailableAccessorThrowsExceptionForExternalAttachment() throws Exception {
        ByteBuddyAgent.AttachmentProvider.Accessor.Unavailable.INSTANCE.getExternalAttachment();
    }
}
