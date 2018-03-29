package net.bytebuddy.dynamic.loading;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class PackageDefinitionStrategyTypeUndefinedTest {

    @Test
    public void testIsUndefined() throws Exception {
        assertThat(PackageDefinitionStrategy.Definition.Undefined.INSTANCE.isDefined(), is(false));
    }

    @Test(expected = IllegalStateException.class)
    public void testSpecificationTitleThrowsException() throws Exception {
        PackageDefinitionStrategy.Definition.Undefined.INSTANCE.getSpecificationTitle();
    }

    @Test(expected = IllegalStateException.class)
    public void testSpecificationVersionThrowsException() throws Exception {
        PackageDefinitionStrategy.Definition.Undefined.INSTANCE.getSpecificationVersion();
    }

    @Test(expected = IllegalStateException.class)
    public void testSpecificationVendorThrowsException() throws Exception {
        PackageDefinitionStrategy.Definition.Undefined.INSTANCE.getSpecificationVendor();
    }

    @Test(expected = IllegalStateException.class)
    public void testImplementationTitleThrowsException() throws Exception {
        PackageDefinitionStrategy.Definition.Undefined.INSTANCE.getImplementationTitle();
    }

    @Test(expected = IllegalStateException.class)
    public void testImplementationVersionThrowsException() throws Exception {
        PackageDefinitionStrategy.Definition.Undefined.INSTANCE.getImplementationVersion();
    }

    @Test(expected = IllegalStateException.class)
    public void testImplementationVendorThrowsException() throws Exception {
        PackageDefinitionStrategy.Definition.Undefined.INSTANCE.getImplementationVendor();
    }

    @Test(expected = IllegalStateException.class)
    public void testSealBaseThrowsException() throws Exception {
        PackageDefinitionStrategy.Definition.Undefined.INSTANCE.getSealBase();
    }

    @Test(expected = IllegalStateException.class)
    public void testIsCompatibleToThrowsException() throws Exception {
        PackageDefinitionStrategy.Definition.Undefined.INSTANCE.isCompatibleTo(getClass().getPackage());
    }
}
