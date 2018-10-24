/*
 * Copyright 2014 - 2018 Rafael Winterhalter
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
import org.gradle.api.GradleException;
import org.gradle.api.Project;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A transformation specification to apply during the Gradle plugin's execution.
 */
public class Transformation extends AbstractUserConfiguration {

    /**
     * The current project.
     */
    private final Project project;

    /**
     * A list of arguments that are provided to the plugin for construction.
     */
    private final List<PluginArgument> arguments;

    /**
     * The fully-qualified name of the plugin type.
     */
    private String plugin;

    /**
     * Creates a new transformation.
     *
     * @param project The current project.
     */
    public Transformation(Project project) {
        this.project = project;
        arguments = new ArrayList<PluginArgument>();
    }

    /**
     * Adds a plugin argument to consider during instantiation.
     *
     * @param closure The closure for configuring the argument.
     */
    public void argument(Closure<?> closure) {
        arguments.add((PluginArgument) project.configure(new PluginArgument(), closure));
    }

    /**
     * Returns the plugin type name.
     *
     * @return The plugin type name.
     */
    public String getPlugin() {
        if (plugin == null || plugin.length() == 0) {
            throw new GradleException("Plugin name was not specified or is empty");
        }
        return plugin;
    }

    /**
     * Returns the plugin name or {@code null} if it is not set.
     *
     * @return The configured plugin name.
     */
    public String getRawPlugin() {
        return plugin;
    }

    /**
     * Sets the plugin's name.
     *
     * @param plugin The fully-qualified name of the plugin type.
     */
    public void setPlugin(String plugin) {
        this.plugin = plugin;
    }

    /**
     * Creates the argument resolvers for the plugin's constructor by transforming the plugin arguments.
     *
     * @return A list of argument resolvers.
     */
    public List<Plugin.Factory.UsingReflection.ArgumentResolver> makeArgumentResolvers() {
        if (arguments == null) {
            return Collections.emptyList();
        } else {
            List<Plugin.Factory.UsingReflection.ArgumentResolver> argumentResolvers = new ArrayList<Plugin.Factory.UsingReflection.ArgumentResolver>();
            for (PluginArgument argument : arguments) {
                argumentResolvers.add(argument.toArgumentResolver());
            }
            return argumentResolvers;
        }
    }
}
