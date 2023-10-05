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
package net.bytebuddy.plugin.gradle.test.jar;

import net.bytebuddy.asm.Advice;

/**
 * A sample advice class.
 */
public class AdviceForLocalClass {

    /**
     * Applies an exit advice.
     *
     * @param returned The return value.
     */
    @Advice.OnMethodExit
    public static void enter(@Advice.Return(readOnly = false) String returned) {
        returned = "instrumented local class from jar plugin";
    }
}
