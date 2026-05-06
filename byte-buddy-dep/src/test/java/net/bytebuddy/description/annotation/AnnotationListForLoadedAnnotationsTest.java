package net.bytebuddy.description.annotation;

import org.junit.Test;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AnnotationListForLoadedAnnotationsTest extends AbstractAnnotationListTest<Annotation> {

    protected Annotation getFirst() throws Exception {
        return Holder.class.getAnnotation(Foo.class);
    }

    protected Annotation getSecond() throws Exception {
        return Holder.class.getAnnotation(Bar.class);
    }

    protected AnnotationList asList(List<Annotation> elements) {
        return new AnnotationList.ForLoadedAnnotations(elements);
    }

    protected AnnotationDescription asElement(Annotation element) {
        return new AnnotationDescription.ForLoadedAnnotation<Annotation>(element);
    }

    @Test
    public void testFiltersNullEntriesFromArray() throws Exception {
        AnnotationList list = new AnnotationList.ForLoadedAnnotations(getFirst(), null, getSecond());
        assertThat(list.size(), is(2));
        assertThat(list.isAnnotationPresent(Foo.class), is(true));
        assertThat(list.isAnnotationPresent(Bar.class), is(true));
    }

    @Test
    public void testFiltersNullEntriesFromList() throws Exception {
        AnnotationList list = new AnnotationList.ForLoadedAnnotations(Arrays.asList(getFirst(), null, getSecond()));
        assertThat(list.size(), is(2));
        assertThat(list.isAnnotationPresent(Foo.class), is(true));
        assertThat(list.isAnnotationPresent(Bar.class), is(true));
    }

    @Test
    public void testAllNullEntriesYieldEmptyList() throws Exception {
        AnnotationList list = new AnnotationList.ForLoadedAnnotations(Arrays.<Annotation>asList(null, null));
        assertThat(list.size(), is(0));
    }

    @Test
    public void testNullArrayEntriesAreFiltered() throws Exception {
        AnnotationList list = new AnnotationList.ForLoadedAnnotations(new Annotation[]{null});
        assertThat(list.size(), is(0));
    }

    @Test
    public void testEmptyListIsPreserved() throws Exception {
        AnnotationList list = new AnnotationList.ForLoadedAnnotations(Collections.<Annotation>emptyList());
        assertThat(list.size(), is(0));
    }
}
