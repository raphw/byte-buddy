package net.bytebuddy.description.module;

import net.bytebuddy.test.utility.JavaVersionRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.io.Closeable;
import java.net.URL;
import java.net.URLClassLoader;

public class ModuleDescriptionForLoadedModuleTest extends AbstractModuleDescriptionTest {

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    private ClassLoader classLoader;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        classLoader = new URLClassLoader(new URL[]{jar.toURI().toURL()}, AbstractModuleDescriptionTest.class.getClassLoader());
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        if (classLoader instanceof Closeable) {
            ((Closeable) classLoader).close();
        }
    }

    @Override
    protected ModuleDescription toModuleDescription() throws Exception {
        Class<?> type = classLoader.loadClass(FOO + "." + BAR);
        Object module = Class.class.getMethod("getModule").invoke(type);
        return ModuleDescription.ForLoadedModule.of(module);
    }

    @Test
    @Override
    @JavaVersionRule.Enforce(9)
    public void testModuleDescription() throws Exception {
        super.testModuleDescription();
    }
}
