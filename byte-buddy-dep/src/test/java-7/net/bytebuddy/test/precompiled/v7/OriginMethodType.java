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
package net.bytebuddy.test.precompiled.v7;

import net.bytebuddy.implementation.bind.annotation.Origin;

import java.lang.invoke.MethodType;

public class OriginMethodType {

    public static Class<?> TYPE = MethodType.class;

    public static Object foo(@Origin MethodType methodType) {
        return methodType;
    }
}
