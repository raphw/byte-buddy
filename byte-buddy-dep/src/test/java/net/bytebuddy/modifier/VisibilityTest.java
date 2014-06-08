package net.bytebuddy.modifier;

import net.bytebuddy.instrumentation.ModifierContributor;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class VisibilityTest extends AbstractModifierContributorTest {

    public VisibilityTest(ModifierContributor modifierContributor, int expectedModifier) {
        super(modifierContributor, expectedModifier);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {Visibility.PUBLIC, Opcodes.ACC_PUBLIC},
                {Visibility.PRIVATE, Opcodes.ACC_PRIVATE},
                {Visibility.PROTECTED, Opcodes.ACC_PROTECTED},
                {Visibility.PACKAGE_PRIVATE, 0}
        });
    }
}
