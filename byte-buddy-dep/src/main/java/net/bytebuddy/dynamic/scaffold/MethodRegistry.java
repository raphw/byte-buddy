package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.LoadedTypeInitializer;
import net.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodList;
import net.bytebuddy.instrumentation.method.MethodLookupEngine;
import net.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import net.bytebuddy.instrumentation.type.InstrumentedType;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import java.util.*;

import static net.bytebuddy.matcher.ElementMatchers.is;
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
     * Prepares this method registry for a given instrumented type.
     *
     * @param instrumentedType The instrumented type that is to be prepared.
     * @return A prepared method registry.
     */
    Prepared prepare(InstrumentedType instrumentedType);

    /**
     * A {@link net.bytebuddy.dynamic.scaffold.MethodRegistry} that was prepared for a given
     * {@link net.bytebuddy.instrumentation.type.InstrumentedType}.
     */
    interface Prepared {

        /**
         * The readily prepared instrumented type with all optional members registered as they are required
         * by this instances {@link net.bytebuddy.instrumentation.Instrumentation}s.
         *
         * @return The final instrumented type.
         */
        TypeDescription getInstrumentedType();

        /**
         * The type initializer as it is required by this instance's
         * {@link net.bytebuddy.instrumentation.Instrumentation}s.
         *
         * @return The final loaded type initializer.
         */
        LoadedTypeInitializer getLoadedTypeInitializer();

        /**
         * Compiles this prepared method registry.
         *
         * @param instrumentationTargetFactory The instrumentation target factory to use for compilation.
         * @param methodLookupEngine           The method lookup engine to use for compilation.
         * @param fallback                     The fallback entry to use.
         * @return A compiled method registry.
         */
        Compiled compile(Instrumentation.Target.Factory instrumentationTargetFactory,
                         MethodLookupEngine methodLookupEngine,
                         TypeWriter.MethodPool.Entry.Factory fallback);
    }

    /**
     * Represents a compiled {@link net.bytebuddy.dynamic.scaffold.MethodRegistry}.
     */
    interface Compiled extends TypeWriter.MethodPool {

        /**
         * The readily prepared instrumented type with all optional members registered as they are required
         * by this instances {@link net.bytebuddy.instrumentation.Instrumentation}s.
         *
         * @return The final instrumented type.
         */
        TypeDescription getInstrumentedType();

        /**
         * The loaded type initializer as it is required by this instance's
         * {@link net.bytebuddy.instrumentation.Instrumentation}s.
         *
         * @return The final loaded type initializer.
         */
        LoadedTypeInitializer getLoadedTypeInitializer();

        /**
         * The type initializer as it is required by this instance's
         * {@link net.bytebuddy.instrumentation.Instrumentation}s.
         *
         * @return The final type initializer.
         */
        InstrumentedType.TypeInitializer getTypeInitializer();

        /**
         * Returns a list of all methods that are invokable on the instrumented type.
         *
         * @return A list of all methods that are invokable on the instrumented type.
         */
        MethodList getInvokableMethods();
    }

    /**
     * A latent method matcher represents a method matcher that might not yet be assembled because it misses
     * information on the actual instrumented type.
     */
    interface LatentMethodMatcher {

        /**
         * Manifests a latent method matcher.
         *
         * @param typeDescription The description of the type that is subject to instrumentation.
         * @return A method matcher that represents the manifested version of this latent method matcher for the
         * given instrumented type description.
         */
        ElementMatcher<? super MethodDescription> manifest(TypeDescription typeDescription);

        /**
         * An wrapper implementation for an already assembled method matcher.
         */
        class Simple implements LatentMethodMatcher {

            /**
             * The method matcher that is represented by this instance.
             */
            private final ElementMatcher<? super MethodDescription> methodMatcher;

            /**
             * Creates a new wrapper.
             *
             * @param methodMatcher The method matcher to be wrapped by this instance.
             */
            public Simple(ElementMatcher<? super MethodDescription> methodMatcher) {
                this.methodMatcher = methodMatcher;
            }

            @Override
            public ElementMatcher<? super MethodDescription> manifest(TypeDescription instrumentedType) {
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
    class Default implements MethodRegistry {

        /**
         * A list of all entries in their registration order.
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
        public MethodRegistry prepend(LatentMethodMatcher latentMethodMatcher,
                                      Instrumentation instrumentation,
                                      MethodAttributeAppender.Factory attributeAppenderFactory) {
            return new Default(join(new Entry(latentMethodMatcher, instrumentation, attributeAppenderFactory), entries));
        }

        @Override
        public MethodRegistry append(LatentMethodMatcher latentMethodMatcher,
                                     Instrumentation instrumentation,
                                     MethodAttributeAppender.Factory attributeAppenderFactory) {
            return new Default(join(entries, new Entry(latentMethodMatcher, instrumentation, attributeAppenderFactory)));
        }

        @Override
        public MethodRegistry.Prepared prepare(InstrumentedType instrumentedType) {
            List<Entry> additionalEntries = new LinkedList<Entry>();
            Set<Instrumentation> instrumentations = new HashSet<Instrumentation>(entries.size());
            for (Entry entry : entries) {
                // Only call the preparation method of an instrumentation if the instrumentation was not yet prepared.
                if (instrumentations.add(entry.getInstrumentation())) {
                    MethodList beforePreparation = instrumentedType.getDeclaredMethods();
                    instrumentedType = entry.getInstrumentation().prepare(instrumentedType);
                    // If an instrumentation adds methods to the instrumented type, those methods should be
                    // handled by this instrumentation. Thus an additional matcher that matches these exact methods
                    // is registered, in case that the instrumentation actually added methods. These matcher must be
                    // prepended to any other entry such that they become of higher precedence to manually registered
                    // method interceptions. Otherwise, those user interceptions could match the methods that were
                    // added by the instrumentation.
                    if (beforePreparation.size() < instrumentedType.getDeclaredMethods().size()) {
                        additionalEntries.add(new Entry(
                                new ListDifferenceMethodMatcher(beforePreparation, instrumentedType.getDeclaredMethods()),
                                entry.getInstrumentation(),
                                MethodAttributeAppender.NoOp.INSTANCE));
                    }
                }
            }
            return new Prepared(instrumentedType.detach(),
                    instrumentedType.getLoadedTypeInitializer(),
                    instrumentedType.getTypeInitializer(),
                    entries,
                    additionalEntries);
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
         * A prepared default method registry.
         */
        protected static class Prepared implements MethodRegistry.Prepared {

            /**
             * A convenience index pointing to the first element of an array to improve the readability of the code.
             */
            private static final int AT_BEGINNING = 0;

            /**
             * The instrumented type this method registry was prepared for.
             */
            private final TypeDescription instrumentedType;

            /**
             * The loaded type initializer this method registry was prepared for.
             */
            private final LoadedTypeInitializer loadedTypeInitializer;

            /**
             * The type initializer this method registry was prepared for.
             */
            private final InstrumentedType.TypeInitializer typeInitializer;

            /**
             * All entries in their application order.
             */
            private final List<Entry> entries;

            /**
             * All additional entries that were added during preparation.
             */
            private final List<Entry> additionalEntries;

            /**
             * Creates a new prepared default method registry.
             *
             * @param instrumentedType      The instrumented type this method registry was prepared for.
             * @param loadedTypeInitializer The loaded type initializer this method registry was prepared for.
             * @param typeInitializer       The type initializer this method registry was prepared for.
             * @param entries               All entries in their application order.
             * @param additionalEntries     All additional entries that were added during preparation.
             */
            protected Prepared(TypeDescription instrumentedType,
                               LoadedTypeInitializer loadedTypeInitializer,
                               InstrumentedType.TypeInitializer typeInitializer,
                               List<Entry> entries,
                               List<Entry> additionalEntries) {
                this.instrumentedType = instrumentedType;
                this.loadedTypeInitializer = loadedTypeInitializer;
                this.typeInitializer = typeInitializer;
                this.entries = entries;
                this.additionalEntries = additionalEntries;
            }

            @Override
            public TypeDescription getInstrumentedType() {
                return instrumentedType;
            }

            @Override
            public LoadedTypeInitializer getLoadedTypeInitializer() {
                return loadedTypeInitializer;
            }

            @Override
            public MethodRegistry.Compiled compile(Instrumentation.Target.Factory instrumentationTargetFactory,
                                                   MethodLookupEngine methodLookupEngine,
                                                   TypeWriter.MethodPool.Entry.Factory fallback) {
                MethodLookupEngine.Finding finding = methodLookupEngine.process(instrumentedType);
                Instrumentation.Target instrumentationTarget = instrumentationTargetFactory.make(finding);
                Map<Instrumentation, ByteCodeAppender> byteCodeAppenders = new HashMap<Instrumentation, ByteCodeAppender>(entries.size());
                List<Compiled.Entry> compiledEntries = new LinkedList<Compiled.Entry>();
                for (Entry entry : entries) {
                    // Make sure that the instrumentation's byte code appender was not yet created.
                    if (!byteCodeAppenders.containsKey(entry.getInstrumentation())) {
                        byteCodeAppenders.put(entry.getInstrumentation(), entry.getInstrumentation().appender(instrumentationTarget));
                    }
                    compiledEntries.add(new Compiled.Entry(entry.getLatentMethodMatcher().manifest(instrumentationTarget.getTypeDescription()),
                            byteCodeAppenders.get(entry.getInstrumentation()),
                            entry.getAttributeAppenderFactory().make(instrumentationTarget.getTypeDescription())));
                }
                // All additional entries must belong to instrumentations that were already registered. The method
                // matchers must be added at the beginning of the compiled entry queue.
                for (Entry entry : additionalEntries) {
                    compiledEntries.add(AT_BEGINNING,
                            new Compiled.Entry(entry.getLatentMethodMatcher().manifest(instrumentationTarget.getTypeDescription()),
                                    byteCodeAppenders.get(entry.getInstrumentation()),
                                    entry.getAttributeAppenderFactory().make(instrumentationTarget.getTypeDescription()))
                    );
                }
                return new Compiled(instrumentedType,
                        loadedTypeInitializer,
                        typeInitializer,
                        finding.getInvokableMethods(),
                        new ArrayList<Compiled.Entry>(compiledEntries),
                        fallback.compile(instrumentationTarget));
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                Prepared prepared = (Prepared) other;
                return additionalEntries.equals(prepared.additionalEntries)
                        && entries.equals(prepared.entries)
                        && instrumentedType.equals(prepared.instrumentedType)
                        && loadedTypeInitializer.equals(prepared.loadedTypeInitializer)
                        && typeInitializer.equals(prepared.typeInitializer);
            }

            @Override
            public int hashCode() {
                int result = instrumentedType.hashCode();
                result = 31 * result + loadedTypeInitializer.hashCode();
                result = 31 * result + typeInitializer.hashCode();
                result = 31 * result + entries.hashCode();
                result = 31 * result + additionalEntries.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "MethodRegistry.Default.Prepared{" +
                        "instrumentedType=" + instrumentedType +
                        ", loadedTypeInitializer=" + loadedTypeInitializer +
                        ", typeInitializer=" + typeInitializer +
                        ", entries=" + entries +
                        ", additionalEntries=" + additionalEntries +
                        '}';
            }
        }

        /**
         * A compiled default method registry.
         */
        protected static class Compiled implements MethodRegistry.Compiled {

            /**
             * The instrumented type.
             */
            private final TypeDescription instrumentedType;

            /**
             * The loaded type initializer.
             */
            private final LoadedTypeInitializer loadedTypeInitializer;

            /**
             * The type initializer.
             */
            private final InstrumentedType.TypeInitializer typeInitializer;

            /**
             * A list of all methods that can be invoked on the instrumented type.
             */
            private final MethodList invokableMethods;

            /**
             * The list of all compiled entries of this compiled method registry.
             */
            private final List<Entry> entries;

            /**
             * The fallback entry to apply for any method that is not matched by any of the registered compiled entries.
             */
            private final MethodRegistry.Compiled.Entry fallback;

            /**
             * Creates a new compiled default method registry.
             *
             * @param instrumentedType      The instrumented type.
             * @param loadedTypeInitializer The loaded type initializer.
             * @param typeInitializer       The type initializer.
             * @param invokableMethods      A list of all methods that can be invoked on the instrumented type.
             * @param entries               The list of all compiled entries of this compiled method registry.
             * @param fallback              The fallback entry to apply for any method that is not matched by any of
             *                              the registered compiled entries.
             */
            protected Compiled(TypeDescription instrumentedType,
                               LoadedTypeInitializer loadedTypeInitializer,
                               InstrumentedType.TypeInitializer typeInitializer,
                               MethodList invokableMethods,
                               List<Entry> entries,
                               MethodRegistry.Compiled.Entry fallback) {
                this.instrumentedType = instrumentedType;
                this.loadedTypeInitializer = loadedTypeInitializer;
                this.typeInitializer = typeInitializer;
                this.invokableMethods = invokableMethods;
                this.entries = entries;
                this.fallback = fallback;
            }

            @Override
            public TypeDescription getInstrumentedType() {
                return instrumentedType;
            }

            @Override
            public LoadedTypeInitializer getLoadedTypeInitializer() {
                return loadedTypeInitializer;
            }

            @Override
            public InstrumentedType.TypeInitializer getTypeInitializer() {
                return typeInitializer;
            }

            @Override
            public MethodList getInvokableMethods() {
                return invokableMethods;
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
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                Compiled compiled = (Compiled) other;
                return entries.equals(compiled.entries)
                        && fallback.equals(compiled.fallback)
                        && instrumentedType.equals(compiled.instrumentedType)
                        && invokableMethods.equals(compiled.invokableMethods)
                        && loadedTypeInitializer.equals(compiled.loadedTypeInitializer)
                        && typeInitializer.equals(compiled.typeInitializer);
            }

            @Override
            public int hashCode() {
                int result = instrumentedType.hashCode();
                result = 31 * result + loadedTypeInitializer.hashCode();
                result = 31 * result + typeInitializer.hashCode();
                result = 31 * result + invokableMethods.hashCode();
                result = 31 * result + entries.hashCode();
                result = 31 * result + fallback.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "MethodRegistry.Default.Compiled{" +
                        "instrumentedType=" + instrumentedType +
                        ", loadedTypeInitializer=" + loadedTypeInitializer +
                        ", typeInitializer=" + typeInitializer +
                        ", invokableMethods=" + invokableMethods +
                        ", entries=" + entries +
                        ", fallback=" + fallback +
                        '}';
            }

            /**
             * An entry of a compiled default method registry.
             */
            protected static class Entry extends TypeWriter.MethodPool.Entry.Simple implements ElementMatcher<MethodDescription> {

                /**
                 * The method matcher that represents this compiled entry.
                 */
                private final ElementMatcher<? super MethodDescription> methodMatcher;

                /**
                 * Creates an entry of a compiled default method registry.
                 *
                 * @param methodMatcher     The method matcher to be wrapped by this instance.
                 * @param byteCodeAppender  The byte code appender that represents this compiled entry.
                 * @param attributeAppender The method attribute appender that represents this compiled entry.
                 */
                protected Entry(ElementMatcher<? super MethodDescription> methodMatcher,
                                ByteCodeAppender byteCodeAppender,
                                MethodAttributeAppender attributeAppender) {
                    super(byteCodeAppender, attributeAppender);
                    this.methodMatcher = methodMatcher;
                }

                @Override
                public boolean matches(MethodDescription methodDescription) {
                    return methodMatcher.matches(methodDescription);
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && super.equals(other)
                            && methodMatcher.equals(((Entry) other).methodMatcher);
                }

                @Override
                public int hashCode() {
                    return 31 * super.hashCode() + methodMatcher.hashCode();
                }

                @Override
                public String toString() {
                    return "MethodRegistry.Default.Compiled.Entry{" +
                            "methodMatcher=" + methodMatcher +
                            ", byteCodeAppender=" + getByteCodeAppender() +
                            ", attributeAppender=" + getAttributeAppender() +
                            '}';
                }
            }
        }

        /**
         * A method matcher that matches methods that are found in only one of two lists.
         */
        protected static class ListDifferenceMethodMatcher implements ElementMatcher<MethodDescription>, LatentMethodMatcher {

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
            protected ListDifferenceMethodMatcher(MethodList beforeMethods, MethodList afterMethods) {
                matchedMethods = afterMethods.subList(beforeMethods.size(), afterMethods.size());
            }

            @Override
            public boolean matches(MethodDescription methodDescription) {
                return matchedMethods.filter(is(methodDescription)).size() == 1;
            }

            @Override
            public ElementMatcher<? super MethodDescription> manifest(TypeDescription typeDescription) {
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

        /**
         * A registration within a method registry, consisting of a latent method matcher, an instrumentation that
         * is to be applied on any method that is matched by the method matcher that is extracted from the latent
         * matcher's manifestation and a method attribute appender factory that is applied to any intercepted method.
         */
        protected static class Entry {

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
            protected Entry(LatentMethodMatcher latentMethodMatcher,
                            Instrumentation instrumentation,
                            MethodAttributeAppender.Factory attributeAppenderFactory) {
                this.latentMethodMatcher = latentMethodMatcher;
                this.instrumentation = instrumentation;
                this.attributeAppenderFactory = attributeAppenderFactory;
            }

            /**
             * Returns this entry's latent method matcher.
             *
             * @return This entry's latent method matcher.
             */
            public LatentMethodMatcher getLatentMethodMatcher() {
                return latentMethodMatcher;
            }

            /**
             * Returns this entry's instrumentation.
             *
             * @return This entry's instrumentation.
             */
            public Instrumentation getInstrumentation() {
                return instrumentation;
            }

            /**
             * Returns this entry's attribute appender factory.
             *
             * @return This entry's attribute appender factory.
             */
            public MethodAttributeAppender.Factory getAttributeAppenderFactory() {
                return attributeAppenderFactory;
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
    }
}
