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
     * Creates a new field registry with the given attribute appender registered for the supplied field matcher.
     *
     * @param latentFieldMatcher       The field matcher uniquely identifying the field to be registered.
     * @param attributeAppenderFactory The field attribute appender factory to be registered for this field.
     * @param defaultValue             The field's default value or {@code null} if no such default value is set.
     * @return A new field registry that knows about the new field registration.
     */
    FieldRegistry include(LatentFieldMatcher latentFieldMatcher,
                          FieldAttributeAppender.Factory attributeAppenderFactory,
                          Object defaultValue);

    Prepared prepare(TypeDescription instrumentedType);

    static interface Prepared {

        Compiled compile(TypeWriter.FieldPool.Entry fallback);
    }

    /**
     * Represents a compiled field registry.
     */
    static interface Compiled extends TypeWriter.FieldPool {

        /**
         * A no-op field registry that does not register annotations for any field.
         */
        static enum NoOp implements Compiled {

            /**
             * The singleton instance.
             */
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

        /**
         * Contains all non-prepared field registry entries mapped by the field name. This map should never be mutated.
         */
        private final Map<String, Entry> entries;

        /**
         * Creates a new field registry without any registered fields.
         */
        public Default() {
            entries = Collections.emptyMap();
        }

        /**
         * Creates a new field registry.
         *
         * @param entries The non-prepared entries of this field registry.
         */
        private Default(Map<String, Entry> entries) {
            this.entries = entries;
        }

        @Override
        public FieldRegistry include(LatentFieldMatcher latentFieldMatcher,
                                     FieldAttributeAppender.Factory attributeAppenderFactory,
                                     Object defaultValue) {
            Map<String, Entry> entries = new HashMap<String, Entry>(this.entries);
            if (entries.put(latentFieldMatcher.getFieldName(), new Entry(attributeAppenderFactory, defaultValue)) != null) {
                throw new IllegalArgumentException("the field name " + latentFieldMatcher.getFieldName() + " is already registered");
            }
            return new Default(entries);
        }

        @Override
        public Prepared prepare(TypeDescription instrumentedType) {
            Map<String, TypeWriter.FieldPool.Entry> entries = new HashMap<String, TypeWriter.FieldPool.Entry>(this.entries.size());
            Map<FieldAttributeAppender.Factory, FieldAttributeAppender> attributeAppenders =
                    new HashMap<FieldAttributeAppender.Factory, FieldAttributeAppender>(this.entries.size());
            for (Map.Entry<String, Entry> entry : this.entries.entrySet()) {
                FieldAttributeAppender attributeAppender = attributeAppenders.get(entry.getValue().getAttributeAppenderFactory());
                if (attributeAppender == null) {
                    attributeAppender = entry.getValue().getAttributeAppenderFactory().make(instrumentedType);
                    attributeAppenders.put(entry.getValue().getAttributeAppenderFactory(), attributeAppender);
                }
                entries.put(entry.getKey(), new TypeWriter.FieldPool.Entry.Simple(attributeAppender, entry.getValue().getDefaultValue()));
            }
            return new Prepared(entries);
        }

        @Override
        public boolean equals(Object other) {
            return this == other || !(other == null || getClass() != other.getClass())
                    && entries.equals(((Default) other).entries);
        }

        @Override
        public int hashCode() {
            return entries.hashCode();
        }

        @Override
        public String toString() {
            return "FieldRegistry.Default{entries=" + entries + '}';
        }

        protected static class Entry {

            private final FieldAttributeAppender.Factory attributeAppenderFactory;

            private final Object defaultValue;

            private Entry(FieldAttributeAppender.Factory attributeAppenderFactory, Object defaultValue) {
                this.attributeAppenderFactory = attributeAppenderFactory;
                this.defaultValue = defaultValue;
            }

            public FieldAttributeAppender.Factory getAttributeAppenderFactory() {
                return attributeAppenderFactory;
            }

            public Object getDefaultValue() {
                return defaultValue;
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                Entry entry = ((Entry) other);
                return attributeAppenderFactory.equals(entry.attributeAppenderFactory)
                        && !(defaultValue != null ? !defaultValue.equals(entry.defaultValue) : entry.defaultValue != null);
            }

            @Override
            public int hashCode() {
                return 31 * attributeAppenderFactory.hashCode() + (defaultValue != null ? defaultValue.hashCode() : 0);
            }

            @Override
            public String toString() {
                return "FieldRegistry.Default.Entry{" +
                        "attributeAppenderFactory=" + attributeAppenderFactory +
                        ", defaultValue=" + defaultValue +
                        '}';
            }
        }

        protected static class Prepared implements FieldRegistry.Prepared {

            private final Map<String, TypeWriter.FieldPool.Entry> entries;

            public Prepared(Map<String, TypeWriter.FieldPool.Entry> entries) {
                this.entries = entries;
            }

            @Override
            public FieldRegistry.Compiled compile(TypeWriter.FieldPool.Entry fallback) {
                return new Compiled(entries, fallback);
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && entries.equals(((Prepared) other).entries);
            }

            @Override
            public int hashCode() {
                return entries.hashCode();
            }

            @Override
            public String toString() {
                return "FieldRegistry.Default.Prepared{entries=" + entries + '}';
            }
        }

        /**
         * A compiled default field registry.
         */
        protected static class Compiled implements FieldRegistry.Compiled {

            private final Map<String, Entry> entries;

            private final Entry fallback;

            public Compiled(Map<String, Entry> entries, Entry fallback) {
                this.entries = entries;
                this.fallback = fallback;
            }

            @Override
            public Entry target(FieldDescription fieldDescription) {
                Entry entry = entries.get(fieldDescription.getInternalName());
                return entry == null ? fallback : entry;
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && entries.equals(((Compiled) other).entries)
                        && fallback.equals(((Compiled) other).fallback);
            }

            @Override
            public int hashCode() {
                return 31 * entries.hashCode() + fallback.hashCode();
            }

            @Override
            public String toString() {
                return "FieldRegistry.Default.Compiled{" +
                        "entries=" + entries +
                        ", fallback=" + fallback +
                        '}';
            }
        }
    }
}
