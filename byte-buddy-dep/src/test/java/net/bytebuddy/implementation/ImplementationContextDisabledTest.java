package net.bytebuddy.implementation;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.TypeInitializer;
import net.bytebuddy.dynamic.scaffold.TypeWriter;
import net.bytebuddy.implementation.attribute.AnnotationValueFilter;
import net.bytebuddy.implementation.auxiliary.AuxiliaryType;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.objectweb.asm.ClassVisitor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ImplementationContextDisabledTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private TypeDescription instrumentedType;

    @Mock
    private ClassFileVersion classFileVersion;

    @Mock
    private TypeWriter.MethodPool methodPool;

    @Mock
    private TypeWriter.MethodPool.Record record;

    @Before
    public void setUp() throws Exception {
        when(methodPool.target(new MethodDescription.Latent.TypeInitializer(instrumentedType))).thenReturn(record);
    }

    @Test
    public void testFactory() throws Exception {
        assertThat(Implementation.Context.Disabled.Factory.INSTANCE.make(instrumentedType,
                mock(AuxiliaryType.NamingStrategy.class),
                mock(TypeInitializer.class),
                classFileVersion,
                mock(ClassFileVersion.class)), is((Implementation.Context.ExtractableView) new Implementation.Context.Disabled(instrumentedType, classFileVersion)));
    }

    @Test(expected = IllegalStateException.class)
    public void testFactoryWithTypeInitializer() throws Exception {
        TypeInitializer typeInitializer = mock(TypeInitializer.class);
        when(typeInitializer.isDefined()).thenReturn(true);
        Implementation.Context.Disabled.Factory.INSTANCE.make(instrumentedType,
                mock(AuxiliaryType.NamingStrategy.class),
                typeInitializer,
                mock(ClassFileVersion.class),
                mock(ClassFileVersion.class));
    }

    @Test
    public void testDisabled() throws Exception {
        assertThat(new Implementation.Context.Disabled(instrumentedType, classFileVersion).isEnabled(), is(false));
    }

    @Test
    public void testAuxiliaryTypes() throws Exception {
        assertThat(new Implementation.Context.Disabled(instrumentedType, classFileVersion).getAuxiliaryTypes().size(), is(0));
    }

    @Test(expected = IllegalStateException.class)
    public void testCannotCacheValue() throws Exception {
        new Implementation.Context.Disabled(instrumentedType, classFileVersion).cache(mock(StackManipulation.class), mock(TypeDescription.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testCannotCreateFieldGetter() throws Exception {
        new Implementation.Context.Disabled(instrumentedType, classFileVersion).registerGetterFor(mock(FieldDescription.class), MethodAccessorFactory.AccessType.DEFAULT);
    }

    @Test(expected = IllegalStateException.class)
    public void testCannotCreateFieldSetter() throws Exception {
        new Implementation.Context.Disabled(instrumentedType, classFileVersion).registerSetterFor(mock(FieldDescription.class), MethodAccessorFactory.AccessType.DEFAULT);
    }

    @Test(expected = IllegalStateException.class)
    public void testCannotCreateMethodAccessor() throws Exception {
        new Implementation.Context.Disabled(instrumentedType, classFileVersion).registerAccessorFor(mock(Implementation.SpecialMethodInvocation.class), MethodAccessorFactory.AccessType.DEFAULT);
    }

    @Test(expected = IllegalStateException.class)
    public void testCannotRegisterAuxiliaryType() throws Exception {
        new Implementation.Context.Disabled(instrumentedType, classFileVersion).register(mock(AuxiliaryType.class));
    }

    @Test
    public void testClassFileVersion() throws Exception {
        assertThat(new Implementation.Context.Disabled(instrumentedType, classFileVersion).getClassFileVersion(), is(classFileVersion));
    }

    @Test
    public void testInstrumentationGetter() throws Exception {
        assertThat(new Implementation.Context.Disabled(instrumentedType, classFileVersion).getInstrumentedType(), is(instrumentedType));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(Implementation.Context.Disabled.class).apply();
        ObjectPropertyAssertion.of(Implementation.Context.Disabled.Factory.class).apply();
    }
}
