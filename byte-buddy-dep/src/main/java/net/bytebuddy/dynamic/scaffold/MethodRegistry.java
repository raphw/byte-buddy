package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.TypeInitializer;
import net.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodList;
import net.bytebuddy.instrumentation.method.MethodLookupEngine;
import net.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import net.bytebuddy.instrumentation.method.matcher.MethodMatcher;
import net.bytebuddy.instrumentation.type.InstrumentedType;
import net.bytebuddy.instrumentation.type.TypeDescription;

import java.util.*;

import static net.bytebuddy.instrumentation.method.matcher.MethodMatchers.is;
import static net.bytebuddy.utility.ByteBuddyCommons.join;

/**
 * A method registry is responsible for storing information on how a method is intercepted.
 */
public interface MethodRegistry {

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
     * retrieval of a compiled entry for a known method. Additionally, a fallback entry is to be supplied which is
     * returned if a requested method is not known to the compiled method registry.
     *
     * @param instrumentedType             The instrumented type for which this field registry is to be compiled.
     * @param methodLookupEngine           The method lookup engine to be used for analyzing the instrumented type.
     * @param instrumentationTargetFactory A factory for creating an {@link net.bytebuddy.instrumentation.Instrumentation.Target}.
     * @param fallback                     The fallback field attribute appender factory that serves as a fallback for unknown methods.
     * @return A compiled method registry representing the methods that were registered with this method registry.
     */
    Compiled compile(InstrumentedType instrumentedType,
                     MethodLookupEngine methodLookupEngine,
                     Instrumentation.Target.Factory instrumentationTargetFactory,
                     TypeWriter.MethodPool.Entry fallback);

    /**
     * Represents a compiled {@link net.bytebuddy.dynamic.scaffold.MethodRegistry}.
     */
    static interface Compiled extends TypeWriter.MethodPool {

        MethodLookupEngine.Finding getFinding();

        TypeInitializer getTypeInitializer();
    }

    /**
     * A latent method matcher represents a method matcher that might not yet be assembled because it misses
     * information on the actual instrumented type.
     */
    static interface LatentMethodMatcher {

        /**
         * Manifests a latent method matcher.
         *
         * @param typeDescription The description of the type that is subject to instrumentation.
         * @return A method matcher that represents the manifested version of this latent method matcher for the
         * given instrumented type description.
         */
        MethodMatcher manifest(TypeDescription typeDescription);

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
                return "MethodRegistry.LatentMethodMatcher.Simple{methodMatcher=" + methodMatcher + '}';
            }
        }
    }

    /**
     * A default implementation of a method registry.
     */
    static class Default implements MethodRegistry {

        private static final int AT_BEGINNING = 0;
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
        public MethodRegistry.Compiled compile(InstrumentedType instrumentedType,
                                               MethodLookupEngine methodLookupEngine,
                                               Instrumentation.Target.Factory instrumentationTargetFactory,
                                               MethodRegistry.Compiled.Entry fallback) {
            List<Entry> additionalEntries = new LinkedList<Entry>();
            instrumentedType = prepareInstrumentedType(instrumentedType, additionalEntries);
            MethodLookupEngine.Finding finding = methodLookupEngine.process(instrumentedType.detach());
            return new Compiled(finding,
                    instrumentedType.getTypeInitializer(),
                    compileEntries(additionalEntries, instrumentationTargetFactory.make(finding)),
                    fallback);
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
                                                    Instrumentation.Target instrumentationTarget) {
            Map<Instrumentation, ByteCodeAppender> byteCodeAppenders = new HashMap<Instrumentation, ByteCodeAppender>(entries.size());
            List<Compiled.Entry> compiledEntries = new LinkedList<Compiled.Entry>();
            for (Entry entry : entries) {
                // Make sure that the instrumentation's byte code appender was not yet created.
                if (!byteCodeAppenders.containsKey(entry.instrumentation)) {
                    byteCodeAppenders.put(entry.instrumentation, entry.instrumentation.appender(instrumentationTarget));
                }
                compiledEntries.add(new Compiled.Entry(entry.latentMethodMatcher.manifest(instrumentationTarget.getTypeDescription()),
                        byteCodeAppenders.get(entry.instrumentation),
                        entry.attributeAppenderFactory.make(instrumentationTarget.getTypeDescription())));
            }
            // All additional entries must belong to instrumentations that were already registered. The method
            // matchers must be added at the beginning of the compiled entry queue.
            for (Entry entry : additionalEntries) {
                compiledEntries.add(AT_BEGINNING,
                        new Compiled.Entry(entry.latentMethodMatcher.manifest(instrumentationTarget.getTypeDescription()),
                                byteCodeAppenders.get(entry.instrumentation),
                                entry.attributeAppenderFactory.make(instrumentationTarget.getTypeDescription()))
                );
            }
            return new ArrayList<Compiled.Entry>(compiledEntries);
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
            return "MethodRegistry.Default{entries=" + entries + '}';
        }

        private static class Compiled implements MethodRegistry.Compiled {

            private final MethodLookupEngine.Finding finding;
            private final TypeInitializer typeInitializer;
            private final List<Entry> entries;
            private final MethodRegistry.Compiled.Entry fallback;

            private Compiled(MethodLookupEngine.Finding finding,
                             TypeInitializer typeInitializer,
                             List<Entry> entries,
                             MethodRegistry.Compiled.Entry fallback) {
                this.finding = finding;
                this.typeInitializer = typeInitializer;
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
            public MethodLookupEngine.Finding getFinding() {
                return finding;
            }

            @Override
            public TypeInitializer getTypeInitializer() {
                return typeInitializer;
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                Compiled compiled = (Compiled) other;
                return entries.equals(compiled.entries)
                        && fallback.equals(compiled.fallback)
                        && finding.equals(compiled.finding)
                        && typeInitializer.equals(compiled.typeInitializer);
            }

            @Override
            public int hashCode() {
                int result = finding.hashCode();
                result = 31 * result + typeInitializer.hashCode();
                result = 31 * result + entries.hashCode();
                result = 31 * result + fallback.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "MethodRegistry.Default.Compiled{" +
                        "finding=" + finding +
                        ", typeInitializer=" + typeInitializer +
                        ", entries=" + entries +
                        ", fallback=" + fallback +
                        '}';
            }

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

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (other == null || getClass() != other.getClass()) return false;
                    Entry entry = (Entry) other;
                    return attributeAppender.equals(entry.attributeAppender)
                            && byteCodeAppender.equals(entry.byteCodeAppender)
                            && methodMatcher.equals(entry.methodMatcher);
                }

                @Override
                public int hashCode() {
                    int result = methodMatcher.hashCode();
                    result = 31 * result + byteCodeAppender.hashCode();
                    result = 31 * result + attributeAppender.hashCode();
                    return result;
                }

                @Override
                public String toString() {
                    return "MethodRegistry.Default.Compiled.Entry{" +
                            "methodMatcher=" + methodMatcher +
                            ", byteCodeAppender=" + byteCodeAppender +
                            ", attributeAppender=" + attributeAppender +
                            '}';
                }
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

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                Entry entry = (Entry) other;
                return attributeAppenderFactory.equals(entry.attributeAppenderFactory)
                        && instrumentation.equals(entry.instrumentation)
                        && latentMethodMatcher.equals(entry.latentMethodMatcher);
            }

            @Override
            public int hashCode() {
                int result = latentMethodMatcher.hashCode();
                result = 31 * result + instrumentation.hashCode();
                result = 31 * result + attributeAppenderFactory.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "MethodRegistry.Default.Entry{" +
                        "latentMethodMatcher=" + latentMethodMatcher +
                        ", instrumentation=" + instrumentation +
                        ", attributeAppenderFactory=" + attributeAppenderFactory +
                        '}';
            }
        }

        private static class ListDifferenceMethodMatcher implements MethodMatcher, LatentMethodMatcher {

            private final MethodList matchedMethods;

            private ListDifferenceMethodMatcher(MethodList beforeMethods, MethodList afterMethods) {
                matchedMethods = afterMethods.subList(beforeMethods.size(), afterMethods.size());
            }

            @Override
            public boolean matches(MethodDescription methodDescription) {
                return matchedMethods.filter(is(methodDescription)).size() == 1;
            }

            @Override
            public MethodMatcher manifest(TypeDescription typeDescription) {
                return this;
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && matchedMethods.equals(((ListDifferenceMethodMatcher) other).matchedMethods);
            }

            @Override
            public int hashCode() {
                return matchedMethods.hashCode();
            }

            @Override
            public String toString() {
                return "oneOf(" + matchedMethods + ')';
            }
        }
    }
}
