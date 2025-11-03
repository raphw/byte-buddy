/*
 * Copyright 2014 - Present Rafael Winterhalter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bytebuddy.dynamic.loading;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.build.AccessControllerPlugin;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.modifier.FieldManifestation;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.module.ModuleDescription;
import net.bytebuddy.description.type.PackageDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.utility.dispatcher.JavaDispatcher;
import net.bytebuddy.utility.nullability.MaybeNull;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

/**
 * A simple implementation of a {@link ClassLoaderDecorator} that creates module layers for dynamically
 * generated types using the Java Module System. The module information is resolved from a provided
 * {@code module-info} class. Without such a class, the decoration is omitted.
 */
@HashCodeAndEqualsPlugin.Enhance
public class ModuleLayerFromSingleClassLoaderDecorator implements ClassLoaderDecorator {

    /**
     * A proxy for {@code java.lang.module.ModuleFinder}.
     */
    private static final ModuleFinder MODULE_FINDER = doPrivileged(JavaDispatcher.of(ModuleFinder.class));

    /**
     * A proxy for {@code java.lang.module.ModuleDescriptor}.
     */
    private static final ModuleDescriptor MODULE_DESCRIPTOR = doPrivileged(JavaDispatcher.of(ModuleDescriptor.class));

    /**
     * A proxy for {@code java.lang.ModuleLayer}.
     */
    private static final ModuleLayer MODULE_LAYER = doPrivileged(JavaDispatcher.of(ModuleLayer.class));

    /**
     * A proxy for {@code java.lang.ModuleLayer.Controller}.
     */
    private static final ModuleLayerController MODULE_LAYER_CONTROLLER = doPrivileged(JavaDispatcher.of(ModuleLayerController.class));

    /**
     * A proxy for {@code java.lang.module.Configuration}.
     */
    private static final Configuration CONFIGURATION = doPrivileged(JavaDispatcher.of(Configuration.class));

    /**
     * A proxy for {@code java.util.Optional}.
     */
    private static final Optional OPTIONAL = doPrivileged(JavaDispatcher.of(Optional.class));

    /**
     * A proxy for {@code java.util.stream.Stream}.
     */
    private static final Stream STREAM = doPrivileged(JavaDispatcher.of(Stream.class));

    /**
     * A proxy for {@code java.nio.file.Path}.
     */
    private static final Path PATH = doPrivileged(JavaDispatcher.of(Path.class));

    /**
     * A proxy for the dynamically generated simple module reference class.
     */
    private static final SimpleModuleReference SIMPLE_MODULE_REFERENCE;

    /**
     * A proxy for the dynamically generated simple module finder class.
     */
    private static final SimpleModuleFinder SIMPLE_MODULE_FINDER;

    /**
     * Attempts to resolve the dynamically generated types to interact with the module system.
     */
    static {
        ClassLoader simpleModuleReferenceClassLoader, simpleModuleFinderClassLoader;
        try {
            ByteBuddy byteBuddy = new ByteBuddy();
            DynamicType.Unloaded<AbstractModuleReader> simpleModuleReader = byteBuddy.subclass(AbstractModuleReader.class, ConstructorStrategy.Default.IMITATE_SUPER_CLASS_OPENING)
                    .implement(Class.forName("java.lang.module.ModuleReader"))
                    .name("net.bytebuddy.dynamic.loading.SimpleModuleReader")
                    .method(named("open").and(takesArguments(String.class)))
                    .intercept(MethodCall.invoke(AbstractModuleReader.class.getDeclaredMethod("doOpen", String.class))
                            .withAllArguments()
                            .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC))
                    .method(named("list").and(takesArguments(0)))
                    .intercept(MethodCall.invoke(AbstractModuleReader.class.getDeclaredMethod("doList"))
                            .withAllArguments()
                            .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC))
                    .method(named("find").and(takesArguments(String.class)))
                    .intercept(MethodCall.invoke(AbstractModuleReader.class.getDeclaredMethod("doFind", String.class))
                            .withAllArguments()
                            .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC))
                    .make();
            Class<?> moduleDescriptor = Class.forName("java.lang.module.ModuleDescriptor"), moduleReference = Class.forName("java.lang.module.ModuleReference");
            DynamicType.Unloaded<?> simpleModuleReference = byteBuddy.subclass(moduleReference, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                    .name("net.bytebuddy.dynamic.loading.SimpleModuleReference")
                    .defineField("types", Map.class, Visibility.PRIVATE, FieldManifestation.FINAL)
                    .defineConstructor(Visibility.PUBLIC)
                    .withParameters(moduleDescriptor, URI.class, Map.class)
                    .intercept(MethodCall.invoke(moduleReference.getDeclaredConstructor(moduleDescriptor, URI.class))
                            .onSuper()
                            .withArgument(0, 1).andThen(FieldAccessor.ofField("types").setsArgumentAt(2)))
                    .method(named("open"))
                    .intercept(MethodCall.construct(simpleModuleReader.getTypeDescription().getDeclaredMethods().filter(isConstructor()).getOnly())
                            .withField("types")
                            .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC))
                    .make();
            DynamicType.Unloaded<AbstractModuleFinder> simpleModuleFinder = byteBuddy.subclass(AbstractModuleFinder.class, ConstructorStrategy.Default.IMITATE_SUPER_CLASS_OPENING)
                    .implement(Class.forName("java.lang.module.ModuleFinder"))
                    .name("net.bytebuddy.dynamic.loading.SimpleModuleFinder")
                    .method(named("find").and(takesArguments(String.class)))
                    .intercept(MethodCall.invoke(AbstractModuleFinder.class.getDeclaredMethod("doFind", String.class))
                            .withAllArguments()
                            .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC))
                    .method(named("findAll").and(takesArguments(0)))
                    .intercept(MethodCall.invoke(AbstractModuleFinder.class.getDeclaredMethod("doFindAll"))
                            .withAllArguments()
                            .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC))
                    .make();
            Map<TypeDescription, Class<?>> types = simpleModuleReader
                    .include(simpleModuleReference, simpleModuleFinder)
                    .load(ModuleLayerFromSingleClassLoaderDecorator.class.getClassLoader()).getAllLoaded();
            simpleModuleReferenceClassLoader = types.get(simpleModuleReference.getTypeDescription()).getClassLoader();
            simpleModuleFinderClassLoader = types.get(simpleModuleFinder.getTypeDescription()).getClassLoader();
        } catch (Exception ignored) {
            simpleModuleReferenceClassLoader = null;
            simpleModuleFinderClassLoader = null;
        }
        SIMPLE_MODULE_REFERENCE = doPrivileged(JavaDispatcher.of(SimpleModuleReference.class, simpleModuleReferenceClassLoader));
        SIMPLE_MODULE_FINDER = doPrivileged(JavaDispatcher.of(SimpleModuleFinder.class, simpleModuleFinderClassLoader));
    }

    /**
     * The class loader to delegate to when types are not handled by the module layer.
     */
    @MaybeNull
    @HashCodeAndEqualsPlugin.ValueHandling(HashCodeAndEqualsPlugin.ValueHandling.Sort.REVERSE_NULLABILITY)
    private final ClassLoader classLoader;

    /**
     * The module layer containing the dynamically created module.
     */
    private final Object moduleLayer;

    /**
     * The name of the module within the module layer.
     */
    private final String name;

    /**
     * The packages that are exported by the module.
     */
    private final Set<String> packages;

    /**
     * Creates a new module layer from module info decorator.
     *
     * @param classLoader The class loader to delegate to when types are not handled by the module layer.
     * @param moduleLayer The module layer containing the dynamically created module.
     * @param name        The name of the module within the module layer.
     * @param packages    The packages that are exported by the module.
     */
    protected ModuleLayerFromSingleClassLoaderDecorator(@MaybeNull ClassLoader classLoader,
                                                        Object moduleLayer,
                                                        String name,
                                                        Set<String> packages) {
        this.classLoader = classLoader;
        this.moduleLayer = moduleLayer;
        this.name = name;
        this.packages = packages;
    }

    /**
     * A proxy for {@code java.security.AccessController#doPrivileged} that is activated if available.
     *
     * @param action The action to execute from a privileged context.
     * @param <T>    The type of the action's resolved value.
     * @return The action's resolved value.
     */
    @AccessControllerPlugin.Enhance
    private static <T> T doPrivileged(PrivilegedAction<T> action) {
        return action.run();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSkipped(TypeDescription typeDescription) {
        return typeDescription.isModuleType();
    }

    /**
     * {@inheritDoc}
     */
    @MaybeNull
    public ClassLoader apply(TypeDescription typeDescription) {
        PackageDescription packageDescription = typeDescription.getPackage();
        return packageDescription == null || !packages.contains(packageDescription.getName())
                ? classLoader
                : MODULE_LAYER.findLoader(moduleLayer, name);
    }

    /**
     * A factory for creating module layer from module info decorators.
     */
    public enum Factory implements ClassLoaderDecorator.Factory {

        /**
         * The singleton instance of this factory.
         */
        INSTANCE;

        /**
         * {@inheritDoc}
         */
        public ClassLoaderDecorator make(@MaybeNull ClassLoader classLoader, Map<String, byte[]> typeDefinitions) {
            if (!typeDefinitions.containsKey(ModuleDescription.MODULE_CLASS_NAME)) {
                return new ClassLoaderDecorator.NoOp(classLoader);
            }
            Object moduleDescriptor;
            try {
                moduleDescriptor = MODULE_DESCRIPTOR.read(new ByteArrayInputStream(typeDefinitions.get(ModuleDescription.MODULE_CLASS_NAME)));
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to create module layer", exception);
            }
            Object moduleReference = SIMPLE_MODULE_REFERENCE.newInstance(moduleDescriptor, null, typeDefinitions);
            return new ModuleLayerFromSingleClassLoaderDecorator(classLoader,
                    MODULE_LAYER_CONTROLLER.layer(MODULE_LAYER.defineModulesWithOneLoader(CONFIGURATION.resolve(MODULE_LAYER.configuration(MODULE_LAYER.boot()),
                                    SIMPLE_MODULE_FINDER.newInstance(MODULE_DESCRIPTOR.name(moduleDescriptor), moduleReference),
                                    MODULE_FINDER.of(PATH.of(0)),
                                    Collections.singleton(MODULE_DESCRIPTOR.name(moduleDescriptor))),
                            Collections.singletonList(MODULE_LAYER.boot()),
                            classLoader)),
                    MODULE_DESCRIPTOR.name(moduleDescriptor),
                    MODULE_DESCRIPTOR.packages(moduleDescriptor));
        }
    }

    /**
     * A proxy for the {@code java.lang.module.ModuleDescriptor} type.
     */
    @JavaDispatcher.Proxied("java.lang.module.ModuleDescriptor")
    protected interface ModuleDescriptor {

        /**
         * Resolves an input stream to a module descriptor.
         *
         * @param inputStream The input stream of the class file of the module.
         * @return A suitable module description.
         * @throws IOException If an I/O exception occurs.
         */
        @JavaDispatcher.IsStatic
        Object read(InputStream inputStream) throws IOException;

        /**
         * Returns the name of the given module descriptor.
         *
         * @param value The module descriptor.
         * @return The module name.
         */
        String name(Object value);

        /**
         * Returns the packages of the given module descriptor.
         *
         * @param value The module descriptor.
         * @return The included packages.
         */
        Set<String> packages(Object value);
    }

    /**
     * A proxy for the {@code java.lang.ModuleLayer} type.
     */
    @JavaDispatcher.Proxied("java.lang.ModuleLayer")
    protected interface ModuleLayer {

        /**
         * Returns the boot module layer.
         *
         * @return The boot module layer.
         */
        @JavaDispatcher.IsStatic
        Object boot();

        /**
         * Defines modules with a single class loader.
         *
         * @param configuration The module configuration.
         * @param moduleLayers  The parent module layers.
         * @param classLoaders  The class loader to use.
         * @return The created module layer controller.
         */
        @JavaDispatcher.IsStatic
        Object defineModulesWithOneLoader(@JavaDispatcher.Proxied("java.lang.module.Configuration") Object configuration,
                                          List<?> moduleLayers,
                                          @MaybeNull ClassLoader classLoaders);

        /**
         * Returns the configuration of the given module layer.
         *
         * @param value The module layer.
         * @return The module layer's configuration.
         */
        Object configuration(Object value);

        /**
         * Finds the class loader for a named module.
         *
         * @param value The module layer.
         * @param name  The module name.
         * @return The class loader for the module or {@code null} if not found.
         */
        @MaybeNull
        ClassLoader findLoader(Object value, String name);
    }

    /**
     * A proxy for the {@code java.lang.ModuleLayer.Controller} type.
     */
    @JavaDispatcher.Proxied("java.lang.ModuleLayer$Controller")
    protected interface ModuleLayerController {

        /**
         * Returns the module layer associated with this controller.
         *
         * @param value The module layer controller.
         * @return The associated module layer.
         */
        Object layer(Object value);
    }

    /**
     * A proxy for the {@code java.lang.module.Configuration} type.
     */
    @JavaDispatcher.Proxied("java.lang.module.Configuration")
    protected interface Configuration {

        /**
         * Resolves a module configuration.
         *
         * @param value  The base configuration.
         * @param before The module finder to search before the system module finder.
         * @param after  The module finder to search after the system module finder.
         * @param roots  The module names to resolve.
         * @return The resolved configuration.
         */
        Object resolve(Object value,
                       @JavaDispatcher.Proxied("java.lang.module.ModuleFinder") Object before,
                       @JavaDispatcher.Proxied("java.lang.module.ModuleFinder") Object after,
                       Collection<String> roots);
    }

    /**
     * A proxy for the {@code java.lang.module.ModuleFinder} type.
     */
    @JavaDispatcher.Proxied("java.lang.module.ModuleFinder")
    protected interface ModuleFinder {

        /**
         * Creates a module finder from the given paths.
         *
         * @param path The paths to search for modules.
         * @return A module finder for the given paths.
         */
        @JavaDispatcher.IsStatic
        Object of(@JavaDispatcher.Proxied("java.nio.file.Path") Object[] path);
    }

    /**
     * A proxy for the {@code java.nio.file.Path} type.
     */
    @JavaDispatcher.Proxied("java.nio.file.Path")
    protected interface Path {

        /**
         * Creates an array of paths with the given length.
         *
         * @param length The length of the path array.
         * @return An array of paths with the specified length.
         */
        @JavaDispatcher.Container
        Object[] of(int length);
    }

    /**
     * A proxy for the dynamically generated {@code SimpleModuleReference} type.
     */
    @JavaDispatcher.Proxied("net.bytebuddy.dynamic.loading.SimpleModuleReference")
    protected interface SimpleModuleReference {

        /**
         * Creates a new instance of the simple module reference.
         *
         * @param moduleDescriptor The module descriptor.
         * @param location         The module location URI or {@code null}.
         * @param types            The map of type names to their byte representations.
         * @return A new simple module reference instance.
         */
        @JavaDispatcher.IsConstructor
        Object newInstance(@JavaDispatcher.Proxied("java.lang.module.ModuleDescriptor") Object moduleDescriptor,
                           @MaybeNull URI location,
                           Map<String, byte[]> types);
    }

    /**
     * A proxy for the dynamically generated {@code SimpleModuleFinder} type.
     */
    @JavaDispatcher.Proxied("net.bytebuddy.dynamic.loading.SimpleModuleFinder")
    protected interface SimpleModuleFinder {

        /**
         * Creates a new instance of the simple module finder.
         *
         * @param name            The module name.
         * @param moduleReference The module reference.
         * @return A new simple module finder instance.
         */
        @JavaDispatcher.IsConstructor
        Object newInstance(String name, Object moduleReference);
    }

    /**
     * A proxy for the {@code java.util.Optional} type.
     */
    @JavaDispatcher.Proxied("java.util.Optional")
    protected interface Optional {

        /**
         * Creates an optional containing the given value.
         *
         * @param value The value to wrap.
         * @return An optional containing the value.
         */
        @JavaDispatcher.IsStatic
        Object of(Object value);

        /**
         * Creates an empty optional.
         *
         * @return An empty optional.
         */
        @JavaDispatcher.IsStatic
        Object empty();
    }

    /**
     * A proxy for the {@code java.util.stream.Stream} type.
     */
    @JavaDispatcher.Proxied("java.util.stream.Stream")
    protected interface Stream {

        /**
         * Creates an empty stream.
         *
         * @return An empty stream.
         */
        @JavaDispatcher.IsStatic
        Object empty();
    }

    /**
     * An abstract implementation of a module reader that provides access to dynamically generated types.
     * <p>
     * This class serves as a base for creating module readers that can handle byte code representations
     * of classes within a module. It implements the {@link Closeable} interface but provides an empty
     * implementation for the close method.
     */
    public abstract static class AbstractModuleReader implements Closeable {

        /**
         * The map containing type names and their byte representations.
         */
        private final Map<String, byte[]> types;

        /**
         * Creates a new abstract module reader.
         *
         * @param types The map of type names to their byte representations.
         */
        protected AbstractModuleReader(Map<String, byte[]> types) {
            this.types = types;
        }

        /**
         * Finds a resource within the module.
         *
         * @param name The resource name.
         * @return An optional containing the resource URI if found, empty otherwise.
         */
        protected Object doFind(String name) {
            if (name.endsWith(".class")) {
                String value = name.substring(0, name.length() - ".class".length()).replace('/', '.');
                byte[] binaryRepresentation = types.get(value);
                if (binaryRepresentation != null) {
                    return OPTIONAL.of(URI.create("bytebuddy://" + name));
                }
            }
            return OPTIONAL.empty();
        }

        /**
         * Lists all resources in the module.
         *
         * @return An empty stream as listing is not supported.
         */
        protected Object doList() {
            return STREAM.empty();
        }

        /**
         * Opens an input stream to a resource within the module.
         *
         * @param name The resource name.
         * @return An optional containing the input stream if the resource exists, empty otherwise.
         */
        protected Object doOpen(String name) {
            if (name.endsWith(".class")) {
                String value = name.substring(0, name.length() - ".class".length()).replace('/', '.');
                byte[] binaryRepresentation = types.get(value);
                if (binaryRepresentation != null) {
                    return OPTIONAL.of(new ByteArrayInputStream(binaryRepresentation));
                }
            }
            return OPTIONAL.empty();
        }

        /**
         * {@inheritDoc}
         */
        public void close() {
            /* empty */
        }
    }

    /**
     * An abstract implementation of a module finder that can locate specific modules.
     * <p>
     * This class provides the base functionality for finding modules based on their names
     * and serves as a foundation for creating custom module finders.
     */
    public abstract static class AbstractModuleFinder {

        /**
         * The name of the module this finder can locate.
         */
        private final String name;

        /**
         * The module reference for the module this finder manages.
         */
        private final Object moduleReference;

        /**
         * Creates a new abstract module finder.
         *
         * @param name            The name of the module.
         * @param moduleReference The module reference.
         */
        protected AbstractModuleFinder(String name, Object moduleReference) {
            this.name = name;
            this.moduleReference = moduleReference;
        }

        /**
         * Finds a module by name.
         *
         * @param name The module name to find.
         * @return An optional containing the module reference if found, empty otherwise.
         */
        @MaybeNull
        protected Object doFind(String name) {
            return name.equals(this.name)
                    ? OPTIONAL.of(moduleReference)
                    : OPTIONAL.empty();
        }

        /**
         * Finds all modules managed by this finder.
         *
         * @return A set containing the single module reference managed by this finder.
         */
        protected Set<?> doFindAll() {
            return Collections.singleton(moduleReference);
        }
    }
}
