package net.bytebuddy.description.annotation;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AnnotationDescriptionAnnotationValueStateTest {

    @Test
    public void testIsDefined() throws Exception {
        assertThat(AnnotationValue.State.RESOLVED.isResolved(), is(true));
        assertThat(AnnotationValue.State.UNRESOLVED.isResolved(), is(false));
        assertThat(AnnotationValue.State.UNDEFINED.isResolved(), is(false));
    }

    @Test
    public void testIsResolved() throws Exception {
        assertThat(AnnotationValue.State.RESOLVED.isDefined(), is(true));
        assertThat(AnnotationValue.State.UNRESOLVED.isDefined(), is(true));
        assertThat(AnnotationValue.State.UNDEFINED.isDefined(), is(false));
    }
}
