package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.instrumentation.Instrumentation;
import net.bytebuddy.instrumentation.LoadedTypeInitializer;
import net.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import net.bytebuddy.instrumentation.method.MethodDescription;
import net.bytebuddy.instrumentation.method.MethodList;
import net.bytebuddy.instrumentation.method.MethodLookupEngine;
import net.bytebuddy.instrumentation.type.InstrumentedType;
import net.bytebuddy.instrumentation.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import java.util.*;

import static net.bytebuddy.matcher.ElementMatchers.*;
import static net.bytebuddy.utility.ByteBuddyCommons.join;

/**
 * A method registry is responsible for storing information on how a method is intercepted.
 */
public interface MethodRegistry {

    MethodRegistry prepend(LatentMethodMatcher latentMethodMatcher,
                           Prepareable prepareable,
                           MethodAttributeAppender.Factory attributeAppenderFactory);

    MethodRegistry append(LatentMethodMatcher latentMethodMatcher,
                          Prepareable prepareable,
                          MethodAttributeAppender.Factory attributeAppenderFactory);

    Prepared prepare(InstrumentedType instrumentedType, LatentMethodMatcher enforcedMethods);

    interface Prepareable {

        InstrumentedType prepare(InstrumentedType instrumentedType);

        TypeWriter.MethodPool.Entry toEntry(Instrumentation.Target instrumentationTarget, MethodAttributeAppender attributeAppender);

        class ForInstrumentation implements Prepareable {

            private final Instrumentation instrumentation;

            public ForInstrumentation(Instrumentation instrumentation) {
                this.instrumentation = instrumentation;
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentation.prepare(instrumentedType);
            }

            @Override
            public TypeWriter.MethodPool.Entry toEntry(Instrumentation.Target instrumentationTarget, MethodAttributeAppender attributeAppender) {
                return new TypeWriter.MethodPool.Entry.ForImplementation(instrumentation.appender(instrumentationTarget), attributeAppender);
            }
        }

        enum ForAbstractMethod implements Prepareable {

            INSTANCE;

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType;
            }

            @Override
            public TypeWriter.MethodPool.Entry toEntry(Instrumentation.Target instrumentationTarget, MethodAttributeAppender attributeAppender) {
                return new TypeWriter.MethodPool.Entry.ForAbstractMethod(attributeAppender);
            }
        }
    }

    interface Prepared {

        TypeDescription getInstrumentedType();

        LoadedTypeInitializer getLoadedTypeInitializer();

        Compiled compile(Instrumentation.Target.Factory instrumentationTargetFactory, MethodLookupEngine methodLookupEngine);
    }

    interface Compiled extends TypeWriter.MethodPool {

        TypeDescription getInstrumentedType();

        LoadedTypeInitializer getLoadedTypeInitializer();

        InstrumentedType.TypeInitializer getTypeInitializer();

        MethodList getInstrumentedMethods();
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

    class Default implements MethodRegistry {

        private final List<Entry> entries;

        public Default() {
            entries = Collections.emptyList();
        }

        private Default(List<Entry> entries) {
            this.entries = entries;
        }

        @Override
        public MethodRegistry prepend(LatentMethodMatcher latentMethodMatcher,
                                      Prepareable prepareable,
                                      MethodAttributeAppender.Factory attributeAppenderFactory) {
            return new Default(join(new Entry(latentMethodMatcher, prepareable, attributeAppenderFactory), entries));
        }

        @Override
        public MethodRegistry append(LatentMethodMatcher latentMethodMatcher,
                                     Prepareable prepareable,
                                     MethodAttributeAppender.Factory attributeAppenderFactory) {
            return new Default(join(entries, new Entry(latentMethodMatcher, prepareable, attributeAppenderFactory)));
        }

        @Override
        public MethodRegistry.Prepared prepare(InstrumentedType instrumentedType, LatentMethodMatcher enforcedMethods) {
            List<Entry> prefixedEntries = new LinkedList<Entry>();
            Set<Prepareable> prepareables = new HashSet<Prepareable>(entries.size());
            for (Entry entry : entries) {
                // Only call the preparation method of an instrumentation if the instrumentation was not yet prepared.
                if (prepareables.add(entry.getPrepareable())) {
                    MethodList beforePreparation = instrumentedType.getDeclaredMethods();
                    instrumentedType = entry.getPrepareable().prepare(instrumentedType);
                    // If an instrumentation adds methods to the instrumented type, those methods should be
                    // handled by this instrumentation. Thus an additional matcher that matches these exact methods
                    // is registered, in case that the instrumentation actually added methods. These matcher must be
                    // prepended to any other entry such that they become of higher precedence to manually registered
                    // method interceptions. Otherwise, those user interceptions could match the methods that were
                    // added by the instrumentation.
                    if (beforePreparation.size() < instrumentedType.getDeclaredMethods().size()) {
                        prefixedEntries.add(new Entry(new ListDifferenceMethodMatcher(beforePreparation, instrumentedType.getDeclaredMethods()),
                                entry.getPrepareable(),
                                MethodAttributeAppender.NoOp.INSTANCE));
                    }
                }
            }
            return new Prepared(instrumentedType.detach(),
                    instrumentedType.getLoadedTypeInitializer(),
                    instrumentedType.getTypeInitializer(),
                    enforcedMethods.manifest(instrumentedType),
                    join(prefixedEntries, entries));
        }

        protected static class Entry {

            private final LatentMethodMatcher latentMethodMatcher;

            private final Prepareable prepareable;

            private final MethodAttributeAppender.Factory attributeAppenderFactory;

            protected Entry(LatentMethodMatcher latentMethodMatcher,
                            Prepareable prepareable,
                            MethodAttributeAppender.Factory attributeAppenderFactory) {
                this.latentMethodMatcher = latentMethodMatcher;
                this.prepareable = prepareable;
                this.attributeAppenderFactory = attributeAppenderFactory;
            }

            protected Prepareable getPrepareable() {
                return prepareable;
            }

            protected Manifest manifest(TypeDescription instrumentedType) {
                return new Manifest(latentMethodMatcher.manifest(instrumentedType), prepareable, attributeAppenderFactory);
            }

            protected static class Manifest implements ElementMatcher<MethodDescription> {

                private final ElementMatcher<? super MethodDescription> methodMatcher;

                private final Prepareable prepareable;

                private final MethodAttributeAppender.Factory attributeAppenderFactory;

                public Manifest(ElementMatcher<? super MethodDescription> methodMatcher,
                                Prepareable prepareable,
                                MethodAttributeAppender.Factory attributeAppenderFactory) {
                    this.methodMatcher = methodMatcher;
                    this.prepareable = prepareable;
                    this.attributeAppenderFactory = attributeAppenderFactory;
                }

                @Override
                public boolean matches(MethodDescription target) {
                    return methodMatcher.matches(target);
                }

                public Compiled compile(Instrumentation.Target instrumentationTarget) {
                    return new Compiled(methodMatcher, prepareable.toEntry(instrumentationTarget,
                            attributeAppenderFactory.make(instrumentationTarget.getTypeDescription())));
                }
            }

            protected static class Compiled implements ElementMatcher<MethodDescription> {

                private final ElementMatcher<? super MethodDescription> elementMatcher;

                private final TypeWriter.MethodPool.Entry entry;

                public Compiled(ElementMatcher<? super MethodDescription> elementMatcher, TypeWriter.MethodPool.Entry entry) {
                    this.elementMatcher = elementMatcher;
                    this.entry = entry;
                }

                @Override
                public boolean matches(MethodDescription target) {
                    return elementMatcher.matches(target);
                }

                public TypeWriter.MethodPool.Entry getEntry() {
                    return entry;
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

        protected static class Prepared implements MethodRegistry.Prepared {

            private final TypeDescription instrumentedType;

            private final LoadedTypeInitializer loadedTypeInitializer;

            private final InstrumentedType.TypeInitializer typeInitializer;

            private final ElementMatcher<? super MethodDescription> ignoredMethods;

            private final List<Default.Entry> entries;

            public Prepared(TypeDescription instrumentedType,
                            LoadedTypeInitializer loadedTypeInitializer,
                            InstrumentedType.TypeInitializer typeInitializer,
                            ElementMatcher<? super MethodDescription> ignoredMethods,
                            List<Default.Entry> entries) {
                this.instrumentedType = instrumentedType;
                this.loadedTypeInitializer = loadedTypeInitializer;
                this.typeInitializer = typeInitializer;
                this.ignoredMethods = ignoredMethods;
                this.entries = entries;
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
            public MethodRegistry.Compiled compile(Instrumentation.Target.Factory instrumentationTargetFactory, MethodLookupEngine methodLookupEngine) {
                Map<Entry, Entry.Manifest> manifestationCache = new HashMap<Entry, Entry.Manifest>(entries.size());
                List<Entry.Manifest> manifestEntries = new ArrayList<Entry.Manifest>(entries.size());
                for (Entry entry : entries) {
                    Entry.Manifest manifestEntry = manifestationCache.get(entry);
                    if (manifestEntry == null) {
                        manifestEntry = entry.manifest(instrumentedType);
                        manifestationCache.put(entry, manifestEntry);
                    }
                    manifestEntries.add(manifestEntry);
                }
                MethodLookupEngine.Finding finding = methodLookupEngine.process(instrumentedType);
                MethodList instrumentedMethods = finding.getInvokableMethods().filter(not(matchesAny(manifestEntries)).or(ignoredMethods));
                Instrumentation.Target instrumentationTarget = instrumentationTargetFactory.make(finding, instrumentedMethods);
                Map<Entry.Manifest, Entry.Compiled> compilationCache = new HashMap<Entry.Manifest, Entry.Compiled>(entries.size());
                List<Entry.Compiled> compiledEntries = new ArrayList<Entry.Compiled>(entries.size());
                for (Entry.Manifest entry : manifestEntries) {
                    Entry.Compiled compiledEntry = compilationCache.get(entry);
                    if (compiledEntry == null) {
                        compiledEntry = entry.compile(instrumentationTarget);
                        compilationCache.put(entry, compiledEntry);
                    }
                    compiledEntries.add(compiledEntry);
                }
                return new Compiled(instrumentedType,
                        loadedTypeInitializer,
                        typeInitializer,
                        instrumentedMethods,
                        compiledEntries);
            }

            protected static class Compiled implements MethodRegistry.Compiled {

                private final TypeDescription instrumentedType;

                private final LoadedTypeInitializer loadedTypeInitializer;

                private final InstrumentedType.TypeInitializer typeInitializer;

                private final MethodList instrumentedMethods;

                private final List<Default.Entry.Compiled> compiledEntries;

                public Compiled(TypeDescription instrumentedType,
                                LoadedTypeInitializer loadedTypeInitializer,
                                InstrumentedType.TypeInitializer typeInitializer,
                                MethodList instrumentedMethods,
                                List<Default.Entry.Compiled> compiledEntries) {
                    this.instrumentedType = instrumentedType;
                    this.loadedTypeInitializer = loadedTypeInitializer;
                    this.typeInitializer = typeInitializer;
                    this.instrumentedMethods = instrumentedMethods;
                    this.compiledEntries = compiledEntries;
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
                public MethodList getInstrumentedMethods() {
                    return instrumentedMethods;
                }

                @Override
                public Entry target(MethodDescription methodDescription) {
                    for (Default.Entry.Compiled compiledEntry : compiledEntries) {
                        if (compiledEntry.matches(methodDescription)) {
                            return compiledEntry.getEntry();
                        }
                    }
                    throw new IllegalArgumentException("Not an instrumented method: " + methodDescription);
                }
            }
        }
    }
}
