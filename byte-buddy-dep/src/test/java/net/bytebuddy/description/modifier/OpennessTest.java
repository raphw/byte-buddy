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
public class OpennessTest extends AbstractModifierContributorTest {

    private final boolean isOpen;

    public OpennessTest(ModifierContributor modifierContributor,
                        int expectedModifier,
                        boolean defaultModifier,
                        boolean isOpen) {
        super(modifierContributor, expectedModifier, defaultModifier);
        this.isOpen = isOpen;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {Openness.CLOSED, 0, true, false},
                {Openness.OPEN, Opcodes.ACC_OPEN, false, true}
        });
    }

    @Test
    public void testProperties() throws Exception {
        assertThat(((Openness) modifierContributor).isOpen(), is(isOpen));
    }
}
