package net.bytebuddy.dynamic.scaffold.subclass;

import net.bytebuddy.description.ByteCodeElement;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.MethodTransformer;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.dynamic.scaffold.MethodRegistry;
import net.bytebuddy.implementation.attribute.MethodAttributeAppender;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.matcher.LatentMatcher;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class ConstructorStrategyDefaultTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodRegistry methodRegistry;

    @Mock
    private InstrumentedType instrumentedType;

    @Mock
    private TypeDescription.Generic superClass;

    @Mock
    private MethodList<?> methodList, filteredMethodList;

    @Mock
    private ByteCodeElement.Token.TokenList<MethodDescription.Token> filteredMethodTokenList;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(methodRegistry.append(any(LatentMatcher.class),
                any(MethodRegistry.Handler.class),
                any(MethodAttributeAppender.Factory.class),
                any(MethodTransformer.class))).thenReturn(methodRegistry);
        when(instrumentedType.getSuperClass()).thenReturn(superClass);
        when(superClass.getDeclaredMethods()).thenReturn((MethodList) methodList);
        when(filteredMethodList.asTokenList(ElementMatchers.is(instrumentedType))).thenReturn(filteredMethodTokenList);
    }

    @Test
    public void testNoConstructorsStrategy() throws Exception {
        assertThat(ConstructorStrategy.Default.NO_CONSTRUCTORS.extractConstructors(instrumentedType).size(), is(0));
        assertThat(ConstructorStrategy.Default.NO_CONSTRUCTORS.inject(methodRegistry), is(methodRegistry));
        verifyZeroInteractions(methodRegistry);
        verifyZeroInteractions(instrumentedType);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testImitateSuperTypeStrategy() throws Exception {
        when(methodList.filter(isConstructor().<MethodDescription>and(isVisibleTo(instrumentedType)))).thenReturn((MethodList) filteredMethodList);
        assertThat(ConstructorStrategy.Default.IMITATE_SUPER_TYPE.extractConstructors(instrumentedType),
                is((List<MethodDescription.Token>) filteredMethodTokenList));
        assertThat(ConstructorStrategy.Default.IMITATE_SUPER_TYPE.inject(methodRegistry), is(methodRegistry));
        verify(methodRegistry).append(any(LatentMatcher.class),
                any(MethodRegistry.Handler.class),
                eq(MethodAttributeAppender.ForInstrumentedMethod.INSTANCE),
                eq(MethodTransformer.NoOp.INSTANCE));
        verifyNoMoreInteractions(methodRegistry);
        verify(instrumentedType, atLeastOnce()).getSuperClass();
        verifyNoMoreInteractions(instrumentedType);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testImitateSuperTypePublicStrategy() throws Exception {
        when(methodList.filter(isPublic().and(isConstructor()))).thenReturn((MethodList) filteredMethodList);
        assertThat(ConstructorStrategy.Default.IMITATE_SUPER_TYPE_PUBLIC.extractConstructors(instrumentedType),
                is((List<MethodDescription.Token>) filteredMethodTokenList));
        assertThat(ConstructorStrategy.Default.IMITATE_SUPER_TYPE_PUBLIC.inject(methodRegistry), is(methodRegistry));
        verify(methodRegistry).append(any(LatentMatcher.class),
                any(MethodRegistry.Handler.class),
                eq(MethodAttributeAppender.ForInstrumentedMethod.INSTANCE),
                eq(MethodTransformer.NoOp.INSTANCE));
        verifyNoMoreInteractions(methodRegistry);
        verify(instrumentedType, atLeastOnce()).getSuperClass();
        verifyNoMoreInteractions(instrumentedType);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testDefaultConstructorStrategy() throws Exception {
        when(methodList.filter(isConstructor().and(takesArguments(0)).<MethodDescription>and(isVisibleTo(instrumentedType)))).thenReturn((MethodList) filteredMethodList);
        when(filteredMethodList.size()).thenReturn(1);
        assertThat(ConstructorStrategy.Default.DEFAULT_CONSTRUCTOR.extractConstructors(instrumentedType),
                is((List<MethodDescription.Token>) filteredMethodTokenList));
        assertThat(ConstructorStrategy.Default.DEFAULT_CONSTRUCTOR.inject(methodRegistry), is(methodRegistry));
        verify(methodRegistry).append(any(LatentMatcher.class),
                any(MethodRegistry.Handler.class),
                eq(MethodAttributeAppender.NoOp.INSTANCE),
                eq(MethodTransformer.NoOp.INSTANCE));
        verifyNoMoreInteractions(methodRegistry);
        verify(instrumentedType, atLeastOnce()).getSuperClass();
        verifyNoMoreInteractions(instrumentedType);
    }

    @Test(expected = IllegalArgumentException.class)
    @SuppressWarnings("unchecked")
    public void testDefaultConstructorStrategyNoDefault() throws Exception {
        when(methodList.filter(isConstructor().and(takesArguments(0)).<MethodDescription>and(isVisibleTo(instrumentedType))))
                .thenReturn((MethodList) filteredMethodList);
        when(filteredMethodList.size()).thenReturn(0);
        ConstructorStrategy.Default.DEFAULT_CONSTRUCTOR.extractConstructors(instrumentedType);
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(ConstructorStrategy.Default.class).apply();
    }
}
