package net.bytebuddy.description.type;

import net.bytebuddy.description.annotation.AnnotationList;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class PackageDescriptionSimpleTest {

    private static final String FOO = "foo";

    @Test
    public void testPackageName() throws Exception {
        assertThat(new PackageDescription.Simple(FOO).getName(), is(FOO));
    }

    @Test
    public void testPackageAnnotations() throws Exception {
        assertThat(new PackageDescription.Simple(FOO).getDeclaredAnnotations(), is((AnnotationList) new AnnotationList.Empty()));
    }
}
