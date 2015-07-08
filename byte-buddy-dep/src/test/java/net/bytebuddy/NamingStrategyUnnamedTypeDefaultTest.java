package net.bytebuddy;

import net.bytebuddy.description.modifier.EnumerationState;
import net.bytebuddy.description.modifier.SyntheticState;
import net.bytebuddy.description.modifier.TypeManifestation;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.test.utility.MockitoRule;
import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;
import org.mockito.asm.Opcodes;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class NamingStrategyUnnamedTypeDefaultTest {

    private static final int MODIFIERS = 42;

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private GenericTypeDescription superType, interfaceType;

    @Mock
    private ClassFileVersion classFileVersion, other;

    private List<GenericTypeDescription> interfaceTypes;

    @Before
    public void setUp() throws Exception {
        interfaceTypes = Collections.singletonList(interfaceType);
    }

    @Test
    public void testProperties() throws Exception {
        NamingStrategy.UnnamedType unnamedType = new NamingStrategy.UnnamedType.Default(superType, interfaceTypes, MODIFIERS, classFileVersion);
        assertThat(unnamedType.getDeclaredInterfaces(), is((Collection<GenericTypeDescription>) interfaceTypes));
        assertThat(unnamedType.getClassFileVersion(), is(classFileVersion));
        assertThat(unnamedType.getSuperClass(), is(superType));
    }

    @Test
    public void testVisibilityProperty() throws Exception {
        assertThat(new NamingStrategy.UnnamedType.Default(superType, interfaceTypes, Opcodes.ACC_PRIVATE, classFileVersion).getVisibility(),
                is(Visibility.PRIVATE));
        assertThat(new NamingStrategy.UnnamedType.Default(superType, interfaceTypes, Opcodes.ACC_PROTECTED, classFileVersion).getVisibility(),
                is(Visibility.PROTECTED));
        assertThat(new NamingStrategy.UnnamedType.Default(superType, interfaceTypes, Opcodes.ACC_PUBLIC, classFileVersion).getVisibility(),
                is(Visibility.PUBLIC));
        assertThat(new NamingStrategy.UnnamedType.Default(superType, interfaceTypes, 0, classFileVersion).getVisibility(),
                is(Visibility.PACKAGE_PRIVATE));
    }

    @Test
    public void testSyntheticProperty() throws Exception {
        assertThat(new NamingStrategy.UnnamedType.Default(superType, interfaceTypes, Opcodes.ACC_SYNTHETIC, classFileVersion).getSyntheticState(),
                is(SyntheticState.SYNTHETIC));
        assertThat(new NamingStrategy.UnnamedType.Default(superType, interfaceTypes, 0, classFileVersion).getSyntheticState(),
                is(SyntheticState.PLAIN));
    }

    @Test
    public void testTypeManifestationProperty() throws Exception {
        assertThat(new NamingStrategy.UnnamedType.Default(superType, interfaceTypes, Opcodes.ACC_ABSTRACT, classFileVersion).getTypeManifestation(),
                is(TypeManifestation.ABSTRACT));
        assertThat(new NamingStrategy.UnnamedType.Default(superType, interfaceTypes, Opcodes.ACC_FINAL, classFileVersion).getTypeManifestation(),
                is(TypeManifestation.FINAL));
        assertThat(new NamingStrategy.UnnamedType.Default(superType, interfaceTypes, Opcodes.ACC_INTERFACE, classFileVersion).getTypeManifestation(),
                is(TypeManifestation.INTERFACE));
        assertThat(new NamingStrategy.UnnamedType.Default(superType, interfaceTypes, 0, classFileVersion).getTypeManifestation(),
                is(TypeManifestation.PLAIN));
    }

    @Test
    public void testEnumerationStateProperty() throws Exception {
        assertThat(new NamingStrategy.UnnamedType.Default(superType, interfaceTypes, Opcodes.ACC_ENUM, classFileVersion).getEnumerationState(),
                is(EnumerationState.ENUMERATION));
        assertThat(new NamingStrategy.UnnamedType.Default(superType, interfaceTypes, 0, classFileVersion).getEnumerationState(),
                is(EnumerationState.PLAIN));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(NamingStrategy.UnnamedType.Default.class).apply();
    }
}
