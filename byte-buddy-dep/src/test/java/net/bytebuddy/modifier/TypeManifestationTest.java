package net.bytebuddy.modifier;

import net.bytebuddy.instrumentation.ModifierContributor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.asm.Opcodes;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class TypeManifestationTest extends AbstractModifierContributorTest {

    public TypeManifestationTest(ModifierContributor modifierContributor, int expectedModifier) {
        super(modifierContributor, expectedModifier);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {TypeManifestation.PLAIN, 0},
                {TypeManifestation.ABSTRACT, Opcodes.ACC_ABSTRACT},
                {TypeManifestation.ABSTRACT_ENUM, Opcodes.ACC_ABSTRACT | Opcodes.ACC_ENUM},
                {TypeManifestation.ENUM, Opcodes.ACC_ENUM | Opcodes.ACC_FINAL},
                {TypeManifestation.FINAL, Opcodes.ACC_FINAL},
                {TypeManifestation.INTERFACE, Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT},
        });
    }

    @Test
    public void testProperties() throws Exception {
        assertThat(((TypeManifestation) modifierContributor).isAbstract(),
                is((expectedModifier & Opcodes.ACC_ABSTRACT) != 0 && (expectedModifier & Opcodes.ACC_INTERFACE) == 0));
        assertThat(((TypeManifestation) modifierContributor).isEnum(), is((expectedModifier & Opcodes.ACC_ENUM) != 0));
        assertThat(((TypeManifestation) modifierContributor).isFinal(), is((expectedModifier & Opcodes.ACC_FINAL) != 0));
        assertThat(((TypeManifestation) modifierContributor).isInterface(), is((expectedModifier & Opcodes.ACC_INTERFACE) != 0));
    }
}
