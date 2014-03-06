package com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.FieldAttributeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.field.FieldDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.InstrumentedType;
import com.blogspot.mydailyjava.bytebuddy.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
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
}
