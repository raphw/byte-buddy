package net.bytebuddy.implementation.attribute;

public enum AnnotationRetention {

    ENABLED(true),

    DISABLED(false);

    private final boolean enabled;

    AnnotationRetention(boolean enabled) {
        this.enabled = enabled;
    }

    public static AnnotationRetention of(boolean enabled) {
        return enabled
                ? ENABLED
                : DISABLED;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
