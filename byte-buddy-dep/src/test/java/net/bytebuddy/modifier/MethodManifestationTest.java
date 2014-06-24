package net.bytebuddy.modifier;

import net.bytebuddy.instrumentation.ModifierContributor;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class MethodManifestationTest extends AbstractModifierContributorTest {

    public MethodManifestationTest(ModifierContributor modifierContributor, int expectedModifier) {
        super(modifierContributor, expectedModifier);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {MethodManifestation.PLAIN, 0},
                {MethodManifestation.NATIVE, Opcodes.ACC_NATIVE},
                {MethodManifestation.ABSTRACT, Opcodes.ACC_ABSTRACT},
                {MethodManifestation.FINAL, Opcodes.ACC_FINAL},
                {MethodManifestation.FINAL_NATIVE, Opcodes.ACC_FINAL | Opcodes.ACC_NATIVE},
                {MethodManifestation.BRIDGE, Opcodes.ACC_BRIDGE}
        });
    }
}
