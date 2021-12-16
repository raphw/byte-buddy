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
package net.bytebuddy.agent.utility.nullability;

import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierDefault;
import javax.annotation.meta.TypeQualifierNickname;
import java.lang.annotation.*;

/**
 * Indicates that a field, method or parameter can never be {@code null}. Typically, this does not need to
 * be declared explicitly but is guaranteed by {@link ByDefault}.
 */
@Documented
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Nonnull
@TypeQualifierNickname
public @interface NeverNull {

    /**
     * Indicates that any field, method return or method and constructor parameter of a package is never {@code null}.
     */
    @Documented
    @Target(ElementType.PACKAGE)
    @Retention(RetentionPolicy.RUNTIME)
    @Nonnull
    @TypeQualifierDefault({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
    @interface ByDefault {
        /* empty */
    }
}
