/*
 * Copyright 2014 - Present Rafael Winterhalter
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
package net.bytebuddy.build.gradle.api;

import org.gradle.api.Transformer;

/**
 * A placeholder representation of Gradle's {@code org.gradle.api.provider.Provider} type.
 *
 * @param <T> The type's type parameter.
 */
@GradleType("org.gradle.api.provider.Provider")
public interface Provider<T> {

    /**
     * A placeholder representation of Gradle's {@code org.gradle.api.provider.Provider#get} method.
     *
     * @param transformer The method's argument.
     * @param <S>         The method's type parameter.
     * @return The method's return value.
     */
    <S> Provider<S> map(Transformer<? extends S, ? super T> transformer);

    /**
     * A placeholder representation of Gradle's {@code org.gradle.api.provider.Provider#get} method.
     *
     * @return The method's return value.
     */
    T get();
}
