package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;

import java.util.*;

public interface MethodGraph {

    interface Factory {

        MethodGraph make(TypeDescription typeDescription);

        class Default implements Factory {

            @Override
            public MethodGraph make(TypeDescription typeDescription) {
                return new KeyStoreMethodGraph(doAnalyze(typeDescription));
            }

            protected Key.Store analyze(GenericTypeDescription typeDescription) {
                return typeDescription == null
                        ? new Key.Store()
                        : doAnalyze(typeDescription);
            }

            protected Key.Store doAnalyze(GenericTypeDescription typeDescription) {
                Key.Store keyStore = analyze(typeDescription.getSuperType());
                for (GenericTypeDescription interfaceType : typeDescription.getInterfaces()) {
                    keyStore.mergeWith(doAnalyze(interfaceType));
                }
                for (MethodDescription methodDescription : typeDescription.getDeclaredMethods()) {
                    keyStore = keyStore.register(methodDescription);
                }
                return keyStore;
            }

            protected static class KeyStoreMethodGraph implements MethodGraph {

                private final Key.Store keyStore;

                protected KeyStoreMethodGraph(Key.Store keyStore) {
                    this.keyStore = keyStore;
                }
            }

            protected static class Key {

                public static Key of(MethodDescription methodDescription) {
                    return new Key(methodDescription.getInternalName(), Collections.singleton(methodDescription.asToken()));
                }

                private final String internalName;

                private final Set<MethodDescription.Token> tokens;

                protected Key(String internalName, Set<MethodDescription.Token> tokens) {
                    this.internalName = internalName;
                    this.tokens = tokens;
                }

                protected Key expandWith(MethodDescription.InDefinedShape methodDescription) {
                    Set<MethodDescription.Token> keys = new HashSet<MethodDescription.Token>(this.tokens);
                    keys.add(methodDescription.asToken());
                    return new Key(internalName, keys);
                }

                protected Key mergeWith(Key key) {
                    Set<MethodDescription.Token> keys = new HashSet<MethodDescription.Token>(this.tokens);
                    keys.addAll(key.tokens);
                    return new Key(internalName, keys);
                }

                @Override
                public boolean equals(Object other) {
                    return other == this || (other instanceof Key
                            && !Collections.disjoint(tokens, ((Key) other).tokens));
                }

                @Override
                public int hashCode() {
                    return internalName.hashCode();
                }

                protected static class Store {

                    private final Map<Key, Entry> entries;

                    protected Store() {
                        this(Collections.<Key, Entry>emptyMap());
                    }

                    private Store(Map<Key, Entry> entries) {
                        this.entries = entries;
                    }

                    protected Store register(MethodDescription methodDescription) {
                        Key key = Key.of(methodDescription);
                        Entry currentEntry = entries.get(key);
                        Entry expandedEntry = (currentEntry == null
                                ? new Entry(key, methodDescription)
                                : currentEntry).expand(methodDescription);
                        Map<Key, Entry> entries = new HashMap<Key, Entry>(this.entries);
                        entries.put(expandedEntry.getKey(), expandedEntry);
                        return new Store(entries);
                    }

                    protected Store mergeWith(Store keyStore) {
                        for (Entry entry : keyStore.entries.values()) {
                            keyStore = keyStore.inject(entry);
                        }
                        return keyStore;
                    }

                    protected Store inject(Entry entry) {
                        Map<Key, Entry> entries = new HashMap<Key, Entry>(this.entries);
                        Entry dominantEntry = entries.get(entry.getKey());
                        Entry mergedEntry = dominantEntry == null
                                ? entry
                                : dominantEntry.mergeWith(entry);
                        entries.put(mergedEntry.getKey(), mergedEntry);
                        return new Store(entries);
                    }

                    protected static class Entry {

                        private final Key key;

                        private final MethodDescription methodDescription;

                        protected Entry(Key key, MethodDescription methodDescription) {
                            this.key = key;
                            this.methodDescription = methodDescription;
                        }

                        protected Entry expand(MethodDescription methodDescription) {
                            return new Entry(key.expandWith(methodDescription.asDefined()), methodDescription);
                        }

                        protected Entry mergeWith(Entry entry) {
                            return new Entry(key.mergeWith(entry.getKey()), methodDescription);
                        }

                        protected Key getKey() {
                            return key;
                        }
                    }
                }
            }
        }
    }
}
