package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.utility.RandomString;

/**
 * A method name transformer provides a unique mapping of a method's name to an alternative name. 方法名称转换器提供方法名称到备用名称的唯一映射
 *
 * @see MethodRebaseResolver
 */
public interface MethodNameTransformer {

    /**
     * Transforms a method's name to an alternative name. This name must not be equal to any existing method of the
     * created class. 将方法名称转换为备用名称。此名称不能等于所创建类的任何现有方法
     *
     * @param methodDescription The original method.
     * @return The alternative name.
     */
    String transform(MethodDescription methodDescription);

    /**
     * A method name transformer that adds a fixed suffix to an original method name, separated by a {@code $}. 一种方法名转换器，它向原始方法名添加一个固定后缀，用{@code $}分隔
     */
    @HashCodeAndEqualsPlugin.Enhance
    class Suffixing implements MethodNameTransformer {

        /**
         * The default suffix to add to an original method name.
         */
        private static final String DEFAULT_SUFFIX = "original$";

        /**
         * The suffix to append to a method name.
         */
        private final String suffix;

        /**
         * Creates a new suffixing method name transformer which adds a default suffix with a random name component.
         *
         * @return A method name transformer that adds a randomized suffix to the original method name.
         */
        public static MethodNameTransformer withRandomSuffix() {
            return new Suffixing(DEFAULT_SUFFIX + RandomString.make());
        }

        /**
         * Creates a new suffixing method name transformer.
         *
         * @param suffix The suffix to add to the method name before the seed.
         */
        public Suffixing(String suffix) {
            this.suffix = suffix;
        }

        @Override
        public String transform(MethodDescription methodDescription) {
            return methodDescription.getInternalName() + "$" + suffix;
        }
    }

    /**
     * A method name transformer that adds a fixed prefix to an original method name. 向原始方法名称添加固定前缀的方法名称转换器
     */
    @HashCodeAndEqualsPlugin.Enhance
    class Prefixing implements MethodNameTransformer {

        /**
         * The default prefix to add to an original method name.
         */
        private static final String DEFAULT_PREFIX = "original";

        /**
         * The prefix that is appended.
         */
        private final String prefix;

        /**
         * Creates a new prefixing method name transformer using a default prefix.
         */
        public Prefixing() {
            this(DEFAULT_PREFIX);
        }

        /**
         * Creates a new prefixing method name transformer.
         *
         * @param prefix The prefix being used.
         */
        public Prefixing(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public String transform(MethodDescription methodDescription) {
            return prefix + methodDescription.getInternalName();
        }
    }
}
