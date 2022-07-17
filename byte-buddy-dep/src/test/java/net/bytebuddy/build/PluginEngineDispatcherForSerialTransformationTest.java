package net.bytebuddy.build;

import net.bytebuddy.description.type.TypeDescription;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class PluginEngineDispatcherForSerialTransformationTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private Plugin.Engine.Target.Sink sink;

    @Mock
    private Plugin.Engine.Dispatcher.Materializable materializable;

    @Mock
    private Executor executor;

    private List<TypeDescription> transformed;

    private Map<TypeDescription, List<Throwable>> failed;

    private List<String> unresolved;

    private Callable<Callable<Plugin.Engine.Dispatcher.Materializable>> work;

    private boolean preprocessing, processing;

    private boolean preprocessingError, processingError;

    @Before
    public void setUp() throws Exception {
        transformed = new ArrayList<TypeDescription>();
        failed = new HashMap<TypeDescription, List<Throwable>>();
        unresolved = new ArrayList<String>();
        work = new Callable<Callable<Plugin.Engine.Dispatcher.Materializable>>() {
            public Callable<Plugin.Engine.Dispatcher.Materializable> call() {
                preprocessing = true;
                if (preprocessingError) {
                    throw new IllegalStateException();
                }
                return new Callable<Plugin.Engine.Dispatcher.Materializable>() {
                    public Plugin.Engine.Dispatcher.Materializable call() {
                        processing = true;
                        if (processingError) {
                            throw new IllegalStateException();
                        }
                        return materializable;
                    }
                };
            }
        };
        Mockito.doAnswer(new Answer<Void>() {

            public Void answer(InvocationOnMock invocationOnMock) {
                ((Runnable) invocationOnMock.getArgument(0)).run();
                return null;
            }
        }).when(executor).execute(any(Runnable.class));
    }

    @Test
    public void testEagerSerialTransformation() throws Exception {
        Plugin.Engine.Dispatcher dispatcher = new Plugin.Engine.Dispatcher.ForSerialTransformation(sink, transformed, failed, unresolved);
        dispatcher.accept(work, true);
        assertThat(preprocessing, is(true));
        assertThat(processing, is(true));
        verify(materializable).materialize(sink, transformed, failed, unresolved);
        verifyNoMoreInteractions(materializable);
    }

    @Test
    public void testDeferredSerialTransformation() throws Exception {
        Plugin.Engine.Dispatcher dispatcher = new Plugin.Engine.Dispatcher.ForSerialTransformation(sink, transformed, failed, unresolved);
        dispatcher.accept(work, false);
        assertThat(preprocessing, is(true));
        assertThat(processing, is(false));
        dispatcher.complete();
        assertThat(processing, is(true));
        verify(materializable).materialize(sink, transformed, failed, unresolved);
        verifyNoMoreInteractions(materializable);
    }

    @Test
    public void testSerialTransformationPreprocessingException() throws Exception {
        Plugin.Engine.Dispatcher dispatcher = new Plugin.Engine.Dispatcher.ForSerialTransformation(sink, transformed, failed, unresolved);
        preprocessingError = true;
        try {
            dispatcher.accept(work, false);
            fail();
        } catch (Exception exception) {
            assertThat(exception, instanceOf(IllegalStateException.class));
        }
        assertThat(preprocessing, is(true));
        assertThat(processing, is(false));
        verifyNoMoreInteractions(materializable);
    }

    @Test
    public void testSerialTransformationProcessingException() throws Exception {
        Plugin.Engine.Dispatcher dispatcher = new Plugin.Engine.Dispatcher.ForSerialTransformation(sink, transformed, failed, unresolved);
        processingError = true;
        dispatcher.accept(work, false);
        assertThat(preprocessing, is(true));
        assertThat(processing, is(false));
        try {
            dispatcher.complete();
            fail();
        } catch (Exception exception) {
            assertThat(exception, instanceOf(IllegalStateException.class));
        }
        assertThat(processing, is(true));
        verifyNoMoreInteractions(materializable);
    }

    @Test
    public void testParallelSerialTransformation() throws Exception {
        Plugin.Engine.Dispatcher dispatcher = new Plugin.Engine.Dispatcher.ForParallelTransformation(executor, sink, transformed, failed, unresolved);
        dispatcher.accept(work, true);
        assertThat(preprocessing, is(true));
        assertThat(processing, is(true));
        dispatcher.complete();
        verify(materializable).materialize(sink, transformed, failed, unresolved);
        verifyNoMoreInteractions(materializable);
    }

    @Test
    public void testDeferredParallelTransformation() throws Exception {
        Plugin.Engine.Dispatcher dispatcher = new Plugin.Engine.Dispatcher.ForParallelTransformation(executor, sink, transformed, failed, unresolved);
        dispatcher.accept(work, false);
        assertThat(preprocessing, is(true));
        assertThat(processing, is(false));
        dispatcher.complete();
        assertThat(processing, is(true));
        verify(materializable).materialize(sink, transformed, failed, unresolved);
        verifyNoMoreInteractions(materializable);
    }

    @Test
    public void testParallelTransformationPreprocessingException() throws Exception {
        Plugin.Engine.Dispatcher dispatcher = new Plugin.Engine.Dispatcher.ForParallelTransformation(executor, sink, transformed, failed, unresolved);
        preprocessingError = true;
        dispatcher.accept(work, false);
        try {
            dispatcher.complete();
            fail();
        } catch (Exception exception) {
            assertThat(exception, instanceOf(IllegalStateException.class));
        }
        assertThat(preprocessing, is(true));
        assertThat(processing, is(false));
        verifyNoMoreInteractions(materializable);
    }

    @Test
    public void testParallelTransformationProcessingException() throws Exception {
        Plugin.Engine.Dispatcher dispatcher = new Plugin.Engine.Dispatcher.ForParallelTransformation(executor, sink, transformed, failed, unresolved);
        processingError = true;
        dispatcher.accept(work, false);
        assertThat(preprocessing, is(true));
        assertThat(processing, is(false));
        try {
            dispatcher.complete();
            fail();
        } catch (Exception exception) {
            assertThat(exception, instanceOf(IllegalStateException.class));
        }
        assertThat(processing, is(true));
        verifyNoMoreInteractions(materializable);
    }
}
