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
package net.bytebuddy.build;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.asm.Advice;

/**
 * An advice class for caching a {@code short} value.
 */
@SuppressFBWarnings(value = "NM_CLASS_NAMING_CONVENTION", justification = "Name is chosen to optimize for simple lookup")
class CachedReturnPlugin$short {

    /**
     * A constructor that prohibits the instantiation of the class.
     */
    private CachedReturnPlugin$short() {
        throw new UnsupportedOperationException("This class is merely an advice template and should not be instantiated");
    }

    /**
     * The enter advice.
     *
     * @param cached The cached field's value.
     * @return {@code true} if a cached value exists.
     */
    @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
    protected static short enter(@CachedReturnPlugin.CacheField short cached) {
        return cached;
    }

    /**
     * The exit advice.
     *
     * @param returned The value that was returned by the method's execution or {@code 0} if it was not executed.
     * @param cached   The previously cached value or {@code 0} if no previous value exists.
     */
    @Advice.OnMethodExit
    @SuppressFBWarnings(value = {"UC_USELESS_VOID_METHOD", "DLS_DEAD_LOCAL_STORE"}, justification = "Advice method serves as a template")
    protected static void exit(@Advice.Return(readOnly = false) short returned, @CachedReturnPlugin.CacheField short cached) {
        if (returned == 0) {
            returned = cached;
        } else {
            cached = returned;
        }
    }
}
