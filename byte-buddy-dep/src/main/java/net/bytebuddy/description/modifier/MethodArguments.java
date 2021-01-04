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
package net.bytebuddy.description.modifier;

import org.objectweb.asm.Opcodes;

/**
 * Describes if a method allows varargs arguments.
 */
public enum MethodArguments implements ModifierContributor.ForMethod {

    /**
     * Describes a method that does not permit varargs.
     */
    PLAIN(EMPTY_MASK),

    /**
     * Describes a method that permits varargs.
     */
    VARARGS(Opcodes.ACC_VARARGS);

    /**
     * The mask of the modifier contributor.
     */
    private final int mask;

    /**
     * Creates a new method arguments representation.
     *
     * @param mask The mask of this instance.
     */
    MethodArguments(int mask) {
        this.mask = mask;
    }

    /**
     * {@inheritDoc}
     */
    public int getMask() {
        return mask;
    }

    /**
     * {@inheritDoc}
     */
    public int getRange() {
        return Opcodes.ACC_VARARGS;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDefault() {
        return this == PLAIN;
    }

    /**
     * Checks if the current state describes a varargs methods.
     *
     * @return {@code true} if the current state represents a varargs method.
     */
    public boolean isVarArgs() {
        return this == VARARGS;
    }
}
