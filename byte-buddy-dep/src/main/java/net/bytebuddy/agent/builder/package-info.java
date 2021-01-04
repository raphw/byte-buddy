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
 * An agent builder is used to easily implement load-time class-transformations using a Java agent. The API
 * builds on Java's {@link java.lang.instrument.ClassFileTransformer} and {@link java.lang.instrument.Instrumentation}
 * but offers higher-level APIs in order to allow for the implementation of very readable transformations using
 * {@link net.bytebuddy.ByteBuddy}.
 */
package net.bytebuddy.agent.builder;
