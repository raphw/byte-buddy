package net.bytebuddy.description.annotation;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.AbstractFilterableListTest;
import net.bytebuddy.matcher.ElementMatchers;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public abstract class AbstractAnnotationListTest<U> extends AbstractFilterableListTest<AnnotationDescription, AnnotationList, U> {

    @Test
    public void testAnnotationIsPresent() throws Exception {
        assertThat(asList(getFirst()).isAnnotationPresent(Foo.class), is(true));
    }

    @Test
    public void testAnnotationIsNotPresent() throws Exception {
        assertThat(asList(getFirst()).isAnnotationPresent(Annotation.class), is(false));
    }

    @Test
    public void testAnnotationIsPresentDescription() throws Exception {
        assertThat(asList(getFirst()).isAnnotationPresent(new TypeDescription.ForLoadedType(Foo.class)), is(true));
    }

    @Test
    public void testAnnotationIsNotPresentDescription() throws Exception {
        assertThat(asList(getFirst()).isAnnotationPresent(new TypeDescription.ForLoadedType(Annotation.class)), is(false));
    }

    @Test
    public void testAnnotationOfType() throws Exception {
        assertThat(asList(getFirst()).ofType(Foo.class), is(AnnotationDescription.ForLoadedAnnotation.of(Holder.class.getAnnotation(Foo.class))));
    }

    @Test
    public void testAnnotationOfTypeWrongType() throws Exception {
        assertThat(asList(getFirst()).ofType(Annotation.class), nullValue(AnnotationDescription.Loadable.class));
    }

    @Test
    public void testAnnotationOfTypeDescription() throws Exception {
        assertThat(asList(getFirst()).ofType(new TypeDescription.ForLoadedType(Foo.class)),
                is((AnnotationDescription) AnnotationDescription.ForLoadedAnnotation.of(Holder.class.getAnnotation(Foo.class))));
    }

    @Test
    public void testAnnotationWrongTypeOfTypeDescription() throws Exception {
        assertThat(asList(getFirst()).ofType(new TypeDescription.ForLoadedType(Annotation.class)), nullValue(AnnotationDescription.class));
    }

    @Test
    public void testInherited() throws Exception {
        assertThat(asList(getFirst()).inherited(Collections.<TypeDescription>emptySet()), is(asList(getFirst())));
    }

    @Test
    public void testInheritedIgnoreType() throws Exception {
        assertThat(asList(getFirst()).inherited(Collections.<TypeDescription>singleton(new TypeDescription.ForLoadedType(Foo.class))).size(), is(0));
    }

    @Test
    public void testInheritedIgnoreNonInherited() throws Exception {
        assertThat(asList(getSecond()).inherited(Collections.<TypeDescription>emptySet()).size(), is(0));
    }

    @Test
    public void testVisible() throws Exception {
        assertThat(asList(getFirst()).visibility(ElementMatchers.is(RetentionPolicy.RUNTIME)), is(asList(getFirst())));
    }

    @Test
    public void testNotVisible() throws Exception {
        assertThat(asList(getFirst()).visibility(ElementMatchers.is(RetentionPolicy.SOURCE)).size(), is(0));
    }

    @Test
    public void testAsTypeList() throws Exception {
        assertThat(asList(getFirst()).asTypeList(), is(Collections.singletonList(asElement(getFirst()).getAnnotationType())));
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    protected @interface Foo {
        /* empty */
    }

    @Retention(RetentionPolicy.RUNTIME)
    protected @interface Bar {
        /* empty */
    }

    @Foo
    @Bar
    public static class Holder {
        /* empty */
    }
}
