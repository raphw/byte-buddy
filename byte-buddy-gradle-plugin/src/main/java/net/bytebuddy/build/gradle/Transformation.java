package net.bytebuddy.build.gradle;

import net.bytebuddy.build.Plugin;
import org.gradle.api.GradleException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A transformation specification to apply during the Gradle plugin's execution.
 */
public class Transformation extends AbstractUserConfiguration {

    /**
     * The fully-qualified name of the plugin type.
     */
    private String plugin;

    /**
     * A list of arguments that are provided to the plugin for construction.
     */
    private List<PluginArgument> arguments;

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

    /**
     * Sets the plugin arguments.
     *
     * @param arguments A list of arguments that are provided to the plugin during construction.
     */
    public void setArguments(List<PluginArgument> arguments) {
        this.arguments = arguments;
    }
}
