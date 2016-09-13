package net.bytebuddy.description.annotation;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AnnotationDescriptionAnnotationValueLoadedStateTest {

    @Test
    public void testIsDefined() throws Exception {
        assertThat(AnnotationValue.Loaded.State.RESOLVED.isResolved(), is(true));
        assertThat(AnnotationValue.Loaded.State.UNRESOLVED.isResolved(), is(false));
        assertThat(AnnotationValue.Loaded.State.UNDEFINED.isResolved(), is(false));
    }

    @Test
    public void testIsResolved() throws Exception {
        assertThat(AnnotationValue.Loaded.State.RESOLVED.isDefined(), is(true));
        assertThat(AnnotationValue.Loaded.State.UNRESOLVED.isDefined(), is(true));
        assertThat(AnnotationValue.Loaded.State.UNDEFINED.isDefined(), is(false));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AnnotationValue.Loaded.State.class).apply();
    }
}
