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
package net.bytebuddy.test.precompiled.v17;

public sealed class Sealed permits Sealed.SubNonSealed, Sealed.SubSealed, Sealed.SubFinal {

    public static non-sealed

    class SubNonSealed extends Sealed {
        /* empty */
    }

    public static sealed class SubSealed extends Sealed permits SubSealed.SubSubFinal {

        public static final class SubSubFinal extends SubSealed {
            /* empty */
        }
    }

    public static final class SubFinal extends Sealed {
        /* empty */
    }
}
