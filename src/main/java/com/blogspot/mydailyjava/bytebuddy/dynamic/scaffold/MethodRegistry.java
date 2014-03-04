package com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodList;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatcher;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.InstrumentedType;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;

import java.util.*;

import static com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatchers.describedBy;
import static com.blogspot.mydailyjava.bytebuddy.utility.ByteBuddyCommons.join;

/**
 * A method registry is responsible for
 */
public interface MethodRegistry {

    /**
     * Represents a compiled {@link com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.MethodRegistry}.
     */
    static interface Compiled {

        /**
         * An entry of a compiled method registry.
         */
        static interface Entry {

            /**
             * A skip entry that instructs to ignore a method.
             */
            static enum Skip implements Entry {
                INSTANCE;

                @Override
                public boolean isDefineMethod() {
                    return false;
                }

                @Override
                public ByteCodeAppender getByteCodeAppender() {
                    throw new IllegalStateException();
                }

                @Override
                public MethodAttributeAppender getAttributeAppender() {
                    throw new IllegalStateException();
                }
            }

            /**
             * A default implementation of {@link com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.MethodRegistry.Compiled.Entry}
             * that is not to be ignored but is represented by a tuple of a byte code appender and a method attribute appender.
             */
            static class Default implements Entry {

                private final ByteCodeAppender byteCodeAppender;
                private final MethodAttributeAppender methodAttributeAppender;

                public Default(ByteCodeAppender byteCodeAppender, MethodAttributeAppender methodAttributeAppender) {
                    this.byteCodeAppender = byteCodeAppender;
                    this.methodAttributeAppender = methodAttributeAppender;
                }

                @Override
                public boolean isDefineMethod() {
                    return true;
                }

                @Override
                public ByteCodeAppender getByteCodeAppender() {
                    return byteCodeAppender;
                }

                @Override
                public MethodAttributeAppender getAttributeAppender() {
                    return methodAttributeAppender;
                }

                @Override
                public boolean equals(Object o) {
                    return this == o || !(o == null || getClass() != o.getClass())
                            && byteCodeAppender.equals(((Default) o).byteCodeAppender)
                            && methodAttributeAppender.equals(((Default) o).methodAttributeAppender);
                }

                @Override
                public int hashCode() {
                    return 31 * byteCodeAppender.hashCode() + methodAttributeAppender.hashCode();
                }

                @Override
                public String toString() {
                    return "Default{" +
                            "byteCodeAppender=" + byteCodeAppender +
                            ", methodAttributeAppender=" + methodAttributeAppender +
                            '}';
                }
            }

            /**
             * Determines if this entry requires a method to be defined for a given instrumentation.
             *
             * @return {@code true} if a method should be defined for a given instrumentation.
             */
            boolean isDefineMethod();

            /**
             * The byte code appender to be used for the instrumentation by this entry. Must not
             * be called if {@link com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.MethodRegistry.Compiled.Entry#isDefineMethod()}
             * returns {@code false}.
             *
             * @return The byte code appender that is responsible for the instrumentation of a method matched for
             * this entry.
             */
            ByteCodeAppender getByteCodeAppender();

            /**
             * The method attribute appender that is to be used for the instrumentation by this entry.  Must not
             * be called if {@link com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.MethodRegistry.Compiled.Entry#isDefineMethod()}
             * returns {@code false}.
             *
             * @return The method attribute appender that is responsible for the instrumentation of a method matched for
             * this entry.
             */
            MethodAttributeAppender getAttributeAppender();
        }

        Entry target(MethodDescription methodDescription);

        InstrumentedType getInstrumentedType();
    }

    /**
     * A latent method matcher represents a method matcher that might not yet be assembled because it misses
     * information on the actual instrumented type.
     */
    static interface LatentMethodMatcher {

        /**
         * An wrapper implementation for an already assembled method matcher.
         */
        static class Simple implements LatentMethodMatcher {

            private final MethodMatcher methodMatcher;

            /**
             * Creates a new wrapper.
             *
             * @param methodMatcher The method matcher to be wrapped by this instance.
             */
            public Simple(MethodMatcher methodMatcher) {
                this.methodMatcher = methodMatcher;
            }

            @Override
            public MethodMatcher manifest(TypeDescription instrumentedType) {
                return methodMatcher;
            }

            @Override
            public boolean equals(Object o) {
                return this == o || !(o == null || getClass() != o.getClass())
                        && methodMatcher.equals(((Simple) o).methodMatcher);
            }

            @Override
            public int hashCode() {
                return methodMatcher.hashCode();
            }

            @Override
            public String toString() {
                return "LatentMethodMatcher.Simple{methodMatcher=" + methodMatcher + '}';
            }
        }

        /**
         * Manifests a latent method matcher.
         *
         * @param typeDescription The description of the type that is subject to instrumentation.
         * @return A method matcher that represents the manifested version of this latent method matcher for the
         * given instrumented type description.
         */
        MethodMatcher manifest(TypeDescription typeDescription);
    }

    /**
     * A default implementation of a method registry.
     */
    static class Default implements MethodRegistry {

        private static final int AT_BEGINNING = 0;

        private static class Compiled implements MethodRegistry.Compiled {

            private static class Entry implements MethodRegistry.Compiled.Entry, MethodMatcher {

                private final MethodMatcher methodMatcher;
                private final ByteCodeAppender byteCodeAppender;
                private final MethodAttributeAppender attributeAppender;

                private Entry(MethodMatcher methodMatcher,
                              ByteCodeAppender byteCodeAppender,
                              MethodAttributeAppender attributeAppender) {
                    this.methodMatcher = methodMatcher;
                    this.byteCodeAppender = byteCodeAppender;
                    this.attributeAppender = attributeAppender;
                }

                @Override
                public boolean isDefineMethod() {
                    return true;
                }

                @Override
                public ByteCodeAppender getByteCodeAppender() {
                    return byteCodeAppender;
                }

                @Override
                public MethodAttributeAppender getAttributeAppender() {
                    return attributeAppender;
                }

                @Override
                public boolean matches(MethodDescription methodDescription) {
                    return methodMatcher.matches(methodDescription);
                }
            }

            private final InstrumentedType instrumentedType;
            private final List<Entry> entries;
            private final MethodRegistry.Compiled.Entry fallback;

            private Compiled(InstrumentedType instrumentedType,
                             List<Entry> entries,
                             MethodRegistry.Compiled.Entry fallback) {
                this.instrumentedType = instrumentedType;
                this.entries = entries;
                this.fallback = fallback;
            }

            @Override
            public MethodRegistry.Compiled.Entry target(MethodDescription methodDescription) {
                for (Entry entry : entries) {
                    if (entry.matches(methodDescription)) {
                        return entry;
                    }
                }
                return fallback;
            }

            @Override
            public InstrumentedType getInstrumentedType() {
                return instrumentedType;
            }
        }

        private static class Entry {

            private final LatentMethodMatcher latentMethodMatcher;
            private final Instrumentation instrumentation;
            private final MethodAttributeAppender.Factory attributeAppenderFactory;

            private Entry(LatentMethodMatcher latentMethodMatcher,
                          Instrumentation instrumentation,
                          MethodAttributeAppender.Factory attributeAppenderFactory) {
                this.latentMethodMatcher = latentMethodMatcher;
                this.instrumentation = instrumentation;
                this.attributeAppenderFactory = attributeAppenderFactory;
            }
        }

        private static class ListDifferenceMethodMatcher implements MethodMatcher, LatentMethodMatcher {

            private final MethodList matchedMethods;

            private ListDifferenceMethodMatcher(MethodList beforeMethods, MethodList afterMethods) {
                matchedMethods = afterMethods.subList(beforeMethods.size(), afterMethods.size());
            }

            @Override
            public boolean matches(MethodDescription methodDescription) {
                return matchedMethods.filter(describedBy(methodDescription)).size() == 1;
            }

            @Override
            public MethodMatcher manifest(TypeDescription typeDescription) {
                return this;
            }
        }

        private final List<Entry> entries;

        /**
         * Creates a new empty method registry.
         */
        public Default() {
            entries = Collections.emptyList();
        }

        private Default(List<Entry> entries) {
            this.entries = entries;
        }

        @Override
        public MethodRegistry append(LatentMethodMatcher latentMethodMatcher,
                                     Instrumentation instrumentation,
                                     MethodAttributeAppender.Factory attributeAppenderFactory) {
            return new Default(join(entries, new Entry(latentMethodMatcher, instrumentation, attributeAppenderFactory)));
        }

        @Override
        public MethodRegistry prepend(LatentMethodMatcher latentMethodMatcher,
                                      Instrumentation instrumentation,
                                      MethodAttributeAppender.Factory attributeAppenderFactory) {
            return new Default(join(new Entry(latentMethodMatcher, instrumentation, attributeAppenderFactory), entries));
        }

        @Override
        public MethodRegistry.Compiled compile(InstrumentedType instrumentedType, MethodRegistry.Compiled.Entry fallback) {
            List<Entry> additionalEntries = new LinkedList<Entry>();
            instrumentedType = prepareInstrumentedType(instrumentedType, additionalEntries);
            return new Compiled(instrumentedType, compileEntries(additionalEntries, instrumentedType), fallback);
        }

        private InstrumentedType prepareInstrumentedType(InstrumentedType instrumentedType, List<Entry> additionalEntries) {
            Set<Instrumentation> instrumentations = new HashSet<Instrumentation>(entries.size());
            for (Entry entry : entries) {
                // Only call the preparation method of an instrumentation if the instrumentation was not yet prepared.
                if (instrumentations.add(entry.instrumentation)) {
                    MethodList beforePreparation = instrumentedType.getDeclaredMethods();
                    instrumentedType = entry.instrumentation.prepare(instrumentedType);
                    // If an instrumentation adds methods to the instrumented type, those methods should be
                    // handled by this instrumentation. Thus a
                    if (beforePreparation.size() < instrumentedType.getDeclaredMethods().size()) {
                        additionalEntries.add(new Entry(
                                new ListDifferenceMethodMatcher(beforePreparation, instrumentedType.getDeclaredMethods()),
                                entry.instrumentation,
                                MethodAttributeAppender.NoOp.INSTANCE));
                    }
                }
            }
            return instrumentedType;
        }

        private List<Compiled.Entry> compileEntries(List<Entry> additionalEntries,
                                                    TypeDescription instrumentedType) {
            Map<Instrumentation, ByteCodeAppender> byteCodeAppenders = new HashMap<Instrumentation, ByteCodeAppender>(entries.size());
            List<Compiled.Entry> compiledEntries = new LinkedList<Compiled.Entry>();
            for (Entry entry : entries) {
                // Make sure that the instrumentation's byte code appender was not yet created.
                if (!byteCodeAppenders.containsKey(entry.instrumentation)) {
                    byteCodeAppenders.put(entry.instrumentation, entry.instrumentation.appender(instrumentedType));
                }
                compiledEntries.add(new Compiled.Entry(entry.latentMethodMatcher.manifest(instrumentedType),
                        byteCodeAppenders.get(entry.instrumentation),
                        entry.attributeAppenderFactory.make(instrumentedType)));
            }
            // All additional entries must belong to instrumentations that were already registered. The method
            // matchers must be added at the beginning of the compiled entry queue.
            for (Entry entry : additionalEntries) {
                compiledEntries.add(AT_BEGINNING,
                        new Compiled.Entry(entry.latentMethodMatcher.manifest(instrumentedType),
                                byteCodeAppenders.get(entry.instrumentation),
                                entry.attributeAppenderFactory.make(instrumentedType)));
            }
            return new ArrayList<Compiled.Entry>(compiledEntries);
        }
    }

    /**
     * Creates a new method registry with a new compilable entry representing the arguments of this method call. The new
     * entry will be matched first, i.e. other registered entries will attempted to be matched <b>before</b> the new entry.
     *
     * @param latentMethodMatcher      A latent method matcher that represents this method matching.
     * @param instrumentation          The instrumentation that is responsible for implementing this method.
     * @param attributeAppenderFactory The attribute appender factory that is responsible for implementing this method.
     * @return A new method registry with the new compilable entry prepended.
     */
    MethodRegistry prepend(LatentMethodMatcher latentMethodMatcher,
                           Instrumentation instrumentation,
                           MethodAttributeAppender.Factory attributeAppenderFactory);

    /**
     * Creates a new method registry with a new compilable entry representing the arguments of this method call. The new
     * entry will be matched last, i.e. other registered entries will attempted to be matched <b>after</b> the new entry.
     *
     * @param latentMethodMatcher      A latent method matcher that represents this method matching.
     * @param instrumentation          The instrumentation that is responsible for implementing this method.
     * @param attributeAppenderFactory The attribute appender factory that is responsible for implementing this method.
     * @return A new method registry with the new compilable entry appended.
     */
    MethodRegistry append(LatentMethodMatcher latentMethodMatcher,
                          Instrumentation instrumentation,
                          MethodAttributeAppender.Factory attributeAppenderFactory);

    /**
     * Once all entries for a method registry were registered, a method registry can be compiled in order to allow the
     * retrieval of {@link com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold.MethodRegistry.Compiled.Entry}s for
     * known method. Additionally, a fallback entry is to be supplied which is returned if a requested
     * method is not known to the compiled method registry.
     *
     * @param instrumentedType The instrumented type for which this field registry is to be compiled.
     * @param fallback         The fallback field attribute appender factory that serves as a fallback for unknown methods.
     * @return A compiled method registry representing the methods that were registered with this method registry.
     */
    Compiled compile(InstrumentedType instrumentedType, Compiled.Entry fallback);
}
