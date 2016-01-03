package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.implementation.attribute.AnnotationAppender;
import net.bytebuddy.implementation.attribute.FieldAttributeAppender;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class FieldRegistryCompiledNoOpTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private FieldDescription fieldDescription;

    @Mock
    private Object defaultValue;

    @Test
    public void testReturnsNullDefaultValue() throws Exception {
        TypeWriter.FieldPool.Record record = FieldRegistry.Compiled.NoOp.INSTANCE.target(fieldDescription);
        assertThat(record.resolveDefault(defaultValue), is(defaultValue));
    }

    @Test
    public void testReturnsFieldAttributeAppender() throws Exception {
        TypeWriter.FieldPool.Record record = FieldRegistry.Compiled.NoOp.INSTANCE.target(fieldDescription);
        assertThat(record.getFieldAppender(), is((FieldAttributeAppender) FieldAttributeAppender.ForInstrumentedField.INSTANCE));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(FieldRegistry.Compiled.NoOp.class).apply();
    }
}
