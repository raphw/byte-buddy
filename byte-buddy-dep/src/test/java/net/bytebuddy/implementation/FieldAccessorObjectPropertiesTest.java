package net.bytebuddy.implementation;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class FieldAccessorObjectPropertiesTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(FieldAccessor.Appender.class).apply();
        ObjectPropertyAssertion.of(FieldAccessor.ForNamedField.class).apply();
        ObjectPropertyAssertion.of(FieldAccessor.ForUnnamedField.class).apply();
        ObjectPropertyAssertion.of(FieldAccessor.FieldNameExtractor.ForBeanProperty.class).apply();
    }
}
