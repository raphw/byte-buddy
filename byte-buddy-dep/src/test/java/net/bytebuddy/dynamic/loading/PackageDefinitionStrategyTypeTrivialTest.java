package net.bytebuddy.dynamic.loading;

import org.junit.Test;

import java.net.URL;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class PackageDefinitionStrategyTypeTrivialTest {

    @Test
    public void testIsDefined() throws Exception {
        assertThat(PackageDefinitionStrategy.Definition.Trivial.INSTANCE.isDefined(), is(true));
    }

    @Test
    public void testSpecificationTitle() throws Exception {
        assertThat(PackageDefinitionStrategy.Definition.Trivial.INSTANCE.getSpecificationTitle(), nullValue(String.class));
    }

    @Test
    public void testSpecificationVersion() throws Exception {
        assertThat(PackageDefinitionStrategy.Definition.Trivial.INSTANCE.getSpecificationVersion(), nullValue(String.class));
    }

    @Test
    public void testSpecificationVendor() throws Exception {
        assertThat(PackageDefinitionStrategy.Definition.Trivial.INSTANCE.getSpecificationVendor(), nullValue(String.class));
    }

    @Test
    public void testImplementationTitle() throws Exception {
        assertThat(PackageDefinitionStrategy.Definition.Trivial.INSTANCE.getImplementationTitle(), nullValue(String.class));
    }

    @Test
    public void testImplementationVersion() throws Exception {
        assertThat(PackageDefinitionStrategy.Definition.Trivial.INSTANCE.getImplementationVersion(), nullValue(String.class));
    }

    @Test
    public void testImplementationVendor() throws Exception {
        assertThat(PackageDefinitionStrategy.Definition.Trivial.INSTANCE.getImplementationVendor(), nullValue(String.class));
    }

    @Test
    public void testSealBase() throws Exception {
        assertThat(PackageDefinitionStrategy.Definition.Trivial.INSTANCE.getSealBase(), nullValue(URL.class));
    }

    @Test
    public void testIsCompatibleTo() throws Exception {
        assertThat(PackageDefinitionStrategy.Definition.Trivial.INSTANCE.isCompatibleTo(getClass().getPackage()), is(true));
    }
}
