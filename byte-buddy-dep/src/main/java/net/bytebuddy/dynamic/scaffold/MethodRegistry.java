package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.LoadedTypeInitializer;
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

        /**
         * Returns the result of a method lookup on the fully prepared instrumented type. The lookup is performed
         * on the fully preferred type after any {@link net.bytebuddy.instrumentation.Instrumentation} was able
         * to register any methods.
         *
         * @return The result of a method lookup
         */
        MethodLookupEngine.Finding getFinding();

        /**
         * Returns the {@link net.bytebuddy.instrumentation.LoadedTypeInitializer} of the instrumented type that is currently
         * under construction. This type initializer is only extracted after any
         * {@link net.bytebuddy.instrumentation.Instrumentation} could register their own type initializers.
         *
         * @return The type initializer of the currently created instrumented type.
         */
        LoadedTypeInitializer getLoadedTypeInitializer();
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

            /**
             * The method matcher that is represented by this instance.
             */
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
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && methodMatcher.equals(((Simple) other).methodMatcher);
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

        /**
         * A pointer for the first index of a list.
         */
        private static final int AT_BEGINNING = 0;

        /**
         * The entries of method instrumentations that were registered for creating this instance.
         */
        private final List<Entry> entries;

        /**
         * Creates a new empty method registry.
         */
        public Default() {
            entries = Collections.emptyList();
        }

        /**
         * Creates a new default {@link net.bytebuddy.dynamic.scaffold.MethodRegistry} with a given list of
         * registered entries.
         *
         * @param entries The entries of this method registry.
         */
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
                    instrumentedType.getLoadedTypeInitializer(),
                    compileEntries(additionalEntries, instrumentationTargetFactory.make(finding)),
                    fallback);
        }

        /**
         * Prepares an instrumented type in the course of compilation.
         *
         * @param instrumentedType  The instrumented type for which this field registry is to be compiled.
         * @param additionalEntries A set of additional entries that are not registered explicitly by a user but
         *                          intend to capture synthetic method matchers that are meant to intercept methods
         *                          that were registered by an {@link net.bytebuddy.instrumentation.Instrumentation}.
         * @return The fully prepared instrumented type.
         */
        private InstrumentedType prepareInstrumentedType(InstrumentedType instrumentedType, List<Entry> additionalEntries) {
            Set<Instrumentation> instrumentations = new HashSet<Instrumentation>(entries.size());
            for (Entry entry : entries) {
                // Only call the preparation method of an instrumentation if the instrumentation was not yet prepared.
                if (instrumentations.add(entry.instrumentation)) {
                    MethodList beforePreparation = instrumentedType.getDeclaredMethods();
                    instrumentedType = entry.instrumentation.prepare(instrumentedType);
                    // If an instrumentation adds methods to the instrumented type, those methods should be
                    // handled by this instrumentation. Thus an additional matcher that matches these exact methods
                    // is registered, in case that the instrumentation actually added methods. These matcher must be
                    // prepended to any other entry such that they become of higher precedence to manually registered
                    // method interceptions. Otherwise, those user interceptions could match the methods that were
                    // added by the instrumentation.
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

        /**
         * Compiles all entries of this method registry.
         *
         * @param additionalEntries     A set of additional entries that are not registered explicitly by a user but
         *                              intend to capture synthetic method matchers that are meant to intercept methods
         *                              that were registered by an {@link net.bytebuddy.instrumentation.Instrumentation}.
         * @param instrumentationTarget The target of the instrumentation this method registry is compiled for.
         * @return A list of the compiled entries of this instance in the same order as they are given where the
         * additional entries are prepended to the list.
         */
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

        /**
         * A compiled default method registry.
         */
        private static class Compiled implements MethodRegistry.Compiled {

            /**
             * The finding of a method lookup engine that was applied on the fully prepared instrumented type
             * this method registry was compiled for.
             */
            private final MethodLookupEngine.Finding finding;

            /**
             * The type initializer of the fully prepared instrumented type this compiled method registry represents.
             */
            private final LoadedTypeInitializer loadedTypeInitializer;

            /**
             * The list of all compiled entries of this compiled method registry.
             */
            private final List<Entry> entries;

            /**
             * The fallback entry to apply for any method that is not matched by any of the registered compiled entries.
             */
            private final MethodRegistry.Compiled.Entry fallback;

            /**
             * Creates a compiled default method registry.
             *
             * @param finding         The finding of a method lookup engine that was applied on the fully prepared
             *                        instrumented type this method registry was compiled for.
             * @param loadedTypeInitializer The type initializer of the fully prepared instrumented type this compiled method
             *                        registry represents.
             * @param entries         The list of all compiled entries of this compiled method registry.
             * @param fallback        The fallback entry to apply for any method that is not matched by any of the
             *                        registered compiled entries.
             */
            private Compiled(MethodLookupEngine.Finding finding,
                             LoadedTypeInitializer loadedTypeInitializer,
                             List<Entry> entries,
                             MethodRegistry.Compiled.Entry fallback) {
                this.finding = finding;
                this.loadedTypeInitializer = loadedTypeInitializer;
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
            public LoadedTypeInitializer getLoadedTypeInitializer() {
                return loadedTypeInitializer;
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                Compiled compiled = (Compiled) other;
                return entries.equals(compiled.entries)
                        && fallback.equals(compiled.fallback)
                        && finding.equals(compiled.finding)
                        && loadedTypeInitializer.equals(compiled.loadedTypeInitializer);
            }

            @Override
            public int hashCode() {
                int result = finding.hashCode();
                result = 31 * result + loadedTypeInitializer.hashCode();
                result = 31 * result + entries.hashCode();
                result = 31 * result + fallback.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "MethodRegistry.Default.Compiled{" +
                        "finding=" + finding +
                        ", typeInitializer=" + loadedTypeInitializer +
                        ", entries=" + entries +
                        ", fallback=" + fallback +
                        '}';
            }

            /**
             * An entry of a compiled default method registry.
             */
            private static class Entry implements MethodRegistry.Compiled.Entry, MethodMatcher {

                /**
                 * The method matcher that represents this compiled entry.
                 */
                private final MethodMatcher methodMatcher;

                /**
                 * The byte code appender that represents this compiled entry.
                 */
                private final ByteCodeAppender byteCodeAppender;

                /**
                 * The method attribute appender that represents this compiled entry.
                 */
                private final MethodAttributeAppender attributeAppender;

                /**
                 * Creates an entry of a compiled default method registry.
                 *
                 * @param methodMatcher     The method matcher to be wrapped by this instance.
                 * @param byteCodeAppender  The byte code appender that represents this compiled entry.
                 * @param attributeAppender The method attribute appender that represents this compiled entry.
                 */
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

        /**
         * A registration within a method registry, consisting of a latent method matcher, an instrumentation that
         * is to be applied on any method that is matched by the method matcher that is extracted from the latent
         * matcher's manifestation and a method attribute appender factory that is applied to any intercepted method.
         */
        private static class Entry {

            /**
             * The latent method matcher that is representing this entry.
             */
            private final LatentMethodMatcher latentMethodMatcher;

            /**
             * The instrumentation that is representing this entry.
             */
            private final Instrumentation instrumentation;

            /**
             * The method attribute appender factory that is representing this entry.
             */
            private final MethodAttributeAppender.Factory attributeAppenderFactory;

            /**
             * Creates a new entry.
             *
             * @param latentMethodMatcher      A latent method matcher that represents this method matching.
             * @param instrumentation          The instrumentation that is responsible for implementing this method.
             * @param attributeAppenderFactory The attribute appender factory that is responsible for implementing
             *                                 this method.
             */
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

        /**
         * A method matcher that matches methods that are found in only one of two lists.
         */
        private static class ListDifferenceMethodMatcher implements MethodMatcher, LatentMethodMatcher {

            /**
             * The methods that are matched by this instance.
             */
            private final MethodList matchedMethods;

            /**
             * Creates a new list difference method matcher.
             *
             * @param beforeMethods A list of methods that should not be matched.
             * @param afterMethods  The same list after adding additional methods. The order of the methods in
             *                      this list must not be altered.
             */
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
