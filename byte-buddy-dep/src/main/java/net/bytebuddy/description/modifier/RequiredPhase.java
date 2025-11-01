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
 * Describes when another module is considered as a requirement.
 */
public enum RequiredPhase implements ModifierContributor.ForModule.OfRequire {

    /**
     * Modifier for requiring another module during all phases. (This is the default modifier.)
     */
    NONE(EMPTY_MASK),

    /**
     * Modifier for requiring another module only during assembly.
     */
    STATIC(Opcodes.ACC_STATIC_PHASE);

    /**
     * The mask of the modifier contributor.
     */
    private final int mask;

    /**
     * Creates a new synthetic state representation.
     *
     * @param mask The modifier mask of this instance.
     */
    RequiredPhase(int mask) {
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
        return Opcodes.ACC_STATIC_PHASE;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDefault() {
        return this == NONE;
    }

    /**
     * Checks if the current state describes static phase requirement.
     *
     * @return {@code true} if the current state is a static phase requirement.
     */
    public boolean isStatic() {
        return this == STATIC;
    }
}