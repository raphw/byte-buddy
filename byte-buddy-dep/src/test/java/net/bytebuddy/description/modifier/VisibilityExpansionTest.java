package net.bytebuddy.description.modifier;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class VisibilityExpansionTest {

    @Test
    public void testPublic() throws Exception {
        assertThat(Visibility.PUBLIC.expandTo(Visibility.PRIVATE), is(Visibility.PUBLIC));
        assertThat(Visibility.PUBLIC.expandTo(Visibility.PACKAGE_PRIVATE), is(Visibility.PUBLIC));
        assertThat(Visibility.PUBLIC.expandTo(Visibility.PROTECTED), is(Visibility.PUBLIC));
        assertThat(Visibility.PUBLIC.expandTo(Visibility.PUBLIC), is(Visibility.PUBLIC));
    }

    @Test
    public void testProtected() throws Exception {
        assertThat(Visibility.PROTECTED.expandTo(Visibility.PRIVATE), is(Visibility.PROTECTED));
        assertThat(Visibility.PROTECTED.expandTo(Visibility.PACKAGE_PRIVATE), is(Visibility.PROTECTED));
        assertThat(Visibility.PROTECTED.expandTo(Visibility.PROTECTED), is(Visibility.PROTECTED));
        assertThat(Visibility.PROTECTED.expandTo(Visibility.PUBLIC), is(Visibility.PUBLIC));
    }

    @Test
    public void testPackagePrivate() throws Exception {
        assertThat(Visibility.PACKAGE_PRIVATE.expandTo(Visibility.PRIVATE), is(Visibility.PACKAGE_PRIVATE));
        assertThat(Visibility.PACKAGE_PRIVATE.expandTo(Visibility.PACKAGE_PRIVATE), is(Visibility.PACKAGE_PRIVATE));
        assertThat(Visibility.PACKAGE_PRIVATE.expandTo(Visibility.PROTECTED), is(Visibility.PROTECTED));
        assertThat(Visibility.PACKAGE_PRIVATE.expandTo(Visibility.PUBLIC), is(Visibility.PUBLIC));
    }

    @Test
    public void testPrivate() throws Exception {
        assertThat(Visibility.PRIVATE.expandTo(Visibility.PRIVATE), is(Visibility.PRIVATE));
        assertThat(Visibility.PRIVATE.expandTo(Visibility.PACKAGE_PRIVATE), is(Visibility.PACKAGE_PRIVATE));
        assertThat(Visibility.PRIVATE.expandTo(Visibility.PROTECTED), is(Visibility.PROTECTED));
        assertThat(Visibility.PRIVATE.expandTo(Visibility.PUBLIC), is(Visibility.PUBLIC));
    }
}
