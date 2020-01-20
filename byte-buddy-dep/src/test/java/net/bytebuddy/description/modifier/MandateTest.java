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
public class MandateTest extends AbstractModifierContributorTest {

    private final boolean isMandated;

    public MandateTest(ModifierContributor modifierContributor,
                       int expectedModifier,
                       boolean defaultModifier,
                       boolean isMandated) {
        super(modifierContributor, expectedModifier, defaultModifier);
        this.isMandated = isMandated;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {Mandate.PLAIN, 0, true, false},
                {Mandate.MANDATED, Opcodes.ACC_MANDATED, false, true}
        });
    }

    @Test
    public void testProperties() throws Exception {
        assertThat(((Mandate) modifierContributor).isMandated(), is(isMandated));
    }
}
