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
 * Determines the ownership of a field or method, i.e. if a member is defined in as {@code static}
 * and belongs to a class or in contrast to an instance.
 */
public enum Ownership implements ModifierContributor.ForField, ModifierContributor.ForMethod, ModifierContributor.ForType {

    /**
     * Modifier for a instance ownership of a type member. (This is the default modifier.)
     */
    MEMBER(EMPTY_MASK),

    /**
     * Modifier for type ownership of a type member.
     */
    STATIC(Opcodes.ACC_STATIC);

    /**
     * The mask the modifier contributor.
     */
    private final int mask;

    /**
     * Creates a new ownership representation.
     *
     * @param mask The modifier mask of this instance.
     */
    Ownership(int mask) {
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
        return Opcodes.ACC_STATIC;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDefault() {
        return this == MEMBER;
    }

    /**
     * Checks if the current state describes a {@code static} member.
     *
     * @return {@code true} if this ownership representation represents a {@code static} member.
     */
    public boolean isStatic() {
        return this == STATIC;
    }
}
