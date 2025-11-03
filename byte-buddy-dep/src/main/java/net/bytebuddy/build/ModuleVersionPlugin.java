package net.bytebuddy.build;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.utility.nullability.MaybeNull;

/**
 * A plugin that includes a version number in the declared module-info class.
 */
@HashCodeAndEqualsPlugin.Enhance
public class ModuleVersionPlugin implements Plugin, Plugin.Factory {

    /**
     * The version to include or {@code null} to not include a version.
     */
    @MaybeNull
    @HashCodeAndEqualsPlugin.ValueHandling(HashCodeAndEqualsPlugin.ValueHandling.Sort.REVERSE_NULLABILITY)
    private final String version;

    /**
     * Creates a new module version plugin.
     *
     * @param version The version to include or {@code null} to not include a version.
     */
    public ModuleVersionPlugin(@MaybeNull String version) {
        this.version = version;
    }

    /**
     * {@inheritDoc}
     */
    public Plugin make() {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public DynamicType.Builder<?> apply(DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassFileLocator classFileLocator) {
        return builder.adjustModule().version(version);
    }

    /**
     * {@inheritDoc}
     */
    public boolean matches(TypeDescription target) {
        return target.isModuleType();
    }

    /**
     * {@inheritDoc}
     */
    public void close() {
        /* do nothing */
    }
}
