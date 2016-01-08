package net.bytebuddy.dynamic.scaffold;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class TypeValidationTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {TypeValidation.ENABLED, true},
                {TypeValidation.DISABLED, false}
        });
    }

    private final TypeValidation typeValidation;

    private final boolean enabled;

    public TypeValidationTest(TypeValidation typeValidation, boolean enabled) {
        this.typeValidation = typeValidation;
        this.enabled = enabled;
    }

    @Test
    public void testIsEnabled() throws Exception {
        assertThat(typeValidation.isEnabled(), is(enabled));
    }

    @Test
    public void testReceival() throws Exception {
        assertThat(TypeValidation.of(enabled), is(typeValidation));
    }
}
