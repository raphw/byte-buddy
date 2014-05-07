package net.bytebuddy.modifier;

import net.bytebuddy.instrumentation.ModifierContributor;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public abstract class AbstractModifierContributorTest {

    protected final ModifierContributor modifierContributor;
    protected final int expectedModifier;

    public AbstractModifierContributorTest(ModifierContributor modifierContributor, int expectedModifier) {
        this.modifierContributor = modifierContributor;
        this.expectedModifier = expectedModifier;
    }

    @Test
    public void testModifierContributor() throws Exception {
        assertThat(modifierContributor.getMask(), is(expectedModifier));
    }
}
