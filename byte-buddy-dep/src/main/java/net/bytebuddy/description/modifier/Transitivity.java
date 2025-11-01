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
 * Defines if a module requires another module also for its users.
 */
public enum Transitivity implements ModifierContributor.ForModule.OfRequire {

    /**
     * Modifier for not marking a type member as synthetic. (This is the default modifier.)
     */
    NONE(EMPTY_MASK),

    /**
     * Modifier for marking a type member as transitive.
     */
    TRANSITIVE(Opcodes.ACC_TRANSITIVE);

    /**
     * The mask of the modifier contributor.
     */
    private final int mask;

    /**
     * Creates a new synthetic state representation.
     *
     * @param mask The modifier mask of this instance.
     */
    Transitivity(int mask) {
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
        return Opcodes.ACC_TRANSITIVE;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDefault() {
        return this == NONE;
    }

    /**
     * Checks if the current state describes transitivity.
     *
     * @return {@code true} if the current state is transitive.
     */
    public boolean isTransitive() {
        return this == TRANSITIVE;
    }
}