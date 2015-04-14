package net.bytebuddy.description.annotation;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.test.utility.MockitoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.doReturn;

public class AnnotationListForLoadedAnnotationTest {

    @Rule
    public TestRule mockitoRule = new MockitoRule(this);

    @Mock
    private Foo foo;

    @Mock
    private Bar bar;

    @Before
    public void setUp() throws Exception {
        doReturn(Foo.class).when(foo).annotationType();
        doReturn(Bar.class).when(bar).annotationType();
    }

    @Test
    public void testInheritedNonIgnored() throws Exception {
        AnnotationList annotationList = new AnnotationList.ForLoadedAnnotation(foo, bar)
                .inherited(Collections.<TypeDescription>emptySet());
        assertThat(annotationList.size(), is(1));
        assertThat(annotationList, hasItem((AnnotationDescription) AnnotationDescription.ForLoadedAnnotation.of(bar)));
    }

    @Test
    public void testInheritedIgnored() throws Exception {
        AnnotationList annotationList = new AnnotationList.ForLoadedAnnotation(foo, bar)
                .inherited(Collections.singleton(new TypeDescription.ForLoadedType(Bar.class)));
        assertThat(annotationList.size(), is(0));
    }

    @Test
    public void testContainment() throws Exception {
        AnnotationList annotationList = new AnnotationList.ForLoadedAnnotation(foo);
        assertThat(annotationList.isAnnotationPresent(Foo.class), is(true));
        assertThat(annotationList.isAnnotationPresent(Bar.class), is(false));
    }

    @Test
    public void testPreparation() throws Exception {
        AnnotationList annotationList = new AnnotationList.ForLoadedAnnotation(foo);
        assertThat(annotationList.ofType(Foo.class), is(AnnotationDescription.ForLoadedAnnotation.of(foo)));
        assertThat(annotationList.ofType(Bar.class), nullValue());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMulti() throws Exception {
        Iterator<Annotation> annotations = Arrays.asList(foo, bar).iterator();
        for (AnnotationList annotationList : AnnotationList.ForLoadedAnnotation.asList(new Annotation[][]{{foo}, {bar}})) {
            assertThat(annotationList.size(), is(1));
            assertThat(annotationList, hasItem(AnnotationDescription.ForLoadedAnnotation.of(annotations.next())));
        }
    }

    @Test
    public void testSubList() throws Exception {
        assertThat(new AnnotationList.ForLoadedAnnotation(foo, bar).subList(0, 1),
                is((AnnotationList) new AnnotationList.ForLoadedAnnotation(foo)));
    }

    private @interface Foo {

    }

    @Inherited
    private @interface Bar {

    }
}
