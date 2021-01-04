/*
 * Copyright 2014 - Present Rafael Winterhalter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.type.RecordComponentDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.Transformer;
import net.bytebuddy.implementation.attribute.RecordComponentAttributeAppender;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.LatentMatcher;

import java.util.*;

/**
 * A record component registry represents an extendable collection of record components which are identified by their names that are mapped
 * to a given {@link RecordComponentAttributeAppender}. Record components
 * can be uniquely identified by their name for a given type since record components are never inherited.
 * <p>&nbsp;</p>
 * This registry is the counterpart of a {@link MethodRegistry}.
 * However, a record component registry is implemented simpler since it does not have to deal with complex signatures or
 * inheritance. For the sake of consistency, the record component registry follows however a similar pattern without introducing
 * unnecessary complexity.
 */
public interface RecordComponentRegistry {

    /**
     * Prepends the given record component definition to this record component registry, i.e. this configuration is applied first.
     *
     * @param matcher                                 The matcher to identify any record component that this definition concerns.
     * @param recordComponentAttributeAppenderFactory The record component attribute appender factory to apply on any matched record component.
     * @param transformer                             The record component transformer to apply to any matched record component.
     * @return An adapted version of this method registry.
     */
    RecordComponentRegistry prepend(LatentMatcher<? super RecordComponentDescription> matcher,
                                    RecordComponentAttributeAppender.Factory recordComponentAttributeAppenderFactory,
                                    Transformer<RecordComponentDescription> transformer);

    /**
     * Prepares the record component registry for a given instrumented type.
     *
     * @param instrumentedType The instrumented type.
     * @return A prepared record component registry.
     */
    Compiled compile(TypeDescription instrumentedType);

    /**
     * Represents a compiled record component registry.
     */
    interface Compiled extends TypeWriter.RecordComponentPool {

        /**
         * A no-op record component registry that does not register annotations for any record component.
         */
        enum NoOp implements Compiled {

            /**
             * The singleton instance.
             */
            INSTANCE;

            /**
             * {@inheritDoc}
             */
            public Record target(RecordComponentDescription recordComponentDescription) {
                return new Record.ForImplicitRecordComponent(recordComponentDescription);
            }
        }
    }

    /**
     * An immutable default implementation of a record component registry.
     */
    @HashCodeAndEqualsPlugin.Enhance
    class Default implements RecordComponentRegistry {

        /**
         * This registries entries.
         */
        private final List<Entry> entries;

        /**
         * Creates a new empty default record component registry.
         */
        public Default() {
            this(Collections.<Entry>emptyList());
        }

        /**
         * Creates a new default record component registry.
         *
         * @param entries The entries of the record component registry.
         */
        private Default(List<Entry> entries) {
            this.entries = entries;
        }

        /**
         * {@inheritDoc}
         */
        public RecordComponentRegistry prepend(LatentMatcher<? super RecordComponentDescription> matcher,
                                               RecordComponentAttributeAppender.Factory recordComponentAttributeAppenderFactory,
                                               Transformer<RecordComponentDescription> transformer) {
            List<Entry> entries = new ArrayList<Entry>(this.entries.size() + 1);
            entries.add(new Entry(matcher, recordComponentAttributeAppenderFactory, transformer));
            entries.addAll(this.entries);
            return new Default(entries);
        }

        /**
         * {@inheritDoc}
         */
        public RecordComponentRegistry.Compiled compile(TypeDescription instrumentedType) {
            List<Compiled.Entry> entries = new ArrayList<Compiled.Entry>(this.entries.size());
            Map<RecordComponentAttributeAppender.Factory, RecordComponentAttributeAppender> recordComponentAttributeAppenders = new HashMap<RecordComponentAttributeAppender.Factory, RecordComponentAttributeAppender>();
            for (Entry entry : this.entries) {
                RecordComponentAttributeAppender recordComponentAttributeAppender = recordComponentAttributeAppenders.get(entry.getRecordComponentAttributeAppender());
                if (recordComponentAttributeAppender == null) {
                    recordComponentAttributeAppender = entry.getRecordComponentAttributeAppender().make(instrumentedType);
                    recordComponentAttributeAppenders.put(entry.getRecordComponentAttributeAppender(), recordComponentAttributeAppender);
                }
                entries.add(new Compiled.Entry(entry.resolve(instrumentedType), recordComponentAttributeAppender, entry.getTransformer()));
            }
            return new Compiled(instrumentedType, entries);
        }

        /**
         * An entry of the default record component registry.
         */
        @HashCodeAndEqualsPlugin.Enhance
        protected static class Entry implements LatentMatcher<RecordComponentDescription> {

            /**
             * The matcher to identify any record component that this definition concerns.
             */
            private final LatentMatcher<? super RecordComponentDescription> matcher;

            /**
             * The record component attribute appender factory to apply on any matched record component.
             */
            private final RecordComponentAttributeAppender.Factory recordComponentAttributeAppender;

            /**
             * The record component transformer to apply to any matched record component.
             */
            private final Transformer<RecordComponentDescription> transformer;

            /**
             * Creates a new entry.
             *
             * @param matcher                          The matcher to identify any record component that this definition concerns.
             * @param recordComponentAttributeAppender The record component attribute appender factory to apply on any matched record component.
             * @param transformer                      The record component transformer to apply to any matched record component.
             */
            protected Entry(LatentMatcher<? super RecordComponentDescription> matcher,
                            RecordComponentAttributeAppender.Factory recordComponentAttributeAppender,
                            Transformer<RecordComponentDescription> transformer) {
                this.matcher = matcher;
                this.recordComponentAttributeAppender = recordComponentAttributeAppender;
                this.transformer = transformer;
            }

            /**
             * Returns the record component attribute appender factory to apply on any matched record component.
             *
             * @return The record component attribute appender factory to apply on any matched record component.
             */
            protected RecordComponentAttributeAppender.Factory getRecordComponentAttributeAppender() {
                return recordComponentAttributeAppender;
            }

            /**
             * Returns the record component transformer to apply to any matched record component.
             *
             * @return The record component transformer to apply to any matched record component.
             */
            protected Transformer<RecordComponentDescription> getTransformer() {
                return transformer;
            }

            /**
             * {@inheritDoc}
             */
            public ElementMatcher<? super RecordComponentDescription> resolve(TypeDescription typeDescription) {
                return matcher.resolve(typeDescription);
            }
        }

        /**
         * A compiled default record component registry.
         */
        @HashCodeAndEqualsPlugin.Enhance
        protected static class Compiled implements RecordComponentRegistry.Compiled {

            /**
             * The instrumented type for which this registry was compiled for.
             */
            private final TypeDescription instrumentedType;

            /**
             * The entries of this compiled record component registry.
             */
            private final List<Entry> entries;

            /**
             * Creates a new compiled record component registry.
             *
             * @param instrumentedType The instrumented type for which this registry was compiled for.
             * @param entries          The entries of this compiled record component registry.
             */
            protected Compiled(TypeDescription instrumentedType, List<Entry> entries) {
                this.instrumentedType = instrumentedType;
                this.entries = entries;
            }

            /**
             * {@inheritDoc}
             */
            public Record target(RecordComponentDescription recordComponentDescription) {
                for (Entry entry : entries) {
                    if (entry.matches(recordComponentDescription)) {
                        return entry.bind(instrumentedType, recordComponentDescription);
                    }
                }
                return new Record.ForImplicitRecordComponent(recordComponentDescription);
            }

            /**
             * An entry of a compiled record component registry.
             */
            @HashCodeAndEqualsPlugin.Enhance
            protected static class Entry implements ElementMatcher<RecordComponentDescription> {

                /**
                 * The matcher to identify any record component that this definition concerns.
                 */
                private final ElementMatcher<? super RecordComponentDescription> matcher;

                /**
                 * The record component attribute appender to apply on any matched record component.
                 */
                private final RecordComponentAttributeAppender recordComponentAttributeAppender;

                /**
                 * The record component transformer to apply to any matched record component.
                 */
                private final Transformer<RecordComponentDescription> transformer;

                /**
                 * Creates a new entry.
                 *
                 * @param matcher                          The matcher to identify any record component that this definition concerns.
                 * @param recordComponentAttributeAppender The record component attribute appender to apply on any matched record component.
                 * @param transformer                      The record component transformer to apply to any matched record component.
                 */
                protected Entry(ElementMatcher<? super RecordComponentDescription> matcher,
                                RecordComponentAttributeAppender recordComponentAttributeAppender,
                                Transformer<RecordComponentDescription> transformer) {
                    this.matcher = matcher;
                    this.recordComponentAttributeAppender = recordComponentAttributeAppender;
                    this.transformer = transformer;
                }

                /**
                 * Binds this entry to the provided record component description.
                 *
                 * @param instrumentedType           The instrumented type for which this entry applies.
                 * @param recordComponentDescription The record component description to be bound to this entry.
                 * @return A record representing the binding of this entry to the provided record component.
                 */
                protected Record bind(TypeDescription instrumentedType, RecordComponentDescription recordComponentDescription) {
                    return new Record.ForExplicitRecordComponent(recordComponentAttributeAppender, transformer.transform(instrumentedType, recordComponentDescription));
                }

                /**
                 * {@inheritDoc}
                 */
                public boolean matches(RecordComponentDescription target) {
                    return matcher.matches(target);
                }
            }
        }
    }
}
