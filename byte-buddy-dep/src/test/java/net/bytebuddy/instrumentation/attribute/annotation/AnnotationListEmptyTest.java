package net.bytebuddy.instrumentation.attribute.annotation;

import net.bytebuddy.instrumentation.type.TypeDescription;
import org.junit.Before;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class AnnotationListEmptyTest {

    private AnnotationList annotationList;

    @Before
    public void setUp() throws Exception {
        annotationList = new AnnotationList.Empty();
    }

    @Test
    public void testIsEmpty() throws Exception {
        assertThat(annotationList.size(), is(0));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testThrowsException() throws Exception {
        annotationList.get(0);
    }

    @Test
    public void testInheritance() throws Exception {
        assertThat(annotationList.inherited(Collections.<TypeDescription>emptySet()), is(annotationList));
    }

    @Test
    public void testNothingPresent() throws Exception {
        assertThat(annotationList.isAnnotationPresent(Annotation.class), is(false));
    }

    @Test
    public void testNoAnnotations() throws Exception {
        assertThat(annotationList.ofType(Annotation.class), nullValue());
    }

    @Test
    public void testMulti() throws Exception {
        for (AnnotationList annotationList : AnnotationList.Empty.asList(1)) {
            assertThat(annotationList, is(this.annotationList));
        }
    }

    @Test
    public void testSubListEmpty() throws Exception {
        assertThat(annotationList.subList(0, 0), is(annotationList));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testSubListOutOfBounds() throws Exception {
        annotationList.subList(0, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubListIllegal() throws Exception {
        annotationList.subList(1, 0);
    }
}
