package net.bytebuddy.pool;

import net.bytebuddy.description.type.AbstractPackageDescriptionTest;
import net.bytebuddy.description.type.PackageDescription;
import org.junit.After;
import org.junit.Before;

public class TypePoolDefaultPackageDescriptionTest extends AbstractPackageDescriptionTest {

    protected PackageDescription describe(Class<?> type) {
        return TypePool.Default.of(type.getClassLoader()).describe(type.getName()).resolve().getPackage();
    }
}
