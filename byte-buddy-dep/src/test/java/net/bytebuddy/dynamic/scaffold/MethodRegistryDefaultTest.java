package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.LoadedTypeInitializer;
import net.bytebuddy.implementation.attribute.MethodAttributeAppender;
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
import static org.mockito.Mockito.*;

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
    private TypeWriter.MethodPool.Record firstRecord, secondRecord;

    @Mock
    private MethodAttributeAppender.Factory firstFactory, secondFactory;

    @Mock
    private MethodAttributeAppender firstAppender, secondAppender;

    @Mock
    private InstrumentedType firstType, secondType, thirdType;

    @Mock
    private TypeDescription typeDescription;

    @Mock
    private MethodDescription instrumentedMethod, firstMethod, secondMethod, thirdMethod;

    @Mock
    private MethodGraph.Compiler methodGraphCompiler;

    @Mock
    private MethodGraph methodGraph;

    @Mock
    private InstrumentedType.TypeInitializer typeInitializer;

    @Mock
    private LoadedTypeInitializer loadedTypeInitializer;

    @Mock
    private ElementMatcher<? super MethodDescription> resolvedMethodFilter, firstFilter, secondFilter;

    @Mock
    private Implementation.Target.Factory implementationTargetFactory;

    @Mock
    private Implementation.Target implementationTarget;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(firstHandler.prepare(firstType)).thenReturn(secondType);
        when(secondHandler.prepare(secondType)).thenReturn(thirdType);
        when(firstHandler.compile(implementationTarget)).thenReturn(firstCompiledHandler);
        when(secondHandler.compile(implementationTarget)).thenReturn(secondCompiledHandler);
        when(thirdType.getTypeInitializer()).thenReturn(typeInitializer);
        when(thirdType.getLoadedTypeInitializer()).thenReturn(loadedTypeInitializer);
//        when(methodLookupEngine.process(thirdType)).thenReturn(finding);
//        when(finding.getTypeDescription()).thenReturn(typeDescription);
//        when(finding.getInvokableMethods()).thenReturn(new MethodList.Explicit(Collections.singletonList(instrumentedMethod)));
        when(firstType.getDeclaredMethods()).thenReturn(new MethodList.Explicit(Collections.singletonList(firstMethod)));
        when(secondType.getDeclaredMethods()).thenReturn(new MethodList.Explicit(Collections.singletonList(secondMethod)));
        when(thirdType.getDeclaredMethods()).thenReturn(new MethodList.Explicit(Collections.singletonList(thirdMethod)));
        when(methodFilter.resolve(thirdType)).thenReturn((ElementMatcher) resolvedMethodFilter);
        when(firstMatcher.resolve(thirdType)).thenReturn((ElementMatcher) firstFilter);
        when(secondMatcher.resolve(thirdType)).thenReturn((ElementMatcher) secondFilter);
        when(firstFactory.make(typeDescription)).thenReturn(firstAppender);
        when(secondFactory.make(typeDescription)).thenReturn(secondAppender);
//        when(implementationTargetFactory.make(finding, new MethodList.Explicit(Collections.singletonList(instrumentedMethod))))
//                .thenReturn(implementationTarget);
        when(firstCompiledHandler.assemble(firstAppender, instrumentedMethod)).thenReturn(firstRecord);
        when(secondCompiledHandler.assemble(secondAppender, instrumentedMethod)).thenReturn(secondRecord);
    }

    @Test
    public void testNonMatchedIsNotIncluded() throws Exception {
        when(resolvedMethodFilter.matches(instrumentedMethod)).thenReturn(true);
        MethodRegistry.Prepared methodRegistry = new MethodRegistry.Default()
                .append(firstMatcher, firstHandler, firstFactory)
                .append(secondMatcher, secondHandler, secondFactory)
                .prepare(firstType, methodGraphCompiler, methodFilter);
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
                .prepare(firstType, methodGraphCompiler, methodFilter);
        assertThat(methodRegistry.getInstrumentedType(), is(typeDescription));
        assertThat(methodRegistry.getInstrumentedMethods().size(), is(0));
        assertThat(methodRegistry.getTypeInitializer(), is(typeInitializer));
        assertThat(methodRegistry.getLoadedTypeInitializer(), is(loadedTypeInitializer));
        verify(firstHandler).prepare(firstType);
        verify(secondHandler).prepare(secondType);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMatchedFirst() throws Exception {
        when(resolvedMethodFilter.matches(instrumentedMethod)).thenReturn(true);
        when(firstFilter.matches(instrumentedMethod)).thenReturn(true);
        MethodRegistry.Prepared methodRegistry = new MethodRegistry.Default()
                .append(firstMatcher, firstHandler, firstFactory)
                .append(secondMatcher, secondHandler, secondFactory)
                .prepare(firstType, methodGraphCompiler, methodFilter);
        assertThat(methodRegistry.getInstrumentedType(), is(typeDescription));
        assertThat(methodRegistry.getInstrumentedMethods(), is((MethodList) new MethodList.Explicit(Collections.singletonList(instrumentedMethod))));
        assertThat(methodRegistry.getTypeInitializer(), is(typeInitializer));
        assertThat(methodRegistry.getLoadedTypeInitializer(), is(loadedTypeInitializer));
        verify(firstHandler).prepare(firstType);
        verify(secondHandler).prepare(secondType);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMatchedSecond() throws Exception {
        when(resolvedMethodFilter.matches(instrumentedMethod)).thenReturn(true);
        when(secondFilter.matches(instrumentedMethod)).thenReturn(true);
        MethodRegistry.Prepared methodRegistry = new MethodRegistry.Default()
                .append(firstMatcher, firstHandler, firstFactory)
                .append(secondMatcher, secondHandler, secondFactory)
                .prepare(firstType, methodGraphCompiler, methodFilter);
        assertThat(methodRegistry.getInstrumentedType(), is(typeDescription));
        assertThat(methodRegistry.getInstrumentedMethods(), is((MethodList) new MethodList.Explicit(Collections.singletonList(instrumentedMethod))));
        assertThat(methodRegistry.getTypeInitializer(), is(typeInitializer));
        assertThat(methodRegistry.getLoadedTypeInitializer(), is(loadedTypeInitializer));
        verify(firstHandler).prepare(firstType);
        verify(secondHandler).prepare(secondType);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMultipleRegistryDoesNotPrepareMultipleTimes() throws Exception {
        when(resolvedMethodFilter.matches(instrumentedMethod)).thenReturn(true);
        when(firstFilter.matches(instrumentedMethod)).thenReturn(true);
        when(secondFilter.matches(instrumentedMethod)).thenReturn(true);
        MethodRegistry.Prepared methodRegistry = new MethodRegistry.Default()
                .append(firstMatcher, firstHandler, firstFactory)
                .append(firstMatcher, firstHandler, firstFactory)
                .append(secondMatcher, firstHandler, firstFactory)
                .append(secondMatcher, firstHandler, secondFactory)
                .append(secondMatcher, secondHandler, secondFactory)
                .append(firstMatcher, secondHandler, secondFactory)
                .append(firstMatcher, firstHandler, secondFactory)
                .append(firstMatcher, secondHandler, firstFactory)
                .prepare(firstType, methodGraphCompiler, methodFilter);
        assertThat(methodRegistry.getInstrumentedType(), is(typeDescription));
        assertThat(methodRegistry.getInstrumentedMethods(), is((MethodList) new MethodList.Explicit(Collections.singletonList(instrumentedMethod))));
        assertThat(methodRegistry.getTypeInitializer(), is(typeInitializer));
        assertThat(methodRegistry.getLoadedTypeInitializer(), is(loadedTypeInitializer));
        verify(firstHandler).prepare(firstType);
        verify(secondHandler).prepare(secondType);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCompiledAppendingMatchesFirstAppended() throws Exception {
        when(resolvedMethodFilter.matches(instrumentedMethod)).thenReturn(true);
        when(firstFilter.matches(instrumentedMethod)).thenReturn(true);
        when(secondFilter.matches(instrumentedMethod)).thenReturn(true);
        when(resolvedMethodFilter.matches(instrumentedMethod)).thenReturn(true);
        MethodRegistry.Compiled methodRegistry = new MethodRegistry.Default()
                .append(firstMatcher, firstHandler, firstFactory)
                .append(secondMatcher, secondHandler, secondFactory)
                .prepare(firstType, methodGraphCompiler, methodFilter)
                .compile(implementationTargetFactory);
        assertThat(methodRegistry.getInstrumentedType(), is(typeDescription));
        assertThat(methodRegistry.getInstrumentedMethods(), is((MethodList) new MethodList.Explicit(Collections.singletonList(instrumentedMethod))));
        assertThat(methodRegistry.getTypeInitializer(), is(typeInitializer));
        assertThat(methodRegistry.getLoadedTypeInitializer(), is(loadedTypeInitializer));
        verify(firstHandler).prepare(firstType);
        verify(secondHandler).prepare(secondType);
        verify(firstFactory).make(typeDescription);
        verifyZeroInteractions(secondFactory);
        assertThat(methodRegistry.target(instrumentedMethod), is(firstRecord));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCompiledPrependingMatchesLastPrepended() throws Exception {
        when(resolvedMethodFilter.matches(instrumentedMethod)).thenReturn(true);
        when(firstFilter.matches(instrumentedMethod)).thenReturn(true);
        when(secondFilter.matches(instrumentedMethod)).thenReturn(true);
        when(resolvedMethodFilter.matches(instrumentedMethod)).thenReturn(true);
        MethodRegistry.Compiled methodRegistry = new MethodRegistry.Default()
                .append(secondMatcher, secondHandler, secondFactory)
                .prepend(firstMatcher, firstHandler, firstFactory)
                .prepare(firstType, methodGraphCompiler, methodFilter)
                .compile(implementationTargetFactory);
        assertThat(methodRegistry.getInstrumentedType(), is(typeDescription));
        assertThat(methodRegistry.getInstrumentedMethods(), is((MethodList) new MethodList.Explicit(Collections.singletonList(instrumentedMethod))));
        assertThat(methodRegistry.getTypeInitializer(), is(typeInitializer));
        assertThat(methodRegistry.getLoadedTypeInitializer(), is(loadedTypeInitializer));
        verify(firstHandler).prepare(firstType);
        verify(secondHandler).prepare(secondType);
        verify(firstFactory).make(typeDescription);
        verifyZeroInteractions(secondFactory);
        assertThat(methodRegistry.target(instrumentedMethod), is(firstRecord));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCompiledAppendingMatchesSecondAppendedIfFirstDoesNotMatch() throws Exception {
        when(resolvedMethodFilter.matches(instrumentedMethod)).thenReturn(true);
        when(firstFilter.matches(instrumentedMethod)).thenReturn(false);
        when(secondFilter.matches(instrumentedMethod)).thenReturn(true);
        when(resolvedMethodFilter.matches(instrumentedMethod)).thenReturn(true);
        MethodRegistry.Compiled methodRegistry = new MethodRegistry.Default()
                .append(firstMatcher, firstHandler, firstFactory)
                .append(secondMatcher, secondHandler, secondFactory)
                .prepare(firstType, methodGraphCompiler, methodFilter)
                .compile(implementationTargetFactory);
        assertThat(methodRegistry.getInstrumentedType(), is(typeDescription));
        assertThat(methodRegistry.getInstrumentedMethods(), is((MethodList) new MethodList.Explicit(Collections.singletonList(instrumentedMethod))));
        assertThat(methodRegistry.getTypeInitializer(), is(typeInitializer));
        assertThat(methodRegistry.getLoadedTypeInitializer(), is(loadedTypeInitializer));
        verify(firstHandler).prepare(firstType);
        verify(secondHandler).prepare(secondType);
        verifyZeroInteractions(firstFactory);
        verify(secondFactory).make(typeDescription);
        assertThat(methodRegistry.target(instrumentedMethod), is(secondRecord));
    }

    @Test
    public void testSkipEntryIfNotMatched() throws Exception {
        when(resolvedMethodFilter.matches(instrumentedMethod)).thenReturn(true);
        when(firstFilter.matches(instrumentedMethod)).thenReturn(false);
        when(secondFilter.matches(instrumentedMethod)).thenReturn(false);
        when(resolvedMethodFilter.matches(instrumentedMethod)).thenReturn(true);
        MethodRegistry.Compiled methodRegistry = new MethodRegistry.Default()
                .append(firstMatcher, firstHandler, firstFactory)
                .append(secondMatcher, secondHandler, secondFactory)
                .prepare(firstType, methodGraphCompiler, methodFilter)
                .compile(implementationTargetFactory);
        assertThat(methodRegistry.getInstrumentedType(), is(typeDescription));
        assertThat(methodRegistry.getInstrumentedMethods().size(), is(0));
        assertThat(methodRegistry.getTypeInitializer(), is(typeInitializer));
        assertThat(methodRegistry.getLoadedTypeInitializer(), is(loadedTypeInitializer));
        verify(firstHandler).prepare(firstType);
        verify(secondHandler).prepare(secondType);
        verifyZeroInteractions(firstFactory);
        verifyZeroInteractions(secondFactory);
        assertThat(methodRegistry.target(instrumentedMethod), is((TypeWriter.MethodPool.Record) TypeWriter.MethodPool.Record.ForInheritedMethod.INSTANCE));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodRegistry.Default.class).apply();
        ObjectPropertyAssertion.of(MethodRegistry.Default.Entry.class).apply();
        ObjectPropertyAssertion.of(MethodRegistry.Default.Prepared.class).apply();
        ObjectPropertyAssertion.of(MethodRegistry.Default.Compiled.class).apply();
        ObjectPropertyAssertion.of(MethodRegistry.Default.Compiled.Entry.class).apply();
    }
}
