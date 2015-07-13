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
public class SyntheticStateTest extends AbstractModifierContributorTest {

    public SyntheticStateTest(ModifierContributor modifierContributor, int expectedModifier, boolean defaultModifier) {
        super(modifierContributor, expectedModifier, defaultModifier);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {SyntheticState.is(false), 0, true},
                {SyntheticState.PLAIN, 0, true},
                {SyntheticState.is(true), Opcodes.ACC_SYNTHETIC, false},
                {SyntheticState.SYNTHETIC, Opcodes.ACC_SYNTHETIC, false}
        });
    }

    @Test
    public void testState() throws Exception {
        assertThat(((SyntheticState) modifierContributor).isSynthetic(), is(expectedModifier != 0));
    }
}
