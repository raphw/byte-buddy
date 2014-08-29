package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.instrumentation.method.MethodLookupEngine;
import net.bytebuddy.instrumentation.method.matcher.MethodMatcher;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class InlineDynamicTypeBuilderTargetHandlerForRedefinitionInstrumentationTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private MethodMatcher methodMatcher;
    @Mock
    private ClassFileVersion classFileVersion;
    @Mock
    private TypeDescription instrumentedType;
    @Mock
    private MethodLookupEngine.Factory methodLookupEngineFactory, other;
    @Mock
    private MethodLookupEngine methodLookupEngine, otherEngine;

    @Before
    public void setUp() throws Exception {
        when(methodLookupEngineFactory.make(any(boolean.class))).thenReturn(methodLookupEngine);
        when(other.make(any(boolean.class))).thenReturn(otherEngine);
    }

    @Test
    public void testHashCodeEquals() throws Exception {
        assertThat(InlineDynamicTypeBuilder.TargetHandler.ForRedefinitionInstrumentation.INSTANCE
                        .prepare(methodMatcher, classFileVersion, instrumentedType, methodLookupEngineFactory).hashCode(),
                is(InlineDynamicTypeBuilder.TargetHandler.ForRedefinitionInstrumentation.INSTANCE
                        .prepare(methodMatcher, classFileVersion, instrumentedType, methodLookupEngineFactory).hashCode()));
        assertThat(InlineDynamicTypeBuilder.TargetHandler.ForRedefinitionInstrumentation.INSTANCE
                        .prepare(methodMatcher, classFileVersion, instrumentedType, methodLookupEngineFactory),
                is(InlineDynamicTypeBuilder.TargetHandler.ForRedefinitionInstrumentation.INSTANCE
                        .prepare(methodMatcher, classFileVersion, instrumentedType, methodLookupEngineFactory)));
        assertThat(InlineDynamicTypeBuilder.TargetHandler.ForRedefinitionInstrumentation.INSTANCE
                        .prepare(methodMatcher, classFileVersion, instrumentedType, methodLookupEngineFactory).hashCode(),
                not(is(InlineDynamicTypeBuilder.TargetHandler.ForRedefinitionInstrumentation.INSTANCE
                        .prepare(methodMatcher, classFileVersion, instrumentedType, other).hashCode())));
        assertThat(InlineDynamicTypeBuilder.TargetHandler.ForRedefinitionInstrumentation.INSTANCE
                        .prepare(methodMatcher, classFileVersion, instrumentedType, methodLookupEngineFactory),
                not(is(InlineDynamicTypeBuilder.TargetHandler.ForRedefinitionInstrumentation.INSTANCE
                        .prepare(methodMatcher, classFileVersion, instrumentedType, other))));
    }
}
