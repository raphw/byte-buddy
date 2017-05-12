package net.bytebuddy.description.modifier;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class TypeManifestationTest extends AbstractModifierContributorTest {

    public TypeManifestationTest(ModifierContributor modifierContributor, int expectedModifier, boolean defaultModifier) {
        super(modifierContributor, expectedModifier, defaultModifier);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {TypeManifestation.PLAIN, 0, true},
                {TypeManifestation.ABSTRACT, Opcodes.ACC_ABSTRACT, false},
                {TypeManifestation.FINAL, Opcodes.ACC_FINAL, false},
                {TypeManifestation.INTERFACE, Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT, false},
                {TypeManifestation.ANNOTATION, Opcodes.ACC_ANNOTATION | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT, false}
        });
    }

    @Test
    public void testProperties() throws Exception {
        assertThat(((TypeManifestation) modifierContributor).isAbstract(),
                is((expectedModifier & Opcodes.ACC_ABSTRACT) != 0 && (expectedModifier & Opcodes.ACC_INTERFACE) == 0));
        assertThat(((TypeManifestation) modifierContributor).isFinal(), is((expectedModifier & Opcodes.ACC_FINAL) != 0));
        assertThat(((TypeManifestation) modifierContributor).isInterface(), is((expectedModifier & Opcodes.ACC_INTERFACE) != 0));
        assertThat(((TypeManifestation) modifierContributor).isAnnotation(), is((expectedModifier & Opcodes.ACC_ANNOTATION) != 0));
    }
}
