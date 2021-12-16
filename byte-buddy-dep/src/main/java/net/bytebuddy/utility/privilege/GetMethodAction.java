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
package net.bytebuddy.utility.privilege;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.utility.nullability.MaybeNull;

import java.lang.reflect.Method;
import java.security.PrivilegedAction;

/**
 * Resolves a public method for a given type or returns {@code null} if the type or method are not available or
 * if a resolution is not possible.
 */
@HashCodeAndEqualsPlugin.Enhance
public class GetMethodAction implements PrivilegedAction<Method> {

    /**
     * The name of the type.
     */
    private final String type;

    /**
     * The name of the method.
     */
    private final String name;

    /**
     * The parameter types of the method.
     */
    private final Class<?>[] parameter;

    /**
     * Creates a new privileged action for resolving a {@link Method}.
     *
     * @param type      The name of the type.
     * @param name      The name of the method.
     * @param parameter The parameter types of the method.
     */
    public GetMethodAction(String type, String name, Class<?>... parameter) {
        this.type = type;
        this.name = name;
        this.parameter = parameter;
    }

    /**
     * {@inheritDoc}
     */
    @MaybeNull
    @SuppressFBWarnings(value = "REC_CATCH_EXCEPTION", justification = "Exception should not be rethrown but trigger a fallback.")
    public Method run() {
        try {
            return Class.forName(type).getMethod(name, parameter);
        } catch (Exception ignored) {
            return null;
        }
    }
}
