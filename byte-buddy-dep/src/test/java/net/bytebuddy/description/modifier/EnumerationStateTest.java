package net.bytebuddy.description.modifier;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.asm.Opcodes;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class EnumerationStateTest extends AbstractModifierContributorTest {

    public EnumerationStateTest(ModifierContributor modifierContributor, int expectedModifier) {
        super(modifierContributor, expectedModifier);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {EnumerationState.ENUMERATION, Opcodes.ACC_ENUM},
                {EnumerationState.is(true), Opcodes.ACC_ENUM},
                {EnumerationState.NON_ENUMERATION, 0},
                {EnumerationState.is(false), 0}
        });
    }

    @Test
    public void testProperties() throws Exception {
        assertThat(((EnumerationState) modifierContributor).isEnumeration(), is((expectedModifier & Opcodes.ACC_ENUM) != 0));
    }
}
