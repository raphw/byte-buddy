package net.bytebuddy.build.gradle;

import net.bytebuddy.build.EntryPoint;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class ByteBuddyTaskExtensionTest {

    private final AbstractByteBuddyTaskExtension<?> extension;

    public ByteBuddyTaskExtensionTest(AbstractByteBuddyTaskExtension<?> extension) {
        this.extension = extension;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {new ByteBuddyTaskExtension()},
                {new ByteBuddySimpleTaskExtension()}
        });
    }

    @Test
    public void testDefaultProperties() {
        assertThat(extension.getTransformations().size(), is(0));
        assertThat(extension.getEntryPoint(), is(EntryPoint.Default.REBASE));
        assertThat(extension.getSuffix(), is(""));
        assertThat(extension.getThreads(), is(0));
        assertThat(extension.isExtendedParsing(), is(false));
        assertThat(extension.isFailFast(), is(false));
        assertThat(extension.isFailOnLiveInitializer(), is(true));
        assertThat(extension.isWarnOnEmptyTypeSet(), is(true));
        if (extension instanceof ByteBuddyTaskExtension) {
            assertThat(((ByteBuddyTaskExtension) extension).getIncrementalResolver(), is((IncrementalResolver) IncrementalResolver.ForChangedFiles.INSTANCE));
        }
    }
}