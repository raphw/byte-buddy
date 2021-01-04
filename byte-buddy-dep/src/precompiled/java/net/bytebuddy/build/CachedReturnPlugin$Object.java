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
import net.bytebuddy.implementation.bytecode.assign.Assigner;

/**
 * An advice class for caching a reference value.
 */
class CachedReturnPlugin$Object {

    /**
     * A constructor that prohibits the instantiation of the class.
     */
    private CachedReturnPlugin$Object() {
        throw new UnsupportedOperationException("This class is merely an advice template and should not be instantiated");
    }

    /**
     * The enter advice.
     *
     * @param cached The cached field's value.
     * @return {@code true} if a cached value exists.
     */
    @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
    protected static Object enter(@CachedReturnPlugin.CacheField Object cached) {
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
    protected static void exit(@Advice.Return(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object returned, @CachedReturnPlugin.CacheField Object cached) {
        if (returned == null) {
            returned = cached;
        } else {
            cached = returned;
        }
    }
}
