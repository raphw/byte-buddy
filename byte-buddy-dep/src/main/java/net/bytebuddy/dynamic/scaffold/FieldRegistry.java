package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.attribute.FieldAttributeAppender;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A field registry represents an extendable collection of fields which are identified by their names that are mapped
 * to a given {@link net.bytebuddy.implementation.attribute.FieldAttributeAppender}. Fields
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
     * @param fieldToken               A token identifying the field for which this entry is valid.
     * @param attributeAppenderFactory The field attribute appender factory to be registered for this field.
     * @param defaultValue             The field's default value or {@code null} if no such default value is set.
     * @return A new field registry that knows about the new field registration.
     */
    FieldRegistry include(FieldDescription.Token fieldToken, FieldAttributeAppender.Factory attributeAppenderFactory, Object defaultValue);

    /**
     * Prepares the field registry for a given instrumented type.
     *
     * @param instrumentedType The instrumented type.
     * @return A prepared field registry.
     */
    Compiled compile(TypeDescription instrumentedType);

    /**
     * Represents a compiled field registry.
     */
    interface Compiled extends TypeWriter.FieldPool {

        /**
         * A no-op field registry that does not register annotations for any field.
         */
        enum NoOp implements Compiled {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public Record target(FieldDescription fieldDescription) {
                return new Record.ForSimpleField(fieldDescription);
            }

            @Override
            public String toString() {
                return "FieldRegistry.Compiled.NoOp." + name();
            }
        }
    }

    /**
     * An immutable default implementation of a field registry.
     */
    class Default implements FieldRegistry {

        /**
         * Contains all non-prepared field registry entries mapped by the field name. This map should never be mutated.
         */
        private final Map<FieldDescription.Token, Entry> entries;

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
        private Default(Map<FieldDescription.Token, Entry> entries) {
            this.entries = entries;
        }

        @Override
        public FieldRegistry include(FieldDescription.Token fieldToken, FieldAttributeAppender.Factory attributeAppenderFactory, Object defaultValue) {
            Map<FieldDescription.Token, Entry> entries = new HashMap<FieldDescription.Token, Entry>(this.entries);
            if (entries.put(fieldToken, new Entry(attributeAppenderFactory, defaultValue)) != null) {
                throw new IllegalArgumentException(fieldToken + " is already registered");
            }
            return new Default(entries);
        }

        @Override
        public FieldRegistry.Compiled compile(TypeDescription instrumentedType) {
            Map<FieldDescription.Token, Compiled.Entry> entries = new HashMap<FieldDescription.Token, Compiled.Entry>(this.entries.size());
            Map<FieldAttributeAppender.Factory, FieldAttributeAppender> attributeAppenders = new HashMap<FieldAttributeAppender.Factory, FieldAttributeAppender>(this.entries.size());
            for (Map.Entry<FieldDescription.Token, Entry> entry : this.entries.entrySet()) {
                FieldAttributeAppender attributeAppender = attributeAppenders.get(entry.getValue().getAttributeAppenderFactory());
                if (attributeAppender == null) {
                    attributeAppender = entry.getValue().getAttributeAppenderFactory().make(instrumentedType);
                    attributeAppenders.put(entry.getValue().getAttributeAppenderFactory(), attributeAppender);
                }
                entries.put(entry.getKey(), new Compiled.Entry(attributeAppender, entry.getValue().getDefaultValue()));
            }
            return new Compiled(entries);
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

        /**
         * An entry of the default field registry.
         */
        protected static class Entry {

            /**
             * The field attribute appender factory that is represented by this entry.
             */
            private final FieldAttributeAppender.Factory attributeAppenderFactory;

            /**
             * The field's default value for this entry.
             */
            private final Object defaultValue;

            /**
             * Creates a new entry.
             *
             * @param attributeAppenderFactory The field attribute appender factory that is represented by this entry.
             * @param defaultValue             The field's default value for this entry.
             */
            protected Entry(FieldAttributeAppender.Factory attributeAppenderFactory, Object defaultValue) {
                this.attributeAppenderFactory = attributeAppenderFactory;
                this.defaultValue = defaultValue;
            }

            /**
             * Returns the field attribute appender factory.
             *
             * @return The field's attribute appender factory.
             */
            public FieldAttributeAppender.Factory getAttributeAppenderFactory() {
                return attributeAppenderFactory;
            }

            /**
             * Returns the default value.
             *
             * @return The default value.
             */
            public Object getDefaultValue() {
                return defaultValue;
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                Entry entry = (Entry) other;
                return attributeAppenderFactory.equals(entry.attributeAppenderFactory)
                        && !(defaultValue != null ? !defaultValue.equals(entry.defaultValue) : entry.defaultValue != null);
            }

            @Override
            public int hashCode() {
                int result = attributeAppenderFactory.hashCode();
                result = 31 * result + (defaultValue != null ? defaultValue.hashCode() : 0);
                return result;
            }

            @Override
            public String toString() {
                return "FieldRegistry.Default.Entry{" +
                        "attributeAppenderFactory=" + attributeAppenderFactory +
                        ", defaultValue=" + defaultValue +
                        '}';
            }
        }

        /**
         * A compiled default field registry.
         */
        protected static class Compiled implements FieldRegistry.Compiled {

            /**
             * A map of entries by field names.
             */
            private final Map<FieldDescription.Token, Entry> entries;

            /**
             * Creates a new compiled default field registry.
             *
             * @param entries A map of entries by field names.
             */
            public Compiled(Map<FieldDescription.Token, Entry> entries) {
                this.entries = entries;
            }

            @Override
            public Record target(FieldDescription fieldDescription) {
                Entry entry = entries.get(fieldDescription.asToken());
                return entry == null
                        ? new Record.ForSimpleField(fieldDescription)
                        : entry.bind(fieldDescription);
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && entries.equals(((Compiled) other).entries);
            }

            @Override
            public int hashCode() {
                return 31 * entries.hashCode();
            }

            @Override
            public String toString() {
                return "FieldRegistry.Default.Compiled{" +
                        "entries=" + entries +
                        '}';
            }

            /**
             * An entry of a compiled field registry.
             */
            protected static class Entry {

                /**
                 * The attribute appender to be applied to any bound field.
                 */
                private final FieldAttributeAppender attributeAppender;

                /**
                 * The default value to be set for any bound field or {@code null} if no default value should be set.
                 */
                private final Object defaultValue;

                /**
                 * Creates a new entry.
                 *
                 * @param attributeAppender The attribute appender to be applied to any bound field.
                 * @param defaultValue      The default value to be set for any bound field or {@code null} if no default value should be set.
                 */
                protected Entry(FieldAttributeAppender attributeAppender, Object defaultValue) {
                    this.attributeAppender = attributeAppender;
                    this.defaultValue = defaultValue;
                }

                /**
                 * Binds this entry to the provided field description.
                 *
                 * @param fieldDescription The field description to be bound to this entry.
                 * @return A record representing the binding of this entry to the provided field.
                 */
                protected Record bind(FieldDescription fieldDescription) {
                    return new Record.ForRichField(attributeAppender, defaultValue, fieldDescription);
                }

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (other == null || getClass() != other.getClass()) return false;
                    Entry entry = (Entry) other;
                    return attributeAppender.equals(entry.attributeAppender)
                            && !(defaultValue != null ? !defaultValue.equals(entry.defaultValue) : entry.defaultValue != null);
                }

                @Override
                public int hashCode() {
                    int result = attributeAppender.hashCode();
                    result = 31 * result + (defaultValue != null ? defaultValue.hashCode() : 0);
                    return result;
                }

                @Override
                public String toString() {
                    return "FieldRegistry.Default.Compiled.Entry{" +
                            "attributeAppender=" + attributeAppender +
                            ", defaultValue=" + defaultValue +
                            '}';
                }
            }
        }
    }
}
