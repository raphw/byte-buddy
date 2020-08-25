package net.bytebuddy.build.gradle;

import net.bytebuddy.build.gradle.api.ChangeType;
import net.bytebuddy.build.gradle.api.FileChange;
import net.bytebuddy.test.utility.MockitoRule;
import org.gradle.api.Project;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import java.io.File;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class IncrementalResolverForChangedFilesTest {

    @Rule
    public MockitoRule mockitoRule = new MockitoRule(this);

    @Mock
    private Project project;

    @Mock
    private FileChange fileChange;

    @Test
    public void testIncrementalChange() {
        when(fileChange.getFile()).thenReturn(new File("/foo/bar/Sample.class"));
        assertThat(IncrementalResolver.ForChangedFiles.INSTANCE.apply(project,
                Collections.singleton(fileChange),
                new File("/foo/bar"),
                new File("/qux/baz")), is(Collections.singletonList(new File("/qux/baz/Sample.class"))));
    }

    @Test
    public void testIncrementalDeletion() {
        when(fileChange.getChangeType()).thenReturn(ChangeType.REMOVED);
        when(fileChange.getFile()).thenReturn(new File("/foo/bar/Sample.class"));
        assertThat(IncrementalResolver.ForChangedFiles.INSTANCE.apply(project,
                Collections.singleton(fileChange),
                new File("/foo/bar"),
                new File("/qux/baz")), is(Collections.<File>emptyList()));
    }
}
