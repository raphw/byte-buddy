package net.bytebuddy.matcher;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.utility.nullability.MaybeNull;
import net.bytebuddy.utility.nullability.UnknownNull;

/**
 * A matcher to consider if a class file version reaches a given boundary.
 *
 * @param <T> The exact type of the type description that is matched.
 */
@HashCodeAndEqualsPlugin.Enhance
public class ClassFileVersionMatcher<T extends TypeDescription> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The targeted class file version.
     */
    private final ClassFileVersion classFileVersion;

    /**
     * {@code true} if the targeted class file version should be at most of the supplied version.
     */
    private final boolean atMost;

    /**
     * Creates a class file version matcher.
     *
     * @param classFileVersion The targeted class file version.
     * @param atMost           {@code true} if the targeted class file version should be at most of the supplied version.
     */
    public ClassFileVersionMatcher(ClassFileVersion classFileVersion, boolean atMost) {
        this.classFileVersion = classFileVersion;
        this.atMost = atMost;
    }

    /**
     * {@inheritDoc}
     */
    public boolean matches(@UnknownNull T target) {
        ClassFileVersion classFileVersion = target.getClassFileVersion();
        return classFileVersion != null && (atMost ? classFileVersion.isAtMost(this.classFileVersion) : classFileVersion.isAtLeast(this.classFileVersion));
    }

    @Override
    public String toString() {
        return "hasClassFileVersion(at " + (atMost ? "most" : "least") + " " + classFileVersion + ")";
    }
}
