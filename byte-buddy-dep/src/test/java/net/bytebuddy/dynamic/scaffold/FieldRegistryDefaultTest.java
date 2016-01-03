package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.dynamic.FieldTransformer;
import net.bytebuddy.implementation.attribute.AnnotationAppender;
import net.bytebuddy.implementation.attribute.FieldAttributeAppender;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.LatentMatcher;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class FieldRegistryDefaultTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private InstrumentedType instrumentedType;

    @Mock
    private FieldAttributeAppender.Factory distinctFactory;

    @Mock
    private FieldAttributeAppender distinct;

    @Mock
    private FieldDescription knownField, unknownField, instrumentedField;

    @Mock
    private LatentMatcher<FieldDescription> latentMatcher;

    @Mock
    private ElementMatcher<FieldDescription> matcher;

    @Mock
    private Object defaultValue, otherDefaultValue;

    @Mock
    private FieldTransformer fieldTransformer;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(distinctFactory.make(instrumentedType)).thenReturn(distinct);
        when(latentMatcher.resolve(instrumentedType)).thenReturn((ElementMatcher) matcher);
        when(matcher.matches(knownField)).thenReturn(true);
        when(fieldTransformer.transform(instrumentedType, knownField)).thenReturn(instrumentedField);
    }

    @Test
    public void testNoFieldsRegistered() throws Exception {
        TypeWriter.FieldPool.Record record = new FieldRegistry.Default()
                .compile(instrumentedType)
                .target(unknownField);
        assertThat(record.resolveDefault(defaultValue), is(defaultValue));
        assertThat(record.getFieldAppender(), is((FieldAttributeAppender) FieldAttributeAppender.ForInstrumentedField.INSTANCE));
    }

    @Test
    public void testKnownFieldRegistered() throws Exception {
        TypeWriter.FieldPool fieldPool = new FieldRegistry.Default()
                .include(latentMatcher, distinctFactory, defaultValue, fieldTransformer)
                .compile(instrumentedType);
        assertThat(fieldPool.target(knownField).getField(), is(instrumentedField));
        assertThat(fieldPool.target(knownField).getFieldAppender(), is(distinct));
        assertThat(fieldPool.target(knownField).resolveDefault(otherDefaultValue), is(defaultValue));
        assertThat(fieldPool.target(unknownField).getField(), is(unknownField));
        assertThat(fieldPool.target(unknownField).resolveDefault(otherDefaultValue), is(otherDefaultValue));
        assertThat(fieldPool.target(unknownField).getFieldAppender(), is((FieldAttributeAppender) FieldAttributeAppender.ForInstrumentedField.INSTANCE));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(FieldRegistry.Default.class).apply();
        ObjectPropertyAssertion.of(FieldRegistry.Default.Entry.class).apply();
        ObjectPropertyAssertion.of(FieldRegistry.Default.Compiled.Entry.class).apply();
    }
}
