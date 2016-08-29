package net.bytebuddy.build.maven;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * A transformation specification to apply during the plugin's execution.
 */
public class Transformation extends AbstractUserConfiguration {

    /**
     * The fully-qualified name of the plugin type.
     */
    protected String plugin;

    /**
     * Returns the plugin type name.
     *
     * @return The plugin type name.
     * @throws MojoExecutionException If the plugin name was not specified or is empty.
     */
    public String getPlugin() throws MojoExecutionException {
        if (plugin == null || plugin.isEmpty()) {
            throw new MojoExecutionException("Plugin name was not specified");
        }
        return plugin;
    }

}
