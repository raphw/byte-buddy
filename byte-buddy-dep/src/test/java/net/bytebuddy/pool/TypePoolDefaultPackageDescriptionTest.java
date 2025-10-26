package net.bytebuddy.pool;

import net.bytebuddy.description.type.AbstractPackageDescriptionTest;
import net.bytebuddy.description.type.PackageDescription;

public class TypePoolDefaultPackageDescriptionTest extends AbstractPackageDescriptionTest {

    protected PackageDescription describe(Class<?> type) {
        return TypePool.Default.of(type.getClassLoader()).describe(type.getName()).resolve().getPackage();
    }
}
