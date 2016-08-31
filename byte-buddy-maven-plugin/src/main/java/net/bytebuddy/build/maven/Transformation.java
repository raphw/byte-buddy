package net.bytebuddy.build.maven;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * A transformation specification to apply during the plugin's execution.
 */
@SuppressFBWarnings(value = "UWF_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD", justification = "Written to by Maven")
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

    /**
     * Returns the plugin name or {@code null} if it is not set.
     *
     * @return The configured plugin name.
     */
    public String getRawPlugin() {
        return plugin;
    }
}
