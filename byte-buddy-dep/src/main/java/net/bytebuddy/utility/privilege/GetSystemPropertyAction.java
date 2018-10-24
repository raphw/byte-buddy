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
package net.bytebuddy.utility.privilege;

import net.bytebuddy.build.HashCodeAndEqualsPlugin;

import java.security.PrivilegedAction;

/**
 * An action for reading a system property as a privileged action.
 */
@HashCodeAndEqualsPlugin.Enhance
public class GetSystemPropertyAction implements PrivilegedAction<String> {

    /**
     * The property key.
     */
    private final String key;

    /**
     * Creates a new action for reading a system property.
     *
     * @param key The property key.
     */
    public GetSystemPropertyAction(String key) {
        this.key = key;
    }

    /**
     * {@inheritDoc}
     */
    public String run() {
        return System.getProperty(key);
    }
}
