package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.LoadedTypeInitializer;
import net.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodList;
import net.bytebuddy.instrumentation.method.MethodLookupEngine;
import net.bytebuddy.instrumentation.type.InstrumentedType;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.LatentMethodMatcher;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MethodRegistryDefaultTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private LatentMethodMatcher firstMatcher, secondMatcher, methodFilter;

    @Mock
    private MethodRegistry.Handler firstHandler, secondHandler;

    @Mock
    private MethodRegistry.Handler.Compiled firstCompiledHandler, secondCompiledHandler;

    @Mock
    private MethodAttributeAppender.Factory firstFactory, secondFactory;

    @Mock
    private MethodAttributeAppender firstAppender, secondAppender;

    @Mock
    private InstrumentedType firstType, secondType, thirdType;

    @Mock
    private TypeDescription typeDescription;

    @Mock
    private MethodLookupEngine methodLookupEngine;

    @Mock
    private MethodDescription instrumentedMethod, firstMethod, secondMethod, thirdMethod;

    @Mock
    private MethodLookupEngine.Finding finding;

    @Mock
    private InstrumentedType.TypeInitializer typeInitializer;

    @Mock
    private LoadedTypeInitializer loadedTypeInitializer;

    @Mock
    private ElementMatcher<? super MethodDescription> resolvedMethodFilter, firstFilter, secondFilter;

    @Mock
    private Instrumentation.Target.Factory instrumentationTargetFactory;

    @Mock
    private Instrumentation.Target instrumentationTarget;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(firstHandler.prepare(firstType)).thenReturn(secondType);
        when(secondHandler.prepare(secondType)).thenReturn(thirdType);
        when(firstHandler.compile(instrumentationTarget)).thenReturn(firstCompiledHandler);
        when(secondHandler.compile(instrumentationTarget)).thenReturn(secondCompiledHandler);
        when(thirdType.getTypeInitializer()).thenReturn(typeInitializer);
        when(thirdType.getLoadedTypeInitializer()).thenReturn(loadedTypeInitializer);
        when(methodLookupEngine.process(thirdType)).thenReturn(finding);
        when(finding.getTypeDescription()).thenReturn(typeDescription);
        when(finding.getInvokableMethods()).thenReturn(new MethodList.Explicit(Collections.singletonList(instrumentedMethod)));
        when(firstType.getDeclaredMethods()).thenReturn(new MethodList.Explicit(Collections.singletonList(firstMethod)));
        when(secondType.getDeclaredMethods()).thenReturn(new MethodList.Explicit(Collections.singletonList(secondMethod)));
        when(thirdType.getDeclaredMethods()).thenReturn(new MethodList.Explicit(Collections.singletonList(thirdMethod)));
        when(methodFilter.resolve(thirdType)).thenReturn((ElementMatcher) resolvedMethodFilter);
        when(firstMatcher.resolve(thirdType)).thenReturn((ElementMatcher) firstFilter);
        when(secondMatcher.resolve(thirdType)).thenReturn((ElementMatcher) secondFilter);
        when(firstFactory.make(typeDescription)).thenReturn(firstAppender);
        when(secondFactory.make(typeDescription)).thenReturn(secondAppender);
        when(instrumentationTargetFactory.make(finding, new MethodList.Explicit(Collections.singletonList(instrumentedMethod))))
                .thenReturn(instrumentationTarget);
    }

    @Test
    public void testNonMatchedIsNotIncluded() throws Exception {
        when(resolvedMethodFilter.matches(instrumentedMethod)).thenReturn(true);
        MethodRegistry.Prepared methodRegistry = new MethodRegistry.Default()
                .append(firstMatcher, firstHandler, firstFactory)
                .append(secondMatcher, secondHandler, secondFactory)
                .prepare(firstType, methodLookupEngine, methodFilter);
        assertThat(methodRegistry.getInstrumentedType(), is(typeDescription));
        assertThat(methodRegistry.getInstrumentedMethods().size(), is(0));
        assertThat(methodRegistry.getTypeInitializer(), is(typeInitializer));
        assertThat(methodRegistry.getLoadedTypeInitializer(), is(loadedTypeInitializer));
        verify(firstHandler).prepare(firstType);
        verify(secondHandler).prepare(secondType);
    }

    @Test
    public void testIgnoredIsNotIncluded() throws Exception {
        when(firstFilter.matches(instrumentedMethod)).thenReturn(true);
        when(secondFilter.matches(instrumentedMethod)).thenReturn(true);
        MethodRegistry.Prepared methodRegistry = new MethodRegistry.Default()
                .append(firstMatcher, firstHandler, firstFactory)
                .append(secondMatcher, secondHandler, secondFactory)
                .prepare(firstType, methodLookupEngine, methodFilter);
        assertThat(methodRegistry.getInstrumentedType(), is(typeDescription));
        assertThat(methodRegistry.getInstrumentedMethods().size(), is(0));
        assertThat(methodRegistry.getTypeInitializer(), is(typeInitializer));
        assertThat(methodRegistry.getLoadedTypeInitializer(), is(loadedTypeInitializer));
        verify(firstHandler).prepare(firstType);
        verify(secondHandler).prepare(secondType);
    }

    @Test
    public void testMatchedFirst() throws Exception {
        when(resolvedMethodFilter.matches(instrumentedMethod)).thenReturn(true);
        when(firstFilter.matches(instrumentedMethod)).thenReturn(true);
        MethodRegistry.Prepared methodRegistry = new MethodRegistry.Default()
                .append(firstMatcher, firstHandler, firstFactory)
                .append(secondMatcher, secondHandler, secondFactory)
                .prepare(firstType, methodLookupEngine, methodFilter);
        assertThat(methodRegistry.getInstrumentedType(), is(typeDescription));
        assertThat(methodRegistry.getInstrumentedMethods(), is((MethodList) new MethodList.Explicit(Collections.singletonList(instrumentedMethod))));
        assertThat(methodRegistry.getTypeInitializer(), is(typeInitializer));
        assertThat(methodRegistry.getLoadedTypeInitializer(), is(loadedTypeInitializer));
        verify(firstHandler).prepare(firstType);
        verify(secondHandler).prepare(secondType);
    }

    @Test
    public void testMatchedSecond() throws Exception {
        when(resolvedMethodFilter.matches(instrumentedMethod)).thenReturn(true);
        when(secondFilter.matches(instrumentedMethod)).thenReturn(true);
        MethodRegistry.Prepared methodRegistry = new MethodRegistry.Default()
                .append(firstMatcher, firstHandler, firstFactory)
                .append(secondMatcher, secondHandler, secondFactory)
                .prepare(firstType, methodLookupEngine, methodFilter);
        assertThat(methodRegistry.getInstrumentedType(), is(typeDescription));
        assertThat(methodRegistry.getInstrumentedMethods(), is((MethodList) new MethodList.Explicit(Collections.singletonList(instrumentedMethod))));
        assertThat(methodRegistry.getTypeInitializer(), is(typeInitializer));
        assertThat(methodRegistry.getLoadedTypeInitializer(), is(loadedTypeInitializer));
        verify(firstHandler).prepare(firstType);
        verify(secondHandler).prepare(secondType);
    }

    @Test
    public void testMultipleRegistryDoesNotPrepareMultipleTimes() throws Exception {
        when(resolvedMethodFilter.matches(instrumentedMethod)).thenReturn(true);
        when(firstFilter.matches(instrumentedMethod)).thenReturn(true);
        when(secondFilter.matches(instrumentedMethod)).thenReturn(true);
        MethodRegistry.Prepared methodRegistry = new MethodRegistry.Default()
                .append(firstMatcher, firstHandler, firstFactory)
                .append(firstMatcher, firstHandler, firstFactory)
                .append(secondMatcher, secondHandler, secondFactory)
                .append(secondMatcher, firstHandler, secondFactory)
                .append(secondMatcher, firstHandler, firstFactory)
                .prepare(firstType, methodLookupEngine, methodFilter);
        assertThat(methodRegistry.getInstrumentedType(), is(typeDescription));
        assertThat(methodRegistry.getInstrumentedMethods(), is((MethodList) new MethodList.Explicit(Collections.singletonList(instrumentedMethod))));
        assertThat(methodRegistry.getTypeInitializer(), is(typeInitializer));
        assertThat(methodRegistry.getLoadedTypeInitializer(), is(loadedTypeInitializer));
        verify(firstHandler).prepare(firstType);
        verify(secondHandler).prepare(secondType);
    }

    @Test
    public void testCompiled() throws Exception {
        when(resolvedMethodFilter.matches(instrumentedMethod)).thenReturn(true);
        when(firstFilter.matches(instrumentedMethod)).thenReturn(true);
        when(secondFilter.matches(instrumentedMethod)).thenReturn(true);
        when(resolvedMethodFilter.matches(instrumentedMethod)).thenReturn(true);
        MethodRegistry.Compiled methodRegistry = new MethodRegistry.Default()
                .append(firstMatcher, firstHandler, firstFactory)
                .append(secondMatcher, secondHandler, secondFactory)
                .prepare(firstType, methodLookupEngine, methodFilter)
                .compile(instrumentationTargetFactory);
        assertThat(methodRegistry.getInstrumentedType(), is(typeDescription));
        assertThat(methodRegistry.getInstrumentedMethods(), is((MethodList) new MethodList.Explicit(Collections.singletonList(instrumentedMethod))));
        assertThat(methodRegistry.getTypeInitializer(), is(typeInitializer));
        assertThat(methodRegistry.getLoadedTypeInitializer(), is(loadedTypeInitializer));
        verify(firstHandler).prepare(firstType);
        verify(secondHandler).prepare(secondType);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodRegistry.Default.class).apply();
        ObjectPropertyAssertion.of(MethodRegistry.Default.Entry.class).apply();
        ObjectPropertyAssertion.of(MethodRegistry.Default.Prepared.class).apply();
        ObjectPropertyAssertion.of(MethodRegistry.Default.Compiled.class).apply();
    }
}
