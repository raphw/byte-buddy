package net.bytebuddy.agent.builder;

import net.bytebuddy.description.type.PackageDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.test.utility.JavaVersionRule;
import net.bytebuddy.utility.JavaModule;
import net.bytebuddy.utility.JavaType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import java.io.PrintStream;
import java.lang.instrument.Instrumentation;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.bytebuddy.matcher.ElementMatchers.none;
import static net.bytebuddy.test.utility.FieldByFieldComparison.hasPrototype;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class AgentBuilderListenerTest {

    private static final String FOO = "foo";

    private static final boolean LOADED = true;

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

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
        verifyNoMoreInteractions(dynamicType);
        AgentBuilder.Listener.NoOp.INSTANCE.onError(FOO, classLoader, module, LOADED, throwable);
        verifyNoMoreInteractions(throwable);
        AgentBuilder.Listener.NoOp.INSTANCE.onIgnored(typeDescription, classLoader, module, LOADED);
        AgentBuilder.Listener.NoOp.INSTANCE.onComplete(FOO, classLoader, module, LOADED);
    }

    @Test
    public void testPseudoAdapter() throws Exception {
        AgentBuilder.Listener listener = new PseudoAdapter();
        listener.onDiscovery(FOO, classLoader, module, LOADED);
        listener.onTransformation(typeDescription, classLoader, module, LOADED, dynamicType);
        verifyNoMoreInteractions(dynamicType);
        listener.onError(FOO, classLoader, module, LOADED, throwable);
        verifyNoMoreInteractions(throwable);
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
        verify(printStream).printf("[Byte Buddy] DISCOVERY %s [%s, %s, %s, loaded=%b]%n", FOO, classLoader, module, Thread.currentThread(), LOADED);
        verifyNoMoreInteractions(printStream);
    }

    @Test
    public void testStreamWritingOnTransformation() throws Exception {
        PrintStream printStream = mock(PrintStream.class);
        AgentBuilder.Listener listener = new AgentBuilder.Listener.StreamWriting(printStream);
        listener.onTransformation(typeDescription, classLoader, module, LOADED, dynamicType);
        verify(printStream).printf("[Byte Buddy] TRANSFORM %s [%s, %s, %s, loaded=%b]%n", FOO, classLoader, module, Thread.currentThread(), LOADED);
        verifyNoMoreInteractions(printStream);
    }

    @Test
    public void testStreamWritingOnError() throws Exception {
        PrintStream printStream = mock(PrintStream.class);
        AgentBuilder.Listener listener = new AgentBuilder.Listener.StreamWriting(printStream);
        listener.onError(FOO, classLoader, module, LOADED, throwable);
        verify(printStream).printf("[Byte Buddy] ERROR %s [%s, %s, %s, loaded=%b]%n", FOO, classLoader, module, Thread.currentThread(), LOADED);
        verifyNoMoreInteractions(printStream);
        verify(throwable).printStackTrace(printStream);
        verifyNoMoreInteractions(throwable);
    }

    @Test
    public void testStreamWritingOnComplete() throws Exception {
        PrintStream printStream = mock(PrintStream.class);
        AgentBuilder.Listener listener = new AgentBuilder.Listener.StreamWriting(printStream);
        listener.onComplete(FOO, classLoader, module, LOADED);
        verify(printStream).printf("[Byte Buddy] COMPLETE %s [%s, %s, %s, loaded=%b]%n", FOO, classLoader, module, Thread.currentThread(), LOADED);
        verifyNoMoreInteractions(printStream);
    }

    @Test
    public void testStreamWritingOnIgnore() throws Exception {
        PrintStream printStream = mock(PrintStream.class);
        AgentBuilder.Listener listener = new AgentBuilder.Listener.StreamWriting(printStream);
        listener.onIgnored(typeDescription, classLoader, module, LOADED);
        verify(printStream).printf("[Byte Buddy] IGNORE %s [%s, %s, %s, loaded=%b]%n", FOO, classLoader, module, Thread.currentThread(), LOADED);
        verifyNoMoreInteractions(printStream);
    }

    @Test
    public void testStreamWritingStandardOutput() throws Exception {
        assertThat(AgentBuilder.Listener.StreamWriting.toSystemOut(), hasPrototype((AgentBuilder.Listener) new AgentBuilder.Listener.StreamWriting(System.out)));
    }

    @Test
    public void testStreamWritingStandardError() throws Exception {
        assertThat(AgentBuilder.Listener.StreamWriting.toSystemError(), hasPrototype((AgentBuilder.Listener) new AgentBuilder.Listener.StreamWriting(System.err)));
    }

    @Test
    public void testStreamWritingTransformationsOnly() throws Exception {
        PrintStream target = mock(PrintStream.class);
        assertThat(new AgentBuilder.Listener.StreamWriting(target).withTransformationsOnly(),
                hasPrototype((AgentBuilder.Listener) new AgentBuilder.Listener.WithTransformationsOnly(new AgentBuilder.Listener.StreamWriting(target))));
    }

    @Test
    public void testStreamWritingErrorOnly() throws Exception {
        PrintStream target = mock(PrintStream.class);
        assertThat(new AgentBuilder.Listener.StreamWriting(target).withErrorsOnly(),
                hasPrototype((AgentBuilder.Listener) new AgentBuilder.Listener.WithErrorsOnly(new AgentBuilder.Listener.StreamWriting(target))));
    }

    @Test
    public void testTransformationsOnly() {
        AgentBuilder.Listener delegate = mock(AgentBuilder.Listener.class);
        AgentBuilder.Listener listener = new AgentBuilder.Listener.WithTransformationsOnly(delegate);
        listener.onDiscovery(FOO, classLoader, module, LOADED);
        listener.onTransformation(typeDescription, classLoader, module, LOADED, dynamicType);
        listener.onError(FOO, classLoader, module, LOADED, throwable);
        listener.onIgnored(typeDescription, classLoader, module, LOADED);
        listener.onComplete(FOO, classLoader, module, LOADED);
        verify(delegate).onTransformation(typeDescription, classLoader, module, LOADED, dynamicType);
        verify(delegate).onError(FOO, classLoader, module, LOADED, throwable);
        verifyNoMoreInteractions(delegate);
    }

    @Test
    public void testErrorsOnly() {
        AgentBuilder.Listener delegate = mock(AgentBuilder.Listener.class);
        AgentBuilder.Listener listener = new AgentBuilder.Listener.WithErrorsOnly(delegate);
        listener.onDiscovery(FOO, classLoader, module, LOADED);
        listener.onTransformation(typeDescription, classLoader, module, LOADED, dynamicType);
        listener.onError(FOO, classLoader, module, LOADED, throwable);
        listener.onIgnored(typeDescription, classLoader, module, LOADED);
        listener.onComplete(FOO, classLoader, module, LOADED);
        verify(delegate).onError(FOO, classLoader, module, LOADED, throwable);
        verifyNoMoreInteractions(delegate);
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
        verifyNoMoreInteractions(delegate);
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
        verifyNoMoreInteractions(target);
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
        verifyNoMoreInteractions(target);
    }

    @Test
    @JavaVersionRule.Enforce(9)
    public void testReadEdgeAddingListenerNamedCannotRead() throws Exception {
        Instrumentation instrumentation = mock(Instrumentation.class);
        JavaModule source = mock(JavaModule.class), target = mock(JavaModule.class);
        when(source.isNamed()).thenReturn(true);
        when(source.canRead(target)).thenReturn(false);
        AgentBuilder.Listener listener = new AgentBuilder.Listener.ModuleReadEdgeCompleting(instrumentation, false, Collections.singleton(target));
        when(Instrumentation.class.getMethod("isModifiableModule", JavaType.MODULE.load()).invoke(instrumentation, (Object) null)).thenReturn(true);
        listener.onTransformation(mock(TypeDescription.class), mock(ClassLoader.class), source, LOADED, mock(DynamicType.class));
        verify(source).isNamed();
        verify(source).canRead(target);
        Instrumentation.class.getMethod("redefineModule",
                JavaType.MODULE.load(),
                Set.class,
                Map.class,
                Map.class,
                Set.class,
                Map.class).invoke(verify(instrumentation),
                null,
                Collections.singleton(null),
                Collections.<String, Set<JavaModule>>emptyMap(),
                Collections.<String, Set<JavaModule>>emptyMap(),
                Collections.<Class<?>>emptySet(),
                Collections.<Class<?>, List<Class<?>>>emptyMap());
        verify(source, times(2)).unwrap();
        verifyNoMoreInteractions(source);
        verify(target).unwrap();
        verifyNoMoreInteractions(target);
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
        verifyNoMoreInteractions(target);
    }

    @Test
    public void testReadEdgeAddingListenerDuplexCanRead() throws Exception {
        Instrumentation instrumentation = mock(Instrumentation.class);
        JavaModule source = mock(JavaModule.class), target = mock(JavaModule.class);
        TypeDescription typeDescription = mock(TypeDescription.class);
        PackageDescription packageDescription = mock(PackageDescription.class);
        when(typeDescription.getPackage()).thenReturn(packageDescription);
        when(source.isNamed()).thenReturn(true);
        when(source.canRead(target)).thenReturn(true);
        when(source.isOpened(packageDescription, target)).thenReturn(true);
        when(target.canRead(source)).thenReturn(true);
        AgentBuilder.Listener listener = new AgentBuilder.Listener.ModuleReadEdgeCompleting(instrumentation, true, Collections.singleton(target));
        listener.onTransformation(typeDescription, mock(ClassLoader.class), source, LOADED, mock(DynamicType.class));
        verify(source).isNamed();
        verify(source).canRead(target);
        verify(source).isOpened(packageDescription, target);
        verifyNoMoreInteractions(source);
        verify(target).canRead(source);
        verifyNoMoreInteractions(target);
    }

    @Test
    @JavaVersionRule.Enforce(9)
    public void testReadEdgeAddingListenerNamedDuplexCannotRead() throws Exception {
        Instrumentation instrumentation = mock(Instrumentation.class);
        JavaModule source = mock(JavaModule.class), target = mock(JavaModule.class);
        when(source.isNamed()).thenReturn(true);
        when(source.canRead(target)).thenReturn(false);
        when(target.canRead(source)).thenReturn(false);
        when(Instrumentation.class.getMethod("isModifiableModule", JavaType.MODULE.load()).invoke(instrumentation, (Object) null)).thenReturn(true);
        AgentBuilder.Listener listener = new AgentBuilder.Listener.ModuleReadEdgeCompleting(instrumentation, true, Collections.singleton(target));
        listener.onTransformation(mock(TypeDescription.class), mock(ClassLoader.class), source, LOADED, mock(DynamicType.class));
        Instrumentation.class.getMethod("redefineModule",
                JavaType.MODULE.load(),
                Set.class,
                Map.class,
                Map.class,
                Set.class,
                Map.class).invoke(verify(instrumentation, times(2)),
                null,
                Collections.singleton(null),
                Collections.<String, Set<JavaModule>>emptyMap(),
                Collections.<String, Set<JavaModule>>emptyMap(),
                Collections.<Class<?>>emptySet(),
                Collections.<Class<?>, List<Class<?>>>emptyMap());
        verify(source).isNamed();
        verify(source).canRead(target);
        verify(source, times(3)).unwrap();
        verifyNoMoreInteractions(source);
        verify(target).canRead(source);
        verify(target, times(3)).unwrap();
        verifyNoMoreInteractions(target);
    }

    private static class PseudoAdapter extends AgentBuilder.Listener.Adapter {
        /* empty */
    }
}
