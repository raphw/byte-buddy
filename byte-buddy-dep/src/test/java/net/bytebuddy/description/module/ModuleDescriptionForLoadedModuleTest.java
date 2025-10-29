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
import java.util.Set;

public class ModuleDescriptionForLoadedModuleTest extends AbstractModuleDescriptionTest {

    @Rule
    public MethodRule javaVersionRule = new JavaVersionRule();

    private ClassLoader classLoader;
    private Object module;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        Class<?> pathClass = Class.forName("java.nio.file.Path");
        Method toPathMethod = File.class.getMethod("toPath");
        Object path = toPathMethod.invoke(jar);
        Object pathArray = Array.newInstance(pathClass, 1);
        Array.set(pathArray, 0, path);

        // --- ModuleFinder.of(Path...) ---
        Class<?> moduleFinderClass = Class.forName("java.lang.module.ModuleFinder");
        Method ofMethod = moduleFinderClass.getMethod("of", pathArray.getClass());
        Object finder = ofMethod.invoke(null, pathArray);
        Object finderEmpty = ofMethod.invoke(null, Array.newInstance(pathClass, 0));

        // --- finder.findAll().stream().findFirst() ---
        Method findAllMethod = moduleFinderClass.getMethod("findAll");
        Collection<?> refs = (Collection<?>) findAllMethod.invoke(finder);
        Object ref = refs.iterator().next();

        // --- ref.descriptor() ---
        Method descriptorMethod = ref.getClass().getMethod("descriptor");
        Object descriptor = descriptorMethod.invoke(ref);

        // --- descriptor.name() ---
        Method nameMethod = descriptor.getClass().getMethod("name");
        String moduleName = (String) nameMethod.invoke(descriptor);

        classLoader = new URLClassLoader(
                new URL[]{jar.toURI().toURL()},
                ClassLoader.getSystemClassLoader().getParent()
        );
        // --- ModuleLayer.boot() ---
        Class<?> moduleLayerClass = Class.forName("java.lang.ModuleLayer");
        Method bootMethod = moduleLayerClass.getMethod("boot");
        Object parentLayer = bootMethod.invoke(null);

        // --- parentLayer.configuration() ---
        Method configMethod = parentLayer.getClass().getMethod("configuration");
        Object parentConfig = configMethod.invoke(parentLayer);

        // --- Configuration.resolve(...) ---
        Class<?> configClass = Class.forName("java.lang.module.Configuration");
        Method resolveMethod = configClass.getMethod(
                "resolve",
                moduleFinderClass,
                moduleFinderClass,
                Collection.class);
        Set<String> roots = Collections.singleton(moduleName);
        Object config = resolveMethod.invoke(parentConfig, finder, finderEmpty, roots);

        // --- parentLayer.defineModulesWithOneLoader(...) ---
        Method defineMethod = moduleLayerClass.getMethod(
                "defineModulesWithOneLoader",
                configClass,
                ClassLoader.class
        );
        Object layer = defineMethod.invoke(parentLayer, config, classLoader);

        // --- layer.findModule(...) ---
        Method findModuleMethod = moduleLayerClass.getMethod("findModule", String.class);
        Object moduleOpt = findModuleMethod.invoke(layer, moduleName);
        Class<?> optionalClass = Class.forName("java.util.Optional");
        Method getMethod = optionalClass.getMethod("orElseThrow");
        module = getMethod.invoke(moduleOpt);
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
