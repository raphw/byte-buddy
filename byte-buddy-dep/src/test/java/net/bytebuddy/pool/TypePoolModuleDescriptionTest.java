package net.bytebuddy.pool;

import net.bytebuddy.description.module.AbstractModuleDescriptionTest;
import net.bytebuddy.description.module.ModuleDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import org.junit.After;
import org.junit.Before;

public class TypePoolModuleDescriptionTest extends AbstractModuleDescriptionTest {

    private TypePool typePool;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        typePool = TypePool.Default.of(ClassFileLocator.ForJarFile.of(jar));
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        typePool.clear();
    }

    @Override
    protected ModuleDescription toModuleDescription() {
        return typePool.describe("module-info").resolve().toModuleDescription();
    }
}
