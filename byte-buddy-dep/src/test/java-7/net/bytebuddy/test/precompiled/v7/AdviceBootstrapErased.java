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

import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class AdviceBootstrapErased {

    public static ConstantCallSite bootstrap(MethodHandles.Lookup lookup,
                                             String invokedMethodName,
                                             MethodType erasedMethodType,
                                             String invokedClassName,
                                             String invokedMethodDescriptor) throws Exception {
        return new ConstantCallSite(lookup.findStatic(Class.forName(invokedClassName, false, lookup.lookupClass().getClassLoader()),
                invokedMethodName,
                MethodType.fromMethodDescriptorString(invokedMethodDescriptor, lookup.lookupClass().getClassLoader())).asType(erasedMethodType));
    }
}