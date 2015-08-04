package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.implementation.attribute.AnnotationAppender;
import net.bytebuddy.implementation.attribute.FieldAttributeAppender;
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
    private FieldDescription knownField, unknownField;

    @Mock
    private FieldDescription.Token knownFieldToken;

    @Mock
    private Object defaultValue;

    @Before
    public void setUp() throws Exception {
        when(distinctFactory.make(instrumentedType)).thenReturn(distinct);
        when(knownField.asToken()).thenReturn(knownFieldToken);
    }

    @Test
    public void testNoFieldsRegistered() throws Exception {
        TypeWriter.FieldPool.Record record = new FieldRegistry.Default()
                .compile(instrumentedType)
                .target(unknownField);
        assertThat(record.getDefaultValue(), is(FieldDescription.NO_DEFAULT_VALUE));
        assertThat(record.getFieldAppender(), is((FieldAttributeAppender) new FieldAttributeAppender.ForField(unknownField,
                AnnotationAppender.ValueFilter.AppendDefaults.INSTANCE)));
    }

    @Test
    public void testKnownFieldRegistered() throws Exception {
        TypeWriter.FieldPool fieldPool = new FieldRegistry.Default()
                .include(knownFieldToken, distinctFactory, defaultValue)
                .compile(instrumentedType);
        assertThat(fieldPool.target(knownField).getFieldAppender(), is(distinct));
        assertThat(fieldPool.target(knownField).getDefaultValue(), is(defaultValue));
        assertThat(fieldPool.target(unknownField).getDefaultValue(), is(FieldDescription.NO_DEFAULT_VALUE));
        assertThat(fieldPool.target(unknownField).getFieldAppender(), is((FieldAttributeAppender) new FieldAttributeAppender.ForField(unknownField,
                AnnotationAppender.ValueFilter.AppendDefaults.INSTANCE)));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(FieldRegistry.Default.class).apply();
        ObjectPropertyAssertion.of(FieldRegistry.Default.Entry.class).apply();
        ObjectPropertyAssertion.of(FieldRegistry.Default.Compiled.Entry.class).apply();
    }
}
