package com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.FieldAttributeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.field.FieldDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A field registry represents an extendable collection of fields which are identified by their names that are mapped
 * to a given {@link com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.FieldAttributeAppender}. Fields
 * can be uniquely identified by their name for a given type since fields are never inherited.
 * <p/>
 * This registry is the counterpart of a {@link com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.MethodRegistry}.
 * However, a field registry is implemented simpler since it does not have to deal with complex signatures or
 * inheritance. For the sake of consistency, the field registry follows however a similar pattern without introducing
 * unnecessary complexity.
 */
public interface FieldRegistry {

    /**
     * Represents a compiled field registry.
     */
    static interface Compiled {

        /**
         * A default implementation of a compiled field registry that simply returns a no-op
         * {@link com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.FieldAttributeAppender}
         * for any field.
         */
        static enum NoOp implements Compiled {
            INSTANCE;

            @Override
            public FieldAttributeAppender.Factory target(FieldDescription fieldDescription) {
                return FieldAttributeAppender.NoOp.INSTANCE;
            }
        }

        /**
         * Returns the field attribute appender that matches a given field description or a default field
         * attribute appender if no appender was registered for the given field.
         *
         * @param fieldDescription The field description of interest.
         * @return The registered field attribute appender for the given field or the default appender if no such
         * appender was found.
         */
        FieldAttributeAppender.Factory target(FieldDescription fieldDescription);
    }

    /**
     * A simple matcher for a field where fields can be uniquely identified by their identifier for a given type.
     */
    static interface LatentFieldMatcher {

        /**
         * Returns the name of the matched field.
         *
         * @return The name of the field to be matched by this field matcher.
         */
        String getFieldName();
    }

    /**
     * An immutable default implementation of a field registry.
     */
    static class Default implements FieldRegistry {

        private class Compiled implements FieldRegistry.Compiled {

            private final FieldAttributeAppender.Factory fallback;

            private Compiled(FieldAttributeAppender.Factory fallback) {
                this.fallback = fallback;
            }

            @Override
            public FieldAttributeAppender.Factory target(FieldDescription fieldDescription) {
                FieldAttributeAppender.Factory attributeAppenderFactory = entries.get(fieldDescription.getInternalName());
                if (attributeAppenderFactory == null) {
                    return fallback;
                } else {
                    return attributeAppenderFactory;
                }
            }
        }

        private final Map<String, FieldAttributeAppender.Factory> entries;

        /**
         * Creates a new field registry without any registered fields.
         */
        public Default() {
            entries = Collections.emptyMap();
        }

        private Default(Map<String, FieldAttributeAppender.Factory> entries) {
            this.entries = entries;
        }

        @Override
        public FieldRegistry include(LatentFieldMatcher latentFieldMatcher, FieldAttributeAppender.Factory attributeAppenderFactory) {
            Map<String, FieldAttributeAppender.Factory> entries = new HashMap<String, FieldAttributeAppender.Factory>(this.entries);
            if (entries.put(latentFieldMatcher.getFieldName(), attributeAppenderFactory) != null) {
                throw new IllegalArgumentException("the field name " + latentFieldMatcher.getFieldName() + " is already registered");
            }
            return new Default(entries);
        }

        @Override
        public FieldRegistry.Compiled compile(TypeDescription instrumentedType, FieldAttributeAppender.Factory fallback) {
            return new Compiled(fallback);
        }
    }

    /**
     * Creates a new field registry with the given attribute appender registered for the supplied field matcher.
     *
     * @param latentFieldMatcher       The field matcher uniquely identifying the field to be registered.
     * @param attributeAppenderFactory The field attribute appender factory to be registered for this field.
     * @return A new field registry that knows about the new field registration.
     */
    FieldRegistry include(LatentFieldMatcher latentFieldMatcher, FieldAttributeAppender.Factory attributeAppenderFactory);

    /**
     * Once all entries for a field registry were registered, a field registry can be compiled in order to allow the
     * retrieval of {@link com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.FieldAttributeAppender}s for
     * known fields. Additionally, a fallback attribute appender is to be supplied which is returned if a requested
     * field cannot is not known to the compiled field registry.
     * <p/>
     * If a field name is already registered, an exception will be thrown.
     *
     * @param instrumentedType The instrumented type for which this field registry is to be compiled.
     * @param fallback         The fallback field attribute appender factory that serves as a fallback for unknown fields.
     * @return A compiled field registry representing the fields that were registered with this field registry.
     */
    Compiled compile(TypeDescription instrumentedType, FieldAttributeAppender.Factory fallback);
}
