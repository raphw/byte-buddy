package net.bytebuddy.description.module;

import net.bytebuddy.test.utility.JavaVersionRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;

import java.io.Closeable;
import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Collections;

public class ModuleDescriptionForLoadedModuleTest extends AbstractModuleDescriptionTest {

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    private ClassLoader classLoader;
    private Object module;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        Class<?> path = Class.forName("java.nio.file.Path");
        Class<?> moduleFinder = Class.forName("java.lang.module.ModuleFinder");
        Class<?> moduleLayer = Class.forName("java.lang.ModuleLayer");
        Object paths = Array.newInstance(path, 1);
        Array.set(paths, 0, File.class.getMethod("toPath").invoke(jar));
        Method moduleFinderOf = moduleFinder.getMethod("of", paths.getClass());
        Object finder = moduleFinderOf.invoke(null, paths);
        Object reference = ((Collection<?>) moduleFinder.getMethod("findAll").invoke(finder)).iterator().next();
        Object descriptor = reference.getClass().getMethod("descriptor").invoke(reference);
        String name = (String) descriptor.getClass().getMethod("name").invoke(descriptor);
        classLoader = new URLClassLoader(
                new URL[]{jar.toURI().toURL()},
                ClassLoader.getSystemClassLoader().getParent()
        );
        Object parent = moduleLayer.getMethod("boot").invoke(null);
        Class<?> configClass = Class.forName("java.lang.module.Configuration");
        Object config = configClass.getMethod(
                "resolve",
                moduleFinder,
                moduleFinder,
                Collection.class).invoke(parent.getClass().getMethod("configuration").invoke(parent),
                finder,
                moduleFinderOf.invoke(null, Array.newInstance(path, 0)),
                Collections.singleton(name));
        module = Class.forName("java.util.Optional")
                .getMethod("orElseThrow")
                .invoke(moduleLayer.getMethod("findModule", String.class).invoke(moduleLayer.getMethod(
                    "defineModulesWithOneLoader",
                    configClass,
                    ClassLoader.class).invoke(parent, config, classLoader), name));
    }

    @After
    public void tearDown() throws Exception {
        if (classLoader instanceof Closeable) {
            ((Closeable) classLoader).close();
        }
        super.tearDown();
    }

    @Override
    protected ModuleDescription toModuleDescription() {
        return ModuleDescription.ForLoadedModule.of(module);
    }

    @Test
    @Override
    @JavaVersionRule.Enforce(9)
    public void testModuleDescription() throws Exception {
        super.testModuleDescription();
    }
}
