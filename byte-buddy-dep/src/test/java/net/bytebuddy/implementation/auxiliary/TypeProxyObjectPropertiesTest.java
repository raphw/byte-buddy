package net.bytebuddy.implementation.auxiliary;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;

import static org.mockito.Mockito.when;

public class TypeProxyObjectPropertiesTest {

    @Test
    @SuppressWarnings("unchecked")
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeProxy.class).apply();
        ObjectPropertyAssertion.of(TypeProxy.AbstractMethodErrorThrow.class).apply();
        ObjectPropertyAssertion.of(TypeProxy.SilentConstruction.class).apply();
        ObjectPropertyAssertion.of(TypeProxy.MethodCall.class).skipSynthetic().apply();
        ObjectPropertyAssertion.of(TypeProxy.MethodCall.Appender.class).refine(new ObjectPropertyAssertion.Refinement<TypeDescription>() {
            @Override
            public void apply(TypeDescription mock) {
                FieldDescription.InDefinedShape fieldDescription = Mockito.mock(FieldDescription.InDefinedShape.class);
                when(fieldDescription.getSourceCodeName()).thenReturn(TypeProxy.INSTANCE_FIELD);
                when(mock.getDeclaredFields()).thenReturn(new FieldList.Explicit<FieldDescription.InDefinedShape>(fieldDescription));
            }
        }).skipSynthetic().apply();
        ObjectPropertyAssertion.of(TypeProxy.MethodCall.Appender.AccessorMethodInvocation.class).skipSynthetic().apply();
        ObjectPropertyAssertion.of(TypeProxy.SilentConstruction.Appender.class).skipSynthetic().apply();
        ObjectPropertyAssertion.of(TypeProxy.InvocationFactory.Default.class).apply();
    }

    @Test
    public void testConstructorObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeProxy.ForSuperMethodByConstructor.class).apply();
    }

    @Test
    public void testReflectionFactoryObjectPropertiesFactoryEqualsHashCode() throws Exception {
        ObjectPropertyAssertion.of(TypeProxy.ForSuperMethodByReflectionFactory.class).apply();
    }

    @Test
    public void testDefaultMethodObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(TypeProxy.ForDefaultMethod.class).apply();
    }
}
