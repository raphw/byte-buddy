package net.bytebuddy.instrumentation.type.auxiliary;

import net.bytebuddy.instrumentation.field.FieldDescription;
import net.bytebuddy.instrumentation.field.FieldList;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.utility.ObjectPropertyAssertion;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;

import static org.mockito.Mockito.when;

public class TypeProxyObjectPropertiesTest {

    @Test
    public void testEqualsHashCode() throws Exception {
        ObjectPropertyAssertion.of(TypeProxy.class).apply();
        ObjectPropertyAssertion.of(TypeProxy.MethodCall.class).skipSynthetic().apply();
        ObjectPropertyAssertion.of(TypeProxy.MethodCall.Appender.class).refine(new ObjectPropertyAssertion.Refinement<TypeDescription>() {
            @Override
            public void apply(TypeDescription mock) {
                FieldDescription fieldDescription = Mockito.mock(FieldDescription.class);
                when(fieldDescription.getInternalName()).thenReturn(TypeProxy.INSTANCE_FIELD);
                when(mock.getDeclaredFields()).thenReturn(new FieldList.Explicit(Arrays.asList(fieldDescription)));
            }
        }).skipSynthetic().apply();
        ObjectPropertyAssertion.of(TypeProxy.MethodCall.Appender.AccessorMethodInvocation .class).skipSynthetic().apply();
        ObjectPropertyAssertion.of(TypeProxy.SilentConstruction.Appender.class).skipSynthetic().apply();
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
