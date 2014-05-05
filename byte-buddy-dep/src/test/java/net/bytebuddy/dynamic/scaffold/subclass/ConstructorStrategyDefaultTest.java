package net.bytebuddy.dynamic.scaffold.subclass;

import net.bytebuddy.dynamic.scaffold.MethodRegistry;
import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.*;
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
    private TypeDescription stringType;
    private TypeDescription fooType;

    @Before
    public void setUp() throws Exception {
        when(methodRegistry.append(any(MethodRegistry.LatentMethodMatcher.class),
                any(Instrumentation.class),
                any(MethodAttributeAppender.Factory.class))).thenReturn(methodRegistry);
        stringType = new TypeDescription.ForLoadedType(String.class);
        fooType = new TypeDescription.ForLoadedType(Foo.class);
    }

    @Test
    public void testNoConstructorsStrategy() throws Exception {
        assertThat(ConstructorStrategy.Default.NO_CONSTRUCTORS.extractConstructors(stringType).size(), is(0));
        assertThat(ConstructorStrategy.Default.NO_CONSTRUCTORS.inject(methodRegistry, methodAttributeAppenderFactory), is(methodRegistry));
        verifyZeroInteractions(methodRegistry);
    }

    @Test
    public void testImitateSuperTypeStrategy() throws Exception {
        assertThat(ConstructorStrategy.Default.IMITATE_SUPER_TYPE.extractConstructors(stringType),
                is(stringType.getDeclaredMethods().filter(isConstructor().and(isProtected().or(isPublic())))));
        assertThat(ConstructorStrategy.Default.IMITATE_SUPER_TYPE.inject(methodRegistry, methodAttributeAppenderFactory), is(methodRegistry));
        verify(methodRegistry).append(any(MethodRegistry.LatentMethodMatcher.class), any(Instrumentation.class), eq(methodAttributeAppenderFactory));
        verifyNoMoreInteractions(methodRegistry);
    }

    @Test
    public void testDefaultConstructorStrategy() throws Exception {
        assertThat(ConstructorStrategy.Default.DEFAULT_CONSTRUCTOR.extractConstructors(stringType),
                is(stringType.getDeclaredMethods().filter(isConstructor().and(takesArguments(0)).and(isProtected().or(isPublic())))));
        assertThat(ConstructorStrategy.Default.DEFAULT_CONSTRUCTOR.inject(methodRegistry, methodAttributeAppenderFactory), is(methodRegistry));
        verify(methodRegistry).append(any(MethodRegistry.LatentMethodMatcher.class), any(Instrumentation.class), eq(methodAttributeAppenderFactory));
        verifyNoMoreInteractions(methodRegistry);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDefaultConstructorStrategyNoDefault() throws Exception {
        ConstructorStrategy.Default.DEFAULT_CONSTRUCTOR.extractConstructors(fooType);
    }

    @SuppressWarnings("unused")
    private static class Foo {

        public Foo(Void v) {
            /* empty */
        }
    }
}
