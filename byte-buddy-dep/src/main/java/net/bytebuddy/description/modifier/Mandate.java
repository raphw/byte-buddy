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
 * Indicates if a member is mandated.
 */
public enum Mandate implements ModifierContributor.ForField, ModifierContributor.ForMethod, ModifierContributor.ForParameter {

    /**
     * Modifier for a non-mandated member. (The default modifier.)
     */
    PLAIN(EMPTY_MASK),

    /**
     * Modifier for a mandated member.
     */
    MANDATED(Opcodes.ACC_MANDATED);

    /**
     * The modifier mask for this mandate.
     */
    private final int mask;

    /**
     * Creates a new mandate state.
     *
     * @param mask The modifier mask for this mandate.
     */
    Mandate(int mask) {
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
        return Opcodes.ACC_MANDATED;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDefault() {
        return this == PLAIN;
    }

    /**
     * Returns {@code true} if this state is mandated.
     *
     * @return {@code true} if this state is mandated.
     */
    public boolean isMandated() {
        return this == MANDATED;
    }
}
