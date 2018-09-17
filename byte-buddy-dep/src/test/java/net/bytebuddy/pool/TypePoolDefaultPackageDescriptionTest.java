package net.bytebuddy.pool;

import net.bytebuddy.description.type.AbstractPackageDescriptionTest;
import net.bytebuddy.description.type.PackageDescription;
import org.junit.After;
import org.junit.Before;

public class TypePoolDefaultPackageDescriptionTest extends AbstractPackageDescriptionTest {

    private TypePool typePool;

    @Before
    public void setUp() throws Exception {
        typePool = TypePool.Default.ofClassPath();
    }

    @After
    public void tearDown() throws Exception {
        typePool.clear();
    }

    protected PackageDescription describe(Class<?> type) {
        return typePool.describe(type.getName()).resolve().getPackage();
    }
}
