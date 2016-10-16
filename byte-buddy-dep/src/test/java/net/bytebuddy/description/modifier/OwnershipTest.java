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
public class OwnershipTest extends AbstractModifierContributorTest {

    public OwnershipTest(ModifierContributor modifierContributor, int expectedModifier, boolean defaultModifier) {
        super(modifierContributor, expectedModifier, defaultModifier);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {Ownership.MEMBER, 0, true},
                {Ownership.STATIC, Opcodes.ACC_STATIC, false}
        });
    }

    @Test
    public void testState() throws Exception {
        assertThat(((Ownership) modifierContributor).isStatic(), is(expectedModifier != 0));
    }
}
