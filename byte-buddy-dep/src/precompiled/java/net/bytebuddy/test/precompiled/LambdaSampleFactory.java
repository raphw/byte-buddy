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

import java.io.Serializable;
import java.util.concurrent.Callable;
import java.util.function.Function;

public class LambdaSampleFactory {

    private static final String FOO = "foo";

    private String foo = FOO;

    public Callable<String> nonCapturing() {
        return () -> FOO;
    }

    public Callable<String> argumentCapturing(String foo) {
        return () -> foo;
    }

    public Callable<String> instanceCapturing() {
        return () -> foo;
    }

    public Function<String, String> nonCapturingWithArguments() {
        return argument -> argument;
    }

    public Function<String, String> capturingWithArguments(String foo) {
        return argument -> argument + this.foo + foo;
    }

    public Callable<String> serializable(String foo) {
        return (Callable<String> & Serializable) () -> foo;
    }

    public Runnable returnTypeTransforming() {
        return this::nonCapturing;
    }

    public Callable<Object> instanceReturning() {
        return Object::new;
    }
}