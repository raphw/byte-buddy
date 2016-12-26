package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.Transformer;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.LoadedTypeInitializer;
import net.bytebuddy.implementation.attribute.MethodAttributeAppender;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.matcher.LatentMatcher;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class MethodRegistryDefaultTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private ClassFileVersion classFileVersion;

    @Mock
    private LatentMatcher<MethodDescription> firstMatcher, secondMatcher, methodFilter;

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
    private MethodDescription instrumentedMethod;

    @Mock
    private MethodGraph.Compiler methodGraphCompiler;

    @Mock
    private MethodGraph.Linked methodGraph;

    @Mock
    private TypeInitializer typeInitializer;

    @Mock
    private LoadedTypeInitializer loadedTypeInitializer;

    @Mock
    private ElementMatcher<? super MethodDescription> resolvedMethodFilter, firstFilter, secondFilter;

    @Mock
    private Implementation.Target.Factory implementationTargetFactory;

    @Mock
    private Implementation.Target implementationTarget;

    @Mock
    private Transformer<MethodDescription> transformer;

    @Mock
    private TypeDescription returnType, parameterType;

    @Mock
    private TypeDescription.Generic genericReturnType, genericParameterType;

    @Mock
    private ParameterDescription.InDefinedShape parameterDescription;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(firstHandler.prepare(firstType)).thenReturn(secondType);
        when(secondHandler.prepare(secondType)).thenReturn(thirdType);
        when(firstHandler.compile(implementationTarget)).thenReturn(firstCompiledHandler);
        when(secondHandler.compile(implementationTarget)).thenReturn(secondCompiledHandler);
        when(thirdType.getTypeInitializer()).thenReturn(typeInitializer);
        when(thirdType.getLoadedTypeInitializer()).thenReturn(loadedTypeInitializer);
        when(methodGraphCompiler.compile(thirdType)).thenReturn(methodGraph);
        when(methodGraph.listNodes()).thenReturn(new MethodGraph.NodeList(Collections.singletonList(new MethodGraph.Node.Simple(instrumentedMethod))));
        when(firstType.getDeclaredMethods()).thenReturn(new MethodList.Empty<MethodDescription.InDefinedShape>());
        when(secondType.getDeclaredMethods()).thenReturn(new MethodList.Empty<MethodDescription.InDefinedShape>());
        when(thirdType.getDeclaredMethods()).thenReturn(new MethodList.Empty<MethodDescription.InDefinedShape>());
        when(methodFilter.resolve(thirdType)).thenReturn((ElementMatcher) resolvedMethodFilter);
        when(firstMatcher.resolve(thirdType)).thenReturn((ElementMatcher) firstFilter);
        when(secondMatcher.resolve(thirdType)).thenReturn((ElementMatcher) secondFilter);
        when(firstFactory.make(typeDescription)).thenReturn(firstAppender);
        when(secondFactory.make(typeDescription)).thenReturn(secondAppender);
        when(implementationTargetFactory.make(typeDescription, methodGraph, classFileVersion)).thenReturn(implementationTarget);
        when(firstCompiledHandler.assemble(instrumentedMethod, firstAppender, Visibility.PUBLIC)).thenReturn(firstRecord);
        when(secondCompiledHandler.assemble(instrumentedMethod, secondAppender, Visibility.PUBLIC)).thenReturn(secondRecord);
        when(transformer.transform(thirdType, instrumentedMethod)).thenReturn(instrumentedMethod);
        when(thirdType.validated()).thenReturn(typeDescription);
        when(implementationTarget.getInstrumentedType()).thenReturn(typeDescription);
        when(genericReturnType.asErasure()).thenReturn(returnType);
        when(genericReturnType.getSort()).thenReturn(TypeDefinition.Sort.NON_GENERIC);
        when(returnType.isVisibleTo(thirdType)).thenReturn(true);
        when(genericParameterType.asErasure()).thenReturn(parameterType);
        when(genericParameterType.getSort()).thenReturn(TypeDefinition.Sort.NON_GENERIC);
        when(parameterType.isVisibleTo(thirdType)).thenReturn(true);
        when(instrumentedMethod.getReturnType()).thenReturn(genericReturnType);
        when(instrumentedMethod.getParameters()).thenReturn((ParameterList) new ParameterList.Explicit<ParameterDescription>(parameterDescription));
        when(parameterDescription.getType()).thenReturn(genericParameterType);
        when(instrumentedMethod.getVisibility()).thenReturn(Visibility.PUBLIC);
    }

    @Test
    public void testNonMatchedIsNotIncluded() throws Exception {
        when(resolvedMethodFilter.matches(instrumentedMethod)).thenReturn(true);
        MethodRegistry.Prepared methodRegistry = new MethodRegistry.Default()
                .append(firstMatcher, firstHandler, firstFactory, transformer)
                .append(secondMatcher, secondHandler, secondFactory, transformer)
                .prepare(firstType, methodGraphCompiler, TypeValidation.ENABLED, methodFilter);
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
                .append(firstMatcher, firstHandler, firstFactory, transformer)
                .append(secondMatcher, secondHandler, secondFactory, transformer)
                .prepare(firstType, methodGraphCompiler, TypeValidation.ENABLED, methodFilter);
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
                .append(firstMatcher, firstHandler, firstFactory, transformer)
                .append(secondMatcher, secondHandler, secondFactory, transformer)
                .prepare(firstType, methodGraphCompiler, TypeValidation.ENABLED, methodFilter);
        assertThat(methodRegistry.getInstrumentedType(), is(typeDescription));
        assertThat(methodRegistry.getInstrumentedMethods(), is((MethodList) new MethodList.Explicit(instrumentedMethod)));
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
                .append(firstMatcher, firstHandler, firstFactory, transformer)
                .append(secondMatcher, secondHandler, secondFactory, transformer)
                .prepare(firstType, methodGraphCompiler, TypeValidation.ENABLED, methodFilter);
        assertThat(methodRegistry.getInstrumentedType(), is(typeDescription));
        assertThat(methodRegistry.getInstrumentedMethods(), is((MethodList) new MethodList.Explicit(instrumentedMethod)));
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
                .append(firstMatcher, firstHandler, firstFactory, transformer)
                .append(firstMatcher, firstHandler, firstFactory, transformer)
                .append(secondMatcher, firstHandler, firstFactory, transformer)
                .append(secondMatcher, firstHandler, secondFactory, transformer)
                .append(secondMatcher, secondHandler, secondFactory, transformer)
                .append(firstMatcher, secondHandler, secondFactory, transformer)
                .append(firstMatcher, firstHandler, secondFactory, transformer)
                .append(firstMatcher, secondHandler, firstFactory, transformer)
                .prepare(firstType, methodGraphCompiler, TypeValidation.ENABLED, methodFilter);
        assertThat(methodRegistry.getInstrumentedType(), is(typeDescription));
        assertThat(methodRegistry.getInstrumentedMethods(), is((MethodList) new MethodList.Explicit(instrumentedMethod)));
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
                .append(firstMatcher, firstHandler, firstFactory, transformer)
                .append(secondMatcher, secondHandler, secondFactory, transformer)
                .prepare(firstType, methodGraphCompiler, TypeValidation.ENABLED, methodFilter)
                .compile(implementationTargetFactory, classFileVersion);
        assertThat(methodRegistry.getInstrumentedType(), is(typeDescription));
        assertThat(methodRegistry.getInstrumentedMethods(), is((MethodList) new MethodList.Explicit(instrumentedMethod)));
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
                .append(secondMatcher, secondHandler, secondFactory, transformer)
                .prepend(firstMatcher, firstHandler, firstFactory, transformer)
                .prepare(firstType, methodGraphCompiler, TypeValidation.ENABLED, methodFilter)
                .compile(implementationTargetFactory, classFileVersion);
        assertThat(methodRegistry.getInstrumentedType(), is(typeDescription));
        assertThat(methodRegistry.getInstrumentedMethods(), is((MethodList) new MethodList.Explicit(instrumentedMethod)));
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
                .append(firstMatcher, firstHandler, firstFactory, transformer)
                .append(secondMatcher, secondHandler, secondFactory, transformer)
                .prepare(firstType, methodGraphCompiler, TypeValidation.ENABLED, methodFilter)
                .compile(implementationTargetFactory, classFileVersion);
        assertThat(methodRegistry.getInstrumentedType(), is(typeDescription));
        assertThat(methodRegistry.getInstrumentedMethods(), is((MethodList) new MethodList.Explicit(instrumentedMethod)));
        assertThat(methodRegistry.getTypeInitializer(), is(typeInitializer));
        assertThat(methodRegistry.getLoadedTypeInitializer(), is(loadedTypeInitializer));
        verify(firstHandler).prepare(firstType);
        verify(secondHandler).prepare(secondType);
        verifyZeroInteractions(firstFactory);
        verify(secondFactory).make(typeDescription);
        assertThat(methodRegistry.target(instrumentedMethod), is(secondRecord));
    }

    @Test
    public void testSkipEntryIfNotMatchedAndVisible() throws Exception {
        when(resolvedMethodFilter.matches(instrumentedMethod)).thenReturn(true);
        when(firstFilter.matches(instrumentedMethod)).thenReturn(false);
        when(secondFilter.matches(instrumentedMethod)).thenReturn(false);
        when(resolvedMethodFilter.matches(instrumentedMethod)).thenReturn(true);
        TypeDescription declaringType = mock(TypeDescription.class);
        when(declaringType.asErasure()).thenReturn(declaringType);
        when(instrumentedMethod.getDeclaringType()).thenReturn(declaringType);
        MethodRegistry.Compiled methodRegistry = new MethodRegistry.Default()
                .append(firstMatcher, firstHandler, firstFactory, transformer)
                .append(secondMatcher, secondHandler, secondFactory, transformer)
                .prepare(firstType, methodGraphCompiler, TypeValidation.ENABLED, methodFilter)
                .compile(implementationTargetFactory, classFileVersion);
        assertThat(methodRegistry.getInstrumentedType(), is(typeDescription));
        assertThat(methodRegistry.getInstrumentedMethods().size(), is(0));
        assertThat(methodRegistry.getTypeInitializer(), is(typeInitializer));
        assertThat(methodRegistry.getLoadedTypeInitializer(), is(loadedTypeInitializer));
        verify(firstHandler).prepare(firstType);
        verify(secondHandler).prepare(secondType);
        verifyZeroInteractions(firstFactory);
        verifyZeroInteractions(secondFactory);
        assertThat(methodRegistry.target(instrumentedMethod), instanceOf(TypeWriter.MethodPool.Record.ForNonImplementedMethod.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testVisibilityBridgeIfNotMatchedAndVisible() throws Exception {
        when(instrumentedMethod.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        when(parameterDescription.getDeclaredAnnotations()).thenReturn(new AnnotationList.Empty());
        when(resolvedMethodFilter.matches(instrumentedMethod)).thenReturn(true);
        when(firstFilter.matches(instrumentedMethod)).thenReturn(false);
        when(secondFilter.matches(instrumentedMethod)).thenReturn(false);
        when(resolvedMethodFilter.matches(instrumentedMethod)).thenReturn(true);
        TypeDescription declaringType = mock(TypeDescription.class);
        when(declaringType.asErasure()).thenReturn(declaringType);
        when(instrumentedMethod.getDeclaringType()).thenReturn(declaringType);
        when(thirdType.isPublic()).thenReturn(true);
        when(instrumentedMethod.isPublic()).thenReturn(true);
        when(declaringType.isPackagePrivate()).thenReturn(true);
        TypeDescription.Generic superClass = mock(TypeDescription.Generic.class);
        TypeDescription rawSuperClass = mock(TypeDescription.class);
        when(superClass.asErasure()).thenReturn(rawSuperClass);
        when(typeDescription.getSuperClass()).thenReturn(superClass);
        MethodDescription.Token methodToken = mock(MethodDescription.Token.class);
        when(instrumentedMethod.asToken(ElementMatchers.is(typeDescription))).thenReturn(methodToken);
        when(methodToken.accept(any(TypeDescription.Generic.Visitor.class))).thenReturn(methodToken);
        when(classFileVersion.isAtLeast(ClassFileVersion.JAVA_V5)).thenReturn(true);
        MethodRegistry.Compiled methodRegistry = new MethodRegistry.Default()
                .append(firstMatcher, firstHandler, firstFactory, transformer)
                .append(secondMatcher, secondHandler, secondFactory, transformer)
                .prepare(firstType, methodGraphCompiler, TypeValidation.ENABLED, methodFilter)
                .compile(implementationTargetFactory, classFileVersion);
        assertThat(methodRegistry.getInstrumentedType(), is(typeDescription));
        assertThat(methodRegistry.getInstrumentedMethods().size(), is(1));
        assertThat(methodRegistry.getTypeInitializer(), is(typeInitializer));
        assertThat(methodRegistry.getLoadedTypeInitializer(), is(loadedTypeInitializer));
        verify(firstHandler).prepare(firstType);
        verify(secondHandler).prepare(secondType);
        verifyZeroInteractions(firstFactory);
        verifyZeroInteractions(secondFactory);
        assertThat(methodRegistry.target(instrumentedMethod), instanceOf(TypeWriter.MethodPool.Record.ForDefinedMethod.OfVisibilityBridge.class));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(MethodRegistry.Default.class).apply();
        ObjectPropertyAssertion.of(MethodRegistry.Default.Entry.class).apply();
        ObjectPropertyAssertion.of(MethodRegistry.Default.Prepared.class).apply();
        ObjectPropertyAssertion.of(MethodRegistry.Default.Prepared.Entry.class).apply();
        ObjectPropertyAssertion.of(MethodRegistry.Default.Compiled.class).apply();
        ObjectPropertyAssertion.of(MethodRegistry.Default.Compiled.Entry.class).apply();
    }
}
