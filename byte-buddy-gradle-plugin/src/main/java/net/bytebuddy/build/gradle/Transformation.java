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
package net.bytebuddy.build.gradle;

import groovy.lang.Closure;
import net.bytebuddy.build.Plugin;
import net.bytebuddy.utility.nullability.MaybeNull;
import net.bytebuddy.utility.nullability.UnknownNull;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;

import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

/**
 * A transformation specification to apply during the Gradle plugin's execution.
 */
public class Transformation {

    /**
     * The current project to use.
     */
    @UnknownNull
    private final transient Project project;

    /**
     * A list of arguments that are provided to the plugin for construction.
     */
    private final List<PluginArgument> arguments;

    /**
     * The plugin type.
     */
    @MaybeNull
    private Class<? extends Plugin> plugin;

    /**
     * The fully-qualified name of the plugin type.
     */
    @MaybeNull
    private String pluginName;

    /**
     * Creates a new transformation without configuring a project. This constructor is for
     * internal use only as closures cannot be resolved.
     */
    public Transformation() {
        project = null;
        arguments = new ArrayList<PluginArgument>();
    }

    /**
     * Creates a new transformation.
     *
     * @param project The object factory to use.
     */
    @Inject
    public Transformation(Project project) {
        this.project = project;
        arguments = new ArrayList<PluginArgument>();
    }

    /**
     * Returns a list of arguments that are provided to the plugin for construction.
     *
     * @return A list of arguments that are provided to the plugin for construction.
     */
    @Input
    public List<PluginArgument> getArguments() {
        return arguments;
    }

    /**
     * Adds a plugin argument to consider during instantiation.
     *
     * @param closure The closure for configuring the argument.
     */
    public void argument(Closure<?> closure) {
        getArguments().add((PluginArgument) project.configure(new PluginArgument(getArguments().size()), closure));
    }

    /**
     * Adds a plugin argument to consider during instantiation.
     *
     * @param action The action for configuring the argument.
     */
    public void argument(Action<PluginArgument> action) {
        PluginArgument argument = new PluginArgument(getArguments().size());
        action.execute(argument);
        getArguments().add(argument);
    }

    /**
     * Creates the argument resolvers for the plugin's constructor by transforming the plugin arguments.
     *
     * @return A list of argument resolvers.
     */
    protected List<Plugin.Factory.UsingReflection.ArgumentResolver> makeArgumentResolvers() {
        List<Plugin.Factory.UsingReflection.ArgumentResolver> argumentResolvers = new ArrayList<Plugin.Factory.UsingReflection.ArgumentResolver>();
        for (PluginArgument argument : getArguments()) {
            argumentResolvers.add(argument.toArgumentResolver());
        }
        return argumentResolvers;
    }

    /**
     * Returns the plugin type to apply.
     *
     * @return The plugin type to apply.
     */
    @Input
    @MaybeNull
    @Optional
    public Class<? extends Plugin> getPlugin() {
        return plugin;
    }

    /**
     * Sets the plugin type to apply.
     *
     * @param plugin The plugin type to apply.
     */
    public void setPlugin(@MaybeNull Class<? extends Plugin> plugin) {
        this.plugin = plugin;
    }

    /**
     * Returns the fully-qualified name of the plugin type to apply.
     *
     * @return The fully-qualified name of the plugin type to apply.
     */
    @Input
    @MaybeNull
    @Optional
    public String getPluginName() {
        return pluginName;
    }

    /**
     * Sets the fully-qualified name of the plugin type to apply.
     *
     * @param pluginName The fully-qualified name of the plugin type to apply.
     */
    public void setPluginName(@MaybeNull String pluginName) {
        this.pluginName = pluginName;
    }

    /**
     * Resolves the plugin type.
     *
     * @param classLoader The class loader to load a plugin from, if appropriate.
     * @return The loaded plugin type.
     */
    protected Class<? extends Plugin> toPlugin(@MaybeNull ClassLoader classLoader) {
        if (plugin != null) {
            if (pluginName != null && !plugin.getName().equals(pluginName)) {
                throw new GradleException("Defined both plugin (" + plugin + ") and plugin name (" + pluginName + ") but they are not equal");
            }
            if (Plugin.class.isAssignableFrom(plugin)) {
                return plugin;
            }
            @SuppressWarnings("unchecked")
            Class<? extends Plugin> type = (Class<? extends Plugin>) PluginResolvingClassLoader.wrap(getClass().getClassLoader(), plugin);
            if (!Plugin.class.isAssignableFrom(type)) {
                throw new GradleException(type.getName() + " does not implement " + Plugin.class.getName());
            }
            return type;
        } else if (pluginName != null) {
            try {
                @SuppressWarnings("unchecked")
                Class<? extends Plugin> type = (Class<? extends Plugin>) Class.forName(pluginName, false, classLoader);
                if (!Plugin.class.isAssignableFrom(type)) {
                    throw new GradleException(type.getName() + " does not implement " + Plugin.class.getName());
                }
                return type;
            } catch (ClassNotFoundException exception) {
                throw new GradleException("Cannot locate plugin class " + pluginName + " by its name", exception);
            }
        } else {
            throw new GradleException("No plugin or plugin name defined for transformation");
        }
    }

    /**
     * Resolves the name of the plugin.
     *
     * @return The name of the plugin.
     */
    protected String toPluginName() {
        if (plugin != null) {
            if (pluginName != null && !plugin.getName().equals(pluginName)) {
                throw new GradleException("Defined both plugin (" + plugin + ") and plugin name (" + pluginName + ") but they are not equal");
            }
            return plugin.getName();
        } else if (pluginName != null) {
            return pluginName;
        } else {
            throw new GradleException("No plugin or plugin name defined for transformation");
        }
    }

    /**
     * A class loader that resolves the plugin against a class loader that is a parent of the Byte Buddy plugin's
     * class loader.
     */
    protected static class PluginResolvingClassLoader extends ClassLoader {

        /**
         * The class loader of the user supplied plugin.
         */
        private final ClassLoader classLoader;

        /**
         * The protection domain to resolve.
         */
        private final ProtectionDomain protectionDomain;

        /**
         * Creates a new plugin resolving class loader.
         *
         * @param parent           The parent class loader.
         * @param classLoader      The class loader of the user supplied plugin.
         * @param protectionDomain The protection domain to resolve.
         */
        protected PluginResolvingClassLoader(ClassLoader parent, ClassLoader classLoader, ProtectionDomain protectionDomain) {
            super(parent);
            this.classLoader = classLoader;
            this.protectionDomain = protectionDomain;
        }

        /**
         * Wraps a plugin type with a class loader that resolves Byte Buddy via the Byte Buddy plugin's class loader.
         *
         * @param parent The parent class loader.
         * @param type   The supplied type.
         * @return The wrapped type.
         */
        public static Class<?> wrap(ClassLoader parent, Class<?> type) {
            try {
                return Class.forName(type.getName(),
                    false,
                    new PluginResolvingClassLoader(parent, type.getClassLoader(), type.getProtectionDomain()));
            } catch (ClassNotFoundException exception) {
                throw new IllegalStateException("Failed to wrap plugin type " + type.getName(), exception);
            }
        }

        /**
         * {@inheritDoc}
         */
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            if (!name.startsWith("net.bytebuddy.")) {
                InputStream inputStream = classLoader.getResourceAsStream(name.replace('.', '/') + ".class");
                if (inputStream == null) {
                    return super.findClass(name);
                }
                byte[] binaryRepresentation;
                try {
                    ByteArrayOutputStream outputStream;
                    try {
                        outputStream = new ByteArrayOutputStream();
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, length);
                        }
                    } finally {
                        inputStream.close();
                    }
                    binaryRepresentation = outputStream.toByteArray();
                } catch (IOException exception) {
                    throw new IllegalStateException(exception);
                }
                return defineClass(name, binaryRepresentation, 0, binaryRepresentation.length, protectionDomain);
            }
            return super.findClass(name);
        }
    }
}
