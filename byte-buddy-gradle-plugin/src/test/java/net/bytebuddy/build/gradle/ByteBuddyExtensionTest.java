package net.bytebuddy.build.gradle;

import groovy.lang.Closure;
import net.bytebuddy.build.EntryPoint;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.test.utility.MockitoRule;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class ByteBuddyExtensionTest {

    private static final String FOO = "foo", BAR = "bar";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private Project project;

    @Mock
    private MethodDescription methodDescription;

    @Mock
    private Closure<?> closure;

    @Mock
    private Task task;

    @Test
    public void testLiveInitializer() throws Exception {
        ByteBuddyExtension byteBuddyExtension = new ByteBuddyExtension(project);
        byteBuddyExtension.setFailOnLiveInitializer(false);
        assertThat(byteBuddyExtension.isFailOnLiveInitializer(), is(false));
    }

    @Test
    public void testLiveInitializerDefault() throws Exception {
        assertThat(new ByteBuddyExtension(project).isFailOnLiveInitializer(), is(true));
    }

    @Test
    public void testSuffix() throws Exception {
        when(methodDescription.getName()).thenReturn(BAR);
        ByteBuddyExtension byteBuddyExtension = new ByteBuddyExtension(project);
        byteBuddyExtension.setSuffix(FOO);
        assertThat(byteBuddyExtension.getMethodNameTransformer().transform(methodDescription), endsWith(FOO));
    }

    @Test
    public void testSuffixEmpty() throws Exception {
        when(methodDescription.getName()).thenReturn(BAR);
        ByteBuddyExtension byteBuddyExtension = new ByteBuddyExtension(project);
        byteBuddyExtension.setSuffix("");
        assertThat(byteBuddyExtension.getMethodNameTransformer().transform(methodDescription), not(BAR));
    }

    @Test
    public void testSuffixDefault() throws Exception {
        when(methodDescription.getName()).thenReturn(BAR);
        assertThat(new ByteBuddyExtension(project).getMethodNameTransformer().transform(methodDescription), not(BAR));
    }

    @Test
    public void testTransformation() throws Exception {
        when(project.configure(any(Transformation.class), eq(closure))).then(new Answer<Transformation>() {
            @Override
            public Transformation answer(InvocationOnMock invocationOnMock) throws Throwable {
                return invocationOnMock.getArgumentAt(0, Transformation.class);
            }
        });
        ByteBuddyExtension byteBuddyExtension = new ByteBuddyExtension(project);
        byteBuddyExtension.transformation(closure);
        assertThat(byteBuddyExtension.getTransformations().size(), is(1));
    }

    @Test
    public void testInitialization() throws Exception {
        when(project.configure(any(Initialization.class), eq(closure))).then(new Answer<Initialization>() {
            @Override
            public Initialization answer(InvocationOnMock invocationOnMock) throws Throwable {
                return invocationOnMock.getArgumentAt(0, Initialization.class);
            }
        });
        ByteBuddyExtension byteBuddyExtension = new ByteBuddyExtension(project);
        byteBuddyExtension.initialization(closure);
        assertThat(byteBuddyExtension.getInitialization(), notNullValue(Initialization.class));
    }

    @Test
    public void testInitializationDefault() throws Exception {
        assertThat(new ByteBuddyExtension(project).getInitialization().getEntryPoint(mock(ClassLoaderResolver.class), mock(File.class), Collections.<File>emptySet()),
                is((EntryPoint) EntryPoint.Default.REBASE));
    }

    @Test(expected = GradleException.class)
    public void testInitializationDuplicate() throws Exception {
        when(project.configure(any(Initialization.class), eq(closure))).then(new Answer<Initialization>() {
            @Override
            public Initialization answer(InvocationOnMock invocationOnMock) throws Throwable {
                return invocationOnMock.getArgumentAt(0, Initialization.class);
            }
        });
        ByteBuddyExtension byteBuddyExtension = new ByteBuddyExtension(project);
        byteBuddyExtension.initialization(closure);
        byteBuddyExtension.initialization(closure);
    }

    @Test
    public void testTasks() throws Exception {
        assertThat(new ByteBuddyExtension(project).implies(task), is(true));
        verifyZeroInteractions(task);
    }

    @Test
    public void testTaskExplicitIncluded() throws Exception {
        ByteBuddyExtension byteBuddyExtension = new ByteBuddyExtension(project);
        byteBuddyExtension.setTasks(Collections.singleton(FOO));
        when(task.getName()).thenReturn(FOO);
        assertThat(byteBuddyExtension.implies(task), is(true));
    }

    @Test
    public void testTaskExplicitExcluded() throws Exception {
        ByteBuddyExtension byteBuddyExtension = new ByteBuddyExtension(project);
        byteBuddyExtension.setTasks(Collections.singleton(FOO));
        when(task.getName()).thenReturn(BAR);
        assertThat(byteBuddyExtension.implies(task), is(false));
    }
}
