package net.bytebuddy.matcher;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.method.MethodDescription;

/**
 * Matches a method description by its general characteristics which are represented as a
 * {@link net.bytebuddy.matcher.MethodSortMatcher.Sort}. 通过表示为 {@link net.bytebuddy.matcher.MethodSortMatcher.Sort} 的一般特征匹配方法描述
 *
 * @param <T> The type of the matched entity.
 */
@HashCodeAndEqualsPlugin.Enhance
public class MethodSortMatcher<T extends MethodDescription> extends ElementMatcher.Junction.AbstractBase<T> {

    /**
     * The sort of method description to be matched by this element matcher. 此元素匹配器要匹配的方法描述的种类
     */
    private final Sort sort;

    /**
     * Creates a new element matcher that matches a specific sort of method description.
     *
     * @param sort The sort of method description to be matched by this element matcher.
     */
    public MethodSortMatcher(Sort sort) {
        this.sort = sort;
    }

    @Override
    public boolean matches(T target) {
        return sort.isSort(target);
    }

    @Override
    public String toString() {
        return sort.getDescription();
    }

    /**
     * Represents a specific characteristic of a method description. 表示方法描述的特定特征
     */
    public enum Sort {

        /**
         * Matches method descriptions that represent methods, not constructors or the type initializer. 匹配表示方法的方法描述，而不是构造函数或类初始化器
         */
        METHOD("isMethod()") {
            @Override
            protected boolean isSort(MethodDescription target) {
                return target.isMethod();
            }
        },


        /**
         * Matches method descriptions that represent constructors, not methods or the type initializer.
         */
        CONSTRUCTOR("isConstructor()") {
            @Override
            protected boolean isSort(MethodDescription target) {
                return target.isConstructor();
            }
        },

        /**
         * Matches method descriptions that represent the type initializers.
         */
        TYPE_INITIALIZER("isTypeInitializer()") {
            @Override
            protected boolean isSort(MethodDescription target) {
                return target.isTypeInitializer();
            }
        },

        /**
         * Matches method descriptions that are overridable.
         */
        VIRTUAL("isVirtual()") {
            @Override
            protected boolean isSort(MethodDescription target) {
                return target.isVirtual();
            }
        },

        /**
         * Matches method descriptions that represent Java 8 default methods.
         */
        DEFAULT_METHOD("isDefaultMethod()") {
            @Override
            protected boolean isSort(MethodDescription target) {
                return target.isDefaultMethod();
            }
        };

        /**
         * A textual representation of the method sort that is represented by this instance.
         */
        private final String description;

        /**
         * Creates a new method sort representation.
         *
         * @param description A textual representation of the method sort that is represented by this instance.
         */
        Sort(String description) {
            this.description = description;
        }

        /**
         * Determines if a method description is of the represented method sort. 确定方法描述是否为表示的方法排序
         *
         * @param target A textual representation of the method sort that is represented by this instance.
         * @return {@code true} if the given method if of the method sort that is represented by this instance.
         */
        protected abstract boolean isSort(MethodDescription target);

        /**
         * Returns a textual representation of this instance's method sort.
         *
         * @return A textual representation of this instance's method sort.
         */
        protected String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            return "MethodSortMatcher.Sort." + name();
        }
    }
}
