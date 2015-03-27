package net.bytebuddy.dynamic;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class DynamicTypeBuilderObjectPropertiesTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(DynamicType.Builder.AbstractBase.DefaultExceptionDeclarableMethodInterception.class).skipSynthetic().apply();
        ObjectPropertyAssertion.of(DynamicType.Builder.AbstractBase.DefaultFieldValueTarget.class).skipSynthetic().apply();
        ObjectPropertyAssertion.of(DynamicType.Builder.AbstractBase.DefaultMatchedMethodInterception.class).skipSynthetic().apply();
        ObjectPropertyAssertion.of(DynamicType.Builder.AbstractBase.DefaultMethodAnnotationTarget.class).skipSynthetic().apply();
        ObjectPropertyAssertion.of(DynamicType.Builder.AbstractBase.DefaultOptionalMatchedMethodInterception.class).skipSynthetic().apply();
        ObjectPropertyAssertion.of(DynamicType.Builder.FieldValueTarget.NumericRangeValidator.class).apply();
    }
}
