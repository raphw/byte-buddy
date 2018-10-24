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
 * An {@link net.bytebuddy.implementation.bytecode.assign.Assigner} is responsible for transforming
 * a given {@link net.bytebuddy.description.type.TypeDescription} into another one. In doing so, an assigner is also
 * able to determine that some assignment is illegal.
 */
package net.bytebuddy.implementation.bytecode.assign;
