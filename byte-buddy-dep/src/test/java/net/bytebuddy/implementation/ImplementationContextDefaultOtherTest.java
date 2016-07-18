package net.bytebuddy.implementation;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

public class ImplementationContextDefaultOtherTest {

//    @Test
//    public void testFactory() throws Exception {
//        assertThat(Implementation.Context.Default.Factory.INSTANCE.make(mock(TypeDescription.class),
//                mock(AuxiliaryType.NamingStrategy.class),
//                mock(TypeInitializer.class),
//                mock(ClassFileVersion.class)), instanceOf(Implementation.Context.Default.class));
//    }
//
//    @Test
//    public void testTypeInitializerNotRetained() throws Exception {
//        assertThat(new Implementation.Context.Default(mock(TypeDescription.class),
//                mock(AuxiliaryType.NamingStrategy.class),
//                mock(TypeInitializer.class),
//                mock(ClassFileVersion.class)).isRetainTypeInitializer(), is(false));
//    }
//
//    @Test
//    public void testFrozenTypeInitializerRetainsInitializer() throws Exception {
//        Implementation.Context.ExtractableView implementationContext = new Implementation.Context.Default(mock(TypeDescription.class),
//                mock(AuxiliaryType.NamingStrategy.class),
//                mock(TypeInitializer.class),
//                mock(ClassFileVersion.class));
//        implementationContext.prohibitTypeInitializer();
//        assertThat(implementationContext.isRetainTypeInitializer(), is(true));
//    }
//
//    @Test(expected = IllegalStateException.class)
//    public void testFrozenTypeInitializerFrozenThrowsExceptionOnDrain() throws Exception {
//        TypeDescription instrumentedType = mock(TypeDescription.class);
//        Implementation.Context.ExtractableView implementationContext = new Implementation.Context.Default(instrumentedType,
//                mock(AuxiliaryType.NamingStrategy.class),
//                mock(TypeInitializer.class),
//                mock(ClassFileVersion.class));
//        implementationContext.prohibitTypeInitializer();
//        TypeWriter.MethodPool methodPool = mock(TypeWriter.MethodPool.class);
//        TypeWriter.MethodPool.Record record = mock(TypeWriter.MethodPool.Record.class);
//        when(record.getSort()).thenReturn(TypeWriter.MethodPool.Record.Sort.DEFINED);
//        when(methodPool.target(new MethodDescription.Latent.TypeInitializer(instrumentedType), true)).thenReturn(record);
//        implementationContext.drain(mock(ClassVisitor.class),
//                methodPool,
//                mock(Implementation.Context.ExtractableView.InjectedCode.class),
//                mock(AnnotationValueFilter.Factory.class),
//                true);
//    }
//
//    @Test
//    public void testInstrumentationGetter() throws Exception {
//        TypeDescription instrumentedType = mock(TypeDescription.class);
//        assertThat(new Implementation.Context.Default(instrumentedType,
//                mock(AuxiliaryType.NamingStrategy.class),
//                mock(TypeInitializer.class),
//                mock(ClassFileVersion.class)).getInstrumentedType(), is(instrumentedType));
//    }

    // TODO

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(Implementation.Context.Default.class).applyBasic();
        ObjectPropertyAssertion.of(Implementation.Context.Default.FieldCacheEntry.class).apply();
        ObjectPropertyAssertion.of(Implementation.Context.Default.AccessorMethodDelegation.class).apply();
        ObjectPropertyAssertion.of(Implementation.Context.Default.FieldSetterDelegation.class).apply();
        ObjectPropertyAssertion.of(Implementation.Context.Default.FieldGetterDelegation.class).apply();
        ObjectPropertyAssertion.of(Implementation.Context.Default.Factory.class).apply();
    }
}
