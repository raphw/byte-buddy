/*
 * Copyright 2014 - 2018 Rafael Winterhalter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bytebuddy.dynamic.scaffold.inline;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.utility.RandomString;

/**
 * A method name transformer provides a unique mapping of a method's name to an alternative name.
 *
 * @see MethodRebaseResolver
 */
public interface MethodNameTransformer {

    /**
     * Transforms a method's name to an alternative name. This name must not be equal to any existing method of the
     * created class.
     *
     * @param methodDescription The original method.
     * @return The alternative name.
     */
    String transform(MethodDescription methodDescription);

    /**
     * A method name transformer that adds a fixed suffix to an original method name, separated by a {@code $}.
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

        /**
         * {@inheritDoc}
         */
        public String transform(MethodDescription methodDescription) {
            return methodDescription.getInternalName() + "$" + suffix;
        }
    }

    /**
     * A method name transformer that adds a fixed prefix to an original method name.
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

        /**
         * {@inheritDoc}
         */
        public String transform(MethodDescription methodDescription) {
            return prefix + methodDescription.getInternalName();
        }
    }
}
