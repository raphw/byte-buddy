package net.bytebuddy.description.modifier;

import org.junit.Test;
import org.objectweb.asm.Opcodes;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ModifierContributorResolverTest {

    @Test
    public void testForType() throws Exception {
        assertThat(ModifierContributor.Resolver.of(Visibility.PUBLIC, TypeManifestation.FINAL).resolve(),
                is(Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL));
        assertThat(ModifierContributor.Resolver.of(Visibility.PUBLIC, TypeManifestation.INTERFACE, TypeManifestation.FINAL).resolve(),
                is(Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL));
        assertThat(ModifierContributor.Resolver.of(Visibility.PUBLIC, TypeManifestation.FINAL).resolve(1),
                is(Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | 1));
    }

    @Test
    public void testForField() throws Exception {
        assertThat(ModifierContributor.Resolver.of(Visibility.PUBLIC, FieldManifestation.FINAL).resolve(),
                is(Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL));
        assertThat(ModifierContributor.Resolver.of(Visibility.PUBLIC, FieldManifestation.VOLATILE, FieldManifestation.FINAL).resolve(),
                is(Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL));
        assertThat(ModifierContributor.Resolver.of(Visibility.PUBLIC, FieldManifestation.FINAL).resolve(1),
                is(Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | 1));
    }

    @Test
    public void testForMethod() throws Exception {
        assertThat(ModifierContributor.Resolver.of(Visibility.PUBLIC, MethodManifestation.FINAL).resolve(),
                is(Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL));
        assertThat(ModifierContributor.Resolver.of(Visibility.PUBLIC, MethodManifestation.ABSTRACT, MethodManifestation.FINAL).resolve(),
                is(Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL));
        assertThat(ModifierContributor.Resolver.of(Visibility.PUBLIC, MethodManifestation.FINAL).resolve(1),
                is(Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | 1));
    }

    @Test
    public void testForParameter() throws Exception {
        assertThat(ModifierContributor.Resolver.of(ProvisioningState.MANDATED, ParameterManifestation.FINAL).resolve(),
                is(Opcodes.ACC_MANDATED | Opcodes.ACC_FINAL));
        assertThat(ModifierContributor.Resolver.of(ProvisioningState.MANDATED, ParameterManifestation.PLAIN, ParameterManifestation.FINAL).resolve(),
                is(Opcodes.ACC_MANDATED | Opcodes.ACC_FINAL));
        assertThat(ModifierContributor.Resolver.of(ProvisioningState.MANDATED, ParameterManifestation.FINAL).resolve(1),
                is(Opcodes.ACC_MANDATED | Opcodes.ACC_FINAL | 1));
    }
}
