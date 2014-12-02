package net.bytebuddy.instrumentation.type;

import net.bytebuddy.instrumentation.attribute.annotation.AnnotationList;
import net.bytebuddy.test.pkg.Sample;
import net.bytebuddy.test.pkg.child.Child;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public abstract class AbstractPackageDescriptionTest {

    protected abstract PackageDescription describe(Class<?> type);

    @Test
    public void testTrivialPackage() throws Exception {
        assertThat(describe(Child.class).getName(), is(Child.class.getPackage().getName()));
        assertThat(describe(Child.class).getDeclaredAnnotations(), is((AnnotationList) new AnnotationList.Empty()));
    }

    @Test
    public void testNonTrivialPackage() throws Exception {
        assertThat(describe(Sample.class).getName(), is(Sample.class.getPackage().getName()));
        assertThat(describe(Sample.class).getDeclaredAnnotations(),
                is((AnnotationList) new AnnotationList.ForLoadedAnnotation(Sample.class.getPackage().getDeclaredAnnotations())));
    }

    @Test
    public void testHashCode() throws Exception {
        assertThat(describe(Child.class).hashCode(), is(Child.class.getPackage().hashCode()));
        assertThat(describe(Child.class).hashCode(), is(describe(Child.class).hashCode()));
        assertThat(describe(Child.class).hashCode(), not(is(describe(Sample.class).hashCode())));
        assertThat(describe(Sample.class).hashCode(), is(Sample.class.getPackage().hashCode()));
        assertThat(describe(Sample.class).hashCode(), is(describe(Sample.class).hashCode()));
        assertThat(describe(Sample.class).hashCode(), not(is(describe(Child.class).hashCode())));
    }

    @Test
    public void testEquals() throws Exception {
        assertThat(describe(Child.class).toString(), not(equalTo(null)));
        assertThat(describe(Child.class).toString(), not(equalTo(new Object())));
        assertThat(describe(Child.class).toString(), equalTo(describe(Child.class).toString()));
        assertThat(describe(Child.class).toString(), not(equalTo(describe(Sample.class).toString())));
        assertThat(describe(Sample.class).toString(), equalTo(describe(Sample.class).toString()));
        assertThat(describe(Sample.class).toString(), not(equalTo(describe(Child.class).toString())));
    }

    @Test
    public void testToString() throws Exception {
        assertThat(describe(Child.class).toString(), is(Child.class.getPackage().toString()));
        assertThat(describe(Child.class).toString(), is(describe(Child.class).toString()));
        assertThat(describe(Child.class).toString(), not(is(describe(Sample.class).toString())));
        assertThat(describe(Sample.class).toString(), is(Sample.class.getPackage().toString()));
        assertThat(describe(Sample.class).toString(), is(describe(Sample.class).toString()));
        assertThat(describe(Sample.class).toString(), not(is(describe(Child.class).toString())));
    }
}
