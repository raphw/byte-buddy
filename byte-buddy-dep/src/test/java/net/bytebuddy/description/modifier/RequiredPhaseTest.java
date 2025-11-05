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
public class RequiredPhaseTest extends AbstractModifierContributorTest {

    private final boolean isStaticPhase;

    public RequiredPhaseTest(ModifierContributor modifierContributor,
                             int expectedModifier,
                             boolean defaultModifier,
                             boolean isStaticPhase) {
        super(modifierContributor, expectedModifier, defaultModifier);
        this.isStaticPhase = isStaticPhase;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {RequiredPhase.ALWAYS, 0, true, false},
                {RequiredPhase.STATIC, Opcodes.ACC_STATIC_PHASE, false, true}
        });
    }

    @Test
    public void testProperties() throws Exception {
        assertThat(((RequiredPhase) modifierContributor).isStatic(), is(isStaticPhase));
    }
}
