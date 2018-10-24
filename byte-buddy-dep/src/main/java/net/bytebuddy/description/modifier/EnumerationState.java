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
package net.bytebuddy.description.modifier;

import org.objectweb.asm.Opcodes;

/**
 * Determines if a type describes an enumeration. Note that enumerations must never also be interfaces.
 */
public enum EnumerationState implements ModifierContributor.ForType, ModifierContributor.ForField {

    /**
     * Modifier for marking a type as a non-enumeration. (This is the default modifier.)
     */
    PLAIN(EMPTY_MASK),

    /**
     * Modifier for marking a type as an enumeration.
     */
    ENUMERATION(Opcodes.ACC_ENUM);

    /**
     * The mask of the modifier contributor.
     */
    private final int mask;

    /**
     * Creates a new enumeration state representation.
     *
     * @param mask The modifier mask of this instance.
     */
    EnumerationState(int mask) {
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
        return Opcodes.ACC_ENUM;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDefault() {
        return this == PLAIN;
    }

    /**
     * Checks if the current state describes the enum state.
     *
     * @return {@code true} if the current state describes an enumeration.
     */
    public boolean isEnumeration() {
        return this == ENUMERATION;
    }
}
