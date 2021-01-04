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
package net.bytebuddy.build.maven;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.build.Plugin;
import org.apache.maven.plugin.MojoExecutionException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A transformation specification to apply during the plugin's execution.
 */
@SuppressFBWarnings(value = "UWF_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD", justification = "Written to by Maven")
public class Transformation extends CoordinateConfiguration {

    /**
     * The fully-qualified name of the plugin type.
     */
    public String plugin;

    /**
     * A list of arguments that are provided to the plugin for construction.
     */
    public List<PluginArgument> arguments;

    /**
     * Returns the plugin type name.
     *
     * @return The plugin type name.
     * @throws MojoExecutionException If the plugin name was not specified or is empty.
     */
    public String getPlugin() throws MojoExecutionException {
        if (plugin == null || plugin.length() == 0) {
            throw new MojoExecutionException("Plugin name was not specified");
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
