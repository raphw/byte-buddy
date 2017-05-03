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
public class FieldManifestationTest extends AbstractModifierContributorTest {

    private final boolean isFinal, isVolatile, isPlain;

    public FieldManifestationTest(ModifierContributor modifierContributor,
                                  int expectedModifier,
                                  boolean defaultModifier,
                                  boolean isFinal,
                                  boolean isVolatile,
                                  boolean isPlain) {
        super(modifierContributor, expectedModifier, defaultModifier);
        this.isFinal = isFinal;
        this.isVolatile = isVolatile;
        this.isPlain = isPlain;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {FieldManifestation.PLAIN, 0, true, false, false, true},
                {FieldManifestation.FINAL, Opcodes.ACC_FINAL, false, true, false, false},
                {FieldManifestation.VOLATILE, Opcodes.ACC_VOLATILE, false, false, true, false}
        });
    }

    @Test
    public void testProperties() throws Exception {
        assertThat(((FieldManifestation) modifierContributor).isFinal(), is(isFinal));
        assertThat(((FieldManifestation) modifierContributor).isVolatile(), is(isVolatile));
        assertThat(((FieldManifestation) modifierContributor).isPlain(), is(isPlain));
    }
}
