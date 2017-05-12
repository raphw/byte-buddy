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
public class EnumerationStateTest extends AbstractModifierContributorTest {

    public EnumerationStateTest(ModifierContributor modifierContributor, int expectedModifier, boolean defaultModifier) {
        super(modifierContributor, expectedModifier, defaultModifier);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {EnumerationState.PLAIN, 0, true},
                {EnumerationState.ENUMERATION, Opcodes.ACC_ENUM, false}
        });
    }

    @Test
    public void testProperties() throws Exception {
        assertThat(((EnumerationState) modifierContributor).isEnumeration(), is((expectedModifier & Opcodes.ACC_ENUM) != 0));
    }
}
