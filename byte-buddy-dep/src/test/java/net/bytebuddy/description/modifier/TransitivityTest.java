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
public class TransitivityTest extends AbstractModifierContributorTest {

    private final boolean isTransitive;

    public TransitivityTest(ModifierContributor modifierContributor,
                            int expectedModifier,
                            boolean defaultModifier,
                            boolean isTransitive) {
        super(modifierContributor, expectedModifier, defaultModifier);
        this.isTransitive = isTransitive;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {Transitivity.NONE, 0, true, false},
                {Transitivity.TRANSITIVE, Opcodes.ACC_OPEN, false, true}
        });
    }

    @Test
    public void testProperties() throws Exception {
        assertThat(((Transitivity) modifierContributor).isTransitive(), is(isTransitive));
    }
}
