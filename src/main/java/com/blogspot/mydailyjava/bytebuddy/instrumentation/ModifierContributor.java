package com.blogspot.mydailyjava.bytebuddy.instrumentation;

public interface ModifierContributor {

    static interface ForMethod extends ModifierContributor {
        /* marker interface */
    }

    static interface ForField extends ModifierContributor {
        /* marker interface */
    }

    static interface ForType extends ModifierContributor {
        /* marker interface */
    }

    int getMask();
}
