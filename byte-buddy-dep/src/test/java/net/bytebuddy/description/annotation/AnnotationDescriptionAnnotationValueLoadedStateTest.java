package net.bytebuddy.description.annotation;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AnnotationDescriptionAnnotationValueLoadedStateTest {

    @Test
    public void testIsDefined() throws Exception {
        assertThat(AnnotationDescription.AnnotationValue.Loaded.State.RESOLVED.isResolved(), is(true));
        assertThat(AnnotationDescription.AnnotationValue.Loaded.State.NON_RESOLVED.isResolved(), is(false));
        assertThat(AnnotationDescription.AnnotationValue.Loaded.State.NON_DEFINED.isResolved(), is(false));
    }

    @Test
    public void testIsResolved() throws Exception {
        assertThat(AnnotationDescription.AnnotationValue.Loaded.State.RESOLVED.isDefined(), is(true));
        assertThat(AnnotationDescription.AnnotationValue.Loaded.State.NON_RESOLVED.isDefined(), is(true));
        assertThat(AnnotationDescription.AnnotationValue.Loaded.State.NON_DEFINED.isDefined(), is(false));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AnnotationDescription.AnnotationValue.Loaded.State.class).apply();
    }
}
