package net.bytebuddy.description;

import net.bytebuddy.description.annotation.AnnotationSource;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.FilterableList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implementations describe an element represented in byte code, i.e. a type, a field or a method or a constructor. 实现描述以字节码表示的元素，即类型、字段、方法或构造函数
 */
public interface ByteCodeElement extends NamedElement.WithRuntimeName, ModifierReviewable, DeclaredByType, AnnotationSource {

    /**
     * The generic type signature of a non-generic byte code element. 非泛型字节码元素的泛型类型签名
     */
    String NON_GENERIC_SIGNATURE = null;

    /**
     * Returns the descriptor of this byte code element. 返回此字节码元素的描述符
     *
     * @return The descriptor of this byte code element.
     */
    String getDescriptor();

    /**
     * Returns the generic signature of this byte code element. If this element does not reference generic types
     * or references malformed generic types, {@code null} is returned as a signature.
     *
     * @return The generic signature or {@code null} if this element is not generic.
     */
    String getGenericSignature();

    /**
     * <p> 对于传入的类型是否可见，比如同级的classLoader加载类，就互相看不见
     * Checks if this element is visible from a given type. Visibility is a wider criteria then accessibility which can be checked by
     * {@link ByteCodeElement#isAccessibleTo(TypeDescription)}. Visibility allows the invocation of a method on itself or on external
     * instances.
     * </p>
     * <p>
     * <b>Note</b>: A method or field might define a signature that includes types that are not visible to a type. Such methods can be  方法或字段可以定义一个签名，其中包含对某个类型不可见的类型
     * legally invoked from this type and can even be implemented as bridge methods by this type. It is however not legal to declare    这样的方法可以合法地从这个类型调用，甚至可以通过这个类型实现为桥方法
     * a method with invisible types in its signature that are not bridges what might require additional validation. 但是，在签名中声明不可见类型的方法是不合法的，因为这些类型不是可能需要额外验证的桥
     * </p>
     * <p>
     * <b>Important</b>: Virtual byte code elements, i.e. virtual methods, are only considered visible if the type they are invoked upon
     * is visible to a given type. The visibility of such virtual members can therefore not be determined by only investigating the invoked
     * method but requires an additional check of the target type. 虚拟字节码元素，即虚拟方法，只有在调用它们的类型对给定类型可见时才被认为是可见的。因此，这种虚拟成员的可见性不能仅通过调查调用的方法来确定，而需要对目标类型进行额外的检查
     * </p>
     *
     * @param typeDescription The type which is checked for its visibility of this element.
     * @return {@code true} if this element is visible for {@code typeDescription}.
     */
    boolean isVisibleTo(TypeDescription typeDescription);

    /**
     * <p> 检查是否可以从给定类型访问此元素  对于传入的类型是否权限访问，比如其他类看不见 private
     * Checks if this element is accessible from a given type. Accessibility is a more narrow criteria then visibility which can be
     * checked by {@link ByteCodeElement#isVisibleTo(TypeDescription)}. Accessibility allows the invocation of a method on external
     * instances or on itself. Methods that can be invoked from within an instance might however not be considered accessible.
     * </p>
     * <p> 方法或字段可以定义一个签名，其中包含对某个类型不可见的类型, 这样的方法可以合法地从这个类型调用，甚至可以通过这个类型实现为桥方法。但是，在签名中声明不可见类型的方法是不合法的，因为这些类型不是可能需要额外验证的桥
     * <b>Note</b>: A method or field might define a signature that includes types that are not visible to a type. Such methods can be
     * legally invoked from this type and can even be implemented as bridge methods by this type. It is however not legal to declare
     * a method with invisible types in its signature that are not bridges what might require additional validation.
     * </p>
     * <p>
     * <b>Important</b>: Virtual byte code elements, i.e. virtual methods, are only considered visible if the type they are invoked upon
     * is visible to a given type. The visibility of such virtual members can therefore not be determined by only investigating the invoked
     * method but requires an additional check of the target type.
     * </p>
     *
     * @param typeDescription The type which is checked for its accessibility of this element.
     * @return {@code true} if this element is accessible for {@code typeDescription}.
     */
    boolean isAccessibleTo(TypeDescription typeDescription);

    /**
     * A type dependant describes an element that is an extension of a type definition, i.e. a field, method or method parameter. 类型依赖项描述了作为类型定义扩展的元素，即字段、方法或方法参数
     * 受扶养者 -> 一个复合类 充当 ElementMatcher 和 Token 的桥梁
     * @param <T> The type dependant's type. 类型依赖项的类型
     * @param <S> The type dependant's token type. 类型依赖项的令牌类型
     */
    interface TypeDependant<T extends TypeDependant<?, S>, S extends ByteCodeElement.Token<S>> {

        /**
         * Returns this type dependant in its defined shape, i.e. the form it is declared in and without its type variable's resolved. 以其定义的形状返回此类型相关项，即声明它的形式，而不解析其类型变量
         * 比如一个类 TypeDependantImpl implments TypeDependant，那么asDefined()就是返回TypeDependantImpl的作用，就是返回定义的类
         * @return This type dependant in its defined shape. 此类型依赖于其定义的形状
         */
        T asDefined();

        /**
         * Returns a token representative of this type dependant. All types that are matched by the supplied matcher are replaced by
         * {@link net.bytebuddy.dynamic.TargetType} descriptions. 返回表示此类型依赖项的标记。所提供的匹配器匹配的所有类型都将替换为 TargetType 描述
         * 把一个 ElementMatcher 变成 Token
         * @param matcher A matcher to identify types to be replaced by {@link net.bytebuddy.dynamic.TargetType} descriptions. 用于标识要由TargetType描述替换的类型的匹配器
         * @return A token representative of this type dependant.
         */
        S asToken(ElementMatcher<? super TypeDescription> matcher);
    }

    /**
     * A token representing a byte code element. 表示字节码元素的标记  这个是链式的，一个 token 继承着上一个 token，意味着一个Token，前面有很多限定条件，他必须是谁的子类，这样你能精确识别 Token
     *
     * @param <T> The type of the implementation.
     */
    interface Token<T extends Token<T>> {

        /**
         * Transforms the types represented by this token by applying the given visitor to them. 通过将给定的访问者应用于此标记所表示的类型来转换这些类型
         *
         * @param visitor The visitor to transform all types that are represented by this token. 访问者可以转换此标记表示的所有类型
         * @return This token with all of its represented types transformed by the supplied visitor. 此令牌及其所有由提供的访问者转换的表示类型
         */
        T accept(TypeDescription.Generic.Visitor<? extends TypeDescription.Generic> visitor);

        /**
         * A list of tokens. 令牌列表   token -> 代表真实的字节码  ASM 的 CoreAPI 接受 ClassVister 遍历 class 字节码。 Token也是类似的，被用来遍历字节码。  一个 Token 就是一个字节码元素
         *
         * @param <S> The actual token type.
         */
        class TokenList<S extends Token<S>> extends FilterableList.AbstractBase<S, TokenList<S>> {

            /**
             * The tokens that this list represents. 此列表表示的标记
             */
            private final List<? extends S> tokens;

            /**
             * Creates a list of tokens. 创建令牌列表
             *
             * @param token The tokens that this list represents. 此列表表示的标记
             */
            @SuppressWarnings("unchecked")
            public TokenList(S... token) {
                this(Arrays.asList(token));
            }

            /**
             * Creates a list of tokens. 创建令牌列表
             *
             * @param tokens The tokens that this list represents. 此列表表示的标记
             */
            public TokenList(List<? extends S> tokens) {
                this.tokens = tokens;
            }

            /**
             * Transforms all tokens that are represented by this list. 转换此列表表示的所有标记  accept 方法，接受一个visitor，作为下一个类型的参数。这是访问者模式
             *
             * @param visitor The visitor to apply to all tokens. 应用所有的令牌的访问者
             * @return A list containing the transformed tokens.  包含已转换标记的令牌
             */
            public TokenList<S> accept(TypeDescription.Generic.Visitor<? extends TypeDescription.Generic> visitor) {
                List<S> tokens = new ArrayList<S>(this.tokens.size());
                for (S token : this.tokens) {
                    tokens.add(token.accept(visitor));
                }
                return new TokenList<S>(tokens);
            }

            @Override
            protected TokenList<S> wrap(List<S> values) {
                return new TokenList<S>(values);
            }

            @Override
            public S get(int index) {
                return tokens.get(index);
            }

            @Override
            public int size() {
                return tokens.size();
            }
        }
    }
}
