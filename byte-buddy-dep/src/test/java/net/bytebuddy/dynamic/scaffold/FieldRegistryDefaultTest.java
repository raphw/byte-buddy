package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.instrumentation.attribute.FieldAttributeAppender;
import net.bytebuddy.instrumentation.field.FieldDescription;
import net.bytebuddy.instrumentation.type.InstrumentedType;
import net.bytebuddy.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class FieldRegistryDefaultTest {

    private static final String FOO = "foo";

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private InstrumentedType instrumentedType;
    @Mock
    private FieldAttributeAppender.Factory distinct;
    @Mock
    private TypeWriter.FieldPool.Entry fallback;
    @Mock
    private FieldDescription knownField, unknownField;
    @Mock
    private FieldRegistry.LatentFieldMatcher latentFieldMatcher;

    @Before
    public void setUp() throws Exception {
        when(latentFieldMatcher.getFieldName()).thenReturn(FOO);
        when(knownField.getInternalName()).thenReturn(FOO);
    }

    @Test
    public void testNoFieldsRegistered() throws Exception {
        assertThat(new FieldRegistry.Default()
                .compile(instrumentedType, fallback)
                .target(knownField), is(fallback));
        assertThat(new FieldRegistry.Default()
                .compile(instrumentedType, fallback)
                .target(unknownField), is(fallback));
    }

    @Test
    public void testKnownFieldRegistered() throws Exception {
        assertThat(new FieldRegistry.Default()
                .include(latentFieldMatcher, distinct)
                .compile(instrumentedType, fallback)
                .target(knownField)
                .getFieldAppenderFactory(), is(distinct));
        assertThat(new FieldRegistry.Default()
                .include(latentFieldMatcher, distinct)
                .compile(instrumentedType, fallback)
                .target(unknownField), is(fallback));
    }

    @Test
    public void testHashCodeEquals() throws Exception {
        assertThat(new FieldRegistry.Default().hashCode(), is(new FieldRegistry.Default().hashCode()));
        assertThat(new FieldRegistry.Default(), is(new FieldRegistry.Default()));
        assertThat(new FieldRegistry.Default().include(latentFieldMatcher, distinct).hashCode(), not(is(new FieldRegistry.Default().hashCode())));
        assertThat(new FieldRegistry.Default().include(latentFieldMatcher, distinct), not(is((FieldRegistry) new FieldRegistry.Default())));
    }

    @Test
    public void testCompiledHashCodeEquals() throws Exception {
        assertThat(new FieldRegistry.Default().compile(instrumentedType, fallback).hashCode(),
                is(new FieldRegistry.Default().compile(instrumentedType, fallback).hashCode()));
        assertThat(new FieldRegistry.Default().compile(instrumentedType, fallback),
                is(new FieldRegistry.Default().compile(instrumentedType, fallback)));
        assertThat(new FieldRegistry.Default().include(latentFieldMatcher, distinct).compile(instrumentedType, fallback).hashCode(),
                not(is(new FieldRegistry.Default().hashCode())));
        assertThat(new FieldRegistry.Default().include(latentFieldMatcher, distinct).compile(instrumentedType, fallback),
                not(is(new FieldRegistry.Default().compile(instrumentedType, fallback))));
    }
}
