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
package net.bytebuddy.test.precompiled;

public abstract class ReceiverTypeSample {

    abstract void foo(@TypeAnnotation(0)ReceiverTypeSample this);

    ReceiverTypeSample() {
        /* empty */
    }

    abstract class Inner {

        abstract void foo(@TypeAnnotation(1)Inner this);

        Inner(@TypeAnnotation(2)ReceiverTypeSample ReceiverTypeSample.this) {
            /* empty */
        }
    }

    abstract static class Nested {

        abstract void foo(@TypeAnnotation(3)Nested this);

        Nested() {
            /* empty */
        }
    }

    abstract static class Generic<T> {

        abstract void foo(@TypeAnnotation(4)Generic<@TypeAnnotation(5) T>this);

        Generic() {
            /* empty */
        }

        abstract class Inner<S> {

            abstract void foo(@TypeAnnotation(6)Generic<@TypeAnnotation(7) T>.@TypeAnnotation(8) Inner<@TypeAnnotation(9) S>this);

            Inner(@TypeAnnotation(10)Generic<@TypeAnnotation(11) T>Generic.this) {
                /* empty */
            }
        }

        abstract static class Nested<S> {

            abstract void foo(@TypeAnnotation(12)Nested<@TypeAnnotation(13) S>this);

            Nested() {
                /* empty */
            }
        }
    }
}
