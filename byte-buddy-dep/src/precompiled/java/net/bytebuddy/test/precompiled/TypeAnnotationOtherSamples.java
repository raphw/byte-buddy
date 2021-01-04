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
package net.bytebuddy.test.precompiled;

public abstract class TypeAnnotationOtherSamples<T> {

    @TypeAnnotation(0)
    Void foo;

    @TypeAnnotation(1) TypeAnnotationOtherSamples<@TypeAnnotation(2) Void>.@TypeAnnotation(3) Bar<@TypeAnnotation(4) Void> bar;

    @TypeAnnotation(5) @OtherTypeAnnotation(6) Void qux;

    @TypeAnnotation(7) Qux.@TypeAnnotation(8) Baz baz;

    @TypeAnnotation(9)
    abstract Void foo(@TypeAnnotation(10) Void v) throws @TypeAnnotation(11) Exception;

    class Bar<S> {
        /* empty */
    }

    static class Qux {

        class Baz {
            /* empty */
        }
    }
}
