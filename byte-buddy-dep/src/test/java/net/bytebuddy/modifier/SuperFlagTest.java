package net.bytebuddy.modifier;

import net.bytebuddy.instrumentation.ModifierContributor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class SuperFlagTest extends AbstractModifierContributorTest {

    public SuperFlagTest(ModifierContributor modifierContributor, int expectedModifier) {
        super(modifierContributor, expectedModifier);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {SuperFlag.UNDEFINED, 0},
                {SuperFlag.is(false), 0},
                {SuperFlag.DEFINED, Opcodes.ACC_SUPER},
                {SuperFlag.is(true), Opcodes.ACC_SUPER}
        });
    }

    @Test
    public void testState() throws Exception {
        assertThat(((SuperFlag) modifierContributor).isSuperFlag(), is(expectedModifier != 0));
    }
}
