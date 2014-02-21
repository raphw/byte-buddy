package com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.FieldAttributeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.field.FieldDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.InstrumentedType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public interface FieldRegistry {

    static interface Compiled {

        FieldAttributeAppender.Factory target(FieldDescription fieldDescription);
    }

    static interface LatentFieldMatcher {

        String getFieldName();
    }

    static class Default implements FieldRegistry {

        private class Compiled implements FieldRegistry.Compiled {

            private final FieldAttributeAppender.Factory fallback;

            private Compiled(FieldAttributeAppender.Factory fallback) {
                this.fallback = fallback;
            }

            @Override
            public FieldAttributeAppender.Factory target(FieldDescription fieldDescription) {
                FieldAttributeAppender.Factory attributeAppenderFactory = entries.get(fieldDescription.getName());
                if (attributeAppenderFactory == null) {
                    return fallback;
                } else {
                    return attributeAppenderFactory;
                }
            }
        }

        private final Map<String, FieldAttributeAppender.Factory> entries;

        public Default() {
            entries = Collections.emptyMap();
        }

        private Default(Map<String, FieldAttributeAppender.Factory> entries) {
            this.entries = entries;
        }

        @Override
        public FieldRegistry prepend(LatentFieldMatcher latentFieldMatcher, FieldAttributeAppender.Factory attributeAppenderFactory) {
            Map<String, FieldAttributeAppender.Factory> entries = new HashMap<String, FieldAttributeAppender.Factory>(this.entries);
            entries.put(latentFieldMatcher.getFieldName(), attributeAppenderFactory);
            return new Default(entries);
        }

        @Override
        public Compiled compile(InstrumentedType instrumentedType, FieldAttributeAppender.Factory fallback) {
            return new Compiled(fallback);
        }
    }

    FieldRegistry prepend(LatentFieldMatcher latentFieldMatcher, FieldAttributeAppender.Factory attributeAppenderFactory);

    Compiled compile(InstrumentedType instrumentedType, FieldAttributeAppender.Factory fallback);
}
