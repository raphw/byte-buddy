package net.bytebuddy.description;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.description.type.TypeList;

import static net.bytebuddy.matcher.ElementMatchers.named;

/** java中有一个TypeVariable,这个是泛型中的变量，比如List<T>。这个T就是TypeVariable -> Byte Buddy 中的 TypeVariableSource 和 TypeVariable 并不对应。这里含义更广 : 代表了 code element 的类型
 * A type variable source represents a code element that can declare type variables. 类型变量源表示可以声明类型变量的代码元素 代表了字节码的类型
 */
public interface TypeVariableSource extends ModifierReviewable.OfAbstraction {

    /**
     * Indicates that a type variable source is undefined. 指示类型变量源未定义
     */
    TypeVariableSource UNDEFINED = null;

    /**
     * Returns the type variables that are declared by this element. 返回此元素声明的类型变量
     *
     * @return The type variables that are declared by this element.
     */
    TypeList.Generic getTypeVariables();

    /**
     * Returns the enclosing source of type variables that are valid in the scope of this type variable source. 返回在此类型变量源范围内有效的类型变量的封闭源  获取外层的包裹类或者方法
     * java 自身的 getEnclosingMethod() 和 getEnclosingClass() 是一样的效果，getEnclosingSource() 相当于两者之和，这个是在匿名类时特别有用
     * @return The enclosing source or {@code null} if no such source exists.
     */
    TypeVariableSource getEnclosingSource();

    /**
     * Finds a particular variable with the given name in the closes type variable source that is visible from this instance.
     *
     * @param symbol The symbolic name of the type variable.
     * @return The type variable.
     */
    TypeDescription.Generic findVariable(String symbol);

    /**
     * Applies a visitor on this type variable source.
     *
     * @param visitor The visitor to apply.
     * @param <T>     The visitor's return type.
     * @return The visitor's return value.
     */
    <T> T accept(Visitor<T> visitor);

    /**
     * Checks if this type variable source has a generic declaration. This means:
     * <ul>
     * <li>A type declares type variables or is an inner class of a type with a generic declaration.</li>
     * <li>A method declares at least one type variable.</li>
     * </ul>
     *
     * @return {@code true} if this type code element has a generic declaration.
     */
    boolean isGenerified();

    /**
     * A visitor that can be applied to a type variable source. 可以应用于类型变量源的访问者
     *
     * @param <T> The visitor's return type. 访客的返回类型
     */
    interface Visitor<T> {

        /**
         * Applies the visitor on a type. 将访问者应用于类型
         *
         * @param typeDescription The type onto which this visitor is applied. 应用此访问者的类型
         * @return The visitor's return value.
         */
        T onType(TypeDescription typeDescription);

        /**
         * Applies the visitor on a method. 在方法上应用访问者
         *
         * @param methodDescription The method onto which this visitor is applied.
         * @return The visitor's return value.
         */
        T onMethod(MethodDescription.InDefinedShape methodDescription);

        /**
         * A none-operational implementation of a type variable visitor that simply returns the visited source. 类型变量visitor的一种不可操作的实现，它只返回被访问的源  NoOp就是统一对类型不加工，传入什么样返回什么样
         */
        enum NoOp implements Visitor<TypeVariableSource> {

            /**
             * The singleton instance.
             */
            INSTANCE;

            @Override
            public TypeVariableSource onType(TypeDescription typeDescription) {
                return typeDescription;
            }

            @Override
            public TypeVariableSource onMethod(MethodDescription.InDefinedShape methodDescription) {
                return methodDescription;
            }
        }
    }

    /**
     * An abstract base implementation of a type variable source.
     */
    abstract class AbstractBase extends ModifierReviewable.AbstractBase implements TypeVariableSource {

        @Override
        public TypeDescription.Generic findVariable(String symbol) {
            TypeList.Generic typeVariables = getTypeVariables().filter(named(symbol));
            if (typeVariables.isEmpty()) {
                TypeVariableSource enclosingSource = getEnclosingSource();
                return enclosingSource == null
                        ? TypeDescription.Generic.UNDEFINED
                        : enclosingSource.findVariable(symbol);
            } else {
                return typeVariables.getOnly();
            }
        }
    }
}
