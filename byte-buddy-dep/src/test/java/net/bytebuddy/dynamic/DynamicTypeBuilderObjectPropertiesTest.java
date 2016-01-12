package net.bytebuddy.dynamic;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class DynamicTypeBuilderObjectPropertiesTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(DynamicType.Builder.AbstractBase.Adapter.TypeVariableDefinitionAdapter.class).apply();
        ObjectPropertyAssertion.of(DynamicType.Builder.AbstractBase.Adapter.FieldDefinitionAdapter.class).apply();
        ObjectPropertyAssertion.of(DynamicType.Builder.AbstractBase.Adapter.FieldMatchAdapter.class).apply();
        ObjectPropertyAssertion.of(DynamicType.Builder.AbstractBase.Adapter.MethodDefinitionAdapter.class).apply();
        ObjectPropertyAssertion.of(DynamicType.Builder.AbstractBase.Adapter.MethodDefinitionAdapter.TypeVariableAnnotationAdapter.class).apply();
        ObjectPropertyAssertion.of(DynamicType.Builder.AbstractBase.Adapter.MethodDefinitionAdapter.AnnotationAdapter.class).apply();
        ObjectPropertyAssertion.of(DynamicType.Builder.AbstractBase.Adapter.MethodDefinitionAdapter.ParameterAnnotationAdapter.class).apply();
        ObjectPropertyAssertion.of(DynamicType.Builder.AbstractBase.Adapter.MethodDefinitionAdapter.SimpleParameterAnnotationAdapter.class).apply();
        ObjectPropertyAssertion.of(DynamicType.Builder.AbstractBase.Adapter.MethodMatchAdapter.class).apply();
        ObjectPropertyAssertion.of(DynamicType.Builder.AbstractBase.Adapter.MethodMatchAdapter.AnnotationAdapter.class).apply();
        ObjectPropertyAssertion.of(DynamicType.Builder.AbstractBase.Adapter.OptionalMethodMatchAdapter.class).apply();
    }
}
