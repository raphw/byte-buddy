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
public class FieldPersistenceTest extends AbstractModifierContributorTest {

    private final boolean isTransient;

    public FieldPersistenceTest(ModifierContributor modifierContributor,
                                int expectedModifier,
                                boolean defaultModifier,
                                boolean isTransient) {
        super(modifierContributor, expectedModifier, defaultModifier);
        this.isTransient = isTransient;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {FieldPersistence.PLAIN, 0, true, false},
                {FieldPersistence.TRANSIENT, Opcodes.ACC_TRANSIENT, false, true}
        });
    }

    @Test
    public void testProperties() throws Exception {
        assertThat(((FieldPersistence) modifierContributor).isTransient(), is(isTransient));
    }
}
