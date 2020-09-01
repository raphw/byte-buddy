package net.bytebuddy.build.gradle;

import net.bytebuddy.build.gradle.api.ChangeType;
import net.bytebuddy.build.gradle.api.FileChange;
import org.gradle.api.Project;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class IncrementalResolverForChangedFilesTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

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
                new File("/qux/baz"),
                Collections.<File>emptyList()), is(Collections.singletonList(new File("/foo/bar/Sample.class"))));
    }

    @Test
    public void testIncrementalDeletion() {
        when(fileChange.getChangeType()).thenReturn(ChangeType.REMOVED);
        when(fileChange.getFile()).thenReturn(new File("/foo/bar/Sample.class"));
        assertThat(IncrementalResolver.ForChangedFiles.INSTANCE.apply(project,
                Collections.singleton(fileChange),
                new File("/foo/bar"),
                new File("/qux/baz"),
                Collections.<File>emptyList()), is(Collections.<File>emptyList()));
        verify(project).delete(new File("/qux/baz/Sample.class"));
    }
}
