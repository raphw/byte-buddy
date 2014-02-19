package com.blogspot.mydailyjava.bytebuddy.dynamic.scaffold;

import com.blogspot.mydailyjava.bytebuddy.instrumentation.Instrumentation;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.attribute.MethodAttributeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.MethodDescription;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.bytecode.ByteCodeAppender;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.method.matcher.MethodMatcher;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.InstrumentedType;
import com.blogspot.mydailyjava.bytebuddy.instrumentation.type.TypeDescription;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface InterceptionRegistry {

    static interface Compiled {

        static interface Entry {

            static class Default implements Entry {

                private final ByteCodeAppender byteCodeAppender;
                private final MethodAttributeAppender methodAttributeAppender;

                public Default(ByteCodeAppender byteCodeAppender, MethodAttributeAppender methodAttributeAppender) {
                    this.byteCodeAppender = byteCodeAppender;
                    this.methodAttributeAppender = methodAttributeAppender;
                }

                @Override
                public ByteCodeAppender getByteCodeAppender() {
                    return byteCodeAppender;
                }

                @Override
                public MethodAttributeAppender getAttributeAppender() {
                    return methodAttributeAppender;
                }
            }

            ByteCodeAppender getByteCodeAppender();

            MethodAttributeAppender getAttributeAppender();
        }

        Entry target(MethodDescription methodDescription);
    }

    static interface LatentDecision {

        static class Simple implements LatentDecision {

            private final MethodMatcher methodMatcher;

            public Simple(MethodMatcher methodMatcher) {
                this.methodMatcher = methodMatcher;
            }

            @Override
            public MethodMatcher manifest(TypeDescription instrumentedType) {
                return methodMatcher;
            }
        }

        MethodMatcher manifest(TypeDescription typeDescription);
    }

    static class Default implements InterceptionRegistry {

        private static class Compiled implements InterceptionRegistry.Compiled {

            private static class Entry implements InterceptionRegistry.Compiled.Entry, MethodMatcher {

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

            private final List<Entry> entries;

            private Compiled(List<Entry> entries) {
                this.entries = entries;
            }

            @Override
            public Entry target(MethodDescription methodDescription) {
                for (Entry entry : entries) {
                    if (entry.matches(methodDescription)) {
                        return entry;
                    }
                }
                throw new IllegalArgumentException();
            }
        }

        private static class Entry {

            private final LatentDecision latentDecision;
            private final Instrumentation instrumentation;
            private final MethodAttributeAppender.Factory attributeAppenderFactory;

            private Entry(LatentDecision latentDecision,
                          Instrumentation instrumentation,
                          MethodAttributeAppender.Factory attributeAppenderFactory) {
                this.latentDecision = latentDecision;
                this.instrumentation = instrumentation;
                this.attributeAppenderFactory = attributeAppenderFactory;
            }
        }

        private final List<Entry> entries;

        private Default(List<Entry> entries) {
            this.entries = entries;
        }

        @Override
        public InterceptionRegistry prepend(LatentDecision latentDecision,
                                            Instrumentation instrumentation,
                                            MethodAttributeAppender.Factory attributeAppenderFactory) {
            List<Entry> entries = new ArrayList<Entry>(this.entries.size() + 1);
            entries.add(new Entry(latentDecision, instrumentation, attributeAppenderFactory));
            entries.addAll(this.entries);
            return new Default(entries);
        }

        @Override
        public Compiled compile(InstrumentedType instrumentedType, Instrumentation.Context context) {
            Map<Instrumentation, Entry> prepared = new HashMap<Instrumentation, Entry>(entries.size());
            for (Entry entry : entries) {
                if (!prepared.containsKey(entry.instrumentation)) {
                    instrumentedType = entry.instrumentation.prepare(instrumentedType);
                    prepared.put(entry.instrumentation, entry);
                }
            }
            List<InterceptionRegistry.Default.Compiled.Entry> compiledEntries = new ArrayList<InterceptionRegistry.Default.Compiled.Entry>(prepared.size());
            for (Entry entry : entries) {
                MethodMatcher methodMatcher = entry.latentDecision.manifest(instrumentedType);
                ByteCodeAppender byteCodeAppender = prepared.get(entry.instrumentation).instrumentation.appender(instrumentedType, context);
                MethodAttributeAppender attributeAppender = entry.attributeAppenderFactory.make(instrumentedType);
                compiledEntries.add(new Compiled.Entry(methodMatcher, byteCodeAppender, attributeAppender));
            }
            return new Compiled(compiledEntries);
        }
    }

    InterceptionRegistry prepend(LatentDecision latentDecision,
                                 Instrumentation instrumentation,
                                 MethodAttributeAppender.Factory attributeAppenderFactory);

    Compiled compile(InstrumentedType instrumentedType, Instrumentation.Context context);
}
