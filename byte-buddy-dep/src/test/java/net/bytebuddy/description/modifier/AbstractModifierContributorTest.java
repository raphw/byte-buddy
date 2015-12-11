package net.bytebuddy.description.modifier;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public abstract class AbstractModifierContributorTest {

    protected final ModifierContributor modifierContributor;

    protected final int expectedModifier;

    protected final boolean defaultModifier;

    public AbstractModifierContributorTest(ModifierContributor modifierContributor, int expectedModifier, boolean defaultModifier) {
        this.modifierContributor = modifierContributor;
        this.expectedModifier = expectedModifier;
        this.defaultModifier = defaultModifier;
    }

    @Test
    public void testModifierContributor() throws Exception {
        assertThat(modifierContributor.getMask(), is(expectedModifier));
    }

    @Test
    public void testDefaultModifier() throws Exception {
        assertThat(modifierContributor.isDefault(), is(defaultModifier));
    }
}
