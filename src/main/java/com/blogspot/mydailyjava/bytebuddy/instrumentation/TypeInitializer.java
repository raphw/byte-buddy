package com.blogspot.mydailyjava.bytebuddy.instrumentation;

import java.io.Serializable;
import java.util.List;

public interface TypeInitializer {

    static class Compound implements TypeInitializer, Serializable {

        private final TypeInitializer[] typeInitializer;

        public Compound(TypeInitializer... typeInitializer) {
            this.typeInitializer = typeInitializer;
        }

        public Compound(List<? extends TypeInitializer> typeInitializers) {
            this.typeInitializer = typeInitializers.toArray(new TypeInitializer[typeInitializers.size()]);
        }

        @Override
        public void onLoad(Class<?> type) {
            for (TypeInitializer typeInitializer : this.typeInitializer) {
                typeInitializer.onLoad(type);
            }
        }
    }

    static enum NoOp implements TypeInitializer {
        INSTANCE;

        @Override
        public void onLoad(Class<?> type) {
            /* do nothing */
        }
    }

    void onLoad(Class<?> type);
}
