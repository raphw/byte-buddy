package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.annotation.AnnotationValue;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.Transformer;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.LoadedTypeInitializer;
import net.bytebuddy.implementation.attribute.MethodAttributeAppender;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.LatentMatcher;
import net.bytebuddy.utility.CompoundList;

import java.util.*;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * A method registry is responsible for storing information on how a method is intercepted.
 */
public interface MethodRegistry {

    /**
     * Prepends the given method definition to this method registry, i.e. this configuration is applied first.
     *
     * @param methodMatcher            A matcher to identify any method that this definition concerns.
     * @param handler                  The handler to instrument any matched method.
     * @param attributeAppenderFactory A method attribute appender to apply to any matched method.
     * @param transformer              The method transformer to be applied to implemented methods.
     * @return An adapted version of this method registry.
     */
    MethodRegistry prepend(LatentMatcher<? super MethodDescription> methodMatcher,
                           Handler handler,
                           MethodAttributeAppender.Factory attributeAppenderFactory,
                           Transformer<MethodDescription> transformer);

    /**
     * Appends the given method definition to this method registry, i.e. this configuration is applied last.
     *
     * @param methodMatcher            A matcher to identify all entries that are to be matched.
     * @param handler                  The handler to instrument any matched method.
     * @param attributeAppenderFactory A method attribute appender to apply to any matched method.
     * @param transformer              The method transformer to be applied to implemented methods.
     * @return An adapted version of this method registry.
     */
    MethodRegistry append(LatentMatcher<? super MethodDescription> methodMatcher,
                          Handler handler,
                          MethodAttributeAppender.Factory attributeAppenderFactory,
                          Transformer<MethodDescription> transformer);

    /**
     * Prepares this method registry.
     *
     * @param instrumentedType    The instrumented type that should be created.
     * @param methodGraphCompiler The method graph compiler to be used for analyzing the fully assembled instrumented type.
     * @param typeValidation      Determines if a type should be explicitly validated.
     * @param ignoredMethods      A filter that only matches methods that should be instrumented.
     * @return A prepared version of this method registry.
     */
    Prepared prepare(InstrumentedType instrumentedType,
                     MethodGraph.Compiler methodGraphCompiler,
                     TypeValidation typeValidation,
                     LatentMatcher<? super MethodDescription> ignoredMethods);

    /**
     * A handler for implementing a method.
     */
    interface Handler extends InstrumentedType.Prepareable {

        /**
         * Compiles this handler.
         *
         * @param implementationTarget The implementation target to compile this handler for.
         * @return A compiled handler.
         */
        Handler.Compiled compile(Implementation.Target implementationTarget);

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
            public TypeWriter.MethodPool.Record assemble(MethodDescription methodDescription, MethodAttributeAppender attributeAppender, Visibility visibility) {
                return new TypeWriter.MethodPool.Record.ForDefinedMethod.WithoutBody(methodDescription, attributeAppender, visibility);
            }

            @Override
            public String toString() {
                return "MethodRegistry.Handler.ForAbstractMethod." + name();
            }
        }

        /**
         * A handler for implementing a visibility bridge.
         */
        enum ForVisibilityBridge implements Handler {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                throw new IllegalStateException("A visibility bridge handler must not apply any preparations");
            }

            @Override
            public Compiled compile(Implementation.Target implementationTarget) {
                return new Compiled(implementationTarget.getInstrumentedType());
            }

            @Override
            public String toString() {
                return "MethodRegistry.Handler.ForVisibilityBridge." + name();
            }

            /**
             * A compiled handler for a visibility bridge handler.
             */
            protected static class Compiled implements Handler.Compiled {

                /**
                 * The instrumented type.
                 */
                private final TypeDescription instrumentedType;

                /**
                 * Creates a new compiled handler for a visibility bridge.
                 *
                 * @param instrumentedType The instrumented type.
                 */
                protected Compiled(TypeDescription instrumentedType) {
                    this.instrumentedType = instrumentedType;
                }

                @Override
                public TypeWriter.MethodPool.Record assemble(MethodDescription methodDescription, MethodAttributeAppender attributeAppender, Visibility visibility) {
                    return TypeWriter.MethodPool.Record.ForDefinedMethod.OfVisibilityBridge.of(instrumentedType, methodDescription, attributeAppender);
                }

                @Override
                public boolean equals(Object other) {
                    return this == other || !(other == null || getClass() != other.getClass())
                            && instrumentedType.equals(((Compiled) other).instrumentedType);
                }

                @Override
                public int hashCode() {
                    return instrumentedType.hashCode();
                }

                @Override
                public String toString() {
                    return "MethodRegistry.Handler.ForVisibilityBridge.Compiled{" +
                            "instrumentedType=" + instrumentedType +
                            '}';
                }
            }
        }

        /**
         * A compiled handler for implementing a method.
         */
        interface Compiled {

            /**
             * Assembles this compiled entry with a method attribute appender.
             *
             * @param methodDescription The method description to apply with this handler.
             * @param attributeAppender The method attribute appender to apply together with this handler.
             * @param visibility        The represented method's minimum visibility.
             * @return A method pool entry representing this handler and the given attribute appender.
             */
            TypeWriter.MethodPool.Record assemble(MethodDescription methodDescription, MethodAttributeAppender attributeAppender, Visibility visibility);
        }

        /**
         * A handler for a method that is implemented as byte code.
         */
        class ForImplementation implements Handler {

            /**
             * The implementation to apply.
             */
            private final Implementation implementation;

            /**
             * Creates a new handler for implementing a method with byte code.
             *
             * @param implementation The implementation to apply.
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
                 * Creates a new compiled handler for a method implementation.
                 *
                 * @param byteCodeAppender The byte code appender to apply.
                 */
                protected Compiled(ByteCodeAppender byteCodeAppender) {
                    this.byteCodeAppender = byteCodeAppender;
                }

                @Override
                public TypeWriter.MethodPool.Record assemble(MethodDescription methodDescription, MethodAttributeAppender attributeAppender, Visibility visibility) {
                    return new TypeWriter.MethodPool.Record.ForDefinedMethod.WithBody(methodDescription, byteCodeAppender, attributeAppender, visibility);
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
         * A handler for defining a default annotation value for a method.
         */
        class ForAnnotationValue implements Handler, Compiled {

            /**
             * The annotation value to set as a default value.
             */
            private final AnnotationValue<?, ?> annotationValue;

            /**
             * Creates a handler for defining a default annotation value for a method.
             *
             * @param annotationValue The annotation value to set as a default value.
             */
            public ForAnnotationValue(AnnotationValue<?, ?> annotationValue) {
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
            public TypeWriter.MethodPool.Record assemble(MethodDescription methodDescription, MethodAttributeAppender attributeAppender, Visibility visibility) {
                return new TypeWriter.MethodPool.Record.ForDefinedMethod.WithAnnotationDefaultValue(methodDescription, annotationValue, attributeAppender);
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
        MethodList<?> getInstrumentedMethods();

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
        TypeInitializer getTypeInitializer();

        /**
         * Compiles this prepared method registry.
         *
         * @param implementationTargetFactory A factory for creating an implementation target.
         * @param classFileVersion            The type's class file version.
         * @return A factory for creating an implementation target.
         */
        Compiled compile(Implementation.Target.Factory implementationTargetFactory, ClassFileVersion classFileVersion);
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
        TypeInitializer getTypeInitializer();
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
        public MethodRegistry prepend(LatentMatcher<? super MethodDescription> matcher,
                                      Handler handler,
                                      MethodAttributeAppender.Factory attributeAppenderFactory,
                                      Transformer<MethodDescription> transformer) {
            return new Default(CompoundList.of(new Entry(matcher, handler, attributeAppenderFactory, transformer), entries));
        }

        @Override
        public MethodRegistry append(LatentMatcher<? super MethodDescription> matcher,
                                     Handler handler,
                                     MethodAttributeAppender.Factory attributeAppenderFactory,
                                     Transformer<MethodDescription> transformer) {
            return new Default(CompoundList.of(entries, new Entry(matcher, handler, attributeAppenderFactory, transformer)));
        }

        @Override
        public MethodRegistry.Prepared prepare(InstrumentedType instrumentedType,
                                               MethodGraph.Compiler methodGraphCompiler,
                                               TypeValidation typeValidation,
                                               LatentMatcher<? super MethodDescription> ignoredMethods) {
            LinkedHashMap<MethodDescription, Prepared.Entry> implementations = new LinkedHashMap<MethodDescription, Prepared.Entry>();
            Set<Handler> handlers = new HashSet<Handler>();
            MethodList<?> helperMethods = instrumentedType.getDeclaredMethods();
            for (Entry entry : entries) {
                if (handlers.add(entry.getHandler())) {
                    instrumentedType = entry.getHandler().prepare(instrumentedType);
                    ElementMatcher<? super MethodDescription> handledMethods = noneOf(helperMethods);
                    helperMethods = instrumentedType.getDeclaredMethods();
                    for (MethodDescription helperMethod : helperMethods.filter(handledMethods)) {
                        implementations.put(helperMethod, entry.asSupplementaryEntry(helperMethod));
                    }
                }
            }
            MethodGraph.Linked methodGraph = methodGraphCompiler.compile(instrumentedType);
            // Casting required for Java 6 compiler.
            ElementMatcher<? super MethodDescription> relevanceMatcher = (ElementMatcher<? super MethodDescription>) not(anyOf(implementations.keySet()))
                    .and(returns(isVisibleTo(instrumentedType)))
                    .and(hasParameters(whereNone(hasType(not(isVisibleTo(instrumentedType))))))
                    .and(ignoredMethods.resolve(instrumentedType));
            for (MethodGraph.Node node : methodGraph.listNodes()) {
                MethodDescription methodDescription = node.getRepresentative();
                boolean visibilityBridge = instrumentedType.isPublic() && !instrumentedType.isInterface();
                if (relevanceMatcher.matches(methodDescription)) {
                    for (Entry entry : entries) {
                        if (entry.resolve(instrumentedType).matches(methodDescription)) {
                            implementations.put(methodDescription, entry.asPreparedEntry(instrumentedType,
                                    methodDescription,
                                    node.getMethodTypes(),
                                    node.getVisibility()));
                            visibilityBridge = false;
                            break;
                        }
                    }
                }
                if (visibilityBridge
                        && methodDescription.isPublic()
                        && !(methodDescription.isAbstract() || methodDescription.isFinal())
                        && !node.getSort().isMadeVisible()
                        && methodDescription.getDeclaringType().isPackagePrivate()) {
                    // Visibility bridges are required for public classes that inherit a public method from a package-private class.
                    implementations.put(methodDescription, Prepared.Entry.forVisibilityBridge(methodDescription, node.getVisibility()));
                }
            }
            for (MethodDescription methodDescription : CompoundList.of(
                    instrumentedType.getDeclaredMethods().filter(not(isVirtual()).and(relevanceMatcher)),
                    new MethodDescription.Latent.TypeInitializer(instrumentedType))) {
                for (Entry entry : entries) {
                    if (entry.resolve(instrumentedType).matches(methodDescription)) {
                        implementations.put(methodDescription, entry.asPreparedEntry(instrumentedType, methodDescription, methodDescription.getVisibility()));
                        break;
                    }
                }
            }
            return new Prepared(implementations,
                    instrumentedType.getLoadedTypeInitializer(),
                    instrumentedType.getTypeInitializer(),
                    typeValidation.isEnabled()
                            ? instrumentedType.validated()
                            : instrumentedType,
                    methodGraph);
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
        protected static class Entry implements LatentMatcher<MethodDescription> {

            /**
             * The latent method matcher that this entry represents.
             */
            private final LatentMatcher<? super MethodDescription> matcher;

            /**
             * The handler to apply to all matched entries.
             */
            private final Handler handler;

            /**
             * A method attribute appender factory to apply to all entries.
             */
            private final MethodAttributeAppender.Factory attributeAppenderFactory;

            /**
             * The method transformer to be applied to implemented methods.
             */
            private final Transformer<MethodDescription> transformer;

            /**
             * Creates a new entry.
             *
             * @param matcher                  The latent method matcher that this entry represents.
             * @param handler                  The handler to apply to all matched entries.
             * @param attributeAppenderFactory A method attribute appender factory to apply to all entries.
             * @param transformer              The method transformer to be applied to implemented methods.
             */
            protected Entry(LatentMatcher<? super MethodDescription> matcher,
                            Handler handler,
                            MethodAttributeAppender.Factory attributeAppenderFactory,
                            Transformer<MethodDescription> transformer) {
                this.matcher = matcher;
                this.handler = handler;
                this.attributeAppenderFactory = attributeAppenderFactory;
                this.transformer = transformer;
            }

            /**
             * Transforms this entry into a prepared state.
             *
             * @param instrumentedType  The instrumented type.
             * @param methodDescription The non-transformed method to be implemented.
             * @param visibility        The represented method's minimum visibility.
             * @return A prepared version of this entry.
             */
            protected Prepared.Entry asPreparedEntry(TypeDescription instrumentedType, MethodDescription methodDescription, Visibility visibility) {
                return asPreparedEntry(instrumentedType, methodDescription, Collections.<MethodDescription.TypeToken>emptySet(), visibility);
            }

            /**
             * Transforms this entry into a prepared state.
             *
             * @param instrumentedType  The instrumented type.
             * @param methodDescription The non-transformed method to be implemented.
             * @param methodTypes       The method types this method represents.
             * @param visibility        The represented method's minimum visibility.
             * @return A prepared version of this entry.
             */
            protected Prepared.Entry asPreparedEntry(TypeDescription instrumentedType,
                                                     MethodDescription methodDescription,
                                                     Set<MethodDescription.TypeToken> methodTypes,
                                                     Visibility visibility) {
                return new Prepared.Entry(handler,
                        attributeAppenderFactory,
                        transformer.transform(instrumentedType, methodDescription),
                        methodTypes,
                        visibility,
                        false);
            }

            /**
             * Returns a prepared entry for a supplementary method.
             *
             * @param methodDescription The method to be implemented.
             * @return An entry for a supplementary entry that is defined by a method implementation instance.
             */
            protected Prepared.Entry asSupplementaryEntry(MethodDescription methodDescription) {
                return new Prepared.Entry(handler,
                        MethodAttributeAppender.Explicit.of(methodDescription),
                        methodDescription,
                        Collections.<MethodDescription.TypeToken>emptySet(),
                        methodDescription.getVisibility(),
                        false);
            }

            /**
             * Returns this entry's handler.
             *
             * @return The handler of this entry.
             */
            protected Handler getHandler() {
                return handler;
            }

            @Override
            public ElementMatcher<? super MethodDescription> resolve(TypeDescription typeDescription) {
                return matcher.resolve(typeDescription);
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                Entry entry = (Entry) other;
                return matcher.equals(entry.matcher)
                        && handler.equals(entry.handler)
                        && attributeAppenderFactory.equals(entry.attributeAppenderFactory)
                        && transformer.equals(entry.transformer);
            }

            @Override
            public int hashCode() {
                int result = matcher.hashCode();
                result = 31 * result + handler.hashCode();
                result = 31 * result + attributeAppenderFactory.hashCode();
                result = 31 * result + transformer.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "MethodRegistry.Default.Entry{" +
                        "matcher=" + matcher +
                        ", handler=" + handler +
                        ", attributeAppenderFactory=" + attributeAppenderFactory +
                        ", transformer=" + transformer +
                        '}';
            }
        }

        /**
         * A prepared version of a default method registry.
         */
        protected static class Prepared implements MethodRegistry.Prepared {

            /**
             * A map of all method descriptions mapped to their handling entries.
             */
            private final LinkedHashMap<MethodDescription, Entry> implementations;

            /**
             * The loaded type initializer of the instrumented type.
             */
            private final LoadedTypeInitializer loadedTypeInitializer;

            /**
             * The type initializer of the instrumented type.
             */
            private final TypeInitializer typeInitializer;

            /**
             * The instrumented type.
             */
            private final TypeDescription instrumentedType;

            /**
             * A method graph describing the instrumented type.
             */
            private final MethodGraph.Linked methodGraph;

            /**
             * Creates a prepared version of a default method registry.
             *
             * @param implementations       A map of all method descriptions mapped to their handling entries.
             * @param loadedTypeInitializer The loaded type initializer of the instrumented type.
             * @param typeInitializer       The type initializer of the instrumented type.
             * @param instrumentedType      The instrumented type.
             * @param methodGraph           A method graph describing the instrumented type.
             */
            protected Prepared(LinkedHashMap<MethodDescription, Entry> implementations,
                               LoadedTypeInitializer loadedTypeInitializer,
                               TypeInitializer typeInitializer,
                               TypeDescription instrumentedType,
                               MethodGraph.Linked methodGraph) {
                this.implementations = implementations;
                this.loadedTypeInitializer = loadedTypeInitializer;
                this.typeInitializer = typeInitializer;
                this.instrumentedType = instrumentedType;
                this.methodGraph = methodGraph;
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
            public TypeInitializer getTypeInitializer() {
                return typeInitializer;
            }

            @Override
            public MethodList<?> getInstrumentedMethods() {
                return new MethodList.Explicit<MethodDescription>(new ArrayList<MethodDescription>(implementations.keySet())).filter(not(isTypeInitializer()));
            }

            @Override
            public MethodRegistry.Compiled compile(Implementation.Target.Factory implementationTargetFactory, ClassFileVersion classFileVersion) {
                Map<Handler, Handler.Compiled> compilationCache = new HashMap<Handler, Handler.Compiled>();
                Map<MethodAttributeAppender.Factory, MethodAttributeAppender> attributeAppenderCache = new HashMap<MethodAttributeAppender.Factory, MethodAttributeAppender>();
                LinkedHashMap<MethodDescription, Compiled.Entry> entries = new LinkedHashMap<MethodDescription, Compiled.Entry>();
                Implementation.Target implementationTarget = implementationTargetFactory.make(instrumentedType, methodGraph, classFileVersion);
                for (Map.Entry<MethodDescription, Entry> entry : implementations.entrySet()) {
                    Handler.Compiled cachedHandler = compilationCache.get(entry.getValue().getHandler());
                    if (cachedHandler == null) {
                        cachedHandler = entry.getValue().getHandler().compile(implementationTarget);
                        compilationCache.put(entry.getValue().getHandler(), cachedHandler);
                    }
                    MethodAttributeAppender cachedAttributeAppender = attributeAppenderCache.get(entry.getValue().getAppenderFactory());
                    if (cachedAttributeAppender == null) {
                        cachedAttributeAppender = entry.getValue().getAppenderFactory().make(instrumentedType);
                        attributeAppenderCache.put(entry.getValue().getAppenderFactory(), cachedAttributeAppender);
                    }
                    entries.put(entry.getKey(), new Compiled.Entry(cachedHandler,
                            cachedAttributeAppender,
                            entry.getValue().getMethodDescription(),
                            entry.getValue().resolveBridgeTypes(),
                            entry.getValue().getVisibility(),
                            entry.getValue().isBridgeMethod()));
                }
                return new Compiled(instrumentedType, loadedTypeInitializer, typeInitializer, entries, classFileVersion.isAtLeast(ClassFileVersion.JAVA_V5));
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                Prepared prepared = (Prepared) other;
                return implementations.equals(prepared.implementations)
                        && loadedTypeInitializer.equals(prepared.loadedTypeInitializer)
                        && typeInitializer.equals(prepared.typeInitializer)
                        && instrumentedType.equals(prepared.instrumentedType)
                        && methodGraph.equals(prepared.methodGraph);
            }

            @Override
            public int hashCode() {
                int result = implementations.hashCode();
                result = 31 * result + loadedTypeInitializer.hashCode();
                result = 31 * result + typeInitializer.hashCode();
                result = 31 * result + instrumentedType.hashCode();
                result = 31 * result + methodGraph.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "MethodRegistry.Default.Prepared{" +
                        "implementations=" + implementations +
                        ", loadedTypeInitializer=" + loadedTypeInitializer +
                        ", typeInitializer=" + typeInitializer +
                        ", instrumentedType=" + instrumentedType +
                        ", methodGraph=" + methodGraph +
                        '}';
            }

            /**
             * An entry of a prepared method registry.
             */
            protected static class Entry {

                /**
                 * The handler for implementing methods.
                 */
                private final Handler handler;

                /**
                 * A attribute appender factory for appending attributes for any implemented method.
                 */
                private final MethodAttributeAppender.Factory attributeAppenderFactory;

                /**
                 * The method this entry represents.
                 */
                private final MethodDescription methodDescription;

                /**
                 * The method's type tokens.
                 */
                private final Set<MethodDescription.TypeToken> typeTokens;

                /**
                 * The minimum required visibility of this method.
                 */
                private Visibility visibility;

                /**
                 * Is {@code true} if this entry represents a bridge method.
                 */
                private final boolean bridgeMethod;

                /**
                 * Creates a new prepared entry.
                 *
                 * @param handler                  The handler for implementing methods.
                 * @param attributeAppenderFactory A attribute appender factory for appending attributes for any implemented method.
                 * @param methodDescription        The method this entry represents.
                 * @param typeTokens               A set of bridges representing the bridge methods of this method.
                 * @param visibility               The minimum required visibility of this method.
                 * @param bridgeMethod             {@code true} if this entry represents a bridge method.
                 */
                protected Entry(Handler handler,
                                MethodAttributeAppender.Factory attributeAppenderFactory,
                                MethodDescription methodDescription,
                                Set<MethodDescription.TypeToken> typeTokens,
                                Visibility visibility,
                                boolean bridgeMethod) {
                    this.handler = handler;
                    this.attributeAppenderFactory = attributeAppenderFactory;
                    this.methodDescription = methodDescription;
                    this.typeTokens = typeTokens;
                    this.visibility = visibility;
                    this.bridgeMethod = bridgeMethod;
                }

                /**
                 * Creates an entry for a visibility bridge.
                 *
                 * @param bridgeTarget The bridge method's target.
                 * @param visibility   The represented method's minimum visibility.
                 * @return An entry representing a visibility bridge.
                 */
                protected static Entry forVisibilityBridge(MethodDescription bridgeTarget, Visibility visibility) {
                    return new Entry(Handler.ForVisibilityBridge.INSTANCE,
                            MethodAttributeAppender.Explicit.of(bridgeTarget),
                            bridgeTarget,
                            Collections.<MethodDescription.TypeToken>emptySet(),
                            visibility,
                            true);
                }

                /**
                 * Returns this entry's handler.
                 *
                 * @return The entry's handler.
                 */
                protected Handler getHandler() {
                    return handler;
                }

                /**
                 * Returns this entry's attribute appender factory.
                 *
                 * @return This entry's attribute appender factory.
                 */
                protected MethodAttributeAppender.Factory getAppenderFactory() {
                    return attributeAppenderFactory;
                }

                /**
                 * Returns the method description this entry represents.
                 *
                 * @return The method description this entry represents.
                 */
                protected MethodDescription getMethodDescription() {
                    return methodDescription;
                }

                /**
                 * Resolves the type tokens of all bridge methods that are required to be implemented for this entry.
                 *
                 * @return A set of type tokens representing the bridge methods required for implementing this type.
                 */
                protected Set<MethodDescription.TypeToken> resolveBridgeTypes() {
                    HashSet<MethodDescription.TypeToken> typeTokens = new HashSet<MethodDescription.TypeToken>(this.typeTokens);
                    typeTokens.remove(methodDescription.asTypeToken());
                    return typeTokens;
                }

                /**
                 * Returns the represented method's minimum visibility.
                 *
                 * @return The represented method's minimum visibility.
                 */
                protected Visibility getVisibility() {
                    return visibility;
                }

                /**
                 * Returns {@code true} if this entry represents a bridge method.
                 *
                 * @return {@code true} if this entry represents a bridge method.
                 */
                protected boolean isBridgeMethod() {
                    return bridgeMethod;
                }

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (other == null || getClass() != other.getClass()) return false;
                    Entry entry = (Entry) other;
                    return handler.equals(entry.handler)
                            && bridgeMethod == entry.bridgeMethod
                            && attributeAppenderFactory.equals(entry.attributeAppenderFactory)
                            && methodDescription.equals(entry.methodDescription)
                            && typeTokens.equals(entry.typeTokens)
                            && visibility.equals(entry.visibility);
                }

                @Override
                public int hashCode() {
                    int result = handler.hashCode();
                    result = 31 * result + (bridgeMethod ? 1 : 0);
                    result = 31 * result + attributeAppenderFactory.hashCode();
                    result = 31 * result + methodDescription.hashCode();
                    result = 31 * result + typeTokens.hashCode();
                    result = 31 * result + visibility.hashCode();
                    return result;
                }

                @Override
                public String toString() {
                    return "MethodRegistry.Default.Prepared.Entry{" +
                            "handler=" + handler +
                            ", attributeAppenderFactory=" + attributeAppenderFactory +
                            ", methodDescription=" + methodDescription +
                            ", typeTokens=" + typeTokens +
                            ", visibility=" + visibility +
                            ", bridgeMethod=" + bridgeMethod +
                            '}';
                }
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
            private final TypeInitializer typeInitializer;

            /**
             * A map of all method descriptions mapped to their handling entries.
             */
            private final LinkedHashMap<MethodDescription, Entry> implementations;

            /**
             * {@code true} if the created type supports bridge methods.
             */
            private final boolean supportsBridges;

            /**
             * Creates a new compiled version of a default method registry.
             *
             * @param instrumentedType      The instrumented type.
             * @param loadedTypeInitializer The loaded type initializer of the instrumented type.
             * @param typeInitializer       The type initializer of the instrumented type.
             * @param implementations       A map of all method descriptions mapped to their handling entries.
             * @param supportsBridges       {@code true} if the created type supports bridge methods.
             */
            protected Compiled(TypeDescription instrumentedType,
                               LoadedTypeInitializer loadedTypeInitializer,
                               TypeInitializer typeInitializer,
                               LinkedHashMap<MethodDescription, Entry> implementations,
                               boolean supportsBridges) {
                this.instrumentedType = instrumentedType;
                this.loadedTypeInitializer = loadedTypeInitializer;
                this.typeInitializer = typeInitializer;
                this.implementations = implementations;
                this.supportsBridges = supportsBridges;
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
            public TypeInitializer getTypeInitializer() {
                return typeInitializer;
            }

            @Override
            public MethodList<?> getInstrumentedMethods() {
                return new MethodList.Explicit<MethodDescription>(new ArrayList<MethodDescription>(implementations.keySet())).filter(not(isTypeInitializer()));
            }

            @Override
            public Record target(MethodDescription methodDescription) {
                Entry entry = implementations.get(methodDescription);
                return entry == null
                        ? new Record.ForNonImplementedMethod(methodDescription)
                        : entry.bind(instrumentedType, supportsBridges);
            }

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                Compiled compiled = (Compiled) other;
                return instrumentedType.equals(compiled.instrumentedType)
                        && loadedTypeInitializer.equals(compiled.loadedTypeInitializer)
                        && typeInitializer.equals(compiled.typeInitializer)
                        && implementations.equals(compiled.implementations)
                        && supportsBridges == compiled.supportsBridges;
            }

            @Override
            public int hashCode() {
                int result = instrumentedType.hashCode();
                result = 31 * result + loadedTypeInitializer.hashCode();
                result = 31 * result + typeInitializer.hashCode();
                result = 31 * result + implementations.hashCode();
                result = 31 * result + (supportsBridges ? 1 : 0);
                return result;
            }

            @Override
            public String toString() {
                return "MethodRegistry.Default.Compiled{" +
                        "instrumentedType=" + instrumentedType +
                        ", loadedTypeInitializer=" + loadedTypeInitializer +
                        ", typeInitializer=" + typeInitializer +
                        ", implementations=" + implementations +
                        ", supportsBridges=" + supportsBridges +
                        '}';
            }

            /**
             * An entry of a compiled method registry.
             */
            protected static class Entry {

                /**
                 * The handler to be used for implementing a method.
                 */
                private final Handler.Compiled handler;

                /**
                 * The attribute appender of a compiled method.
                 */
                private final MethodAttributeAppender attributeAppender;

                /**
                 * The method to be implemented including potential transformations.
                 */
                private final MethodDescription methodDescription;

                /**
                 * The type tokens representing all bridge methods for the method.
                 */
                private final Set<MethodDescription.TypeToken> bridgeTypes;

                /**
                 * The represented method's minimum visibility.
                 */
                private final Visibility visibility;

                /**
                 * {@code true} if this entry represents a bridge method.
                 */
                private final boolean bridgeMethod;

                /**
                 * Creates a new entry for a compiled method registry.
                 *
                 * @param handler           The handler to be used for implementing a method.
                 * @param attributeAppender The attribute appender of a compiled method.
                 * @param methodDescription The method to be implemented including potential transformations.
                 * @param bridgeTypes       The type tokens representing all bridge methods for the method.
                 * @param visibility        The represented method's minimum visibility.
                 * @param bridgeMethod      {@code true} if this entry represents a bridge method.
                 */
                protected Entry(Handler.Compiled handler,
                                MethodAttributeAppender attributeAppender,
                                MethodDescription methodDescription,
                                Set<MethodDescription.TypeToken> bridgeTypes,
                                Visibility visibility,
                                boolean bridgeMethod) {
                    this.handler = handler;
                    this.attributeAppender = attributeAppender;
                    this.methodDescription = methodDescription;
                    this.bridgeTypes = bridgeTypes;
                    this.visibility = visibility;
                    this.bridgeMethod = bridgeMethod;
                }

                /**
                 * Transforms this entry into a method record.
                 *
                 * @param instrumentedType The instrumented type to bind.
                 * @param supportsBridges  {@code true} if the record should support bridge methods.
                 * @return A record representing this entry's properties.
                 */
                protected Record bind(TypeDescription instrumentedType, boolean supportsBridges) {
                    if (bridgeMethod && !supportsBridges) {
                        return new Record.ForNonImplementedMethod(methodDescription);
                    }
                    Record record = handler.assemble(methodDescription, attributeAppender, visibility);
                    return supportsBridges
                            ? TypeWriter.MethodPool.Record.AccessBridgeWrapper.of(record, instrumentedType, methodDescription, bridgeTypes, attributeAppender)
                            : record;
                }

                @Override
                public boolean equals(Object other) {
                    if (this == other) return true;
                    if (other == null || getClass() != other.getClass()) return false;
                    Entry entry = (Entry) other;
                    return handler.equals(entry.handler)
                            && bridgeMethod == entry.bridgeMethod
                            && attributeAppender.equals(entry.attributeAppender)
                            && methodDescription.equals(entry.methodDescription)
                            && bridgeTypes.equals(entry.bridgeTypes)
                            && visibility.equals(entry.visibility);
                }

                @Override
                public int hashCode() {
                    int result = handler.hashCode();
                    result = 31 * result + (bridgeMethod ? 1 : 0);
                    result = 31 * result + attributeAppender.hashCode();
                    result = 31 * result + methodDescription.hashCode();
                    result = 31 * result + bridgeTypes.hashCode();
                    result = 31 * result + visibility.hashCode();
                    return result;
                }

                @Override
                public String toString() {
                    return "MethodRegistry.Default.Compiled.Entry{" +
                            "handler=" + handler +
                            ", attributeAppender=" + attributeAppender +
                            ", methodDescription=" + methodDescription +
                            ", bridgeTypes=" + bridgeTypes +
                            ", bridgeMethod=" + bridgeMethod +
                            ", visibility=" + visibility +
                            '}';
                }
            }
        }
    }
}
