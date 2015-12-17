package net.bytebuddy.dynamic.loading;

import net.bytebuddy.test.utility.ObjectPropertyAssertion;
import org.junit.Test;

import java.net.URL;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class PackageDefinitionTrivialTest {

    private static final String FOO = "foo", BAR = "bar";

    @Test
    public void testPackageNotDefined() throws Exception {
        PackageDefinitionStrategy.Definition definition = PackageDefinitionStrategy.Trivial.INSTANCE.define(getClass().getClassLoader(), FOO, BAR);
        assertThat(definition.isDefined(), is(true));
        assertThat(definition.getImplementationTitle(), nullValue(String.class));
        assertThat(definition.getImplementationVersion(), nullValue(String.class));
        assertThat(definition.getImplementationVendor(), nullValue(String.class));
        assertThat(definition.getSpecificationTitle(), nullValue(String.class));
        assertThat(definition.getSpecificationVersion(), nullValue(String.class));
        assertThat(definition.getSpecificationVendor(), nullValue(String.class));
        assertThat(definition.getSealBase(), nullValue(URL.class));
        assertThat(definition.isCompatibleTo(getClass().getPackage()), is(true));
    }

    @Test
    public void testObjectProperties() throws Exception {
        ObjectPropertyAssertion.of(PackageDefinitionStrategy.Trivial.class).apply();
    }
}
