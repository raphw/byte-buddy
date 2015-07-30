package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import java.util.*;

import static net.bytebuddy.matcher.ElementMatchers.any;
import static net.bytebuddy.matcher.ElementMatchers.isVirtual;
import static net.bytebuddy.matcher.ElementMatchers.isVisibleTo;

public interface MethodGraph {

    Node locate(MethodDescription.Token methodToken);

    List<Node> listNodes();

    interface Linked extends MethodGraph {

        MethodGraph getSuperGraph();

        MethodGraph getInterfaceGraph(TypeDescription typeDescription);

        class Delegation implements Linked {

            private final MethodGraph methodGraph;

            private final MethodGraph superGraph;

            private final Map<TypeDescription, MethodGraph> interfaceGraphs;

            public Delegation(MethodGraph methodGraph, MethodGraph superGraph, Map<TypeDescription, MethodGraph> interfaceGraphs) {
                this.methodGraph = methodGraph;
                this.superGraph = superGraph;
                this.interfaceGraphs = interfaceGraphs;
            }

            @Override
            public MethodGraph getSuperGraph() {
                return superGraph;
            }

            @Override
            public MethodGraph getInterfaceGraph(TypeDescription typeDescription) {
                MethodGraph interfaceGraph = interfaceGraphs.get(typeDescription);
                return interfaceGraph == null
                        ? Illegal.INSTANCE
                        : interfaceGraph;
            }

            @Override
            public Node locate(MethodDescription.Token methodToken) {
                return methodGraph.locate(methodToken);
            }

            @Override
            public List<Node> listNodes() {
                return methodGraph.listNodes();
            }
        }
    }

    enum Sort {

        RESOLVED(true, true),

        AMBIGUOUS(true, false),

        UNRESOLVED(false, false);

        private final boolean resolved;

        private final boolean unique;

        Sort(boolean resolved, boolean unique) {
            this.resolved = resolved;
            this.unique = unique;
        }

        public boolean isResolved() {
            return resolved;
        }

        public boolean isUnique() {
            return unique;
        }
    }

    interface Node {

        Sort getSort();

        MethodDescription getRepresentative();

        Set<MethodDescription.Token> getBridges();

        boolean isMadeVisible();

        enum Illegal implements Node {

            INSTANCE;

            @Override
            public Sort getSort() {
                return Sort.UNRESOLVED;
            }

            @Override
            public MethodDescription getRepresentative() {
                throw new IllegalStateException("Cannot resolve the method of an illegal node");
            }

            @Override
            public Set<MethodDescription.Token> getBridges() {
                throw new IllegalStateException("Cannot resolve bridge method of an illegal node");
            }

            @Override
            public boolean isMadeVisible() {
                throw new IllegalStateException("Cannot resolve visibility of an illegal node");
            }
        }
    }

    interface Compiler {

        MethodGraph.Linked make(TypeDescription typeDescription);

        class Default implements Compiler {

            private final Identifier.Factory identifierFactory;

            public Default(Identifier.Factory identifierFactory) {
                this.identifierFactory = identifierFactory;
            }

            @Override
            public MethodGraph.Linked make(TypeDescription typeDescription) {
                Map<GenericTypeDescription, Key.Store> snapshots = new HashMap<GenericTypeDescription, Key.Store>();
                GenericTypeDescription superType = typeDescription.getSuperType();
                List<GenericTypeDescription> interfaceTypes = typeDescription.getInterfaces();
                Map<TypeDescription, MethodGraph> interfaceGraphs = new HashMap<TypeDescription, MethodGraph>(interfaceTypes.size());
                for (GenericTypeDescription interfaceType : interfaceTypes) {
                    interfaceGraphs.put(interfaceType.asRawType(), snapshots.get(interfaceType));
                }
                return new Linked.Delegation(analyze(typeDescription, snapshots, any(), isVirtual().and(isVisibleTo(typeDescription))),
                        superType == null
                                ? Illegal.INSTANCE
                                : snapshots.get(superType),
                        interfaceGraphs);
            }

            protected Key.Store analyze(GenericTypeDescription typeDescription,
                                        Map<GenericTypeDescription, Key.Store> snapshots,
                                        ElementMatcher<? super MethodDescription> currentMatcher,
                                        ElementMatcher<? super MethodDescription> nextMatcher) {
                Key.Store keyStore = snapshots.get(typeDescription);
                if (keyStore == null) {
                    keyStore = doAnalyze(typeDescription, snapshots, currentMatcher, nextMatcher);
                    snapshots.put(typeDescription, keyStore);
                }
                return keyStore;
            }

            protected Key.Store analyzeNullable(GenericTypeDescription typeDescription,
                                                Map<GenericTypeDescription, Key.Store> snapshots,
                                                ElementMatcher<? super MethodDescription> currentMatcher,
                                                ElementMatcher<? super MethodDescription> nextMatcher) {
                return typeDescription == null
                        ? new Key.Store()
                        : analyze(typeDescription, snapshots, currentMatcher, nextMatcher);
            }

            protected Key.Store doAnalyze(GenericTypeDescription typeDescription,
                                          Map<GenericTypeDescription, Key.Store> snapshots,
                                          ElementMatcher<? super MethodDescription> currentMatcher,
                                          ElementMatcher<? super MethodDescription> nextMatcher) {
                Key.Store keyStore = analyzeNullable(typeDescription.getSuperType(), snapshots, nextMatcher, nextMatcher);
                for (GenericTypeDescription interfaceType : typeDescription.getInterfaces()) {
                    keyStore = keyStore.mergeWith(analyze(interfaceType, snapshots, nextMatcher, nextMatcher));
                }
                for (MethodDescription methodDescription : typeDescription.getDeclaredMethods().filter(currentMatcher)) {
                    keyStore = keyStore.registerTopLevel(methodDescription);
                }
                return keyStore;
            }

            public interface Identifier {

                MethodDescription.Token getToken();

                interface Factory<T extends Identifier> {

                    T wrap(MethodDescription.Token token);

                    enum ForJavaMethod implements Factory<Identifier.ForJavaMethod> {

                        INSTANCE;

                        @Override
                        public Identifier.ForJavaMethod wrap(MethodDescription.Token token) {
                            return new Identifier.ForJavaMethod(token);
                        }
                    }
                }

                class ForJavaMethod implements Identifier {

                    private final MethodDescription.Token token;

                    protected ForJavaMethod(MethodDescription.Token token) {
                        this.token = token;
                    }

                    @Override
                    public MethodDescription.Token getToken() {
                        return token;
                    }

                    @Override
                    public int hashCode() {
                        int result = token.getInternalName().hashCode();
                        for (ParameterDescription.Token parameterToken : token.getParameterTokens()) {
                            result = 31 * result + parameterToken.getType().asRawType().hashCode();
                        }
                        return result;
                    }

                    @Override
                    public boolean equals(Object other) {
                        if (this == other) return true;
                        if (!(other instanceof ForJavaMethod)) return false;
                        ForJavaMethod forJavaMethod = (ForJavaMethod) other;
                        if (!token.getInternalName().equals(forJavaMethod.token.getInternalName())) return false;
                        List<ParameterDescription.Token> tokens = token.getParameterTokens(), otherTokens = forJavaMethod.token.getParameterTokens();
                        if (tokens.size() != otherTokens.size()) return false;
                        for (int index = 0; index < tokens.size(); index++) {
                            if (!tokens.get(index).getType().asRawType().equals(otherTokens.get(index).getType().asRawType())) return false;
                        }
                        return true;
                    }
                }
            }

            protected static class Key {

                public static Key of(MethodDescription methodDescription) {
                    return new Key(methodDescription.getInternalName(), Collections.singleton(methodDescription.asToken()));
                }

                private final String internalName;

                private final Set<MethodDescription.Token> tokens;

                protected Key(String internalName, MethodDescription.Token token) {
                    this(internalName, Collections.singleton(token));
                }

                private Key(String internalName, Set<MethodDescription.Token> tokens) {
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

                protected Set<MethodDescription.Token> findBridges(MethodDescription.Token methodToken) {
                    Set<MethodDescription.Token> tokens = new HashSet<MethodDescription.Token>(this.tokens);
                    tokens.remove(methodToken);
                    return tokens;
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

                protected static class Store implements MethodGraph {

                    private static final int EMPTY = 0;

                    private final LinkedHashMap<Key, Entry> entries;

                    protected Store() {
                        this(new LinkedHashMap<Key, Entry>(EMPTY));
                    }

                    private Store(LinkedHashMap<Key, Entry> entries) {
                        this.entries = entries;
                    }

                    protected Store registerTopLevel(MethodDescription methodDescription) {
                        Key key = Key.of(methodDescription);
                        Entry currentEntry = entries.get(key);
                        Entry expandedEntry = (currentEntry == null
                                ? new Entry.ForMethod(key, methodDescription)
                                : currentEntry).expandWith(methodDescription);
                        LinkedHashMap<Key, Entry> entries = new LinkedHashMap<Key, Entry>(this.entries);
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
                        LinkedHashMap<Key, Entry> entries = new LinkedHashMap<Key, Entry>(this.entries);
                        Entry dominantEntry = entries.get(entry.getKey());
                        Entry mergedEntry = dominantEntry == null
                                ? entry
                                : dominantEntry.mergeWith(entry.getKey());
                        entries.put(mergedEntry.getKey(), mergedEntry);
                        return new Store(entries);
                    }

                    @Override
                    public Node locate(MethodDescription.Token methodToken) {
                        Entry entry = entries.get(new Key(methodToken.getInternalName(), methodToken));
                        return entry == null
                                ? Node.Illegal.INSTANCE
                                : entry;
                    }

                    @Override
                    public List<Node> listNodes() {
                        return new ArrayList<Node>(entries.values());
                    }

                    protected interface Entry extends Node {

                        Key getKey();

                        Entry expandWith(MethodDescription methodDescription);

                        Entry mergeWith(Key key);

                        class Ambiguous implements Entry {

                            private final Key key;

                            private final TypeDescription declaringType;

                            private final MethodDescription.Token methodToken;

                            protected static Entry of(Key key, MethodDescription firstMethod, MethodDescription secondMethod) {
                                return firstMethod.isBridge() ^ secondMethod.isBridge()
                                        ? new ForMethod(key, firstMethod.isBridge() ? secondMethod : firstMethod)
                                        : new Ambiguous(key, firstMethod.getDeclaringType().asRawType(), secondMethod.asToken());
                            }

                            protected Ambiguous(Key key, TypeDescription declaringType, MethodDescription.Token methodToken) {
                                this.key = key;
                                this.declaringType = declaringType;
                                this.methodToken = methodToken;
                            }

                            @Override
                            public Key getKey() {
                                return key;
                            }

                            @Override
                            public Entry expandWith(MethodDescription methodDescription) {
                                Key key = this.key.expandWith(methodDescription.asDefined());
                                if (methodDescription.getDeclaringType().asRawType().equals(declaringType)) {
                                    return methodToken.isBridge() ^ methodDescription.isBridge()
                                            ? methodToken.isBridge() ? new ForMethod(key, methodDescription) : new Ambiguous(key, declaringType, methodToken)
                                            : new Ambiguous(key, declaringType, methodDescription.asToken());
                                } else {
                                    return methodDescription.isBridge()
                                            ? new Ambiguous(key, declaringType, methodToken)
                                            : new ForMethod(key, methodDescription);
                                }
                            }

                            @Override
                            public Entry mergeWith(Key key) {
                                return new Ambiguous(key.mergeWith(key), declaringType, methodToken);
                            }

                            @Override
                            public Sort getSort() {
                                return Sort.AMBIGUOUS;
                            }

                            @Override
                            public MethodDescription getRepresentative() {
                                return new MethodDescription.Latent(declaringType, methodToken);
                            }

                            @Override
                            public Set<MethodDescription.Token> getBridges() {
                                return key.findBridges(methodToken);
                            }

                            @Override
                            public boolean isMadeVisible() {
                                return false;
                            }
                        }

                        class ForMethod implements Entry {

                            private final Key key;

                            private final MethodDescription methodDescription;

                            private final boolean madeVisible;

                            protected ForMethod(Key key, MethodDescription methodDescription) {
                                this(key, methodDescription, false);
                            }

                            protected ForMethod(Key key, MethodDescription methodDescription, boolean madeVisible) {
                                this.key = key;
                                this.methodDescription = methodDescription;
                                this.madeVisible = madeVisible;
                            }

                            @Override
                            public Entry expandWith(MethodDescription methodDescription) {
                                Key key = this.key.expandWith(methodDescription.asDefined());
                                return methodDescription.getDeclaringType().equals(this.methodDescription.getDeclaringType())
                                        ? Ambiguous.of(key, methodDescription, this.methodDescription)
                                        : new ForMethod(key, methodDescription.isBridge() ? this.methodDescription : methodDescription, true);
                            }

                            @Override
                            public Entry mergeWith(Key key) {
                                return new Entry.ForMethod(key.mergeWith(key), methodDescription, madeVisible);
                            }

                            @Override
                            public Key getKey() {
                                return key;
                            }

                            @Override
                            public Sort getSort() {
                                return Sort.RESOLVED;
                            }

                            @Override
                            public MethodDescription getRepresentative() {
                                return methodDescription;
                            }

                            @Override
                            public Set<MethodDescription.Token> getBridges() {
                                return key.findBridges(methodDescription.asToken());
                            }

                            @Override
                            public boolean isMadeVisible() {
                                return true;
                            }
                        }
                    }
                }
            }
        }
    }

    enum Illegal implements MethodGraph.Linked {

        INSTANCE;

        @Override
        public Node locate(MethodDescription.Token methodToken) {
            return Node.Illegal.INSTANCE;
        }

        @Override
        public List<Node> listNodes() {
            return Collections.emptyList();
        }

        @Override
        public MethodGraph getSuperGraph() {
            return this;
        }

        @Override
        public MethodGraph getInterfaceGraph(TypeDescription typeDescription) {
            return this;
        }
    }
}
