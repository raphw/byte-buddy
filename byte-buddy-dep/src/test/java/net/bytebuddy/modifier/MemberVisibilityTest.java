package net.bytebuddy.modifier;

import net.bytebuddy.instrumentation.ModifierContributor;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class MemberVisibilityTest extends AbstractModifierContributorTest {

    public MemberVisibilityTest(ModifierContributor modifierContributor, int expectedModifier) {
        super(modifierContributor, expectedModifier);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {FieldManifestation.PLAIN, 0},
                {FieldManifestation.FINAL, org.objectweb.asm.Opcodes.ACC_FINAL},
                {FieldManifestation.VOLATILE, org.objectweb.asm.Opcodes.ACC_VOLATILE},
        });
    }
}
