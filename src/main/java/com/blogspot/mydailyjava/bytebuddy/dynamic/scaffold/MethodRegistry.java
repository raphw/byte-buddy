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

public interface MethodRegistry {

    static interface Compiled {

        static interface Entry {

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
            }

            boolean isDefineMethod();

            ByteCodeAppender getByteCodeAppender();

            MethodAttributeAppender getAttributeAppender();
        }

        Entry target(MethodDescription methodDescription);
    }

    static interface LatentMethodMatcher {

        static class Simple implements LatentMethodMatcher {

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

    static class Default implements MethodRegistry {

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

            private final List<Entry> entries;
            private final MethodRegistry.Compiled.Entry fallback;

            private Compiled(List<Entry> entries, MethodRegistry.Compiled.Entry fallback) {
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

        private static class ListDifferenceMethodMatcher implements MethodMatcher {

            private final MethodList matchedMethods;

            public ListDifferenceMethodMatcher(MethodList beforeMethods, MethodList afterMethods) {
                matchedMethods = afterMethods.subList(beforeMethods.size(), afterMethods.size());
            }

            @Override
            public boolean matches(MethodDescription methodDescription) {
                return matchedMethods.filter(describedBy(methodDescription)).size() == 1;
            }
        }

        private final List<Entry> entries;

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
            List<Entry> entries = new ArrayList<Entry>(this.entries.size() + 1);
            entries.addAll(this.entries);
            entries.add(new Entry(latentMethodMatcher, instrumentation, attributeAppenderFactory));
            return new Default(entries);
        }

        @Override
        public MethodRegistry prepend(LatentMethodMatcher latentMethodMatcher,
                                      Instrumentation instrumentation,
                                      MethodAttributeAppender.Factory attributeAppenderFactory) {
            List<Entry> entries = new ArrayList<Entry>(this.entries.size() + 1);
            entries.add(new Entry(latentMethodMatcher, instrumentation, attributeAppenderFactory));
            entries.addAll(this.entries);
            return new Default(entries);
        }

        @Override
        public MethodRegistry.Compiled compile(InstrumentedType instrumentedType, MethodRegistry.Compiled.Entry fallback) {
            Map<Instrumentation, Entry> prepared = new HashMap<Instrumentation, Entry>(entries.size());
            LinkedHashMap<Instrumentation, MethodMatcher> instrumentationMethods = new LinkedHashMap<Instrumentation, MethodMatcher>();
            for (Entry entry : entries) {
                if (!prepared.containsKey(entry.instrumentation)) {
                    MethodList beforeMethods = instrumentedType.getDeclaredMethods();
                    instrumentedType = entry.instrumentation.prepare(instrumentedType);
                    prepared.put(entry.instrumentation, entry);
                    if (beforeMethods.size() < instrumentedType.getDeclaredMethods().size()) {
                        instrumentationMethods.put(entry.instrumentation,
                                new ListDifferenceMethodMatcher(beforeMethods, instrumentedType.getDeclaredMethods()));
                    }
                }
            }
            List<MethodRegistry.Default.Compiled.Entry> compiledEntries = new LinkedList<Compiled.Entry>();
            LinkedHashMap<Instrumentation, ByteCodeAppender> byteCodeAppenders = new LinkedHashMap<Instrumentation, ByteCodeAppender>(prepared.size());
            for (Entry entry : entries) {
                MethodMatcher methodMatcher = entry.latentMethodMatcher.manifest(instrumentedType);
                ByteCodeAppender byteCodeAppender = prepared.get(entry.instrumentation).instrumentation.appender(instrumentedType);
                MethodAttributeAppender attributeAppender = entry.attributeAppenderFactory.make(instrumentedType);
                compiledEntries.add(new Compiled.Entry(methodMatcher, byteCodeAppender, attributeAppender));
                byteCodeAppenders.put(entry.instrumentation, byteCodeAppender);
            }
            for (Map.Entry<Instrumentation, MethodMatcher> entry : instrumentationMethods.entrySet()) {
                compiledEntries.add(0, new Compiled.Entry(entry.getValue(),
                        byteCodeAppenders.get(entry.getKey()),
                        MethodAttributeAppender.NoOp.INSTANCE));
            }
            return new Compiled(new ArrayList<Compiled.Entry>(compiledEntries), fallback);
        }
    }

    MethodRegistry prepend(LatentMethodMatcher latentMethodMatcher,
                           Instrumentation instrumentation,
                           MethodAttributeAppender.Factory attributeAppenderFactory);

    MethodRegistry append(LatentMethodMatcher latentMethodMatcher,
                          Instrumentation instrumentation,
                          MethodAttributeAppender.Factory attributeAppenderFactory);

    Compiled compile(InstrumentedType instrumentedType, Compiled.Entry fallback);
}
