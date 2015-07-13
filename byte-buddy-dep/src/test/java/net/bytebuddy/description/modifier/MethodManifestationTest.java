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
public class MethodManifestationTest extends AbstractModifierContributorTest {

    public MethodManifestationTest(ModifierContributor modifierContributor, int expectedModifier, boolean defaultModifier) {
        super(modifierContributor, expectedModifier, defaultModifier);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {MethodManifestation.PLAIN, 0, true},
                {MethodManifestation.NATIVE, Opcodes.ACC_NATIVE, false},
                {MethodManifestation.ABSTRACT, Opcodes.ACC_ABSTRACT, false},
                {MethodManifestation.FINAL, Opcodes.ACC_FINAL, false},
                {MethodManifestation.FINAL_NATIVE, Opcodes.ACC_FINAL | Opcodes.ACC_NATIVE, false},
                {MethodManifestation.BRIDGE, Opcodes.ACC_BRIDGE, false}
        });
    }

    @Test
    public void testProperties() throws Exception {
        assertThat(((MethodManifestation) modifierContributor).isAbstract(), is((expectedModifier & Opcodes.ACC_ABSTRACT) != 0));
        assertThat(((MethodManifestation) modifierContributor).isBridge(), is((expectedModifier & Opcodes.ACC_BRIDGE) != 0));
        assertThat(((MethodManifestation) modifierContributor).isFinal(), is((expectedModifier & Opcodes.ACC_FINAL) != 0));
        assertThat(((MethodManifestation) modifierContributor).isNative(), is((expectedModifier & Opcodes.ACC_NATIVE) != 0));
    }
}
