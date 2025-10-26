package net.bytebuddy.description.type;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.test.visibility.Sample;
import net.bytebuddy.test.visibility.child.Child;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
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
                is((AnnotationList) new AnnotationList.ForLoadedAnnotations(Sample.class.getPackage().getDeclaredAnnotations())));
    }

    @Test
    public void testPackageContains() throws Exception {
        assertThat(describe(Child.class).contains(TypeDescription.ForLoadedType.of(Child.class)), is(true));
        assertThat(describe(Object.class).contains(TypeDescription.ForLoadedType.of(Child.class)), is(false));
    }

    @Test
    public void testHashCode() throws Exception {
        assertThat(describe(Child.class).hashCode(), is(Child.class.getPackage().hashCode()));
        assertThat(describe(Child.class).hashCode(), is(describe(Child.class).hashCode()));
        assertThat(describe(Child.class).hashCode(), not(describe(Sample.class).hashCode()));
        assertThat(describe(Sample.class).hashCode(), is(Sample.class.getPackage().hashCode()));
        assertThat(describe(Sample.class).hashCode(), is(describe(Sample.class).hashCode()));
        assertThat(describe(Sample.class).hashCode(), not(describe(Child.class).hashCode()));
    }

    @Test
    public void testEquals() throws Exception {
        assertThat(describe(Child.class).toString(), not(equalTo(null)));
        assertThat(describe(Child.class).toString(), not(new Object()));
        assertThat(describe(Child.class).toString(), is(describe(Child.class).toString()));
        assertThat(describe(Child.class).toString(), not(describe(Sample.class).toString()));
        assertThat(describe(Sample.class).toString(), is(describe(Sample.class).toString()));
        assertThat(describe(Sample.class).toString(), not(describe(Child.class).toString()));
    }

    @Test
    public void testToString() throws Exception {
        assertThat(describe(Child.class).toString(), is(Child.class.getPackage().toString()));
        assertThat(describe(Child.class).toString(), is(describe(Child.class).toString()));
        assertThat(describe(Child.class).toString(), not(describe(Sample.class).toString()));
        assertThat(describe(Sample.class).toString(), is(Sample.class.getPackage().toString()));
        assertThat(describe(Sample.class).toString(), is(describe(Sample.class).toString()));
        assertThat(describe(Sample.class).toString(), not(describe(Child.class).toString()));
    }

    @Test
    public void testDefault() throws Exception {
        assertThat(describe(Child.class).isDefault(), is(false));
        Class<?> type = new ByteBuddy().subclass(Object.class)
                .name("TypeWithoutPackage")
                .make()
                .load(ClassLoadingStrategy.BOOTSTRAP_LOADER, ClassLoadingStrategy.Default.WRAPPER_PERSISTENT)
                .getLoaded();
        assertThat(describe(type).isDefault(), is(true));
        assertThat(describe(type).getName(), is(NamedElement.EMPTY_NAME));
    }

    @Test
    public void testNoPackage() throws Exception {
        assertThat(describe(boolean.class), nullValue(PackageDescription.class));
        assertThat(describe(Object[].class), nullValue(PackageDescription.class));
    }
}
