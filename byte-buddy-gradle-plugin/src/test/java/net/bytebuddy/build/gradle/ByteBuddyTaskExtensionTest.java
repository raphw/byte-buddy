package net.bytebuddy.build.gradle;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.build.EntryPoint;
import org.gradle.api.Action;
import org.gradle.api.Task;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
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
        assertThat(extension.getEntryPoint(), is((EntryPoint) EntryPoint.Default.REBASE));
        assertThat(extension.getSuffix(), is(""));
        assertThat(extension.getThreads(), is(0));
        assertThat(extension.isExtendedParsing(), is(false));
        assertThat(extension.isFailFast(), is(true));
        assertThat(extension.isFailOnLiveInitializer(), is(true));
        assertThat(extension.getAdjustment(), is(Adjustment.FULL));
        assertThat(extension.getAdjustmentErrorHandler(), is(Adjustment.ErrorHandler.WARN));
        assertThat(extension.getAdjustmentPostProcessor(), is((Action<Task>) Adjustment.NoOpPostProcessor.INSTANCE));
        assertThat(extension.isWarnOnEmptyTypeSet(), is(true));
        assertThat(extension.isLazy(), is(false));
        assertThat(extension.getDiscovery(), is(Discovery.EMPTY));
        assertThat(extension.getClassFileVersion(), nullValue(ClassFileVersion.class));
        if (extension instanceof ByteBuddyTaskExtension) {
            assertThat(((ByteBuddyTaskExtension) extension).getIncrementalResolver(), is((IncrementalResolver) IncrementalResolver.ForChangedFiles.INSTANCE));
        }
    }
}