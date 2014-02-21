package com.blogspot.mydailyjava.bytebuddy.instrumentation;

public interface ModifierContributor {

    static final int EMPTY_MASK = 0;

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
