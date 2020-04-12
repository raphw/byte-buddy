/*
 * Copyright 2014 - 2020 Rafael Winterhalter
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
package net.bytebuddy.description.type;

import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.FilterableList;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Implementations represent a list of record component descriptions.
 *
 * @param <T> The type of record component descriptions represented by this list.
 */
public interface RecordComponentList<T extends RecordComponentDescription> extends FilterableList<T, RecordComponentList<T>> {

    /**
     * Transforms the list of record component descriptions into a list of detached tokens. All types that are matched by the provided
     * target type matcher are substituted by {@link net.bytebuddy.dynamic.TargetType}.
     *
     * @param matcher A matcher that indicates type substitution.
     * @return The transformed token list.
     */
    List<RecordComponentDescription.Token> asTokenList(ElementMatcher<? super TypeDescription> matcher);

    /**
     * An abstract base implementation of a list of record components.
     *
     * @param <S> The type of record component descriptions represented by this list.
     */
    abstract class AbstractBase<S extends RecordComponentDescription> extends FilterableList.AbstractBase<S, RecordComponentList<S>> implements RecordComponentList<S> {

        /**
         * {@inheritDoc}
         */
        public List<RecordComponentDescription.Token> asTokenList(ElementMatcher<? super TypeDescription> matcher) {
            List<RecordComponentDescription.Token> tokens = new ArrayList<RecordComponentDescription.Token>(size());
            for (RecordComponentDescription recordComponentDescription : this) {
                tokens.add(recordComponentDescription.asToken(matcher));
            }
            return tokens;
        }

        @Override
        protected RecordComponentList<S> wrap(List<S> values) {
            return new Explicit<S>(values);
        }
    }

    /**
     * A list of loaded record components.
     */
    class ForLoadedRecordComponents extends AbstractBase<RecordComponentDescription.InDefinedShape> {

        /**
         * The represented record components.
         */
        private final List<?> recordComponents;

        /**
         * Creates a list of loaded record components.
         *
         * @param recordComponent The represented record components.
         */
        protected ForLoadedRecordComponents(Object... recordComponent) {
            this(Arrays.asList(recordComponent));
        }

        /**
         * Creates a list of loaded record components.
         *
         * @param recordComponents The represented record components.
         */
        protected ForLoadedRecordComponents(List<?> recordComponents) {
            this.recordComponents = recordComponents;
        }

        /**
         * {@inheritDoc}
         */
        public RecordComponentDescription.InDefinedShape get(int index) {
            return new RecordComponentDescription.ForLoadedRecordComponent((AnnotatedElement) recordComponents.get(index));
        }

        /**
         * {@inheritDoc}
         */
        public int size() {
            return recordComponents.size();
        }
    }

    /**
     * A wrapper implementation of an explicit list of record components.
     *
     * @param <S> The type of record component descriptions represented by this list.
     */
    class Explicit<S extends RecordComponentDescription> extends AbstractBase<S> {

        /**
         * The record components represented by this list.
         */
        private final List<? extends S> recordComponents;

        /**
         * Creates a new list of record component descriptions.
         *
         * @param recordComponent The represented record components.
         */
        @SuppressWarnings("unchecked")
        public Explicit(S... recordComponent) {
            this(Arrays.asList(recordComponent));
        }

        /**
         * Creates a new list of record component descriptions.
         *
         * @param recordComponents The represented record components.
         */
        public Explicit(List<? extends S> recordComponents) {
            this.recordComponents = recordComponents;
        }

        /**
         * {@inheritDoc}
         */
        public S get(int index) {
            return recordComponents.get(index);
        }

        /**
         * {@inheritDoc}
         */
        public int size() {
            return recordComponents.size();
        }
    }

    /**
     * A list of record components described as tokens.
     */
    class ForTokens extends AbstractBase<RecordComponentDescription.InDefinedShape> {

        /**
         * The record component's declaring type.
         */
        private final TypeDescription typeDescription;

        /**
         * The list of represented tokens.
         */
        private final List<? extends RecordComponentDescription.Token> tokens;

        /**
         * Creates a new list of record components that are described as tokens.
         *
         * @param typeDescription The record component's declaring type.
         * @param token           The list of represented tokens.
         */
        public ForTokens(TypeDescription typeDescription, RecordComponentDescription.Token... token) {
            this(typeDescription, Arrays.asList(token));
        }

        /**
         * Creates a new list of record components that are described as tokens.
         *
         * @param typeDescription The record component's declaring type.
         * @param tokens          The list of represented tokens.
         */
        public ForTokens(TypeDescription typeDescription, List<? extends RecordComponentDescription.Token> tokens) {
            this.typeDescription = typeDescription;
            this.tokens = tokens;
        }

        @Override
        public RecordComponentDescription.InDefinedShape get(int index) {
            return new RecordComponentDescription.Latent(typeDescription, tokens.get(index));
        }

        @Override
        public int size() {
            return tokens.size();
        }
    }

    /**
     * An empty list of record components.
     *
     * @param <S> The type of record component descriptions represented by this list.
     */
    class Empty<S extends RecordComponentDescription> extends FilterableList.Empty<S, RecordComponentList<S>> implements RecordComponentList<S> {

        /**
         * {@inheritDoc}
         */
        public List<RecordComponentDescription.Token> asTokenList(ElementMatcher<? super TypeDescription> matcher) {
            return Collections.emptyList();
        }
    }
}
