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
import net.bytebuddy.matcher.LatentMethodMatcher;

import java.util.*;

import static net.bytebuddy.matcher.ElementMatchers.anyOf;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.utility.ByteBuddyCommons.join;

/**
 * A method registry is responsible for storing information on how a method is intercepted.
 */
public interface MethodRegistry {

    MethodRegistry prepend(LatentMethodMatcher methodMatcher,
                           Handler handler,
                           MethodAttributeAppender.Factory attributeAppenderFactory);

    MethodRegistry append(LatentMethodMatcher methodMatcher,
                          Handler handler,
                          MethodAttributeAppender.Factory attributeAppenderFactory);

    Prepared prepare(InstrumentedType instrumentedType,
                     MethodLookupEngine methodLookupEngine,
                     LatentMethodMatcher methodFilter);

    interface Handler {

        InstrumentedType prepare(InstrumentedType instrumentedType);

        Handler.Compiled compile(Instrumentation.Target instrumentationTarget);

        interface Compiled {

            TypeWriter.MethodPool.Entry assemble(MethodAttributeAppender attributeAppender);
        }

        class ForInstrumentation implements Handler {

            private final Instrumentation instrumentation;

            public ForInstrumentation(Instrumentation instrumentation) {
                this.instrumentation = instrumentation;
            }

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentation.prepare(instrumentedType);
            }

            @Override
            public Compiled compile(Instrumentation.Target instrumentationTarget) {
                return new Compiled(instrumentation.appender(instrumentationTarget));
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && instrumentation.equals(((ForInstrumentation) other).instrumentation);
            }

            @Override
            public int hashCode() {
                return instrumentation.hashCode();
            }

            @Override
            public String toString() {
                return "MethodRegistry.Preparable.ForInstrumentation{" +
                        "instrumentation=" + instrumentation +
                        '}';
            }

            protected static class Compiled implements Handler.Compiled {

                private final ByteCodeAppender byteCodeAppender;

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
                    return "MethodRegistry.Preparable.ForInstrumentation.Compiled{" +
                            "byteCodeAppender=" + byteCodeAppender +
                            '}';
                }
            }
        }

        enum ForAbstractMethod implements Handler, Compiled {

            INSTANCE;

            @Override
            public InstrumentedType prepare(InstrumentedType instrumentedType) {
                return instrumentedType;
            }

            @Override
            public Compiled compile(Instrumentation.Target instrumentationTarget) {
                return this;
            }

            @Override
            public TypeWriter.MethodPool.Entry assemble(MethodAttributeAppender attributeAppender) {
                return new TypeWriter.MethodPool.Entry.ForAbstractMethod(attributeAppender);
            }

            @Override
            public String toString() {
                return "MethodRegistry.Preparable.ForAbstractMethod." + name();
            }
        }
    }

    interface Prepared {

        TypeDescription getInstrumentedType();

        MethodList getInstrumentedMethods();

        LoadedTypeInitializer getLoadedTypeInitializer();

        InstrumentedType.TypeInitializer getTypeInitializer();

        Compiled compile(Instrumentation.Target.Factory instrumentationTargetFactory);
    }

    interface Compiled extends TypeWriter.MethodPool {

        TypeDescription getInstrumentedType();

        LoadedTypeInitializer getLoadedTypeInitializer();

        InstrumentedType.TypeInitializer getTypeInitializer();

        MethodList getInstrumentedMethods();
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
            ElementMatcher<? super MethodDescription> instrumented = not(anyOf(instrumentations.keySet())).and(methodFilter.manifest(instrumentedType));
            for (MethodDescription methodDescription : finding.getInvokableMethods().filter(instrumented)) {
                for (Entry entry : entries) {
                    if (entry.manifest(instrumentedType).matches(methodDescription)) {
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

        protected static class Entry implements LatentMethodMatcher {

            private final LatentMethodMatcher methodMatcher;

            private final Handler handler;

            private final MethodAttributeAppender.Factory attributeAppenderFactory;

            protected Entry(LatentMethodMatcher methodMatcher,
                            Handler handler,
                            MethodAttributeAppender.Factory attributeAppenderFactory) {
                this.methodMatcher = methodMatcher;
                this.handler = handler;
                this.attributeAppenderFactory = attributeAppenderFactory;
            }

            protected Handler getHandler() {
                return handler;
            }

            protected MethodAttributeAppender.Factory getAppenderFactory() {
                return attributeAppenderFactory;
            }

            @Override
            public ElementMatcher<? super MethodDescription> manifest(TypeDescription instrumentedType) {
                return methodMatcher.manifest(instrumentedType);
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
                        ", prepareable=" + handler +
                        ", attributeAppenderFactory=" + attributeAppenderFactory +
                        '}';
            }
        }

        protected static class Prepared implements MethodRegistry.Prepared {

            private final Map<MethodDescription, Entry> instrumentations;

            private final LoadedTypeInitializer loadedTypeInitializer;

            private final InstrumentedType.TypeInitializer typeInitializer;

            private final MethodLookupEngine.Finding finding;

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
                return new MethodList.Explicit(new ArrayList<MethodDescription>(instrumentations.keySet()));
            }

            @Override
            public MethodRegistry.Compiled compile(Instrumentation.Target.Factory instrumentationTargetFactory) {
                Map<Handler, Handler.Compiled> compilationCache = new HashMap<Handler, Handler.Compiled>(instrumentations.size());
                Map<MethodAttributeAppender.Factory, MethodAttributeAppender> attributeAppenderCache = new HashMap<MethodAttributeAppender.Factory, MethodAttributeAppender>(instrumentations.size());
                Map<MethodDescription, TypeWriter.MethodPool.Entry> entries = new HashMap<MethodDescription, TypeWriter.MethodPool.Entry>(instrumentations.size());
                Instrumentation.Target instrumentationTarget = instrumentationTargetFactory.make(finding, getInstrumentedMethods());
                for (Map.Entry<MethodDescription, Entry> entry : instrumentations.entrySet()) {
                    Handler.Compiled cachedEntry = compilationCache.get(entry.getValue().getHandler());
                    if (cachedEntry == null) {
                        cachedEntry = entry.getValue().getHandler().compile(instrumentationTarget);
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

        protected static class Compiled implements MethodRegistry.Compiled {

            private final TypeDescription instrumentedType;

            private final LoadedTypeInitializer loadedTypeInitializer;

            private final InstrumentedType.TypeInitializer typeInitializer;

            private final Map<MethodDescription, Entry> instrumentations;

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
                return new MethodList.Explicit(new ArrayList<MethodDescription>(instrumentations.keySet()));
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
