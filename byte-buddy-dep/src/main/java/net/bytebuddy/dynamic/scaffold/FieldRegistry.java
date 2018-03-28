package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.Transformer;
import net.bytebuddy.implementation.attribute.FieldAttributeAppender;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.LatentMatcher;

import java.util.*;

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
     * Prepends the given field definition to this field registry, i.e. this configuration is applied first.
     *
     * @param matcher                       The matcher to identify any field that this definition concerns.
     * @param fieldAttributeAppenderFactory The field attribute appender factory to apply on any matched field.
     * @param defaultValue                  The default value to write to the field or {@code null} if no default value is to be set for the field.
     * @param transformer              The field transformer to apply to any matched field.
     * @return An adapted version of this method registry.
     */
    FieldRegistry prepend(LatentMatcher<? super FieldDescription> matcher,
                          FieldAttributeAppender.Factory fieldAttributeAppenderFactory,
                          Object defaultValue,
                          Transformer<FieldDescription> transformer);

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
                return new Record.ForImplicitField(fieldDescription);
            }
        }
    }

    /**
     * An immutable default implementation of a field registry.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class Default implements FieldRegistry {

        /**
         * This registries entries.
         */
        private final List<Entry> entries;

        /**
         * Creates a new empty default field registry.
         */
        public Default() {
            this(Collections.<Entry>emptyList());
        }

        /**
         * Creates a new default field registry.
         *
         * @param entries The entries of the field registry.
         */
        private Default(List<Entry> entries) {
            this.entries = entries;
        }

        @Override
        public FieldRegistry prepend(LatentMatcher<? super FieldDescription> matcher,
                                     FieldAttributeAppender.Factory fieldAttributeAppenderFactory,
                                     Object defaultValue,
                                     Transformer<FieldDescription> transformer) {
            List<Entry> entries = new ArrayList<Entry>(this.entries.size() + 1);
            entries.add(new Entry(matcher, fieldAttributeAppenderFactory, defaultValue, transformer));
            entries.addAll(this.entries);
            return new Default(entries);
        }

        @Override
        public FieldRegistry.Compiled compile(TypeDescription instrumentedType) {
            List<Compiled.Entry> entries = new ArrayList<Compiled.Entry>(this.entries.size());
            Map<FieldAttributeAppender.Factory, FieldAttributeAppender> fieldAttributeAppenders = new HashMap<FieldAttributeAppender.Factory, FieldAttributeAppender>();
            for (Entry entry : this.entries) {
                FieldAttributeAppender fieldAttributeAppender = fieldAttributeAppenders.get(entry.getFieldAttributeAppenderFactory());
                if (fieldAttributeAppender == null) {
                    fieldAttributeAppender = entry.getFieldAttributeAppenderFactory().make(instrumentedType);
                    fieldAttributeAppenders.put(entry.getFieldAttributeAppenderFactory(), fieldAttributeAppender);
                }
                entries.add(new Compiled.Entry(entry.resolve(instrumentedType), fieldAttributeAppender, entry.getDefaultValue(), entry.getTransformer()));
            }
            return new Compiled(instrumentedType, entries);
        }

        /**
         * An entry of the default field registry.
         */
        @HashCodeAndEqualsPlugin.Enhance
        protected static class Entry implements LatentMatcher<FieldDescription> {

            /**
             * The matcher to identify any field that this definition concerns.
             */
            private final LatentMatcher<? super FieldDescription> matcher;

            /**
             * The field attribute appender factory to apply on any matched field.
             */
            private final FieldAttributeAppender.Factory fieldAttributeAppenderFactory;

            /**
             * The default value to write to the field or {@code null} if no default value is to be set for the field.
             */
            private final Object defaultValue;

            /**
             * The field transformer to apply to any matched field.
             */
            private final Transformer<FieldDescription> transformer;

            /**
             * Creates a new entry.
             *
             * @param matcher                       The matcher to identify any field that this definition concerns.
             * @param fieldAttributeAppenderFactory The field attribute appender factory to apply on any matched field.
             * @param defaultValue                  The default value to write to the field or {@code null} if no default value is to be set for the field.
             * @param transformer              The field transformer to apply to any matched field.
             */
            protected Entry(LatentMatcher<? super FieldDescription> matcher,
                            FieldAttributeAppender.Factory fieldAttributeAppenderFactory,
                            Object defaultValue,
                            Transformer<FieldDescription> transformer) {
                this.matcher = matcher;
                this.fieldAttributeAppenderFactory = fieldAttributeAppenderFactory;
                this.defaultValue = defaultValue;
                this.transformer = transformer;
            }

            /**
             * Returns the field attribute appender factory to apply on any matched field.
             *
             * @return The field attribute appender factory to apply on any matched field.
             */
            protected FieldAttributeAppender.Factory getFieldAttributeAppenderFactory() {
                return fieldAttributeAppenderFactory;
            }

            /**
             * Returns the default value to write to the field or {@code null} if no default value is to be set for the field.
             *
             * @return The default value to write to the field or {@code null} if no default value is to be set for the field.
             */
            protected Object getDefaultValue() {
                return defaultValue;
            }

            /**
             * Returns the field transformer to apply to any matched field.
             *
             * @return The field transformer to apply to any matched field.
             */
            protected Transformer<FieldDescription> getTransformer() {
                return transformer;
            }

            @Override
            public ElementMatcher<? super FieldDescription> resolve(TypeDescription typeDescription) {
                return matcher.resolve(typeDescription);
            }
        }

        /**
         * A compiled default field registry.
         */
        @HashCodeAndEqualsPlugin.Enhance
        protected static class Compiled implements FieldRegistry.Compiled {

            /**
             * The instrumented type for which this registry was compiled for.
             */
            private final TypeDescription instrumentedType;

            /**
             * The entries of this compiled field registry.
             */
            private final List<Entry> entries;

            /**
             * Creates a new compiled field registry.
             *
             * @param instrumentedType The instrumented type for which this registry was compiled for.
             * @param entries          The entries of this compiled field registry.
             */
            protected Compiled(TypeDescription instrumentedType, List<Entry> entries) {
                this.instrumentedType = instrumentedType;
                this.entries = entries;
            }

            @Override
            public Record target(FieldDescription fieldDescription) {
                for (Entry entry : entries) {
                    if (entry.matches(fieldDescription)) {
                        return entry.bind(instrumentedType, fieldDescription);
                    }
                }
                return new Record.ForImplicitField(fieldDescription);
            }

            /**
             * An entry of a compiled field registry.
             */
            @HashCodeAndEqualsPlugin.Enhance
            protected static class Entry implements ElementMatcher<FieldDescription> {

                /**
                 * The matcher to identify any field that this definition concerns.
                 */
                private final ElementMatcher<? super FieldDescription> matcher;

                /**
                 * The field attribute appender to apply on any matched field.
                 */
                private final FieldAttributeAppender fieldAttributeAppender;

                /**
                 * The default value to write to the field or {@code null} if no default value is to be set for the field.
                 */
                private final Object defaultValue;

                /**
                 * The field transformer to apply to any matched field.
                 */
                private final Transformer<FieldDescription> transformer;

                /**
                 * Creates a new entry.
                 *
                 * @param matcher                The matcher to identify any field that this definition concerns.
                 * @param fieldAttributeAppender The field attribute appender to apply on any matched field.
                 * @param defaultValue           The default value to write to the field or {@code null} if no default value is to be set for the field.
                 * @param transformer       The field transformer to apply to any matched field.
                 */
                protected Entry(ElementMatcher<? super FieldDescription> matcher,
                                FieldAttributeAppender fieldAttributeAppender,
                                Object defaultValue,
                                Transformer<FieldDescription> transformer) {
                    this.matcher = matcher;
                    this.fieldAttributeAppender = fieldAttributeAppender;
                    this.defaultValue = defaultValue;
                    this.transformer = transformer;
                }

                /**
                 * Binds this entry to the provided field description.
                 *
                 * @param instrumentedType The instrumented type for which this entry applies.
                 * @param fieldDescription The field description to be bound to this entry.
                 * @return A record representing the binding of this entry to the provided field.
                 */
                protected Record bind(TypeDescription instrumentedType, FieldDescription fieldDescription) {
                    return new Record.ForExplicitField(fieldAttributeAppender, defaultValue, transformer.transform(instrumentedType, fieldDescription));
                }

                @Override
                public boolean matches(FieldDescription target) {
                    return matcher.matches(target);
                }
            }
        }
    }
}
