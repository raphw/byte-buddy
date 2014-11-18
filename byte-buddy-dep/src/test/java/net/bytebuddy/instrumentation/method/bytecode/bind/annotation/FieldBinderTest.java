package net.bytebuddy.instrumentation.method.bytecode.bind.annotation;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.field.FieldDescription;
import net.bytebuddy.instrumentation.field.FieldList;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.utility.ObjectPropertyAssertion;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class FieldBinderTest {

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(Field.Binder.class).apply();
        ObjectPropertyAssertion.of(Field.Binder.FieldLocator.Legal.class).apply();
        ObjectPropertyAssertion.of(Field.Binder.FieldLocator.Illegal.class).apply();
        ObjectPropertyAssertion.of(Field.Binder.FieldLocator.LookupEngine.ForHierarchy.class).apply();
        ObjectPropertyAssertion.of(Field.Binder.FieldLocator.LookupEngine.ForExplicitType.class).apply();
        ObjectPropertyAssertion.of(Field.Binder.FieldLocator.LookupEngine.Illegal.class).apply();
        ObjectPropertyAssertion.of(Field.Binder.FieldLocator.Resolution.Resolved.class).apply();
        ObjectPropertyAssertion.of(Field.Binder.FieldLocator.Resolution.Unresolved.class).apply();
        ObjectPropertyAssertion.of(Field.Binder.InstanceFieldConstructor.class).apply();
        ObjectPropertyAssertion.of(Field.Binder.InstanceFieldConstructor.Appender.class).refine(new ObjectPropertyAssertion.Refinement<Instrumentation.Target>() {
            @Override
            public void apply(Instrumentation.Target mock) {
                TypeDescription typeDescription = Mockito.mock(TypeDescription.class);
                when(mock.getTypeDescription()).thenReturn(typeDescription);
                FieldList fieldList = Mockito.mock(FieldList.class);
                when(typeDescription.getDeclaredFields()).thenReturn(fieldList);
                when(fieldList.named(any(String.class))).thenReturn(Mockito.mock(FieldDescription.class));
            }
        }).skipSynthetic().apply();
        ObjectPropertyAssertion.of(Field.Binder.AccessType.Getter.class).apply();
        ObjectPropertyAssertion.of(Field.Binder.AccessType.Getter.Appender.class).refine(new ObjectPropertyAssertion.Refinement<Instrumentation.Target>() {
            @Override
            public void apply(Instrumentation.Target mock) {
                when(mock.getTypeDescription()).thenReturn(Mockito.mock(TypeDescription.class));
            }
        }).skipSynthetic().apply();
        ObjectPropertyAssertion.of(Field.Binder.AccessType.Setter.class).apply();
        ObjectPropertyAssertion.of(Field.Binder.AccessType.Setter.Appender.class).refine(new ObjectPropertyAssertion.Refinement<Instrumentation.Target>() {
            @Override
            public void apply(Instrumentation.Target mock) {
                when(mock.getTypeDescription()).thenReturn(Mockito.mock(TypeDescription.class));
            }
        }).skipSynthetic().apply();
        ObjectPropertyAssertion.of(Field.Binder.AccessorProxy.class).apply();
    }
}
