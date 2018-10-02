package net.bytebuddy.build.gradle;

import net.bytebuddy.build.Plugin;

/**
 * Describes an argument to a {@link Plugin} constuctor.
 */
public class PluginArgument {

    /**
     * The argument index.
     */
    private int index;

    /**
     * The argument value.
     */
    private Object value;

    /**
     * Creats a new plugin argument with default initialization.
     */
    public PluginArgument() {
        /* empty */
    }

    /**
     * Creates a new plugin argument assignment.
     *
     * @param index The argument index.
     * @param value The argument value.
     */
    public PluginArgument(int index, Object value) {
        this.index = index;
        this.value = value;
    }

    /**
     * Sets the argument index.
     *
     * @param index The argument index.
     */
    public void setIndex(int index) {
        this.index = index;
    }

    /**
     * Sets the argument value.
     *
     * @param value The argument value.
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Resolves this plugin argument to an argument resolver.
     *
     * @return An argument resolver that represents this plugin argument.
     */
    public Plugin.Factory.UsingReflection.ArgumentResolver toArgumentResolver() {
        return new Plugin.Factory.UsingReflection.ArgumentResolver.ForIndex(index, value);
    }
}
