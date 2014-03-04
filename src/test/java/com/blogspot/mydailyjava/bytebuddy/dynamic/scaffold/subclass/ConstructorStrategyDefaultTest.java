package com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.subclass;

import com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.MethodRegistry;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import com.blogspot.mydailyjava.bytebuddy.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.lang.reflect.Constructor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class ConstructorStrategyDefaultTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodRegistry methodRegistry;
    @Mock
    private MethodAttributeAppender.Factory methodAttributeAppenderFactory;

    @Before
    public void setUp() throws Exception {
        when(methodRegistry.append(any(MethodRegistry.LatentMethodMatcher.class),
                any(Instrumentation.class),
                any(MethodAttributeAppender.Factory.class))).thenReturn(methodRegistry);
    }

    @Test
    public void testNoConstructorsStrategy() throws Exception {
        assertThat(ConstructorStrategy.Default.NO_CONSTRUCTORS.extractConstructors(Object.class), is(new Constructor<?>[0]));
        assertThat(ConstructorStrategy.Default.NO_CONSTRUCTORS.inject(methodRegistry, methodAttributeAppenderFactory), is(methodRegistry));
        verifyZeroInteractions(methodRegistry);
    }

    @Test
    public void testImitateSuperTypeStrategy() throws Exception {
        assertThat(ConstructorStrategy.Default.IMITATE_SUPER_TYPE.extractConstructors(Object.class), is(Object.class.getDeclaredConstructors()));
        assertThat(ConstructorStrategy.Default.IMITATE_SUPER_TYPE.inject(methodRegistry, methodAttributeAppenderFactory), is(methodRegistry));
        verify(methodRegistry).append(any(MethodRegistry.LatentMethodMatcher.class), any(Instrumentation.class), eq(methodAttributeAppenderFactory));
        verifyNoMoreInteractions(methodRegistry);
    }
}
