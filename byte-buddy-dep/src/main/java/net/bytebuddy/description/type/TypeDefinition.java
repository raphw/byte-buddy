package net.bytebuddy.description.type;

import net.bytebuddy.description.ModifierReviewable;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.field.FieldList;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.implementation.bytecode.StackSize;

import java.lang.reflect.*;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Implementations define a type, either as a {@link TypeDescription} or as a {@link TypeDescription.Generic}. 实现将类型定义为{@link TypeDescription}或{@link TypeDescription.Generic}.  代表了一个类型的定义，真实的定义要么是TypeDescription，要么是TypeDescription.Generic
 */
public interface TypeDefinition extends NamedElement, ModifierReviewable.ForTypeDefinition, Iterable<TypeDefinition> {

    /**
     * <p>
     * If this property is set to {@code true}, non-generic {@link TypeDefinition}s do no longer resolve their referenced  如果此属性设置为{@code true}，则非泛型{@link TypeDefinition}在遍历类型层次结构时不再解析其引用的泛型类型
     * generic types when traversing type hierarchies. Setting this property can cause unexpected side effects such as
     * {@link ClassCastException}s from overridden methods as type variables are resolved to their erasures where a method
     * might return that is unexpected by the callee. Setting this property also makes type annotations unavailable using
     * such type navigation. 设置此属性可能会导致意外的副作用，例如重写方法的{@link ClassCastException}，因为类型变量被解析为其擦除，其中方法可能返回被调用方意外的结果。设置此属性还使得使用此类类型导航的类型注释不可用
     * </p>
     * <p>
     * Setting this property can be useful if generic type information is not required in order to avoid bugs in
     * implementations of the JVM where processing generic types can cause segmentation faults. Byte Buddy will undertake
     * a best effort to retain the generic type information and information about type annotations within the redefined
     * types' class files. Typically, this property can be meaningful in combination with a Java agent that only changes
     * byte code without changing a class type's structure. 如果不需要泛型类型信息以避免JVM实现中处理泛型类型可能导致分段错误的bug，那么设置此属性非常有用。ByteBuddy将尽最大努力在重新定义的类型的类文件中保留泛型类型信息和有关类型注释的信息。通常，与只更改字节码而不更改类类型结构的Java代理结合使用时，此属性可能很有意义
     * </p>
     */
    String RAW_TYPES_PROPERTY = "net.bytebuddy.raw";

    /**
     * Returns this type definition as a generic type. 转化为泛型
     *
     * @return This type definition represented as a generic type.
     */
    TypeDescription.Generic asGenericType();

    /**
     * Returns the erasure of this type. Wildcard types ({@link TypeDescription.Generic.Sort#WILDCARD})
     * do not have a well-defined erasure and cause an {@link IllegalStateException} to be thrown. 返回此类型的擦除。通配符类型（{@linkTypeDescription.Generic.Sort#WILDCARD})不要使用定义良好的擦除操作，并导致抛出{@link IllegalStateException}
     *
     * @return The erasure of this type.
     */
    TypeDescription asErasure();

    /**
     * Returns the super class of this type. A super type is only defined for non-generic types ({@link Sort#NON_GENERIC}),
     * parameterized types ({@link Sort#PARAMETERIZED}) or generic array types ({@link Sort#GENERIC_ARRAY}) types. Interface types
     * and the {@link Object} class do not define a super class where {@code null} is returned. Array types define {@link Object}
     * as their direct super class.
     *
     * @return The super class of this type or {@code null} if no super class exists for this type. 返回父类
     */
    TypeDescription.Generic getSuperClass();

    /**
     * Returns the interfaces that this type implements. A super type is only defined for non-generic types ({@link Sort#NON_GENERIC}),
     * parameterized types ({@link Sort#PARAMETERIZED}) or generic array types ({@link Sort#GENERIC_ARRAY}) types.
     *
     * @return The interfaces that this type implements. 返回实现的接口类型
     */
    TypeList.Generic getInterfaces();

    /**
     * Returns the fields that this type declares. A super type is only defined for non-generic types ({@link Sort#NON_GENERIC}), 返回此类型声明的字段。超级类型只为非泛型类型定义 ({@link Sort#NON_GENERIC})
     * parameterized types ({@link Sort#PARAMETERIZED}) or generic array types ({@link Sort#GENERIC_ARRAY}) types. Generic array
     * types never define fields and the returned list is always empty for such types. 参数化类型 ({@link Sort#PARAMETERIZED}) 或 泛型数组类型({@link Sort#GENERIC_ARRAY}) 类型。泛型数组类型从不定义字段，对于此类类型，返回的列表始终为空
     *
     * @return The fields that this type declares. A super type is only defined for non-generic types ({@link Sort#NON_GENERIC}),
     * parameterized types ({@link Sort#PARAMETERIZED}) or generic array types ({@link Sort#GENERIC_ARRAY}) types. Generic array
     * types never define methods and the returned list is always empty for such types. 返回Fileld类型
     */
    FieldList<?> getDeclaredFields();

    /**
     * Returns the methods that this type declares.
     *
     * @return The methods that this type declares. 返回method类型
     */
    MethodList<?> getDeclaredMethods();

    /**
     * <p>
     * Returns the component type of this type.
     * </p>
     * <p>
     * Only non-generic types ({@link TypeDescription.Generic.Sort#NON_GENERIC}) and generic array types
     * {@link TypeDescription.Generic.Sort#GENERIC_ARRAY}) define a component type. For other
     * types, an {@link IllegalStateException} is thrown. 返回数组的类型，比如String[]，就返回代表String的类型
     * </p>
     *
     * @return The component type of this type or {@code null} if this type does not represent an array type.
     */
    TypeDefinition getComponentType();

    /**
     * Returns the sort of the generic type this instance represents.
     *
     * @return The sort of the generic type. 返回对象代表的一堆类型，Sort是个集合
     */
    Sort getSort();

    /**
     * Returns the name of the type. For generic types, this name is their {@link Object#toString()} representations. For a non-generic
     * type, it is the fully qualified binary name of the type.
     *
     * @return The name of this type. record是jdk 14新特性，类似C的Struct
     */
    String getTypeName();

    /**
     * Returns the size of the type described by this instance. Wildcard types
     * ({@link TypeDescription.Generic.Sort#WILDCARD} do not have a well-defined a stack size and
     * cause an {@link IllegalStateException} to be thrown. 返回此实例描述的类型的大小。通配符类型 {@link TypeDescription.Generic.Sort#WILDCARD} 没有定义良好的堆栈大小并导致抛出 {@link IllegalStateException}
     *
     * @return The size of the type described by this instance. 栈帧大小
     */
    StackSize getStackSize();

    /**
     * Checks if the type described by this entity is an array.
     *
     * @return {@code true} if this type description represents an array.
     */
    boolean isArray();

    /**
     * Checks if the type described by this entity is a primitive type.
     *
     * @return {@code true} if this type description represents a primitive type.
     */
    boolean isPrimitive();

    /**
     * Checks if the type described by this instance represents {@code type}. 检查此实例描述的类型是否表示 {@code type}
     *
     * @param type The type of interest. 关注的类型
     * @return {@code true} if the type described by this instance represents {@code type}.
     */
    boolean represents(Type type);

    /**
     * Represents a {@link TypeDescription.Generic}'s form. 表示 {@link TypeDescription.Generic} 的形式  对象类型的常量的集合
     */
    enum Sort {

        /**
         * Represents a non-generic type. 表示非通用类型
         */
        NON_GENERIC,

        /**
         * Represents a generic array type. 表示通用数组类型
         */
        GENERIC_ARRAY,

        /**
         * Represents a parameterized type. 表示参数化类型
         */
        PARAMETERIZED,

        /**
         * Represents a wildcard type. 表示通配符类型  WildcardType是Type的子接口，用于描述形如“? extends classA” 或 “？super classB”的“泛型参数表达式”。List<? extends String>这种类型就叫WildcardType
         */
        WILDCARD,

        /**
         * Represents a type variable that is attached to a {@link net.bytebuddy.description.TypeVariableSource}. 表示附加到{@link net.bytebuddy.description.TypeVariableSource}的类型变量
         */
        VARIABLE,

        /**
         * Represents a type variable that is merely symbolic and is not attached to a {@link net.bytebuddy.description.TypeVariableSource}
         * and does not defined bounds. 表示仅是符号性的类型变量，未附加到{@link net.bytebuddy.description.TypeVariableSource}且未定义界限   代表了一个类型，但是仅仅是一个符号，并不会被绑定到net.bytebuddy.description.TypeVariableSource
         */
        VARIABLE_SYMBOLIC;

        /**
         * Describes a loaded generic type as a {@link TypeDescription.Generic}. 将加载的通用类型描述为{@link TypeDescription.Generic}
         * 将一个 java 中 Tpye 类型，转化为 TypeDescription.Generic 这个方法很关键提供了这样的转化
         * @param type The type to describe. 要描述的类型
         * @return A description of the provided generic type.
         */
        public static TypeDescription.Generic describe(Type type) {
            return describe(type, TypeDescription.Generic.AnnotationReader.NoOp.INSTANCE);
        }

        /**
         * Describes the generic type while using the supplied annotation reader for resolving type annotations if this
         * language feature is available on the current JVM. 如果当前JVM上提供了此语言功能，则在使用提供的注解阅读器 解析 类型注解时 描述泛型类型
         *
         * @param type             The type to describe.
         * @param annotationReader The annotation reader for extracting type annotations.
         * @return A description of the provided generic annotated type.
         */
        protected static TypeDescription.Generic describe(Type type, TypeDescription.Generic.AnnotationReader annotationReader) {
            if (type instanceof Class<?>) {
                return new TypeDescription.Generic.OfNonGenericType.ForLoadedType((Class<?>) type, annotationReader);
            } else if (type instanceof GenericArrayType) {
                return new TypeDescription.Generic.OfGenericArray.ForLoadedType((GenericArrayType) type, annotationReader);
            } else if (type instanceof ParameterizedType) {
                return new TypeDescription.Generic.OfParameterizedType.ForLoadedType((ParameterizedType) type, annotationReader);
            } else if (type instanceof TypeVariable) {
                return new TypeDescription.Generic.OfTypeVariable.ForLoadedType((TypeVariable<?>) type, annotationReader);
            } else if (type instanceof WildcardType) {
                return new TypeDescription.Generic.OfWildcardType.ForLoadedType((WildcardType) type, annotationReader);
            } else {
                throw new IllegalArgumentException("Unknown type: " + type);
            }
        }

        /**
         * Checks if this type sort represents a non-generic type. 检查此类型排序是否表示非泛型类型
         *
         * @return {@code true} if this sort form represents a non-generic. {@code true} 如果此排序形式表示非泛型
         */
        public boolean isNonGeneric() {
            return this == NON_GENERIC;
        }

        /**
         * Checks if this type sort represents a parameterized type. 检查此类型排序是否表示参数化类型
         *
         * @return {@code true} if this sort form represents a parameterized type.
         */
        public boolean isParameterized() {
            return this == PARAMETERIZED;
        }

        /**
         * Checks if this type sort represents a generic array. 检查此类型排序是否表示泛型数组
         *
         * @return {@code true} if this type sort represents a generic array.
         */
        public boolean isGenericArray() {
            return this == GENERIC_ARRAY;
        }

        /**
         * Checks if this type sort represents a wildcard. 检查此类型排序是否表示通配符
         *
         * @return {@code true} if this type sort represents a wildcard.
         */
        public boolean isWildcard() {
            return this == WILDCARD;
        }

        /**
         * Checks if this type sort represents a type variable of any form. 检查此类型排序是否表示任何形式的类型变量
         *
         * @return {@code true} if this type sort represents an attached type variable.
         */
        public boolean isTypeVariable() {
            return this == VARIABLE || this == VARIABLE_SYMBOLIC;
        }
    }

    /**
     * An iterator that iterates over a type's class hierarchy. 在类型的类层次结构上进行迭代的迭代器
     */
    class SuperClassIterator implements Iterator<TypeDefinition> {

        /**
         * The next class to represent.
         */
        private TypeDefinition nextClass;

        /**
         * Creates a new iterator.
         *
         * @param initialType The initial type of this iterator.
         */
        public SuperClassIterator(TypeDefinition initialType) {
            nextClass = initialType;
        }

        @Override
        public boolean hasNext() {
            return nextClass != null;
        }

        @Override
        public TypeDefinition next() {
            if (!hasNext()) {
                throw new NoSuchElementException("End of type hierarchy");
            }
            try {
                return nextClass;
            } finally {
                nextClass = nextClass.getSuperClass();
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove");
        }
    }
}
