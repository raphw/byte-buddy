package net.bytebuddy.modifier;

import net.bytebuddy.instrumentation.ModifierContributor;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class OwnershipTest extends AbstractModifierContributorTest {

    public OwnershipTest(ModifierContributor modifierContributor, int expectedModifier) {
        super(modifierContributor, expectedModifier);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {Ownership.MEMBER, 0},
                {Ownership.STATIC, Opcodes.ACC_STATIC},
        });
    }
}
