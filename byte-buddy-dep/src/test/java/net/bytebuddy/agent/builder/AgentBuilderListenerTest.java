package net.bytebuddy.agent.builder;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import net.bytebuddy.utility.JavaModule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.io.PrintStream;
import java.lang.instrument.Instrumentation;
import java.util.Collections;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.none;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class AgentBuilderListenerTest {

    private static final String FOO = "foo";

    private static final boolean LOADED = true;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private AgentBuilder.Listener first, second;

    @Mock
    private TypeDescription typeDescription;

    @Mock
    private ClassLoader classLoader;

    @Mock
    private JavaModule module;

    @Mock
    private DynamicType dynamicType;

    @Mock
    private Throwable throwable;

    @Before
    public void setUp() throws Exception {
        when(typeDescription.getName()).thenReturn(FOO);
    }

    @Test
    public void testNoOp() throws Exception {
        AgentBuilder.Listener.NoOp.INSTANCE.onDiscovery(FOO, classLoader, module, LOADED);
        AgentBuilder.Listener.NoOp.INSTANCE.onTransformation(typeDescription, classLoader, module, LOADED, dynamicType);
        verifyZeroInteractions(dynamicType);
        AgentBuilder.Listener.NoOp.INSTANCE.onError(FOO, classLoader, module, LOADED, throwable);
        verifyZeroInteractions(throwable);
        AgentBuilder.Listener.NoOp.INSTANCE.onIgnored(typeDescription, classLoader, module, LOADED);
        AgentBuilder.Listener.NoOp.INSTANCE.onComplete(FOO, classLoader, module, LOADED);
    }

    @Test
    public void testPseudoAdapter() throws Exception {
        AgentBuilder.Listener listener = new PseudoAdapter();
        listener.onDiscovery(FOO, classLoader, module, LOADED);
        listener.onTransformation(typeDescription, classLoader, module, LOADED, dynamicType);
        verifyZeroInteractions(dynamicType);
        listener.onError(FOO, classLoader, module, LOADED, throwable);
        verifyZeroInteractions(throwable);
        listener.onIgnored(typeDescription, classLoader, module, LOADED);
        listener.onComplete(FOO, classLoader, module, LOADED);
    }

    @Test
    public void testCompoundOnDiscovery() throws Exception {
        new AgentBuilder.Listener.Compound(first, second).onDiscovery(FOO, classLoader, module, LOADED);
        verify(first).onDiscovery(FOO, classLoader, module, LOADED);
        verifyNoMoreInteractions(first);
        verify(second).onDiscovery(FOO, classLoader, module, LOADED);
        verifyNoMoreInteractions(second);
    }

    @Test
    public void testCompoundOnTransformation() throws Exception {
        new AgentBuilder.Listener.Compound(first, second).onTransformation(typeDescription, classLoader, module, LOADED, dynamicType);
        verify(first).onTransformation(typeDescription, classLoader, module, LOADED, dynamicType);
        verifyNoMoreInteractions(first);
        verify(second).onTransformation(typeDescription, classLoader, module, LOADED, dynamicType);
        verifyNoMoreInteractions(second);
    }

    @Test
    public void testCompoundOnError() throws Exception {
        new AgentBuilder.Listener.Compound(first, second).onError(FOO, classLoader, module, LOADED, throwable);
        verify(first).onError(FOO, classLoader, module, LOADED, throwable);
        verifyNoMoreInteractions(first);
        verify(second).onError(FOO, classLoader, module, LOADED, throwable);
        verifyNoMoreInteractions(second);
    }

    @Test
    public void testCompoundOnIgnored() throws Exception {
        new AgentBuilder.Listener.Compound(first, second).onIgnored(typeDescription, classLoader, module, LOADED);
        verify(first).onIgnored(typeDescription, classLoader, module, LOADED);
        verifyNoMoreInteractions(first);
        verify(second).onIgnored(typeDescription, classLoader, module, LOADED);
        verifyNoMoreInteractions(second);
    }

    @Test
    public void testCompoundOnComplete() throws Exception {
        new AgentBuilder.Listener.Compound(first, second).onComplete(FOO, classLoader, module, LOADED);
        verify(first).onComplete(FOO, classLoader, module, LOADED);
        verifyNoMoreInteractions(first);
        verify(second).onComplete(FOO, classLoader, module, LOADED);
        verifyNoMoreInteractions(second);
    }

    @Test
    public void testStreamWritingOnDiscovery() throws Exception {
        PrintStream printStream = mock(PrintStream.class);
        AgentBuilder.Listener listener = new AgentBuilder.Listener.StreamWriting(printStream);
        listener.onDiscovery(FOO, classLoader, module, LOADED);
        verify(printStream).printf("[Byte Buddy] DISCOVERY %s [%s, %s, loaded=%b]%n", FOO, classLoader, module, LOADED);
        verifyNoMoreInteractions(printStream);
    }

    @Test
    public void testStreamWritingOnTransformation() throws Exception {
        PrintStream printStream = mock(PrintStream.class);
        AgentBuilder.Listener listener = new AgentBuilder.Listener.StreamWriting(printStream);
        listener.onTransformation(typeDescription, classLoader, module, LOADED, dynamicType);
        verify(printStream).printf("[Byte Buddy] TRANSFORM %s [%s, %s, loaded=%b]%n", FOO, classLoader, module, LOADED);
        verifyNoMoreInteractions(printStream);
    }

    @Test
    public void testStreamWritingOnError() throws Exception {
        PrintStream printStream = mock(PrintStream.class);
        AgentBuilder.Listener listener = new AgentBuilder.Listener.StreamWriting(printStream);
        listener.onError(FOO, classLoader, module, LOADED, throwable);
        verify(printStream).printf("[Byte Buddy] ERROR %s [%s, %s, loaded=%b]%n", FOO, classLoader, module, LOADED);
        verifyNoMoreInteractions(printStream);
        verify(throwable).printStackTrace(printStream);
        verifyNoMoreInteractions(throwable);
    }

    @Test
    public void testStreamWritingOnComplete() throws Exception {
        PrintStream printStream = mock(PrintStream.class);
        AgentBuilder.Listener listener = new AgentBuilder.Listener.StreamWriting(printStream);
        listener.onComplete(FOO, classLoader, module, LOADED);
        verify(printStream).printf("[Byte Buddy] COMPLETE %s [%s, %s, loaded=%b]%n", FOO, classLoader, module, LOADED);
        verifyNoMoreInteractions(printStream);
    }

    @Test
    public void testStreamWritingOnIgnore() throws Exception {
        PrintStream printStream = mock(PrintStream.class);
        AgentBuilder.Listener listener = new AgentBuilder.Listener.StreamWriting(printStream);
        listener.onIgnored(typeDescription, classLoader, module, LOADED);
        verify(printStream).printf("[Byte Buddy] IGNORE %s [%s, %s, loaded=%b]%n", FOO, classLoader, module, LOADED);
        verifyNoMoreInteractions(printStream);
    }

    @Test
    public void testStreamWritingStandardOutput() throws Exception {
        assertThat(AgentBuilder.Listener.StreamWriting.toSystemOut(), is((AgentBuilder.Listener) new AgentBuilder.Listener.StreamWriting(System.out)));
    }

    @Test
    public void testStreamWritingStandardError() throws Exception {
        assertThat(AgentBuilder.Listener.StreamWriting.toSystemError(), is((AgentBuilder.Listener) new AgentBuilder.Listener.StreamWriting(System.err)));
    }

    @Test
    public void testFilteringDoesNotMatch() throws Exception {
        AgentBuilder.Listener delegate = mock(AgentBuilder.Listener.class);
        AgentBuilder.Listener listener = new AgentBuilder.Listener.Filtering(none(), delegate);
        listener.onDiscovery(FOO, classLoader, module, LOADED);
        listener.onTransformation(typeDescription, classLoader, module, LOADED, dynamicType);
        listener.onError(FOO, classLoader, module, LOADED, throwable);
        listener.onIgnored(typeDescription, classLoader, module, LOADED);
        listener.onComplete(FOO, classLoader, module, LOADED);
        verifyZeroInteractions(delegate);
    }

    @Test
    public void testFilteringMatch() throws Exception {
        AgentBuilder.Listener delegate = mock(AgentBuilder.Listener.class);
        AgentBuilder.Listener listener = new AgentBuilder.Listener.Filtering(ElementMatchers.any(), delegate);
        listener.onDiscovery(FOO, classLoader, module, LOADED);
        listener.onTransformation(typeDescription, classLoader, module, LOADED, dynamicType);
        listener.onError(FOO, classLoader, module, LOADED, throwable);
        listener.onIgnored(typeDescription, classLoader, module, LOADED);
        listener.onComplete(FOO, classLoader, module, LOADED);
        verify(delegate).onDiscovery(FOO, classLoader, module, LOADED);
        verify(delegate).onTransformation(typeDescription, classLoader, module, LOADED, dynamicType);
        verify(delegate).onError(FOO, classLoader, module, LOADED, throwable);
        verify(delegate).onIgnored(typeDescription, classLoader, module, LOADED);
        verify(delegate).onComplete(FOO, classLoader, module, LOADED);
        verifyNoMoreInteractions(delegate);
    }

    @Test
    public void testReadEdgeAddingListenerNotSupported() throws Exception {
        Instrumentation instrumentation = mock(Instrumentation.class);
        AgentBuilder.Listener listener = new AgentBuilder.Listener.ModuleReadEdgeCompleting(instrumentation, false, Collections.<JavaModule>emptySet());
        listener.onTransformation(mock(TypeDescription.class), mock(ClassLoader.class), JavaModule.UNSUPPORTED, LOADED, mock(DynamicType.class));
    }

    @Test
    public void testReadEdgeAddingListenerUnnamed() throws Exception {
        Instrumentation instrumentation = mock(Instrumentation.class);
        JavaModule source = mock(JavaModule.class), target = mock(JavaModule.class);
        AgentBuilder.Listener listener = new AgentBuilder.Listener.ModuleReadEdgeCompleting(instrumentation, false, Collections.singleton(target));
        listener.onTransformation(mock(TypeDescription.class), mock(ClassLoader.class), source, LOADED, mock(DynamicType.class));
        verify(source).isNamed();
        verifyNoMoreInteractions(source);
        verifyZeroInteractions(target);
    }

    @Test
    public void testReadEdgeAddingListenerCanRead() throws Exception {
        Instrumentation instrumentation = mock(Instrumentation.class);
        JavaModule source = mock(JavaModule.class), target = mock(JavaModule.class);
        when(source.isNamed()).thenReturn(true);
        when(source.canRead(target)).thenReturn(true);
        AgentBuilder.Listener listener = new AgentBuilder.Listener.ModuleReadEdgeCompleting(instrumentation, false, Collections.singleton(target));
        listener.onTransformation(mock(TypeDescription.class), mock(ClassLoader.class), source, LOADED, mock(DynamicType.class));
        verify(source).isNamed();
        verify(source).canRead(target);
        verifyNoMoreInteractions(source);
        verifyZeroInteractions(target);
    }

    @Test
    public void testReadEdgeAddingListenerNamedCannotRead() throws Exception {
        Instrumentation instrumentation = mock(Instrumentation.class);
        JavaModule source = mock(JavaModule.class), target = mock(JavaModule.class);
        when(source.isNamed()).thenReturn(true);
        when(source.canRead(target)).thenReturn(false);
        AgentBuilder.Listener listener = new AgentBuilder.Listener.ModuleReadEdgeCompleting(instrumentation, false, Collections.singleton(target));
        listener.onTransformation(mock(TypeDescription.class), mock(ClassLoader.class), source, LOADED, mock(DynamicType.class));
        verify(source).isNamed();
        verify(source).canRead(target);
        verify(source).addReads(instrumentation, target);
        verifyNoMoreInteractions(source);
        verifyZeroInteractions(target);
    }

    @Test
    public void testReadEdgeAddingListenerDuplexNotSupported() throws Exception {
        Instrumentation instrumentation = mock(Instrumentation.class);
        AgentBuilder.Listener listener = new AgentBuilder.Listener.ModuleReadEdgeCompleting(instrumentation, true, Collections.<JavaModule>emptySet());
        listener.onTransformation(mock(TypeDescription.class), mock(ClassLoader.class), JavaModule.UNSUPPORTED, LOADED, mock(DynamicType.class));
    }

    @Test
    public void testReadEdgeAddingListenerDuplexUnnamed() throws Exception {
        Instrumentation instrumentation = mock(Instrumentation.class);
        JavaModule source = mock(JavaModule.class), target = mock(JavaModule.class);
        AgentBuilder.Listener listener = new AgentBuilder.Listener.ModuleReadEdgeCompleting(instrumentation, true, Collections.singleton(target));
        listener.onTransformation(mock(TypeDescription.class), mock(ClassLoader.class), source, LOADED, mock(DynamicType.class));
        verify(source).isNamed();
        verifyNoMoreInteractions(source);
        verifyZeroInteractions(target);
    }

    @Test
    public void testReadEdgeAddingListenerDuplexCanRead() throws Exception {
        Instrumentation instrumentation = mock(Instrumentation.class);
        JavaModule source = mock(JavaModule.class), target = mock(JavaModule.class);
        when(source.isNamed()).thenReturn(true);
        when(source.canRead(target)).thenReturn(true);
        when(target.canRead(source)).thenReturn(true);
        AgentBuilder.Listener listener = new AgentBuilder.Listener.ModuleReadEdgeCompleting(instrumentation, true, Collections.singleton(target));
        listener.onTransformation(mock(TypeDescription.class), mock(ClassLoader.class), source, LOADED, mock(DynamicType.class));
        verify(source).isNamed();
        verify(source).canRead(target);
        verifyNoMoreInteractions(source);
        verify(target).canRead(source);
        verifyNoMoreInteractions(target);
    }

    @Test
    public void testReadEdgeAddingListenerNamedDuplexCannotRead() throws Exception {
        Instrumentation instrumentation = mock(Instrumentation.class);
        JavaModule source = mock(JavaModule.class), target = mock(JavaModule.class);
        when(source.isNamed()).thenReturn(true);
        when(source.canRead(target)).thenReturn(false);
        when(target.canRead(source)).thenReturn(false);
        AgentBuilder.Listener listener = new AgentBuilder.Listener.ModuleReadEdgeCompleting(instrumentation, true, Collections.singleton(target));
        listener.onTransformation(mock(TypeDescription.class), mock(ClassLoader.class), source, LOADED, mock(DynamicType.class));
        verify(source).isNamed();
        verify(source).canRead(target);
        verify(source).addReads(instrumentation, target);
        verifyNoMoreInteractions(source);
        verify(target).canRead(source);
        verify(target).addReads(instrumentation, source);
        verifyNoMoreInteractions(target);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AgentBuilder.Listener.NoOp.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.Listener.StreamWriting.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.Listener.Filtering.class).apply();
        ObjectPropertyAssertion.of(AgentBuilder.Listener.Compound.class).create(new ObjectPropertyAssertion.Creator<List<?>>() {
            @Override
            public List<?> create() {
                return Collections.singletonList(mock(AgentBuilder.Listener.class));
            }
        }).apply();
        ObjectPropertyAssertion.of(AgentBuilder.Listener.ModuleReadEdgeCompleting.class).apply();
    }

    private static class PseudoAdapter extends AgentBuilder.Listener.Adapter {
        /* empty */
    }
}
