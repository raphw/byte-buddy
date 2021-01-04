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
import org.gradle.api.tasks.Input;
import org.gradle.util.ConfigureUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * A transformation specification to apply during the Gradle plugin's execution.
 */
public class Transformation {

    /**
     * A list of arguments that are provided to the plugin for construction.
     */
    private final List<PluginArgument> arguments;

    /**
     * The fully-qualified name of the plugin type.
     */
    private Class<? extends Plugin> plugin;

    /**
     * Creates a new transformation.
     */
    public Transformation() {
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
        getArguments().add(ConfigureUtil.configure(closure, new PluginArgument(getArguments().size())));
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
    public Class<? extends Plugin> getPlugin() {
        return plugin;
    }

    /**
     * Sets the plugin type to apply.
     *
     * @param plugin The plugin type to apply.
     */
    public void setPlugin(Class<? extends Plugin> plugin) {
        this.plugin = plugin;
    }
}
