package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.instrumentation.attribute.FieldAttributeAppender;
import net.bytebuddy.instrumentation.field.FieldDescription;
import net.bytebuddy.instrumentation.type.TypeDescription;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A field registry represents an extendable collection of fields which are identified by their names that are mapped
 * to a given {@link net.bytebuddy.instrumentation.attribute.FieldAttributeAppender}. Fields
 * can be uniquely identified by their name for a given type since fields are never inherited.
 * <p>&nbsp;</p>
 * This registry is the counterpart of a {@link net.bytebuddy.dynamic.scaffold.MethodRegistry}.
 * However, a field registry is implemented simpler since it does not have to deal with complex signatures or
 * inheritance. For the sake of consistency, the field registry follows however a similar pattern without introducing
 * unnecessary complexity.
 */
public interface FieldRegistry {

    /**
     * Represents a compiled field registry.
     */
    static interface Compiled extends TypeWriter.FieldPool {

        /**
         * A no-op field registry that does not register annotations for any field.
         */
        static enum NoOp implements Compiled {
            INSTANCE;

            @Override
            public Entry target(FieldDescription fieldDescription) {
                return Entry.NoOp.INSTANCE;
            }
        }
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

            private final TypeWriter.FieldPool.Entry fallback;

            private Compiled(TypeWriter.FieldPool.Entry fallback) {
                this.fallback = fallback;
            }

            @Override
            public TypeWriter.FieldPool.Entry target(FieldDescription fieldDescription) {
                TypeWriter.FieldPool.Entry entry = entries.get(fieldDescription.getInternalName());
                if (entry == null) {
                    return fallback;
                } else {
                    return entry;
                }
            }
        }

        private final Map<String, TypeWriter.FieldPool.Entry> entries;

        /**
         * Creates a new field registry without any registered fields.
         */
        public Default() {
            entries = Collections.emptyMap();
        }

        private Default(Map<String, TypeWriter.FieldPool.Entry> entries) {
            this.entries = entries;
        }

        @Override
        public FieldRegistry include(LatentFieldMatcher latentFieldMatcher, FieldAttributeAppender.Factory attributeAppenderFactory) {
            Map<String, TypeWriter.FieldPool.Entry> entries = new HashMap<String, TypeWriter.FieldPool.Entry>(this.entries);
            TypeWriter.FieldPool.Entry entry = new TypeWriter.FieldPool.Entry.Simple(attributeAppenderFactory);
            if (entries.put(latentFieldMatcher.getFieldName(), entry) != null) {
                throw new IllegalArgumentException("the field name " + latentFieldMatcher.getFieldName() + " is already registered");
            }
            return new Default(entries);
        }

        @Override
        public FieldRegistry.Compiled compile(TypeDescription instrumentedType, TypeWriter.FieldPool.Entry fallback) {
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
     * retrieval of {@link net.bytebuddy.instrumentation.attribute.FieldAttributeAppender}s for
     * known fields. Additionally, a fallback attribute appender is to be supplied which is returned if a requested
     * field cannot is not known to the compiled field registry.
     * <p>&nbsp;</p>
     * If a field name is already registered, an exception will be thrown.
     *
     * @param instrumentedType The instrumented type for which this field registry is to be compiled.
     * @param fallback         A fallback entry that serves as a fallback for non-registered fields.
     * @return A compiled field registry representing the fields that were registered with this field registry.
     */
    Compiled compile(TypeDescription instrumentedType, TypeWriter.FieldPool.Entry fallback);
}
