package net.bytebuddy.pool;

import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.type.TypeDescription;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class TypePoolLazyAnnotationListTest {

    private AnnotationList annotationList;

    private TypePool typePool;

    @Before
    public void setUp() throws Exception {
        typePool = TypePool.Default.ofClassPath();
        annotationList = typePool.describe(Carrier.class.getName()).resolve().getDeclaredAnnotations();
    }

    @After
    public void tearDown() throws Exception {
        typePool.clear();
    }

    @Test
    public void testInheritedNonIgnored() throws Exception {
        AnnotationList annotationList = this.annotationList.inherited(Collections.<TypeDescription>emptySet());
        assertThat(annotationList.size(), is(1));
        assertThat(annotationList, hasItem(this.annotationList.get(1)));
    }

    @Test
    public void testInheritedIgnored() throws Exception {
        AnnotationList annotationList = this.annotationList
                .inherited(Collections.singleton(new TypeDescription.ForLoadedType(Bar.class)));
        assertThat(annotationList.size(), is(0));
    }

    @Test
    public void testContainment() throws Exception {
        AnnotationList annotationList = this.annotationList.subList(0, 1);
        assertThat(annotationList.isAnnotationPresent(Foo.class), is(true));
        assertThat(annotationList.isAnnotationPresent(Bar.class), is(false));
    }

    @Test
    public void testPreparation() throws Exception {
        AnnotationList annotationList = this.annotationList.subList(0, 1);
        assertThat(annotationList.ofType(Foo.class), is(this.annotationList.get(0).prepare(Foo.class)));
        assertThat(annotationList.ofType(Bar.class), nullValue());
    }

    @Test
    public void testSubList() throws Exception {
        assertThat(annotationList.subList(0, 1), is((AnnotationList) new AnnotationList.Explicit(Collections.singletonList(annotationList.get(0)))));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testSubListOutOfBounds() throws Exception {
        annotationList.subList(0, 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubListIllegal() throws Exception {
        annotationList.subList(1, 0);
    }

    @Retention(RetentionPolicy.RUNTIME)
    private @interface Foo {

    }

    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    private @interface Bar {

    }

    @Foo
    @Bar
    public static class Carrier {

    }
}
