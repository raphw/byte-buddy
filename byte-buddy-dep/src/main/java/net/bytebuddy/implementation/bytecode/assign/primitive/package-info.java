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
/**
 * {@link net.bytebuddy.implementation.bytecode.assign.Assigner} implementations of this package
 * are capable of handling primitive types or the {@code void} type. On assignments from or to reference types,
 * these assigners usually delegate a boxed assignment to a reference aware assigner.
 */
package net.bytebuddy.implementation.bytecode.assign.primitive;
