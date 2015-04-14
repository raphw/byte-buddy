package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.LoadedTypeInitializer;
import net.bytebuddy.implementation.attribute.MethodAttributeAppender;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.LatentMethodMatcher;

import java.util.*;

import static net.bytebuddy.matcher.ElementMatchers.*;
import static net.bytebuddy.utility.ByteBuddyCommons.join;

/**
 * A method registry is responsible for storing information on how a method is intercepted.
 */
public interface MethodRegistry {

    /**
     * Prepends the given instrumentation configuration to this method registry, i.e. this configuration is applied first.
     *
     * @param methodMatcher            A matcher to identify all entries that are to be matched.
     * @param handler                  The handler to instrument any matched method.
     * @param attributeAppenderFactory A method attribute appender to apply to any matched method.
     * @return A mutated version of this method registry.
     */
    MethodRegistry prepend(LatentMethodMatcher methodMatcher,
                           Handler handler,
                           MethodAttributeAppender.Factory attributeAppenderFactory);

    /**
     * Appends the given instrumentation configuration to this method registry, i.e. this configuration is applied last.
     *
     * @param methodMatcher            A matcher to identify all entries that are to be matched.
     * @param handler                  The handler to instrument any matched method.
     * @param attributeAppenderFactory A method attribute appender to apply to any matched method.
     * @return A mutated version of this method registry.
     */
    MethodRegistry append(LatentMethodMatcher methodMatcher,
                          Handler handler,
                          MethodAttributeAppender.Factory attributeAppenderFactory);

    /**
     * Prepares this method registry.
     *
     * @param instrumentedType   The instrumented type that should be created.
     * @param methodLookupEngine A method lookup engine to analyze the fully prepared type.
     * @param methodFilter       A filter that only matches methods that should be instrumented.
     * @return A preprated version of this method registry.
     */
    Prepared prepare(InstrumentedType instrumentedType,
                     MethodLookupEngine methodLookupEngine,
                     LatentMethodMatcher methodFilter);

    /**
     * A handler for implementing a method.
     */
    interface Handler {

        /**
         * Prepares the instrumented type for this handler.
         *
         * @param instrumentedType The instrumented type to prepare.
         * @return The prepared instrumented type.
         */
        InstrumentedType prepare(InstrumentedType instrumentedType);

        /**
         * Compiles this handler.
         *
         * @param implementationTarget The instrumentation target to compile this handler for.
         * @return A compiled handler.
         */
        Handler.Compiled compile(Implementation.Target implementationTarget);

        /**
         * A compiled handler for implementing a method.
         */
        interface Compiled {

            /**
             * Assembles this compiled entry with a method attribute appender.
             *
             * @param attributeAppender The method attribute appender to apply together with this handler.
             * @return A method pool entry representing this handler and the given attribute appender.
             */
            TypeWriter.MethodPool.Entry assemble(MethodAttributeAppender attributeAppender);
        }

        /**
         * A handler for a method that is implemented as byte code.
         */
        class ForImplementation implements Handler {

            /**
             * The instrumentation to apply.
             */
            private final Implementation implementation;

            /**
             * Creates a new handler for instrumenting a method with byte code.
             *
             * @param implementation The instrumentation to apply.
             */
            public ForImplementation(Implementation implementation) {
                this.implementation = implementation;
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return implementation.prepare(instrumentedType);
            }

            @Override
            public Compiled compile(Implementation.Target implementationTarget) {
                return new Compiled(implementation.appender(implementationTarget));
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && implementation.equals(((ForImplementation) other).implementation);
            }

            @Override
            public int hashCode() {
                return implementation.hashCode();
            }

            @Override
            public String toString() {
                return "MethodRegistry.Handler.ForImplementation{" +
                        "implementation=" + implementation +
                        '}';
            }

            /**
             * A compiled handler for implementing a method.
             */
            protected static class Compiled implements Handler.Compiled {

                /**
                 * The byte code appender to apply.
                 */
                private final ByteCodeAppender byteCodeAppender;

                /**
                 * Creates a new compiled instrumentation.
                 *
                 * @param byteCodeAppender The byte code appender to apply.
                 */
                protected Compiled(ByteCodeAppender byteCodeAppender) {
                    this.byteCodeAppender = byteCodeAppender;
                }

                @Override
                public TypeWriter.MethodPool.Entry assemble(MethodAttributeAppender attributeAppender) {
                    return new TypeWriter.MethodPool.Entry.ForImplementation(byteCodeAppender, attributeAppender);
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && byteCodeAppender.equals(((Compiled) other).byteCodeAppender);
                }

                @Override
                public int hashCode() {
                    return byteCodeAppender.hashCode();
                }

                @Override
                public String toString() {
                    return "MethodRegistry.Handler.ForImplementation.Compiled{" +
                            "byteCodeAppender=" + byteCodeAppender +
                            '}';
                }
            }
        }

        /**
         * A handler for defining an abstract or native method.
         */
        enum ForAbstractMethod implements Handler, Compiled {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType;
            }

            @Override
            public Compiled compile(Implementation.Target implementationTarget) {
                return this;
            }

            @Override
            public TypeWriter.MethodPool.Entry assemble(MethodAttributeAppender attributeAppender) {
                return new TypeWriter.MethodPool.Entry.ForAbstractMethod(attributeAppender);
            }

            @Override
            public String toString() {
                return "MethodRegistry.Handler.ForAbstractMethod." + name();
            }
        }

        /**
         * A handler for defining a default annotation value for a method.
         */
        class ForAnnotationValue implements Handler, Compiled {

            /**
             * The annotation value to set as a default value.
             */
            private final Object annotationValue;

            /**
             * Represents the given value as an annotation default value handler after validating its suitability.
             *
             * @param annotationValue The annotation value to represent.
             * @return A handler for setting the given value as a default value for instrumented methods.
             */
            public static Handler of(Object annotationValue) {
                TypeDescription typeDescription = new TypeDescription.ForLoadedType(annotationValue.getClass());
                if (!typeDescription.isAnnotationValue() && !typeDescription.isPrimitiveWrapper()) {
                    throw new IllegalArgumentException("Not an annotation value type: " + typeDescription);
                }
                return new ForAnnotationValue(annotationValue);
            }

            /**
             * Creates a handler for defining a default annotation value for a method.
             *
             * @param annotationValue The annotation value to set as a default value.
             */
            protected ForAnnotationValue(Object annotationValue) {
                this.annotationValue = annotationValue;
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType;
            }

            @Override
            public Compiled compile(Implementation.Target implementationTarget) {
                return this;
            }

            @Override
            public TypeWriter.MethodPool.Entry assemble(MethodAttributeAppender attributeAppender) {
                return new TypeWriter.MethodPool.Entry.ForAnnotationDefaultValue(annotationValue, attributeAppender);
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && annotationValue.equals(((ForAnnotationValue) other).annotationValue);
            }

            @Override
            public int hashCode() {
                return annotationValue.hashCode();
            }

            @Override
            public String toString() {
                return "MethodRegistry.Handler.ForAnnotationValue{" +
                        "annotationValue=" + annotationValue +
                        '}';
            }
        }
    }

    /**
     * A method registry that fully prepared the instrumented type.
     */
    interface Prepared {

        /**
         * Returns the fully prepared instrumented type.
         *
         * @return The fully prepared instrumented type.
         */
        TypeDescription getInstrumentedType();

        /**
         * Returns a list of all methods that should be instrumented.
         *
         * @return A list of all methods that should be instrumented.
         */
        MethodList getInstrumentedMethods();

        /**
         * Returns the loaded type initializer of the instrumented type.
         *
         * @return The loaded type initializer of the instrumented type.
         */
        LoadedTypeInitializer getLoadedTypeInitializer();

        /**
         * The type initializer of the instrumented type.
         *
         * @return The type initializer of the instrumented type.
         */
        InstrumentedType.TypeInitializer getTypeInitializer();

        /**
         * Compiles this prepared method registry.
         *
         * @param implementationTargetFactory A factory for creating an instrumentation target.
         * @return A factory for creating an instrumentation target.
         */
        Compiled compile(Implementation.Target.Factory implementationTargetFactory);
    }

    /**
     * A compiled version of a method registry.
     */
    interface Compiled extends TypeWriter.MethodPool {

        /**
         * Returns the instrumented type that is to be created.
         *
         * @return The instrumented type that is to be created.
         */
        TypeDescription getInstrumentedType();

        /**
         * Returns a list of all methods that should be instrumented.
         *
         * @return A list of all methods that should be instrumented.
         */
        MethodList getInstrumentedMethods();

        /**
         * Returns the loaded type initializer of the instrumented type.
         *
         * @return The loaded type initializer of the instrumented type.
         */
        LoadedTypeInitializer getLoadedTypeInitializer();

        /**
         * The type initializer of the instrumented type.
         *
         * @return The type initializer of the instrumented type.
         */
        InstrumentedType.TypeInitializer getTypeInitializer();
    }

    /**
     * A default implementation of a method registry.
     */
    class Default implements MethodRegistry {

        /**
         * The list of currently registered entries in their application order.
         */
        private final List<Entry> entries;

        /**
         * Creates a new default method registry without entries.
         */
        public Default() {
            entries = Collections.emptyList();
        }

        /**
         * Creates a new default method registry.
         *
         * @param entries The currently registered entries.
         */
        private Default(List<Entry> entries) {
            this.entries = entries;
        }

        @Override
        public MethodRegistry prepend(LatentMethodMatcher methodMatcher,
                                      Handler handler,
                                      MethodAttributeAppender.Factory attributeAppenderFactory) {
            return new Default(join(new Entry(methodMatcher, handler, attributeAppenderFactory), entries));
        }

        @Override
        public MethodRegistry append(LatentMethodMatcher methodMatcher,
                                     Handler handler,
                                     MethodAttributeAppender.Factory attributeAppenderFactory) {
            return new Default(join(entries, new Entry(methodMatcher, handler, attributeAppenderFactory)));
        }

        @Override
        public MethodRegistry.Prepared prepare(InstrumentedType instrumentedType,
                                               MethodLookupEngine methodLookupEngine,
                                               LatentMethodMatcher methodFilter) {
            Map<MethodDescription, Entry> instrumentations = new HashMap<MethodDescription, Entry>();
            Set<Handler> handlers = new HashSet<Handler>(entries.size());
            int helperMethodIndex = instrumentedType.getDeclaredMethods().size();
            for (Entry entry : entries) {
                if (handlers.add(entry.getHandler())) {
                    instrumentedType = entry.getHandler().prepare(instrumentedType);
                    MethodList helperMethods = instrumentedType.getDeclaredMethods();
                    for (MethodDescription helperMethod : helperMethods.subList(helperMethodIndex, helperMethods.size())) {
                        instrumentations.put(helperMethod, entry);
                    }
                    helperMethodIndex = helperMethods.size();
                }
            }
            MethodLookupEngine.Finding finding = methodLookupEngine.process(instrumentedType);
            ElementMatcher<? super MethodDescription> instrumented = not(anyOf(instrumentations.keySet())).and(methodFilter.resolve(instrumentedType));
            List<MethodDescription> methodDescriptions = join(MethodDescription.Latent.typeInitializerOf(instrumentedType),
                    finding.getInvokableMethods().filter(instrumented));
            for (MethodDescription methodDescription : methodDescriptions) {
                for (Entry entry : entries) {
                    if (entry.resolve(instrumentedType).matches(methodDescription)) {
                        instrumentations.put(methodDescription, entry);
                        break;
                    }
                }
            }
            return new Prepared(instrumentations,
                    instrumentedType.getLoadedTypeInitializer(),
                    instrumentedType.getTypeInitializer(),
                    finding);
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
            return "MethodRegistry.Default{" +
                    "entries=" + entries +
                    '}';
        }

        /**
         * An entry of a default method registry.
         */
        protected static class Entry implements LatentMethodMatcher {

            /**
             * The latent method matcher that this entry represents.
             */
            private final LatentMethodMatcher methodMatcher;

            /**
             * The handler to apply to all matched entries.
             */
            private final Handler handler;

            /**
             * A method attribute appender factory to apply to all entries.
             */
            private final MethodAttributeAppender.Factory attributeAppenderFactory;

            /**
             * Creates a new entry.
             *
             * @param methodMatcher            The latent method matcher that this entry represents.
             * @param handler                  The handler to apply to all matched entries.
             * @param attributeAppenderFactory A method attribute appender factory to apply to all entries.
             */
            protected Entry(LatentMethodMatcher methodMatcher,
                            Handler handler,
                            MethodAttributeAppender.Factory attributeAppenderFactory) {
                this.methodMatcher = methodMatcher;
                this.handler = handler;
                this.attributeAppenderFactory = attributeAppenderFactory;
            }

            /**
             * Returns the handler of this entry.
             *
             * @return The handler of this entry.
             */
            protected Handler getHandler() {
                return handler;
            }

            /**
             * Returns the attribute appender factory of this entry.
             *
             * @return The attribute appender factory of this entry.
             */
            protected MethodAttributeAppender.Factory getAppenderFactory() {
                return attributeAppenderFactory;
            }

            @Override
            public ElementMatcher<? super MethodDescription> resolve(TypeDescription instrumentedType) {
                return methodMatcher.resolve(instrumentedType);
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                Entry entry = (Entry) other;
                return methodMatcher.equals(entry.methodMatcher)
                        && handler.equals(entry.handler)
                        && attributeAppenderFactory.equals(entry.attributeAppenderFactory);
            }

            @Override
            public int hashCode() {
                int result = methodMatcher.hashCode();
                result = 31 * result + handler.hashCode();
                result = 31 * result + attributeAppenderFactory.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "MethodRegistry.Default.Entry{" +
                        "methodMatcher=" + methodMatcher +
                        ", handler=" + handler +
                        ", attributeAppenderFactory=" + attributeAppenderFactory +
                        '}';
            }
        }

        /**
         * A prepared version of a default method registry.
         */
        protected static class Prepared implements MethodRegistry.Prepared {

            /**
             * A map of all method descriptions mapped to their handling entires.
             */
            private final Map<MethodDescription, Entry> instrumentations;

            /**
             * The loaded type initializer of the instrumented type.
             */
            private final LoadedTypeInitializer loadedTypeInitializer;

            /**
             * The type intiailizer of the instrumented type.
             */
            private final InstrumentedType.TypeInitializer typeInitializer;

            /**
             * The analyzed instrumented type.
             */
            private final MethodLookupEngine.Finding finding;

            /**
             * Creates a prepared version of a default method registry.
             *
             * @param instrumentations      A map of all method descriptions mapped to their handling entires.
             * @param loadedTypeInitializer The loaded type initializer of the instrumented type.
             * @param typeInitializer       The type intiailizer of the instrumented type.
             * @param finding               The analyzed instrumented type.
             */
            public Prepared(Map<MethodDescription, Entry> instrumentations,
                            LoadedTypeInitializer loadedTypeInitializer,
                            InstrumentedType.TypeInitializer typeInitializer,
                            MethodLookupEngine.Finding finding) {
                this.instrumentations = instrumentations;
                this.loadedTypeInitializer = loadedTypeInitializer;
                this.typeInitializer = typeInitializer;
                this.finding = finding;
            }

            @Override
            public TypeDescription getInstrumentedType() {
                return finding.getTypeDescription();
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
                return new MethodList.Explicit(new ArrayList<MethodDescription>(instrumentations.keySet())).filter(not(isTypeInitializer()));
            }

            @Override
            public MethodRegistry.Compiled compile(Implementation.Target.Factory implementationTargetFactory) {
                Map<Handler, Handler.Compiled> compilationCache = new HashMap<Handler, Handler.Compiled>(instrumentations.size());
                Map<MethodAttributeAppender.Factory, MethodAttributeAppender> attributeAppenderCache = new HashMap<MethodAttributeAppender.Factory, MethodAttributeAppender>(instrumentations.size());
                Map<MethodDescription, TypeWriter.MethodPool.Entry> entries = new HashMap<MethodDescription, TypeWriter.MethodPool.Entry>(instrumentations.size());
                Implementation.Target implementationTarget = implementationTargetFactory.make(finding, getInstrumentedMethods());
                for (Map.Entry<MethodDescription, Entry> entry : instrumentations.entrySet()) {
                    Handler.Compiled cachedEntry = compilationCache.get(entry.getValue().getHandler());
                    if (cachedEntry == null) {
                        cachedEntry = entry.getValue().getHandler().compile(implementationTarget);
                        compilationCache.put(entry.getValue().getHandler(), cachedEntry);
                    }
                    MethodAttributeAppender cachedAttributeAppender = attributeAppenderCache.get(entry.getValue().getAppenderFactory());
                    if (cachedAttributeAppender == null) {
                        cachedAttributeAppender = entry.getValue().getAppenderFactory().make(finding.getTypeDescription());
                        attributeAppenderCache.put(entry.getValue().getAppenderFactory(), cachedAttributeAppender);
                    }
                    entries.put(entry.getKey(), cachedEntry.assemble(cachedAttributeAppender));
                }
                return new Compiled(finding.getTypeDescription(), loadedTypeInitializer, typeInitializer, entries);
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                Prepared prepared = (Prepared) other;
                return instrumentations.equals(prepared.instrumentations)
                        && loadedTypeInitializer.equals(prepared.loadedTypeInitializer)
                        && typeInitializer.equals(prepared.typeInitializer)
                        && finding.equals(prepared.finding);
            }

            @Override
            public int hashCode() {
                int result = instrumentations.hashCode();
                result = 31 * result + loadedTypeInitializer.hashCode();
                result = 31 * result + typeInitializer.hashCode();
                result = 31 * result + finding.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "MethodRegistry.Default.Prepared{" +
                        "instrumentations=" + instrumentations +
                        ", loadedTypeInitializer=" + loadedTypeInitializer +
                        ", typeInitializer=" + typeInitializer +
                        ", finding=" + finding +
                        '}';
            }
        }

        /**
         * A compiled version of a default method registry.
         */
        protected static class Compiled implements MethodRegistry.Compiled {

            /**
             * The instrumented type.
             */
            private final TypeDescription instrumentedType;

            /**
             * The loaded type initializer of the instrumented type.
             */
            private final LoadedTypeInitializer loadedTypeInitializer;

            /**
             * The type initializer of the instrumented type.
             */
            private final InstrumentedType.TypeInitializer typeInitializer;

            /**
             * A map of all method descriptions mapped to their handling entries.
             */
            private final Map<MethodDescription, Entry> instrumentations;

            /**
             * Creates a new compiled version of a default method registry.
             *
             * @param instrumentedType      The instrumented type.
             * @param loadedTypeInitializer The loaded type initializer of the instrumented type.
             * @param typeInitializer       The type initializer of the instrumented type.
             * @param instrumentations      A map of all method descriptions mapped to their handling entries.
             */
            public Compiled(TypeDescription instrumentedType,
                            LoadedTypeInitializer loadedTypeInitializer,
                            InstrumentedType.TypeInitializer typeInitializer,
                            Map<MethodDescription, Entry> instrumentations) {
                this.instrumentedType = instrumentedType;
                this.loadedTypeInitializer = loadedTypeInitializer;
                this.typeInitializer = typeInitializer;
                this.instrumentations = instrumentations;
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
                return new MethodList.Explicit(new ArrayList<MethodDescription>(instrumentations.keySet())).filter(not(isTypeInitializer()));
            }

            @Override
            public Entry target(MethodDescription methodDescription) {
                Entry entry = instrumentations.get(methodDescription);
                return entry == null
                        ? Entry.ForSkippedMethod.INSTANCE
                        : entry;
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                Compiled compiled = (Compiled) other;
                return instrumentedType.equals(compiled.instrumentedType)
                        && loadedTypeInitializer.equals(compiled.loadedTypeInitializer)
                        && typeInitializer.equals(compiled.typeInitializer)
                        && instrumentations.equals(compiled.instrumentations);
            }

            @Override
            public int hashCode() {
                int result = instrumentedType.hashCode();
                result = 31 * result + loadedTypeInitializer.hashCode();
                result = 31 * result + typeInitializer.hashCode();
                result = 31 * result + instrumentations.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "MethodRegistry.Default.Compiled{" +
                        "instrumentedType=" + instrumentedType +
                        ", loadedTypeInitializer=" + loadedTypeInitializer +
                        ", typeInitializer=" + typeInitializer +
                        ", instrumentations=" + instrumentations +
                        '}';
            }
        }
    }
}
