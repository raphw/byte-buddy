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
public class MethodArgumentsTest extends AbstractModifierContributorTest {

    public MethodArgumentsTest(ModifierContributor modifierContributor, int expectedModifier, boolean defaultModifier) {
        super(modifierContributor, expectedModifier, defaultModifier);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {MethodArguments.PLAIN, 0, true},
                {MethodArguments.isVarargs(false), 0, true},
                {MethodArguments.VARARGS, Opcodes.ACC_VARARGS, false},
                {MethodArguments.isVarargs(true), Opcodes.ACC_VARARGS, false},
        });
    }

    @Test
    public void testState() throws Exception {
        assertThat(((MethodArguments) modifierContributor).isVarargs(), is(expectedModifier != 0));
    }
}
