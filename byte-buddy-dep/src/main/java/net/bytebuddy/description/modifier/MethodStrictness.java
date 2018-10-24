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
 * A modifier contributor to determine the use of {@code strictfp} on a method.
 */
public enum MethodStrictness implements ModifierContributor.ForMethod {

    /**
     * Modifier for a non-strict method. (This is the default modifier.)
     */
    PLAIN(EMPTY_MASK),

    /**
     * Modifier for a method that applies strict floating-point computation.
     */
    STRICT(Opcodes.ACC_STRICT);

    /**
     * The modifier contributors mask.
     */
    private final int mask;

    /**
     * Creates a new modifier contributor for a method.
     *
     * @param mask The modifier contributors mask.
     */
    MethodStrictness(int mask) {
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
        return Opcodes.ACC_STRICT;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDefault() {
        return this == PLAIN;
    }

    /**
     * Returns {@code true} if this modifier contributor indicates strict floating-point computation.
     *
     * @return {@code true} if this modifier contributor indicates strict floating-point computation.
     */
    public boolean isStrict() {
        return this == STRICT;
    }
}
