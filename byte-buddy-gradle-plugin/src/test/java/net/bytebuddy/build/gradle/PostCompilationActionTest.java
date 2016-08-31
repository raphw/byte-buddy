package net.bytebuddy.build.gradle;

import net.bytebuddy.test.utility.MockitoRule;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class PostCompilationActionTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private Project project;

    @Mock
    private Logger logger;

    @Mock
    private ByteBuddyExtension byteBuddyExtension;

    @Mock
    private AbstractCompile task;

    @Before
    public void setUp() throws Exception {
        when(project.getLogger()).thenReturn(logger);
    }

    @Test
    public void testApplication() throws Exception {
        when(byteBuddyExtension.implies(task)).thenReturn(true);
        new PostCompilationAction(project, byteBuddyExtension).execute(task);
        verify(task).doLast(any(TransformationAction.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testNoApplication() throws Exception {
        when(byteBuddyExtension.implies(task)).thenReturn(false);
        new PostCompilationAction(project, byteBuddyExtension).execute(task);
        verify(task, never()).doLast(any(Action.class));
    }
}
