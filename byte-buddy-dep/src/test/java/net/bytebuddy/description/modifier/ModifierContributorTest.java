package net.bytebuddy.description.modifier;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class ModifierContributorTest {

    private final Class<? extends ModifierContributor> type;

    public ModifierContributorTest(Class<? extends ModifierContributor> type) {
        this.type = type;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {EnumerationState.class},
                {FieldManifestation.class},
                {MethodArguments.class},
                {MethodManifestation.class},
                {MethodStrictness.class},
                {Ownership.class},
                {ParameterManifestation.class},
                {ProvisioningState.class},
                {SynchronizationState.class},
                {SyntheticState.class},
                {TypeManifestation.class},
                {Visibility.class}
        });
    }

    @Test
    public void testRange() throws Exception {
        ModifierContributor[] modifierContributor = (ModifierContributor[]) type.getDeclaredMethod("values").invoke(null);
        int mask = 0;
        for (ModifierContributor contributor : modifierContributor) {
            mask |= contributor.getMask();
        }
        for (ModifierContributor contributor : modifierContributor) {
            assertThat(mask, is(contributor.getRange()));
        }
    }
}
