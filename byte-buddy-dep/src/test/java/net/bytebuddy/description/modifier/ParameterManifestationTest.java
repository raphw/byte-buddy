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
public class ParameterManifestationTest extends AbstractModifierContributorTest {

    public ParameterManifestationTest(ModifierContributor modifierContributor, int expectedModifier, boolean defaultModifier) {
        super(modifierContributor, expectedModifier, defaultModifier);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {ParameterManifestation.is(false), 0, true},
                {ParameterManifestation.PLAIN, 0, true},
                {ParameterManifestation.is(true), Opcodes.ACC_FINAL, false},
                {ParameterManifestation.FINAL, Opcodes.ACC_FINAL, false}
        });
    }

    @Test
    public void testState() throws Exception {
        assertThat(((ParameterManifestation) modifierContributor).isFinal(), is(expectedModifier != 0));
    }
}