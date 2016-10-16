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
public class MethodStrictnessTest extends AbstractModifierContributorTest {

    public MethodStrictnessTest(ModifierContributor modifierContributor, int expectedModifier, boolean defaultModifier) {
        super(modifierContributor, expectedModifier, defaultModifier);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {MethodStrictness.PLAIN, 0, true},
                {MethodStrictness.STRICT, Opcodes.ACC_STRICT, false}
        });
    }

    @Test
    public void testState() throws Exception {
        assertThat(((MethodStrictness) modifierContributor).isStrict(), is(expectedModifier != 0));
    }
}
