package net.bytebuddy.modifier;

import net.bytebuddy.instrumentation.ModifierContributor;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class FieldManifestationTest extends AbstractModifierContributorTest {

    public FieldManifestationTest(ModifierContributor modifierContributor, int expectedModifier) {
        super(modifierContributor, expectedModifier);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {FieldManifestation.PLAIN, 0},
                {FieldManifestation.FINAL, Opcodes.ACC_FINAL},
                {FieldManifestation.VOLATILE, Opcodes.ACC_VOLATILE},
        });
    }
}
