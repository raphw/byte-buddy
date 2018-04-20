package net.bytebuddy.description.annotation;

import net.bytebuddy.description.type.TypeDescription;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.util.Collections;

import static net.bytebuddy.matcher.ElementMatchers.none;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class AnnotationListEmptyTest {

    @Test
    public void testAnnotationIsPresent() throws Exception {
        assertThat(new AnnotationList.Empty().isAnnotationPresent(Annotation.class), is(false));
    }

    @Test
    public void testAnnotationIsPresentDescription() throws Exception {
        assertThat(new AnnotationList.Empty().isAnnotationPresent(TypeDescription.ForLoadedType.of(Annotation.class)), is(false));
    }

    @Test
    public void testAnnotationOfType() throws Exception {
        assertThat(new AnnotationList.Empty().ofType(Annotation.class), nullValue(AnnotationDescription.Loadable.class));
    }

    @Test
    public void testAnnotationInherited() throws Exception {
        assertThat(new AnnotationList.Empty().inherited(Collections.<TypeDescription>emptySet()).size(), is(0));
    }

    @Test
    public void testAnnotationVisibility() throws Exception {
        assertThat(new AnnotationList.Empty().visibility(none()).size(), is(0));
    }

    @Test
    public void testAsTypeList() throws Exception {
        assertThat(new AnnotationList.Empty().asTypeList().size(), is(0));
    }
}
