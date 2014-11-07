package net.bytebuddy.instrumentation.attribute.annotation;

import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.utility.MockitoRule;
import net.bytebuddy.utility.ObjectPropertyAssertion;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.mockito.Mock;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
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
        AnnotationList annotationList = new AnnotationList.ForLoadedAnnotation(new Annotation[]{foo, bar})
                .inherited(Collections.<TypeDescription>emptySet());
        assertThat(annotationList.size(), is(1));
        assertThat(annotationList, hasItem((AnnotationDescription) AnnotationDescription.ForLoadedAnnotation.of(bar)));
    }

    @Test
    public void testInheritedIgnored() throws Exception {
        AnnotationList annotationList = new AnnotationList.ForLoadedAnnotation(new Annotation[]{foo, bar})
                .inherited(Collections.singleton(new TypeDescription.ForLoadedType(Bar.class)));
        assertThat(annotationList.size(), is(0));
    }

    @Test
    public void testContainment() throws Exception {
        AnnotationList annotationList = new AnnotationList.ForLoadedAnnotation(new Annotation[]{foo});
        assertThat(annotationList.isAnnotationPresent(Foo.class), is(true));
        assertThat(annotationList.isAnnotationPresent(Bar.class), is(false));
    }

    @Test
    public void testPreparation() throws Exception {
        AnnotationList annotationList = new AnnotationList.ForLoadedAnnotation(new Annotation[]{foo});
        assertThat(annotationList.ofType(Foo.class), is(AnnotationDescription.ForLoadedAnnotation.of(foo)));
        assertThat(annotationList.ofType(Bar.class), nullValue());
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(AnnotationList.ForLoadedAnnotation.class)
                .apply(new AnnotationList.ForLoadedAnnotation(new Annotation[0]));

    }

    private static @interface Foo {
    }

    @Inherited
    private static @interface Bar {

    }
}
