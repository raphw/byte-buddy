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
package net.bytebuddy.build.gradle;

import net.bytebuddy.build.Plugin;
import org.gradle.api.tasks.Input;

import java.io.Serializable;

/**
 * Describes an argument to a {@link Plugin} constuctor.
 */
public class PluginArgument implements Serializable {

    /**
     * The serial version UID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The argument index.
     */
    private int index;

    /**
     * The argument value.
     */
    private Object value;

    /**
     * Creates a new plugin argument with default initialization.
     */
    @SuppressWarnings("unused")
    public PluginArgument() {
        /* empty */
    }

    /**
     * Creates a new plugin argument assignment.
     *
     * @param index The argument index.
     */
    protected PluginArgument(int index) {
        this.index = index;
    }

    /**
     * Creates a new plugin argument assignment.
     *
     * @param index The argument index.
     * @param value The argument value.
     */
    protected PluginArgument(int index, Object value) {
        this.index = index;
        this.value = value;
    }

    /**
     * Returns the argument index.
     *
     * @return The argument index.
     */
    @Input
    public int getIndex() {
        return index;
    }

    /**
     * Sets the argument index.
     *
     * @param index The argument index.
     */
    public void setIndex(int index) {
        this.index = index;
    }

    /**
     * Returns the argument value.
     *
     * @return The argument value.
     */
    @Input
    public Object getValue() {
        return value;
    }

    /**
     * Sets the argument value.
     *
     * @param value The argument value.
     */
    public void setValue(Object value) {
        this.value = value;
    }

    /**
     * Resolves this plugin argument to an argument resolver.
     *
     * @return An argument resolver that represents this plugin argument.
     */
    protected Plugin.Factory.UsingReflection.ArgumentResolver toArgumentResolver() {
        return new Plugin.Factory.UsingReflection.ArgumentResolver.ForIndex(index, value);
    }
}
