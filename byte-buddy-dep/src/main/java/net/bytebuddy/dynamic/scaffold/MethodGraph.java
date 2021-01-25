package net.bytebuddy.dynamic.scaffold;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.FilterableList;
import org.objectweb.asm.Opcodes;

import java.util.*;

import static net.bytebuddy.matcher.ElementMatchers.*;

/**
 * A method graph represents a view on a set of methods as they are seen from a given type. Any method is represented as a node that represents
 * a method, its bridge methods, its resolution state and information on if it was made visible by a visibility bridge. 方法图表示从给定类型看到的一组方法的视图。任何方法都表示为一个节点，该节点表示一个方法、它的桥方法、它的解析状态以及有关它是否被可见性桥显示的信息
 */
public interface MethodGraph {
    // 代表了一个方法集合的关系视图 -> Compiler用来生成一个MethodGraph。不同Compiler生成不同的，有默认的
    /**
     * Locates a node in this graph which represents the provided method token. 在此图中查找表示提供的方法令牌的节点
     *
     * @param token A method token that represents the method to be located.
     * @return The node representing the given token.
     */
    Node locate(MethodDescription.SignatureToken token);

    /**
     * Lists all nodes of this method graph.
     *
     * @return A list of all nodes of this method graph.
     */
    NodeList listNodes();

    /**
     * A canonical implementation of an empty method graph. 空方法图的规范实现
     */
    enum Empty implements MethodGraph.Linked, MethodGraph.Compiler {

        /**
         * The singleton instance.
         */
        INSTANCE;

        @Override
        public Node locate(MethodDescription.SignatureToken token) {
            return Node.Unresolved.INSTANCE;
        }

        @Override
        public NodeList listNodes() {
            return new NodeList(Collections.<Node>emptyList());
        }

        @Override
        public MethodGraph getSuperClassGraph() {
            return this;
        }

        @Override
        public MethodGraph getInterfaceGraph(TypeDescription typeDescription) {
            return this;
        }

        @Override
        public Linked compile(TypeDescription typeDescription) {
            return this;
        }

        @Override
        public Linked compile(TypeDefinition typeDefinition, TypeDescription viewPoint) {
            return this;
        }
    }

    /**
     * A linked method graph represents a view that additionally exposes information of a given type's super type view and a
     * view on this graph's directly implemented interfaces. 链接的方法图表示一个视图，该视图还公开给定类型的超级类型视图的信息以及此图直接实现的接口上的视图
     */
    interface Linked extends MethodGraph {

        /**
         * Returns a graph representing the view on this represented type's super type. 返回表示此表示类型的超类型上的视图的图形
         *
         * @return A graph representing the view on this represented type's super type. 表示此表示类型的超类型上的视图的图形
         */
        MethodGraph getSuperClassGraph();

        /**
         * Returns a graph representing the view on this represented type's directly implemented interface type. 返回表示此表示类型的直接实现接口类型上的视图的图形
         *
         * @param typeDescription The interface type for which a view is to be returned.
         * @return A graph representing the view on this represented type's directly implemented interface type.
         */
        MethodGraph getInterfaceGraph(TypeDescription typeDescription);

        /**
         * A simple implementation of a linked method graph that exposes views by delegation to given method graphs. 一个链接方法图的简单实现，通过委托将视图公开给给定的方法图
         */
        @HashCodeAndEqualsPlugin.Enhance
        class Delegation implements Linked {

            /**
             * The represented type's method graph. 表示类型的方法图
             */
            private final MethodGraph methodGraph;

            /**
             * The super class's method graph. 超类的方法图
             */
            private final MethodGraph superClassGraph;

            /**
             * A mapping of method graphs of the represented type's directly implemented interfaces to their graph representatives. 表示类型的直接实现接口的方法图到它们的图表示的映射
             */
            private final Map<TypeDescription, MethodGraph> interfaceGraphs;

            /**
             * Creates a new delegation method graph. 创建新的委派方法图
             *
             * @param methodGraph     The represented type's method graph.
             * @param superClassGraph The super class's method graph.
             * @param interfaceGraphs A mapping of method graphs of the represented type's directly implemented interfaces to their graph representatives.
             */
            public Delegation(MethodGraph methodGraph, MethodGraph superClassGraph, Map<TypeDescription, MethodGraph> interfaceGraphs) {
                this.methodGraph = methodGraph;
                this.superClassGraph = superClassGraph;
                this.interfaceGraphs = interfaceGraphs;
            }

            @Override
            public MethodGraph getSuperClassGraph() {
                return superClassGraph;
            }

            @Override
            public MethodGraph getInterfaceGraph(TypeDescription typeDescription) {
                MethodGraph interfaceGraph = interfaceGraphs.get(typeDescription);
                return interfaceGraph == null
                        ? Empty.INSTANCE
                        : interfaceGraph;
            }

            @Override
            public Node locate(MethodDescription.SignatureToken token) {
                return methodGraph.locate(token);
            }

            @Override
            public NodeList listNodes() {
                return methodGraph.listNodes();
            }
        }
    }

    /**
     * Represents a node within a method graph. 表示方法图中的节点
     */
    interface Node {

        /**
         * Returns the sort of this node. 返回此节点的排序
         *
         * @return The sort of this node. 这个节点的类型
         */
        Sort getSort();

        /**
         * Returns the method that is represented by this node. 返回此节点表示的方法
         *
         * @return The method that is represented by this node. 此节点表示的方法
         */
        MethodDescription getRepresentative();

        /**
         * Returns a set of type tokens that this method represents. This set contains the actual method's type including the
         * types of all bridge methods. 返回此方法表示的一组类型标记。此集合包含实际方法的类型，包括所有桥方法的类型
         *
         * @return A set of type tokens that this method represents. 此方法表示的一组类型标记
         */
        Set<MethodDescription.TypeToken> getMethodTypes();

        /**
         * Returns the minimal method visibility of all methods that are represented by this node. 返回此节点表示的所有方法的最小方法可见性
         *
         * @return The minimal method visibility of all methods that are represented by this node. 此节点表示的所有方法的最小方法可见性
         */
        Visibility getVisibility();

        /**
         * Represents a {@link net.bytebuddy.dynamic.scaffold.MethodGraph.Node}'s state.
         */
        enum Sort {

            /**
             * Represents a resolved node that was made visible by a visibility bridge. 表示由可见性桥显示的已解析节点
             */
            VISIBLE(true, true, true),

            /**
             * Represents a resolved node that was not made visible by a visibility bridge. 表示未被可见性桥显示的已解析节点
             */
            RESOLVED(true, true, false),

            /**
             * Represents an ambiguous node, i.e. a node that might refer to several methods. 表示不明确的节点，即可能引用多个方法的节点
             */
            AMBIGUOUS(true, false, false),

            /**
             * Represents an unresolved node. 表示未解析的节点
             */
            UNRESOLVED(false, false, false);

            /**
             * {@code true} if this sort represents a resolved node. true -> 如果此排序表示已解析的节点
             */
            private final boolean resolved;

            /**
             * {@code true} if this sort represents a non-ambiguous node. true -> 如果此排序表示非二义节点
             */
            private final boolean unique;

            /**
             * {@code true} if this sort represents a node that was made by a visibility bridge. true -> 如果此排序表示由可见性桥生成的节点
             */
            private final boolean madeVisible;

            /**
             * Creates a new sort.
             *
             * @param resolved    {@code true} if this sort represents a resolved node.
             * @param unique      {@code true} if this sort represents a non-ambiguous node.
             * @param madeVisible {@code true} if this sort represents a node that was made by a visibility bridge.
             */
            Sort(boolean resolved, boolean unique, boolean madeVisible) {
                this.resolved = resolved;
                this.unique = unique;
                this.madeVisible = madeVisible;
            }

            /**
             * Verifies if this sort represents a resolved node.
             *
             * @return {@code true} if this sort represents a resolved node.
             */
            public boolean isResolved() {
                return resolved;
            }

            /**
             * Verifies if this sort represents a non-ambiguous node.
             *
             * @return {@code true} if this sort represents a non-ambiguous node.
             */
            public boolean isUnique() {
                return unique;
            }

            /**
             * Verifies if this sort represents a node that was made visible by a visibility bridge.
             *
             * @return {@code true} if this sort represents a node that was made visible by a visibility bridge.
             */
            public boolean isMadeVisible() {
                return madeVisible;
            }
        }

        /**
         * A canonical implementation of an unresolved node. 未解析节点的规范实现
         */
        enum Unresolved implements Node {

            /**
             * The singleton instance.
             */
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
            public Set<MethodDescription.TypeToken> getMethodTypes() {
                throw new IllegalStateException("Cannot resolve bridge method of an illegal node");
            }

            @Override
            public Visibility getVisibility() {
                throw new IllegalStateException("Cannot resolve visibility of an illegal node");
            }
        }

        /**
         * A simple implementation of a resolved node of a method without bridges. 无桥方法的解析节点的简单实现
         */
        @HashCodeAndEqualsPlugin.Enhance
        class Simple implements Node {

            /**
             * The represented method. 表示方法
             */
            private final MethodDescription methodDescription;

            /**
             * Creates a simple node. 创建简单节点
             *
             * @param methodDescription The represented method.
             */
            public Simple(MethodDescription methodDescription) {
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
            public Set<MethodDescription.TypeToken> getMethodTypes() {
                return Collections.emptySet();
            }

            @Override
            public Visibility getVisibility() {
                return methodDescription.getVisibility();
            }
        }
    }

    /**
     * A compiler to produce a {@link MethodGraph} from a given type. 从给定类型生成方法图的编译器
     */
    @SuppressFBWarnings(value = "IC_SUPERCLASS_USES_SUBCLASS_DURING_INITIALIZATION", justification = "Safe initialization is implied")
    interface Compiler {

        /**
         * The default compiler for compiling Java methods. 编译Java方法的默认编译器
         */
        Compiler DEFAULT = MethodGraph.Compiler.Default.forJavaHierarchy();

        /**
         * Compiles the given type into a method graph considering the type to be the viewpoint. 将给定的类型编译为方法图，并将该类型视为视点
         *
         * @param typeDescription The type to be compiled. 要编译的类型
         * @return A linked method graph representing the given type. 表示给定类型的链接方法图
         */
        MethodGraph.Linked compile(TypeDescription typeDescription);

        /**
         * Compiles the given type into a method graph. 将给定类型编译为方法图
         *
         * @param typeDefinition The type to be compiled. 要编译的类型
         * @param viewPoint      The view point that determines the method's visibility. 确定方法可见性的视图点
         * @return A linked method graph representing the given type. 表示给定类型的链接方法图
         */
        MethodGraph.Linked compile(TypeDefinition typeDefinition, TypeDescription viewPoint);

        /**
         * A flat compiler that simply returns the methods that are declared by the instrumented type. 一个平面编译器，它只返回由插入指令的类型声明的方法
         */
        enum ForDeclaredMethods implements Compiler {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public Linked compile(TypeDescription typeDescription) {
                return compile(typeDescription, typeDescription);
            }

            @Override
            public Linked compile(TypeDefinition typeDefinition, TypeDescription viewPoint) {
                LinkedHashMap<MethodDescription.SignatureToken, Node> nodes = new LinkedHashMap<MethodDescription.SignatureToken, Node>();
                for (MethodDescription methodDescription : typeDefinition.getDeclaredMethods().filter(isVirtual().and(not(isBridge())).and(isVisibleTo(viewPoint)))) {
                    nodes.put(methodDescription.asSignatureToken(), new Node.Simple(methodDescription));
                }
                return new Linked.Delegation(new MethodGraph.Simple(nodes), Empty.INSTANCE, Collections.<TypeDescription, MethodGraph>emptyMap());
            }
        }

        /**
         * An abstract base implementation of a method graph compiler. 方法图编译器的抽象基实现
         */
        abstract class AbstractBase implements Compiler {

            @Override
            public Linked compile(TypeDescription typeDescription) {
                return compile(typeDescription, typeDescription);
            }
        }

        /**
         * A default implementation of a method graph. 方法图的默认实现
         *
         * @param <T> The type of the harmonizer token to be used for linking methods of different types. 用于链接不同类型的方法的harmonizer标记的类型
         */
        @HashCodeAndEqualsPlugin.Enhance
        class Default<T> extends AbstractBase {

            /**
             * The harmonizer to be used. 要使用的协调器
             */
            private final Harmonizer<T> harmonizer;

            /**
             * The merger to be used. 要使用的合并器
             */
            private final Merger merger;

            /**
             * A visitor to apply to all type descriptions before analyzing their methods or resolving super types. 在分析方法或解析超级类型之前应用于所有类型描述的访问者
             */
            private final TypeDescription.Generic.Visitor<? extends TypeDescription.Generic> visitor;

            /**
             * Creates a new default method graph compiler. 创建新的默认方法图编译器
             *
             * @param harmonizer The harmonizer to be used.
             * @param merger     The merger to be used.
             * @param visitor    A visitor to apply to all type descriptions before analyzing their methods or resolving super types.
             */
            protected Default(Harmonizer<T> harmonizer, Merger merger, TypeDescription.Generic.Visitor<? extends TypeDescription.Generic> visitor) {
                this.harmonizer = harmonizer;
                this.merger = merger;
                this.visitor = visitor;
            }

            /**
             * Creates a default compiler using the given harmonizer and merger. All raw types are reified before analyzing their properties. 使用给定的协调器和合并器创建默认编译器。所有原始类型在分析其属性之前都被具体化
             *
             * @param harmonizer The harmonizer to be used for creating tokens that uniquely identify a method hierarchy.
             * @param merger     The merger to be used for identifying a method to represent an ambiguous method resolution.
             * @param <S>        The type of the harmonizer token.
             * @return A default compiler for the given harmonizer and merger.
             */
            public static <S> Compiler of(Harmonizer<S> harmonizer, Merger merger) {
                return new Default<S>(harmonizer, merger, TypeDescription.Generic.Visitor.Reifying.INITIATING);
            }

            /**
             * Creates a default compiler using the given harmonizer and merger.
             *
             * @param harmonizer The harmonizer to be used for creating tokens that uniquely identify a method hierarchy.
             * @param merger     The merger to be used for identifying a method to represent an ambiguous method resolution.
             * @param visitor    A visitor to apply to all type descriptions before analyzing their methods or resolving super types.
             * @param <S>        The type of the harmonizer token.
             * @return A default compiler for the given harmonizer and merger.
             */
            public static <S> Compiler of(Harmonizer<S> harmonizer, Merger merger, TypeDescription.Generic.Visitor<? extends TypeDescription.Generic> visitor) {
                return new Default<S>(harmonizer, merger, visitor);
            }

            /**
             * <p>
             * Creates a default compiler for a method hierarchy following the rules of the Java programming language. According
             * to these rules, two methods of the same name are only different if their parameter types represent different raw
             * types. The return type is not considered as a part of the signature. 根据Java编程语言的规则为方法层次结构创建默认编译器。根据这些规则，只有当两个同名方法的参数类型表示不同的原始类型时，它们才是不同的。返回类型不被视为签名的一部分
             * </p>
             * <p>
             * Ambiguous methods are merged by considering the method that was discovered first. 通过考虑最先发现的方法来合并模糊方法
             * </p>
             *
             * @return A compiler for resolving a method hierarchy following the rules of the Java programming language. 一种编译器，用于按照Java编程语言的规则解析方法层次结构
             */
            public static Compiler forJavaHierarchy() {
                return of(Harmonizer.ForJavaMethod.INSTANCE, Merger.Directional.LEFT);
            }

            /**
             * <p>
             * Creates a default compiler for a method hierarchy following the rules of the Java virtual machine. According
             * to these rules, two methods of the same name are different if their parameter types and return types represent
             * different type erasures. 根据Java虚拟机的规则为方法层次结构创建默认编译器。根据这些规则，如果相同名称的两个方法的参数类型和返回类型表示不同的类型擦除，则它们是不同的
             * </p>
             * <p>
             * Ambiguous methods are merged by considering the method that was discovered first. 通过考虑最先发现的方法来合并模糊方法
             * </p>
             *
             * @return A compiler for resolving a method hierarchy following the rules of the Java programming language. 一种编译器，用于按照Java编程语言的规则解析方法层次结构
             */
            public static Compiler forJVMHierarchy() {
                return of(Harmonizer.ForJVMMethod.INSTANCE, Merger.Directional.LEFT);
            }

            @Override
            public MethodGraph.Linked compile(TypeDefinition typeDefinition, TypeDescription viewPoint) {
                Map<TypeDefinition, Key.Store<T>> snapshots = new HashMap<TypeDefinition, Key.Store<T>>();
                Key.Store<?> rootStore = doAnalyze(typeDefinition, snapshots, isVirtual().and(isVisibleTo(viewPoint)));
                TypeDescription.Generic superClass = typeDefinition.getSuperClass();
                List<TypeDescription.Generic> interfaceTypes = typeDefinition.getInterfaces();
                Map<TypeDescription, MethodGraph> interfaceGraphs = new HashMap<TypeDescription, MethodGraph>();
                for (TypeDescription.Generic interfaceType : interfaceTypes) {
                    interfaceGraphs.put(interfaceType.asErasure(), snapshots.get(interfaceType).asGraph(merger));
                }
                return new Linked.Delegation(rootStore.asGraph(merger),
                        superClass == null
                                ? Empty.INSTANCE
                                : snapshots.get(superClass).asGraph(merger),
                        interfaceGraphs);
            }

            /**
             * Analyzes the given type description without checking if the end of the type hierarchy was reached. 分析给定的类型描述，而不检查是否已到达类型层次结构的末尾
             *
             * @param typeDefinition   The type to analyze. 要分析的类型
             * @param key              The type in its original form before applying the visitor. 在应用访问者之前以其原始形式输入
             * @param snapshots        A map containing snapshots of key stores for previously analyzed types. 包含先前分析类型的密钥存储快照的映射
             * @param relevanceMatcher A matcher for filtering methods that should be included in the graph. 用于筛选应包含在图中的方法的匹配器
             * @return A key store describing the provided type. 描述所提供类型的密钥存储
             */
            protected Key.Store<T> analyze(TypeDefinition typeDefinition,
                                           TypeDefinition key,
                                           Map<TypeDefinition, Key.Store<T>> snapshots,
                                           ElementMatcher<? super MethodDescription> relevanceMatcher) {
                Key.Store<T> store = snapshots.get(key);
                if (store == null) {
                    store = doAnalyze(typeDefinition, snapshots, relevanceMatcher);
                    snapshots.put(key, store);
                }
                return store;
            }

            /**
             * Analyzes the given type description. 分析给定的类型描述
             *
             * @param typeDescription  The type to analyze.
             * @param snapshots        A map containing snapshots of key stores for previously analyzed types.
             * @param relevanceMatcher A matcher for filtering methods that should be included in the graph.
             * @return A key store describing the provided type.
             */
            protected Key.Store<T> analyzeNullable(TypeDescription.Generic typeDescription,
                                                   Map<TypeDefinition, Key.Store<T>> snapshots,
                                                   ElementMatcher<? super MethodDescription> relevanceMatcher) {
                return typeDescription == null
                        ? new Key.Store<T>()
                        : analyze(typeDescription.accept(visitor), typeDescription, snapshots, relevanceMatcher);
            }

            /**
             * Analyzes the given type description without checking if it is already presented in the key store. 分析给定的类型描述，而不检查它是否已出现在密钥存储中
             *
             * @param typeDefinition   The type to analyze.
             * @param snapshots        A map containing snapshots of key stores for previously analyzed types. 包含先前分析类型的密钥存储快照的映射
             * @param relevanceMatcher A matcher for filtering methods that should be included in the graph. 用于筛选应包含在图中的方法的匹配器
             * @return A key store describing the provided type. 描述所提供类型的密钥存储
             */
            protected Key.Store<T> doAnalyze(TypeDefinition typeDefinition,
                                             Map<TypeDefinition, Key.Store<T>> snapshots,
                                             ElementMatcher<? super MethodDescription> relevanceMatcher) {
                Key.Store<T> store = analyzeNullable(typeDefinition.getSuperClass(), snapshots, relevanceMatcher);
                Key.Store<T> interfaceStore = new Key.Store<T>();
                for (TypeDescription.Generic interfaceType : typeDefinition.getInterfaces()) {
                    interfaceStore = interfaceStore.combineWith(analyze(interfaceType.accept(visitor), interfaceType, snapshots, relevanceMatcher));
                }
                return store.inject(interfaceStore).registerTopLevel(typeDefinition.getDeclaredMethods().filter(relevanceMatcher), harmonizer);
            }

            /**
             * A harmonizer is responsible for creating a token that identifies a method's relevant attributes for considering
             * two methods of being equal or not. 协调器负责创建一个标识方法相关属性的标记，以考虑两个方法是否相等
             *
             * @param <S> The type of the token that is created by the implementing harmonizer. 由实现协调器创建的令牌的类型
             */
            public interface Harmonizer<S> {

                /**
                 * Harmonizes the given type token. 协调给定的类型标记
                 *
                 * @param typeToken The type token to harmonize.
                 * @return A token representing the given type token.
                 */
                S harmonize(MethodDescription.TypeToken typeToken);

                /**
                 * A harmonizer for the Java programming language that identifies a method by its parameter types only.
                 */
                enum ForJavaMethod implements Harmonizer<ForJavaMethod.Token> {

                    /**
                     * The singleton instance.
                     */
                    INSTANCE;

                    @Override
                    public Token harmonize(MethodDescription.TypeToken typeToken) {
                        return new Token(typeToken);
                    }

                    /**
                     * A token that identifies a Java method's type by its parameter types only.
                     */
                    protected static class Token {

                        /**
                         * The represented type token.
                         */
                        private final MethodDescription.TypeToken typeToken;

                        /**
                         * The hash code of this token which is precomputed for to improve performance.
                         */
                        private final int hashCode;

                        /**
                         * Creates a new type token for a Java method.
                         *
                         * @param typeToken The represented type token.
                         */
                        protected Token(MethodDescription.TypeToken typeToken) {
                            this.typeToken = typeToken;
                            hashCode = typeToken.getParameterTypes().hashCode();
                        }

                        @Override
                        public boolean equals(Object other) {
                            return this == other || other instanceof Token && typeToken.getParameterTypes().equals(((Token) other).typeToken.getParameterTypes());
                        }

                        @Override
                        public int hashCode() {
                            return hashCode;
                        }

                        @Override
                        public String toString() {
                            return typeToken.getParameterTypes().toString();
                        }
                    }
                }

                /**
                 * A harmonizer for the Java virtual machine's method dispatching rules that identifies a method by its parameter types and return type.
                 */
                enum ForJVMMethod implements Harmonizer<ForJVMMethod.Token> {

                    /**
                     * The singleton instance.
                     */
                    INSTANCE;

                    @Override
                    public Token harmonize(MethodDescription.TypeToken typeToken) {
                        return new Token(typeToken);
                    }

                    /**
                     * A token that identifies a Java method's type by its parameter types and return type.
                     */
                    protected static class Token {

                        /**
                         * The represented type token.
                         */
                        private final MethodDescription.TypeToken typeToken;

                        /**
                         * The hash code of this token which is precomputed for to improve performance.
                         */
                        private final int hashCode;

                        /**
                         * Creates a new type token for a JVM method.
                         *
                         * @param typeToken The represented type token.
                         */
                        public Token(MethodDescription.TypeToken typeToken) {
                            this.typeToken = typeToken;
                            hashCode = typeToken.getReturnType().hashCode() + 31 * typeToken.getParameterTypes().hashCode();
                        }

                        @Override
                        public boolean equals(Object other) {
                            if (this == other) {
                                return true;
                            } else if (!(other instanceof Token)) {
                                return false;
                            }
                            Token token = (Token) other;
                            return typeToken.getReturnType().equals(token.typeToken.getReturnType())
                                    && typeToken.getParameterTypes().equals(token.typeToken.getParameterTypes());
                        }

                        @Override
                        public int hashCode() {
                            return hashCode;
                        }

                        @Override
                        public String toString() {
                            return typeToken.toString();
                        }
                    }
                }
            }

            /**
             * Implementations are responsible for identifying a representative method for a {@link net.bytebuddy.dynamic.scaffold.MethodGraph.Node}
             * between several ambiguously resolved methods.
             */
            public interface Merger {

                /**
                 * Merges two ambiguously resolved methods to yield a single representative. 合并两个模糊解析的方法以产生一个代表
                 *
                 * @param left  The left method description, i.e. the method that was discovered first or was previously merged.
                 * @param right The right method description, i.e. the method that was discovered last.
                 * @return A method description compatible to both method's types that is used as a representative.
                 */
                MethodDescription merge(MethodDescription left, MethodDescription right);

                /**
                 * A directional merger that always returns either the left or right method description. 总是返回左或右方法描述的定向合并
                 */
                enum Directional implements Merger {

                    /**
                     * A merger that always returns the left method, i.e. the method that was discovered first or was previously merged.
                     */
                    LEFT(true),

                    /**
                     * A merger that always returns the right method, i.e. the method that was discovered last.
                     */
                    RIGHT(false);

                    /**
                     * {@code true} if the left method should be returned when merging methods.
                     */
                    private final boolean left;

                    /**
                     * Creates a directional merger.
                     *
                     * @param left {@code true} if the left method should be returned when merging methods.
                     */
                    Directional(boolean left) {
                        this.left = left;
                    }

                    @Override
                    public MethodDescription merge(MethodDescription left, MethodDescription right) {
                        return this.left
                                ? left
                                : right;
                    }
                }
            }

            /**
             * A key represents a collection of methods within a method graph to later yield a node representing a collection of methods,
             * i.e. a method representative including information on the required method bridges. 键表示方法图中的方法集合，以稍后产生表示方法集合的节点，即，包括关于所需方法桥的信息的方法代表
             *
             * @param <S> The type of the token used for deciding on method equality. 用于确定方法相等性的标记的类型
             */
            protected abstract static class Key<S> {

                /**
                 * The internal name of the method this key identifies. 此键标识的方法的内部名称
                 */
                protected final String internalName;

                /**
                 * The number of method parameters of the method this key identifies. 此键标识的方法的方法参数数
                 */
                protected final int parameterCount;

                /**
                 * Creates a new key.
                 *
                 * @param internalName   The internal name of the method this key identifies.
                 * @param parameterCount The number of method parameters of the method this key identifies.
                 */
                protected Key(String internalName, int parameterCount) {
                    this.internalName = internalName;
                    this.parameterCount = parameterCount;
                }

                /**
                 * Returns a set of all identifiers of this key.
                 *
                 * @return A set of all identifiers of this key.
                 */
                protected abstract Set<S> getIdentifiers();

                @Override
                public boolean equals(Object other) {
                    if (this == other) {
                        return true;
                    } else if (!(other instanceof Key)) {
                        return false;
                    }
                    Key key = (Key) other;
                    return internalName.equals(key.internalName)
                            && parameterCount == key.parameterCount
                            && !Collections.disjoint(getIdentifiers(), key.getIdentifiers());
                }

                @Override
                public int hashCode() {
                    return internalName.hashCode() + 31 * parameterCount;
                }

                /**
                 * A harmonized key represents a key where equality is decided based on tokens that are returned by a
                 * {@link net.bytebuddy.dynamic.scaffold.MethodGraph.Compiler.Default.Harmonizer}. 协调密钥表示根据 {@link net.bytebuddy.dynamic.scaffold.MethodGraph.Compiler.Default.Harmonizer} 返回的令牌决定相等性的密钥
                 *
                 * @param <V> The type of the tokens yielded by a harmonizer. 协调器产生的令牌类型
                 */
                protected static class Harmonized<V> extends Key<V> {

                    /**
                     * A mapping of identifiers to the type tokens they represent. 标识符到它们所表示的类型标记的映射
                     */
                    private final Map<V, Set<MethodDescription.TypeToken>> identifiers;

                    /**
                     * Creates a new harmonized key.
                     *
                     * @param internalName   The internal name of the method this key identifies.
                     * @param parameterCount The number of method parameters of the method this key identifies.
                     * @param identifiers    A mapping of identifiers to the type tokens they represent.
                     */
                    protected Harmonized(String internalName, int parameterCount, Map<V, Set<MethodDescription.TypeToken>> identifiers) {
                        super(internalName, parameterCount);
                        this.identifiers = identifiers;
                    }

                    /**
                     * Creates a new harmonized key for the given method description.
                     *
                     * @param methodDescription The method description to represent as a harmonized key.
                     * @param harmonizer        The harmonizer to use.
                     * @param <Q>               The type of the token yielded by a harmonizer.
                     * @return A harmonized key representing the provided method.
                     */
                    protected static <Q> Harmonized<Q> of(MethodDescription methodDescription, Harmonizer<Q> harmonizer) {
                        MethodDescription.TypeToken typeToken = methodDescription.asTypeToken();
                        return new Harmonized<Q>(methodDescription.getInternalName(),
                                methodDescription.getParameters().size(),
                                Collections.singletonMap(harmonizer.harmonize(typeToken), Collections.<MethodDescription.TypeToken>emptySet()));
                    }

                    /**
                     * Creates a detached version of this key.
                     *
                     * @param typeToken The type token of the representative method.
                     * @return The detached version of this key.
                     */
                    protected Detached detach(MethodDescription.TypeToken typeToken) {
                        Set<MethodDescription.TypeToken> identifiers = new HashSet<MethodDescription.TypeToken>();
                        for (Set<MethodDescription.TypeToken> typeTokens : this.identifiers.values()) {
                            identifiers.addAll(typeTokens);
                        }
                        identifiers.add(typeToken);
                        return new Detached(internalName, parameterCount, identifiers);
                    }

                    /**
                     * Combines this key with the given key.
                     *
                     * @param key The key to be merged with this key.
                     * @return A harmonized key representing the merger of this key and the given key.
                     */
                    protected Harmonized<V> combineWith(Harmonized<V> key) {
                        Map<V, Set<MethodDescription.TypeToken>> identifiers = new HashMap<V, Set<MethodDescription.TypeToken>>(this.identifiers);
                        for (Map.Entry<V, Set<MethodDescription.TypeToken>> entry : key.identifiers.entrySet()) {
                            Set<MethodDescription.TypeToken> typeTokens = identifiers.get(entry.getKey());
                            if (typeTokens == null) {
                                identifiers.put(entry.getKey(), entry.getValue());
                            } else {
                                typeTokens = new HashSet<MethodDescription.TypeToken>(typeTokens);
                                typeTokens.addAll(entry.getValue());
                                identifiers.put(entry.getKey(), typeTokens);
                            }
                        }
                        return new Harmonized<V>(internalName, parameterCount, identifiers);
                    }

                    /**
                     * Extends this key by the given method description.
                     *
                     * @param methodDescription The method to extend this key with.
                     * @param harmonizer        The harmonizer to use for determining method equality.
                     * @return The harmonized key representing the extension of this key with the provided method.
                     */
                    protected Harmonized<V> extend(MethodDescription.InDefinedShape methodDescription, Harmonizer<V> harmonizer) {
                        Map<V, Set<MethodDescription.TypeToken>> identifiers = new HashMap<V, Set<MethodDescription.TypeToken>>(this.identifiers);
                        MethodDescription.TypeToken typeToken = methodDescription.asTypeToken();
                        V identifier = harmonizer.harmonize(typeToken);
                        Set<MethodDescription.TypeToken> typeTokens = identifiers.get(identifier);
                        if (typeTokens == null) {
                            identifiers.put(identifier, Collections.singleton(typeToken));
                        } else {
                            typeTokens = new HashSet<MethodDescription.TypeToken>(typeTokens);
                            typeTokens.add(typeToken);
                            identifiers.put(identifier, typeTokens);
                        }
                        return new Harmonized<V>(internalName, parameterCount, identifiers);
                    }

                    @Override
                    protected Set<V> getIdentifiers() {
                        return identifiers.keySet();
                    }
                }

                /**
                 * A detached version of a key that identifies methods by their JVM signature, i.e. parameter types and return type. 一种键的分离版本，它通过方法的JVM签名（即参数类型和返回类型）来标识方法
                 */
                protected static class Detached extends Key<MethodDescription.TypeToken> {

                    /**
                     * The type tokens represented by this key. 此键表示的类型标记
                     */
                    private final Set<MethodDescription.TypeToken> identifiers;

                    /**
                     * Creates a new detached key.
                     *
                     * @param internalName   The internal name of the method this key identifies. 此键标识的方法的内部名称
                     * @param parameterCount The number of method parameters of the method this key identifies. 此键标识的方法的方法参数数
                     * @param identifiers    The type tokens represented by this key. 此键表示的类型标记
                     */
                    protected Detached(String internalName, int parameterCount, Set<MethodDescription.TypeToken> identifiers) {
                        super(internalName, parameterCount);
                        this.identifiers = identifiers;
                    }

                    /**
                     * Creates a new detached key of the given method token. 创建给定方法令牌的新分离密钥
                     *
                     * @param token The method token to represent as a key.
                     * @return A detached key representing the given method token..
                     */
                    protected static Detached of(MethodDescription.SignatureToken token) {
                        return new Detached(token.getName(), token.getParameterTypes().size(), Collections.singleton(token.asTypeToken()));
                    }

                    @Override
                    protected Set<MethodDescription.TypeToken> getIdentifiers() {
                        return identifiers;
                    }
                }

                /**
                 * A store for collected methods that are identified by keys. 由键标识的已收集方法的存储区
                 *
                 * @param <V> The type of the token used for deciding on method equality.
                 */
                @HashCodeAndEqualsPlugin.Enhance
                protected static class Store<V> {

                    /**
                     * A mapping of harmonized keys to their represented entry. 协调键到它们所代表的条目的映射
                     */
                    private final LinkedHashMap<Harmonized<V>, Entry<V>> entries;

                    /**
                     * Creates an empty store.
                     */
                    protected Store() {
                        this(new LinkedHashMap<Harmonized<V>, Entry<V>>());
                    }

                    /**
                     * Creates a new store representing the given entries. 创建表示给定项的新存储
                     *
                     * @param entries A mapping of harmonized keys to their represented entry.
                     */
                    private Store(LinkedHashMap<Harmonized<V>, Entry<V>> entries) {
                        this.entries = entries;
                    }

                    /**
                     * Combines the two given stores.
                     *
                     * @param left  The left store to be combined.
                     * @param right The right store to be combined.
                     * @param <W>   The type of the harmonized key of both stores.
                     * @return An entry representing the combination of both stores.
                     */
                    private static <W> Entry<W> combine(Entry<W> left, Entry<W> right) {
                        Set<MethodDescription> leftMethods = left.getCandidates(), rightMethods = right.getCandidates();
                        LinkedHashSet<MethodDescription> combined = new LinkedHashSet<MethodDescription>();
                        combined.addAll(leftMethods);
                        combined.addAll(rightMethods);
                        for (MethodDescription leftMethod : leftMethods) {
                            TypeDescription leftType = leftMethod.getDeclaringType().asErasure();
                            for (MethodDescription rightMethod : rightMethods) {
                                TypeDescription rightType = rightMethod.getDeclaringType().asErasure();
                                if (leftType.equals(rightType)) {
                                    break;
                                } else if (leftType.isAssignableTo(rightType)) {
                                    combined.remove(rightMethod);
                                    break;
                                } else if (leftType.isAssignableFrom(rightType)) {
                                    combined.remove(leftMethod);
                                    break;
                                }
                            }
                        }
                        Key.Harmonized<W> key = left.getKey().combineWith(right.getKey());
                        Visibility visibility = left.getVisibility().expandTo(right.getVisibility());
                        return combined.size() == 1
                                ? new Entry.Resolved<W>(key, combined.iterator().next(), visibility, Entry.Resolved.NOT_MADE_VISIBLE)
                                : new Entry.Ambiguous<W>(key, combined, visibility);
                    }

                    /**
                     * Registers a new top level method within this store.
                     *
                     * @param methodDescriptions The methods to register.
                     * @param harmonizer         The harmonizer to use for determining method equality.
                     * @return A store with the given method registered as a top-level method.
                     */
                    protected Store<V> registerTopLevel(List<? extends MethodDescription> methodDescriptions, Harmonizer<V> harmonizer) {
                        if (methodDescriptions.isEmpty()) {
                            return this;
                        }
                        LinkedHashMap<Harmonized<V>, Entry<V>> entries = new LinkedHashMap<Harmonized<V>, Entry<V>>(this.entries);
                        for (MethodDescription methodDescription : methodDescriptions) {
                            Harmonized<V> key = Harmonized.of(methodDescription, harmonizer);
                            Entry<V> currentEntry = entries.remove(key), extendedEntry = (currentEntry == null
                                    ? new Entry.Initial<V>(key)
                                    : currentEntry).extendBy(methodDescription, harmonizer);
                            entries.put(extendedEntry.getKey(), extendedEntry);
                        }
                        return new Store<V>(entries);
                    }

                    /**
                     * Combines this store with the given store.
                     *
                     * @param store The store to combine with this store.
                     * @return A store representing a combination of this store and the given store.
                     */
                    protected Store<V> combineWith(Store<V> store) {
                        if (entries.isEmpty()) {
                            return store;
                        } else if (store.entries.isEmpty()) {
                            return this;
                        }
                        LinkedHashMap<Harmonized<V>, Entry<V>> entries = new LinkedHashMap<Harmonized<V>, Entry<V>>(this.entries);
                        for (Entry<V> entry : store.entries.values()) {
                            Entry<V> previousEntry = entries.remove(entry.getKey()), injectedEntry = previousEntry == null
                                    ? entry
                                    : combine(previousEntry, entry);
                            entries.put(injectedEntry.getKey(), injectedEntry);
                        }
                        return new Store<V>(entries);
                    }

                    /**
                     * Injects the given store into this store.
                     *
                     * @param store The key store to inject into this store.
                     * @return A store that represents this store with the given store injected.
                     */
                    protected Store<V> inject(Store<V> store) {
                        if (entries.isEmpty()) {
                            return store;
                        } else if (store.entries.isEmpty()) {
                            return this;
                        }
                        LinkedHashMap<Harmonized<V>, Entry<V>> entries = new LinkedHashMap<Harmonized<V>, Entry<V>>(this.entries);
                        for (Entry<V> entry : store.entries.values()) {
                            Entry<V> dominantEntry = entries.remove(entry.getKey()), injectedEntry = dominantEntry == null
                                    ? entry
                                    : dominantEntry.inject(entry.getKey(), entry.getVisibility());
                            entries.put(injectedEntry.getKey(), injectedEntry);
                        }
                        return new Store<V>(entries);
                    }

                    /**
                     * Transforms this store into a method graph by applying the given merger. 通过应用给定的合并将此存储转换为方法图
                     *
                     * @param merger The merger to apply for resolving the representative for ambiguous resolutions. 申请解决合并代表人决议不明确的
                     * @return The method graph that represents this key store.
                     */
                    protected MethodGraph asGraph(Merger merger) {
                        LinkedHashMap<Key<MethodDescription.TypeToken>, Node> entries = new LinkedHashMap<Key<MethodDescription.TypeToken>, Node>();
                        for (Entry<V> entry : this.entries.values()) {
                            Node node = entry.asNode(merger);
                            entries.put(entry.getKey().detach(node.getRepresentative().asTypeToken()), node);
                        }
                        return new Graph(entries);
                    }

                    /**
                     * An entry of a key store.
                     *
                     * @param <W> The type of the harmonized token used for determining method equality.
                     */
                    protected interface Entry<W> {

                        /**
                         * Returns the harmonized key of this entry.
                         *
                         * @return The harmonized key of this entry.
                         */
                        Harmonized<W> getKey();

                        /**
                         * Returns all candidate methods represented by this entry.
                         *
                         * @return All candidate methods represented by this entry.
                         */
                        Set<MethodDescription> getCandidates();

                        /**
                         * Returns the minimal visibility of this entry.
                         *
                         * @return The minimal visibility of this entry.
                         */
                        Visibility getVisibility();

                        /**
                         * Extends this entry by the given method.
                         *
                         * @param methodDescription The method description to extend this entry with.
                         * @param harmonizer        The harmonizer to use for determining method equality.
                         * @return This key extended by the given method.
                         */
                        Entry<W> extendBy(MethodDescription methodDescription, Harmonizer<W> harmonizer);

                        /**
                         * Injects the given key into this entry.
                         *
                         * @param key        The key to inject into this entry.
                         * @param visibility The entry's minimal visibility.
                         * @return This entry extended with the given key.
                         */
                        Entry<W> inject(Harmonized<W> key, Visibility visibility);

                        /**
                         * Transforms this entry into a node.
                         *
                         * @param merger The merger to use for determining the representative method of an ambiguous node.
                         * @return The resolved node.
                         */
                        Node asNode(Merger merger);

                        /**
                         * An entry in its initial state before registering any method as a representative.
                         *
                         * @param <U> The type of the harmonized key to determine method equality.
                         */
                        class Initial<U> implements Entry<U> {

                            /**
                             * The harmonized key this entry represents.
                             */
                            private final Harmonized<U> key;

                            /**
                             * Creates a new initial key.
                             *
                             * @param key The harmonized key this entry represents.
                             */
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
                            public Visibility getVisibility() {
                                throw new IllegalStateException("Cannot extract visibility from initial entry:" + this);
                            }

                            @Override
                            public Entry<U> extendBy(MethodDescription methodDescription, Harmonizer<U> harmonizer) {
                                return new Resolved<U>(key.extend(methodDescription.asDefined(), harmonizer),
                                        methodDescription,
                                        methodDescription.getVisibility(),
                                        Resolved.NOT_MADE_VISIBLE);
                            }

                            @Override
                            public Entry<U> inject(Harmonized<U> key, Visibility visibility) {
                                throw new IllegalStateException("Cannot inject into initial entry without a registered method: " + this);
                            }

                            @Override
                            public Node asNode(Merger merger) {
                                throw new IllegalStateException("Cannot transform initial entry without a registered method: " + this);
                            }

                            @Override
                            public boolean equals(Object other) {
                                if (this == other) {
                                    return true;
                                } else if (other == null || getClass() != other.getClass()) {
                                    return false;
                                }
                                Initial<?> initial = (Initial<?>) other;
                                return key.equals(initial.key);
                            }

                            @Override
                            public int hashCode() {
                                return key.hashCode();
                            }
                        }

                        /**
                         * An entry representing a non-ambiguous node resolution.
                         *
                         * @param <U> The type of the harmonized key to determine method equality.
                         */
                        @HashCodeAndEqualsPlugin.Enhance
                        class Resolved<U> implements Entry<U> {

                            /**
                             * Indicates that a type's methods are already globally visible, meaning that a bridge method is not added
                             * with the intend of creating a visibility bridge.
                             */
                            private static final int MADE_VISIBLE = Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED;

                            /**
                             * Indicates that the entry was not made visible.
                             */
                            private static final boolean NOT_MADE_VISIBLE = false;

                            /**
                             * The harmonized key this entry represents.
                             */
                            private final Harmonized<U> key;

                            /**
                             * The non-ambiguous, representative method of this entry.
                             */
                            private final MethodDescription methodDescription;

                            /**
                             * The minimal required visibility for this method.
                             */
                            private final Visibility visibility;

                            /**
                             * {@code true} if this entry's representative was made visible by a visibility bridge.
                             */
                            private final boolean madeVisible;

                            /**
                             * Creates a new resolved entry.
                             *
                             * @param key               The harmonized key this entry represents.
                             * @param methodDescription The non-ambiguous, representative method of this entry.
                             * @param visibility        The minimal required visibility for this method.
                             * @param madeVisible       {@code true} if this entry's representative was made visible by a visibility bridge.
                             */
                            protected Resolved(Harmonized<U> key, MethodDescription methodDescription, Visibility visibility, boolean madeVisible) {
                                this.key = key;
                                this.methodDescription = methodDescription;
                                this.visibility = visibility;
                                this.madeVisible = madeVisible;
                            }

                            /**
                             * Creates an entry for an override where a method overrides another method within a super class.
                             *
                             * @param key        The merged key for both methods.
                             * @param override   The method declared by the extending type, potentially a bridge method.
                             * @param original   The method that is overridden by the extending type.
                             * @param visibility The minimal required visibility for this entry.
                             * @param <V>        The type of the harmonized key to determine method equality.
                             * @return An entry representing the merger of both methods.
                             */
                            private static <V> Entry<V> of(Harmonized<V> key, MethodDescription override, MethodDescription original, Visibility visibility) {
                                visibility = visibility.expandTo(original.getVisibility()).expandTo(override.getVisibility());
                                return override.isBridge()
                                        ? new Resolved<V>(key, original, visibility, (original.getDeclaringType().getModifiers() & MADE_VISIBLE) == 0)
                                        : new Resolved<V>(key, override, visibility, NOT_MADE_VISIBLE);
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
                            public Visibility getVisibility() {
                                return visibility;
                            }

                            @Override
                            public Entry<U> extendBy(MethodDescription methodDescription, Harmonizer<U> harmonizer) {
                                Harmonized<U> key = this.key.extend(methodDescription.asDefined(), harmonizer);
                                Visibility visibility = this.visibility.expandTo(methodDescription.getVisibility());
                                return methodDescription.getDeclaringType().equals(this.methodDescription.getDeclaringType())
                                        ? Ambiguous.of(key, methodDescription, this.methodDescription, visibility)
                                        : Resolved.of(key, methodDescription, this.methodDescription, visibility);
                            }

                            @Override
                            public Entry<U> inject(Harmonized<U> key, Visibility visibility) {
                                return new Resolved<U>(this.key.combineWith(key), methodDescription, this.visibility.expandTo(visibility), madeVisible);
                            }

                            @Override
                            public MethodGraph.Node asNode(Merger merger) {
                                return new Node(key.detach(methodDescription.asTypeToken()), methodDescription, visibility, madeVisible);
                            }

                            /**
                             * A node implementation representing a non-ambiguous method.
                             */
                            @HashCodeAndEqualsPlugin.Enhance
                            protected static class Node implements MethodGraph.Node {

                                /**
                                 * The detached key representing this node.
                                 */
                                private final Detached key;

                                /**
                                 * The representative method of this node.
                                 */
                                private final MethodDescription methodDescription;

                                /**
                                 * The node's minimal visibility.
                                 */
                                private final Visibility visibility;

                                /**
                                 * {@code true} if the represented method was made explicitly visible by a visibility bridge.
                                 */
                                private final boolean visible;

                                /**
                                 * Creates a new node.
                                 *
                                 * @param key               The detached key representing this node.
                                 * @param methodDescription The representative method of this node.
                                 * @param visibility        The node's minimal visibility.
                                 * @param visible           {@code true} if the represented method was made explicitly visible by a visibility bridge.
                                 */
                                protected Node(Detached key, MethodDescription methodDescription, Visibility visibility, boolean visible) {
                                    this.key = key;
                                    this.methodDescription = methodDescription;
                                    this.visibility = visibility;
                                    this.visible = visible;
                                }

                                @Override
                                public Sort getSort() {
                                    return visible
                                            ? Sort.VISIBLE
                                            : Sort.RESOLVED;
                                }

                                @Override
                                public MethodDescription getRepresentative() {
                                    return methodDescription;
                                }

                                @Override
                                public Set<MethodDescription.TypeToken> getMethodTypes() {
                                    return key.getIdentifiers();
                                }

                                @Override
                                public Visibility getVisibility() {
                                    return visibility;
                                }
                            }
                        }

                        /**
                         * An entry representing an ambiguous node resolution.
                         *
                         * @param <U> The type of the harmonized key to determine method equality.
                         */
                        @HashCodeAndEqualsPlugin.Enhance
                        class Ambiguous<U> implements Entry<U> {

                            /**
                             * The harmonized key this entry represents.
                             */
                            private final Harmonized<U> key;

                            /**
                             * A set of ambiguous methods that this entry represents.
                             */
                            private final LinkedHashSet<MethodDescription> methodDescriptions;

                            /**
                             * The minimal required visibility for this method.
                             */
                            private final Visibility visibility;

                            /**
                             * Creates a new ambiguous entry.
                             *
                             * @param key                The harmonized key this entry represents.
                             * @param methodDescriptions A set of ambiguous methods that this entry represents.
                             * @param visibility         The minimal required visibility for this method.
                             */
                            protected Ambiguous(Harmonized<U> key, LinkedHashSet<MethodDescription> methodDescriptions, Visibility visibility) {
                                this.key = key;
                                this.methodDescriptions = methodDescriptions;
                                this.visibility = visibility;
                            }

                            /**
                             * Creates a new ambiguous entry if both provided entries are not considered to be a bridge of one another.
                             *
                             * @param key        The key of the entry to be created.
                             * @param left       The left method to be considered.
                             * @param right      The right method to be considered.
                             * @param visibility The entry's minimal visibility.
                             * @param <Q>        The type of the token of the harmonized key to determine method equality.
                             * @return The entry representing both methods.
                             */
                            protected static <Q> Entry<Q> of(Harmonized<Q> key, MethodDescription left, MethodDescription right, Visibility visibility) {
                                visibility = visibility.expandTo(left.getVisibility()).expandTo(right.getVisibility());
                                return left.isBridge() ^ right.isBridge()
                                        ? new Resolved<Q>(key, left.isBridge() ? right : left, visibility, Resolved.NOT_MADE_VISIBLE)
                                        : new Ambiguous<Q>(key, new LinkedHashSet<MethodDescription>(Arrays.asList(left, right)), visibility);
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
                            public Visibility getVisibility() {
                                return visibility;
                            }

                            @Override
                            public Entry<U> extendBy(MethodDescription methodDescription, Harmonizer<U> harmonizer) {
                                Harmonized<U> key = this.key.extend(methodDescription.asDefined(), harmonizer);
                                LinkedHashSet<MethodDescription> methodDescriptions = new LinkedHashSet<MethodDescription>();
                                TypeDescription declaringType = methodDescription.getDeclaringType().asErasure();
                                boolean bridge = methodDescription.isBridge();
                                Visibility visibility = this.visibility;
                                for (MethodDescription extendedMethod : this.methodDescriptions) {
                                    if (extendedMethod.getDeclaringType().asErasure().equals(declaringType)) {
                                        if (extendedMethod.isBridge() ^ bridge) {
                                            methodDescriptions.add(bridge ? extendedMethod : methodDescription);
                                        } else {
                                            methodDescriptions.add(methodDescription);
                                            methodDescriptions.add(extendedMethod);
                                        }
                                    }
                                    visibility = visibility.expandTo(extendedMethod.getVisibility());
                                }
                                if (methodDescriptions.isEmpty()) {
                                    return new Resolved<U>(key, methodDescription, visibility, bridge);
                                } else if (methodDescriptions.size() == 1) {
                                    return new Resolved<U>(key, methodDescriptions.iterator().next(), visibility, Resolved.NOT_MADE_VISIBLE);
                                } else {
                                    return new Ambiguous<U>(key, methodDescriptions, visibility);
                                }
                            }

                            @Override
                            public Entry<U> inject(Harmonized<U> key, Visibility visibility) {
                                return new Ambiguous<U>(this.key.combineWith(key), methodDescriptions, this.visibility.expandTo(visibility));
                            }

                            @Override
                            public MethodGraph.Node asNode(Merger merger) {
                                Iterator<MethodDescription> iterator = methodDescriptions.iterator();
                                MethodDescription methodDescription = iterator.next();
                                while (iterator.hasNext()) {
                                    methodDescription = merger.merge(methodDescription, iterator.next());
                                }
                                return new Node(key.detach(methodDescription.asTypeToken()), methodDescription, visibility);
                            }

                            /**
                             * A node implementation representing an ambiguous method resolution.
                             */
                            @HashCodeAndEqualsPlugin.Enhance
                            protected static class Node implements MethodGraph.Node {

                                /**
                                 * The detached key representing this node.
                                 */
                                private final Detached key;

                                /**
                                 * The representative method of this node.
                                 */
                                private final MethodDescription methodDescription;

                                /**
                                 * The node's minimal visibility.
                                 */
                                private final Visibility visibility;

                                /**
                                 * @param key               The detached key representing this node.
                                 * @param methodDescription The representative method of this node.
                                 * @param visibility        The node's minimal visibility.
                                 */
                                protected Node(Detached key, MethodDescription methodDescription, Visibility visibility) {
                                    this.key = key;
                                    this.methodDescription = methodDescription;
                                    this.visibility = visibility;
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
                                public Set<MethodDescription.TypeToken> getMethodTypes() {
                                    return key.getIdentifiers();
                                }

                                @Override
                                public Visibility getVisibility() {
                                    return visibility;
                                }
                            }
                        }
                    }

                    /**
                     * A graph implementation based on a key store.
                     */
                    @HashCodeAndEqualsPlugin.Enhance
                    protected static class Graph implements MethodGraph {

                        /**
                         * A mapping of a node's type tokens to the represented node.
                         */
                        private final LinkedHashMap<Key<MethodDescription.TypeToken>, Node> entries;

                        /**
                         * Creates a new graph.
                         *
                         * @param entries A mapping of a node's type tokens to the represented node.
                         */
                        protected Graph(LinkedHashMap<Key<MethodDescription.TypeToken>, Node> entries) {
                            this.entries = entries;
                        }

                        @Override
                        public Node locate(MethodDescription.SignatureToken token) {
                            Node node = entries.get(Detached.of(token));
                            return node == null
                                    ? Node.Unresolved.INSTANCE
                                    : node;
                        }

                        @Override
                        public NodeList listNodes() {
                            return new NodeList(new ArrayList<Node>(entries.values()));
                        }
                    }
                }
            }
        }
    }

    /**
     * A list of nodes.
     */
    class NodeList extends FilterableList.AbstractBase<Node, NodeList> {

        /**
         * The represented nodes.
         */
        private final List<? extends Node> nodes;

        /**
         * Creates a list of nodes.
         *
         * @param nodes The represented nodes.
         */
        public NodeList(List<? extends Node> nodes) {
            this.nodes = nodes;
        }

        @Override
        public Node get(int index) {
            return nodes.get(index);
        }

        @Override
        public int size() {
            return nodes.size();
        }

        @Override
        protected NodeList wrap(List<Node> values) {
            return new NodeList(values);
        }

        /**
         * Transforms this list of nodes into a list of the node's representatives.
         *
         * @return A list of these node's representatives.
         */
        public MethodList<?> asMethodList() {
            List<MethodDescription> methodDescriptions = new ArrayList<MethodDescription>(size());
            for (Node node : nodes) {
                methodDescriptions.add(node.getRepresentative());
            }
            return new MethodList.Explicit<MethodDescription>(methodDescriptions);
        }
    }

    /**
     * A simple implementation of a method graph. 方法图的简单实现
     */
    @HashCodeAndEqualsPlugin.Enhance
    class Simple implements MethodGraph {

        /**
         * The nodes represented by this method graph. 本方法图表示的节点
         */
        private final LinkedHashMap<MethodDescription.SignatureToken, Node> nodes;

        /**
         * Creates a new simple method graph.
         *
         * @param nodes The nodes represented by this method graph.
         */
        public Simple(LinkedHashMap<MethodDescription.SignatureToken, Node> nodes) {
            this.nodes = nodes;
        }

        /**
         * Returns a method graph that contains all of the provided methods as simple nodes.
         *
         * @param methodDescriptions A list of method descriptions to be represented as simple nodes.
         * @return A method graph that represents all of the provided methods as simple nodes.
         */
        public static MethodGraph of(List<? extends MethodDescription> methodDescriptions) {
            LinkedHashMap<MethodDescription.SignatureToken, Node> nodes = new LinkedHashMap<MethodDescription.SignatureToken, Node>();
            for (MethodDescription methodDescription : methodDescriptions) {
                nodes.put(methodDescription.asSignatureToken(), new Node.Simple(methodDescription));
            }
            return new Simple(nodes);
        }

        @Override
        public Node locate(MethodDescription.SignatureToken token) {
            Node node = nodes.get(token);
            return node == null
                    ? Node.Unresolved.INSTANCE
                    : node;
        }

        @Override
        public NodeList listNodes() {
            return new NodeList(new ArrayList<Node>(nodes.values()));
        }
    }
}
