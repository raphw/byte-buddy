package net.bytebuddy.modifier;

import net.bytebuddy.instrumentation.ModifierContributor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.objectweb.asm.Opcodes;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class VisibilityTest extends AbstractModifierContributorTest {

    private final boolean isPublic, isProtected, isPackagePrivate, isPrivate;

    public VisibilityTest(ModifierContributor modifierContributor,
                          int expectedModifier,
                          boolean isPublic,
                          boolean isProtected,
                          boolean isPackagePrivate,
                          boolean isPrivate) {
        super(modifierContributor, expectedModifier);
        this.isPublic = isPublic;
        this.isProtected = isProtected;
        this.isPackagePrivate = isPackagePrivate;
        this.isPrivate = isPrivate;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {Visibility.PUBLIC, Opcodes.ACC_PUBLIC, true, false, false, false},
                {Visibility.PRIVATE, Opcodes.ACC_PRIVATE, false, false, false, true},
                {Visibility.PROTECTED, Opcodes.ACC_PROTECTED, false, true, false, false},
                {Visibility.PACKAGE_PRIVATE, 0, false, false, true, false}
        });
    }

    @Test
    public void testProperties() throws Exception {
        assertThat(((Visibility) modifierContributor).isPublic(), is(isPublic));
        assertThat(((Visibility) modifierContributor).isPackagePrivate(), is(isPackagePrivate));
        assertThat(((Visibility) modifierContributor).isProtected(), is(isProtected));
        assertThat(((Visibility) modifierContributor).isPrivate(), is(isPrivate));
    }
}
