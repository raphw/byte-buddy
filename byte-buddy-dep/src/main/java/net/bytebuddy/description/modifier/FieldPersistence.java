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
 * Describes the persistence of a field, i.e. if it is {@code transient}.
 */
public enum FieldPersistence implements ModifierContributor.ForField {

    /**
     * Modifier for a non-transient field. (This is the default modifier.)
     */
    PLAIN(EMPTY_MASK),

    /**
     * Modifier for a transient field.
     */
    TRANSIENT(Opcodes.ACC_TRANSIENT);

    /**
     * The modifier mask for this persistence type.
     */
    private final int mask;

    /**
     * Creates a new field persistence description.
     *
     * @param mask The modifier mask for this persistence type.
     */
    FieldPersistence(int mask) {
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
        return Opcodes.ACC_TRANSIENT;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDefault() {
        return this == PLAIN;
    }

    /**
     * Returns {@code true} if this manifestation represents a field that is {@code transient}.
     *
     * @return {@code true} if this manifestation represents a field that is {@code transient}.
     */
    public boolean isTransient() {
        return (mask & Opcodes.ACC_TRANSIENT) != 0;
    }
}
