package net.bytebuddy.pool;

import net.bytebuddy.description.module.AbstractModuleDescriptionTest;
import net.bytebuddy.description.module.ModuleDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import org.junit.After;
import org.junit.Before;

public class TypePoolModuleDescriptionTest extends AbstractModuleDescriptionTest {

    private ClassFileLocator classFileLocator;
    private TypePool typePool;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        classFileLocator = ClassFileLocator.ForJarFile.of(jar);
        typePool = TypePool.Default.of(classFileLocator);
    }

    @After
    public void tearDown() throws Exception {
        typePool.clear();
        classFileLocator.close();
        super.tearDown();
    }

    @Override
    protected ModuleDescription toModuleDescription() {
        return typePool.describe("module-info").resolve().toModuleDescription();
    }
}
