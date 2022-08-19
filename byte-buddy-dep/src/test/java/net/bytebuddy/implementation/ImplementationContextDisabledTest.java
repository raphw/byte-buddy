package net.bytebuddy.implementation;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.scaffold.TypeInitializer;
import net.bytebuddy.dynamic.scaffold.TypeWriter;
import net.bytebuddy.implementation.auxiliary.AuxiliaryType;
import net.bytebuddy.implementation.bytecode.StackManipulation;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;

import static net.bytebuddy.test.utility.FieldByFieldComparison.hasPrototype;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ImplementationContextDisabledTest {

    @Rule
    public MethodRule mockitoRule = MockitoJUnit.rule().silent();

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
                mock(ClassFileVersion.class),
                Implementation.Context.FrameGeneration.DISABLED), hasPrototype((Implementation.Context.ExtractableView) new Implementation.Context.Disabled(instrumentedType,
                classFileVersion,
                Implementation.Context.FrameGeneration.DISABLED)));
    }

    @Test(expected = IllegalStateException.class)
    public void testFactoryWithTypeInitializer() throws Exception {
        TypeInitializer typeInitializer = mock(TypeInitializer.class);
        when(typeInitializer.isDefined()).thenReturn(true);
        Implementation.Context.Disabled.Factory.INSTANCE.make(instrumentedType,
                mock(AuxiliaryType.NamingStrategy.class),
                typeInitializer,
                mock(ClassFileVersion.class),
                mock(ClassFileVersion.class),
                Implementation.Context.FrameGeneration.DISABLED);
    }

    @Test
    public void testDisabled() throws Exception {
        assertThat(new Implementation.Context.Disabled(instrumentedType, classFileVersion, Implementation.Context.FrameGeneration.DISABLED).isEnabled(), is(false));
    }

    @Test
    public void testAuxiliaryTypes() throws Exception {
        assertThat(new Implementation.Context.Disabled(instrumentedType, classFileVersion, Implementation.Context.FrameGeneration.DISABLED).getAuxiliaryTypes().size(), is(0));
    }

    @Test(expected = IllegalStateException.class)
    public void testCannotCacheValue() throws Exception {
        new Implementation.Context.Disabled(instrumentedType, classFileVersion, Implementation.Context.FrameGeneration.DISABLED).cache(mock(StackManipulation.class), mock(TypeDescription.class));
    }

    @Test(expected = IllegalStateException.class)
    public void testCannotCreateFieldGetter() throws Exception {
        new Implementation.Context.Disabled(instrumentedType, classFileVersion, Implementation.Context.FrameGeneration.DISABLED).registerGetterFor(mock(FieldDescription.class), MethodAccessorFactory.AccessType.DEFAULT);
    }

    @Test(expected = IllegalStateException.class)
    public void testCannotCreateFieldSetter() throws Exception {
        new Implementation.Context.Disabled(instrumentedType, classFileVersion, Implementation.Context.FrameGeneration.DISABLED).registerSetterFor(mock(FieldDescription.class), MethodAccessorFactory.AccessType.DEFAULT);
    }

    @Test(expected = IllegalStateException.class)
    public void testCannotCreateMethodAccessor() throws Exception {
        new Implementation.Context.Disabled(instrumentedType, classFileVersion, Implementation.Context.FrameGeneration.DISABLED).registerAccessorFor(mock(Implementation.SpecialMethodInvocation.class), MethodAccessorFactory.AccessType.DEFAULT);
    }

    @Test(expected = IllegalStateException.class)
    public void testCannotRegisterAuxiliaryType() throws Exception {
        new Implementation.Context.Disabled(instrumentedType, classFileVersion, Implementation.Context.FrameGeneration.DISABLED).register(mock(AuxiliaryType.class));
    }

    @Test
    public void testClassFileVersion() throws Exception {
        assertThat(new Implementation.Context.Disabled(instrumentedType, classFileVersion, Implementation.Context.FrameGeneration.DISABLED).getClassFileVersion(), is(classFileVersion));
    }

    @Test
    public void testInstrumentationGetter() throws Exception {
        assertThat(new Implementation.Context.Disabled(instrumentedType, classFileVersion, Implementation.Context.FrameGeneration.DISABLED).getInstrumentedType(), is(instrumentedType));
    }
}
