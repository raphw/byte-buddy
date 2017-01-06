package net.bytebuddy.implementation.attribute;

/**
 * An annotation retention strategy decides if annotations that are contained within a class file are preserved upon redefining
 * or rebasing a method. When annotations are retained, it is important not to define annotations explicitly that are already
 * defined. When annotations are retained, they are retained in their original format, i.e. default values that were not included
 * in the class file are not added or skipped as determined by a {@link AnnotationValueFilter}.
 */
public enum AnnotationRetention {

    /**
     * Enables annotation retention, i.e. annotations within an existing class files are preserved as they are.
     */
    ENABLED(true),

    /**
     * Disables annotation retention, i.e. annotations within an existing class files are discarded.
     */
    DISABLED(false);

    /**
     * {@code true} if annotation retention is enabled.
     */
    private final boolean enabled;

    /**
     * Creates an annotation retention strategy.
     *
     * @param enabled {@code true} if annotation retention is enabled.
     */
    AnnotationRetention(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Resolves an annotation retention from a boolean value.
     *
     * @param enabled {@code true} if annotation retention is enabled.
     * @return An enabled annotation retention if the value is {@code true}.
     */
    public static AnnotationRetention of(boolean enabled) {
        return enabled
                ? ENABLED
                : DISABLED;
    }

    /**
     * Returns {@code true} if annotation retention is enabled.
     *
     * @return {@code true} if annotation retention is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }
}
