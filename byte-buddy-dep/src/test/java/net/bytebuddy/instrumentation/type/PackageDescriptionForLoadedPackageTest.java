package net.bytebuddy.instrumentation.type;

public class PackageDescriptionForLoadedPackageTest extends AbstractPackageDescriptionTest {

    @Override
    protected PackageDescription describe(Class<?> type) {
        return new PackageDescription.ForLoadedPackage(type.getPackage());
    }
}
