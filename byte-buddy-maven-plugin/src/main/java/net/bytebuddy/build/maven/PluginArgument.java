package net.bytebuddy.build.maven;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.build.Plugin;

/**
 * Describes an argument to a {@link Plugin} constuctor.
 */
@SuppressFBWarnings(value = "UWF_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD", justification = "Written to by Maven.")
public class PluginArgument {

    /**
     * The argument index.
     */
    public int index;

    /**
     * The argument value.
     */
    public String value;

    /**
     * Resolves this plugin argument to an argument resolver.
     *
     * @return An argument resolver that represents this plugin argument.
     */
    public Plugin.Factory.UsingReflection.ArgumentResolver toArgumentResolver() {
        return new Plugin.Factory.UsingReflection.ArgumentResolver.ForIndex.WithDynamicType(index, value);
    }
}
