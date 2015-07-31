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

        class Default<T> implements Compiler {

            public static <S> Compiler of(Harmonizer<S> harmonizer, Merger merger) {
                return new Default<S>(harmonizer, merger);
            }

            public static Compiler forJavaHierarchy() {
                return of(Harmonizer.ForJavaMethod.INSTANCE, Merger.Directional.LEFT);
            }

            public static Compiler forJVMHierarchy() {
                return of(Harmonizer.ForJVMMethod.INSTANCE, Merger.Directional.LEFT);
            }

            private final Harmonizer<T> harmonizer;

            private final Merger merger;

            protected Default(Harmonizer<T> harmonizer, Merger merger) {
                this.harmonizer = harmonizer;
                this.merger = merger;
            }

            @Override
            public MethodGraph.Linked make(TypeDescription typeDescription) {
                Map<GenericTypeDescription, Key.Store<T>> snapshots = new HashMap<GenericTypeDescription, Key.Store<T>>();
                Key.Store<?> rootStore = analyze(typeDescription, snapshots, any(), isVirtual().and(isVisibleTo(typeDescription)));
                GenericTypeDescription superType = typeDescription.getSuperType();
                List<GenericTypeDescription> interfaceTypes = typeDescription.getInterfaces();
                Map<TypeDescription, MethodGraph> interfaceGraphs = new HashMap<TypeDescription, MethodGraph>(interfaceTypes.size());
                for (GenericTypeDescription interfaceType : interfaceTypes) {
                    interfaceGraphs.put(interfaceType.asRawType(), snapshots.get(interfaceType).asGraph(merger));
                }
                return new Linked.Delegation(rootStore.asGraph(merger),
                        superType == null
                                ? Empty.INSTANCE
                                : snapshots.get(superType).asGraph(merger),
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
                Key.Store<T> interfaceKeyStore = new Key.Store<T>();
                for (GenericTypeDescription interfaceType : typeDescription.getInterfaces()) {
                    interfaceKeyStore = interfaceKeyStore.combineWith(analyze(interfaceType, snapshots, nextMatcher, nextMatcher));
                }
                keyStore = keyStore.inject(interfaceKeyStore);
                for (MethodDescription methodDescription : typeDescription.getDeclaredMethods().filter(currentMatcher)) {
                    keyStore = keyStore.registerTopLevel(methodDescription, harmonizer);
                }
                return keyStore;
            }

            @Override
            public boolean equals(Object other) {
                return this == other || !(other == null || getClass() != other.getClass())
                        && harmonizer.equals(((Default<?>) other).harmonizer)
                        && merger.equals(((Default<?>) other).merger);
            }

            @Override
            public int hashCode() {
                return harmonizer.hashCode() + 31 * merger.hashCode();
            }

            @Override
            public String toString() {
                return "MethodGraph.Compiler.Default{" +
                        "harmonizer=" + harmonizer +
                        ", merger=" + merger +
                        '}';
            }

            public interface Harmonizer<S> {

                S wrap(MethodDescription.Token methodToken);

                enum ForJavaMethod implements Harmonizer<ForJavaMethod.Token> {

                    INSTANCE;

                    @Override
                    public Token wrap(MethodDescription.Token methodToken) {
                        return new Token(methodToken);
                    }

                    @Override
                    public String toString() {
                        return "MethodGraph.Compiler.Default.Harmonizer.ForJavaMethod." + name();
                    }

                    protected static class Token {

                        private final MethodDescription.Token methodToken;

                        protected Token(MethodDescription.Token methodToken) {
                            this.methodToken = methodToken;
                        }

                        @Override
                        public boolean equals(Object other) {
                            if (this == other) return true;
                            if (!(other instanceof Token)) return false;
                            Token forJavaMethod = (Token) other;
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
                            return "MethodGraph.Compiler.Default.Harmonizer.ForJavaMethod.Token{" +
                                    "methodToken=" + methodToken +
                                    '}';
                        }
                    }
                }

                enum ForJVMMethod implements Harmonizer<ForJVMMethod.Token> {

                    INSTANCE;

                    @Override
                    public Token wrap(MethodDescription.Token methodToken) {
                        return new Token(methodToken);
                    }

                    @Override
                    public String toString() {
                        return "MethodGraph.Compiler.Default.Harmonizer.ForJVMMethod." + name();
                    }

                    protected static class Token {

                        private final MethodDescription.Token methodToken;

                        public Token(MethodDescription.Token methodToken) {
                            this.methodToken = methodToken;
                        }

                        @Override
                        public boolean equals(Object other) {
                            if (this == other) return true;
                            if (!(other instanceof Token)) return false;
                            Token token = (Token) other;
                            if (!methodToken.getReturnType().asRawType().equals(token.methodToken.getReturnType().asRawType())) return false;
                            List<ParameterDescription.Token> tokens = methodToken.getParameterTokens(), otherTokens = token.methodToken.getParameterTokens();
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
                            return "MethodGraph.Compiler.Default.Harmonizer.ForJVMMethod.Token{" +
                                    "methodToken=" + methodToken +
                                    '}';
                        }
                    }
                }
            }

            public interface Merger {

                MethodDescription merge(MethodDescription left, MethodDescription right);

                enum Strict implements Merger {

                    INSTANCE;

                    @Override
                    public MethodDescription merge(MethodDescription left, MethodDescription right) {
                        if (left.asToken().isIdenticalTo(right.asToken())) {
                            return left;
                        } else {
                            throw new IllegalArgumentException("Discovered conflicting methods: " + left + " and " + right);
                        }
                    }

                    @Override
                    public String toString() {
                        return "MethodGraph.Compiler.Default.Merger.Strict." + name();
                    }
                }

                enum Directional implements Merger {

                    LEFT(true),

                    RIGHT(false);

                    private final boolean left;

                    Directional(boolean left) {
                        this.left = left;
                    }

                    @Override
                    public MethodDescription merge(MethodDescription left, MethodDescription right) {
                        return this.left
                                ? left
                                : right;
                    }

                    @Override
                    public String toString() {
                        return "MethodGraph.Compiler.Default.Merger.Directional." + name();
                    }
                }
            }

            protected abstract static class Key<S> {

                protected final String internalName;

                protected Key(String internalName) {
                    this.internalName = internalName;
                }

                protected abstract Set<S> getIdentifiers();

                @Override
                public boolean equals(Object other) {
                    return other == this || (other instanceof Key
                            && internalName.equals(((Key) other).internalName)
                            && !Collections.disjoint(getIdentifiers(), ((Key) other).getIdentifiers()));
                }

                @Override
                public int hashCode() {
                    return internalName.hashCode();
                }

                protected static class Detached extends Key<MethodDescription.Token> {

                    protected static Detached of(MethodDescription.Token methodToken) {
                        return new Detached(methodToken.getInternalName(), Collections.singleton(methodToken));
                    }

                    private final Set<MethodDescription.Token> identifiers;

                    protected Detached(String internalName, Set<MethodDescription.Token> identifiers) {
                        super(internalName);
                        this.identifiers = identifiers;
                    }

                    protected Set<MethodDescription.Token> resolveBridges(MethodDescription.Token excluded) {
                        Set<MethodDescription.Token> tokens = new HashSet<MethodDescription.Token>(identifiers);
                        tokens.remove(excluded);
                        return tokens;
                    }

                    @Override
                    protected Set<MethodDescription.Token> getIdentifiers() {
                        return identifiers;
                    }

                    @Override
                    public String toString() {
                        return "MethodGraph.Compiler.Default.Key.Detached{" +
                                "internalName='" + internalName + '\'' +
                                ", identifiers=" + identifiers +
                                '}';
                    }
                }

                protected static class Harmonized<V> extends Key<V> {

                    protected static <Q> Harmonized<Q> of(MethodDescription methodDescription, Harmonizer<Q> harmonizer) {
                        MethodDescription.Token methodToken = methodDescription.asToken();
                        return new Harmonized<Q>(methodDescription.getInternalName(),
                                Collections.singletonMap(harmonizer.wrap(methodToken), Collections.<MethodDescription.Token>emptySet()));
                    }

                    private final Map<V, Set<MethodDescription.Token>> identifiers;

                    protected Harmonized(String internalName, Map<V, Set<MethodDescription.Token>> identifiers) {
                        super(internalName);
                        this.identifiers = identifiers;
                    }

                    protected Detached detach() {
                        Set<MethodDescription.Token> identifiers = new HashSet<MethodDescription.Token>();
                        for (Set<MethodDescription.Token> methodTokens : this.identifiers.values()) {
                            for (MethodDescription.Token methodToken : methodTokens) {
                                identifiers.add(methodToken);
                            }
                        }
                        return new Detached(internalName, identifiers);
                    }

                    protected Harmonized<V> combineWith(Key.Harmonized<V> key) {
                        Map<V, Set<MethodDescription.Token>> identifiers = new HashMap<V, Set<MethodDescription.Token>>(this.identifiers);
                        for (Map.Entry<V, Set<MethodDescription.Token>> entry : key.identifiers.entrySet()) {
                            Set<MethodDescription.Token> methodTokens = identifiers.get(entry.getKey());
                            if (methodTokens == null) {
                                identifiers.put(entry.getKey(), entry.getValue());
                            } else {
                                methodTokens = new HashSet<MethodDescription.Token>(methodTokens);
                                methodTokens.addAll(entry.getValue());
                                identifiers.put(entry.getKey(), methodTokens);
                            }
                        }
                        return new Harmonized<V>(internalName, identifiers);
                    }

                    protected Harmonized<V> extend(MethodDescription.InDefinedShape methodDescription, Harmonizer<V> harmonizer) {
                        Map<V, Set<MethodDescription.Token>> identifiers = new HashMap<V, Set<MethodDescription.Token>>(this.identifiers);
                        MethodDescription.Token methodToken = methodDescription.asToken();
                        V identifier = harmonizer.wrap(methodToken);
                        Set<MethodDescription.Token> methodTokens = identifiers.get(identifier);
                        if (methodTokens == null) {
                            identifiers.put(identifier, Collections.singleton(methodToken));
                        } else {
                            methodTokens = new HashSet<MethodDescription.Token>(methodTokens);
                            methodTokens.add(methodToken);
                            identifiers.put(identifier, methodTokens);
                        }
                        return new Harmonized<V>(internalName, identifiers);
                    }

                    protected Harmonized<V> inject(Harmonized<V> key) {
                        Map<V, Set<MethodDescription.Token>> identifiers = new HashMap<V, Set<MethodDescription.Token>>(this.identifiers);
                        for (Map.Entry<V, Set<MethodDescription.Token>> entry : key.identifiers.entrySet()) {
                            Set<MethodDescription.Token> methodTokens = identifiers.get(entry.getKey());
                            if (methodTokens == null) {
                                identifiers.put(entry.getKey(), entry.getValue());
                            } else {
                                methodTokens = new HashSet<MethodDescription.Token>(methodTokens);
                                methodTokens.addAll(entry.getValue());
                                identifiers.put(entry.getKey(), methodTokens);
                            }
                        }
                        return new Harmonized<V>(internalName, identifiers);
                    }

                    @Override
                    protected Set<V> getIdentifiers() {
                        return identifiers.keySet();
                    }

                    @Override
                    public String toString() {
                        return "MethodGraph.Compiler.Default.Key.Harmonized{" +
                                "internalName='" + internalName + '\'' +
                                ", identifiers=" + identifiers +
                                '}';
                    }
                }

                protected static class Store<V> {

                    private static final int EMPTY = 0;

                    private final LinkedHashMap<Harmonized<V>, Entry<V>> entries;

                    protected Store() {
                        this(new LinkedHashMap<Harmonized<V>, Entry<V>>(EMPTY));
                    }

                    private Store(LinkedHashMap<Harmonized<V>, Entry<V>> entries) {
                        this.entries = entries;
                    }

                    protected Store<V> registerTopLevel(MethodDescription methodDescription, Harmonizer<V> harmonizer) {
                        Harmonized<V> key = Harmonized.of(methodDescription, harmonizer);
                        LinkedHashMap<Harmonized<V>, Entry<V>> entries = new LinkedHashMap<Harmonized<V>, Entry<V>>(this.entries);
                        Entry<V> currentEntry = entries.remove(key);
                        Entry<V> extendedEntry = (currentEntry == null
                                ? new Entry.Initial<V>(key)
                                : currentEntry).extendBy(methodDescription, harmonizer);
                        entries.put(extendedEntry.getKey(), extendedEntry);
                        return new Store<V>(entries);
                    }

                    protected Store<V> combineWith(Store<V> keyStore) {
                        Store<V> combinedStore = this;
                        for (Entry<V> entry : keyStore.entries.values()) {
                            combinedStore = combinedStore.combineWith(entry);
                        }
                        return combinedStore;
                    }

                    protected Store<V> combineWith(Entry<V> entry) {
                        LinkedHashMap<Harmonized<V>, Entry<V>> entries = new LinkedHashMap<Harmonized<V>, Entry<V>>(this.entries);
                        Entry<V> previousEntry = entries.remove(entry.getKey());
                        Entry<V> injectedEntry = previousEntry == null
                                ? entry
                                : combine(previousEntry, entry);
                        entries.put(injectedEntry.getKey(), injectedEntry);
                        return new Store<V>(entries);
                    }

                    private static <W> Entry<W> combine(Entry<W> left, Entry<W> right) {
                        Set<MethodDescription> leftMethods = left.getCandidates(), rightMethods = right.getCandidates();
                        Set<MethodDescription> combined = new HashSet<MethodDescription>(leftMethods.size() + rightMethods.size());
                        combined.addAll(leftMethods);
                        combined.addAll(rightMethods);
                        for (MethodDescription leftMethod : leftMethods) {
                            TypeDescription leftType = leftMethod.getDeclaringType().asRawType();
                            for (MethodDescription rightMethod : rightMethods) {
                                TypeDescription rightType = rightMethod.getDeclaringType().asRawType();
                                if (leftType.isAssignableTo(rightType)) {
                                    combined.remove(rightMethod);
                                    break;
                                } else if (leftType.isAssignableFrom(rightType)) {
                                    combined.remove(leftMethod);
                                    break;
                                }
                            }
                        }
                        Key.Harmonized<W> key = left.getKey().combineWith(right.getKey());
                        return combined.size() == 1
                                ? new Entry.Resolved<W>(key, combined.iterator().next(), false)
                                : new Entry.Ambiguous<W>(key, combined);
                    }

                    protected Store<V> inject(Store<V> keyStore) {
                        Store<V> injectedStore = this;
                        for (Entry<V> entry : keyStore.entries.values()) {
                            injectedStore = injectedStore.inject(entry);
                        }
                        return injectedStore;
                    }

                    protected Store<V> inject(Entry<V> entry) {
                        LinkedHashMap<Harmonized<V>, Entry<V>> entries = new LinkedHashMap<Harmonized<V>, Entry<V>>(this.entries);
                        Entry<V> dominantEntry = entries.remove(entry.getKey());
                        Entry<V> injectedEntry = dominantEntry == null
                                ? entry
                                : dominantEntry.inject(entry.getKey());
                        entries.put(injectedEntry.getKey(), injectedEntry);
                        return new Store<V>(entries);
                    }

                    protected MethodGraph asGraph(Merger merger) {
                        LinkedHashMap<Key<MethodDescription.Token>, Node> entries = new LinkedHashMap<Key<MethodDescription.Token>, Node>(this.entries.size());
                        for (Entry<V> entry : this.entries.values()) {
                            entries.put(entry.getKey().detach(), entry.asNode(merger));
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
                            Node node = entries.get(Detached.of(methodToken));
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

                    protected interface Entry<W> {

                        Harmonized<W> getKey();

                        Set<MethodDescription> getCandidates();

                        Entry<W> extendBy(MethodDescription methodDescription, Harmonizer<W> harmonizer);

                        Entry<W> inject(Harmonized<W> key);

                        Node asNode(Merger merger);

                        class Initial<U> implements Entry<U> {

                            private final Harmonized<U> key;

                            protected Initial(Harmonized<U> key) {
                                this.key = key;
                            }

                            @Override
                            public Harmonized<U> getKey() {
                                throw new IllegalStateException("Cannot extract key from initial entry:" + this);
                            }

                            @Override
                            public Set<MethodDescription> getCandidates() {
                                throw new IllegalStateException("Cannot extract method from initial entry:" + this);
                            }

                            @Override
                            public Entry<U> extendBy(MethodDescription methodDescription, Harmonizer<U> harmonizer) {
                                return new Resolved<U>(key.extend(methodDescription.asDefined(), harmonizer), methodDescription, false);
                            }

                            @Override
                            public Entry<U> inject(Harmonized<U> key) {
                                throw new IllegalStateException("Cannot inject into initial entry without a registered method: " + this);
                            }

                            @Override
                            public Node asNode(Merger merger) {
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

                        class Resolved<U> implements Entry<U> {

                            private final Harmonized<U> key;

                            private final MethodDescription methodDescription;

                            private final boolean madeVisible;

                            protected Resolved(Harmonized<U> key, MethodDescription methodDescription, boolean madeVisible) {
                                this.key = key;
                                this.methodDescription = methodDescription;
                                this.madeVisible = madeVisible;
                            }

                            @Override
                            public Harmonized<U> getKey() {
                                return key;
                            }

                            @Override
                            public Set<MethodDescription> getCandidates() {
                                return Collections.singleton(methodDescription);
                            }

                            @Override
                            public Entry<U> extendBy(MethodDescription methodDescription, Harmonizer<U> harmonizer) {
                                Harmonized<U> key = this.key.extend(methodDescription.asDefined(), harmonizer);
                                return methodDescription.getDeclaringType().equals(this.methodDescription.getDeclaringType())
                                        ? Ambiguous.of(key, methodDescription, this.methodDescription)
                                        : new Resolved<U>(key, methodDescription.isBridge() ? this.methodDescription : methodDescription, methodDescription.isBridge());
                            }

                            @Override
                            public Entry<U> inject(Harmonized<U> key) {
                                return new Resolved<U>(key.inject(key), methodDescription, madeVisible);
                            }

                            @Override
                            public MethodGraph.Node asNode(Merger merger) {
                                return new Node(key.detach(), methodDescription, madeVisible);
                            }

                            @Override
                            public boolean equals(Object other) {
                                if (this == other) return true;
                                if (other == null || getClass() != other.getClass()) return false;
                                Resolved<?> resolved = (Resolved<?>) other;
                                return madeVisible == resolved.madeVisible
                                        && key.equals(resolved.key)
                                        && methodDescription.equals(resolved.methodDescription);
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

                                private final Detached key;

                                private final MethodDescription methodDescription;

                                private final boolean madeVisible;

                                public Node(Detached key, MethodDescription methodDescription, boolean madeVisible) {
                                    this.key = key;
                                    this.methodDescription = methodDescription;
                                    this.madeVisible = madeVisible;
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
                                    return madeVisible;
                                }

                                @Override
                                public boolean equals(Object other) {
                                    if (this == other) return true;
                                    if (other == null || getClass() != other.getClass()) return false;
                                    Node node = (Node) other;
                                    return madeVisible == node.madeVisible
                                            && key.equals(node.key)
                                            && methodDescription.equals(node.methodDescription);
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
                                    return "MethodGraph.Compiler.Default.Key.Store.Entry.ForMethod.Node{" +
                                            "key=" + key +
                                            ", methodDescription=" + methodDescription +
                                            ", madeVisible=" + madeVisible +
                                            '}';
                                }
                            }
                        }

                        class Ambiguous<U> implements Entry<U> {

                            private final Harmonized<U> key;

                            private final Set<MethodDescription> methodDescriptions;

                            protected static <Q> Entry<Q> of(Harmonized<Q> key, MethodDescription left, MethodDescription right) {
                                return left.isBridge() ^ right.isBridge()
                                        ? new Resolved<Q>(key, left.isBridge() ? right : left, false)
                                        : new Ambiguous<Q>(key, new HashSet<MethodDescription>(Arrays.asList(left, right)));
                            }

                            protected Ambiguous(Harmonized<U> key, Set<MethodDescription> methodDescriptions) {
                                this.key = key;
                                this.methodDescriptions = methodDescriptions;
                            }

                            @Override
                            public Harmonized<U> getKey() {
                                return key;
                            }

                            @Override
                            public Set<MethodDescription> getCandidates() {
                                return methodDescriptions;
                            }

                            @Override
                            public Entry<U> extendBy(MethodDescription methodDescription, Harmonizer<U> harmonizer) {
                                Harmonized<U> key = this.key.extend(methodDescription.asDefined(), harmonizer);
                                Set<MethodDescription> methodDescriptions = new HashSet<MethodDescription>(this.methodDescriptions.size() + 1);
                                GenericTypeDescription declaringType = methodDescription.getDeclaringType();
                                boolean bridge = methodDescription.isBridge();
                                for (MethodDescription extendedMethod : this.methodDescriptions) {
                                    if (extendedMethod.getDeclaringType().equals(declaringType)) {
                                        if (extendedMethod.isBridge() ^ bridge) {
                                            methodDescriptions.add(bridge ? extendedMethod : methodDescription);
                                        } else {
                                            methodDescriptions.add(methodDescription);
                                            methodDescriptions.add(extendedMethod);
                                        }
                                    }
                                }
                                if (methodDescriptions.isEmpty()) {
                                    return new Resolved<U>(key, methodDescription, bridge);
                                } else if (methodDescriptions.size() == 1) {
                                    return new Resolved<U>(key, methodDescriptions.iterator().next(), false);
                                } else {
                                    return new Ambiguous<U>(key, methodDescriptions);
                                }
                            }

                            @Override
                            public Entry<U> inject(Harmonized<U> key) {
                                return new Ambiguous<U>(key.inject(key), methodDescriptions);
                            }

                            @Override
                            public MethodGraph.Node asNode(Merger merger) {
                                Iterator<MethodDescription> iterator = methodDescriptions.iterator();
                                MethodDescription methodDescription = iterator.next();
                                while (iterator.hasNext()) {
                                    methodDescription = merger.merge(methodDescription, iterator.next());
                                }
                                return new Node(key.detach(), methodDescription);
                            }

                            @Override
                            public boolean equals(Object other) {
                                if (this == other) return true;
                                if (other == null || getClass() != other.getClass()) return false;
                                Ambiguous<?> ambiguous = (Ambiguous<?>) other;
                                return key.equals(ambiguous.key) && methodDescriptions.equals(ambiguous.methodDescriptions);
                            }

                            @Override
                            public int hashCode() {
                                int result = key.hashCode();
                                result = 31 * result + methodDescriptions.hashCode();
                                return result;
                            }

                            @Override
                            public String toString() {
                                return "MethodGraph.Compiler.Default.Key.Store.Entry.Ambiguous{" +
                                        "key=" + key +
                                        ", methodDescriptions=" + methodDescriptions +
                                        '}';
                            }

                            protected static class Node implements MethodGraph.Node {

                                private final Detached key;

                                private final MethodDescription methodDescription;

                                public Node(Detached key, MethodDescription methodDescription) {
                                    this.key = key;
                                    this.methodDescription = methodDescription;
                                }

                                @Override
                                public Sort getSort() {
                                    return Sort.AMBIGUOUS;
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
                                    return false;
                                }

                                @Override
                                public boolean equals(Object other) {
                                    if (this == other) return true;
                                    if (other == null || getClass() != other.getClass()) return false;
                                    Node node = (Node) other;
                                    return key.equals(node.key) && methodDescription.equals(node.methodDescription);
                                }

                                @Override
                                public int hashCode() {
                                    int result = key.hashCode();
                                    result = 31 * result + methodDescription.hashCode();
                                    return result;
                                }

                                @Override
                                public String toString() {
                                    return "MethodGraph.Compiler.Default.Key.Store.Entry.Ambiguous.Node{" +
                                            "key=" + key +
                                            ", methodDescription=" + methodDescription +
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
