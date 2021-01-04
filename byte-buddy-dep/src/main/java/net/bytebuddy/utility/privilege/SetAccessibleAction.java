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

import net.bytebuddy.build.HashCodeAndEqualsPlugin;

import java.lang.reflect.AccessibleObject;
import java.security.PrivilegedAction;

/**
 * An action for making an {@link AccessibleObject} accessible.
 *
 * @param <T> The type of the accessible object.
 */
@HashCodeAndEqualsPlugin.Enhance
public class SetAccessibleAction<T extends AccessibleObject> implements PrivilegedAction<T> {

    /**
     * The accessible object.
     */
    private final T accessibleObject;

    /**
     * Creates a new access action.
     *
     * @param accessibleObject The accessible object.
     */
    public SetAccessibleAction(T accessibleObject) {
        this.accessibleObject = accessibleObject;
    }

    /**
     * {@inheritDoc}
     */
    public T run() {
        accessibleObject.setAccessible(true);
        return accessibleObject;
    }
}
