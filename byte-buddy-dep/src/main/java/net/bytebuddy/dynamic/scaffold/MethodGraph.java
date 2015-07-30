package net.bytebuddy.dynamic.scaffold;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.generic.GenericTypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import java.util.*;

import static net.bytebuddy.matcher.ElementMatchers.*;

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
                        ? Empty.INSTANCE
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

            @Override
            public boolean equals(Object other) {
                if (this == other) return true;
                if (other == null || getClass() != other.getClass()) return false;
                Delegation that = (Delegation) other;
                return methodGraph.equals(that.methodGraph)
                        && superGraph.equals(that.superGraph)
                        && interfaceGraphs.equals(that.interfaceGraphs);
            }

            @Override
            public int hashCode() {
                int result = methodGraph.hashCode();
                result = 31 * result + superGraph.hashCode();
                result = 31 * result + interfaceGraphs.hashCode();
                return result;
            }

            @Override
            public String toString() {
                return "MethodGraph.Linked.Delegation{" +
                        "methodGraph=" + methodGraph +
                        ", superGraph=" + superGraph +
                        ", interfaceGraphs=" + interfaceGraphs +
                        '}';
            }
        }
    }

    interface Node {

        Sort getSort();

        MethodDescription getRepresentative();

        Set<MethodDescription.Token> getBridges();

        boolean isMadeVisible();

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

            @Override
            public String toString() {
                return "MethodGraph.Node.Sort." + name();
            }
        }

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

            @Override
            public String toString() {
                return "MethodGraph.Node.Illegal." + name();
            }
        }
    }

    interface Compiler {

        MethodGraph.Linked make(TypeDescription typeDescription);

        class Default<T extends Default.Identifier> implements Compiler {

            public static Compiler forJavaHierarchy() {
                return new Default<Identifier.ForJavaMethod>(Identifier.Factory.ForJavaMethod.INSTANCE);
            }

            public static Compiler forJVMHierarchy() {
                return new Default<Identifier.ForJVMMethod>(Identifier.Factory.ForJVMMethod.INSTANCE);
            }

            private final Identifier.Factory<T> identifierFactory;

            public Default(Identifier.Factory<T> identifierFactory) {
                this.identifierFactory = identifierFactory;
            }

            @Override
            public MethodGraph.Linked make(TypeDescription typeDescription) {
                Map<GenericTypeDescription, Key.Store<T>> snapshots = new HashMap<GenericTypeDescription, Key.Store<T>>();
                Key.Store<?> rootStore = analyze(typeDescription, snapshots, any(), isVirtual().and(isVisibleTo(typeDescription)));
                GenericTypeDescription superType = typeDescription.getSuperType();
                List<GenericTypeDescription> interfaceTypes = typeDescription.getInterfaces();
                Map<TypeDescription, MethodGraph> interfaceGraphs = new HashMap<TypeDescription, MethodGraph>(interfaceTypes.size());
                for (GenericTypeDescription interfaceType : interfaceTypes) {
                    interfaceGraphs.put(interfaceType.asRawType(), snapshots.get(interfaceType).asGraph());
                }
                return new Linked.Delegation(rootStore.asGraph(),
                        superType == null
                                ? Empty.INSTANCE
                                : snapshots.get(superType).asGraph(),
                        interfaceGraphs);
            }

            protected Key.Store<T> analyze(GenericTypeDescription typeDescription,
                                           Map<GenericTypeDescription, Key.Store<T>> snapshots,
                                           ElementMatcher<? super MethodDescription> currentMatcher,
                                           ElementMatcher<? super MethodDescription> nextMatcher) {
                Key.Store<T> keyStore = snapshots.get(typeDescription);
                if (keyStore == null) {
                    keyStore = doAnalyze(typeDescription, snapshots, currentMatcher, nextMatcher);
                    snapshots.put(typeDescription, keyStore);
                }
                return keyStore;
            }

            protected Key.Store<T> analyzeNullable(GenericTypeDescription typeDescription,
                                                   Map<GenericTypeDescription, Key.Store<T>> snapshots,
                                                   ElementMatcher<? super MethodDescription> currentMatcher,
                                                   ElementMatcher<? super MethodDescription> nextMatcher) {
                return typeDescription == null
                        ? new Key.Store<T>()
                        : analyze(typeDescription, snapshots, currentMatcher, nextMatcher);
            }

            protected Key.Store<T> doAnalyze(GenericTypeDescription typeDescription,
                                             Map<GenericTypeDescription, Key.Store<T>> snapshots,
                                             ElementMatcher<? super MethodDescription> currentMatcher,
                                             ElementMatcher<? super MethodDescription> nextMatcher) {
                Key.Store<T> keyStore = analyzeNullable(typeDescription.getSuperType(), snapshots, nextMatcher, nextMatcher);
                for (GenericTypeDescription interfaceType : typeDescription.getInterfaces()) {
                    keyStore = keyStore.mergeWith(analyze(interfaceType, snapshots, nextMatcher, nextMatcher));
                }
                for (MethodDescription methodDescription : typeDescription.getDeclaredMethods().filter(currentMatcher)) {
                    keyStore = keyStore.registerTopLevel(methodDescription, identifierFactory);
                }
                return keyStore;
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && identifierFactory.equals(((Default<?>) other).identifierFactory);
            }

            @Override
            public int hashCode() {
                return identifierFactory.hashCode();
            }

            @Override
            public String toString() {
                return "MethodGraph.Compiler.Default{" +
                        "identifierFactory=" + identifierFactory +
                        '}';
            }

            public interface Identifier {

                MethodDescription.Token getToken();

                interface Factory<T extends Identifier> {

                    T wrap(MethodDescription.Token methodToken);

                    enum ForJavaMethod implements Factory<Identifier.ForJavaMethod> {

                        INSTANCE;

                        @Override
                        public Identifier.ForJavaMethod wrap(MethodDescription.Token methodToken) {
                            return new Identifier.ForJavaMethod(methodToken);
                        }

                        @Override
                        public String toString() {
                            return "MethodGraph.Compiler.Default.Identifier.Factory.ForJavaMethod." + name();
                        }
                    }

                    enum ForJVMMethod implements Factory<Identifier.ForJVMMethod> {

                        INSTANCE;

                        @Override
                        public Identifier.ForJVMMethod wrap(MethodDescription.Token methodToken) {
                            return new Identifier.ForJVMMethod(methodToken);
                        }

                        @Override
                        public String toString() {
                            return "MethodGraph.Compiler.Default.Identifier.Factory.ForJVMMethod." + name();
                        }
                    }
                }

                class ForJavaMethod implements Identifier {

                    private final MethodDescription.Token methodToken;

                    protected ForJavaMethod(MethodDescription.Token methodToken) {
                        this.methodToken = methodToken;
                    }

                    @Override
                    public MethodDescription.Token getToken() {
                        return methodToken;
                    }

                    @Override
                    public boolean equals(Object other) {
                        if (this == other) return true;
                        if (!(other instanceof ForJavaMethod)) return false;
                        ForJavaMethod forJavaMethod = (ForJavaMethod) other;
                        List<ParameterDescription.Token> tokens = methodToken.getParameterTokens(), otherTokens = forJavaMethod.methodToken.getParameterTokens();
                        if (tokens.size() != otherTokens.size()) return false;
                        for (int index = 0; index < tokens.size(); index++) {
                            if (!tokens.get(index).getType().asRawType().equals(otherTokens.get(index).getType().asRawType())) return false;
                        }
                        return true;
                    }

                    @Override
                    public int hashCode() {
                        int result = 17;
                        for (ParameterDescription.Token parameterToken : methodToken.getParameterTokens()) {
                            result = 31 * result + parameterToken.getType().asRawType().hashCode();
                        }
                        return result;
                    }

                    @Override
                    public String toString() {
                        return "MethodGraph.Compiler.Default.Identifier.ForJavaMethod{" +
                                "methodToken=" + methodToken +
                                '}';
                    }
                }

                class ForJVMMethod implements Identifier {

                    private final MethodDescription.Token methodToken;

                    public ForJVMMethod(MethodDescription.Token methodToken) {
                        this.methodToken = methodToken;
                    }

                    @Override
                    public MethodDescription.Token getToken() {
                        return methodToken;
                    }

                    @Override
                    public boolean equals(Object other) {
                        if (this == other) return true;
                        if (!(other instanceof ForJVMMethod)) return false;
                        ForJVMMethod forJavaMethod = (ForJVMMethod) other;
                        if (!methodToken.getReturnType().asRawType().equals(forJavaMethod.methodToken.getReturnType().asRawType())) return false;
                        List<ParameterDescription.Token> tokens = methodToken.getParameterTokens(), otherTokens = forJavaMethod.methodToken.getParameterTokens();
                        if (tokens.size() != otherTokens.size()) return false;
                        for (int index = 0; index < tokens.size(); index++) {
                            if (!tokens.get(index).getType().asRawType().equals(otherTokens.get(index).getType().asRawType())) return false;
                        }
                        return true;
                    }

                    @Override
                    public int hashCode() {
                        int result = methodToken.getReturnType().asRawType().hashCode();
                        for (ParameterDescription.Token parameterToken : methodToken.getParameterTokens()) {
                            result = 31 * result + parameterToken.getType().asRawType().hashCode();
                        }
                        return result;
                    }

                    @Override
                    public String toString() {
                        return "MethodGraph.Compiler.Default.Identifier.ForJVMMethod{" +
                                "methodToken=" + methodToken +
                                '}';
                    }
                }
            }

            protected static class Key<S> {

                protected final String internalName;

                protected final Set<S> identifiers;

                protected Key(String internalName, Set<S> identifiers) {
                    this.internalName = internalName;
                    this.identifiers = identifiers;
                }

                protected Set<S> resolveBridges(S excluded) {
                    Set<S> tokens = new HashSet<S>(this.identifiers);
                    tokens.remove(excluded);
                    return tokens;
                }

                @Override
                public boolean equals(Object other) {
                    return other == this || (other instanceof Key
                            && internalName.equals(((Key) other).internalName)
                            && !Collections.disjoint(identifiers, ((Key) other).identifiers));
                }

                @Override
                public int hashCode() {
                    return internalName.hashCode();
                }

                @Override
                public String toString() {
                    return "MethodGraph.Compiler.Default.Key{" +
                            "internalName='" + internalName + '\'' +
                            ", identifiers=" + identifiers +
                            '}';
                }

                protected static class Identifying<V extends Identifier> extends Key<V> {

                    public static <Q extends Identifier> Key.Identifying<Q> of(MethodDescription.InDefinedShape methodDescription, Identifier.Factory<Q> factory) {
                        return new Key.Identifying<Q>(methodDescription.getInternalName(), Collections.singleton(factory.wrap(methodDescription.asToken())));
                    }

                    protected Identifying(String internalName, Set<V> identifiers) {
                        super(internalName, identifiers);
                    }

                    protected Key<MethodDescription.Token> asTokenKey() {
                        Set<MethodDescription.Token> identifiers = new HashSet<MethodDescription.Token>(this.identifiers.size());
                        for (V identifier : this.identifiers) {
                            identifiers.add(identifier.getToken());
                        }
                        return new Key<MethodDescription.Token>(internalName, identifiers);
                    }

                    protected Key.Identifying<V> expandWith(MethodDescription methodDescription, Identifier.Factory<V> factory) {
                        Set<V> keys = new HashSet<V>(this.identifiers);
                        keys.add(factory.wrap(methodDescription.asToken()));
                        return new Key.Identifying<V>(internalName, keys);
                    }

                    protected Key.Identifying<V> mergeWith(Key.Identifying<V> key) {
                        Set<V> keys = new HashSet<V>(this.identifiers);
                        keys.addAll(key.identifiers);
                        return new Key.Identifying<V>(internalName, keys);
                    }

                    @Override
                    public String toString() {
                        return "MethodGraph.Compiler.Default.Key.Identifying{" +
                                "internalName='" + internalName + '\'' +
                                ", identifiers=" + identifiers +
                                '}';
                    }
                }

                protected static class Store<V extends Identifier> {

                    private static final int EMPTY = 0;

                    private final LinkedHashMap<Key.Identifying<V>, Entry<V>> entries;

                    protected Store() {
                        this(new LinkedHashMap<Key.Identifying<V>, Entry<V>>(EMPTY));
                    }

                    private Store(LinkedHashMap<Key.Identifying<V>, Entry<V>> entries) {
                        this.entries = entries;
                    }

                    protected Store<V> registerTopLevel(MethodDescription methodDescription, Identifier.Factory<V> factory) {
                        Key.Identifying<V> key = Key.Identifying.of(methodDescription.asDefined(), factory);
                        Entry<V> currentEntry = entries.get(key);
                        Entry<V> expandedEntry = (currentEntry == null
                                ? new Entry.Initial<V>(key)
                                : currentEntry).expandWith(methodDescription, factory);
                        LinkedHashMap<Key.Identifying<V>, Entry<V>> entries = new LinkedHashMap<Key.Identifying<V>, Entry<V>>(this.entries);
                        entries.put(expandedEntry.getKey(), expandedEntry);
                        return new Store<V>(entries);
                    }

                    protected Store<V> mergeWith(Store<V> keyStore) {
                        Store<V> mergedStore = this;
                        for (Entry<V> entry : keyStore.entries.values()) {
                            mergedStore = mergedStore.inject(entry);
                        }
                        return mergedStore;
                    }

                    protected Store<V> inject(Entry<V> entry) {
                        LinkedHashMap<Key.Identifying<V>, Entry<V>> entries = new LinkedHashMap<Key.Identifying<V>, Entry<V>>(this.entries);
                        Entry<V> dominantEntry = entries.get(entry.getKey());
                        Entry<V> mergedEntry = dominantEntry == null
                                ? entry
                                : dominantEntry.mergeWith(entry.getKey());
                        entries.put(mergedEntry.getKey(), mergedEntry);
                        return new Store<V>(entries);
                    }

                    protected MethodGraph asGraph() {
                        LinkedHashMap<Key<MethodDescription.Token>, Node> entries = new LinkedHashMap<Key<MethodDescription.Token>, Node>(this.entries.size());
                        for (Entry<V> entry : this.entries.values()) {
                            entries.put(entry.getKey().asTokenKey(), entry.asNode());
                        }
                        return new Graph(entries);
                    }

                    @Override
                    public boolean equals(Object other) {
                        return this == other || !(other == null || getClass() != other.getClass())
                                && entries.equals(((Store<?>) other).entries);
                    }

                    @Override
                    public int hashCode() {
                        return entries.hashCode();
                    }

                    @Override
                    public String toString() {
                        return "MethodGraph.Compiler.Default.Key.Store{" +
                                "entries=" + entries +
                                '}';
                    }

                    protected static class Graph implements MethodGraph {

                        private final LinkedHashMap<Key<MethodDescription.Token>, Node> entries;

                        protected Graph(LinkedHashMap<Key<MethodDescription.Token>, Node> entries) {
                            this.entries = entries;
                        }

                        @Override
                        public Node locate(MethodDescription.Token methodToken) {
                            Node node = entries.get(new Key<MethodDescription.Token>(methodToken.getInternalName(), Collections.singleton(methodToken)));
                            return node == null
                                    ? Node.Illegal.INSTANCE
                                    : node;
                        }

                        @Override
                        public List<Node> listNodes() {
                            return new ArrayList<Node>(entries.values());
                        }

                        @Override
                        public boolean equals(Object other) {
                            return this == other || !(other == null || getClass() != other.getClass())
                                    && entries.equals(((Graph) other).entries);
                        }

                        @Override
                        public int hashCode() {
                            return entries.hashCode();
                        }

                        @Override
                        public String toString() {
                            return "MethodGraph.Compiler.Default.Key.Store.Graph{" +
                                    "entries=" + entries +
                                    '}';
                        }
                    }

                    protected interface Entry<W extends Identifier> {

                        Key.Identifying<W> getKey();

                        Entry<W> expandWith(MethodDescription methodDescription, Identifier.Factory<W> identifierFactory);

                        Entry<W> mergeWith(Key.Identifying<W> key);

                        Node asNode();

                        class Initial<U extends Identifier> implements Entry<U> {

                            private final Key.Identifying<U> key;

                            protected Initial(Key.Identifying<U> key) {
                                this.key = key;
                            }

                            @Override
                            public Identifying<U> getKey() {
                                return key;
                            }

                            @Override
                            public Entry<U> expandWith(MethodDescription methodDescription, Identifier.Factory<U> identifierFactory) {
                                return new ForMethod<U>(key.expandWith(methodDescription, identifierFactory), methodDescription, false);
                            }

                            @Override
                            public Entry<U> mergeWith(Identifying<U> key) {
                                throw new IllegalStateException("Cannot merge initial entry without a registered method: " + this);
                            }

                            @Override
                            public Node asNode() {
                                throw new IllegalStateException("Cannot transform initial entry without a registered method: " + this);
                            }

                            @Override
                            public boolean equals(Object other) {
                                return this == other || !(other == null || getClass() != other.getClass())
                                        && key.equals(((Initial<?>) other).key);
                            }

                            @Override
                            public int hashCode() {
                                return key.hashCode();
                            }

                            @Override
                            public String toString() {
                                return "MethodGraph.Compiler.Default.Key.Store.Entry.Initial{key=" + key + '}';
                            }
                        }

                        class ForMethod<U extends Identifier> implements Entry<U> {

                            private final Key.Identifying<U> key;

                            private final MethodDescription methodDescription;

                            private final boolean madeVisible;

                            protected ForMethod(Key.Identifying<U> key, MethodDescription methodDescription) {
                                this(key, methodDescription, false);
                            }

                            protected ForMethod(Key.Identifying<U> key, MethodDescription methodDescription, boolean madeVisible) {
                                this.key = key;
                                this.methodDescription = methodDescription;
                                this.madeVisible = madeVisible;
                            }

                            @Override
                            public Entry<U> expandWith(MethodDescription methodDescription, Identifier.Factory<U> identifierFactory) {
                                Key.Identifying<U> key = this.key.expandWith(methodDescription, identifierFactory);
                                return methodDescription.getDeclaringType().equals(this.methodDescription.getDeclaringType())
                                        ? Ambiguous.of(key, methodDescription, this.methodDescription)
                                        : new ForMethod<U>(key, methodDescription.isBridge() ? this.methodDescription : methodDescription, true);
                            }

                            @Override
                            public Entry<U> mergeWith(Key.Identifying<U> key) {
                                return new Entry.ForMethod<U>(key.mergeWith(key), methodDescription, madeVisible);
                            }

                            @Override
                            public Key.Identifying<U> getKey() {
                                return key;
                            }

                            @Override
                            public MethodGraph.Node asNode() {
                                return new Node(key.asTokenKey(), methodDescription);
                            }

                            @Override
                            public boolean equals(Object other) {
                                if (this == other) return true;
                                if (other == null || getClass() != other.getClass()) return false;
                                ForMethod<?> forMethod = (ForMethod<?>) other;
                                return madeVisible == forMethod.madeVisible
                                        && key.equals(forMethod.key)
                                        && methodDescription.equals(forMethod.methodDescription);
                            }

                            @Override
                            public int hashCode() {
                                int result = key.hashCode();
                                result = 31 * result + methodDescription.hashCode();
                                result = 31 * result + (madeVisible ? 1 : 0);
                                return result;
                            }

                            @Override
                            public String toString() {
                                return "MethodGraph.Compiler.Default.Key.Store.Entry.ForMethod{" +
                                        "key=" + key +
                                        ", methodDescription=" + methodDescription +
                                        ", madeVisible=" + madeVisible +
                                        '}';
                            }

                            protected static class Node implements MethodGraph.Node {

                                private final Key<MethodDescription.Token> key;

                                private final MethodDescription methodDescription;

                                public Node(Key<MethodDescription.Token> key, MethodDescription methodDescription) {
                                    this.key = key;
                                    this.methodDescription = methodDescription;
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
                                    return key.resolveBridges(methodDescription.asToken());
                                }

                                @Override
                                public boolean isMadeVisible() {
                                    return true;
                                }

                                @Override
                                public boolean equals(Object other) {
                                    return this == other || !(other == null || getClass() != other.getClass())
                                            && key.equals(((Node) other).key)
                                            && methodDescription.equals(((Node) other).methodDescription);
                                }

                                @Override
                                public int hashCode() {
                                    int result = key.hashCode();
                                    result = 31 * result + methodDescription.hashCode();
                                    return result;
                                }

                                @Override
                                public String toString() {
                                    return "MethodGraph.Compiler.Default.Key.Store.Entry.ForMethod.Node{" +
                                            "key=" + key +
                                            ", methodDescription=" + methodDescription +
                                            '}';
                                }
                            }
                        }

                        class Ambiguous<U extends Identifier> implements Entry<U> {

                            private final Key.Identifying<U> key;

                            private final TypeDescription declaringType;

                            private final MethodDescription.Token methodToken;

                            protected static <Q extends Identifier> Entry<Q> of(Key.Identifying<Q> key, MethodDescription left, MethodDescription right) {
                                return left.isBridge() ^ right.isBridge()
                                        ? new ForMethod<Q>(key, left.isBridge() ? right : left)
                                        : new Ambiguous<Q>(key, left.getDeclaringType().asRawType(), right.asToken());
                            }

                            protected Ambiguous(Key.Identifying<U> key, TypeDescription declaringType, MethodDescription.Token methodToken) {
                                this.key = key;
                                this.declaringType = declaringType;
                                this.methodToken = methodToken;
                            }

                            @Override
                            public Key.Identifying<U> getKey() {
                                return key;
                            }

                            @Override
                            public Entry<U> expandWith(MethodDescription methodDescription, Identifier.Factory<U> identifierFactory) {
                                Key.Identifying<U> key = this.key.expandWith(methodDescription, identifierFactory);
                                if (methodDescription.getDeclaringType().asRawType().equals(declaringType)) {
                                    return methodToken.isBridge() ^ methodDescription.isBridge()
                                            ? methodToken.isBridge() ? new ForMethod<U>(key, methodDescription) : new Ambiguous<U>(key, declaringType, methodToken)
                                            : new Ambiguous<U>(key, declaringType, methodDescription.asToken());
                                } else {
                                    return methodDescription.isBridge()
                                            ? new Ambiguous<U>(key, declaringType, methodToken)
                                            : new ForMethod<U>(key, methodDescription);
                                }
                            }

                            @Override
                            public Entry<U> mergeWith(Key.Identifying<U> key) {
                                return new Ambiguous<U>(key.mergeWith(key), declaringType, methodToken);
                            }

                            @Override
                            public MethodGraph.Node asNode() {
                                return new Node(key.asTokenKey(), declaringType, methodToken);
                            }

                            @Override
                            public boolean equals(Object other) {
                                if (this == other) return true;
                                if (other == null || getClass() != other.getClass()) return false;
                                Ambiguous<?> ambiguous = (Ambiguous<?>) other;
                                return key.equals(ambiguous.key)
                                        && declaringType.equals(ambiguous.declaringType)
                                        && methodToken.equals(ambiguous.methodToken);
                            }

                            @Override
                            public int hashCode() {
                                int result = key.hashCode();
                                result = 31 * result + declaringType.hashCode();
                                result = 31 * result + methodToken.hashCode();
                                return result;
                            }

                            @Override
                            public String toString() {
                                return "MethodGraph.Compiler.Default.Key.Store.Entry.Ambiguous{" +
                                        "key=" + key +
                                        ", declaringType=" + declaringType +
                                        ", methodToken=" + methodToken +
                                        '}';
                            }

                            protected static class Node implements MethodGraph.Node {

                                private final Key<MethodDescription.Token> key;

                                private final TypeDescription declaringType;

                                private final MethodDescription.Token methodToken;

                                public Node(Key<MethodDescription.Token> key, TypeDescription declaringType, MethodDescription.Token methodToken) {
                                    this.key = key;
                                    this.declaringType = declaringType;
                                    this.methodToken = methodToken;
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
                                    return key.resolveBridges(methodToken);
                                }

                                @Override
                                public boolean isMadeVisible() {
                                    return false;
                                }

                                @Override
                                public boolean equals(Object other) {
                                    if (this == other) return true;
                                    if (other == null || getClass() != other.getClass()) return false;
                                    Node node = (Node) other;
                                    return key.equals(node.key)
                                            && declaringType.equals(node.declaringType)
                                            && methodToken.equals(node.methodToken);
                                }

                                @Override
                                public int hashCode() {
                                    int result = key.hashCode();
                                    result = 31 * result + declaringType.hashCode();
                                    result = 31 * result + methodToken.hashCode();
                                    return result;
                                }

                                @Override
                                public String toString() {
                                    return "MethodGraph.Compiler.Default.Key.Store.Entry.Ambiguous.Node{" +
                                            "key=" + key +
                                            ", declaringType=" + declaringType +
                                            ", methodToken=" + methodToken +
                                            '}';
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    enum Empty implements MethodGraph.Linked {

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

        @Override
        public String toString() {
            return "MethodGraph.Empty." + name();
        }
    }
}
