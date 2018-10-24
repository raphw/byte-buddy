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
 * Indicates whether a parameter was denoted as {@code final} or not.
 */
public enum ParameterManifestation implements ModifierContributor.ForParameter {

    /**
     * A non-final parameter. (This is the default modifier.)
     */
    PLAIN(EMPTY_MASK),

    /**
     * A final parameter.
     */
    FINAL(Opcodes.ACC_FINAL);

    /**
     * The mask of this parameter manifestation.
     */
    private final int mask;

    /**
     * Creates a new parameter.
     *
     * @param mask The mask of this parameter.
     */
    ParameterManifestation(int mask) {
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
        return Opcodes.ACC_FINAL;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDefault() {
        return this == PLAIN;
    }

    /**
     * Checks if this instance represents a final state.
     *
     * @return {@code true} if this instance represents a final state.
     */
    public boolean isFinal() {
        return this == FINAL;
    }
}
