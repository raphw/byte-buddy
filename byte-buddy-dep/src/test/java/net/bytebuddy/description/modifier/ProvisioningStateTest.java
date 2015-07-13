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
public class ProvisioningStateTest extends AbstractModifierContributorTest {

    public ProvisioningStateTest(ModifierContributor modifierContributor, int expectedModifier, boolean defaultModifier) {
        super(modifierContributor, expectedModifier, defaultModifier);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {ProvisioningState.is(false), 0, true},
                {ProvisioningState.PLAIN, 0, true},
                {ProvisioningState.is(true), Opcodes.ACC_MANDATED, false},
                {ProvisioningState.MANDATED, Opcodes.ACC_MANDATED, false}
        });
    }

    @Test
    public void testState() throws Exception {
        assertThat(((ProvisioningState) modifierContributor).isMandated(), is(expectedModifier != 0));
    }
}
