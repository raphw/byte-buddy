package net.bytebuddy.dynamic.scaffold.subclass;

import net.bytebuddy.dynamic.scaffold.MethodRegistry;
import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodList;
import net.bytebuddy.instrumentation.type.InstrumentedType;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static net.bytebuddy.matcher.ElementMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class ConstructorStrategyDefaultTest {
//
//    @Rule
//    public TestRule mockitoRule = new MockitoRule(this);
//
//    @Mock
//    private MethodRegistry methodRegistry;
//
//    @Mock
//    private MethodAttributeAppender.Factory methodAttributeAppenderFactory;
//
//    @Mock
//    private InstrumentedType instrumentedType, superType;
//
//    @Mock
//    private MethodList methodList, filteredMethodList;
//
//    @Before
//    public void setUp() throws Exception {
//        when(methodRegistry.append(any(MethodRegistry.LatentMethodMatcher.class),
//                any(Instrumentation.class),
//                any(MethodAttributeAppender.Factory.class))).thenReturn(methodRegistry);
//        when(instrumentedType.getSupertype()).thenReturn(superType);
//        when(superType.getDeclaredMethods()).thenReturn(methodList);
//    }
//
//    @Test
//    public void testNoConstructorsStrategy() throws Exception {
//        assertThat(ConstructorStrategy.Default.NO_CONSTRUCTORS.extractConstructors(instrumentedType).size(), is(0));
//        assertThat(ConstructorStrategy.Default.NO_CONSTRUCTORS.inject(methodRegistry, methodAttributeAppenderFactory), is(methodRegistry));
//        verifyZeroInteractions(methodRegistry);
//        verifyZeroInteractions(instrumentedType);
//    }
//
//    @Test
//    public void testImitateSuperTypeStrategy() throws Exception {
//        when(methodList.filter(isConstructor().<MethodDescription>and(isVisibleTo(instrumentedType)))).thenReturn(filteredMethodList);
//        assertThat(ConstructorStrategy.Default.IMITATE_SUPER_TYPE.extractConstructors(instrumentedType), is(filteredMethodList));
//        assertThat(ConstructorStrategy.Default.IMITATE_SUPER_TYPE.inject(methodRegistry, methodAttributeAppenderFactory), is(methodRegistry));
//        verify(methodRegistry).append(any(MethodRegistry.LatentMethodMatcher.class), any(Instrumentation.class), eq(methodAttributeAppenderFactory));
//        verifyNoMoreInteractions(methodRegistry);
//        verify(instrumentedType, atLeastOnce()).getSupertype();
//        verifyNoMoreInteractions(instrumentedType);
//    }
//
//    @Test
//    public void testImitateSuperTypePublicStrategy() throws Exception {
//        when(methodList.filter(isPublic().and(isConstructor()))).thenReturn(filteredMethodList);
//        assertThat(ConstructorStrategy.Default.IMITATE_SUPER_TYPE_PUBLIC.extractConstructors(instrumentedType), is(filteredMethodList));
//        assertThat(ConstructorStrategy.Default.IMITATE_SUPER_TYPE_PUBLIC.inject(methodRegistry, methodAttributeAppenderFactory), is(methodRegistry));
//        verify(methodRegistry).append(any(MethodRegistry.LatentMethodMatcher.class), any(Instrumentation.class), eq(methodAttributeAppenderFactory));
//        verifyNoMoreInteractions(methodRegistry);
//        verify(instrumentedType, atLeastOnce()).getSupertype();
//        verifyNoMoreInteractions(instrumentedType);
//    }
//
//    @Test
//    public void testDefaultConstructorStrategy() throws Exception {
//        when(methodList.filter(isConstructor().and(takesArguments(0)).<MethodDescription>and(isVisibleTo(instrumentedType)))).thenReturn(filteredMethodList);
//        when(filteredMethodList.size()).thenReturn(1);
//        assertThat(ConstructorStrategy.Default.DEFAULT_CONSTRUCTOR.extractConstructors(instrumentedType), is(filteredMethodList));
//        assertThat(ConstructorStrategy.Default.DEFAULT_CONSTRUCTOR.inject(methodRegistry, methodAttributeAppenderFactory), is(methodRegistry));
//        verify(methodRegistry).append(any(MethodRegistry.LatentMethodMatcher.class), any(Instrumentation.class), eq(methodAttributeAppenderFactory));
//        verifyNoMoreInteractions(methodRegistry);
//        verify(instrumentedType, atLeastOnce()).getSupertype();
//        verifyNoMoreInteractions(instrumentedType);
//    }
//
//    @Test(expected = IllegalArgumentException.class)
//    public void testDefaultConstructorStrategyNoDefault() throws Exception {
//        when(methodList.filter(isConstructor().and(takesArguments(0)).<MethodDescription>and(isVisibleTo(instrumentedType)))).thenReturn(filteredMethodList);
//        when(filteredMethodList.size()).thenReturn(0);
//        ConstructorStrategy.Default.DEFAULT_CONSTRUCTOR.extractConstructors(instrumentedType);
//    }
//
//    @Test
//    public void testObjectProperties() throws Exception {
//        ObjectPropertyAssertion.of(ConstructorStrategy.Default.class).apply();
//    }
}
