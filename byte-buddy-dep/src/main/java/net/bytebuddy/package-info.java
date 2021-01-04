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
/**
 * Byte Buddy is a library for creating Java classes at runtime of a Java program. For this purpose, the
 * {@link net.bytebuddy.ByteBuddy} class serves as an entry point. The following example
 * <pre>
 * Class&#60;?&#62; dynamicType = new ByteBuddy()
 *    .subclass(Object.class)
 *    .implement(Serializable.class)
 *    .intercept(named("toString"), FixedValue.value("Hello World!"))
 *    .make()
 *    .load(getClass().getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
 *    .getLoaded();
 * dynamicType.newInstance().toString; // returns "Hello World!"</pre>
 * creates a subclass of the {@link java.lang.Object} class which implements the {@link java.io.Serializable}
 * interface. The {@link java.lang.Object#toString()} method is overridden to return {@code Hello World!}.
 */
package net.bytebuddy;
