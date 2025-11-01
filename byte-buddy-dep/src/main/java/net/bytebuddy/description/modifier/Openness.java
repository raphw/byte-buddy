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
 * Defines a module's restriction on reflection on its packages.
 */
public enum Openness implements ModifierContributor.ForModule {

    /**
     * Modifier for a regular module. (This is the default modifier.)
     */
    CLOSED(EMPTY_MASK),

    /**
     * Modifier for an open module.
     */
    OPEN(Opcodes.ACC_OPEN);

    /**
     * The mask the modifier contributor.
     */
    private final int mask;

    /**
     * Creates a new ownership representation.
     *
     * @param mask The modifier mask of this instance.
     */
    Openness(int mask) {
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
        return Opcodes.ACC_OPEN;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDefault() {
        return this == CLOSED;
    }

    /**
     * Checks if the current state describes an {@code open} module.
     *
     * @return {@code true} if this openness representation represents an {@code open} module.
     */
    public boolean isOpen() {
        return this == OPEN;
    }
}
