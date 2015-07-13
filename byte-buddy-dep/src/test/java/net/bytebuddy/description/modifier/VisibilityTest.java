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
public class VisibilityTest extends AbstractModifierContributorTest {

    private final boolean isPublic, isProtected, isPackagePrivate, isPrivate;

    public VisibilityTest(ModifierContributor modifierContributor,
                          int expectedModifier,
                          boolean defaultModifier,
                          boolean isPublic,
                          boolean isProtected,
                          boolean isPackagePrivate,
                          boolean isPrivate) {
        super(modifierContributor, expectedModifier, defaultModifier);
        this.isPublic = isPublic;
        this.isProtected = isProtected;
        this.isPackagePrivate = isPackagePrivate;
        this.isPrivate = isPrivate;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {Visibility.PUBLIC, Opcodes.ACC_PUBLIC, false, true, false, false, false},
                {Visibility.PROTECTED, Opcodes.ACC_PROTECTED, false, false, true, false, false},
                {Visibility.PACKAGE_PRIVATE, 0, true, false, false, true, false},
                {Visibility.PRIVATE, Opcodes.ACC_PRIVATE, false, false, false, false, true}
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
