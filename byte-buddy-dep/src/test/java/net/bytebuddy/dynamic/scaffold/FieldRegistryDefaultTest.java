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
    private FieldAttributeAppender.Factory distinctFactory;
    @Mock
    private FieldAttributeAppender distinct;
    @Mock
    private TypeWriter.FieldPool.Entry fallback;
    @Mock
    private FieldDescription knownField, unknownField;
    @Mock
    private FieldRegistry.LatentFieldMatcher latentFieldMatcher;

    @Before
    public void setUp() throws Exception {
        when(distinctFactory.make(instrumentedType)).thenReturn(distinct);
        when(latentFieldMatcher.getFieldName()).thenReturn(FOO);
        when(knownField.getInternalName()).thenReturn(FOO);
    }

    @Test
    public void testNoFieldsRegistered() throws Exception {
        assertThat(new FieldRegistry.Default()
                .prepare(instrumentedType)
                .compile(fallback)
                .target(knownField), is(fallback));
        assertThat(new FieldRegistry.Default()
                .prepare(instrumentedType)
                .compile(fallback)
                .target(unknownField), is(fallback));
    }

    @Test
    public void testKnownFieldRegistered() throws Exception {
        assertThat(new FieldRegistry.Default()
                .include(latentFieldMatcher, distinctFactory, null)
                .prepare(instrumentedType)
                .compile(fallback)
                .target(knownField)
                .getFieldAppender(), is(distinct));
        assertThat(new FieldRegistry.Default()
                .include(latentFieldMatcher, distinctFactory, null)
                .prepare(instrumentedType)
                .compile(fallback)
                .target(unknownField), is(fallback));
    }

    @Test
    public void testHashCodeEquals() throws Exception {
        assertThat(new FieldRegistry.Default().hashCode(), is(new FieldRegistry.Default().hashCode()));
        assertThat(new FieldRegistry.Default(), is(new FieldRegistry.Default()));
        assertThat(new FieldRegistry.Default().include(latentFieldMatcher, distinctFactory, null).hashCode(),
                not(is(new FieldRegistry.Default().hashCode())));
        assertThat(new FieldRegistry.Default().include(latentFieldMatcher, distinctFactory, null),
                not(is((FieldRegistry) new FieldRegistry.Default())));
    }

    @Test
    public void testCompiledHashCodeEquals() throws Exception {
        assertThat(new FieldRegistry.Default().prepare(instrumentedType).compile(fallback).hashCode(),
                is(new FieldRegistry.Default().prepare(instrumentedType).compile(fallback).hashCode()));
        assertThat(new FieldRegistry.Default().prepare(instrumentedType).compile(fallback),
                is(new FieldRegistry.Default().prepare(instrumentedType).compile(fallback)));
        assertThat(new FieldRegistry.Default()
                        .include(latentFieldMatcher, distinctFactory, null)
                        .prepare(instrumentedType)
                        .compile(fallback).hashCode(),
                not(is(new FieldRegistry.Default().hashCode())));
        assertThat(new FieldRegistry.Default()
                        .include(latentFieldMatcher, distinctFactory, null)
                        .prepare(instrumentedType)
                        .compile(fallback),
                not(is(new FieldRegistry.Default()
                        .prepare(instrumentedType)
                        .compile(fallback))));
    }
}
