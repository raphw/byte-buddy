package net.bytebuddy.agent;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

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

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ByteBuddyAgent.AttachmentProvider.ForJigsawVm.class).apply();
        ObjectPropertyAssertion.of(ByteBuddyAgent.AttachmentProvider.ForJ9Vm.class).apply();
        ObjectPropertyAssertion.of(ByteBuddyAgent.AttachmentProvider.ForToolsJarVm.class).apply();
        ObjectPropertyAssertion.of(ByteBuddyAgent.AttachmentProvider.ForUnixHotSpotVm.class).apply();
        ObjectPropertyAssertion.of(ByteBuddyAgent.AttachmentProvider.Compound.class).create(new ObjectPropertyAssertion.Creator<List<?>>() {
            @Override
            public List<?> create() {
                return Collections.singletonList(mock(ByteBuddyAgent.AttachmentProvider.class));
            }
        }).apply();
        final Iterator<Class<?>> types = Arrays.<Class<?>>asList(Void.class, Object.class, String.class, Integer.class).iterator();
        ObjectPropertyAssertion.of(ByteBuddyAgent.AttachmentProvider.Accessor.Simple.WithExternalAttachment.class)
                .create(new ObjectPropertyAssertion.Creator<Class<?>>() {
                    @Override
                    public Class<?> create() {
                        return types.next();
                    }
                }).apply();
        ObjectPropertyAssertion.of(ByteBuddyAgent.AttachmentProvider.Accessor.Simple.WithoutExternalAttachment.class)
                .create(new ObjectPropertyAssertion.Creator<Class<?>>() {
                    @Override
                    public Class<?> create() {
                        return types.next();
                    }
                }).apply();
        ObjectPropertyAssertion.of(ByteBuddyAgent.AttachmentProvider.Accessor.Unavailable.class).apply();
    }
}
