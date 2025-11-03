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

@HashCodeAndEqualsPlugin.Enhance
public class SimpleModuleLayerResolver implements ModuleLayerResolver {

    private static final ModuleFinder MODULE_FINDER = doPrivileged(JavaDispatcher.of(ModuleFinder.class));

    private static final ModuleDescriptor MODULE_DESCRIPTOR = doPrivileged(JavaDispatcher.of(ModuleDescriptor.class));

    private static final ModuleLayer MODULE_LAYER = doPrivileged(JavaDispatcher.of(ModuleLayer.class));

    private static final ModuleLayerController1 MODULE_LAYER_CONTROLLER = doPrivileged(JavaDispatcher.of(ModuleLayerController1.class));

    private static final Configuration CONFIGURATION = doPrivileged(JavaDispatcher.of(Configuration.class));

    private static final Optional OPTIONAL = doPrivileged(JavaDispatcher.of(Optional.class));

    private static final Stream STREAM = doPrivileged(JavaDispatcher.of(Stream.class));

    private static final Path PATH = doPrivileged(JavaDispatcher.of(Path.class));

    private static final SimpleModuleReference SIMPLE_MODULE_REFERENCE;

    private static final SimpleModuleFinder SIMPLE_MODULE_FINDER;

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
                    .load(SimpleModuleLayerResolver.class.getClassLoader()).getAllLoaded();
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
    @MaybeNull
    public ClassLoader resolve(@MaybeNull ClassLoader classLoader, Map<String, byte[]> types) {
        Object moduleDescriptor;
        try {
            moduleDescriptor = MODULE_DESCRIPTOR.read(new ByteArrayInputStream(types.get(ModuleDescription.MODULE_CLASS_NAME)));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to create module layer", exception);
        }
        Object moduleReference = SIMPLE_MODULE_REFERENCE.newInstance(moduleDescriptor, null, types);
        return MODULE_LAYER.findLoader(MODULE_LAYER_CONTROLLER.layer(
                MODULE_LAYER.defineModulesWithOneLoader(CONFIGURATION.resolve(configuration(),
                                SIMPLE_MODULE_FINDER.newInstance(MODULE_DESCRIPTOR.name(moduleDescriptor), moduleReference),
                                moduleFinder(),
                                Collections.singleton(MODULE_DESCRIPTOR.name(moduleDescriptor))),
                        Collections.singletonList(MODULE_LAYER.boot()),
                        classLoader)), MODULE_DESCRIPTOR.name(moduleDescriptor));
    }

    protected Object configuration() {
        return MODULE_LAYER.configuration(MODULE_LAYER.boot());
    }

    protected Object moduleFinder() {
        return MODULE_FINDER.of(PATH.of(0));
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

        String name(Object value);
    }

    /**
     * A proxy for the {@code java.lang.ModuleLayer} type.
     */
    @JavaDispatcher.Proxied("java.lang.ModuleLayer")
    protected interface ModuleLayer {

        @JavaDispatcher.IsStatic
        Object boot();

        @JavaDispatcher.IsStatic
        Object defineModulesWithOneLoader(@JavaDispatcher.Proxied("java.lang.module.Configuration") Object configuration,
                                          List<?> moduleLayers,
                                          @MaybeNull ClassLoader classLoaders);

        Object configuration(Object value);

        @MaybeNull
        ClassLoader findLoader(Object value, String name);
    }

    @JavaDispatcher.Proxied("java.lang.ModuleLayer$Controller")
    protected interface ModuleLayerController1 {

        Object layer(Object value);
    }

    @JavaDispatcher.Proxied("java.lang.module.Configuration")
    protected interface Configuration {

        Object resolve(Object value,
                       @JavaDispatcher.Proxied("java.lang.module.ModuleFinder") Object before,
                       @JavaDispatcher.Proxied("java.lang.module.ModuleFinder") Object after,
                       Collection<String> roots);
    }

    @JavaDispatcher.Proxied("java.lang.module.ModuleFinder")
    protected interface ModuleFinder {

        @JavaDispatcher.IsStatic
        Object of(@JavaDispatcher.Proxied("java.nio.file.Path") Object[] path);
    }

    @JavaDispatcher.Proxied("java.nio.file.Path")
    protected interface Path {

        @JavaDispatcher.Container
        Object[] of(int length);
    }

    @JavaDispatcher.Proxied("net.bytebuddy.dynamic.loading.SimpleModuleReference")
    protected interface SimpleModuleReference {

        @JavaDispatcher.IsConstructor
        Object newInstance(@JavaDispatcher.Proxied("java.lang.module.ModuleDescriptor") Object moduleDescriptor,
                           @MaybeNull URI location,
                           Map<String, byte[]> types);
    }

    @JavaDispatcher.Proxied("net.bytebuddy.dynamic.loading.SimpleModuleFinder")
    protected interface SimpleModuleFinder {

        @JavaDispatcher.IsConstructor
        Object newInstance(String name, Object moduleReference);
    }

    @JavaDispatcher.Proxied("java.util.Optional")
    protected interface Optional {

        @JavaDispatcher.IsStatic
        Object of(Object value);

        @JavaDispatcher.IsStatic
        Object empty();
    }

    @JavaDispatcher.Proxied("java.util.stream.Stream")
    protected interface Stream {

        @JavaDispatcher.IsStatic
        Object empty();
    }

    public abstract static class AbstractModuleReader implements Closeable {

        private final Map<String, byte[]> types;

        protected AbstractModuleReader(Map<String, byte[]> types) {
            this.types = types;
        }

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

        protected Object doList() {
            return STREAM.empty();
        }

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

    public abstract static class AbstractModuleFinder {

        private final String name;

        private final Object moduleReference;

        protected AbstractModuleFinder(String name, Object moduleReference) {
            this.name = name;
            this.moduleReference = moduleReference;
        }

        @MaybeNull
        protected Object doFind(String name) {
            return name.equals(this.name)
                    ? OPTIONAL.of(moduleReference)
                    : OPTIONAL.empty();
        }

        protected Set<?> doFindAll() {
            return Collections.singleton(moduleReference);
        }
    }
}
