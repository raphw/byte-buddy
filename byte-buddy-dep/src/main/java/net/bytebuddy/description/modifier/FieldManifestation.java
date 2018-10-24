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
 * Describes the manifestation of a class's field, i.e. if a field is final, volatile or neither.
 */
public enum FieldManifestation implements ModifierContributor.ForField {

    /**
     * Modifier for a non-final, non-volatile field. (This is the default modifier.)
     */
    PLAIN(EMPTY_MASK),

    /**
     * Modifier for a final field.
     */
    FINAL(Opcodes.ACC_FINAL),

    /**
     * Modifier for a volatile field.
     */
    VOLATILE(Opcodes.ACC_VOLATILE);

    /**
     * The mask the modifier contributor.
     */
    private final int mask;

    /**
     * Creates a new field manifestation.
     *
     * @param mask The modifier mask of this instance.
     */
    FieldManifestation(int mask) {
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
        return Opcodes.ACC_FINAL | Opcodes.ACC_VOLATILE;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDefault() {
        return this == PLAIN;
    }

    /**
     * Returns {@code true} if this manifestation represents a {@code final} type.
     *
     * @return {@code true} if this manifestation represents a {@code final} type.
     */
    public boolean isFinal() {
        return (mask & Opcodes.ACC_FINAL) != 0;
    }

    /**
     * Returns {@code true} if this manifestation represents a {@code volatile} type.
     *
     * @return {@code true} if this manifestation represents a {@code volatile} type.
     */
    public boolean isVolatile() {
        return (mask & Opcodes.ACC_VOLATILE) != 0;
    }

    /**
     * Returns {@code true} if this manifestation represents a field that is neither {@code final} or {@code volatile}.
     *
     * @return {@code true} if this manifestation represents a field that is neither {@code final} or {@code volatile}.
     */
    public boolean isPlain() {
        return !isFinal() && !isVolatile();
    }
}
