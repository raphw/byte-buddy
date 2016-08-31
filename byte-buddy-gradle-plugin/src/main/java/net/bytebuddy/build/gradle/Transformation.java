package net.bytebuddy.build.gradle;

import org.gradle.api.GradleException;

/**
 * A transformation specification to apply during the Gradle plugin's execution.
 */
public class Transformation extends AbstractUserConfiguration {

    /**
     * The fully-qualified name of the plugin type.
     */
    private String plugin;

    /**
     * Returns the plugin type name.
     *
     * @return The plugin type name.
     */
    public String getPlugin() {
        if (plugin == null || plugin.isEmpty()) {
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
}
