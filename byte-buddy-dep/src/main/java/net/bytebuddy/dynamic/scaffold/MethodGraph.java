package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;

import java.util.*;

public interface MethodGraph extends MethodLookupEngine.Finding {

    interface Factory {

        MethodGraph make(TypeDescription typeDescription);

        class Default implements Factory {

            @Override
            public MethodGraph make(TypeDescription typeDescription) {
                return null;
            }

            protected static class Key {

                public static Key of(MethodDescription methodDescription) {
                    return new Key(methodDescription.getInternalName(), Collections.singleton(methodDescription.asToken()));
                }

                private final String internalName;

                private final Set<MethodDescription.Token> keys;

                protected Key(String internalName, Set<MethodDescription.Token> keys) {
                    this.internalName = internalName;
                    this.keys = keys;
                }

                protected Key expand(MethodDescription.InDefinedShape methodDescription) {
                    Set<MethodDescription.Token> keys = new HashSet<MethodDescription.Token>(this.keys);
                    keys.add(methodDescription.asToken());
                    return new Key(internalName, keys);
                }

                @Override
                public boolean equals(Object other) {
                    return other == this || (other instanceof Key
                            && !Collections.disjoint(keys, ((Key) other).keys));
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

                    protected static class Entry {

                        private final Key key;

                        private final MethodDescription methodDescription;

                        protected Entry(Key key, MethodDescription methodDescription) {
                            this.key = key;
                            this.methodDescription = methodDescription;
                        }

                        protected Entry expand(MethodDescription methodDescription) {
                            return new Entry(key.expand(methodDescription.asDefined()), methodDescription);
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
