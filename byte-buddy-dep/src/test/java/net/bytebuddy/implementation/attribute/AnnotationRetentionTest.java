package net.bytebuddy.implementation.attribute;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;

@RunWith(Parameterized.class)
public class AnnotationRetentionTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {AnnotationRetention.ENABLED, true},
                {AnnotationRetention.DISABLED, false}
        });
    }

    private final AnnotationRetention annotationRetention;

    private final boolean enabled;

    public AnnotationRetentionTest(AnnotationRetention annotationRetention, boolean enabled) {
        this.annotationRetention = annotationRetention;
        this.enabled = enabled;
    }

    @Test
    public void testEnabled() throws Exception {
        assertThat(annotationRetention.isEnabled(), is(enabled));
    }

    @Test
    public void testRetention() throws Exception {
        assertThat(AnnotationRetention.of(enabled), is(annotationRetention));
    }
}
